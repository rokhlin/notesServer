# Rule: Server-Side Coding Standards (Ktor)
## 1. General Kotlin Conventions 
- **Style Guide:** Strictly adhere to the official [JetBrains Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html). 
- **Immutability:** Use `val` by default. Only use `var` when mutability is strictly required. Prefer immutable collections. 
- **Formatting:** Enforce a 4-space indentation. All code must pass `ktlint` and `detekt` static analysis checks. 
## 2. Dependency Injection (Koin) 
- **Framework:** The server exclusively utilizes **Koin** for Dependency Injection. 
- **Implementation:** Koin must be integrated into the Ktor application lifecycle. Separate modules by domain (e.g., `userModule`, `notesModule`, `authModule`). Use constructor injection for all Services and Repositories. 
## 3. Concurrency and Coroutines 
- **Structured Concurrency:** Avoid `GlobalScope`. Coroutines handling requests must be tied to the Ktor call context. 
- **Dispatchers:** - Ensure all database queries, file reads, and external API calls are executed on `Dispatchers.IO` to prevent blocking the Ktor main event loop. 
- CPU-intensive tasks (e.g., heavy encryption, hashing, image processing) must be offloaded to `Dispatchers.Default`. 
## 4. Layered Architecture 
- **Routing/Controllers:** Responsible solely for HTTP/WebSocket request parsing, input validation, and mapping responses. Absolutely no business logic is permitted here. 
- **Services:** Contain pure business logic. Must operate on domain models and remain entirely decoupled from the HTTP framework (no Ktor `ApplicationCall` references) and database drivers. 
- **Repositories (Data Access):** The only layer permitted to execute SQL queries (via Exposed, SQLDelight, etc.) or interact directly with the database. 
## 5. Error Handling 
- **Domain Errors:** Use sealed classes or the standard `Result<T>` type to return domain errors from the Service layer to the Routing layer. Do not use generic Exceptions for expected business rule violations (e.g., "Note Not Found", "Invalid Password"). 
- **Global Exception Handling:** The Ktor application must configure a global `StatusPages` plugin to catch unhandled exceptions, log them (using the standard logging framework), and return standardized JSON error responses (e.g., HTTP 500) to the client.
## 6. Logging and Monitoring (LoggerManager)
- **Centralized Logger:** Implement a `LoggerManager` abstraction (wrapping SLF4J with Logback) to manage log levels. Logging levels must be dynamically configurable via environment variables (`.env`) or `application.conf`.
- **Annotation-Based Auto-Logging:** Implement an interceptor (e.g., using Koin AOP features or Ktor application plugins) supporting a custom `@Loggable` annotation. It must automatically log public method calls, parameters, execution time, and the final response without cluttering the business logic.
- **Inline & Structured Logging:** Expose an API for manual inline logging. To ensure logs are visually actionable in external dashboards, all server logs must be structured (e.g., JSON format) to allow seamless parsing by aggregation tools (like ELK Stack, Grafana Loki, or Datadog).
- **Error Tracking & Context:** Logs at the `ERROR` level must automatically capture the full stack trace, the current Ktor request context (e.g., Request ID, User ID), and route the payload to an error monitoring service (e.g., Sentry).