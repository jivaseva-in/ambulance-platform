*Note: the links and images in this document may point to external sources. Please review them before opening.*

# 🚑 Smart Ambulance Dispatch & Real-Time Tracking Platform

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Framework](https://img.shields.io/badge/Spring%20Boot-3.3%2B-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/PostgreSQL-15%2B-blue.svg)](https://www.postgresql.org/)
[![Frontend](https://img.shields.io/badge/Frontend-Vanilla%20ES6%20%7C%20Tailwind%20%7C%20Leaflet-teal.svg)](https://leafletjs.com/)

A modern, responsive emergency dispatch and real-time ambulance tracking system built with **Java 21**, **Spring Boot 3.3+**, **PostgreSQL**, and a single-server modular browser interface.

The platform provides dedicated, role-separated workflows for **Citizens in Emergency** (zero-login guest booking & live map telemetry), **Ambulance Drivers** (dispatch queue, status transitions, GPS pinging), and **Call Center Dispatchers** (inbound phone triage logging).

---

## 🌟 Key Features

* **🚨 Anonymous / Guest Emergency Booking:** Citizens in distress can dispatch an ambulance instantly without registering or logging in.
* **📍 Interactive OpenStreetMap (Leaflet.js):** Pinpoint pickup locations via map drag-and-drop or one-click HTML5 GPS detection.
* **📡 Real-Time Telemetry & Tracking:** Live tracking card with speed, status badges, and animated ambulance marker routes.
* **🚐 Driver Operations Portal:** Role-gated dashboard allowing drivers to claim available jobs and transition bookings through their operational lifecycle:

  $$
  \text{REQUESTED} \longrightarrow \text{ASSIGNED} \longrightarrow \text{IN\_TRANSIT} \longrightarrow \text{ARRIVED} \longrightarrow \text{COMPLETED}
  $$

* **🎧 Call Center Dispatch Intake:** Dedicated console for operators to triage phone calls, take clinical notes, and dispatch units.
* **🔒 Enterprise Security:** Stateless Spring Security 6 with JWT authentication, BCrypt password hashing, and role-based method security (`@PreAuthorize`).
* **🧩 Modular Component Architecture:** Lightweight ES6 modules (`type="module"`) and dynamic HTML component partials (`/components/*.html`) served directly by Spring Boot without Node.js or bundlers.

---

## 🏗️ Architecture & Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Java 21, Spring Boot 3.3+, Spring Data JPA, Spring Security 6 |
| **Database** | PostgreSQL (`ambulance_db`), Hibernate ORM |
| **Security & Auth** | JSON Web Tokens (`jjwt 0.12.6`), BCrypt Password Encoding |
| **API Documentation** | OpenAPI 3.0 / Swagger UI |
| **Frontend Web** | HTML5 Partials, Vanilla ES6 Modules, Tailwind CSS (CDN), Leaflet.js |
| **Build & Tooling** | Apache Maven, IntelliJ HTTP Client |

---

## 📁 Repository Structure

```text
ambulance-platform/
├── backend/
│   ├── pom.xml                                  # Maven dependencies and build configuration
│   ├── test-requests.http                       # End-to-end API test workflow (IntelliJ HTTP Client)
│   └── src/
│       ├── main/
│       │   ├── java/com/ambulanceservice/
│       │   │   ├── AmbulanceServiceApplication.java
│       │   │   ├── common/                      # Global exception handling & custom exceptions
│       │   │   ├── config/                      # SecurityConfig, CORS & PasswordEncoder
│       │   │   ├── security/                    # JWT utils, AuthTokenFilter, UserDetailsServiceImpl
│       │   │   ├── user/                        # User entity, Role enum, AuthController
│       │   │   ├── booking/                     # Booking entity, enums, Repository, Service, Controller
│       │   │   ├── dispatch/                    # Phone call intake & triage controllers
│       │   │   └── tracking/                    # Driver GPS telemetry services & endpoints
│       │   └── resources/
│       │       ├── application.properties.example # Template properties file (committed to VCS)
│       │       ├── application.properties         # Real config & secrets (git-ignored, create locally)
│       │       └── static/                      # Embedded Single-Server Web GUI
│       │           ├── index.html               # Main application shell & navbar
│       │           ├── components/              # HTML Partials
│       │           │   ├── emergency.html       # Citizen booking & tracking view
│       │           │   ├── driver.html          # Driver portal & lifecycle controls
│       │           │   └── dispatcher.html      # Call center intake console
│       │           └── js/
│       │               ├── app.js               # Async component loader & orchestrator
│       │               ├── state.js             # Reactive client state & localStorage
│       │               ├── api.js               # Centralized fetch wrapper with JWT
│       │               ├── ui.js                # Notification & alert handling
│       │               └── modules/             # Business modules (emergency, tracking, driver, dispatcher)
├── frontend-web/                                # (Reserved for future standalone React/Vue clients)
├── mobile-android/                              # (Reserved for future native Android client)
├── .gitignore
├── LICENSE
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

* **Java Development Kit (JDK):** Version 21 or higher.
* **Apache Maven:** 3.8+ (or use the Maven wrapper).
* **PostgreSQL:** Version 15+ installed and running.

---

### 1. Database Setup

Open your `psql` shell or pgAdmin 4 connected as superuser `postgres`, and execute:

```sql
-- 1. Create the dedicated database
CREATE DATABASE ambulance_db;

-- 2. Create the application user
CREATE USER ambulance_user WITH ENCRYPTED PASSWORD '<YOUR_POSTGRESQL_PASSWORD_HERE>';

-- 3. Grant database and schema privileges
ALTER DATABASE ambulance_db OWNER TO ambulance_user;
\c ambulance_db postgres
ALTER SCHEMA public OWNER TO ambulance_user;
GRANT ALL ON SCHEMA public TO ambulance_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ambulance_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ambulance_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ambulance_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ambulance_user;
```

---

### 2. Configure Application Properties & Secrets

To prevent sensitive credentials (passwords, JWT signing keys) from being accidentally committed to version control, **`application.properties` is omitted from Git**. Instead, a template is provided in `application.properties.example`.

#### Step 2.1: Create your local configuration file

Copy the template file to create your active `application.properties`:

**Windows (PowerShell / Command Prompt):**
```powershell
copy backend\src\main\resources\application.properties.example backend\src\main\resources\application.properties
```

**macOS / Linux:**
```bash
cp backend/src/main/resources/application.properties.example backend/src/main/resources/application.properties
```

#### Step 2.2: Update your secrets

Open `backend/src/main/resources/application.properties` and customize your local secrets:

```properties
# Server Port
server.port=8080

# PostgreSQL Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/ambulance_db
spring.datasource.username=ambulance_user
spring.datasource.password=<YOUR_POSTGRESQL_PASSWORD_HERE>

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Super Admin User (created on first startup)
app.superadmin.email=superadmin@example.com
app.superadmin.password=<YOUR_SUPERADMIN_PASSWORD_HERE>
app.superadmin.phone=<YOUR_SUPERADMIN_PHONE_HERE>

# Registration rules
app.registration.admin-invite-code=<YOUR_ADMIN_INVITE_CODE_HERE>

# JWT Properties
app.jwt.secret=<YOUR_RANDOM_32+_CHAR_SECRET_HERE>
app.jwt.expiration-ms=86400000

# Swagger / OpenAPI
springdoc.swagger-ui.path=/swagger-ui.html
```

> **Security Note:** Ensure your root `.gitignore` contains `backend/src/main/resources/application.properties` so private passwords and keys are never pushed to public repositories.

---

### 3. Build and Run

Run from your terminal or directly inside IntelliJ IDEA:

```bash
cd backend
mvn clean spring-boot:run
```

Once started, the application is available at:

* **Web Portal (Unified GUI):** <http://localhost:8080/>
* **Swagger OpenAPI Docs:** <http://localhost:8080/swagger-ui.html>

---

## 📱 Testing on Mobile Devices (Firewall-Free)

If network firewall policies on your host PC restrict incoming LAN connections, you can test directly from a physical mobile device or emulator without modifying firewall rules:

### Option A: USB Reverse Tunneling (Physical Android Device)

1. Enable **Developer Options** and **USB Debugging** on your Android phone.
2. Connect your phone to your PC via USB cable.
3. In your PC terminal, run:
   ```bash
   adb reverse tcp:8080 tcp:8080
   ```
4. Open Chrome on your phone and browse to: `http://localhost:8080/`

### Option B: Android Studio Emulator

* Launch the Android Virtual Device (AVD).
* Open the mobile browser and visit: `http://10.0.2.2:8080/` (points to `localhost:8080` on host).

---

## 🧪 Testing with IntelliJ HTTP Client

You can run automated end-to-end API tests using `backend/test-requests.http`:

1. Open `backend/test-requests.http` in IntelliJ IDEA.
2. Run **Step 1: Anonymous Emergency Booking** (verifies zero-auth booking).
3. Run **Step 2: Register & Log in Dave Driver** (generates and captures JWT).
4. Run **Steps 3–6: Accept Trip & Transition Status** (`IN_TRANSIT` $\rightarrow$ `ARRIVED` $\rightarrow$ `COMPLETED`).
5. Run **Step 7: Push & Poll GPS Telemetry** (updates real-time coordinates).

---

## 🤝 Acknowledgments

* Designed and built as an iterative learning project exploring Java 21, Spring Boot 3, and full-stack systems engineering.
* Architectural design, code refactoring, and documentation developed with assistance from conversational AI engineering tools.