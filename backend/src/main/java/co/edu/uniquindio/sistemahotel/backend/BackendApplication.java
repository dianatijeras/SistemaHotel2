package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.controller.HabitacionController;
import co.edu.uniquindio.sistemahotel.backend.controller.ReservaController;
import co.edu.uniquindio.sistemahotel.backend.view.VentanaPrincipal;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {

        SpringApplication application = new SpringApplication(BackendApplication.class);

        /*
         * Spring Boot debe permitir
         * el entorno gráfico de Swing.
         */
        application.setHeadless(false);

        ConfigurableApplicationContext context = application.run(args);

        SwingUtilities.invokeLater(() -> {

            ReservaController reservaController = context.getBean(ReservaController.class);

            HabitacionController habitacionController = context.getBean(HabitacionController.class);

            VentanaPrincipal ventana = new VentanaPrincipal(reservaController, habitacionController);

            ventana.setVisible(true);
        });
    }
}