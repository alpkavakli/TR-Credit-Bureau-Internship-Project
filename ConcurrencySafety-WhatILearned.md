# Concurrency Safety, What I Learned

## So what's this Tomcat
Tomcat's job is to be a HTTP server and run Java Web Code. It owns the TCP socket on port 8080. It does some low-level grunt work like parsing HTTP, and other network stuff.

In node.js, this job is already built-in the language, we don't need a seperate language.
But in Java, we need a seperate software that handles it.

### What's a servlet
A servlet is a java object that handles an HTTP request.

**Servlet Containers** like Tomcat handles these servlet objects, for example which request gets handled by which servlet.

Tomcat is a thread pool (default 200). Each request is a thread held for the entire request. Blocking is normal and expected in here, the concurrency is handled by having many threads.

Also, each thread uses a database connection from a database pool (whichever connection is empty, default 10 connections )

### If it consists of threads, what happens when many threads try to use and change the same db column in tomcat?

Java doesn't help us at all, all the threads fire SQL at database, and database is what serializes conflicts like simultaneous changes through locks and transactions.

### So how does the database actually serialize conflicts

Every thread gets its own **connection** and fires SQL. The DB keeps things safe with **transactions** (a group of statements that is all-or-nothing = atomic, and doesn't see other transactions' half-done work = isolated) and **locks**.

In Spring you start a transaction by putting **@Transactional** on a service method.

### The classic bug: the lost update

Two threads read `balance = 100`, both add 50, both write 150. Should've been 200. One update got silently overwritten. This happens on any **read-modify-write on the same column**. This is *the* bug the database exists to prevent — but only if you use it right.

### Ways to make it safe (weakest -> strongest)

- **Atomic UPDATE** — let the DB do the math: `UPDATE ... SET balance = balance + 50`. The DB row-locks automatically for that statement. Cheapest fix.
- **Pessimistic lock** — `SELECT ... FOR UPDATE`. "Lock the row before I touch it." Other writers **wait** until I commit. JPA: `@Lock(PESSIMISTIC_WRITE)`.
- **Optimistic lock** — add a `@Version` column. Update only if version unchanged; if someone beat me, it throws -> I retry. Good for low-conflict, read-heavy stuff.
- **Isolation levels** (READ COMMITTED -> SERIALIZABLE) tune how paranoid the DB is.

**Note:** in my own app, `CreditScore` and `AuditLog` are **insert-only**, so no shared column, no lost-update. The one real race is `register()` (`existsByEmail` then `save`) — and the **unique constraint on email** is what actually saves me, not the Java `if`. -> **DB is the final arbiter.**

## So how do banks do it (the best approach)

They **don't UPDATE a balance column at all.** They use an **append-only ledger** = **double-entry bookkeeping**.

Balance is NOT a stored field — it's **derived from an immutable list of transactions**. You never overwrite, you only **INSERT** a new line. Every money move = two entries that sum to zero (debit one account, credit another).

Since it's insert-only, the lost-update race **can't happen** — nothing gets overwritten. (Same trick my insert-only tables use, on purpose, for money.)

### How do we know the latest transaction?

**Highest monotonic seq number** (a `BIGSERIAL` counter that only goes up). **Never a timestamp** — clocks drift and two rows can share a millisecond. Time is for showing, the counter is for ordering.

### How do we see the current balance if we only have past transactions?

You **fold the whole history into one number**: `SELECT SUM(amount) WHERE account = ?`. The **ledger IS the truth**; the balance is a question you compute, not a field you keep.

### Running balance checkpoint (the cache)

Summing millions of rows every time is slow, so you store a **running total** and update it each insert. This is a **cache** — mutable, not the truth. Because it's a mutable shared value, two threads could clobber it -> so you update it **under a lock** (`FOR UPDATE`), inside the **same transaction** as the ledger insert.

**Key point:** append-only didn't remove the need to lock. What it bought me: the cache is **rebuildable** from the ledger. So a concurrency bug = "recompute the cache" (**recoverable**), NOT "money gone forever" (which is what a naive `balance` column would mean).

