package co.edu.uniquindio.sistemahotel.backend.dto;

import java.time.LocalDate;


public class ReservaActualizacionDTO {

    private int numeroHabitacion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    /**
     * Metodo para obtener el número de habitación de la reserva.
     * @return
     */
    public int getNumeroHabitacion() {
        return numeroHabitacion;
    }

    /**
     * Metodo para establecer el número de habitación de la reserva.
     * @param numeroHabitacion
     */
    public void setNumeroHabitacion(int numeroHabitacion) {
        this.numeroHabitacion = numeroHabitacion;
    }

    /**
     * Metodo para obtener la fecha de entrada.
     * @return
     */
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    /**
     * Metodo para establecer la fecha de entrada.
     * @param fechaInicio
     */
    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    /**
     * Metodo para obtener la fecha de salida.
     * @return
     */
    public LocalDate getFechaFin() {
        return fechaFin;
    }

    /**
     * Metodo para establecer la fecha de salida.
     * @param fechaFin
     */
    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }
}