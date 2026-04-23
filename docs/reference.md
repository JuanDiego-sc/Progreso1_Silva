# Guía Técnica Práctica – SaludVital Integration
## ISWZ3104 – Integración de Sistemas | Progreso 1

---

## Índice

1. [Prerequisitos y entorno](#1-prerequisitos-y-entorno)
2. [Estructura del proyecto Maven](#2-estructura-del-proyecto-maven)
3. [Dependencias pom.xml](#3-dependencias-pomxml)
4. [Estructura de directorios de datos](#4-estructura-de-directorios-de-datos)
5. [Archivos CSV de prueba](#5-archivos-csv-de-prueba)
6. [Lógica de validación](#6-lógica-de-validación)
7. [Ruta Apache Camel](#7-ruta-apache-camel)
8. [Configuración de la aplicación](#8-configuración-de-la-aplicación)
9. [Punto de entrada](#9-punto-de-entrada)
10. [Verificación de ejecución](#10-verificación-de-ejecución)
11. [Evidencias esperadas](#11-evidencias-esperadas)
12. [Especificación OpenAPI](#12-especificación-openapi)

---

## 1. Prerequisitos y entorno

Verificar que el entorno tenga instalado todo lo necesario antes de escribir una sola línea de código.

### 1.1 Java

```bash
java -version
```

Salida esperada: `openjdk version "17"` o superior. Apache Camel 4.x requiere Java 17+.

### 1.2 Maven

```bash
mvn -version
```

Salida esperada: `Apache Maven 3.8.x` o superior.

### 1.3 Crear el directorio de trabajo

```bash
mkdir -p ~/saludvital-integration
cd ~/saludvital-integration
```

---

## 2. Estructura del proyecto Maven

Crear la estructura de paquetes manualmente o con el arquetipo. La estructura final debe quedar así:

```
saludvital-integration/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── saludvital/
│       │           ├── Main.java
│       │           ├── route/
│       │           │   └── AdmisionesRoute.java
│       │           └── processor/
│       │               └── CsvValidationProcessor.java
│       └── resources/
│           └── application.properties
└── data/
    ├── input/
    ├── output/
    ├── archive/
    └── error/
```

Crear los directorios de datos:

```bash
mkdir -p data/input data/output data/archive data/error
mkdir -p src/main/java/com/saludvital/route
mkdir -p src/main/java/com/saludvital/processor
mkdir -p src/main/resources
```

---

## 3. Dependencias pom.xml

Crear el archivo `pom.xml` en la raíz del proyecto con el siguiente contenido completo:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.saludvital</groupId>
    <artifactId>saludvital-integration</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <camel.version>4.4.0</camel.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.apache.camel</groupId>
                <artifactId>camel-bom</artifactId>
                <version>${camel.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>

        <!-- Camel Core -->
        <dependency>
            <groupId>org.apache.camel</groupId>
            <artifactId>camel-core</artifactId>
        </dependency>

        <!-- Componente File -->
        <dependency>
            <groupId>org.apache.camel</groupId>
            <artifactId>camel-file</artifactId>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.9</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.14</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>

            <!-- Plugin para ejecutar con: mvn exec:java -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>com.saludvital.Main</mainClass>
                </configuration>
            </plugin>

            <!-- JAR ejecutable con todas las dependencias -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation=
                                    "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.saludvital.Main</mainClass>
                                </transformer>
                                <!-- Requerido por Camel para combinar META-INF de múltiples JARs -->
                                <transformer implementation=
                                    "org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>META-INF/services/org/apache/camel/TypeConverter</resource>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```

Verificar que Maven descarga las dependencias correctamente:

```bash
mvn dependency:resolve
```

No debe haber errores de tipo `BUILD FAILURE`. Si los hay, verificar conexión a internet y que la versión de Camel esté disponible en Maven Central.

---

## 4. Estructura de directorios de datos

Los directorios deben existir antes de ejecutar la aplicación. Camel puede crearlos automáticamente con la opción `autoCreate=true`, pero es mejor crearlos explícitamente para evitar problemas de permisos.

```bash
# Desde la raíz del proyecto
mkdir -p data/input
mkdir -p data/output
mkdir -p data/archive
mkdir -p data/error
```

Verificar que existen:

```bash
ls -la data/
```

Salida esperada:
```
drwxr-xr-x  archive/
drwxr-xr-x  error/
drwxr-xr-x  input/
drwxr-xr-x  output/
```

---

## 5. Archivos CSV de prueba

Crear cuatro archivos de prueba que cubran todos los escenarios posibles: válido, sin encabezado, campos vacíos, fecha incorrecta y código de seguro inválido.

### 5.1 Archivo válido

Crear `data/input/pacientes_valido.csv`:

```csv
patient_id,full_name,appointment_date,insurance_code
PAC-001,Maria Lopez,2026-04-22,IESS
PAC-002,Carlos Ruiz,2026-04-22,PRIVADO
PAC-003,Ana Torres,2026-04-23,NINGUNO
PAC-004,Jorge Mendez,2026-04-23,IESS
```

### 5.2 Archivo con encabezado incorrecto

Crear `data/input/pacientes_sin_encabezado.csv`:

```csv
id,nombre,fecha,seguro
PAC-005,Luis Paredes,2026-04-22,IESS
```

El encabezado tiene nombres distintos a los requeridos. Este archivo debe ir a `/data/error`.

### 5.3 Archivo con campo vacío

Crear `data/input/pacientes_campo_vacio.csv`:

```csv
patient_id,full_name,appointment_date,insurance_code
PAC-006,Elena Vega,,IESS
PAC-007,Pedro Mora,2026-04-22,PRIVADO
```

La tercera fila tiene `appointment_date` vacío. Este archivo debe ir a `/data/error`.

### 5.4 Archivo con fecha incorrecta

Crear `data/input/pacientes_fecha_invalida.csv`:

```csv
patient_id,full_name,appointment_date,insurance_code
PAC-008,Sofia Ibarra,22/04/2026,IESS
```

La fecha está en formato `DD/MM/YYYY` en lugar de `YYYY-MM-DD`. Este archivo debe ir a `/data/error`.

### 5.5 Archivo con código de seguro inválido

Crear `data/input/pacientes_seguro_invalido.csv`:

```csv
patient_id,full_name,appointment_date,insurance_code
PAC-009,Roberto Lara,2026-04-22,SEGURO_GENERAL
```

`SEGURO_GENERAL` no pertenece al conjunto permitido. Este archivo debe ir a `/data/error`.

---

## 6. Lógica de validación

Crear el archivo `src/main/java/com/saludvital/processor/CsvValidationProcessor.java`.

Este procesador es responsable de leer el contenido del archivo CSV, aplicar todas las reglas de validación y marcar el mensaje con una propiedad `valid` de tipo booleano y una propiedad `failReason` con el motivo del fallo si corresponde.

```java
package com.saludvital.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class CsvValidationProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(CsvValidationProcessor.class);

    // Encabezado exacto requerido en ese orden
    private static final List<String> REQUIRED_HEADERS =
        Arrays.asList("patient_id", "full_name", "appointment_date", "insurance_code");

    // Valores permitidos para insurance_code
    private static final Set<String> VALID_INSURANCE_CODES =
        Set.of("IESS", "PRIVADO", "NINGUNO");

    // Patron YYYY-MM-DD
    private static final Pattern DATE_PATTERN =
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    @Override
    public void process(Exchange exchange) {

        String fileName   = exchange.getIn().getHeader("CamelFileName", String.class);
        String content    = exchange.getIn().getBody(String.class);

        log.info("[INICIO] Procesando archivo: {}", fileName);

        // Separar lineas ignorando lineas en blanco al final
        String[] lines = content.split("\\r?\\n");

        // Validacion 1: el archivo debe tener al menos encabezado + 1 fila de datos
        if (lines.length < 2) {
            markInvalid(exchange, fileName, "El archivo esta vacio o solo tiene encabezado.");
            return;
        }

        // Validacion 2: encabezado correcto
        String headerLine = lines[0].trim();
        List<String> actualHeaders = Arrays.asList(headerLine.split(","));

        if (!actualHeaders.equals(REQUIRED_HEADERS)) {
            markInvalid(exchange, fileName,
                "Encabezado incorrecto. Recibido: [" + headerLine +
                "] Esperado: [" + String.join(",", REQUIRED_HEADERS) + "]");
            return;
        }

        // Validacion de cada fila de datos (desde la linea 1 en adelante)
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();

            // Ignorar lineas en blanco al final del archivo
            if (line.isEmpty()) continue;

            String[] fields = line.split(",", -1); // -1 para conservar campos vacios al final

            // Validacion 3: numero de columnas correcto
            if (fields.length != REQUIRED_HEADERS.size()) {
                markInvalid(exchange, fileName,
                    "Fila " + (i + 1) + " tiene " + fields.length +
                    " columnas, se esperaban " + REQUIRED_HEADERS.size() + ".");
                return;
            }

            String patientId      = fields[0].trim();
            String fullName       = fields[1].trim();
            String appointmentDate = fields[2].trim();
            String insuranceCode  = fields[3].trim();

            // Validacion 4: ningun campo puede estar vacio
            if (patientId.isEmpty() || fullName.isEmpty() ||
                appointmentDate.isEmpty() || insuranceCode.isEmpty()) {
                markInvalid(exchange, fileName,
                    "Fila " + (i + 1) + " tiene uno o mas campos vacios.");
                return;
            }

            // Validacion 5: formato de fecha YYYY-MM-DD
            if (!DATE_PATTERN.matcher(appointmentDate).matches()) {
                markInvalid(exchange, fileName,
                    "Fila " + (i + 1) + " tiene appointment_date con formato invalido: '"
                    + appointmentDate + "'. Se espera YYYY-MM-DD.");
                return;
            }

            // Validacion 6: insurance_code en el conjunto permitido
            if (!VALID_INSURANCE_CODES.contains(insuranceCode)) {
                markInvalid(exchange, fileName,
                    "Fila " + (i + 1) + " tiene insurance_code invalido: '"
                    + insuranceCode + "'. Valores permitidos: " + VALID_INSURANCE_CODES);
                return;
            }
        }

        // Si llega aqui, el archivo es valido
        exchange.setProperty("valid", true);
        exchange.setProperty("failReason", null);
        log.info("[VALIDO] Archivo aprobado: {}", fileName);
    }

    private void markInvalid(Exchange exchange, String fileName, String reason) {
        exchange.setProperty("valid", false);
        exchange.setProperty("failReason", reason);
        log.warn("[INVALIDO] Archivo: {} | Motivo: {}", fileName, reason);
    }
}
```

### Puntos clave de la validación

- `split(",", -1)` con `-1` como límite conserva los campos vacíos al final de la línea. Sin ese parámetro, `"PAC-001,Maria,,"` devolvería solo dos elementos en lugar de cuatro.
- El método `markInvalid` centraliza el registro del motivo de fallo, evitando duplicación de lógica.
- Las validaciones se aplican en orden de menos a más costoso: primero estructura, luego contenido.
- Se retorna inmediatamente al primer error encontrado (fail-fast), lo cual es correcto porque el archivo completo es la unidad de procesamiento.

---

## 7. Ruta Apache Camel

Crear el archivo `src/main/java/com/saludvital/route/AdmisionesRoute.java`.

Esta es la pieza central de la solución. Define el flujo completo de procesamiento de archivos.

```java
package com.saludvital.route;

import com.saludvital.processor.CsvValidationProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.language.simple.Simple;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdmisionesRoute extends RouteBuilder {

    // Formatter para el timestamp del nombre de archivo archivado
    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    @Override
    public void configure() {

        /*
         * Manejo global de excepciones no controladas.
         * Si algo falla de forma inesperada dentro de la ruta,
         * el archivo se mueve a error y se registra el fallo.
         */
        onException(Exception.class)
            .handled(true)
            .process(exchange -> {
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                log.error("[ERROR_INESPERADO] Archivo: {} | Excepcion: {}",
                    fileName, cause.getMessage());
                exchange.setProperty("failReason", "Excepcion inesperada: " + cause.getMessage());
            })
            .toD("file:data/error?fileName=${header.CamelFileName}")
            .log("Archivo movido a error por excepcion inesperada: ${header.CamelFileName}");

        /*
         * RUTA PRINCIPAL
         *
         * from: monitorea data/input cada 5 segundos buscando archivos .csv
         *
         * Opciones del endpoint file de entrada:
         *   - noop=false           : Camel ELIMINA el archivo de input al finalizar (comportamiento por defecto).
         *                           Si se pusiera noop=true, el archivo NO se elimina. Aqui lo dejamos en false
         *                           (o simplemente no lo declaramos) para que se elimine.
         *   - delay=5000           : intervalo de polling en milisegundos.
         *   - include=.*\\.csv     : solo procesa archivos con extension .csv.
         *   - autoCreate=true      : crea la carpeta si no existe.
         *   - readLock=changed     : espera a que el archivo deje de crecer antes de procesarlo,
         *                           evitando leer un archivo que aun esta siendo escrito.
         *   - readLockCheckInterval=1000 : verifica cada 1 segundo si el archivo dejo de cambiar.
         *   - readLockMinAge=500   : el archivo debe tener al menos 500ms sin cambios.
         */
        from("file:data/input" +
             "?noop=false" +
             "&delay=5000" +
             "&include=.*\\.csv" +
             "&autoCreate=true" +
             "&readLock=changed" +
             "&readLockCheckInterval=1000" +
             "&readLockMinAge=500")
            .routeId("saludvital-admisiones-route")

            // Guardar el nombre original antes de que Camel lo modifique
            .process(exchange -> {
                String originalName = exchange.getIn().getHeader("CamelFileName", String.class);
                exchange.setProperty("originalFileName", originalName);

                // Generar el nombre de archivo con timestamp para el archivado
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
                // Obtener solo el nombre sin extension
                String nameWithoutExt = originalName.replaceAll("\\.csv$", "");
                String archivedName = nameWithoutExt + "_" + timestamp + ".csv";
                exchange.setProperty("archivedFileName", archivedName);

                log.info("[DETECTADO] Archivo en cola: {}", originalName);
            })

            // Ejecutar validacion
            .process(new CsvValidationProcessor())

            // Tomar decision de enrutamiento basada en resultado de validacion
            .choice()

                // RAMA VALIDA: el archivo pasa todas las validaciones
                .when(exchangeProperty("valid").isEqualTo(true))
                    .log("[PROCESANDO] Archivo valido: ${exchangeProperty.originalFileName}")

                    // 1. Enviar al output para Facturacion
                    // multicast permite enviar el mismo mensaje a multiples destinos
                    .multicast()
                        // Destino 1: output para consumo de Facturacion
                        .to("file:data/output?fileName=${exchangeProperty.originalFileName}")

                        // Destino 2: archive con nombre renombrado con timestamp
                        .to("file:data/archive?fileName=${exchangeProperty.archivedFileName}")
                    .end()

                    .log("[EXITO] Archivo procesado. Output: ${exchangeProperty.originalFileName} " +
                         "| Archive: ${exchangeProperty.archivedFileName}")

                // RAMA INVALIDA: el archivo fallo alguna validacion
                .otherwise()
                    .log("[RECHAZADO] Archivo invalido: ${exchangeProperty.originalFileName} " +
                         "| Motivo: ${exchangeProperty.failReason}")

                    // 1. Mover a error para revision manual
                    .multicast()
                        .to("file:data/error?fileName=${exchangeProperty.originalFileName}")
                        // Tambien archivar el invalido para trazabilidad historica
                        .to("file:data/archive?fileName=${exchangeProperty.archivedFileName}")
                    .end()

                    .log("[TRAZABILIDAD] Archivo invalido archivado: ${exchangeProperty.archivedFileName}")

            .end();
    }
}
```

### Por qué `noop=false` y no `delete=true`

En el componente `file:` de Camel, el comportamiento por defecto cuando `noop=false` es mover el archivo procesado a una subcarpeta `.camel/` dentro del directorio de origen. Para eliminarlo completamente, se debe agregar `delete=true`. Elegir entre ambas opciones:

- `delete=true`: elimina el archivo original de `data/input`. Es la opción más limpia para prevenir reprocesamiento.
- `move=.processed`: mueve el archivo a una subcarpeta `.processed/` dentro de `data/input`. Útil si se quiere un registro adicional.

Para este caso, usar `delete=true` es la opción correcta porque el archivado con timestamp en `data/archive` ya garantiza la trazabilidad. Modificar el endpoint así:

```java
from("file:data/input" +
     "?delete=true" +      // <-- cambiar noop=false por delete=true
     "&delay=5000" +
     // ... resto de opciones
```

### Por qué `multicast` y no dos `to` separados

Dos `to` consecutivos en Camel procesan el mensaje en cadena: el segundo `to` recibe el resultado del primero. Si el primer endpoint modifica el cuerpo del mensaje, el segundo recibe el mensaje modificado. Con `multicast`, ambos endpoints reciben el mismo mensaje original de forma independiente y paralela (o secuencial si no se configura `parallelProcessing`).

---

## 8. Configuración de la aplicación

Crear `src/main/resources/application.properties`:

```properties
# Nombre de la aplicacion
camel.main.name=saludvital-integration

# Tiempo de ejecucion en segundos (0 = indefinido, corre hasta que se detenga manualmente)
camel.main.duration-max-seconds=-1

# Intervalo de polling en ms (puede sobreescribirse en el endpoint)
saludvital.input.dir=data/input
saludvital.output.dir=data/output
saludvital.archive.dir=data/archive
saludvital.error.dir=data/error
```

Crear también `src/main/resources/logback.xml` para controlar el formato de los logs:

```xml
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%-5level] [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Log a archivo para evidencia de ejecucion -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/saludvital.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/saludvital.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%-5level] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Nivel INFO para la aplicacion, reducir verbosidad de Camel internals -->
    <logger name="com.saludvital" level="INFO"/>
    <logger name="org.apache.camel" level="INFO"/>

    <root level="WARN">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

</configuration>
```

Crear el directorio de logs:

```bash
mkdir -p logs
```

---

## 9. Punto de entrada

Crear `src/main/java/com/saludvital/Main.java`:

```java
package com.saludvital;

import com.saludvital.route.AdmisionesRoute;
import org.apache.camel.main.Main;

public class Main {

    public static void main(String[] args) throws Exception {

        Main camelMain = new Main();

        // Registrar la ruta
        camelMain.configure().addRoutesBuilder(new AdmisionesRoute());

        // Iniciar Camel (bloquea hasta recibir Ctrl+C)
        System.out.println("=== SaludVital Integration iniciada. Esperando archivos en data/input/ ===");
        System.out.println("=== Presionar Ctrl+C para detener ===");
        camelMain.run(args);
    }
}
```

---

## 10. Verificación de ejecución

### 10.1 Compilar el proyecto

```bash
mvn clean compile
```

Verificar que no hay errores de compilación. Si aparece `BUILD SUCCESS`, continuar.

### 10.2 Ejecutar la aplicación

```bash
mvn exec:java
```

La consola debe mostrar:

```
=== SaludVital Integration iniciada. Esperando archivos en data/input/ ===
=== Presionar Ctrl+C para detener ===
... [INFO] Apache Camel 4.4.0 (saludvital-integration) started in ...
```

### 10.3 Prueba con archivo válido

Con la aplicación corriendo, en otra terminal:

```bash
cp data/input/pacientes_valido.csv data/input/test_valido.csv
```

Salida esperada en consola:

```
[INFO] [DETECTADO] Archivo en cola: test_valido.csv
[INFO] [VALIDO] Archivo aprobado: test_valido.csv
[INFO] [PROCESANDO] Archivo valido: test_valido.csv
[INFO] [EXITO] Archivo procesado. Output: test_valido.csv | Archive: test_valido_2026-04-22_103015.csv
```

Verificar el resultado:

```bash
ls data/output/      # debe contener test_valido.csv
ls data/archive/     # debe contener test_valido_YYYY-MM-DD_HHmmss.csv
ls data/input/       # debe estar VACIO (archivo eliminado)
ls data/error/       # debe estar VACIO
```

### 10.4 Prueba con archivo inválido (encabezado)

```bash
cp data/input/pacientes_sin_encabezado.csv data/input/test_encabezado.csv
```

Salida esperada en consola:

```
[INFO]  [DETECTADO] Archivo en cola: test_encabezado.csv
[WARN]  [INVALIDO] Archivo: test_encabezado.csv | Motivo: Encabezado incorrecto. Recibido: [id,nombre,fecha,seguro] ...
[INFO]  [RECHAZADO] Archivo invalido: test_encabezado.csv | Motivo: Encabezado incorrecto...
[INFO]  [TRAZABILIDAD] Archivo invalido archivado: test_encabezado_2026-04-22_103020.csv
```

Verificar:

```bash
ls data/error/       # debe contener test_encabezado.csv
ls data/archive/     # debe contener test_encabezado_YYYY-MM-DD_HHmmss.csv
ls data/input/       # debe estar VACIO
ls data/output/      # sin cambios
```

### 10.5 Prueba de reprocesamiento

Intentar copiar el mismo archivo nuevamente:

```bash
cp data/input/pacientes_valido.csv data/input/test_valido.csv
```

El archivo debe procesarse nuevamente generando un nuevo timestamp en archive. Esto confirma que el mecanismo de eliminación del archivo original previene el reprocesamiento del mismo archivo: cada vez que llega un archivo, es nuevo.

### 10.6 Verificar el log en archivo

```bash
cat logs/saludvital.log
```

El log debe mostrar todos los eventos de procesamiento con timestamps.

---

## 11. Evidencias esperadas

Las siguientes capturas de pantalla deben tomarse para el entregable:

| Evidencia | Qué capturar |
|---|---|
| Captura 1 | Consola con logs de procesamiento de archivo válido |
| Captura 2 | Consola con logs de procesamiento de archivo inválido |
| Captura 3 | Contenido de `data/output/` con archivo válido procesado |
| Captura 4 | Contenido de `data/archive/` con archivos renombrados con timestamp |
| Captura 5 | Contenido de `data/error/` con archivos inválidos |
| Captura 6 | `data/input/` vacío confirmando que los archivos se eliminaron |
| Captura 7 | Contenido del archivo `logs/saludvital.log` |

Comandos útiles para generar evidencias de terminal:

```bash
# Ver todos los directorios de una vez
echo "=== INPUT ===" && ls -la data/input/
echo "=== OUTPUT ===" && ls -la data/output/
echo "=== ARCHIVE ===" && ls -la data/archive/
echo "=== ERROR ===" && ls -la data/error/

# Ver ultimas 50 lineas del log
tail -50 logs/saludvital.log
```

---

## 12. Especificación OpenAPI

Esta sección cubre la especificación OpenAPI 3.0 para la API futura descrita en la Parte 4 del examen.

### 12.1 Cómo generar el YAML de OpenAPI

Existen dos enfoques para generar la especificación:

**Enfoque A: Design-first (recomendado para este caso)**

Escribir el YAML manualmente y luego pegar el resultado en Swagger UI para visualizarlo:

1. Ir a [https://editor.swagger.io](https://editor.swagger.io)
2. Borrar el contenido de ejemplo
3. Pegar la especificación de la sección 12.2
4. El editor valida la especificación en tiempo real y muestra la UI interactiva a la derecha
5. Exportar como PNG o tomar captura de la UI para el entregable

**Enfoque B: Code-first con Camel y Swagger**

Si se implementara la API REST con camel-servlet o camel-rest-dsl, se puede agregar la dependencia `camel-openapi-java` y Camel genera el YAML automáticamente desde las definiciones de las rutas REST. Para este examen no es necesario implementar la API, solo documentarla.

### 12.2 Especificación OpenAPI 3.0 completa

Guardar el siguiente contenido como `openapi/pre-registros-api.yaml`:

```yaml
openapi: 3.0.3

info:
  title: SaludVital – API de Pre-registros
  description: >
    API REST para la consulta y gestión de pre-registros de pacientes del
    Sistema de Admisiones de la Clínica SaludVital. Esta API representa la
    evolución futura del proceso de integración actual basado en File Transfer,
    permitiendo consultas en tiempo real con mejor trazabilidad y seguridad.
  version: 1.0.0
  contact:
    name: TI SaludVital

servers:
  - url: http://localhost:8080/api/v1
    description: Servidor de desarrollo local
  - url: https://api.saludvital.ec/v1
    description: Servidor de producción

tags:
  - name: pre-registros
    description: Operaciones sobre pre-registros de pacientes

paths:

  /pre-registros:

    get:
      tags: [pre-registros]
      summary: Listar pre-registros
      description: >
        Retorna la lista paginada de pre-registros. Por defecto devuelve los
        pre-registros del día actual. Se puede filtrar por fecha y por estado.
      operationId: listarPreRegistros
      parameters:
        - name: fecha
          in: query
          description: Filtrar por fecha de cita en formato YYYY-MM-DD. Por defecto, fecha actual.
          required: false
          schema:
            type: string
            format: date
            example: "2026-04-22"
        - name: estado
          in: query
          description: Filtrar por estado de procesamiento del pre-registro.
          required: false
          schema:
            type: string
            enum: [PENDIENTE, PROCESADO, RECHAZADO]
            example: PENDIENTE
        - name: pagina
          in: query
          description: Número de página (base 1).
          required: false
          schema:
            type: integer
            minimum: 1
            default: 1
        - name: por_pagina
          in: query
          description: Cantidad de registros por página.
          required: false
          schema:
            type: integer
            minimum: 1
            maximum: 100
            default: 10
      responses:
        "200":
          description: Lista de pre-registros obtenida exitosamente.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PreRegistroListResponse'
              example:
                total: 25
                pagina: 1
                por_pagina: 10
                datos:
                  - patient_id: "PAC-2026-00123"
                    full_name: "Maria Lopez"
                    appointment_date: "2026-04-22"
                    insurance_code: "IESS"
                    estado: "PENDIENTE"
        "400":
          $ref: '#/components/responses/BadRequest'
        "500":
          $ref: '#/components/responses/InternalError'

    post:
      tags: [pre-registros]
      summary: Crear pre-registro
      description: >
        Crea un nuevo pre-registro de paciente. El sistema valida el formato
        de los datos antes de persistir. Retorna el recurso creado con su
        estado inicial PENDIENTE.
      operationId: crearPreRegistro
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PreRegistroRequest'
            example:
              patient_id: "PAC-2026-00124"
              full_name: "Carlos Ruiz"
              appointment_date: "2026-04-22"
              insurance_code: "PRIVADO"
      responses:
        "201":
          description: Pre-registro creado exitosamente.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PreRegistroResponse'
        "400":
          $ref: '#/components/responses/BadRequest'
        "422":
          $ref: '#/components/responses/UnprocessableEntity'
        "500":
          $ref: '#/components/responses/InternalError'

  /pre-registros/{patient_id}:

    get:
      tags: [pre-registros]
      summary: Obtener pre-registro por ID
      description: Retorna el detalle completo de un pre-registro específico.
      operationId: obtenerPreRegistro
      parameters:
        - name: patient_id
          in: path
          required: true
          description: Identificador único del paciente pre-registrado.
          schema:
            type: string
            example: "PAC-2026-00123"
      responses:
        "200":
          description: Pre-registro encontrado.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PreRegistroResponse'
              example:
                patient_id: "PAC-2026-00123"
                full_name: "Maria Lopez"
                appointment_date: "2026-04-22"
                insurance_code: "IESS"
                estado: "PENDIENTE"
                fecha_registro: "2026-04-22T09:15:00Z"
                procesado_por: null
        "404":
          $ref: '#/components/responses/NotFound'
        "500":
          $ref: '#/components/responses/InternalError'

  /pre-registros/{patient_id}/estado:

    patch:
      tags: [pre-registros]
      summary: Actualizar estado de un pre-registro
      description: >
        Permite al Sistema de Facturación actualizar el estado de procesamiento
        de un pre-registro. Es el mecanismo de respuesta que cierra el ciclo
        de comunicación bidireccional con Admisiones.
      operationId: actualizarEstadoPreRegistro
      parameters:
        - name: patient_id
          in: path
          required: true
          schema:
            type: string
            example: "PAC-2026-00123"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/EstadoUpdateRequest'
            example:
              estado: "PROCESADO"
              procesado_por: "facturacion-service"
              observacion: "Pre-registro aceptado y factura generada."
      responses:
        "200":
          description: Estado actualizado exitosamente.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PreRegistroResponse'
        "400":
          $ref: '#/components/responses/BadRequest'
        "404":
          $ref: '#/components/responses/NotFound'
        "500":
          $ref: '#/components/responses/InternalError'

components:

  schemas:

    PreRegistroRequest:
      type: object
      required: [patient_id, full_name, appointment_date, insurance_code]
      properties:
        patient_id:
          type: string
          description: Identificador único del paciente.
          example: "PAC-2026-00123"
        full_name:
          type: string
          description: Nombre completo del paciente.
          minLength: 2
          maxLength: 200
          example: "Maria Lopez"
        appointment_date:
          type: string
          format: date
          description: Fecha de la cita médica en formato YYYY-MM-DD.
          example: "2026-04-22"
        insurance_code:
          type: string
          description: Código del seguro médico del paciente.
          enum: [IESS, PRIVADO, NINGUNO]
          example: "IESS"

    PreRegistroResponse:
      allOf:
        - $ref: '#/components/schemas/PreRegistroRequest'
        - type: object
          properties:
            estado:
              type: string
              enum: [PENDIENTE, PROCESADO, RECHAZADO]
              description: Estado actual del procesamiento del pre-registro.
              example: "PENDIENTE"
            fecha_registro:
              type: string
              format: date-time
              description: Timestamp de creación del pre-registro en UTC.
              example: "2026-04-22T09:15:00Z"
            procesado_por:
              type: string
              nullable: true
              description: Identificador del servicio que actualizó el estado.
              example: null

    PreRegistroListResponse:
      type: object
      properties:
        total:
          type: integer
          description: Total de registros que coinciden con los filtros aplicados.
          example: 25
        pagina:
          type: integer
          example: 1
        por_pagina:
          type: integer
          example: 10
        datos:
          type: array
          items:
            $ref: '#/components/schemas/PreRegistroResponse'

    EstadoUpdateRequest:
      type: object
      required: [estado]
      properties:
        estado:
          type: string
          enum: [PROCESADO, RECHAZADO]
          description: Nuevo estado. Solo se permite PROCESADO o RECHAZADO en una actualización.
          example: "PROCESADO"
        procesado_por:
          type: string
          description: Identificador del servicio o usuario que realiza la actualización.
          example: "facturacion-service"
        observacion:
          type: string
          description: Observación opcional sobre el resultado del procesamiento.
          example: "Pre-registro aceptado y factura generada."

    ErrorResponse:
      type: object
      required: [codigo, mensaje, timestamp]
      properties:
        codigo:
          type: string
          description: Código de error interno de la aplicación.
          example: "VALIDACION_FALLIDA"
        mensaje:
          type: string
          description: Descripción legible del error.
          example: "El campo appointment_date no cumple el formato YYYY-MM-DD."
        timestamp:
          type: string
          format: date-time
          example: "2026-04-22T09:20:00Z"

  responses:

    BadRequest:
      description: La solicitud contiene datos inválidos o campos incorrectos.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            codigo: "VALIDACION_FALLIDA"
            mensaje: "El campo insurance_code debe ser IESS, PRIVADO o NINGUNO."
            timestamp: "2026-04-22T09:20:00Z"

    NotFound:
      description: El recurso solicitado no existe.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            codigo: "RECURSO_NO_ENCONTRADO"
            mensaje: "No se encontró un pre-registro con patient_id PAC-2026-99999."
            timestamp: "2026-04-22T09:20:00Z"

    UnprocessableEntity:
      description: El body es JSON válido pero contiene datos semánticamente incorrectos.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            codigo: "FECHA_EN_PASADO"
            mensaje: "La appointment_date no puede ser una fecha anterior a hoy."
            timestamp: "2026-04-22T09:20:00Z"

    InternalError:
      description: Error interno del servidor.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          example:
            codigo: "ERROR_INTERNO"
            mensaje: "Ocurrió un error inesperado. Contacte al administrador."
            timestamp: "2026-04-22T09:20:00Z"
```

### 12.3 Cómo visualizar la especificación

**Opción 1: Swagger UI online**

1. Ir a [https://editor.swagger.io](https://editor.swagger.io)
2. `File > Clear editor`
3. Pegar el contenido del YAML anterior
4. La columna derecha muestra la UI interactiva con todos los endpoints

**Opción 2: Swagger UI con Docker (si está disponible)**

```bash
docker run -p 8081:8080 \
  -e SWAGGER_JSON=/openapi/pre-registros-api.yaml \
  -v $(pwd)/openapi:/openapi \
  swaggerapi/swagger-ui
```

Luego abrir `http://localhost:8081` en el navegador.

**Opción 3: Swagger UI con Node.js**

```bash
npx @redocly/cli preview-docs openapi/pre-registros-api.yaml
```

### 12.4 Decisiones de diseño relevantes para la documentación

Estas decisiones deben mencionarse en el documento de entrega:

**Uso de `allOf` en PreRegistroResponse**

En lugar de duplicar todos los campos de `PreRegistroRequest` en `PreRegistroResponse`, se usa `allOf` para extender el esquema. Esto garantiza que cualquier cambio en los campos del request se refleja automáticamente en el response, siguiendo el principio DRY en el nivel del contrato de la API.

**`PATCH` en lugar de `PUT` para actualizar estado**

`PUT` implica reemplazar el recurso completo. Como solo se actualiza el estado y no el pre-registro completo, `PATCH` es el método HTTP semánticamente correcto según la especificación RFC 5789.

**Paginación en el listado**

El endpoint `GET /pre-registros` incluye parámetros de paginación desde el primer diseño. Una API que no pagina desde el inicio se convierte en un problema cuando el volumen de datos crece, ya que forzar paginación retroactivamente rompe la compatibilidad con los consumidores existentes.

**Separación de `400` y `422`**

`400 Bad Request` aplica cuando la estructura del request es inválida (campo requerido ausente, tipo de dato incorrecto). `422 Unprocessable Entity` aplica cuando la estructura es correcta pero el contenido es semánticamente inválido (fecha en el pasado, patient_id duplicado). Esta distinción le permite al consumidor diferenciar errores de programación de errores de negocio.

---

*Fin de la guía técnica.*