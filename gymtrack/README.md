# GymTrack - Gym Membership and Attendance Management System

Java 17 + Spring Boot 3 + Spring Data JPA + Thymeleaf. Uses an H2 file database by default (no setup needed).

## Run
```
mvn spring-boot:run
```
Open http://localhost:8080

To use MySQL, edit `src/main/resources/application.properties` (instructions inside).

## Modules
1. Members: add, edit, search, delete
2. Plans: Monthly / Quarterly / Yearly seeded on first run, add your own
3. Payments & renewals: renewing early keeps the remaining days
4. Attendance: check-in / check-out, monthly summary per member
5. Dashboard: totals, expiring in 7 days, expired members

## Core Java concepts used
OOP and encapsulation, enums with fields and static factory methods, records, custom exceptions,
Collections (Map, List, Optional), Streams and Collectors, Date/Time API (LocalDate, Duration, YearMonth),
BigDecimal for money, layered architecture (model / repository / service / controller).
