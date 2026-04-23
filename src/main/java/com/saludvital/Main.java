package com.saludvital;

import com.saludvital.route.AdmisionesRoute;
import org.apache.camel.main.Main;

public class Main {

    public static void main(String[] args) throws Exception {

        Main camelMain = new Main();
        camelMain.configure().addRoutesBuilder(new AdmisionesRoute());

        System.out.println("=== SaludVital Integration iniciada. Esperando archivos en data/input/ ===");
        System.out.println("=== Presionar Ctrl+C para detener ===");
        camelMain.run(args);
    }
}
