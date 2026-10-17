package co.edu.uniquindio.sistemahotel.backend.dto;

import java.time.LocalDate;

/**
 * DTO para la creación de reservas.
 */
public class ReservaRequestDTO {

    private String idHuesped;
    private int numeroHabitacion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int adultos;
    private int ninos;

    /**
     * Metodo para obtener el ID del huésped asociado a la reserva.
     * @return
     */
    public String getIdHuesped() {
        return idHuesped;
    }

    /**
     * Metodo para establecer el ID del huésped asociado a la reserva.
     * @param idHuesped
     */
    public void setIdHuesped(String idHuesped) {
        this.idHuesped = idHuesped;
    }

    /**
     *
     * @return
     */
    public int getNumeroHabitacion() {
        return numeroHabitacion;
    }

    /**
     * Metodo para establecer el número de habitación asociado a la reserva.
     * @param n
     */
    public void setNumeroHabitacion(int n) {
        this.numeroHabitacion = n;
    }

    /**
     * Metodo para obtener la fecha de inicio de la reserva.
     * @return
     */
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    /**
     * Metodo para establecer la fecha de inicio de la reserva.
     * @param f
     */
    public void setFechaInicio(LocalDate f) {
        this.fechaInicio = f;
    }

    /**
     * Metodo para obtener la fecha de fin de la reserva.
     * @return
     */
    public LocalDate getFechaFin() {
        return fechaFin;
    }

    /**
     * Metodo para establecer la fecha de fin de la reserva.
     * @param f
     */
    public void setFechaFin(LocalDate f) {
        this.fechaFin = f;
    }

    /**
     * Metodo para obtener el número de adultos asociados a la reserva.
     * @return
     */
    public int getAdultos() {
        return adultos;
    }

    /**
     * Metodo para establecer el número de adultos asociados a la reserva.
     * @param a
     */
    public void setAdultos(int a) {
        this.adultos = a;
    }

    /**
     * Metodo para obtener el número de niños asociados a la reserva.
     * @return
     */
    public int getNinos() {
        return ninos;
    }

    /**
     * Metodo para establecer el número de niños asociados a la reserva.
     * @param n
     */
    public void setNinos(int n) {
        this.ninos = n;
    }
}
