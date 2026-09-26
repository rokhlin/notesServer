# Notes Server (`notesServer`)

Backend REST API for **NotesAlltogether**, built with **Kotlin** and **Ktor Server**.

## 🚀 Features

- **Ktor 3** asynchronous server engine (Netty)
- **JSON Serialization** via `kotlinx.serialization`
- **CORS** configured for Web client access
- **RESTful Endpoints** for managing notes:
  - `GET /` - Service status
  - `GET /health` - Health check endpoint
  - `GET /api/notes` - List all notes (ordered by recent update)
  - `GET /api/notes/{id}` - Get note by ID
  - `POST /api/notes` - Create a new note
  - `PUT /api/notes/{id}` - Update an existing note
  - `DELETE /api/notes/{id}` - Delete note by ID
- **Automated Tests** using `ktor-server-test-host`

---

## 🛠️ Requirements

- **JDK 17** or higher
- Git

---

## 💻 Running the Server Locally

### On Windows:
```powershell
.\gradlew.bat run
```

### On macOS / Linux:
```bash
./gradlew run
```

By default, the server will start on `http://0.0.0.0:8080`.

---

## 🧪 Running Tests

```powershell
.\gradlew.bat test
```

---

## 📦 Building Production JAR

```powershell
.\gradlew.bat build
```
The runnable distribution/JAR will be located in `build/libs/`.

---

## 📤 Publishing to GitHub

To push this repository to GitHub:

1. Create a new empty repository on [GitHub](https://github.com/new), named e.g. `notes-server`.
2. Link remote and push:
```bash
git remote add origin https://github.com/<YOUR_USERNAME>/notes-server.git
git branch -M main
git push -u origin main
```
