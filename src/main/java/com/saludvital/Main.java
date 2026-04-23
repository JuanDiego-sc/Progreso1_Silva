package com.saludvital;

import com.saludvital.route.AdmisionesRoute;

public class Main {

    public static void main(String[] args) throws Exception {

        // Nombre completamente calificado para evitar conflicto con este mismo clase
        org.apache.camel.main.Main camelMain = new org.apache.camel.main.Main();
        camelMain.configure().addRoutesBuilder(new AdmisionesRoute());

        System.out.println("=== SaludVital Integration iniciada. Esperando archivos en data/input/ ===");
        System.out.println("=== Presionar Ctrl+C para detener ===");
        camelMain.run(args);
    }
}
