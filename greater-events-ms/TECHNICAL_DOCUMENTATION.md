# DOCUMENTO TÉCNICO DE ARQUITECTURA Y DISEÑO
## TRABAJO FINAL - PROGRAMACIÓN DISTRIBUIDA Y CONCURRENTE (PDyC 2026)
### Plataforma Distribuida de Gestión de Eventos y Red Social ("Greater Events Microservices")

---

**Institución:** Universidad Nacional del Noroeste de la Provincia de Buenos Aires (UNNOBA)  
**Unidad Académica:** Escuela de Tecnología  
**Carrera:** Ingeniería en Informática  
**Asignatura:** Programación Distribuida y Concurrente (PDyC)  
**Ciclo Lectivo:** Cursada 2026  
**Modalidad:** Individual  
**Alumno:** Bruno Bertacchini  
**Mejora de Evolución Seleccionada:**  
* **Opción F – Calidad:** Estrategia Integral de Pruebas Automatizadas (Unitarias Puras con Mockito y de Integración con JUnit 5 y H2)

---

## 1. INTRODUCCIÓN Y ALCANCE DEL SISTEMA

### 1.1 Contexto y Problemática Distribuida
Los sistemas modernos de catálogo de espectáculos masivos y redes sociales asociadas enfrentan desafíos críticos de concurrencia, disponibilidad, escalabilidad y consistencia de datos. En las arquitecturas monolíticas tradicionales, los picos de demanda generados por anuncios de venta de entradas o cancelaciones masivas degradan el rendimiento general del sistema, produciendo bloqueos en bases de datos compartidas y acoplando dominios disímiles como la gestión administrativa del catálogo, las interacciones sociales de los usuarios y el despacho de notificaciones.

**Greater Events Microservices** es una plataforma distribuida concebida para resolver esta problemática, desacoplando los dominios funcionales en microservicios autónomos que se comunican mediante protocolos especializados (HTTP/REST sincrónico, mensajería asíncrona AMQP sobre RabbitMQ y llamadas remotas binarias de alta velocidad con gRPC sobre HTTP/2).

### 1.2 Requerimientos del Sistema
#### Funcionales:
1. **Catálogo de Artistas y Eventos:** Alta, modificación, control de ciclo de vida del evento (`TENTATIVE`, `CONFIRMED`, `RESCHEDULED`, `CANCELLED`), asignación y desvinculación de artistas.
2. **Interacción Social:** Capacidad de los usuarios autenticados de seguir/dejar de seguir artistas, guardar eventos como favoritos y consultar la cartelera de sus artistas seguidos.
3. **Identidad y Control de Acceso:** Autenticación centralizada con protocolo OpenID Connect / OAuth2 sobre Keycloak y control de acceso basado en roles (`ROLE_admin` para la gestión administrativa).
4. **Notificaciones Automatizadas:** Despacho automatizado, desacoplado y confiable de notificaciones a todos los usuarios interesados ante la cancelación de un evento musical.

#### No Funcionales (Atributos de Calidad Distribuida):
1. **Desacoplamiento Temporal:** La cancelación de un evento no debe bloquearse esperando el procesamiento de notificaciones a miles de usuarios.
2. **Aislamiento de Fallos y Datos (*Database per Service*):** La indisponibilidad de una base de datos o servicio no debe comprometer la operatividad de los demás módulos.
3. **Baja Latencia Inter-Servicio:** Consultas internas de alto volumen deben resolverse con protocolos binarios de alta eficiencia (gRPC sobre HTTP/2).
4. **Portabilidad y Despliegue:** Toda la infraestructura y servicios deben ser reproducibles en cualquier entorno mediante contenedores Docker.
5. **Calidad y Verificabilidad Rigurosa:** Cobertura de pruebas automatizadas que garanticen la solidez de las reglas de negocio, el aislamiento de fallos de red y los contratos de integración.

---

## 2. ARQUITECTURA GENERAL Y TOPOLOGÍA DEL SISTEMA

