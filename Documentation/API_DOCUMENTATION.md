# 📚 Workforce Planning System - API Documentation

## 📋 Table of Contents
- [Overview](#overview)
- [Getting Started](#getting-started)
- [Authentication](#authentication)
- [Employee Management API](#employee-management-api)
- [Project Management API](#project-management-api)
- [Application Management API](#application-management-api)
- [Assignment Management API](#assignment-management-api)
- [Workflow Management API](#workflow-management-api)
- [UI Endpoints Reference](#ui-endpoints-reference)
- [Error Codes](#error-codes)
- [Testing Guide](#testing-guide)

---

## 🎯 Overview

The Workforce Planning System is a comprehensive REST API built with Spring Boot for managing employees, projects, job applications, and workflow approvals.

**Base URL:** `http://localhost:8081`

**Technology Stack:**
- **Framework:** Spring Boot 3.2.0
- **Database:** PostgreSQL 15
- **Workflow Engine:** Flowable 7.2.0
- **Security:** Spring Security with BCrypt
- **Port:** 8081

---

## 🚀 Getting Started

### Prerequisites
1. **Java 17+** installed
2. **PostgreSQL 15+** installed and running
3. **Maven** for building the project
4. **Postman** or **Insomnia** for testing APIs (recommended)

### Database Setup
```sql
-- Create database
CREATE DATABASE workforce_db;

-- Connect to database
\c workforce_db

-- Tables will be auto-created by Hibernate on first run
```

### Environment Configuration

**Option 1: Using application.properties**
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/workforce_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
server.port=8081
```

**Option 2: Using Environment Variables**
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=workforce_db
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

### Running the Application
```bash
# Clone the repository
git clone [your-repo-url]
cd workforce-planning

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Application starts at http://localhost:8081
```

### Sample Data
On first startup, the application automatically creates:
- **3 Sample Employees:** John Doe, Jane Smith, Bob Wilson
- **4 User Accounts:** pm (Project Manager), depthead (Department Head), planner (Resource Planner), emp1 (Employee)
- **2 Sample Projects:** Website Redesign, Mobile App Development

---

## 🔐 Authentication

### Login Credentials

The system has 4 pre-configured users:

| Username | Password | Role | Access |
|----------|----------|------|--------|
| `pm` | `pm123` | PROJECT_MANAGER | Create/manage projects |
| `depthead` | `head123` | DEPARTMENT_HEAD | Approve projects |
| `planner` | `planner123` | RESOURCE_PLANNER | Assign employees |
| `emp1` | `emp123` | EMPLOYEE | Browse/apply to projects |

### How Authentication Works

**Web UI Login:**
```
URL: http://localhost:8081/login
Method: POST
Content-Type: application/x-www-form-urlencoded

Parameters:
- username: [your_username]
- password: [your_password]
- _csrf: [auto-generated token]
```

**After successful login:**
- Session cookie is created
- User is redirected to their role-specific dashboard
- All subsequent requests include the session cookie automatically

**API Access:**
- For testing REST APIs, you need to login first through the web UI
- OR use Postman with session management
- OR disable security for testing (not recommended for production)

---

## 👥 Employee Management API

### 1. Get All Employees

**Endpoint:** `GET /employees`

**Description:** Retrieves a list of all employees in the system

**Authentication:** Required

**Request:**
```http
GET http://localhost:8081/employees
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@company.com",
    "username": "emp1",
    "skills": ["Java", "Spring Boot", "PostgreSQL"],
    "department": "Engineering",
    "role": "Senior Developer",
    "available": true,
    "createdAt": "2025-11-28T10:30:00",
    "updatedAt": "2025-11-28T10:30:00"
  },
  {
    "id": 2,
    "name": "Jane Smith",
    "email": "jane@company.com",
    "username": "emp2",
    "skills": ["React", "JavaScript", "CSS"],
    "department": "Engineering",
    "role": "Frontend Developer",
    "available": false,
    "createdAt": "2025-11-28T10:31:00",
    "updatedAt": "2025-12-05T14:20:00"
  }
]
```

---

### 2. Get Employee by ID

**Endpoint:** `GET /employees/{id}`

**Description:** Retrieves details of a specific employee

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Employee ID

**Request:**
```http
GET http://localhost:8081/employees/1
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@company.com",
  "username": "emp1",
  "skills": ["Java", "Spring Boot", "PostgreSQL"],
  "department": "Engineering",
  "role": "Senior Developer",
  "available": true,
  "createdAt": "2025-11-28T10:30:00",
  "updatedAt": "2025-11-28T10:30:00"
}
```

**Error Response:** `404 Not Found`
```json
{
  "timestamp": "2025-12-01T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id: 999"
}
```

---

### 3. Create Employee (Single)

**Endpoint:** `POST /employees`

**Description:** Creates a new employee in the system

**Authentication:** Required (HR_MANAGER or ADMIN role)

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Alice Johnson",
  "email": "alice@company.com",
  "username": "alice.j",
  "password": "password123",
  "skills": ["Python", "Django", "AWS"],
  "department": "Engineering",
  "role": "Backend Developer",
  "available": true
}
```

**Response:** `201 Created`
```json
{
  "id": 3,
  "name": "Alice Johnson",
  "email": "alice@company.com",
  "username": "alice.j",
  "skills": ["Python", "Django", "AWS"],
  "department": "Engineering",
  "role": "Backend Developer",
  "available": true,
  "createdAt": "2025-12-01T11:45:00",
  "updatedAt": "2025-12-01T11:45:00"
}
```

**Validation Errors:** `400 Bad Request`
```json
{
  "timestamp": "2025-12-01T11:45:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already exists",
  "errors": [
    {
      "field": "email",
      "message": "Email alice@company.com is already registered"
    }
  ]
}
```

---

### 4. Create Multiple Employees (Batch)

**Endpoint:** `POST /employees/batch`

**Description:** Creates multiple employees at once

**Authentication:** Required (HR_MANAGER or ADMIN role)

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
[
  {
    "name": "Bob Wilson",
    "email": "bob@company.com",
    "username": "bob.w",
    "password": "password123",
    "skills": ["Java", "Spring"],
    "department": "Engineering",
    "role": "Developer",
    "available": true
  },
  {
    "name": "Carol White",
    "email": "carol@company.com",
    "username": "carol.w",
    "password": "password123",
    "skills": ["React", "Node.js"],
    "department": "Engineering",
    "role": "Full Stack Developer",
    "available": true
  }
]
```

**Response:** `201 Created`
```json
[
  {
    "id": 4,
    "name": "Bob Wilson",
    "email": "bob@company.com",
    ...
  },
  {
    "id": 5,
    "name": "Carol White",
    "email": "carol@company.com",
    ...
  }
]
```

---

### 5. Search Employees by Skills

**Endpoint:** `GET /employees/search?skills={skillName}`

**Description:** Finds employees with specific skills

**Authentication:** Required

**Query Parameters:**
- `skills` (String) - Skill to search for (case-insensitive)

**Request:**
```http
GET http://localhost:8081/employees/search?skills=Java
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "skills": ["Java", "Spring Boot", "PostgreSQL"],
    "available": true
  },
  {
    "id": 4,
    "name": "Bob Wilson",
    "skills": ["Java", "Spring"],
    "available": true
  }
]
```

---

### 6. Get Available Employees

**Endpoint:** `GET /employees/available`

**Description:** Retrieves only employees who are available for assignment

**Authentication:** Required

**Request:**
```http
GET http://localhost:8081/employees/available
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@company.com",
    "skills": ["Java", "Spring Boot"],
    "available": true
  }
]
```

---

## 📁 Project Management API

### 1. Get All Projects

**Endpoint:** `GET /projects`

**Description:** Retrieves all projects (filtered by user role)

**Authentication:** Required

**Request:**
```http
GET http://localhost:8081/projects
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Website Redesign",
    "description": "Redesign company website with modern UI",
    "startDate": "2026-01-15",
    "endDate": "2026-03-30",
    "budget": 50000.00,
    "totalEmployeesRequired": 3,
    "status": "APPROVED",
    "published": true,
    "visibleToAll": true,
    "createdBy": "pm",
    "createdAt": "2025-11-28T09:00:00",
    "skillRequirements": [
      {
        "id": 1,
        "skill": "React",
        "requiredCount": 2
      },
      {
        "id": 2,
        "skill": "Node.js",
        "requiredCount": 1
      }
    ]
  }
]
```

---

### 2. Get Project by ID

**Endpoint:** `GET /projects/{id}`

**Description:** Retrieves details of a specific project

**Authentication:** Required

**Path Parameters:**
- `id` (Long) - Project ID

**Request:**
```http
GET http://localhost:8081/projects/1
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Website Redesign",
  "description": "Redesign company website with modern UI",
  "startDate": "2026-01-15",
  "endDate": "2026-03-30",
  "budget": 50000.00,
  "totalEmployeesRequired": 3,
  "status": "APPROVED",
  "published": true,
  "visibleToAll": true,
  "createdBy": "pm",
  "createdAt": "2025-11-28T09:00:00",
  "skillRequirements": [
    {
      "id": 1,
      "skill": "React",
      "requiredCount": 2
    }
  ]
}
```

---

### 3. Create Project

**Endpoint:** `POST /projects`

**Description:** Creates a new project

**Authentication:** Required (PROJECT_MANAGER role)

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Mobile App Development",
  "description": "Build iOS and Android app for customer portal",
  "startDate": "2026-02-01",
  "endDate": "2026-06-30",
  "budget": 80000.00,
  "totalEmployeesRequired": 4,
  "skillRequirements": [
    {
      "skill": "React Native",
      "requiredCount": 2
    },
    {
      "skill": "Node.js",
      "requiredCount": 1
    },
    {
      "skill": "AWS",
      "requiredCount": 1
    }
  ]
}
```

**Response:** `201 Created`
```json
{
  "id": 2,
  "name": "Mobile App Development",
  "description": "Build iOS and Android app for customer portal",
  "startDate": "2026-02-01",
  "endDate": "2026-06-30",
  "budget": 80000.00,
  "totalEmployeesRequired": 4,
  "status": "PENDING",
  "published": false,
  "visibleToAll": false,
  "createdBy": "pm",
  "createdAt": "2025-12-01T10:30:00",
  "skillRequirements": [
    {
      "id": 3,
      "skill": "React Native",
      "requiredCount": 2
    },
    {
      "id": 4,
      "skill": "Node.js",
      "requiredCount": 1
    },
    {
      "id": 5,
      "skill": "AWS",
      "requiredCount": 1
    }
  ]
}
```

---

### 4. Update Project

**Endpoint:** `PUT /projects/{id}`

**Description:** Updates an existing project

**Authentication:** Required (PROJECT_MANAGER - owner only)

**Path Parameters:**
- `id` (Long) - Project ID

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Mobile App Development - Updated",
  "description": "Build cross-platform mobile app",
  "startDate": "2026-02-01",
  "endDate": "2026-07-31",
  "budget": 90000.00,
  "totalEmployeesRequired": 5,
  "skillRequirements": [
    {
      "skill": "React Native",
      "requiredCount": 3
    },
    {
      "skill": "Node.js",
      "requiredCount": 2
    }
  ]
}
```

**Response:** `200 OK`
```json
{
  "id": 2,
  "name": "Mobile App Development - Updated",
  "description": "Build cross-platform mobile app",
  ...
}
```

---

### 5. Delete Project

**Endpoint:** `DELETE /projects/{id}`

**Description:** Deletes a project (only if no assignments exist)

**Authentication:** Required (PROJECT_MANAGER - owner only)

**Path Parameters:**
- `id` (Long) - Project ID

**Request:**
```http
DELETE http://localhost:8081/projects/2
```

**Response:** `200 OK`
```json
{
  "message": "Project deleted successfully",
  "projectId": 2,
  "projectName": "Mobile App Development"
}
```

**Error Response:** `400 Bad Request`
```json
{
  "timestamp": "2025-12-01T11:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot delete project with existing assignments"
}
```

---

### 6. Publish Project

**Endpoint:** `POST /projects/{id}/publish`

**Description:** Publishes a project to make it visible for Department Head approval

**Authentication:** Required (PROJECT_MANAGER - owner only)

**Path Parameters:**
- `id` (Long) - Project ID

**Request:**
```http
POST http://localhost:8081/projects/2/publish
```

**Response:** `200 OK`
```json
{
  "message": "Project published successfully",
  "projectId": 2,
  "status": "PENDING",
  "workflowStatus": "AWAITING_DEPARTMENT_HEAD_APPROVAL",
  "published": true,
  "visibleToAll": false
}
```

---

### 7. Unpublish Project

**Endpoint:** `POST /projects/{id}/unpublish`

**Description:** Unpublishes a project (makes it private again)

**Authentication:** Required (PROJECT_MANAGER - owner only)

**Path Parameters:**
- `id` (Long) - Project ID

**Request:**
```http
POST http://localhost:8081/projects/2/unpublish
```

**Response:** `200 OK`
```json
{
  "message": "Project unpublished successfully",
  "projectId": 2,
  "published": false,
  "visibleToAll": false
}
```

---

### 8. Cancel Project

**Endpoint:** `POST /projects/{id}/cancel`

**Description:** Cancels a published project

**Authentication:** Required (PROJECT_MANAGER - owner only)

**Path Parameters:**
- `id` (Long) - Project ID

**Request:**
```http
POST http://localhost:8081/projects/2/cancel
```

**Response:** `200 OK`
```json
{
  "message": "Project cancelled successfully",
  "projectId": 2,
  "status": "CANCELLED"
}
```

---

### 9. Get Published Projects (Employee View)

**Endpoint:** `GET /projects/published`

**Description:** Retrieves all published and approved projects (visible to employees)

**Authentication:** Required (EMPLOYEE role)

**Request:**
```http
GET http://localhost:8081/projects/published
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Website Redesign",
    "description": "Redesign company website",
    "status": "APPROVED",
    "published": true,
    "visibleToAll": true,
    "skillRequirements": [
      {
        "skill": "React",
        "requiredCount": 2
      }
    ],
    "assignedEmployees": 1,
    "totalRequired": 3
  }
]
```

---

## 📝 Application Management API

### 1. Get All Applications

**Endpoint:** `GET /applications`

**Description:** Retrieves all job applications (filtered by user role)

**Authentication:** Required

**Request:**
```http
GET http://localhost:8081/applications
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "employee": {
      "id": 1,
      "name": "John Doe",
      "email": "john@company.com"
    },
    "project": {
      "id": 1,
      "name": "Website Redesign"
    },
    "status": "PENDING",
    "appliedAt": "2025-12-01T09:30:00",
    "reviewedAt": null
  }
]
```

---

### 2. Submit Application

**Endpoint:** `POST /applications`

**Description:** Employee applies to a project

**Authentication:** Required (EMPLOYEE role)

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "projectId": 1,
  "coverLetter": "I am very interested in this project and have the required skills."
}
```

