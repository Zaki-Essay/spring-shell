# Database Management System - Project Structure

## 📁 Root Directory Structure

```
database-management-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── company/
│   │   │           └── database/
│   │   │               ├── cli/                    # CLI Components
│   │   │               ├── config/                 # Configuration Classes
│   │   │               ├── connection/             # Connection Management
│   │   │               ├── exception/              # Custom Exceptions
│   │   │               ├── model/                  # Data Models
│   │   │               ├── schema/                 # Schema Management
│   │   │               ├── constant/               # Constants
│   │   │               ├── util/                   # Utility Classes
│   │   │               └── DatabaseApplication.java # Main Application
│   │   └── resources/
│   │       ├── application.yml                     # Configuration
│   │       ├── application-dev.yml                 # Development Config
│   │       ├── application-prod.yml                # Production Config
│   │       ├── banner.txt                          # Spring Boot Banner
│   │       ├── logback-spring.xml                  # Logging Configuration
│   │       └── sql/                                # SQL Scripts
│   │           ├── schema/                         # DDL Scripts
│   │           └── data/                           # DML Scripts
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── company/
│       │           └── database/
│       │               ├── cli/                    # CLI Tests
│       │               ├── connection/             # Connection Tests
│       │               ├── schema/                 # Schema Tests
│       │               └── integration/            # Integration Tests
│       └── resources/
│           ├── application-test.yml                # Test Configuration
│           └── test-data/                          # Test Data
├── docs/                                           # Documentation
│   ├── api/                                        # API Documentation
│   ├── user-guide/                                 # User Guide
│   └── deployment/                                 # Deployment Guides
├── scripts/                                        # Build & Deployment Scripts
│   ├── build.sh                                    # Build Script
│   ├── deploy.sh                                   # Deployment Script
│   └── database/                                   # Database Scripts
├── docker/                                         # Docker Configuration
│   ├── Dockerfile                                  # Application Dockerfile
│   └── docker-compose.yml                         # Multi-container Setup
├── .gitignore                                      # Git Ignore File
├── README.md                                       # Project README
├── pom.xml                                         # Maven Configuration
└── CHANGELOG.md                                    # Change Log
```

## 📦 Package Structure Detail

### 1. **CLI Package** (`com.company.database.cli`)
```
cli/
├── EnhancedDatabaseCLI.java           # Main CLI Component
├── command/
│   ├── ConnectionCommand.java         # Connection Commands
│   ├── SchemaCommand.java            # Schema Commands
│   ├── SqlCommand.java               # SQL Commands
│   └── HealthCommand.java            # Health Commands
├── formatter/
│   ├── ResponseFormatter.java        # Response Formatting
│   ├── TableFormatter.java           # Table Display
│   └── ColorFormatter.java           # Color Support
└── validator/
    ├── InputValidator.java           # Input Validation
    └── SqlValidator.java             # SQL Validation
```

### 2. **Configuration Package** (`com.company.database.config`)
```
config/
├── DatabaseManagementConfig.java     # Main Configuration
├── DatabaseConfig.java               # Database Properties
├── MetricsConfig.java                # Metrics Configuration
├── SecurityConfig.java               # Security Configuration
└── properties/
    ├── ConnectionProperties.java     # Connection Properties
    └── PoolProperties.java           # Connection Pool Properties
```

### 3. **Connection Package** (`com.company.database.connection`)
```
connection/
├── DatabaseConnectionManager.java    # Connection Manager
├── factory/
│   ├── DataSourceFactory.java       # DataSource Factory
│   └── ConnectionFactory.java       # Connection Factory
├── pool/
│   ├── ConnectionPoolManager.java   # Pool Management
│   └── PoolMetrics.java             # Pool Metrics
└── health/
    ├── ConnectionHealthChecker.java  # Health Checker
    └── HealthIndicator.java          # Health Indicator
```

