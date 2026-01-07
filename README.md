# AgileProject
# Team Setup Instructions

## Prerequisites
- Java 17 or higher
- Docker Desktop installed
- IntelliJ IDEA or any IDE

## Setup Steps

### 1. Clone Repository
```bash
git clone https://github.com/Mprabhu26/AgileProject.git
cd AgileProject
```

### 2. Start PostgreSQL Database
```bash
docker run --name workforce-postgres \
  -e POSTGRES_DB=workforcedb \
  -e POSTGRES_USER=workforce \
  -e POSTGRES_PASSWORD=workforce123 \
  -p 5432:5432 \
  -d postgres:15
```

### 3. Verify Docker is Running
```bash
docker ps
# Should show workforce-postgres container
```

### 4. Open Project in IntelliJ
- File → Open → Select AgileProject folder
- Wait for Maven to download dependencies

### 5. Run Application
- Right-click `WorkforcePlanningApplication.java`
- Click "Run"
- Wait for "Started WorkforcePlanningApplication" message

### 6. Verify Database Tables
```bash
docker exec -it workforce-postgres psql -U workforce -d workforcedb
\dt
\q
```

You should see: employees, projects, assignments tables (EMPTY)

### 7. Each Developer Works Independently wrt database just for now.
- Your database is LOCAL to your machine
- Your test data won't affect others
- Tables are auto-created from code
- BPMN workflow auto-deployed

## Notes
- Database tables will be EMPTY on first run (this is normal!)
- We'll add shared test data in Week 7
- Each person has their own Docker PostgreSQL
- Don't worry about sharing database content for NOW