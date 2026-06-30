pom.xml is the doc containing conf details about project assembly
artifacts are executable files after the build, işte .jar'a tıklıyosun program çalışıyo

{user}/.m2/ -> the path where maven stores libraries, dependencies 
/target -> build çıktılarının konduğu path, artifacts are also here 

pom syntaxi htmle benziyo bence

JpaRepository<Entity, IDType(pktype yani)>  Database operationlarını kolaylaştırıyor,
CRUD, sayfalama pagination ve sorting sıralama için prebuild metodları var. 


# Apache

Apache Maven helps programmers manage their projects and all the things they need to build their programs and Maven knows how to find all of these dependencies.

There are dependencies of dependencies and Maven manages these "Transitive" dependencies.
It also builds and tests our projects as well.

# Maven Concepts

Apache Maven is a command line tool.
Maven Wrapper is used, ./mvnw file (mvnw.cmd shell/bash script)

### There are 3 phases in Maven: 
clean: removes temporary directories and files
default: where most useful goals live
site: where documentation is generated


## clean and /target directory:
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