El sistema adopta una arquitectura orientada a microservicios sobre la pila **Spring Cloud 2024.0.1** y **Spring Boot 3.4.5**, ejecutada sobre el entorno de ejecución **Java 17**.

### 2.1 Diagrama de Arquitectura Global

```
                                      +-------------------------------+
                                      |         Eureka Server         |
                                      |     (Service Discovery 8761)  |
                                      +---------------+---------------+
                                                      ^
                                                      | Heartbeat & Registro
+--------------------+  JWT Bearer   +----------------+---------------+
| Cliente Web / REST | ------------> |          API Gateway           |
+--------------------+               |   (Spring Cloud Gateway 8080)  |
                                     +----------------+---------------+
                                                      |
                  +-----------------------------------+-----------------------------------+
                  | (HTTP REST + TokenRelay)          | (HTTP REST + TokenRelay)          | (HTTP REST + TokenRelay)
                  v                                   v                                   v
    +---------------------------+       +---------------------------+       +---------------------------+
    |      catalog-service      |       |    user-social-service    |       |   notification-service    |
    |        (Port 8082)        |       |    (8083 REST / 9091 gRPC)|       |        (Port 8085)        |
    +-------------+-------------+       +-------------+-------------+       +-------------+-------------+
                  |                                   ^                                   ^
                  | 1. Publica Evento                 |                                   |
                  |    AMQP                           | 3. Consulta gRPC                  | 2. Consume Evento
                  v                                   |    (Followers / Favoriters)       |    AMQP
         +-----------------+                          +-----------------------------------+
         |    RabbitMQ     | -------------------------------------------------------------+
         | (Topic Exchange)|
         +-----------------+

                  |                                   |                                   |
                  v                                   v                                   v
         +-----------------+                 +-----------------+                 +-----------------+
         | greater_catalog |                 |  greater_users  |                 |  greater_notif  |
         | (PostgreSQL 16) |                 | (PostgreSQL 16) |                 | (PostgreSQL 16) |
         |   (Port 5433)   |                 |   (Port 5434)   |                 |   (Port 5435)   |
         +-----------------+                 +-----------------+                 +-----------------+
```

### 2.2 Inventario de Componentes y Puertos

| Módulo / Contenedor | Puerto Host | Puerto Contenedor | Protocolo | Responsabilidad Técnica |
| :--- | :---: | :---: | :---: | :--- |
| **`api-gateway`** | `8080` | `8080` | HTTP/1.1 (WebFlux) | Puerta de entrada única, enrutamiento dinámico, retransmisión de tokens JWT (`TokenRelay`). |
| **`eureka-server`** | `8761` | `8761` | HTTP/1.1 | Registro y catálogo dinámico de instancias de servicio vivas (*Service Discovery*). |
| **`config-server`** | `8888` | `8888` | HTTP/1.1 | Servidor de configuración centralizada en modo nativo (`classpath:/config`). |
| **`catalog-service`** | `8082` | `8082` | HTTP/1.1 (MVC) | Administración del catálogo de artistas y eventos. Publicador de eventos AMQP. |
| **`user-social-service`** | `8083` | `8083` | HTTP/1.1 (MVC) | API REST para seguimiento, favoritos y administración de usuarios Keycloak. |
| **`user-social-service (gRPC)`** | `9091` | `9091` | gRPC sobre HTTP/2 | Servidor RPC binario de alta velocidad para consultas internas de relaciones sociales. |
| **`notification-service`** | `8085` | `8085` | HTTP/1.1 (MVC) | Consumidor AMQP, cliente gRPC y consulta de notificaciones de usuario. |
| **Keycloak IAM** | `8084` | `8080` | HTTP/1.1 | Identity Provider (IdP) OAuth2 / OpenID Connect (Realm `unnoba`). |
| **RabbitMQ Broker** | `5672` | `5672` | AMQP 0-9-1 | Broker de mensajería orientada a eventos para desacoplamiento asíncrono. |
| **RabbitMQ Management** | `15672` | `15672` | HTTP/1.1 | Consola gráfica de monitoreo de colas, exchanges y tasas de consumo. |
| **PostgreSQL Catalog** | `5433` | `5432` | PostgreSQL Wire | Persistencia relacional de catálogo (`greater_catalog`). |
| **PostgreSQL Users** | `5434` | `5432` | PostgreSQL Wire | Persistencia relacional social y referencias de usuarios (`greater_users`). |
| **PostgreSQL Notifications** | `5435` | `5432` | PostgreSQL Wire | Persistencia relacional de notificaciones (`greater_notifications`). |

