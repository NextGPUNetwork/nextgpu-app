# NextGPU

## Overview

Building the private AI compute layer for local models, workflows, and GPU-powered applications.

## Project Structure

- **common**: Houses shared promptModels, utilities, and fundamental components.
- **agent**: Desktop app (Kotlin + JetBrains Compose) used by GPU providers; can be packaged as a Windows Inno Setup installer.

## Technologies Used

- **Java 21 (Spring Boot)** - Backend framework
- **Gradle** - Dependency and build management
- **PostgreSQL** - Primary database
- **Redis** - In-memory caching
- **JUnit 5** - Comprehensive testing framework

## Getting Started

### Prerequisites

Before running the project, ensure you have:

- **Java 21** runtime
- **Gradle** for dependency management

## Running Tests

Run all test suites:

```sh
./gradlew test
```

Run module-specific tests:

```sh
./gradlew :common:test
./gradlew :core:test
./gradlew :web:test
```

## Agent Desktop (Windows Inno Setup)

The `agent` module is a JetBrains Compose Desktop app that can be packaged into a Windows executable installer using Inno Setup.

### Prerequisites

- **Windows 10/11** (Packaging is only supported on Windows)
- **Java 21 (JDK)** available on PATH
- **Inno Setup v7.x**
  - Download: https://jrsoftware.org/isdl.php
  - After installation, ensure the directory containing `iscc.exe` (e.g., `C:\Program Files\Inno Setup 7`) is on your **PATH** variable so Gradle can communicate with it.

### Build the Installer

Run from the project root in Windows PowerShell or CMD:

```bat
./gradlew.bat :agent:packageInnoSetup
```

This task will:

1. Create a distributable version of the application.
2. Stage all necessary files (executable, scripts, EULA).
3. Compile the Inno Setup script (`installer.iss`) into a final setup executable.

### Build the Runnable Jar (devs only)

```bat
./gradlew.bat :agent:clean :agent:bootJar
```

### Where to find the Installer

The installer will be generated under:

```
agent\build\innosetup-output\
```

The file name will be `NextGPU-<version>.exe` (e.g., `NextGPU-0.1.0.exe`).

### Running the Agent without packaging

For quick local runs during development:

```sh
./gradlew.bat :agent:run
```
