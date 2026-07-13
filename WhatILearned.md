pom.xml is the doc containing conf details about project assembly
artifacts are executable files after the build, işte .jar'a tıklıyosun program çalışıyo

{user}/.m2/ -> the path where maven stores libraries, dependencies 
/target -> build çıktılarının konduğu path, artifacts are also here 

pom syntaxi htmle benziyo bence

JpaRepository<Entity, IDType(pktype yani)>  Database operationlarını kolaylaştırıyor,
CRUD, sayfalama pagination ve sorting sıralama için prebuild metodları var. 


# What does Maven do

Apache Maven helps programmers manage their projects and all the things they need to build their programs and Maven knows how to find all of these dependencies.

There are dependencies of dependencies and Maven manages these "Transitive" dependencies.
It also builds and tests our projects as well.

# Maven Concepts

Apache Maven is a command line tool.
Maven Wrapper is used, ./mvnw file (mvnw.cmd shell/bash script)

## There are 3 phases in Maven: 
clean: removes temporary directories and files
default: where most useful goals live
site: where documentation is generated


### clean and /target directory:
Maven puts all the processed stuff to src/target directory; reports, artifacts etc.

And ***mvnw clean***  cleans the src/target directory

## default stage/phase, lifecycle in order
compile: compiles code into bytecode
test: runs the unit test codes
package: creates a executable .jar or .war file
verify: runs checks n integration tests 

** note: they run in a sequential order, when you run verify all the above(comp, test, pack) runs too, or ** ***mvnw test*** ** also runs compile** 


## Maven Project Structure:

#### src/main: 
**/java** contains our java codes.
**/resources** any static files, any files we want it to be loaded are put here 
**/test** it usually contains a replica of the directory structure used in the production code, and it may also contain a resources folder.

#### /target:
our built project and anything processed.

## Maven Workflow example: (but not test driven)
regularly we run *mvnw clean compile* to get fast feedback

after code is ready to push, we do
*mvnw clean package* or *mvnw clean verify* 
then we *cd /target* (change directory to /target) 
*ls* to see .jar file
then *java -jar jarFileNameBlaBla.jar* runs the app. This is the same way it'll be run on prod

then we can commit n push it


## Maven Spring Boot Plugin
***./mvnw spring-boot:run*** also runs the code

# Spring Framework vs Spring Boot

**A framework is a chunk of code that sits on the top of the languages core library to solve commen problems**

Connecting to databases, exposing REST APIs are a repetitive burden to do in many projects, and this can be done with Spring framework. 

Spring framework is highly configurable, which is great but it is also a burden that takes effort.

Spring Boot is a framework built on top of Spring to help with this issue.
It's going to do Springs configurations for us, which will speed a lot.

## Spring App Layers:

There are layers in Spring that has a specific purpose which solves a specific problem.
Almost all Spring projects can be thought about terms of these layers:

### Persistence Layer:
It handles interactions with database. We must know about the term "Entity". Entities are Java objects that represent our domain and often maps to tables in a DB.  

Also domain meant the real-world business area our software is about, like a topic. Banking systems are a domain and transactions are an example entity of that domain.

We will use entities to interact with a database using one of a few different patterns:

#### Data Access Objects (DAOs): 
The DAO is a traditional pattern that isolates database access logic into a separate layer. It acts as an intermediary between your business logic and the database.

#### Repositories: 
The Repository pattern is a higher-level abstraction built on top of DAO concepts. It treats the database like an in-memory collection (more object-oriented). Spring Data JPA provides ready-made Repository implementations, so you rarely write the implementation yourself.

**Note:** Entities are stored in **/entity** folder.

Also **Spring Data JPA** is a framework that eliminates boilerplate code for data access layers by automatically generating Repository implementations.
Bizim repository folderında çok bir şey yazmamıza gerek olmuyor Data JPA hallediyor çoğunu.

#### DAO vs Repo when to use which

Use **Repository** for simple stuff like standard CRUD, simple queries etc.

Use **DAO** for complex custom logic.

###### Also don't confuse DAO with Data Transfer Object DTO

**Note:** Also DAO's and Repositories are placed inside **/repository**

So the persistence layer handles all the different interactions with consistent and persistent technologies like a database.
And then we have to expose them with well defined interfaces.

### Service Layer:
The goal of this layer is to use all the functionally exposed by Persistence Layer and then use it to meet the requirements our application is being built to.

Call must always go through the service layer. The service layer can contain very complex business logic, or can be very simple. It doesn't matter, we don't want the Presentation layer to interact with Persistence layer directly. 

We use **/service** folder.

