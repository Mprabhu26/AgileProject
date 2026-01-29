# Workforce Planning System

A comprehensive web-based workforce planning and project staffing management system built with Spring Boot and Flowable workflow engine.

## 📋 Overview

The Workforce Planning System streamlines the process of project staffing by connecting Project Managers, Department Heads, Resource Planners, and Employees through an automated workflow. The system manages project creation, approval workflows, employee assignments, and job applications.

## ✨ Features

### For Project Managers
- Create and manage projects with required skills
- Publish projects for Department Head approval
- View and manage employee applications
- Track project staffing status

### For Department Heads
- Review and approve/reject project requests
- View approval history and statistics
- Dashboard with pending approvals

### For Resource Planners
- View all approved projects
- Search employees by skills and availability
- Assign employees to projects
- Track skill gaps and staffing progress
- Export employee list to CSV

### For Employees
- Browse published and approved projects
- Apply to projects matching their skills
- View application status
- Confirm or reject project assignments
- Track assignment history

### Workflow Features
- Automated approval workflows using Flowable
- Department Head approval process
- Employee assignment notifications
- Status tracking throughout the process

## 🛠️ Technology Stack

- **Backend Framework:** Spring Boot 3.2.0
- **Database:** PostgreSQL 15
- **Workflow Engine:** Flowable 7.2.0
- **Security:** Spring Security with BCrypt password hashing
- **Frontend:** Thymeleaf templates, HTML, CSS, JavaScript
- **Build Tool:** Maven
- **Java Version:** 17+

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK) 17 or higher**
  - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
  - Verify: `java -version`

