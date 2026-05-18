# Real Estate Server — MongoDB Branch

Java 21 + Javalin REST API backed by MongoDB (migrated from PostgreSQL).

## Prerequisites

- Java 21
- Maven
- Docker Desktop (running)

## Setup

### 1. Start MongoDB

```bash
docker compose up -d
```

This starts a Mongo 7 container on port 27017.

### 2. Import CSV data

```bash
docker exec realestate-mongo mongoimport \
  --db realestate --collection sales \
  --type csv --headerline --file /csv/pp-complete.csv
```

This loads ~4.8M sale records. Takes a couple minutes.

### 3. Create indexes

```bash
docker exec realestate-mongo mongosh realestate --eval '
  db.sales.createIndex({ post_code: 1 });
  db.sales.createIndex({ property_id: 1 });
  db.listings.createIndex({ property_id: 1, listing_date: 1 });
  db.users.createIndex({ email: 1 }, { unique: true });
  db.user_preferences.createIndex({ user_id: 1 });
  db.access_counts.createIndex({ endpoint: 1, resource_id: 1 }, { unique: true });
'
```

### 4. Seed listings and users

```bash
docker exec realestate-mongo mongosh realestate --eval '
  db.listings.insertMany([
    { property_id: "1", listing_date: "2024-01-15", listing_price: 250000, listing_status: "active" },
    { property_id: "2", listing_date: "2024-02-20", listing_price: 475000, listing_status: "active" },
    { property_id: "3", listing_date: "2024-03-10", listing_price: 320000, listing_status: "sold" }
  ]);
  db.users.insertMany([
    { name: "Alice", email: "alice@example.com" },
    { name: "Bob", email: "bob@example.com" }
  ]);
  db.user_preferences.insertMany([
    { user_id: db.users.findOne({ email: "alice@example.com" })._id.toString(), post_code: "SW1A 1AA" },
    { user_id: db.users.findOne({ email: "bob@example.com" })._id.toString(), post_code: "EC1A 1BB" }
  ]);
'
```

### 5. Build and run

```bash
cd REServer
mvn clean package -q
java -jar target/REServer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Server starts on http://localhost:7070. Swagger UI at http://localhost:7070/swagger.

## What changed from main

- **Database**: PostgreSQL → MongoDB 7
- **Driver**: `postgresql` JDBC → `mongodb-driver-sync`
- **docker-compose.yml**: Postgres container → Mongo container
- **DatabaseConfig.java**: JDBC connection → MongoClient
- **All DAOs**: Rewritten to use MongoDB `MongoCollection` API
- **BaseDAO.java**: Deleted (not needed with MongoDB)
- **Controllers, models, routes**: Unchanged — same API behavior