### Controller Layer:
The data that comes from the service layers are getting exposed to user in this layer. We use APIs like REST API, GraphQL, WebSocket API etc.  

### Modularity Concept n Dependencies

Let's say we want to interact with a database using Java objects, and we use Spring.

We would have to create several beans manually and handle many managers etc.
But if we used Spring Boot we could just include dependencies, put some conf in a properties file that says how to connect to DB. And Spring Boot would handle the rest.

Spring Boot uses sensible defaults to do this.





## Dependency Injection, Inversion of Control

Instead of defining concrete classes of dependencies inside classes, we define interfaces, and we don't send the concrete classes ourselves.
We let the Spring framework do that job.
This allows us to change the dependencies without changing the codes, we just have to tell Spring to inject another dependency that meets the same interface.

This idea/process is called Inversion of Control, and we did it by making Spring do dependency injections. 

*Note: SOLIDs Open/Closed Principle is applied.*

### Concept of Beans
To let the framework supply the concrete classes to wherever we declare our interfaces, we need a way to briefly tell the framework how and where it should supply the concrete classes.

We use annotations like ***@Component, @Service, @Repository, and @Controller*** to mark classes as Spring-managed components.

The objects created and managed by Spring are called **beans**.

Then, when a class needs a dependency, Spring looks for a suitable bean and injects it automatically.

#### **@Configuration** 
It is used when the class is going to have bean declarations.
#### **@Bean**
It is used inside a Config class, just before the declaration of a Bean.

To declare a bean, you can annotate a method with the @Bean annotation. You use this method to register a bean definition within an ApplicationContext of the type specified by the method’s return type. By default, the bean name is the same as the method name (unless a different bean name generator is configured).

### Componentsta kaldık 49:39

### Components (and friends) to declare beans
When we put **@Component** before a class, it tells the Spring that, the class is a bean and its dependencies must be handled by Spring. 

**@Component** annotation says that, Spring create an object from this class, keep it in itselves container as a bean. If needed insert it to somewhere.

The component annotation on top of the implementing class works in pretty much the same way as doing *@Bean* declaration inside a configuration class.  

Example Code:
#### With **@Bean** (and @Conf)

```java
@Configuration
public class AppConfig {

    @Bean
    public RedPrinter redPrinter() {
        return new RedPrinter();
    }
}
```

It tells to Spring "Save this RedPrinter object as a Bean" 
You need a conf class to declare beans on @Bean methods. 
#### With **@Component**

```java
@Component
public class RedPrinter implements ColorPrinter {
    
}
```
Spring, finds this class during component scan and saves it as a bean.  
Extra: But we didn't use @ComponentScan, why is that?  
Well **@SpringBootApplication** also harbours (barındırmak) **@ComponentScan**   
Which scans main classes package and its lower classes  

AI Summary:
@Component marks a class as a Spring-managed component. During component scanning, Spring detects this class, creates an object from it, keeps it inside the Spring container as a bean, and injects it into other classes when needed.

In Spring Boot, we usually do not need to write @ComponentScan manually because @SpringBootApplication already includes it.

#### Friends of @Component
**@Service** or other friends of **@Component** are components that are more descriptive.


### Component Scanning

Component scanning is searching for beans and inserting those beans to the correct places.  

#### How does Spring know inserting correct beans to correct place

Spring looks at what data type the constructor/field needs.  
Then it searches the container for a bean of that data type.  

We usually use Interfaces as Constructor parameters, and we have a component bean that implements that Interface. 
Spring inserts that component as a constructor parameter.

### **@SpringBootApplication** Annotation

It consists of many other annotations like **@ComponentScan** we mentioned.
@Target(ElementType.TYPE)  
@Retention(RetentionPolicy.RUNTIME)   
@Documented  
@Inherited  
@SpringBootConfiguration  
@EnableAutoConfiguration  

### What is AutoConfiguration

Spring Boot looks at your project and automatically configures many things for you, based on what dependencies and classes it sees.  

If you re-configure autoconfigured configurations, Spring Boot backs off.  
Autoconfiguration is enabled in @SpringBootApplication, as it contains @EnableAutoConfiguration.




