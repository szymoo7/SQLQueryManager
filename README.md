# Project Overview

**JetbrainsTask** is a Spring Boot application designed to manage and execute SQL queries.  
It supports both synchronous and asynchronous execution, caching of results, and tracking of query statuses.  
The project demonstrates efficient query handling and background execution management

You can view the full project source code here:  
[https://github.com/szymoo7/JetbrainsTask](https://github.com/szymoo7/JetbrainsTask)

---
## How to Get the Code

To download the project from GitHub, open a terminal and run:

```bash
git clone https://github.com/szymoo7/JetbrainsTask.git

cd JetbrainsTask
````

---

## How to Run the Application

### 1. Running the Application from a JAR

1. **Build the project without running tests**
   In the project root directory, run:

   ```bash
   mvn clean package -DskipTests
   ```

   This command will create a JAR file inside the `target/` directory.

2. **Run the JAR file**
   Use the following command to start the application:

   ```bash
   java -jar target/JetbrainsTask-0.0.1-SNAPSHOT.jar
   ```

3. **Access the application**
   Once the application is running, open your browser or API client and visit:

   ```
   http://localhost:8080
   ```

---

### 2. Running the Application with Docker

Use the **Docker branch** version when running the application in a container,  
as the database path configuration differs from the master branch.

1. **Switch to the Docker branch**

   ```bash
   git checkout docker
   ```

2. **Build the Docker image**

   ```bash
   docker build -t jetbrainstask .
   ```

3. **Run the Docker container**

   ```bash
   docker run -p 8080:8080 jetbrainstask
   ```

4. **Access the application**

   After the container starts, the application will be available at:

   ```
   http://localhost:8080
   ```

---
## Database Structure

The application uses an in-memory H2 database preloaded with the Titanic passengers dataset.  
The dataset is represented by a single table named `titanic`, which stores basic passenger information.

### Table Definition

```sql
CREATE TABLE titanic (
    PassengerId INT PRIMARY KEY,
    Survived BOOLEAN,
    Pclass INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Sex VARCHAR(10) CHECK (Sex IN ('male', 'female')),
    Age DECIMAL(3,1),
    SibSp INT,
    Parch INT,
    Ticket VARCHAR(50),
    Fare DECIMAL(8,4),
    Cabin VARCHAR(16),
    Embarked CHAR(1) CHECK (Embarked IN ('C', 'Q', 'S'))
);
```
---


## REST API Endpoints

### `POST /queries`
Adds one or more SQL queries to the queue.  
Multiple queries can be separated by a semicolon (`;`).  
Returns a list of query IDs assigned to the submitted queries.


### `GET /queries`
Returns a list of all currently queued queries with their IDs, SQL text, and current status.


### `GET /execute?query={id}`
Executes a query with the specified `id`.  
Depending on the query type and internal analyzer rules, it may run synchronously or asynchronously.  
Returns the query result immediately (for synchronous queries) or a placeholder indicating that the query is running (for asynchronous queries).


### `GET /execute/{id}`
Retrieves the result of an asynchronous query after it has completed.  
If the query is still running, it returns a status message indicating that execution is in progress.


### Expected Responses

* `RUNNING` – the query is still being processed asynchronously  
* `COMPLETED` – the query execution finished successfully  
* `FAILED` – the query failed during execution or an invalid ID was provided  
* `TO_BE_SEEN` – the query has been detected as asynchronous, and its result will be available for retrieval via `/execute/{id}`
* `READY` – the query is ready to execute 