**Response:** `201 Created`
```json
{
  "id": 2,
  "employee": {
    "id": 1,
    "name": "John Doe"
  },
  "project": {
    "id": 1,
    "name": "Website Redesign"
  },
  "status": "PENDING",
  "coverLetter": "I am very interested in this project...",
  "appliedAt": "2025-12-01T10:15:00"
}
```

**Error Response:** `400 Bad Request`
```json
{
  "timestamp": "2025-12-01T10:15:00",
  "status": 400,
  "error": "Bad Request",
  "message": "You have already applied to this project"
}
```

---

### 3. Get My Applications (Employee)

**Endpoint:** `GET /applications/my-applications`

**Description:** Retrieves all applications submitted by the logged-in employee

**Authentication:** Required (EMPLOYEE role)

**Request:**
```http
GET http://localhost:8081/applications/my-applications
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "project": {
      "id": 1,
      "name": "Website Redesign"
    },
    "status": "PENDING",
    "appliedAt": "2025-12-01T09:30:00",
    "reviewedAt": null
  },
  {
    "id": 2,
    "project": {
      "id": 3,
      "name": "Mobile App"
    },
    "status": "APPROVED",
    "appliedAt": "2025-11-28T14:20:00",
    "reviewedAt": "2025-11-29T10:00:00"
  }
]
```

