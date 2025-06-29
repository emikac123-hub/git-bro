# Git-Bro: GitHub PR AI Code Reviewer

Git-Bro is a Spring Boot application designed to automate code reviews on GitHub Pull Requests using advanced AI models like ChatGPT and Gemini. It processes git diffs, analyzes code changes chunk-by-chunk, and provides actionable feedback on potential bugs, style violations, and other code quality issues.

## Table of Contents
- [Features](#features)
- [AI Model Selection](#ai-model-selection)
- [Data Model: Review vs. ReviewIteration](#data-model-review-vs-reviewiteration)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Development Setup](#local-development-setup)
  - [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [GitHub Authentication](#github-authentication)
- [Contributing](#contributing)
- [License](#license)

## Features

-   **Git Diff Ingestion**: Efficiently ingests and processes git diffs from pull requests.
-   **AI-Powered Code Analysis**: Utilizes pluggable AI clients (e.g., ChatGPT, Gemini) for in-depth code analysis.
-   **Persistence**: Stores review feedback using Spring Data JPA (configurable for various databases).
-   **Asynchronous Processing**: Leverages virtual threads for non-blocking, asynchronous operations.
-   **Auditable Feedback**: Saves all generated feedback for historical tracking and auditing.
-   **Pluggable AI Architecture**: Easily integrate new AI models beyond the currently supported ones.

## AI Model Selection

This application supports dynamic selection of the AI model used for code analysis. The desired model is specified as an input parameter in the POST request to the `/api/review/analyze-file-by-line` endpoint.

Currently supported models:
-   `chatgpt`: Utilizes the OpenAI ChatGPT API for code analysis.
-   `gemini`: Utilizes the Google Gemini API for code analysis.

To specify the model, include a `modelName` parameter in your request with one of the supported values.

## Data Model: Review vs. ReviewIteration

To accurately model the code review process, this application distinguishes between `ReviewIteration` and `Review`:

-   **`Review`**: Represents an individual AI-generated comment or issue found during a `ReviewIteration`. Each `Review` is associated with a specific `ReviewIteration` and contains details such as the file name, the line number, the comment itself, and a derived severity score. This `derivedSeverityScore` reflects the severity of that *specific* individual issue.
-   **`ReviewIteration`**: Represents a single, complete run of the AI code review process for a specific pull request. Each `ReviewIteration` is uniquely identified by the pull request ID and the commit SHA it was run against. It acts as a container for all individual review comments generated during that particular analysis. The `derivedSeverityScore` for a `ReviewIteration` is the *highest* severity score among all the `Review` objects associated with that iteration, effectively representing the most critical issue found in that review run.

Essentially, one `ReviewIteration` can contain multiple `Review` objects, providing a historical record of all AI feedback for each analysis run on a pull request.

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:
-   Java Development Kit (JDK) 17 or newer
-   Apache Maven
-   Docker (for local PostgreSQL database)
-   Ngrok (optional, for exposing local server to GitHub webhooks)

### Local Development Setup

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/your-username/git-bro.git
    cd git-bro
    ```

2.  **Database Setup (PostgreSQL)**:
    For local development, a non-ephemeral PostgreSQL database is used. Start the database using Docker Compose:
    ```bash
    docker compose -f docker/docker-compose-test-db.yaml up -d
    ```

3.  **Apply Liquibase Migrations**:
    Ensure your Docker database is running, then apply the database migrations:
    ```bash
    mvn liquibase:update
    ```
    This command will apply all pending changesets defined in `src/main/resources/db/changelog/db.changelog-master.yaml`.

4.  **Configure `application.yaml`**:
    Update `src/main/resources/application.yaml` with your specific configurations, including API tokens for AI models (e.g., Hugging Face, OpenAI, Google Gemini).

### Running the Application

To run the Spring Boot application:
```bash
mvn spring-boot:run
```
The application will typically start on `http://localhost:8080`.

**Ngrok (for GitHub Webhooks)**:
If you need to expose your local server to GitHub webhooks for testing, you can use Ngrok:
```bash
ngrok http 8080
```
Copy the Ngrok URL and use it in your GitHub webhook configurations.

## Project Structure

```yaml
src
├── main
│   ├── java/com/erik/git_bro
│   │   ├── client      # AI clients (e.g., Gemini, ChatGPT)
│   │   ├── config      # Application configuration (e.g., Async executor)
│   │   ├── controller  # REST API endpoints
│   │   ├── model       # JPA entities for data model
│   │   ├── repository  # Spring Data JPA Repositories
│   │   └── service     # Core business logic (diff parsing, feedback generation)
│   └── resources
│       ├── application.yaml      # Main application configuration
│       └── db/changelog          # Liquibase changelogs for database migrations
├── test
│   └── java/com/erik/git_bro     # Unit and integration tests
```

## Configuration

Key configuration properties are managed in `src/main/resources/application.yaml`.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:codereview # Example for H2, update for PostgreSQL
  jpa:
    show-sql: true
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml

huggingface:
  api:
    token: YOUR_HUGGINGFACE_TOKEN # Replace with your actual token

app:
  feedback:
    file-path: /path/to/code-review-feedback.txt # Path for feedback storage
```

## GitHub Authentication

This application interacts with the GitHub API, which requires authentication using JSON Web Tokens (JWTs) generated from a GitHub App's private key.

**Retrieving a GitHub App Private Key**:
You will need to obtain a `.pem` file containing the private key from your GitHub App settings. This key is essential for programmatically generating JWTs to authenticate API requests.

**Key Format**:
The private key downloaded from GitHub is typically in PKCS#8 format, which is compatible with Java's `KeyFactory`. If you have a key in PKCS#1 format (e.g., from OpenSSL), you may need to convert it to PKCS#8 using `openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in my_rsa_github_pem.pem -out private_key_pkcs8.pem`.

The application handles the loading and processing of this PEM file to create the necessary JWTs for GitHub API interactions.

## Contributing

Contributions are welcome! Please see the `CONTRIBUTING.md` file (to be created) for guidelines on how to contribute to this project.

## License

This project is licensed under the MIT License - see the `LICENSE` file (to be created) for details.