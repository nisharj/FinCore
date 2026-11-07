
# GitHub Copilot Instructions

## Project Overview
Fincore is a Java Spring Boot microservices-based
financial application.

## Technology Stack
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT Authentication
- Spring Cloud Gateway
- Eureka Discovery
- OpenFeign

## 1. Think Before Coding
- Understand existing code before implementation.
- Never assume the architecture.
- Identify assumptions and trade-offs.
- Ask questions when requirements are unclear.
- Provide an implementation plan for complex tasks.

## 2. Simplicity First
- Write minimal, maintainable code.
- Avoid unnecessary abstractions.
- Don't introduce dependencies without justification.
- Follow existing coding conventions.
- Don't implement features that weren't requested.

## 3. Surgical Changes
- Modify only necessary files.
- Avoid unrelated refactoring.
- Preserve existing API contracts.
- Don't change working implementations unnecessarily.
- Remove only unused code introduced by your changes.

## 4. Goal-Driven Execution
- Define clear success criteria.
- Write or update relevant tests.
- Run tests after implementation.
- Verify API responses and error handling.
- Report test results honestly.

## 5. Security
- Never expose passwords, secrets or API keys.
- Validate incoming requests.
- Follow Spring Security best practices.
- Enforce role-based authorization.
- Protect sensitive financial operations.

## 6. Microservices
- Respect existing service boundaries.
- Each service owns its database.
- Use Feign clients for inter-service communication
  where appropriate.
- Follow existing API Gateway routing.
- Handle service failures properly.

## 7. Database
- Follow existing entity and repository patterns.
- Use transactions for critical operations.
- Avoid unintended schema changes.
- Maintain migration compatibility.

## 8. Code Quality
- Follow Java naming conventions.
- Use constructor injection.
- Avoid duplicated business logic.
- Prefer meaningful exception handling.
- Maintain readable and testable code.