---

## 3. DECISIONES DE DISEÑO Y PATRONES DISTRIBUIDOS

### 3.1 Patrón *Database per Service*
* **Problema:** Compartir una base de datos común entre microservicios introduce un único punto de fallo (SPOF), genera bloqueos a nivel de tablas durante transacciones intensivas y acopla los esquemas de persistencia.
* **Solución Aplicada:** Cada microservicio (`catalog-service`, `user-social-service`, `notification-service`) administra su propia base de datos física aislada en PostgreSQL.
* **Composición en Capa de Servicio:** `user-social-service` no utiliza claves foráneas directas hacia tablas de catálogo; en su lugar, persiste identificadores numéricos (`Set<Long> followedArtists`, `Set<Long> favoriteEvents`). Para renderizar información enriquecida (nombres de artistas, fechas de eventos), utiliza clientes declarativos **OpenFeign** para consultar sincrónicamente al `catalog-service` y componer los DTOs en memoria.

### 3.2 Patrón *API Gateway* y Seguridad con *Token Relay*
* **Enrutamiento Centralizado:** Los clientes externos solo interactúan con el Gateway (`8080`). Mediante predicados de ruta basados en Eureka (`lb://catalog-service`, `lb://user-social-service`, `lb://notification-service`), el Gateway balancea y oculta la topología interna.
* **Propagación Segura de Identidad:** El Gateway aplica el filtro **`TokenRelay`**, transmitiendo el encabezado `Authorization: Bearer <JWT>` hacia los microservicios downstream. Cada servicio actúa como un **OAuth2 Resource Server** que valida localmente la firma del token mediante el endpoint JWKS de Keycloak (`jwk-set-uri`), y extrae los roles (`ROLE_admin`) mediante `KeycloakJwtGrantedAuthoritiesConverter`.

### 3.3 Arquitectura Orientada a Eventos (*Event-Driven*) con RabbitMQ
Para la notificación de eventos cancelados se implementó un desacoplamiento asíncrono no bloqueante:

```
[ Admin ] ---> PUT /admin/events/{id}/cancel
                  |
                  v
         +-----------------+
         | catalog-service | -> 1. Actualiza estado a CANCELLED en base de datos local
         +--------+--------+
                  |
                  | 2. rabbitTemplate.convertAndSend("events.exchange", "event.cancelled", msg)
                  v
       =======================
       RabbitMQ Topic Exchange: "events.exchange"
       Routing Key: "event.cancelled"
       Queue: "event.cancelled.queue" (Durable)
       =======================
                  |
                  v 3. Consumo no bloqueante (@RabbitListener)
     +----------------------+
     | notification-service |
     +----------+-----------+
                |
                v 4. Persiste notificaciones en greater_notifications
```

* **Tolerancia a Caídas:** La cola `event.cancelled.queue` es durable. Si `notification-service` experimenta una caída momentánea, el mensaje permanece encolado en RabbitMQ y se procesa automáticamente al restaurarse el servicio, garantizando **consistencia eventual**.

### 3.4 RPC Binario de Alto Rendimiento (gRPC sobre HTTP/2)
Para consultar los miles de usuarios que siguen a un artista o tienen un evento cancelado en favoritos, se descartó el protocolo REST/JSON por su sobrecarga de cabeceras HTTP/1.1 y costo de serialización de texto.

