# DOCUMENTO TÉCNICO DE ARQUITECTURA
## TRABAJO FINAL - PROGRAMACIÓN DISTRIBUIDA Y CONCURRENTE (PDyC 2026)
### Sistema de Gestión de Eventos Masivos y Red Social ("Greater Events Microservices")

**Universidad Nacional del Noroeste de la Provincia de Buenos Aires (UNNOBA)**  
**Carrera:** Ingeniería en Informática / Licenciatura en Sistemas  
**Asignatura:** Programación Distribuida y Concurrente (PDyC)  
**Cursada:** 2026  
**Integrantes:** Bruno Bertacchini, Lucas Lovizzio (Grupo de 2 integrantes)  
**Mejoras Seleccionadas:** 
- **Opción E:** Contenerización Completa (Docker Compose)
- **Opción F:** Calidad y Pruebas Automatizadas de Integración

---

## 1. INTRODUCCIÓN Y OBJETIVOS

El presente documento técnico describe la arquitectura, diseño e implementación de la plataforma **Greater Events**, un sistema distribuido de microservicios concebido para la gestión de catálogo de eventos culturales/musicales, interacción social de usuarios (seguimiento de artistas y favoritos), gestión centralizada de identidad y notificaciones en tiempo real ante eventualidades (cancelación de eventos).

A partir de la base desarrollada durante la cursada, el grupo de 2 integrantes ha evolucionado el sistema mediante dos mejoras principales:
1. **Opción E (Contenerización Total):** Empaquetado de los 6 microservicios Spring Boot mediante Dockerfiles optimizados y su orquestación integral en `docker-compose.yml` junto con la infraestructura subyacente (PostgreSQLs, Keycloak, RabbitMQ).
2. **Opción F (Calidad / Pruebas Automatizadas):** Diseño y ejecución de una suite completa de pruebas automatizadas de integración (`@SpringBootTest`) utilizando base de datos en memoria (H2) y mocks de mensajería/gRPC, verificando la lógica de negocio, reglas de dominio y resiliencia de servicios.

---

## 2. ARQUITECTURA GENERAL DEL SISTEMA

El sistema adopta una arquitectura orientada a microservicios sobre la pila **Spring Cloud (2024.0.1)** y **Spring Boot (3.4.5)** ejecutándose sobre **Java 17**.

### Diagrama de Arquitectura y Flujo de Componentes

```
                                      +-------------------+
                                      |   Eureka Server   |
                                      |     (8761)        |
                                      +---------+---------+
                                                ^
                                                | Registro / Lookup
+---------------+  JWT Bearer   +---------------+---------------+
| Client / HTTP | ------------> |          API Gateway          |
+---------------+               |             (8080)            |
                                +---------------+---------------+
                                                |
                 +------------------------------+------------------------------+
                 | (HTTP REST TokenRelay)       | (HTTP REST TokenRelay)       | (HTTP REST TokenRelay)
                 v                              v                              v
    +------------------------+     +------------------------+     +------------------------+
    |    catalog-service     |     |  user-social-service   |     |  notification-service  |
    |         (8082)         |     |      (8083/9091)       |     |         (8085)         |
    +-----------+------------+     +-----------+------------+     +-----------+------------+
                |                              ^                              ^
                | RabbitMQ Event               |                              |
                | ("event.cancelled")          +----- gRPC (UserSocial) ------+
                v                                (Consulta de seguidores/favs)
         +--------------+
         |   RabbitMQ   | ----------------------------------------------------+
         +--------------+
```

---

## 3. COMPONENTES Y RESPONSABILIDADES

El ecosistema está fragmentado en **8 módulos Maven** independientes:

| Módulo / Servicio | Puerto Host | Responsabilidad Principal | Tecnologías Clave |
| :--- | :--- | :--- | :--- |
| **`grpc-api`** | N/A (Librería) | Definición centralizada de esquemas Protobuf (`user_social.proto`) y código gRPC generado. | Protobuf 3, gRPC Java Stub |
| **`config-server`** | `8888` | Proveedor centralizado de configuración nativa (`classpath:/config`) para todos los microservicios. | Spring Cloud Config Server |
| **`eureka-server`** | `8761` | Servidor de registro y descubrimiento dinámico de servicios para el enrutamiento y balanceo de carga. | Spring Cloud Netflix Eureka |
| **`api-gateway`** | `8080` | Punto de entrada único (Single Entry Point), enrutamiento reactivo y propagación de JWT (`TokenRelay`). | Spring Cloud Gateway (WebFlux) |
| **`catalog-service`** | `8082` | Gestión del catálogo de artistas y eventos. Control de ciclo de vida del evento y emisor de eventos RabbitMQ. | Spring Data JPA, RabbitTemplate |
| **`user-social-service`**| `8083` / `9091` | Registro de usuarios en Keycloak, seguimiento de artistas, eventos favoritos y servidor gRPC. | OpenFeign, gRPC Server, Keycloak Client |
| **`notification-service`**| `8085` | Consumidor asíncrono de eventos de cancelación, cliente gRPC de consulta y gestor de notificaciones. | RabbitListener, gRPC Client |
| **Keycloak IAM** | `8084` | Servidor de autenticación OAuth2 / OpenID Connect (Realm `unnoba`). | Keycloak Docker Container |

