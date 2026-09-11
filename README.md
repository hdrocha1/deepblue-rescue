# DeepBlue Rescue — Persistence Lab

Proyecto de persistencia para DeepBlue Rescue, desarrollado con Java 21, Spring Boot, Spring Data JPA, PostgreSQL y Flyway. Las pruebas de integración utilizan Testcontainers con PostgreSQL.

## 1. Descripción

El proyecto implementa únicamente la capa de persistencia del sistema de rescate de fauna marina. Se trabajan entidades JPA, relaciones, repositorios, Query Methods, consultas JPQL, migraciones Flyway y pruebas de integración.

## 2. Tecnologías

- Java 21
- Spring Boot 4.1.x
- Maven
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- JUnit
- Testcontainers

## 3. Modelo de datos

Entidades:

- RescueCenter
- RescueCase
- Animal
- MedicalRecord
- Specialist
- Expertise
- Treatment

Relaciones principales:

- RescueCenter 1:N RescueCase
- RescueCase 1:1 Animal
- Animal 1:1 MedicalRecord
- Animal 1:N Treatment
- Specialist 1:N Treatment
- Specialist N:M Expertise

## 4. Base de datos y Flyway

Flyway administra la creación y evolución del esquema. Hibernate utiliza `ddl-auto: validate`, por lo que no crea ni modifica las tablas.

Migraciones:

- `V1__create_schema.sql`: crea el esquema, relaciones, restricciones e índices.
- `V2__insert_expertise_catalog.sql`: inserta el catálogo inicial de expertise.
- `V3__add_tracking_device_to_animal.sql`: agrega `tracking_device_code` a `animals` con restricción `UNIQUE`.

## 5. Repositories

Se utilizan:

- RescueCenterRepository
- RescueCaseRepository
- AnimalRepository
- MedicalRecordRepository
- SpecialistRepository
- ExpertiseRepository
- TreatmentRepository

Todos extienden `JpaRepository<Entity, Long>`.

## 6. Query Methods

Entre los Query Methods implementados se encuentran:

- `findByCode`
- `findByCaseCode`
- `findByStatusOrderByRescueDateAsc`
- `findByRescueCenter_Code`
- `findByRescueDateAfterOrderByRescueDateDesc`
- `findByAnimalCode`
- `findByCommonNameContainingIgnoreCase`
- `findByRescueCase_Status`
- `findByRescueCase_RescueCenter_Code`
- `findByNameIgnoreCase`
- `findByAnimal_IdOrderByPerformedAtAsc`

También se utilizan los métodos heredados de `JpaRepository`, como `save`, `saveAll`, `findById`, `existsById`, `count`, `delete`, `deleteById`, `flush` y `saveAndFlush`.

## 7. Consultas JPQL

El proyecto incluye consultas JPQL para:

- Buscar especialistas activos por expertise.
- Buscar tratamientos realizados entre dos fechas.
- Buscar tratamientos según la expertise del especialista.
- Obtener animales según el estado del caso y la expertise del especialista, utilizando `JOIN` y `DISTINCT`.

Las consultas personalizadas utilizan `@Query` y parámetros nombrados.

## 8. Relaciones y restricciones

Se validan relaciones 1:N, 1:1 y N:M, además de restricciones reales de PostgreSQL como `NOT NULL`, `UNIQUE`, `CHECK` y claves foráneas.

## 9. Pruebas de integración

Las pruebas utilizan Testcontainers y PostgreSQL real. No se utiliza H2.

Para ejecutar las pruebas:

```bash
mvn clean test
```

Docker debe estar activo para que Testcontainers pueda iniciar PostgreSQL.

## 10. Estructura

```text
src/
├── main/
│   ├── java/com/deepblue/rescue/
│   │   ├── domain/
│   │   └── repository/
│   └── resources/
│       ├── application.yml
│       └── db/migration/
│
└── test/
    └── java/com/deepblue/rescue/
```

## 11. Resultado esperado

La validación del proyecto se realiza mediante las pruebas de integración y debe finalizar con:

```text
BUILD SUCCESS
```
