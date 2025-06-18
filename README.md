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

## How to retreive a GitHub jwt token

  Load the GitHub App private key from PEM file.
  
  PEM files contain Base64-encoded key data, wrapped in header/footer lines.
  We strip the BEGIN/END headers, then decode the Base64 body to obtain DER-encoded key bytes.
  
  I included stripping the headers in code, in case I forget to manually remove them.
  
  To get this working with Java's KeyFactory, the private key must be in PKCS#8 format.
  The private key you download from GitHub IS already PKCS#8, so **you do not need to convert it** — 
  unless you've manually generated keys elsewhere (like OpenSSL) in PKCS#1 format.
  
  If you DO need to convert a PKCS#1 private key to PKCS#8, use this command:
  openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in my_rsa_github_pem.pem -out private_key_pkcs8.pem
  
  Additionally, to generate the corresponding public key (optional for constructing some JWT libraries):
  openssl rsa -in gitbro-ai-platform.2025-06-17.private-key.pem -pubout -out gitbro-public-key.pem
  
  Now you should have a valid private key (PKCS#8) and public key (PEM).
  These keys are needed to build the JWT for GitHub App authentication, typically done using Nimbus JOSE JWT.
   