---

### 4. Approve Application

**Endpoint:** `POST /applications/{id}/approve`

**Description:** Project Manager approves an employee's application

**Authentication:** Required (PROJECT_MANAGER role)

**Path Parameters:**
- `id` (Long) - Application ID

**Request:**
```http
POST http://localhost:8081/applications/1/approve
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "employee": {
    "id": 1,
    "name": "John Doe"
  },
  "project": {
    "id": 1,
    "name": "Website Redesign"
  },
  "status": "APPROVED",
  "appliedAt": "2025-12-01T09:30:00",
  "reviewedAt": "2025-12-01T11:00:00"
}
```

---

### 5. Reject Application

**Endpoint:** `POST /applications/{id}/reject`

**Description:** Project Manager rejects an employee's application

**Authentication:** Required (PROJECT_MANAGER role)

**Path Parameters:**
- `id` (Long) - Application ID

**Request:**
```http
POST http://localhost:8081/applications/1/reject
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "employee": {
    "id": 1,
    "name": "John Doe"
  },
  "project": {
    "id": 1,
    "name": "Website Redesign"
  },
  "status": "REJECTED",
  "appliedAt": "2025-12-01T09:30:00",
  "reviewedAt": "2025-12-01T11:00:00"
}
```

---

### 6. Get Applications for Project

