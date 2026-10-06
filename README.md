DeepBlue Rescue

Proyecto de persistencia y capa de servicio para DeepBlue Rescue, una plataforma para administrar el rescate y rehabilitación de fauna marina.

Desarrollado con Java 21, Spring Boot 4.1.x, Spring Data JPA, Hibernate, PostgreSQL, Flyway, MapStruct, JUnit, Mockito y AssertJ.

1. Descripción

El proyecto evolucionó desde una capa de persistencia hacia una arquitectura que incorpora una capa de servicio.

La capa de persistencia implementa entidades JPA, relaciones, repositorios, Query Methods, consultas JPQL, migraciones Flyway y pruebas de integración con PostgreSQL mediante Testcontainers.

La capa de servicio agrega DTOs, mapeo Entity ↔ DTO con MapStruct, excepciones personalizadas, reglas de negocio, transacciones y pruebas unitarias aisladas con Mockito y AssertJ.

La arquitectura principal es:

DTO / Request
      ↓
   Service
      ↓
Repository ←→ Mapper
      ↓
   Entity
      ↓
 Hibernate / JPA
      ↓
 PostgreSQL

2. Tecnologías

Java 21

Spring Boot 4.1.x

Maven

Spring Data JPA / Hibernate

PostgreSQL

Flyway

Testcontainers

JUnit

Mockito

AssertJ

MapStruct 1.6.3

3. Modelo de datos

Entidades

RescueCenter

RescueCase

Animal

MedicalRecord

Specialist

Expertise

Treatment

Relaciones principales

RescueCenter 1 RescueCase

RescueCase 1:1 Animal

Animal 1:1 MedicalRecord

Animal 1 Treatment

Specialist 1 Treatment

Specialist N Expertise

Enums

AnimalSex

RescueStatus

TreatmentType

4. Base de datos y Flyway

Flyway administra la creación y evolución del esquema. Hibernate utiliza:

ddl-auto: validate

por lo que Hibernate no crea ni modifica las tablas; únicamente valida que el mapeo de las entidades sea compatible con la base de datos.

Migraciones

V1__create_schema.sql: crea el esquema, relaciones, restricciones e índices.

V2__insert_expertise_catalog.sql: inserta el catálogo inicial de expertise.

V3__add_tracking_device_to_animal.sql: agrega tracking_device_code a animals con restricción UNIQUE.

5. Repositories

Se utilizan:

RescueCenterRepository

RescueCaseRepository

AnimalRepository

MedicalRecordRepository

SpecialistRepository

ExpertiseRepository

TreatmentRepository

Todos extienden:

JpaRepository<Entity, Long>

Esto proporciona métodos heredados como:

save

saveAll

findById

findAll

existsById

count

delete

deleteById

flush

saveAndFlush

6. Query Methods

Entre los Query Methods implementados se encuentran:

findByCode

findByCaseCode

findByStatusOrderByRescueDateAsc

findByRescueCenter_Code

findByRescueDateAfterOrderByRescueDateDesc

findByAnimalCode

findByCommonNameContainingIgnoreCase

findByRescueCase_Status

findByRescueCase_RescueCenter_Code

findByNameIgnoreCase

findByAnimal_IdOrderByPerformedAtAsc

Los Query Methods permiten expresar consultas mediante el nombre del método y también permiten navegar propiedades y relaciones.

7. Consultas JPQL

El proyecto incluye consultas JPQL para:

Buscar especialistas activos por expertise.

Buscar tratamientos realizados entre dos fechas.

Buscar tratamientos según la expertise del especialista.

Obtener animales según el estado del caso y la expertise del especialista, utilizando JOIN y DISTINCT.

Las consultas personalizadas utilizan @Query y parámetros nombrados.

El flujo es:

Repository
   ↓
JPQL
   ↓
Hibernate
   ↓
SQL
   ↓
PostgreSQL

8. Restricciones y relaciones

Se validan relaciones 1, 1:1 y N, además de restricciones reales de PostgreSQL como:

PRIMARY KEY

FOREIGN KEY

NOT NULL

UNIQUE

CHECK

También se utiliza cascade en la relación entre Animal y MedicalRecord.

9. Capa de servicio

La capa de servicio se incorpora entre la entrada de la aplicación y los repositories:

DTO
 ↓
Service
 ↓
Repository

Su responsabilidad es aplicar reglas de negocio, coordinar repositories, controlar transacciones y transformar entidades a DTOs.

Services

RescueCaseService

RescueCaseServiceImpl

TreatmentService

TreatmentServiceImpl

AnimalService