---

## 4. PATRONES DE PROGRAMACIÓN DISTRIBUIDA Y DECISIONES DE DISEÑO

### 4.1 Patrón *Database per Service*
Cada microservicio gestiona su propia base de datos física aislada en contenedores PostgreSQL independientes:
* **`catalog-service`** $\rightarrow$ `greater_catalog` (Tablas: `artists`, `events`, `event_artists`).
* **`user-social-service`** $\rightarrow$ `greater_users` (Tablas: `users`, `user_followed_artists`, `user_favorite_events`).
* **`notification-service`** $\rightarrow$ `greater_notifications` (Tabla: `notifications`).

*Justificación:* Evita acoplamiento a nivel de esquema, previene bloqueos compartidos y permite escalar cada motor de base de datos de manera independiente.

### 4.2 API Gateway & Token Relay Pattern
Todas las peticiones del exterior ingresan por el **API Gateway** en el puerto `8080`. El gateway no solo enruta según el *path*, sino que mediante el filtro **`TokenRelay`** adjunta el encabezado `Authorization: Bearer <JWT>` en la comunicación downstream hacia los microservicios de negocio. Cada microservicio actúa como un **OAuth2 Resource Server** que valida la firma del token y extrae los roles (`ROLE_admin`) mediante `KeycloakJwtGrantedAuthoritiesConverter`.

### 4.3 Comunicación Inter-Servicio Híbrida: Sincrónica (Feign) vs Asincrónica (RabbitMQ) vs gRPC

1. **OpenFeign (HTTP REST Sincrónico):** `user-social-service` utiliza `@FeignClient(name = "catalog-service")` para obtener detalles de artistas y eventos requeridos en las vistas del usuario (`/me/following`, `/me/favorite-events`).
2. **RabbitMQ (AMQP Asincrónico Event-Driven):** Al cancelar un evento en `catalog-service`, se emite una notificación de cancelación no bloqueante a la *topic exchange* `events.exchange` con routing key `event.cancelled`. Esto desacopla completamente el catálogo del sistema de notificaciones.
3. **gRPC (RPC Binario de Alto Rendimiento):** Al recibir la notificación en `notification-service`, este consulta de forma sincrónica e hiper-rápida a `user-social-service` a través del puerto gRPC `9091` (`GetFollowersByArtistId` y `GetFavoritersByEventId`), reduciendo latencia y consumo de ancho de banda respecto a REST/JSON tradicional.

---

## 5. EVOLUCIÓN DEL SISTEMA – MEJORAS INCORPORADAS

### 5.1 Opción E – Contenerización Completa (Docker Compose)
Se desarrollaron `Dockerfile` livianos basados en la imagen oficial `eclipse-temurin:17-jre-alpine` para los 6 microservicios de la solución:
* `config-server/Dockerfile`
* `eureka-server/Dockerfile`
* `api-gateway/Dockerfile`
* `catalog-service/Dockerfile`
* `user-social-service/Dockerfile`
* `notification-service/Dockerfile`

Se actualizó `docker-compose.yml` para orquestar los 11 contenedores del ecosistema:
* **Infrastructure Services:** Keycloak, RabbitMQ (con UI de administración en `15672`), PostgreSQL Catalog (`5433`), PostgreSQL Users (`5434`), PostgreSQL Notifications (`5435`).
* **Healthchecks:** Configurados mediante `pg_isready` y `rabbitmq-diagnostics ping` para garantizar que las bases de datos y el broker estén 100% operativos antes de iniciar los microservicios (`depends_on` con `condition: service_healthy`).
* **Red:** Red tipo puente (`bridge`) aislada llamada `greater-events-net`.

### 5.2 Opción F – Calidad y Pruebas Automatizadas (Unitarias y de Integración)
Se construyó una suite integral de pruebas automatizadas combinando **Unit Tests puros (JUnit 5 + Mockito)** para la lógica de negocio y **Integration Tests (@SpringBootTest + H2)** para la persistencia y ciclo de vida de los microservicios (40 pruebas automatizadas en total):

