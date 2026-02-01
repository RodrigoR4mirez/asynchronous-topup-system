1. topup-gateway-api (API Gateway)
Responsabilidad: Punto de entrada para los usuarios. Valida el formato del JSON e inserta la solicitud inicial en la base de datos.
URL: POST http://localhost:8080/v1/topups
Input (JSON):
{
  "phoneNumber": "987654321",
  "amount": 50.0,
  "carrier": "MOVISTAR"
}
Output: 202 Accepted (Indica que la petición fue recibida y está en proceso de validación).


## Arquitectura Hexagonal (Ports & Adapters)

Este proyecto sigue estrictamente el patrón de **Arquitectura Hexagonal**, cuyo objetivo es desacoplar la lógica de negocio (Dominio) de los detalles de implementación externos (Infraestructura, UI, Bases de Datos).

### Estructura y Propósito de las Capas:

1.  **Domain (Núcleo)**:
    *   **Objetivo**: Contener la lógica de negocio pura y las entidades del modelo. No depende de ningún framework ni librería externa (ni siquiera de Quarkus o Hibernate en su forma más pura, aunque aquí usamos algunas anotaciones por pragmatismo pero mantenemos el acoplamiento mínimo).
    *   **Componentes**: `TopupRequest` (Modelo), `TopupRepositoryPort` (Puerto/Interfaz).
    *   **Por qué**: Para que el negocio sea el centro de la aplicación y pueda evolucionar independientemente de la tecnología.

2.  **Application (Caso de Uso)**:
    *   **Objetivo**: Orquestar los flujos de negocio. Implementa los casos de uso específicos.
    *   **Componentes**: `TopupService`.
    *   **Por qué**: Sirve de puente entre el mundo exterior (Infraestructura) y el Núcleo (Domain), asegurando que las reglas de negocio se ejecuten.

3.  **Infrastructure (Adaptadores)**:
    *   **Objetivo**: Implementar la comunicación con el mundo exterior. Se divide en:
        *   **Adapters In (Entrada)**: "Drivean" la aplicación. Ejemplo: `TopupResource` (API REST). Reciben HTTP y llaman al Servicio de Aplicación.
        *   **Adapters Out (Salida)**: Son "Drireados" por la aplicación. Ejemplo: `PanacheTopupRepository` (Persistencia). Implementan los Puertos definidos en el Dominio para guardar datos en la BD.
    *   **Por qué**: Permite cambiar la base de datos o exponer la funcionalidad por otro medio (ej. Kafka, gRPC) sin tocar ni una línea de la lógica de negocio.

### Árbol del Proyecto:

```
src/main/java/pe/com/topup/gateway
├── application
│   └── service
│       └── TopupService.java           <-- Application: Orquestador de lógica
├── domain
│   ├── model
│   │   └── TopupRequest.java           <-- Domain: Entidad pura
│   └── port
│       └── TopupRepositoryPort.java    <-- Domain: Puerto (Interfaz para BD)
└── infrastructure
    ├── adapter
    │   ├── in
    │   │   └── web
    │   │       ├── TopupResource.java              <-- Infra (In): Adaptador REST
    │   │       ├── dto
    │   │       │   ├── ErrorResponse.java
    │   │       │   └── TopupRequestDto.java
    │   │       └── errorhandler
    │   │           └── ValidationExceptionMapper.java
    │   └── out
    │       └── persistence
    │           ├── PanacheTopupRepository.java     <-- Infra (Out): Adaptador BD
    │           └── entity
    │               └── TopupRequestEntity.java
```

# topup-gateway-api

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/topup-gateway-api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- Reactive MySQL client ([guide](https://quarkus.io/guides/reactive-sql-clients)): Connect to the MySQL database using the reactive pattern
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- YAML Configuration ([guide](https://quarkus.io/guides/config-yaml)): Use YAML to configure your Quarkus application