AnimalServiceImpl

Operaciones principales

RescueCaseService permite:

Buscar un caso por código.

Buscar casos por estado.

Cambiar el estado de un caso validando las transiciones permitidas.

TreatmentService permite:

Registrar tratamientos.

Consultar tratamientos por animal.

AnimalService permite:

Buscar un animal por código.

Obtener animales que se encuentran en rehabilitación.

Determinar si un animal puede recibir tratamientos.

10. DTOs y MapStruct

Los DTOs se implementan mediante record.

Request DTOs

ChangeRescueStatusRequest

CreateTreatmentRequest

Response DTOs

RescueCaseResponse

TreatmentResponse

AnimalResponse

MapStruct transforma:

Entity
  ↓
Mapper
  ↓
DTO

Mappers:

RescueCaseMapper

TreatmentMapper

AnimalMapper

Los mappers están configurados con:

@Mapper(componentModel = "spring")

11. Excepciones y reglas de negocio

Se utilizan dos excepciones personalizadas:

ResourceNotFoundException: cuando el recurso solicitado no existe.

BusinessRuleException: cuando el recurso existe, pero la operación viola una regla de negocio.

Reglas principales

Para cambiar el estado de un caso:

ADMITTED
   ↓
UNDER_EVALUATION
   ↓
IN_REHABILITATION
   ↓
READY_FOR_RELEASE
   ↓
RELEASED

No se permiten saltos de estado inválidos.

Para registrar un tratamiento:

El animal debe existir.

El especialista debe existir.

El especialista debe estar activo.

El caso no debe estar RELEASED ni CLOSED.

La fecha del tratamiento no puede ser anterior a la fecha de rescate.

12. Transacciones

Las operaciones de lectura utilizan:

@Transactional(readOnly = true)

Las operaciones que modifican información utilizan:

@Transactional

Esto se aplica principalmente a cambios de estado y registro de tratamientos.

13. Pruebas de integración

Las pruebas de persistencia utilizan Testcontainers con PostgreSQL real.

La clase principal es:

PersistenceIntegrationTest

Estas pruebas verifican:

métodos heredados de JpaRepository

relaciones 1

relaciones 1:1

relación N

cascade

Query Methods

consultas JPQL

restricciones UNIQUE

restricciones CHECK

escenario integrador

No se utiliza H2.

Para ejecutar estas pruebas, Docker debe estar activo porque Testcontainers inicia PostgreSQL en un contenedor.

14. Pruebas unitarias de Service

Las pruebas de la capa Service son unitarias y no utilizan PostgreSQL, Testcontainers ni @SpringBootTest.

Utilizan:

JUnit

Mockito

AssertJ

Clases:

AnimalServiceImplTest

RescueCaseServiceImplTest

TreatmentServiceImplTest

Los repositories y mappers se reemplazan por mocks para probar la lógica del Service de forma aislada.

Se comprueban escenarios como:

recurso existente → retorna DTO

recurso inexistente → ResourceNotFoundException

transición válida → guarda correctamente

transición inválida → BusinessRuleException y no ejecuta save

tratamiento válido → guarda correctamente

especialista inactivo → BusinessRuleException

caso RELEASED → BusinessRuleException

15. Diferencia entre pruebas de integración y pruebas unitarias

Integración

Test
 ↓
Spring
 ↓
Repository real
 ↓
Hibernate
 ↓
PostgreSQL / Testcontainers

Unitarias de Service

Test
 ↓
Service
 ↓
Repository Mock
 ↓
Mapper Mock

Las pruebas unitarias aíslan la lógica de negocio del acceso a la base de datos.

16. Estructura del proyecto

src/
├── main/
│   ├── java/com/deepblue/rescue/
│   │   ├── domain/
│   │   ├── repository/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   ├── exception/
│   │   └── service/
│   │       └── impl/
│   └── resources/
│       ├── application.yml
│       └── db/migration/
│
└── test/
    └── java/com/deepblue/rescue/
        ├── PersistenceIntegrationTest.java
        └── service/
            ├── AnimalServiceImplTest.java
            ├── RescueCaseServiceImplTest.java
            └── TreatmentServiceImplTest.java

17. Ejecución

Compilar

mvn clean compile

Ejecutar todas las pruebas

mvn clean test

Para ejecutar todas las pruebas, Docker debe estar activo porque PersistenceIntegrationTest utiliza Testcontainers.

18. Resultado de validación

La versión actual del proyecto fue validada con:

Tests run: 21
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS

Por tanto, la capa de persistencia y la nueva capa de servicio pasan correctamente las pruebas disponibles