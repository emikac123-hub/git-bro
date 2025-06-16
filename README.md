# Git-Bro: GitHub PR AI Code Reviewer

**Git-Bro** is a Spring Boot application that performs automated code review on GitHub pull requests using AI models like CodeBERT or ChatGPT. It ingests git diffs, analyzes them chunk-by-chunk, and returns meaningful feedback about potential bugs, style violations, or other code issues.

---

## Features

-  Git diff ingestion and chunking  
-  AI-powered code analysis using CodeBERT (or plug in ChatGPT)  
-  Persistence via Spring Data JPA (H2 in-memory DB)  
-  Asynchronous processing using virtual threads  
-  Feedback is saved to disk for auditing  
-  Pluggable AI client architecture for CodeBERT, OpenAI, or others  

---

## 📁 Project Structure

src
├── main
│ ├── java/com/erik/git_bro
│ │ ├── client # AI clients (e.g., CodeBERT)
│ │ ├── config # Async executor config
│ │ ├── controller # REST API
│ │ ├── model # JPA entities
│ │ ├── repository # Spring Data Repositories
│ │ └── service # Core logic (diff parsing, feedback generation)
│ └── resources
│ ├── application.yaml
│ └── db/changelog # Liquibase changelogs


---

## 🔧 Configuration

### `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:codereview
  jpa:
    show-sql: true
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml

huggingface:
  api:
    token: YOUR_HUGGINGFACE_TOKEN

app:
  feedback:
    file-path: /path/to/code-review-feedback.txt
```