### Does FOR UPDATE also block reading?

- Blocks other **writers** and other **`SELECT ... FOR UPDATE`** -> they wait. (This is what serializes two transfers — the second can't even read-to-decide until the first commits.)
- Does **NOT** block a plain `SELECT` -> a viewer instantly reads the last **committed** value, no waiting. (Postgres **MVCC** keeps old versions around.)

Rule: **readers don't block writers, writers don't block readers, only writer-vs-writer waits.** `FOR UPDATE` means "I'm reading this because I'm about to change it."

### If the cache is wrong (too high) can someone overdraw?

Only if you **authorize from the cache**. So don't. Rules:
- Keep the cache update **in the same transaction** as the ledger insert -> atomicity means it can't drift under normal operation.
- The **money decision reads the truth under the lock**, cache is only for display.
- Put a **`CHECK (balance >= 0)`** constraint at the DB — even a buggy app can't persist an overdraft.
- Real banks: two numbers — **ledger balance** (settled truth) vs **available balance**, and place a **hold** at swipe time so funds are reserved immediately (can't spend the same money twice).

### Other bank tricks
- **Idempotency key** — client sends a unique key per operation, stored with a unique constraint -> same request twice != double charge. (I already wrote this goal in my md.)
- **Reconciliation** — end-of-day job re-sums the ledger and catches any drift. The last line of defense.

## Why does overdraft STILL happen after all these checks?

Because **every lock/constraint is LOCAL** — it only works where everyone consults the one authority at decision time. It breaks wherever that assumption breaks:

- **Offline authorization** — the decision happens on a terminal that isn't talking to the bank. Lock never consulted.
- **Auth vs settlement gap** — check happens at one moment, money leaves later (gas pump pre-auths $1, charges $80; holds expire).
- **Only one database** — money crosses banks/networks/ACH; there's **no global lock**. Cross-system = distributed = no single arbiter.
- **Lock is a convention** — one code path that forgets `FOR UPDATE` (batch job, admin tool) leaks through.
- **Later reversals** — chargeback claws money back after it's spent -> negative balance.

### The deep reason = CAP
**Consistency needs communication.** When part of the system can't reach the authority at decision time (offline, partition), it must either **refuse service** or **approve optimistically**. Banks choose **availability** (approve now, reconcile later) because declining every offline txn would kill commerce.

So the pro mindset: stop trying to make bad states **impossible** (can't) -> make them **rare, detectable, recoverable**. That's why **reconciliation + recovery** is a permanent layer, not an afterthought.

## How can a card terminal approve without contacting the bank?

The **EMV chip is a tiny piece of the bank.** It's a secure microprocessor carrying crypto keys + the issuer's rules.

- **Offline data authentication** — the card carries a certificate signed by the bank, signed by Visa/Mastercard. Terminal has the scheme's root keys pre-loaded, so it **cryptographically verifies the card is real with no network call.**
- **Floor limit** — terminals only approve small amounts offline; big ones force online.
- **On-chip counters** — after a few offline txns or a cumulative cap, the **chip forces itself online** -> bank finally checks balance. The bank's policy travels *with the card*.

### Why can't I just spend money I don't have with many cards?
- **Can't fake/clone cards** — private key never leaves the chip; forging the cert chain is infeasible.
- **Each card is individually capped** (floor limit + counters).
- **You're fully identified** — offline = deferred, not invisible. At **settlement** it's stamped with your card/account/time; overdraft gets clawed back, fees, and it's **fraud -> prosecuted**. Self-incriminating.
- **Hot-lists + velocity checks** blacklist abused cards fast.
- **Liability shift** — if the terminal followed the rules, the loss is bounded and insured (issuer eats it); if the merchant broke the rules, merchant eats it.

So offline approval = a **controlled, insured leak, not an open door.** Same pattern as everything above: **prevent what you can (crypto, limits, counters) -> accept a bounded risk when you can't reach the authority (CAP: choose availability) -> detect & recover the rest (settlement, hot-lists, clawback).**