1. **Pruebas Unitarias Aisladas (Mockito puro, sin sobrecarga de contexto Spring):**
   * **`ArtistServiceTest`** (8 tests): Validación de nombres y géneros, listado y filtrado, prohibición de edición de artistas con eventos asignados, borrado físico vs desactivación lógica (`active = false`).
   * **`EventServiceTest`** (10 tests): Ciclo de vida estricto de eventos, validación de fechas, impedimento de mutaciones en estados no tentativos, validación de artistas inactivos, reprogramación (`reschedule`), cancelación y verificación de la emisión a RabbitMQ.
   * **`UserServiceTest`** (8 tests): Seguimiento y desuscripción de artistas (`follow`/`unfollow`), guardado y desmarcado de favoritos con validación de estados permitidos (`CONFIRMED`/`RESCHEDULED`) y fechas futuras, manejo de fallos Feign.
   * **`NotificationServiceTest`** (4 tests): Aislamiento y seguridad por usuario (lanzamiento de `AccessDeniedException` si un usuario intenta alterar notificaciones ajenas).
   * **`EventCancelledListenerTest`** (2 tests): Manejo del mensaje AMQP, consulta gRPC y desduplicación estricta de usuarios afectados.

2. **Pruebas de Integración con Spring Boot y H2 en Memoria:**
   * **`CatalogServiceIntegrationTest`** (5 tests): Integración JPA, base de datos H2 en memoria y flujo transaccional.
   * **`UserSocialServiceIntegrationTest`** (2 tests): Integración de colecciones JPA (`user_followed_artists`, `user_favorite_events`).
   * **`NotificationServiceIntegrationTest`** (1 test): Integración end-to-end de recepción de mensaje, resolución gRPC y persistencia.

*Resultado:* Ejecución exitosa en Maven (`mvn clean test`) con **100% de éxito en todos los módulos**.

---

## 6. GUÍA DE DESPLIEGUE Y EJECUCIÓN PASO A PASO

### Prerrequisitos
* Java 17+ instalado.
* Docker Desktop 4.+ y Docker Compose v2.+ activos.

### Paso 1: Compilación de Código y Pruebas Automatizadas
Ejecutar en la raíz del proyecto:
```bash
mvn clean test
```
*Esto compilará todos los prototipos gRPC, ejecutará los tests de integración en memoria y generará los artefactos JAR.*

### Paso 2: Generación de Artefactos JAR
```bash
mvn clean package -DskipTests
```

### Paso 3: Despliegue con Docker Compose
```bash
docker compose up --build -d
```

### Paso 4: Verificación de Contenedores y Salud
```bash
docker compose ps
```

### URLs de Acceso a Servicios
* **API Gateway:** `http://localhost:8080`
* **Eureka Dashboard:** `http://localhost:8761`
* **Keycloak Admin Console:** `http://localhost:8084` (User/Pass: `admin`/`admin`)
* **RabbitMQ Management Console:** `http://localhost:15672` (User/Pass: `guest`/`guest`)

---

## 7. PREPARACIÓN PARA LA DEFENSA ORAL (20 MINUTOS)

### Preguntas Típicas de la Mesa Examinadora y Respuestas Modelo

1. **¿Por qué utilizaron gRPC entre Notification Service y User Social Service en lugar de REST/Feign?**
   * *Respuesta:* La consulta de seguidores y usuarios que marcaron evento favorito ocurre durante el procesamiento de un evento en tiempo real. gRPC utiliza HTTP/2 multiplexado y serialización binaria (Protobuf), lo que reduce drásticamente el tamaño del payload y la latencia en comparación con JSON sobre HTTP/1.1.
2. **¿Cómo garantizan la consistencia eventual cuando se cancela un evento?**
   * *Respuesta:* Aplicamos el patrón Event-Driven. `catalog-service` actualiza su estado local en su transacción de base de datos y publica un mensaje a RabbitMQ. `notification-service` escucha la cola y procesa las notificaciones de manera asíncrona. Si el servicio de notificaciones está temporalmente caído, las notificaciones permanecen seguras en la cola persistente de RabbitMQ.
3. **¿Cómo funciona la autenticación distribuida a través del Gateway?**
   * *Respuesta:* El usuario obtiene su token JWT en Keycloak (`/realms/unnoba`). Luego envía cada petición al Gateway con el encabezado `Authorization: Bearer <token>`. El Gateway utiliza el filtro `TokenRelay` para propagar de manera transparente este token a los microservicios downstream, los cuales validan la firma con la clave pública de Keycloak (`jwk-set-uri`).

---

## 8. CONCLUSIONES Y TRABAJO FUTURO

La arquitectura lograda cumple rigurosamente con los requisitos de un sistema distribuido moderno, tolerante a fallos, desacoplado y de alto rendimiento. Como líneas de trabajo futuro se propone:
1. Implementar observabilidad distribuida con Spring Boot Actuator, Prometheus y Grafana (Opción D).
2. Incorporar Circuit Breakers con Resilience4j en las llamadas Feign para degradación elegante de servicio (Opción C).
