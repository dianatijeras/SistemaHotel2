package co.edu.uniquindio.sistemahotel.backend.view;

import co.edu.uniquindio.sistemahotel.backend.controller.HabitacionController;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class VentanaConsultarDisponibilidad
        extends JFrame {

    private final HabitacionController habitacionController;

    private JTextField txtFechaInicio;
    private JTextField txtFechaFin;

    private JButton btnConsultar;

    private JTable tablaHabitaciones;

    private DefaultTableModel modeloTabla;


    public VentanaConsultarDisponibilidad(HabitacionController habitacionController) {
        this.habitacionController = habitacionController;
        configurarVentana();
        inicializarComponentes();
    }


    private void configurarVentana() {
        setTitle("Consultar disponibilidad");

        setSize(800, 500);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setLocationRelativeTo(null);
    }


    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));


        /*
         * Capturar fecha de entrada.
         */
        panelBusqueda.add(new JLabel("Fecha entrada:"));

        txtFechaInicio = new JTextField(10);

        panelBusqueda.add(txtFechaInicio);


        /*
         * Capturar fecha de salida.
         */
        panelBusqueda.add(new JLabel("Fecha salida:"));

        txtFechaFin = new JTextField(10);


        panelBusqueda.add(txtFechaFin);

        btnConsultar = new JButton("Consultar disponibilidad");

        panelBusqueda.add(btnConsultar);

        /*
         * Tabla donde se mostrarán
         * únicamente las habitaciones disponibles.
         */
        String[] columnas = {

                "Número",
                "Tipo",
                "Capacidad",
                "Precio",
                "Piso",
                "Descripción"
        };


        modeloTabla = new DefaultTableModel(columnas, 0) {

                    /*
                     * Evitamos que el usuario modifique
                     * directamente el contenido.
                     */
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
        };


        tablaHabitaciones = new JTable(modeloTabla);

        JScrollPane scroll = new JScrollPane(tablaHabitaciones);

        add(panelBusqueda, BorderLayout.NORTH);

        add(scroll, BorderLayout.CENTER);

        btnConsultar.addActionListener(e -> consultarDisponibilidad());
    }


    /**
     * FUNCIONALIDAD F-02:
     * Consultar disponibilidad.
     *
     * Responsabilidad de Swing:
     *
     * 1. Capturar fechas.
     * 2. Convertirlas a LocalDate.
     * 3. Llamar al Controller.
     * 4. Mostrar los resultados.
     *
     * No contiene reglas de negocio.
     */
    private void consultarDisponibilidad() {

        try {

            LocalDate fechaInicio = LocalDate.parse(txtFechaInicio.getText().trim());


            LocalDate fechaFin = LocalDate.parse(txtFechaFin.getText().trim());


            /*
             * Cambio de capa:
             *
             * Swing a Controller
             */
            List<Habitacion> habitacionesDisponibles = habitacionController.consultarDisponibilidad(fechaInicio, fechaFin);

            mostrarHabitaciones(habitacionesDisponibles);

        } catch (DateTimeParseException ex) {

            JOptionPane.showMessageDialog(this, "Las fechas deben tener el formato " + "AAAA-MM-DD.\n" + "Ejemplo: 2026-10-15", "Formato de fecha incorrecto", JOptionPane.ERROR_MESSAGE);


        } catch (Exception ex) {

            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo consultar " + "la disponibilidad", JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Muestra únicamente las habitaciones
     * entregadas por la capa de negocio.
     */
    private void mostrarHabitaciones(List<Habitacion> habitaciones) {

        /*
         * Limpiamos resultados anteriores.
         */
        modeloTabla.setRowCount(0);


        for (Habitacion habitacion : habitaciones) {
            Object[] fila = {

                    habitacion
                            .getNumeroHabitacion(),

                    habitacion
                            .getTipoHabitacion(),

                    habitacion
                            .getCapacidad(),

                    habitacion
                            .getPrecio(),

                    habitacion
                            .getPiso(),

                    habitacion
                            .getDescripcion()
            };


            modeloTabla.addRow(fila);
        }


        if (habitaciones.isEmpty()) {

            JOptionPane.showMessageDialog(this, "No hay habitaciones disponibles " + "para las fechas seleccionadas.", "Sin disponibilidad", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
