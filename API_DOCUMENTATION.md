# Workforce Planning API Documentation

## Overview
REST API for managing employees, projects, and workflow approvals.

**Base URL:** `http://localhost:8081`

---

## Endpoints

### Employee Management

#### 1. Get All Employees
```http
GET /employees
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@test.com",
    "skills": ["Java", "PostgreSQL", "Spring Boot"],
    "department": "IT",
    "available": true,
    "createdAt": "2025-12-28T13:01:24.242",
    "updatedAt": "2025-12-28T13:01:24.242"
  }
]
```

---

#### 2. Create Employee
```http
POST /employees
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Jane Smith",
  "email": "jane@company.com",
  "skills": ["React", "JavaScript"],
  "department": "IT",
  "available": true
}
```

**Response:** `201 Created`
```json
{
  "id": 2,
  "name": "Jane Smith",
  "email": "jane@company.com",
  ...
}
```

---

### Project Management

#### 3. Get All Projects
```http
GET /projects
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Website Redesign",
    "description": "Redesign company website",
    "requiredSkills": ["React", "JavaScript"],
    "status": "PENDING"
  }
]
```

---

#### 4. Create Project
```http
POST /projects
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Mobile App Development",
  "description": "Create mobile app",
  "requiredSkills": ["Java", "Spring Boot"],
  "status": "PENDING"
}
```

---

### Workflow Management

#### 5. Start Workflow
```http
POST /workflow/start/{projectId}
```

**Parameters:**
- `projectId` (path) - ID of the project

**Example:** `POST /workflow/start/1`

**Response:** `200 OK`
```
"Workflow started for project 1"
```

---

#### 6. Get Active Workflow Tasks
```http
GET /workflow/tasks
```

**Response:** `200 OK`
```json
[
  {
    "taskId": "12345",
    "name": "Review Staffing Request",
    "assignee": null,
    "processInstanceId": "67890"
  }
]
```

---

#### 7. Complete/Approve Task
```http
POST /workflow/tasks/{taskId}/complete?approved={boolean}
```

**Parameters:**
- `taskId` (path) - ID of the task
- `approved` (query) - true or false

**Example:** `POST /workflow/tasks/12345/complete?approved=true`

**Response:** `200 OK`
```
"Task 12345 completed. Approved = true"
```

---

## Complete Workflow Example

### Scenario: Request and Approve Staffing
```bash
# Step 1: Create a project
POST /projects
Body: {
  "name": "New Website",
  "requiredSkills": ["Java"],
  "status": "PENDING"
}
# Returns: Project with id=1

# Step 2: Start workflow for project
POST /workflow/start/1
# Returns: "Workflow started for project 1"

# Step 3: Get active tasks
GET /workflow/tasks
# Returns: Task with taskId="abc123"

# Step 4: Approve the task
POST /workflow/tasks/abc123/complete?approved=true
# Returns: "Task completed. Approved = true"

# Step 5: Verify project status updated
GET /projects
# Project status should be updated
```

---

## Testing

Use the provided Postman collection: `Workforce_Planning_API.postman_collection.json`

1. Import collection into Postman
2. Test each endpoint
3. Follow the workflow example above

---

## Sample Data - Only if you havent pull the code from github and have created no data before.

The application includes a DataInitializer that loads sample data on first startup:
- 3 Employees (John Doe, Jane Smith, Bob Wilson)
- 2 Projects (Website Redesign, Mobile App Development)

---

## Tech Stack

- **Backend:** Spring Boot 3.2.0
- **Database:** PostgreSQL 15
- **Workflow Engine:** Flowable 7.2.0
- **API:** REST
- **Port:** 8081