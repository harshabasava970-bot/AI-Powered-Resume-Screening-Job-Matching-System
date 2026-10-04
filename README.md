# 🤖 AI-Powered Resume Screening & Job Matching System

A complete, production-ready full-stack application that uses intelligent skill-based matching to connect candidates with the most suitable job opportunities.

---

## 📋 Project Overview

This system allows job seekers to upload their resumes and receive transparent, explainable match scores against job descriptions. Recruiters can post jobs and view ranked candidate matches. An admin panel provides system-wide oversight.

**Built entirely in Java with Spring Boot** — designed as a portfolio project demonstrating real-world enterprise Java skills.

---

## 🎯 Problem Statement

Traditional resume screening is manual, time-consuming, and inconsistent. This system automates the process using:
- Automated skill extraction from PDF/DOCX resumes
- Configurable weighted scoring against job requirements
- Transparent, explainable match results (no black box)
- Ranked job recommendations for candidates
- Candidate ranking for recruiters

---

## ✨ Features

| Feature | Description |
|---|---|
| 📄 Resume Upload | PDF & DOCX support, text extraction via Apache PDFBox & POI |
| 🔍 Skill Extraction | Case-insensitive n-gram matching against a configurable skill dictionary |
| 🎯 Match Analysis | Weighted scoring: Required Skills 60%, Preferred 20%, Experience 10%, Education 10% |
| 💡 Score Explanation | Full breakdown showing exactly why each score was generated |
| 🚀 Recommendations | Ranked job recommendations from candidate's resume |
| 📊 Dashboards | Role-specific dashboards for Candidate, Recruiter, Admin |
| 🔐 Security | BCrypt password hashing, Spring Security, role-based authorization |
| 📱 Responsive UI | Works on desktop, tablet, and mobile |

---

## 🛠 Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2 |
| **Security** | Spring Security 6 (Session-based, BCrypt) |
| **ORM** | Spring Data JPA + Hibernate |
| **Database** | MySQL 8.0 |
| **Frontend** | Thymeleaf + Plain HTML/CSS/JS |
| **PDF Parsing** | Apache PDFBox 3.0 |
| **DOCX Parsing** | Apache POI 5.2 |
| **Build Tool** | Maven |
| **Testing** | JUnit 5 + Mockito |
| **Deployment** | Docker + Docker Compose |

---

## 🏗 Architecture

```
Browser (Thymeleaf)
    │
    ▼
Spring Security Filter Chain
    │
    ▼
Controllers (MVC + REST API)
    │
    ▼
Service Layer (Business Logic)
    │         │
    ▼         ▼
Repository   File Storage
Layer        (./uploads/)
    │
    ▼
MySQL Database
```

**Matching Pipeline:**
```
Resume File
    │
    ▼
Text Extraction (PDFBox / POI)
    │
    ▼
Skill Extraction (n-gram matching against Skill dictionary)
    │
    ▼
Job Requirement Skills (from DB)
    │
    ▼
MatchingService.computeScore()
    │
    ▼
Match Score + Breakdown + Recommendations
```

---

## 📁 Folder Structure

```
src/main/java/com/resumescreening/
├── config/           # Security, FileStorage, DataInitializer
├── controller/       # MVC controllers (web pages)
│   └── api/          # REST API controllers
├── dto/
│   ├── request/      # Input DTOs with validation
│   └── response/     # Output DTOs (never expose entities)
├── entity/           # JPA entities
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── repository/       # Spring Data JPA repositories
├── security/         # UserDetailsService, SecurityUtils
├── service/          # Service interfaces
│   └── impl/         # Service implementations
└── util/             # Utility classes

src/main/resources/
├── templates/        # Thymeleaf HTML templates
│   ├── auth/         # Login, Register
│   ├── candidate/    # Candidate pages
│   ├── recruiter/    # Recruiter pages
│   ├── admin/        # Admin pages
│   ├── jobs/         # Job listing/detail
│   ├── fragments/    # Reusable layout fragments
│   └── error/        # Error pages
├── static/
│   ├── css/main.css  # All styles
│   └── js/main.js    # Client-side JS
└── application.properties
```

---

## 🗄 Database Design

