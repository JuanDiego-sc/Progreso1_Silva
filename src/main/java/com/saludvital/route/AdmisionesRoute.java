package com.saludvital.route;

import com.saludvital.processor.CsvValidationProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdmisionesRoute extends RouteBuilder {

    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    @Override
    public void configure() {

        /*
         * Manejo global de excepciones no controladas.
         * Cualquier excepcion inesperada dentro de la ruta mueve el archivo
         * a data/error y registra el motivo antes de marcar el exchange como manejado.
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
         * Monitorea data/input cada 5 segundos buscando archivos .csv.
         *
         * Opciones relevantes del endpoint file:
         *   delete=true          - elimina el archivo de input tras procesarlo (evita reprocesamiento)
         *   readLock=changed     - espera a que el archivo deje de crecer antes de leerlo
         *   readLockCheckInterval/readLockMinAge - parametros de estabilidad del readLock
         */
        from("file:data/input" +
             "?delete=true" +
             "&delay=5000" +
             "&include=.*\\.csv" +
             "&autoCreate=true" +
             "&readLock=changed" +
             "&readLockCheckInterval=1000" +
             "&readLockMinAge=500")
            .routeId("saludvital-admisiones-route")

            // Guardar nombre original y generar nombre con timestamp para archivado
            .process(exchange -> {
                String originalName = exchange.getIn().getHeader("CamelFileName", String.class);
                exchange.setProperty("originalFileName", originalName);

                String timestamp    = LocalDateTime.now().format(TIMESTAMP_FMT);
                String nameWithoutExt = originalName.replaceAll("\\.csv$", "");
                String archivedName   = nameWithoutExt + "_" + timestamp + ".csv";
                exchange.setProperty("archivedFileName", archivedName);

                log.info("[DETECTADO] Archivo en cola: {}", originalName);
            })

            .process(new CsvValidationProcessor())

            .choice()

                // RAMA VALIDA: copia a output y archive
                .when(exchangeProperty("valid").isEqualTo(true))
                    .log("[PROCESANDO] Archivo valido: ${exchangeProperty.originalFileName}")
                    .multicast()
                        .to("file:data/output?fileName=${exchangeProperty.originalFileName}")
                        .to("file:data/archive?fileName=${exchangeProperty.archivedFileName}")
                    .end()
                    .log("[EXITO] Output: ${exchangeProperty.originalFileName} " +
                         "| Archive: ${exchangeProperty.archivedFileName}")

                // RAMA INVALIDA: copia a error y archive para trazabilidad
                .otherwise()
                    .log("[RECHAZADO] Archivo: ${exchangeProperty.originalFileName} " +
                         "| Motivo: ${exchangeProperty.failReason}")
                    .multicast()
                        .to("file:data/error?fileName=${exchangeProperty.originalFileName}")
                        .to("file:data/archive?fileName=${exchangeProperty.archivedFileName}")
                    .end()
                    .log("[TRAZABILIDAD] Archivo invalido archivado: ${exchangeProperty.archivedFileName}")

            .end();
    }
}