**Endpoint:** `GET /applications/project/{projectId}`

**Description:** Get all applications for a specific project

**Authentication:** Required (PROJECT_MANAGER - owner only)

**Path Parameters:**
- `projectId` (Long) - Project ID

**Request:**
```http
GET http://localhost:8081/applications/project/1
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "employee": {
      "id": 1,
      "name": "John Doe",
      "skills": ["React", "JavaScript"]
    },
    "status": "PENDING",
    "appliedAt": "2025-12-01T09:30:00"
  },
  {
    "id": 2,
    "employee": {
      "id": 2,
      "name": "Jane Smith",
      "skills": ["React", "CSS"]
    },
    "status": "PENDING",
    "appliedAt": "2025-12-01T10:45:00"
  }
]
```

---

## 👔 Assignment Management API

### 1. Get All Assignments

**Endpoint:** `GET /assignments`

**Description:** Retrieves all employee-project assignments

**Authentication:** Required

**Request:**
```http
GET http://localhost:8081/assignments
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "employee": {
      "id": 1,
      "name": "John Doe"
    },
    "project": {
      "id": 1,
      "name": "Website Redesign"
    },
    "status": "CONFIRMED",
    "assignedAt": "2025-12-01T10:00:00",
    "confirmedAt": "2025-12-01T14:30:00"
  }
]
```

