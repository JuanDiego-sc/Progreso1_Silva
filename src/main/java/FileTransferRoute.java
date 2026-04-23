import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

public class FileTransferRoute extends RouteBuilder {
    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.configure().addRoutesBuilder(new FileTransferRoute());
        main.run();
    }

    @Override
    public void configure() throws Exception {
        // Ruta para procesar archivos CSV
        // ESCRIBIR LA RUTA AQUI VALIDA PARA PROCESAR LOS ARCHIVOS CSV DENTRO
        // DENTRO DE CADA MAQUINA CORRESPONDIENTE
        from("file:input?noop=true")
                .filter(header("CamelFileName").endsWith(".csv"))
                .log("Procesando archivo: ${file:name} a las ${date:now:yyyy-MM-dd HH:mm:ss}")
                .convertBodyTo(String.class)
                .transform().simple("${body.toUpperCase()}")
                .to("file:output")
                .to("file:archived")
                .log("Archivo ${file:name} procesado y movido a output y archived");
    }
}
