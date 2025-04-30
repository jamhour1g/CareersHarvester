# CareersHarvester

CareersHarvester is a Kotlin-based job aggregation system that collects job postings from various providers and organizes them into a unified structure.
The project is designed to be modular, extensible, and efficient, leveraging Kotlin coroutines and serialization for asynchronous operations and data handling.

## Features

- **Job Providers**: Integrates with multiple job providers such as AsalTech, Harri, Userpilot, Foothill, Jobs.ps, and Foras.
- **Caching**: Implements a caching mechanism with expiry using `SuspendingLazyWithExpiry` to reduce redundant API calls.
- **Asynchronous Operations**: Uses Kotlin coroutines for non-blocking job fetching and processing.
- **Custom Serialization**: Supports custom serializers for handling complex data types like `URI`, `ZonedDateTime`, and `LocalDate`.
- **Logging**: Provides configurable logging utilities for both console and file outputs.
- **Extensibility**: Easily add new job providers by extending the `AbstractJobsProvider` class.

## Project Structure

The project is organized into the following modules:

### 1. **Core**
Contains the core abstractions and implementations for jobs, job posters, and job providers.

- **Key Classes**:
  - `AbstractJobsProvider`: Base class for all job providers.
  - `Job`: Represents a job posting.
  - `JobPoster`: Represents the entity posting the job.
  - `JobProviderStatus`: Enum for tracking the status of job providers.

### 2. **Providers**
Implements specific job providers by extending the core abstractions.

- **Implemented Providers**:
  - `AsalTech`
  - `Harri`
  - `Userpilot`
  - `Foothill`
  - `Jobs.ps`
  - `Foras`

### 3. **Util**
Provides utility classes and functions for logging, HTTP requests, and serialization.

- **Key Utilities**:
  - `SuspendingLazyWithExpiry`: A caching mechanism with expiry.
  - `LoggingUtils`: Configurable logging setup.
  - `HttpUtils`: Helper functions for making HTTP requests.
  - `Serializers`: Custom serializers for `URI`, `ZonedDateTime`, and `LocalDate`.

# Logging
Logging is configurable via environment variables:

- `ENABLE_CONSOLE_LOGGING`: Set to true to enable console logging.
- `ENABLE_FILE_LOGGING`: Set to true to enable file logging.

Log files are stored in the logs/ directory.

### Prerequisites
- **Java Development Kit (JDK)**: Version 21 or later.
- **Gradle**: Version 8.5 or later.
- **Kotlin**: Version 2.0.20.