---

### 2. Create Assignment

**Endpoint:** `POST /assignments`

**Description:** Resource Planner assigns employee to project

**Authentication:** Required (RESOURCE_PLANNER role)

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "employeeId": 1,
  "projectId": 1
}
```

**Response:** `201 Created`
```json
{
  "id": 2,
  "employee": {
    "id": 1,
    "name": "John Doe"
  },
  "project": {
    "id": 1,
    "name": "Website Redesign"
  },
  "status": "PENDING",
  "assignedAt": "2025-12-01T11:00:00"
}
```

**Error Response:** `400 Bad Request`
```json
{
  "timestamp": "2025-12-01T11:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Employee is already assigned to another project"
}
```

---

### 3. Confirm Assignment (Employee)

**Endpoint:** `PUT /assignments/{id}/confirm`

**Description:** Employee confirms their assignment to a project

**Authentication:** Required (EMPLOYEE role - must be the assigned employee)

**Path Parameters:**
- `id` (Long) - Assignment ID

**Request:**
```http
PUT http://localhost:8081/assignments/2/confirm
```

**Response:** `200 OK`
```json
{
  "id": 2,
  "employee": {
    "id": 1,
    "name": "John Doe"
  },
  "project": {
    "id": 1,
    "name": "Website Redesign"
  },
  "status": "CONFIRMED",
  "assignedAt": "2025-12-01T11:00:00",
  "confirmedAt": "2025-12-01T14:30:00"
}
```

**Note:** After confirmation, employee's `available` status changes to `false`

---

### 4. Reject Assignment (Employee)

**Endpoint:** `PUT /assignments/{id}/reject`

**Description:** Employee rejects their assignment

**Authentication:** Required (EMPLOYEE role - must be the assigned employee)

**Path Parameters:**
- `id` (Long) - Assignment ID

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "reason": "I don't have availability during the project timeline"
}
```

