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

#### *@Configuration* 
It is used when the class is going to have bean declarations.
#### *@Bean*
It is used inside a Config class, just before the declaration of a Bean.

To declare a bean, you can annotate a method with the @Bean annotation. You use this method to register a bean definition within an ApplicationContext of the type specified by the method’s return type. By default, the bean name is the same as the method name (unless a different bean name generator is configured).

### Componentsta kaldık 49:39
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