* **Implementación:**
  * En el módulo compartido [`grpc-api`](file:///Users/brunobertacchini/IdeaProjects/pdyc2026-junin-bertacchini-lovizzio/greater-events-ms/grpc-api/src/main/proto/user_social.proto), se compila el contrato Protobuf `user_social.proto`.
  * `user-social-service` expone el servidor gRPC en el puerto `9091`.
  * `notification-service` consume el servicio con `@GrpcClient("user-social-service")` utilizando resolución dinámica de nombres (`discovery:///user-social-service`) integrada con Eureka.

---

## 4. MODELO DE DATOS Y REGLAS DE NEGOCIO

### 4.1 Esquemas Relacionales por Microservicio
* **Catálogo (`catalog-service`):**
  * `artists`: `id` (PK), `name` (VarChar), `genre` (`ROCK`, `TECHNO`, `POP`, `JAZZ`, `FOLK`), `active` (Boolean).
  * `events`: `id` (PK), `name` (VarChar), `description` (VarChar 1000), `start_date` (Date), `state` (`TENTATIVE`, `CONFIRMED`, `RESCHEDULED`, `CANCELLED`).
  * `event_artists`: `event_id` (FK), `artist_id` (FK).
* **Social (`user-social-service`):**
  * `users`: `id` (PK), `username` (VarChar, Unique), `email` (VarChar, Unique).
  * `user_followed_artists`: `user_id` (FK), `artist_id` (BigInt).
  * `user_favorite_events`: `user_id` (FK), `event_id` (BigInt).
* **Notificaciones (`notification-service`):**
  * `notifications`: `id` (PK), `username` (VarChar), `event_id` (BigInt), `message` (VarChar 1000), `read` (Boolean), `created_at` (Timestamp).

### 4.2 Máquina de Estados del Evento y Reglas de Integridad
* **Ciclo de Vida:** Los eventos se crean en estado `TENTATIVE`. Solo en este estado pueden modificarse o eliminarse.
* **Confirmación y Reprogramación:** `confirm()` requiere que `start_date` sea futura. `reschedule()` solo aplica a eventos `CONFIRMED` o `RESCHEDULED` y exige una nueva fecha futura.
* **Cancelación:** Cambia el estado a `CANCELLED` y despacha el mensaje `EventCancelledMessage` a RabbitMQ.
* **Inmutabilidad Histórica de Artistas:** Si un artista tiene eventos asociados, se prohíbe su edición o borrado físico; en su lugar, se aplica un borrado lógico (`active = false`).
* **Regla de Favoritos:** Un usuario solo puede marcar como favorito un evento en estado `CONFIRMED` o `RESCHEDULED` con fecha futura.

---

## 5. EVOLUCIÓN DEL SISTEMA: OPCIÓN F – ESTRATEGIA DE CALIDAD Y PRUEBAS AUTOMATIZADAS

Como requerimiento de evolución para la modalidad individual, se diseñó e implementó una **estrategia de calidad integral basada en la Pirámide de Pruebas**, alcanzando un total de **42 pruebas automatizadas** que garantizan la confiabilidad del sistema sin depender de infraestructura externa.

```
                         /\
                        /  \
                       /    \
                      / INT. \   --> 8 Tests de Integración (@SpringBootTest + H2)
                     / TESTS  \      Validación de contexto, JPA y transacciones
                    /----------\
                   /            \
                  /  UNIT TESTS  \  --> 34 Tests Unitarios Puros (JUnit 5 + Mockito)
                 /  (Lógica Pura) \     Validación exhaustiva de ramas y excepciones
                /------------------\
```

### 5.1 Justificación Teórica: Tests Unitarios Puros vs. Tests de Integración
En arquitecturas distribuidas, depender exclusivamente de pruebas manuales o de tests que levantan todo el contexto de Spring Boot introduce dos problemas graves:
1. **Lentitud:** Levantar Spring Boot, escanear beans y configurar conexiones tarda varios segundos por clase.
2. **Dificultad para simular fallos:** Probar el comportamiento del sistema cuando un servicio externo falla (ej. caída de Feign, timeout o error en RabbitMQ) es complejo si se depende de instancias reales.

Por ello, la estrategia implementada combina:
* **Pruebas Unitarias Aisladas con Mockito:** Se ejecutan en memoria pura de la JVM (menos de 200 ms en total), permitiendo testear exhaustivamente cada rama condicional, validación y excepción de negocio (`assertThrows(BusinessException.class)`).
* **Pruebas de Integración con H2:** Verifican las sentencias SQL reales, mapeos de Hibernate y persistencia transaccional sin requerir bases de datos PostgreSQL externas.

### 5.2 Desglose de las 42 Pruebas Automatizadas

#### A. `catalog-service` (25 tests en total)
1. **`ArtistServiceTest` (9 tests unitarios - Mockito puro):**
   * Creación válida de artista y persistencia.
   * Rechazo con `BusinessException` ante nombre nulo o en blanco.
   * Rechazo ante género nulo.
   * Búsqueda por ID con retorno exitoso vs. `ResourceNotFoundException`.
   * Listado general y filtrado específico por género.
   * Bloqueo de actualización (`update`) cuando el artista tiene eventos asignados.
   * Rechazo de actualización sin campos válidos.
   * Borrado físico en repositorio cuando el artista no tiene eventos.
   * Borrado lógico (`active = false`) cuando el artista tiene historial de eventos.
2. **`EventServiceTest` (11 tests unitarios - Mockito puro):**
   * Creación exitosa en estado `TENTATIVE`.
   * Rechazo de creación con nombre vacío o fecha nula.
   * Bloqueo de modificación en eventos no tentativos (`CONFIRMED`/`CANCELLED`).
   * Bloqueo de eliminación en eventos no tentativos.
   * Rechazo al intentar agregar un artista desactivado (`active = false`).
   * Rechazo al intentar remover un artista no asociado al evento.
   * Confirmación exitosa con fecha futura vs. rechazo con fecha pasada.
   * Reprogramación exitosa con nueva fecha futura.
   * Rechazo de reprogramación con fechas pasadas o nulas.
   * Cancelación exitosa y verificación de emisión del mensaje a RabbitMQ mediante `verify(rabbitTemplate).convertAndSend(...)`.
   * Bloqueo de cancelación sobre eventos en estado `TENTATIVE`.
3. **`CatalogServiceIntegrationTest` (5 tests de integración - Spring Boot + H2):**
   * Creación y consulta filtrada en base de datos H2.
   * Flujo transaccional de alta y vinculación de artistas a eventos.
   * Ciclo de vida completo persistido (`TENTATIVE` -> `CONFIRMED` -> `CANCELLED`).
   * Validación de excepciones en capa de servicio con rollback transaccional.
   * Persistencia del estado inactivo en borrado lógico de artistas.

#### B. `user-social-service` (10 tests en total)
1. **`UserServiceTest` (8 tests unitarios - Mockito puro):**
   * Seguir a un artista activo con mock de `CatalogFeignClient`.
   * Captura de fallos en Feign (simulación de catálogo caído o 404) lanzando `ResourceNotFoundException`.
   * Bloqueo de seguimiento a artistas inactivos (`BusinessException`).
   * Rechazo al intentar dejar de seguir un artista no seguido previamente.
   * Guardado de evento favorito en estado `CONFIRMED` con fecha futura.
   * Rechazo al intentar marcar como favorito un evento en estado `TENTATIVE`.
   * Rechazo al intentar marcar como favorito un evento con fecha pasada.
   * Rechazo al intentar quitar de favoritos un evento no guardado.
2. **`UserSocialServiceIntegrationTest` (2 tests de integración - Spring Boot + H2):**
   * Persistencia de colecciones `@ElementCollection` en tablas de unión `user_followed_artists` y `user_favorite_events`.
   * Verificación de persistencia de usuario y recuperación de listas sociales.

#### C. `notification-service` (7 tests en total)
1. **`NotificationServiceTest` (4 tests unitarios - Mockito puro):**
   * Consulta ordenada de notificaciones totales y filtro de no leídas.
   * Marcado exitoso de notificación como leída (`read = true`).
   * **Control de Seguridad y Aislamiento:** Verificación de lanzamiento de `AccessDeniedException` cuando un usuario intenta marcar como leída una notificación perteneciente a otro usuario.
   * Rechazo con `ResourceNotFoundException` ante ID de notificación inexistente.
2. **`EventCancelledListenerTest` (2 tests unitarios - Mockito puro):**
   * **Algoritmo de Desduplicación:** Verificación con `ArgumentCaptor<Notification>` de que usuarios que son seguidores de múltiples artistas y además tienen el evento en favoritos reciban **exactamente 1 sola notificación**, evitando spam.
   * Resiliencia ante mensajes con lista de artistas nula (`artistIds == null`), validando que no ocurra `NullPointerException`.
3. **`NotificationServiceIntegrationTest` (1 test de integración - Spring Boot + H2):**
   * Prueba end-to-end de recepción del mensaje, mock del stub gRPC y almacenamiento en base de datos H2.

### 5.3 Resolución Técnica de Compatibilidad con Java Moderno
Durante la ejecución de las pruebas unitarias en entornos con Java 25 (JVM moderna), se identificó que la instrumentación dinámica de Mockito requería habilitar el soporte experimental de ByteBuddy. Para ello, se parametrizó la ejecución en Maven pasando:
```bash
-DargLine="-Dnet.bytebuddy.experimental=true"
```
Esto permite a ByteBuddy inyectar los proxies dinámicos en tiempo de ejecución sin advertencias ni bloqueos del ClassLoader.

---

## 6. INFRAESTRUCTURA DE DESPLIEGUE (DOCKER COMPOSE)

Para satisfacer el requisito general de "Despliegue funcional de todos los servicios", se configuró la orquestación integral mediante Docker Compose:
* **Dockerfiles JRE:** Se crearon Dockerfiles livianos (`eclipse-temurin:17-jre`) para los 6 microservicios Java, logrando imágenes de menos de 200 MB con inicio ultra rápido.
* **Orquestación de 11 Contenedores:** PostgreSQL (3 bases independientes), RabbitMQ con panel web, Keycloak y los 6 microservicios.
* **Healthchecks y Dependencias Condicionales:** Las aplicaciones Java esperan a que PostgreSQL responda (`pg_isready`) y RabbitMQ responda (`rabbitmq-diagnostics ping`) mediante `depends_on: condition: service_healthy`, eliminando fallos por condiciones de carrera al levantar el sistema.

---

## 7. GUÍA DE COMPILACIÓN, EJECUCIÓN Y OPERACIÓN

### 7.1 Ejecución de las Pruebas Automatizadas
Desde la raíz del repositorio:
```bash
mvn clean test -DargLine="-Dnet.bytebuddy.experimental=true"
```
*Resultado:* `BUILD SUCCESS`, **42 pruebas ejecutadas, 0 fallos, 0 errores en 14 segundos**.

### 7.2 Generación de Artefactos y Despliegue con Docker
```bash
# Compilar artefactos JAR
mvn clean package -DskipTests

# Levantar toda la infraestructura y microservicios
docker compose up --build -d

# Verificar el estado de los contenedores
docker compose ps
```

### 7.3 URLs de Acceso
* **API Gateway:** `http://localhost:8080`
* **Eureka Discovery Dashboard:** `http://localhost:8761`
* **RabbitMQ Management Console:** `http://localhost:15672` (User/Pass: `guest`/`guest`)
* **Keycloak IAM Admin Console:** `http://localhost:8084` (User/Pass: `admin`/`admin`)

---

## 9. CONCLUSIONES

La plataforma **Greater Events Microservices** cumple de forma rigurosa con los principios de diseño de sistemas distribuidos modernos: desacoplamiento funcional, tolerancia a fallos, asincronismo y alto rendimiento. La incorporación de la **Opción F (Estrategia Integral de Pruebas Automatizadas)** aporta un estándar profesional de calidad de software, dotando a la solución de una suite reproducible de 42 pruebas que respaldan la estabilidad de la lógica de negocio y la arquitectura de microservicios presentada.