**Response:** `200 OK`
```json
{
  "id": 2,
  "status": "REJECTED",
  "rejectionReason": "I don't have availability during the project timeline"
}
```

---

### 5. Remove Assignment

**Endpoint:** `DELETE /assignments/{id}`

**Description:** Removes an employee from a project assignment

**Authentication:** Required (RESOURCE_PLANNER or PROJECT_MANAGER)

**Path Parameters:**
- `id` (Long) - Assignment ID

**Request:**
```http
DELETE http://localhost:8081/assignments/2
```

**Response:** `200 OK`
```json
{
  "message": "Assignment removed successfully",
  "assignmentId": 2,
  "employeeId": 1,
  "projectId": 1
}
```

**Note:** Employee's `available` status changes back to `true`

---

### 6. Get Assignments for Project

**Endpoint:** `GET /assignments/project/{projectId}`

**Description:** Get all assignments for a specific project

**Authentication:** Required

**Path Parameters:**
- `projectId` (Long) - Project ID

**Request:**
```http
GET http://localhost:8081/assignments/project/1
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "employee": {
      "id": 1,
      "name": "John Doe",
      "skills": ["React", "JavaScript"]
    },
    "status": "CONFIRMED",
    "assignedAt": "2025-12-01T10:00:00"
  },
  {
    "id": 2,
    "employee": {
      "id": 2,
      "name": "Jane Smith",
      "skills": ["React", "CSS"]
    },
    "status": "PENDING",
    "assignedAt": "2025-12-01T11:00:00"
  }
]
```

---

### 7. Get My Assignments (Employee)

**Endpoint:** `GET /assignments/my-assignments`

**Description:** Retrieves all assignments for the logged-in employee

**Authentication:** Required (EMPLOYEE role)

**Request:**
```http
GET http://localhost:8081/assignments/my-assignments
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "project": {
      "id": 1,
      "name": "Website Redesign",
      "startDate": "2026-01-15",
      "endDate": "2026-03-30"
    },
    "status": "CONFIRMED",
    "assignedAt": "2025-12-01T10:00:00",
    "confirmedAt": "2025-12-01T14:30:00"
  }
]
```

---

## 🔄 Workflow Management API

### 1. Start Workflow for Project

**Endpoint:** `POST /workflow/start/{projectId}`

**Description:** Starts approval workflow for a project

**Authentication:** Required (PROJECT_MANAGER - owner only)

**Path Parameters:**
- `projectId` (Long) - Project ID

**Request:**
```http
POST http://localhost:8081/workflow/start/1
```

**Response:** `200 OK`
```json
{
  "message": "Workflow started successfully",
  "projectId": 1,
  "projectName": "Website Redesign",
  "processInstanceId": "process-12345",
  "workflowStatus": "RUNNING"
}
```

---

### 2. Get Active Workflow Tasks

**Endpoint:** `GET /workflow/tasks`

**Description:** Retrieves all active workflow tasks (filtered by user role)

**Authentication:** Required

**Request:**
```http
GET http://localhost:8081/workflow/tasks
```

**Response:** `200 OK`
```json
[
  {
    "taskId": "task-67890",
    "taskName": "Approve Staffing Request",
    "assignee": null,
    "candidateGroup": "DepartmentHead",
    "processInstanceId": "process-12345",
    "createTime": "2025-12-01T09:00:00",
    "projectId": 1,
    "projectName": "Website Redesign"
  }
]
```

---

### 3. Complete Workflow Task

