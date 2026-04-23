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

    private static final List<String> REQUIRED_HEADERS =
        Arrays.asList("patient_id", "full_name", "appointment_date", "insurance_code");

    private static final Set<String> VALID_INSURANCE_CODES =
        Set.of("IESS", "PRIVADO", "NINGUNO");

    private static final Pattern DATE_PATTERN =
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    @Override
    public void process(Exchange exchange) {

        String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
        String content  = exchange.getIn().getBody(String.class);

        log.info("[INICIO] Procesando archivo: {}", fileName);

        String[] lines = content.split("\\r?\\n");

        if (lines.length < 2) {
            markInvalid(exchange, fileName, "El archivo esta vacio o solo tiene encabezado.");
            return;
        }

        String headerLine = lines[0].trim();
        List<String> actualHeaders = Arrays.asList(headerLine.split(","));

        if (!actualHeaders.equals(REQUIRED_HEADERS)) {
            markInvalid(exchange, fileName,
                "Encabezado incorrecto. Recibido: [" + headerLine +
                "] Esperado: [" + String.join(",", REQUIRED_HEADERS) + "]");
            return;
        }

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // -1 conserva campos vacios al final de la linea
            String[] fields = line.split(",", -1);

            if (fields.length != REQUIRED_HEADERS.size()) {
                markInvalid(exchange, fileName,
                    "Fila " + (i + 1) + " tiene " + fields.length +
                    " columnas, se esperaban " + REQUIRED_HEADERS.size() + ".");
                return;
            }

            String patientId       = fields[0].trim();
            String fullName        = fields[1].trim();
            String appointmentDate = fields[2].trim();
            String insuranceCode   = fields[3].trim();

            if (patientId.isEmpty() || fullName.isEmpty() ||
                appointmentDate.isEmpty() || insuranceCode.isEmpty()) {
                markInvalid(exchange, fileName,
                    "Fila " + (i + 1) + " tiene uno o mas campos vacios.");
                return;
            }

            if (!DATE_PATTERN.matcher(appointmentDate).matches()) {
                markInvalid(exchange, fileName,
                    "Fila " + (i + 1) + " tiene appointment_date con formato invalido: '"
                    + appointmentDate + "'. Se espera YYYY-MM-DD.");
                return;
            }

            if (!VALID_INSURANCE_CODES.contains(insuranceCode)) {
                markInvalid(exchange, fileName,
                    "Fila " + (i + 1) + " tiene insurance_code invalido: '"
                    + insuranceCode + "'. Valores permitidos: " + VALID_INSURANCE_CODES);
                return;
            }
        }

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
