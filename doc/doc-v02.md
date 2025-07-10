# Spring Shell Database CLI Setup Guide

To test all your Spring Shell database CLI commands (connect, switch, list schemas/tables, execute SQL, etc.), here's a docker-compose.yml file that spins up the following services:

- MySQL
- PostgreSQL
- Adminer (for optional DB visualization)

You can connect to each one using your CLI.

## ✅ docker-compose.yml

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: db-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: testdb
      MYSQL_USER: user
      MYSQL_PASSWORD: pass
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  postgres:
    image: postgres:15
    container_name: db-postgres
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  adminer:
    image: adminer
    restart: always
    ports:
      - 8080:8080

volumes:
  mysql_data:
  postgres_data:
```

## ✅ How to Use It with Your CLI

Once the containers are up (run with `docker-compose up -d`):

### 🟢 MySQL CLI Command:
```bash
connect --name mysqlConn --type mysql --url jdbc:mysql://localhost:3306/testdb --username user --password pass
```

### 🟢 PostgreSQL CLI Command:
```bash
connect --name pgConn --type postgresql --url jdbc:postgresql://localhost:5432/testdb --username user --password pass
```

### 🟢 List All Connections:
```bash
connections
```

### 🟢 Switch Between Them:
```bash
switch --name pgConn
```

### 🟢 Execute SQL:
```bash
sql --query "CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(255));"
```

## ✅ H2 Memory Default

Your DatabaseConnectionManager already supports H2 by default:

```bash
connect --name h2mem --type h2 --url jdbc:h2:mem:testmemdb --username sa --password
```

---

## Extended Configuration

Parfait ! Voici une version complète du docker-compose.yml avec MySQL, PostgreSQL, SQLite (via volume mount), et Adminer pour l'interface graphique. Je t'ajoute aussi :

✅ un volume pour SQLite
✅ un script SQL pour précharger des données dans MySQL et PostgreSQL
✅ les commandes connect correspondantes à ton CLI

### 🔧 docker-compose.yml (complet avec MySQL, PostgreSQL, SQLite, Adminer)

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: db-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: testdb
      MYSQL_USER: user
      MYSQL_PASSWORD: pass
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init-scripts/mysql:/docker-entrypoint-initdb.d

  postgres:
    image: postgres:15
    container_name: db-postgres
    environment:
      POSTGRES_DB: testdb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts/postgres:/docker-entrypoint-initdb.d

  adminer:
    image: adminer
    restart: always
    ports:
      - "8080:8080"

  sqlite:
    image: nouchka/sqlite3:latest
    container_name: db-sqlite
    volumes:
      - ./data/sqlite:/root/db
    command: tail -f /dev/null # keep the container running for inspection

volumes:
  mysql_data:
  postgres_data:
```

### 📁 Arborescence des fichiers

```
.
├── docker-compose.yml
├── init-scripts
│   ├── mysql
│   │   └── init.sql
│   └── postgres
│       └── init.sql
└── data
    └── sqlite
        └── test.db   ← (tu peux le générer via DBeaver ou sqlite3 CLI)
```

### 📄 Exemple de init.sql (MySQL & PostgreSQL)

```sql
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

INSERT INTO users (name, email) VALUES
('Alice', 'alice@example.com'),
('Bob', 'bob@example.com');
```

### 🚀 Commandes CLI à exécuter

#### 🔌 Connexion MySQL
```bash
connect --name mysqlConn --type mysql --url jdbc:mysql://localhost:3306/testdb --username user --password pass
```

#### 🔌 Connexion PostgreSQL
```bash
connect --name pgConn --type postgresql --url jdbc:postgresql://localhost:5432/testdb --username user --password pass
```

#### 🔌 Connexion SQLite
```bash
connect --name sqliteConn --type sqlite --url jdbc:sqlite:/root/db/test.db
```

⚠️ Pour SQLite, tu dois copier le fichier test.db dans ./data/sqlite avant de lancer docker-compose up.

### ✅ Pour lancer le tout
```bash
docker-compose up -d
```

Tu veux aussi que je t'ajoute un script pour générer un test.db SQLite prêt à l'emploi en ligne de commande ?