**Endpoint:** `POST /workflow/tasks/{taskId}/complete`

**Description:** Completes a workflow task (approve or reject)

**Authentication:** Required (Must be in candidate group)

**Path Parameters:**
- `taskId` (String) - Flowable task ID

**Query Parameters:**
- `approved` (Boolean) - true for approval, false for rejection

**Request:**
```http
POST http://localhost:8081/workflow/tasks/task-67890/complete?approved=true
```

**Response:** `200 OK`
```json
{
  "message": "Task completed successfully",
  "taskId": "task-67890",
  "taskName": "Approve Staffing Request",
  "approved": true,
  "projectId": 1,
  "projectStatus": "APPROVED"
}
```

---

### 4. Get Task Details

**Endpoint:** `GET /workflow/tasks/{taskId}`

**Description:** Retrieves details of a specific workflow task

**Authentication:** Required

**Path Parameters:**
- `taskId` (String) - Flowable task ID

**Request:**
```http
GET http://localhost:8081/workflow/tasks/task-67890
```

**Response:** `200 OK`
```json
{
  "taskId": "task-67890",
  "taskName": "Approve Staffing Request",
  "assignee": null,
  "candidateGroup": "DepartmentHead",
  "processInstanceId": "process-12345",
  "createTime": "2025-12-01T09:00:00",
  "variables": {
    "projectId": 1,
    "projectName": "Website Redesign",
    "createdBy": "pm"
  }
}
```

---

### 5. Get Process History

**Endpoint:** `GET /workflow/history/{processInstanceId}`

**Description:** Retrieves history of a workflow process

**Authentication:** Required

**Path Parameters:**
- `processInstanceId` (String) - Flowable process instance ID

**Request:**
```http
GET http://localhost:8081/workflow/history/process-12345
```

**Response:** `200 OK`
```json
{
  "processInstanceId": "process-12345",
  "processDefinitionKey": "staffingApproval",
  "startTime": "2025-12-01T09:00:00",
  "endTime": "2025-12-01T15:30:00",
  "duration": 23400000,
  "tasks": [
    {
      "taskId": "task-67890",
      "taskName": "Approve Staffing Request",
      "assignee": "depthead",
      "startTime": "2025-12-01T09:00:00",
      "endTime": "2025-12-01T10:30:00",
      "outcome": "approved"
    }
  ]
}
```

---

## 🖥️ UI Endpoints Reference

These are web-based UI endpoints (HTML pages) - not REST APIs.

### Project Manager UI

| Endpoint | Description |
|----------|-------------|
| `GET /ui/projects` | Project Manager dashboard |
| `GET /ui/projects/create` | Create new project form |
| `GET /ui/projects/{id}` | View project details |
| `GET /ui/projects/{id}/edit` | Edit project form |
| `POST /ui/projects/{id}/publish` | Publish project |
| `GET /ui/projects/{id}/applications` | View applications for project |

### Department Head UI

| Endpoint | Description |
|----------|-------------|
| `GET /ui/department-head/dashboard` | Department Head dashboard |
| `GET /ui/department-head/tasks/{taskId}` | View task details |
| `POST /ui/department-head/tasks/{taskId}/approve` | Approve project |
| `POST /ui/department-head/tasks/{taskId}/reject` | Reject project |
| `GET /ui/department-head/history` | View approval history |

### Resource Planner UI

| Endpoint | Description |
|----------|-------------|
| `GET /ui/resource-planner/dashboard` | Resource Planner dashboard |
| `GET /ui/resource-planner/project/{projectId}` | View project staffing details |
| `POST /ui/resource-planner/project/{projectId}/propose` | Propose employee for project |
| `GET /ui/resource-planner/employees` | View all employees list |

### Employee UI

| Endpoint | Description |
|----------|-------------|
| `GET /ui/employee/dashboard` | Employee dashboard |
| `GET /ui/employee/projects` | Browse published projects |
| `GET /ui/employee/projects/{id}` | View project details |
| `POST /ui/employee/projects/{id}/apply` | Apply to project |
| `GET /ui/employee/applications` | View my applications |
| `GET /ui/employee/assignments` | View my assignments |
| `POST /ui/employee/assignments/{id}/confirm` | Confirm assignment |