### 4. **Schema Package** (`com.company.database.schema`)
```
schema/
├── SchemaManager.java                # Schema Manager
├── service/
│   ├── TableService.java            # Table Operations
│   ├── ColumnService.java           # Column Operations
│   └── IndexService.java            # Index Operations
├── builder/
│   ├── SqlBuilder.java              # SQL Builder
│   └── DDLBuilder.java              # DDL Builder
└── analyzer/
    ├── SchemaAnalyzer.java          # Schema Analysis
    └── TableAnalyzer.java           # Table Analysis
```

### 5. **Model Package** (`com.company.database.model`)
```
model/
├── dto/                              # Data Transfer Objects
│   ├── ConnectionDto.java           # Connection DTO
│   ├── TableDto.java                # Table DTO
│   └── ColumnDto.java               # Column DTO
├── entity/                           # Entity Classes
│   ├── DatabaseInfo.java           # Database Info
│   ├── TableInfo.java              # Table Info
│   ├── ColumnInfo.java             # Column Info
│   └── ColumnDefinition.java       # Column Definition
└── enums/                           # Enumerations
    ├── DatabaseType.java           # Database Types
    ├── ColumnType.java             # Column Types
    └── ConnectionStatus.java       # Connection Status
```

### 6. **Exception Package** (`com.company.database.exception`)
```
exception/
├── DatabaseException.java           # Base Exception
├── ConnectionException.java         # Connection Exception
├── SchemaException.java            # Schema Exception
├── SqlExecutionException.java      # SQL Exception
└── handler/
    ├── GlobalExceptionHandler.java  # Global Handler
    └── CliExceptionHandler.java     # CLI Handler
```

### 7. **Constant Package** (`com.company.database.constant`)
```
constant/
├── DatabaseConstants.java          # Database Constants
├── SqlConstants.java              # SQL Constants
├── MessageConstants.java          # Message Constants
└── ConfigConstants.java           # Configuration Constants
```

### 8. **Utility Package** (`com.company.database.util`)
```
util/
├── StringUtils.java                # String Utilities
├── SqlUtils.java                  # SQL Utilities
├── ValidationUtils.java           # Validation Utilities
├── DateUtils.java                 # Date Utilities
└── FileUtils.java                 # File Utilities
```

## 🗂️ Configuration Files

### **application.yml**
```yaml
# Main application configuration
spring:
  application:
    name: database-management-system
  profiles:
    active: dev

database:
  max-pool-size: 10
  min-idle: 2
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
  enable-metrics: true
  health-check-query: "SELECT 1"
  default-connection:
    url: "jdbc:h2:mem:testdb"
    username: "sa"
    password: ""
    type: "h2"

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.company.database: DEBUG
    com.zaxxer.hikari: INFO
    org.springframework.shell: INFO
```

### **logback-spring.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/database-management.log</file>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/database-management.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
        </appender>
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

## 📋 Maven Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.company</groupId>
    <artifactId>database-management-system</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>Database Management System</name>
    <description>Enhanced CLI Database Management System</description>
    
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.0</spring-boot.version>
        <spring-shell.version>3.2.0</spring-shell.version>
        <hikari.version>5.0.1</hikari.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.shell</groupId>
            <artifactId>spring-shell-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Database Dependencies -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>
        
        <!-- Database Drivers -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
        </dependency>
        
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        
        <!-- Utility Dependencies -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## 🚀 Best Practices Applied

### **1. Separation of Concerns**
- CLI commands separated from business logic
- Configuration isolated in dedicated classes
- Exception handling centralized

### **2. Modularity**
- Each package has a specific responsibility
- Loose coupling between components
- Easy to test and maintain

### **3. Configuration Management**
- Profile-based configuration
- Externalized properties
- Environment-specific settings

### **4. Error Handling**
- Hierarchical exception structure
- Meaningful error messages
- Proper logging and monitoring

### **5. Testing Strategy**
- Unit tests for each component
- Integration tests for workflows
- Test data management

### **6. Documentation**
- Comprehensive README
- API documentation
- User guides and tutorials

This structure provides a solid foundation for a production-ready database management system with proper separation of concerns, maintainability, and scalability.