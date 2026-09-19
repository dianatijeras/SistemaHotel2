package co.edu.uniquindio.sistemahotel.backend.view;

import co.edu.uniquindio.sistemahotel.backend.controller.ReservaController;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaResponseDTO;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class VentanaRegistrarReserva extends JFrame {

    private final ReservaController reservaController;

    private JTextField txtIdHuesped;
    private JTextField txtNumeroHabitacion;
    private JTextField txtFechaInicio;
    private JTextField txtFechaFin;
    private JTextField txtAdultos;
    private JTextField txtNinos;

    private JButton btnRegistrar;

    public VentanaRegistrarReserva(
            ReservaController reservaController) {

        this.reservaController = reservaController;

        setTitle("Registrar Reserva");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        inicializarComponentes();
    }


    private void inicializarComponentes() {

        JPanel panelPrincipal = new JPanel();

        panelPrincipal.setLayout(
                new GridLayout(7, 2, 10, 10)
        );

        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );


        // RF-01.3
        panelPrincipal.add(
                new JLabel("ID huésped:")
        );

        txtIdHuesped = new JTextField();

        panelPrincipal.add(txtIdHuesped);


        // RF-01.4
        panelPrincipal.add(
                new JLabel("Número habitación:")
        );

        txtNumeroHabitacion = new JTextField();

        panelPrincipal.add(
                txtNumeroHabitacion
        );


        // RF-01.2
        panelPrincipal.add(
                new JLabel(
                        "Fecha entrada (AAAA-MM-DD):"
                )
        );

        txtFechaInicio = new JTextField();

        panelPrincipal.add(txtFechaInicio);


        // RF-01.2
        panelPrincipal.add(
                new JLabel(
                        "Fecha salida (AAAA-MM-DD):"
                )
        );

        txtFechaFin = new JTextField();

        panelPrincipal.add(txtFechaFin);


        // RF-01.6
        panelPrincipal.add(
                new JLabel("Adultos:")
        );

        txtAdultos = new JTextField();

        panelPrincipal.add(txtAdultos);


        // RF-01.6
        panelPrincipal.add(
                new JLabel("Niños:")
        );

        txtNinos = new JTextField();

        panelPrincipal.add(txtNinos);


        btnRegistrar =
                new JButton("Registrar reserva");

        panelPrincipal.add(
                new JLabel("")
        );

        panelPrincipal.add(btnRegistrar);


        btnRegistrar.addActionListener(
                e -> registrarReserva()
        );


        add(panelPrincipal);
    }


    /**
     * FUNCIONALIDAD F-01: Registrar reserva
     *
     * Este método pertenece a la interfaz Swing.
     *
     * Su responsabilidad es:
     * 1. Leer los datos ingresados.
     * 2. Construir el DTO.
     * 3. Llamar al Controller.
     * 4. Mostrar el resultado.
     *
     * NO contiene reglas de negocio.
     */
    private void registrarReserva() {

        try {

            ReservaRequestDTO dto =
                    new ReservaRequestDTO();


            // RF-01.3
            dto.setIdHuesped(
                    txtIdHuesped
                            .getText()
                            .trim()
            );


            // RF-01.4
            dto.setNumeroHabitacion(
                    Integer.parseInt(
                            txtNumeroHabitacion
                                    .getText()
                                    .trim()
                    )
            );


            // RF-01.2
            dto.setFechaInicio(
                    LocalDate.parse(
                            txtFechaInicio
                                    .getText()
                                    .trim()
                    )
            );


            // RF-01.2
            dto.setFechaFin(
                    LocalDate.parse(
                            txtFechaFin
                                    .getText()
                                    .trim()
                    )
            );


            // RF-01.6
            dto.setAdultos(
                    Integer.parseInt(
                            txtAdultos
                                    .getText()
                                    .trim()
                    )
            );


            // RF-01.6
            dto.setNinos(
                    Integer.parseInt(
                            txtNinos
                                    .getText()
                                    .trim()
                    )
            );


            /*
             * AQUÍ PASAMOS DE LA CAPA
             * DE INTERFAZ AL CONTROLLER
             */
            ReservaResponseDTO respuesta =
                    reservaController
                            .registrarReserva(dto);


            JOptionPane.showMessageDialog(
                    this,
                    "Reserva registrada correctamente.\n\n"
                            + "Código: "
                            + respuesta.getCodigoReserva(),
                    "Reserva registrada",
                    JOptionPane.INFORMATION_MESSAGE
            );


            limpiarCampos();

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "El número de habitación, "
                            + "adultos y niños deben ser números.",
                    "Datos incorrectos",
                    JOptionPane.ERROR_MESSAGE
            );

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "No se pudo registrar la reserva",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }


    private void limpiarCampos() {

        txtIdHuesped.setText("");
        txtNumeroHabitacion.setText("");
        txtFechaInicio.setText("");
        txtFechaFin.setText("");
        txtAdultos.setText("");
        txtNinos.setText("");
    }
}
