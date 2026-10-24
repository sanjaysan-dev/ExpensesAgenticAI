# 💰 Expense AI Chatbot

An AI-powered expense tracking web application built with **Java, Spring Boot, React, Tailwind CSS, and MySQL**. The application helps users record expenses through a chat interface, organize spending by category, and review their expense history.

WebSite Link : https://expensesagenticai-backend.onrender.com/

## 🚀 Features

- **User Authentication** — User registration and login.
- **Expense Tracking** — Record expenses with amounts, descriptions, categories, and dates.
- **Conversational Interface** — Enter expenses using natural-language messages.
- **Automatic Expense Categorization** — Identify expense details and categorize supported transactions.
- **Chat History** — Create conversations and retrieve previous messages.
- **Expense History** — Retrieve expenses within a selected date range.
- **Category-wise Tracking** — Organize expenses into categories such as Food, Travel, and Shopping.
- **Monthly Expense Analysis** — Support expense summaries based on date and category.
- **Database Persistence** — Store user accounts, conversations, messages, and expenses in MySQL.
- **Multi-user Support** — Keep each user's application data associated with their account.

## 🛠️ Tech Stack

| Layer | Technologies |
|---|---|
| Frontend | React, Tailwind CSS, JavaScript |
| Backend | Java 17, Spring Boot |
| API | REST APIs |
| Database | MySQL |
| ORM | Spring Data JPA, Hibernate |
| Authentication | Spring Security, BCrypt password hashing |
| Build Tool | Maven |
| Database Management | MySQL Workbench |
| Deployment | Vercel / Render / Google Cloud Run / Aiven, depending on configured services |

## 🏗️ Architecture

```text
                  User
                   |
                   v
          React + Tailwind CSS
               Frontend
                   |
                REST API
                   |
                   v
           Spring Boot Backend
                   |
          +--------+--------+
          |        |        |
          v        v        v
        Users  Conversations Messages
                   |
                   v
               Expenses
                   |
                   v
               MySQL DB
```

## 📂 Project Modules

### Frontend
- Registration and login screens
- Expense chatbot interface
- Conversation and message history
- Expense entry and tracking interface

### Backend
- Authentication APIs
- User management
- Conversation management
- Message management
- Expense management
- Expense parsing and categorization
- Date-range and category-based expense retrieval

### Database

The application uses the following main tables:

| Table | Purpose |
|---|---|
| `users` | Stores user account information |
| `conversations` | Stores chat conversation details |
| `messages` | Stores conversation messages |
| `expenses` | Stores expense records |

## ⚙️ Getting Started

### Prerequisites

Install the following software:

- Java 17 or compatible configured JDK
- Node.js and npm
- MySQL Server
- Git
- Maven, or use the included Maven Wrapper if available

### 1. Clone the Repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
cd YOUR_PROJECT_FOLDER
```

Replace the placeholders with your actual repository URL and folder name.

### 2. Configure the Database

Create the MySQL database:

```sql
CREATE DATABASE expense_ai;
```

Configure the database connection in the backend using environment variables.

Example `application.yml` configuration:

```yaml
spring:
  datasource:
    url: "jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslMode=REQUIRED"
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
```

For local MySQL, configure `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` with your local database values. For hosted MySQL, use the credentials supplied by your database provider.

**Security:** Never commit database passwords, API keys, or other secrets to GitHub.

### 3. Run the Backend

Open a terminal in the Spring Boot backend directory.

On Windows, if the project includes Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Alternatively, build the project:

```powershell
.\mvnw.cmd clean package
```

The backend will start on the configured Spring Boot port, typically `8080` unless changed in the application configuration.

### 4. Run the Frontend

Open another terminal in the React frontend directory:

```bash
npm install
npm run dev
```

Open the local URL displayed by the frontend development server, commonly `http://localhost:5173`.

Configure the frontend API base URL to point to the running backend.

## 🌐 Deployment

The application can be deployed using separate hosting services:

- **Frontend:** Vercel or another static frontend host.
- **Backend:** Render, Google Cloud Run, or another Java-compatible hosting platform.
- **Database:** Aiven MySQL or another hosted MySQL provider.

Set the backend environment variables in the hosting platform and configure the frontend with the public backend API URL.

For cross-origin authentication, configure CORS and cookie settings appropriately. Use HTTPS in production.

Free-tier availability, resource limits, and billing conditions depend on the selected hosting provider.

## 🔐 Security Considerations

- Passwords are hashed using BCrypt.
- Database credentials should be stored in environment variables.
- Production endpoints should use HTTPS.
- Configure CORS for trusted frontend origins only.
- Ensure users can access only their own conversations and expenses.
- Use secure session cookies and appropriate cross-origin settings when required.

## 🎯 Future Enhancements

- Interactive spending charts and dashboards
- Monthly budgets and spending alerts
- Downloadable expense reports
- Improved natural-language expense extraction
- More detailed financial insights
- Responsive UI enhancements

## 👨‍💻 Author

**Sanjay S**

Software Developer | Java | Spring Boot | React | MySQL

---

⭐ If you find this project useful, consider giving the repository a star.

*Built to make personal expense tracking simpler through a conversational interface.*
