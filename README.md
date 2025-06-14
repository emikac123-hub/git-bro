# Read Me First
The following was discovered as part of building this project:

* The original package name 'com.erik.git-bro' is invalid and this project uses 'com.erik.git_bro' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.0/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.0/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.0/reference/web/servlet.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.5.0/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Security](https://docs.spring.io/spring-boot/3.5.0/reference/web/spring-security.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/3.5.0/reference/actuator/index.html)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.5.0/reference/using/devtools.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.


# Tech Stack

* Minikube
* Docker 
* Java Spring Boot
* Java 21
* AI Model - Use CodeBERT (via Hugging Face’s Java-compatible libraries) for code analysis, as it’s lightweight and pre-trained for code tasks. Alternatively, use Azure AI Text Analytics for simpler integration, leveraging your Azure experience.
* Database - H2 for testing, with JPA for persistence.

* Concurrency - Use Virtual Threads for handling multiple PR requests efficiently.

* Security - Basic API key authentication (Spring Security) to prevent unauthorized access.


# MVP Definition

Receive Code Diffs: Accept PR diff data via a REST API (e.g., from GitHub webhooks).

Analyze Code: Use an AI model to detect issues (e.g., style violations, potential bugs).

Generate Feedback: Return review comments as JSON (later used by the GitHub Action).

Store Metadata: Save review history in a database for tracking (optional for MVP).

