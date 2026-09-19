package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.controller.ReservaController;
import co.edu.uniquindio.sistemahotel.backend.view.VentanaRegistrarReserva;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {

        // Creamos manualmente la aplicación Spring
        SpringApplication application =
                new SpringApplication(BackendApplication.class);

        // IMPORTANTE:
        // Permitimos que Spring Boot utilice interfaz gráfica Swing
        application.setHeadless(false);

        // Iniciamos Spring
        ConfigurableApplicationContext context =
                application.run(args);

        // Abrimos la ventana Swing
        SwingUtilities.invokeLater(() -> {

            ReservaController reservaController =
                    context.getBean(ReservaController.class);

            VentanaRegistrarReserva ventana =
                    new VentanaRegistrarReserva(
                            reservaController
                    );

            ventana.setVisible(true);
        });
    }
}