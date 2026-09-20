package co.edu.uniquindio.sistemahotel.backend.view;

import co.edu.uniquindio.sistemahotel.backend.controller.HabitacionController;
import co.edu.uniquindio.sistemahotel.backend.controller.ReservaController;

import javax.swing.*;
import java.awt.*;

public class VentanaPrincipal extends JFrame {

    private final ReservaController reservaController;
    private final HabitacionController habitacionController;


    public VentanaPrincipal(ReservaController reservaController, HabitacionController habitacionController) {
        this.reservaController = reservaController;
        this.habitacionController = habitacionController;
        configurarVentana();
        inicializarComponentes();
    }


    private void configurarVentana() {
        setTitle("Sistema de Reservas del Hotel");

        setSize(400, 250);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        setResizable(false);
    }


    private void inicializarComponentes() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(3, 1, 10, 20));

        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JLabel titulo = new JLabel("Sistema de Reservas", SwingConstants.CENTER);

        titulo.setFont(new Font("Arial", Font.BOLD, 20));

        JButton btnRegistrar = new JButton("Registrar reserva");

        JButton btnConsultar = new JButton("Consultar disponibilidad");

        panel.add(titulo);

        panel.add(btnRegistrar);

        panel.add(btnConsultar);

        add(panel);


        /*
         * F-01 Registrar reserva
         */
        btnRegistrar
                .addActionListener(
                        e -> {

                            VentanaRegistrarReserva ventana = new VentanaRegistrarReserva(reservaController);
                            ventana.setVisible(true);
                        }
                );


        /*
         * F-02 Consultar disponibilidad
         */
        btnConsultar
                .addActionListener(
                        e -> {

                            VentanaConsultarDisponibilidad ventana = new VentanaConsultarDisponibilidad(habitacionController);
                            ventana.setVisible(true);
                        }
                );
    }
}
