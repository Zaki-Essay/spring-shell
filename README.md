# Database Interaction Shell (DIS)

**A Spring Boot-powered CLI for database management and developer productivity**

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.4-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

## 📖 Table of Contents
- [Features](#-features)
- [Installation](#-installation)
- [Usage](#-usage)
- [GraalVM and Spring Boot 3](#-graalvm-and-spring-boot-3)
- [Contributing](#-contributing)
- [License](#-license)
- [Acknowledgments](#-acknowledgments)

## 🚀 Features

### Core Features

| Feature                      | Description                                  | Status  |
|------------------------------|----------------------------------------------|---------|
| Schema Inspection            | List tables, describe structure             | ✅ Done |
| Data Generation              | Generate mock test data                     | ✅ Done |
| Direct Query Execution       | Execute raw SQL queries                     | ✅ Done |
| Connection Management        | Check DB connection status                  | ✅ Done |
| Basic CRUD Operations        | Create/Read/Update/Delete records           | ✅ Done |
| Index Management             | List and analyze indexes                    | 🚧 WIP  |
| Transaction Control          | Manual transaction management               | 🚧 WIP  |
| Data Export/Import           | CSV/JSON data transfer                      | ✅ Done |

### Bonus Features

| Feature                      | Description                                  | Status     |
|------------------------------|----------------------------------------------|------------|
| Auto-completion              | Tab-complete for tables/columns             | ✅ Done    |
| Color-coded Output           | ANSI-colored terminal output                | ✅ Done    |
| Output Formatting            | JSON/CSV/Table display formats               | 🚧 WIP    |
| Command History              | Persistent command history                  | ✅ Done    |
| Environment Switching        | Switch between dev/test/prod environments    | ⏳ Planned |
| Query Bookmarking            | Save and recall frequent queries            | ⏳ Planned |
| Data Anonymization           | GDPR-compliant data masking                  | ⏳ Planned |
| Bulk Operations              | Mass insert/update/delete                   | ⏳ Planned |
| Async Operations             | Background task execution                   | 🚧 WIP    |
| Health Checks                | Database connection health monitoring       | ✅ Done    |

## 💻 Installation

### Prerequisites
- Java 17+
- Maven 3.8+
- Supported Databases: MySQL, PostgreSQL, H2

### Quick Start
```bash
git clone https://github.com/yourusername/database-interaction-shell.git
cd database-interaction-shell
mvn clean install
java -jar target/dis-shell.jar

# Connect to your database
DSI:> configure-db --url jdbc:mysql://localhost:3306/mydb --user root --pass secret
```

## 🛠 Usage

### Common Commands
```bash
# Schema inspection
schema-tables
schema-describe --table customers

# Data manipulation
generate-test-data --table orders --count 1000
query "SELECT * FROM users WHERE active = true"

# Export/Import
export-csv --table products --file products.csv
import-json --file users.json --table users

# Performance
explain "SELECT * FROM orders WHERE total > 100"
stats-slow-queries

# Administration
cache-clear
connection-status
```

### Example Workflow
```bash
DSI:> configure-db --env production
Connected to production database (v2.4.1)

DSI:> schema-tables
[orders, customers, products, audit_log]

DSI:> generate-test-data --table customers --count 50
Generated 50 customer records with realistic test data

DSI:> export-csv --table customers --file /exports/customers_20231105.csv
Exported 50 records to CSV
```

## 🚀 GraalVM and Spring Boot 3
Spring Boot 3 did not use GraalVM (GraalVM is typically used for ahead-of-time compilation of JVM-based applications into native executables). Spring Boot continues to evolve, and GraalVM is a high-performance runtime that provides significant benefits for Java applications. Its main use cases include:

### 1. **Native Image Compilation (Ahead-of-Time - AOT Compilation)**
- Converts Java applications into native executables, reducing startup time and memory usage.
- Eliminates the need for a traditional JVM at runtime.
- Used in **Spring Boot 3** with the **Spring Native** support to create lightweight microservices.

### 2. **Improved Performance for Java Applications**
- Offers an optimized Just-In-Time (JIT) compiler, improving the execution speed of Java applications.
- Replaces the default JVM JIT compiler (C2) with a more efficient Graal JIT compiler.

### 3. **Polyglot Capabilities**
- Supports multiple languages like JavaScript, Python, Ruby, and R.
- Enables interoperability between languages in a single runtime.

### 4. **Cloud-Native & Serverless Applications**
- Reduces resource consumption, making it ideal for serverless functions (e.g., AWS Lambda, Google Cloud Functions).

### 5. **Security & Isolation**
- Since native images do not rely on a traditional JVM, they have a smaller attack surface.
- Can reduce vulnerabilities associated with dynamic class loading.

#### **Spring Boot 3 & GraalVM**
Spring Boot 3 has **first-class support** for GraalVM through **Spring Native**, allowing you to:
- Build native images using `GraalVM Native Image`.
- Improve cold-start performance for microservices.
- Optimize memory usage for cloud deployments.

Would you like to try building a native image for your Spring Boot 3 application? 🚀

## 🤝 Contributing
We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
6. Report Issues using GitHub Issues

## 📄 License
This project is licensed under the MIT License - see the LICENSE.md file for details

## 🙏 Acknowledgments
- Built with Spring Shell
- Inspired by pgcli and MyCLI
- Test data generation powered by Java Faker