| Table | Purpose |
|---|---|
| `users` | Authentication, role, enabled flag |
| `candidate_profiles` | Extended candidate info |
| `recruiter_profiles` | Company/recruiter info |
| `resumes` | File metadata + extracted text + parsed info |
| `skills` | Global skill dictionary (normalized names) |
| `resume_skills` | Many-to-many: resume ↔ skill |
| `jobs` | Job postings |
| `job_skills` | Many-to-many: job ↔ skill with REQUIRED/PREFERRED type |
| `match_analyses` | Stored analysis results (score, breakdown, skill lists) |

---

## 🔌 API Endpoints

```
POST   /api/auth/register          Register new user
GET    /api/auth/check-username    Check username availability
GET    /api/auth/check-email       Check email availability

POST   /api/resumes/upload         Upload resume (CANDIDATE)
GET    /api/resumes                List own resumes (CANDIDATE)
GET    /api/resumes/{id}           Get resume details (CANDIDATE)
DELETE /api/resumes/{id}           Remove resume (CANDIDATE)

GET    /api/jobs                   List active jobs
GET    /api/jobs?search=keyword    Search jobs
GET    /api/jobs/{id}              Get job details
POST   /api/jobs                   Create job (RECRUITER)
PUT    /api/jobs/{id}              Update job (RECRUITER)
DELETE /api/jobs/{id}              Delete job (RECRUITER)

POST   /api/matching/analyze       Run analysis (CANDIDATE)
GET    /api/matching/history       Analysis history (CANDIDATE)
GET    /api/matching/{id}          Get analysis by ID (CANDIDATE)
GET    /api/recommendations        Get recommendations (CANDIDATE)

GET    /actuator/health            Health check endpoint
```

---

## 🧠 How Matching Works

The matching engine is **transparent and deterministic**. The same inputs always produce the same score.

### Scoring Formula:
```
Score = (
    RequiredSkillScore  × 0.60  +   ← 60% weight
    PreferredSkillScore × 0.20  +   ← 20% weight
    ExperienceScore     × 0.10  +   ← 10% weight
    EducationScore      × 0.10      ← 10% weight
) × 100
```

### Example:
- Resume skills: Java, Spring Boot, MySQL, Git
- Job required: Java, Spring Boot, MySQL, Docker, AWS
- Job preferred: Kubernetes, React

**Calculation:**
- Required: 3/5 matched = 60% → 60 × 0.60 = **36 pts**
- Preferred: 0/2 matched = 0% → 0 × 0.20 = **0 pts**
- Experience: detected 2 years, required 1-3 → 100% → 100 × 0.10 = **10 pts**
- Education: B.Tech detected → 85% → 85 × 0.10 = **8.5 pts**
- **Total: 54.5%**

Weights are configurable in `application.properties`.

---

## 🚀 How to Run Locally

### Prerequisites
- Java 17+
- MySQL 8.0 running locally
- Maven (or use included `mvnw`)

### Step 1: Create MySQL database
```sql
CREATE DATABASE resume_screening_db;
CREATE USER 'appuser'@'localhost' IDENTIFIED BY 'yourpassword';
GRANT ALL PRIVILEGES ON resume_screening_db.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
```

### Step 2: Configure environment
```bash
# Create .env file from template
cp .env.example .env
# Edit .env with your database credentials
```

### Step 3: Run
```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=

# Linux/Mac
chmod +x mvnw && ./mvnw spring-boot:run
```

### Step 4: Open in browser
```
http://localhost:8080
```

---

## 🐳 Run with Docker Compose (Easiest)

```bash
# 1. Copy and edit environment file
cp .env.example .env

# 2. Build and start all containers
docker-compose up -d

# 3. View logs
docker-compose logs -f app

# 4. Open browser
# http://localhost:8080

# 5. Stop
docker-compose down
```

---

## ⚙️ Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC connection URL | `jdbc:mysql://localhost:3306/resume_screening_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | _(empty)_ |
| `SERVER_PORT` | HTTP port | `8080` |
| `APP_SECRET` | Application secret key | _(change in prod)_ |
| `FILE_UPLOAD_DIR` | Resume upload directory | `./uploads` |
| `MAX_FILE_SIZE` | Max resume file size | `10MB` |
| `JPA_DDL_AUTO` | Hibernate DDL mode | `update` |