### Authentication UI

| Endpoint | Method | Description |
|----------|--------|-------------|
| `GET /login` | GET | Login page |
| `POST /login` | POST | Submit login credentials |
| `POST /logout` | POST | Logout user |
| `GET /register` | GET | Employee registration form |
| `POST /register` | POST | Submit registration |

---

## ⚠️ Error Codes

### HTTP Status Codes

| Code | Status | Description |
|------|--------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request data or business logic error |
| 401 | Unauthorized | Authentication required or failed |
| 403 | Forbidden | User doesn't have permission |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Server error |

### Common Error Response Format

```json
{
  "timestamp": "2025-12-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Employee is already assigned to another project",
  "path": "/assignments"
}
```

### Validation Error Response

```json
{
  "timestamp": "2025-12-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email is required"
    },
    {
      "field": "skills",
      "message": "At least one skill is required"
    }
  ]
}
```

---

## 🧪 Testing Guide

### Using Postman

**Step 1: Import Collection**
- Download the Postman collection from repository
- Import into Postman: `File → Import → Select JSON file`

**Step 2: Set Up Environment**
```json
{
  "base_url": "http://localhost:8081",
  "auth_token": ""
}
```

**Step 3: Login First**
```
POST {{base_url}}/login
Body (x-www-form-urlencoded):
  username: pm
  password: pm123
```

**Step 4: Test Endpoints**
- Use saved session cookie for subsequent requests
- Postman automatically handles cookies

### Testing Workflow (Complete Example)

**Scenario: Project Manager creates project, gets it approved, Resource Planner assigns employees**

```
1. Login as Project Manager
   POST /login
   Body: username=pm, password=pm123

2. Create a new project
   POST /projects
   Body: {
     "name": "New Website",
     "description": "Build new company website",
     "requiredSkills": ["React", "Node.js"],
     "status": "PENDING",
     "totalEmployeesRequired": 2
   }
   → Response: Project created with id=5

3. Publish the project
   POST /projects/5/publish
   → Project status: PENDING, workflowStatus: AWAITING_DEPARTMENT_HEAD_APPROVAL

4. Logout and login as Department Head
   POST /logout
   POST /login
   Body: username=depthead, password=head123

5. Get pending tasks
   GET /workflow/tasks
   → Response: Task with taskId="abc123" for project 5

6. Approve the project
   POST /workflow/tasks/abc123/complete?approved=true
   → Project status changes to APPROVED

7. Logout and login as Resource Planner
   POST /logout
   POST /login
   Body: username=planner, password=planner123

8. View available employees
   GET /employees/available
   → Response: List of available employees

9. Assign employee to project
   POST /assignments
   Body: {
     "employeeId": 1,
     "projectId": 5
   }
   → Assignment created with status=PENDING

10. Logout and login as Employee
    POST /logout
    POST /login
    Body: username=emp1, password=emp123

11. View my assignments
    GET /assignments/my-assignments
    → Response: Assignment for project 5

12. Confirm assignment
    PUT /assignments/1/confirm
    → Assignment status: CONFIRMED
    → Employee availability: false
```

### Manual Testing via Browser

**Access UI directly:**
```
1. Open browser: http://localhost:8081/login
2. Login with credentials (e.g., pm/pm123)
3. Navigate through UI pages
4. Perform actions via web forms
```

---

## 🔗 Related Documentation

- **Database Schema:** See `DATABASE_SCHEMA.md`
- **Deployment Guide:** See `DEPLOYMENT.md`
- **User Manual:** See `USER_GUIDE.md`

---

## 📧 Support

For questions or issues:
- **GitHub Issues:** [Create an issue in the repository]
- **Documentation:** [Link to wiki or confluence]

---

## 📝 Changelog

### Version 1.0 (Current)
- Initial API documentation
- Complete REST API endpoints
- Workflow integration
- Authentication and authorization

---

**Last Updated:** January 27, 2026  
**Version:** 1.0  
**Contributors:** [Your Team Names]