## Configuration
[Click to visit Conf Application Properties Page ](https://docs.spring.io/spring-boot/appendix/application-properties/index.html)

If we want to configure something, for example the port number, we visit this site and find the property we are trying to change.
We found
server.port=8181





## Database Basics

Database Driver is what you need to interact with specific database,
JDBC is how you connect to database and query with SQL
JPA is how you would query it with java objects.


### ORM (Object-Relational Mapping)

ORM is a technique that maps Java objects to database tables, so you work with objects instead of writing raw SQL.

Instead of writing:
```sql
SELECT * FROM users WHERE id = 1;
```

You write:
```java
User user = userRepository.findById(1L);
```

The ORM tool translates your objects into SQL behind the scenes.


### Hibernate

Hibernate is the most common ORM implementation used in Java. It's the library that actually does the object-to-table mapping work.

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}
```

**@Entity** tells Hibernate "this class maps to a table."  
**@Id** marks the primary key field.  
**@GeneratedValue** tells the database to auto-generate the id (like AUTO_INCREMENT).

AI Summary:
Hibernate is a concrete ORM framework. JPA is the specification (the rules/interfaces); Hibernate is one implementation of that specification, similar to how an interface and its implementing class relate.


### JPA and Spring Data JPA

**JPA (Jakarta Persistence API)** is a specification, not a real implementation. It defines interfaces like `EntityManager` for persistence, but Hibernate does the actual work underneath.

**Spring Data JPA** is a further abstraction on top of JPA. It lets you skip writing most implementation code entirely by just declaring an interface.

```java
public interface UserRepository extends JpaRepository {
    Optional findByEmail(String email);
}
```

You don't implement this interface. Spring Data JPA generates the implementation at runtime based on the method name (`findByEmail` → `SELECT * FROM users WHERE email = ?`).

AI Summary:
JPA = specification (rules). Hibernate = implementation of those rules. Spring Data JPA = a layer on top that auto-generates repository implementations from interface method names, so you barely write any query code yourself.

### Spring JDBC

Spring JDBC is a lower-level, more manual way of talking to the database compared to JPA. You write your own SQL, but Spring handles connection management, exception handling, and boilerplate for you.

```java
@Repository
public class UserJdbcDao {

    private final JdbcTemplate jdbcTemplate;

    public UserJdbcDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), id);
    }
}
```

`JdbcTemplate` is the core class; it removes the need for manual `Connection`, `Statement`, and `ResultSet` handling that plain JDBC requires.

AI Summary:
Spring JDBC gives you control by letting you write SQL directly, while Spring handles the repetitive setup/teardown code. It sits below JPA in abstraction level — more control, more manual work.

### H2 Database

H2 is a lightweight, in-memory (or file-based) database written in Java. It's commonly used for testing and local development because it needs no separate installation — it runs inside your application.

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
```

With `spring.h2.console.enabled=true`, you get a web UI (usually at `/h2-console`) to inspect the in-memory tables while the app runs.

AI Summary:
H2 is a Java-native database useful for quick testing without setting up MySQL/PostgreSQL. Data in `mem:` mode disappears when the app stops, since it lives only in memory.

### Connecting to a PostgreSQL Database
We need these dependencies:  
PostgreSQL Driver  
JDBC API/JPA


## Lombok

Lombok is a library that generates boilerplate Java code (getters, setters, constructors, `toString()`, etc.) automatically at compile time using annotations, instead of you writing it by hand.

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
}
```

Without Lombok, you'd manually write `getId()`, `setId()`, `getName()`, `setName()`, and constructors yourself.

**@Data** is a shortcut annotation that bundles `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, and a constructor for final fields.

AI Summary:
Lombok removes repetitive code (getters/setters/constructors) by generating it during compilation via annotations, reducing class file size and manual maintenance.






# Eklemem gerekenler #
User auth: JWT, refresh token  
Accounts, transactions, balance  
PostgreSQL schema düzgün  
Redis cache  
Idempotency key: aynı ödeme iki kere işlenmesin  
Rate limiting  
Swagger/OpenAPI  
Unit + integration tests  
Docker Compose ile tek komutta çalışsın  
GitHub Actions CI  
Cloud’a deploy edilmiş demo  
README: architecture diagram, API examples, trade-offs  


# .md syntaxı hakkında öğrendiğim: 

kod bloğu yazmaca:
```c  
printf("hello world");
    int main void()
    // I'm written with ``` yani alt gr , yapın birkaç kez 
```

> blockquote vermece
> > nested blockquote

### horizontal rules
---
üçerli olrk üstteki - alttaki * 
***

### tablolar

| zartList1 | zurtList1 |
| ----------|-----------|
| 1. zart   | 1. zurt   |
| 2. zart| 2.zurt|

### 2 spaces at the end to line break
#### Boşluklu:
a1  
a2  

#### 2 Boşluksuz:
a1
a2

### To give links
<https://docs.spring.io/spring-boot/appendix/application-properties/index.html>
#### Link with note
[ Click to visit Conf Application Properties Page ](https://docs.spring.io/spring-boot/appendix/application-properties/index.html)


### lokal