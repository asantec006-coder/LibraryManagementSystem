# Library Management System

A complete JavaFX desktop application for managing books, members, and book loans. The project follows a layered architecture with clear separation between model, repository, service, and controller components.

## Features

- Add, view, update, and delete books
- Add, view, update, and delete members
- Issue and return books
- Search books and members
- Embedded H2 database with automatic initialization
- Clean JavaFX UI with FXML and CSS styling

## Tech Stack

- Java 25
- JavaFX 21
- Maven
- H2 Database
- JUnit 5

## Project Structure

- src/main/java/com/library/model - domain models
- src/main/java/com/library/repository - data access layer
- src/main/java/com/library/service - business logic
- src/main/java/com/library/controller - JavaFX controllers
- src/main/resources/fxml - FXML view definitions
- src/main/resources/css - styling
- src/main/resources/database - SQL schema initialization

## Build and Run

### Build

```bash
mvn clean package
```

### Run

```bash
mvn javafx:run
```

## Notes

The application uses an embedded H2 database stored locally in the project runtime directory. The database is initialized automatically when the application starts.