---

## 🧪 How to Run Tests

```bash
# Run all unit tests
mvnw.cmd test

# Run specific test class
mvnw.cmd test -Dtest=SkillExtractionServiceTest

# Run with test report
mvnw.cmd test surefire-report:report
```

Tests use H2 in-memory database — no MySQL required for testing.

---

## 🏗 How to Build

```bash
# Build JAR (skip tests)
mvnw.cmd package -DskipTests

# Build JAR (with tests)
mvnw.cmd package

# The JAR will be at: target/ai-resume-screening-1.0.0.jar
```

---

## 🌐 Deployment Instructions

### Option 1: Docker Compose (Recommended)
See "Run with Docker Compose" section above.

### Option 2: Railway (Free tier)
1. Push code to GitHub
2. Connect repository to [Railway.app](https://railway.app)
3. Add MySQL plugin
4. Set environment variables from `.env.example`
5. Deploy — Railway auto-detects Dockerfile

### Option 3: Render (Free tier)
1. Push to GitHub
2. Create a new Web Service on [Render.com](https://render.com)
3. Select Docker runtime
4. Add MySQL (PlanetScale or Clever Cloud free tier)
5. Set environment variables

### Option 4: Local JAR deployment
```bash
# Build
mvnw.cmd package -DskipTests

# Run with environment variables
java -jar target/ai-resume-screening-1.0.0.jar \
  --DB_URL=jdbc:mysql://... \
  --DB_USERNAME=user \
  --DB_PASSWORD=pass
```

---

## ⚠️ Free Tier Limitations

| Platform | Limitation |
|---|---|
| **Railway** | Free tier may spin down after inactivity (cold start ~30s) |
| **Render** | Free tier sleeps after 15min inactivity |
| **PlanetScale** | Free tier MySQL: 5GB storage, limited connections |
| **Clever Cloud** | Free MySQL: 256MB storage |

All free tiers are suitable for portfolio demonstrations.

---

## 🔑 Demo Credentials

> These are **safe dummy credentials** for demonstration only.

| Role | Username | Password |
|---|---|---|
| Candidate | `candidate1` | `Candidate@1234` |
| Recruiter | `recruiter1` | `Recruiter@1234` |
| Admin | `admin` | `Admin@1234` |

---

## 🖼 Screenshots

_(Add screenshots of your deployed application here)_
- Landing page
- Candidate dashboard
- Resume upload & parsing
- Match analysis result with score breakdown
- Job recommendations
- Recruiter dashboard

---

## 🔮 Future Enhancements

- [ ] Email notifications for new job matches
- [ ] Resume export to PDF with match report
- [ ] Advanced search with filters (location, salary, type)
- [ ] Skill gap learning resource suggestions
- [ ] LinkedIn resume import
- [ ] Batch analysis of all jobs at once
- [ ] Admin analytics charts (Chart.js)
- [ ] JWT-based API authentication option

---

## 🎤 Interview Explanation

**Q: Explain this project in simple terms.**

**A:** This is a job matching system that helps candidates find the best-fit jobs using their resume.

The flow is:
1. A candidate registers and uploads their resume (PDF or DOCX)
2. Apache PDFBox/POI extracts the raw text
3. A keyword matching algorithm scans the text for known skills from a database dictionary
4. The candidate selects a job they're interested in
5. The matching engine compares the candidate's detected skills against the job's required and preferred skills
6. It calculates a weighted score: required skills (60%), preferred skills (20%), experience (10%), education (10%)
7. The result shows: match percentage, which skills matched, which are missing, and a plain-English explanation
8. The system also auto-generates ranked job recommendations

The AI component is intentionally explainable — it uses keyword matching, not a black-box neural network, so every score can be justified to a user.

**Key Java concepts demonstrated:**
- OOP (entities, services, controllers with single responsibility)
- Dependency Injection (constructor injection throughout)
- Spring Data JPA with proper relationships
- Spring Security with role-based access control
- BCrypt password hashing
- Bean Validation (@Valid, @NotBlank, @Email)
- Global exception handling (@ControllerAdvice)
- DTO pattern (never expose JPA entities directly)
- Service interfaces for testability
- JUnit 5 + Mockito unit tests