- **Docker Desktop**
  - Download from [Docker](https://www.docker.com/products/docker-desktop/)
  - Verify: `docker --version`

- **Maven** (optional, included with most IDEs)
  - Download from [Maven](https://maven.apache.org/download.cgi)
  - Verify: `mvn --version`

- **IDE** (recommended)
  - IntelliJ IDEA, Eclipse, or VS Code

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Mprabhu26/AgileProject.git
cd AgileProject
```

### 2. Start PostgreSQL Database with Docker

```bash
docker run --name workforce-postgres \
  -e POSTGRES_DB=workforcedb \
  -e POSTGRES_USER=workforce \
  -e POSTGRES_PASSWORD=workforce123 \
  -p 5432:5432 \
  -d postgres:15
```

**Verify Docker container is running:**
```bash
docker ps
```

You should see `workforce-postgres` in the list.

### 3. Configure Database Connection

**Option A: Using application.properties (Recommended)**

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/workforcedb
spring.datasource.username=workforce
spring.datasource.password=workforce123
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

server.port=8081
```

**Option B: Using Environment Variables**

Set the following environment variables:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=workforcedb
DB_USERNAME=workforce
DB_PASSWORD=workforce123
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

**Option A: Using Maven**
```bash
mvn spring-boot:run
```

**Option B: Using IDE**
- Open project in IntelliJ IDEA or Eclipse
- Navigate to `src/main/java/com/workforce/workforceplanning/WorkforcePlanningApplication.java`
- Right-click and select "Run"

**Wait for startup message:**
```
Started WorkforcePlanningApplication in X.XXX seconds
```

### 6. Access the Application

Open your browser and navigate to:
```
http://localhost:8081/login
```

## 🔑 Default Login Credentials

The application comes with pre-configured users for testing:

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| `pm` | `pm123` | Project Manager | Create and manage projects |
| `depthead` | `head123` | Department Head | Approve/reject projects |
| `planner` | `planner123` | Resource Planner | Assign employees to projects |
| `emp1` | `emp123` | Employee | Browse and apply to projects |

## 📚 Sample Data

On first startup, the application automatically creates:
- **3 Sample Employees:** John Doe, Jane Smith, Bob Wilson
- **2 Sample Projects:** Website Redesign, Mobile App Development
- **Pre-configured User Accounts** (see login credentials above)

## 🗂️ Project Structure

```
AgileProject/
├── src/
│   ├── main/
│   │   ├── java/com/workforce/workforceplanning/
│   │   │   ├── config/              # Security and Flowable configuration
│   │   │   ├── controller/          # REST API controllers
│   │   │   ├── ui/                  # UI controllers (Thymeleaf)
│   │   │   ├── model/               # Entity classes
│   │   │   ├── repository/          # Spring Data JPA repositories
│   │   │   ├── service/             # Business logic services
│   │   │   ├── workflow/            # Flowable workflow delegates
│   │   │   └── WorkforcePlanningApplication.java
│   │   └── resources/
│   │       ├── processes/           # BPMN workflow definitions
│   │       ├── templates/           # Thymeleaf HTML templates
│   │       ├── static/              # CSS, JS, images
│   │       └── application.properties
│   └── test/                        # Unit and integration tests
├── pom.xml                          # Maven dependencies
├── README.md                        # This file
├── API_DOCUMENTATION.md             # Complete API reference
└── .gitignore
```

## 🔄 Complete Workflow Example

### Scenario: Project Manager creates a project and gets it staffed

1. **Login as Project Manager** (`pm/pm123`)
2. **Create a new project** at `/ui/projects/create`
3. **Publish the project** - sends to Department Head for approval
4. **Logout and login as Department Head** (`depthead/head123`)
5. **Approve the project** from dashboard
6. **Logout and login as Resource Planner** (`planner/planner123`)
7. **View approved projects** and assign employees
8. **Logout and login as Employee** (`emp1/emp123`)
9. **View assignment** and confirm participation

## 📖 API Documentation

For complete REST API documentation, including all endpoints, request/response examples, and authentication details, see:

**[API_DOCUMENTATION.md](API_DOCUMENTATION.md)**

The API documentation includes:
- 34+ REST API endpoints
- Request/response examples
- Authentication guide
- Error handling
- Testing guide with Postman

## 🗃️ Database Schema

The application uses the following main tables:

- **users** - User authentication and roles
- **employees** - Employee profiles and skills
- **projects** - Project information and requirements
- **project_skill_requirements** - Skills required for each project
- **applications** - Employee job applications
- **assignments** - Employee-project assignments
- **notifications** - System notifications

Tables are automatically created on first run using Hibernate DDL auto-generation.

## 🛠️ Common Issues and Solutions

### Issue: Application won't start - Database connection error

**Error:** `Driver org.postgresql.Driver claims to not accept jdbcUrl`

**Solution:**
- Verify Docker container is running: `docker ps`
- Check database credentials in `application.properties`
- Ensure environment variables are set correctly

### Issue: Docker container already exists

**Error:** `docker: Error response from daemon: Conflict. The container name "/workforce-postgres" is already in use.`

**Solution:**
```bash
# Stop and remove existing container
docker stop workforce-postgres
docker rm workforce-postgres

# Then run the docker run command again
```

### Issue: Port 8081 already in use

**Solution:**
- Change port in `application.properties`: `server.port=8082`
- Or stop the application using port 8081

### Issue: Tables not created automatically

**Solution:**
- Verify `spring.jpa.hibernate.ddl-auto=update` in `application.properties`
- Check database connection is successful in application logs
- Manually connect to database and verify: `docker exec -it workforce-postgres psql -U workforce -d workforcedb`

## 🧪 Testing

### Manual Testing via Web UI

1. Start the application
2. Navigate to `http://localhost:8081/login`
3. Login with provided credentials
4. Test features through web interface

### API Testing with Postman

1. Import the API collection (if available in repository)
2. Set base URL: `http://localhost:8081`
3. Login first to obtain session cookie
4. Test individual endpoints

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for detailed testing examples.

## 🔧 Development

### Adding New Features

1. Create entity classes in `model/` package
2. Create repository interfaces in `repository/` package
3. Implement business logic in `service/` package
4. Create REST endpoints in `controller/` package
5. Create UI pages in `templates/` folder
6. Add UI controllers in `ui/` package

### Database Migrations

The application uses Hibernate auto-generation (`spring.jpa.hibernate.ddl-auto=update`).

For production deployments, consider:
- Using `ddl-auto=validate` 
- Implementing Flyway or Liquibase for version-controlled migrations

## 🤝 Contributing

### Branching Strategy

- `main` - Production-ready code
- `develop` - Integration branch
- `feature/*` - Feature branches
- `bugfix/*` - Bug fix branches

### Development Workflow

1. Pull latest changes from `develop`
2. Create feature branch: `git checkout -b feature/your-feature-name`
3. Make changes and commit
4. Push to remote: `git push origin feature/your-feature-name`
5. Create Pull Request to `develop`

## 📝 Sprint Planning

The project follows Agile methodology with defined sprints:

- **Sprint 1:** October 31 - November 28, 2025 : Core project and workflow setup
- **Sprint 2:** November 28 - December 19, 2025 - Employee core features
- **Sprint 3:** December 19, 2025 - January 16, 2026 - Advanced features and polish

## 📞 Support

For questions or issues:
- Create an issue in the GitHub repository
- Contact the development team
- Refer to API documentation for endpoint details

## 👥 Team

This project is developed as part of an Agile Software Development course.

**Team Members:**
- Amrita Elizabeth
- Ayush Parmar
- Mithila Prabhu
- Snehamol Sunny

**Course:** Agile Software Development  
**Institution:** [Institution name]  
**Academic Year:** 2025-2026

## 📄 License

This project is developed for educational purposes as part of an Agile Software Development course.

## 🔄 Version History

### Version 1.0 (Current)
- Complete employee management functionality
- Project creation and approval workflow
- Department Head approval process
- Resource Planner assignment features
- Employee application and assignment system
- REST API with 34+ endpoints
- Web-based UI for all roles

---

**Last Updated:** January 27, 2026  
**Current Version:** 1.0  
**Status:** Active Development
