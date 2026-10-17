package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa una reserva hecha por un huésped sobre una habitación,
 * en un rango de fechas determinado.
 */
public class Reserva {

    private String idReserva;
    private String codigoReserva;
    private LocalDate fechaReserva;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDate fechaEstadia;
    private int cantidadPersonas;
    private int adultos;
    private int ninos;
    private EstadoReserva estadoReserva;
    private Huesped huesped;
    private Habitacion habitacion;

    /**
     * Constructor para crear una nueva reserva. Por defecto, el estado de la reserva se establece como RESERVADA.
     * @param idReserva
     * @param fechaInicio
     * @param fechaFin
     * @param adultos
     * @param ninos
     * @param huesped
     * @param habitacion
     */
    public Reserva(String idReserva, LocalDate fechaInicio, LocalDate fechaFin, int adultos, int ninos, Huesped huesped, Habitacion habitacion) {
        this.idReserva = idReserva;
        this.codigoReserva = idReserva;
        this.fechaReserva = LocalDate.now();
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.adultos = adultos;
        this.ninos = ninos;
        this.cantidadPersonas = adultos + ninos;
        this.huesped = huesped;
        this.habitacion = habitacion;
        this.estadoReserva = EstadoReserva.RESERVADA;
    }

    /**
     * Método para calcular el número de noches de la reserva. Delega en {@link Costos#noches},
     * @return
     */
    public long calcularNoches() {
        return Costos.noches(fechaInicio, fechaFin);
    }

    /**
     * Método para calcular el subtotal de alojamiento de la reserva (noches x precio por noche),
     * usando la misma regla de precisión que el resto del sistema.
     * @return
     */
    public double calcularTotalHabitacion() {
        return Costos.aDouble(Costos.subtotalAlojamiento(calcularNoches(), habitacion.getPrecioNoche()));
    }

    /**
     * Método para obtener el ID de la reserva
     * @return
     */
    public String getIdReserva() {
        return idReserva;
    }

    /**
     * Método para establecer el ID de la reserva
     * @param id
     */
    public void setIdReserva(String id) {
        this.idReserva = id;
    }

    /**
     * Método para obtener el código de la reserva
     * @return
     */
    public String getCodigoReserva() {
        return codigoReserva;
    }

    /**
     * Método para establecer el código de la reserva
     * @param codigoReserva
     */
    public void setCodigoReserva(String codigoReserva) {
        this.codigoReserva = codigoReserva;
    }

    /**
     * Método para obtener la fecha de la reserva
     * @return
     */
    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    /**
     * Método para establecer la fecha de la reserva
     * @param f
     */
    public void setFechaReserva(LocalDate f) {
        this.fechaReserva = f;
    }

    /**
     * Método para obtener la fecha de inicio de la reserva
     * @return
     */
    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    /**
     * Método para establecer la fecha de inicio de la reserva
     * @param f
     */
    public void setFechaInicio(LocalDate f) {
        this.fechaInicio = f;
    }

    /**
     * Método para obtener la fecha de fin de la reserva
     * @return
     */
    public LocalDate getFechaFin() {
        return fechaFin;
    }

    /**
     * Método para establecer la fecha de fin de la reserva
     * @param f
     */
    public void setFechaFin(LocalDate f) {
        this.fechaFin = f;
    }

    /**
     * Método para obtener la fecha de estadía de la reserva
     * @return
     */
    public LocalDate getFechaEstadia() {
        return fechaEstadia;
    }

    /**
     * Método para establecer la fecha de estadía de la reserva
     * @param f
     */
    public void setFechaEstadia(LocalDate f) {
        this.fechaEstadia = f;
    }

    /**
     * Método para obtener la cantidad total de personas en la reserva (adultos + niños)
     * @return
     */
    public int getCantidadPersonas() {
        return cantidadPersonas;
    }

    /**
     * Método para establecer la cantidad total de personas en la reserva (adultos + niños)
     * @param c
     */
    public void setCantidadPersonas(int c) {
        this.cantidadPersonas = c;
    }

    /**
     * Método para obtener la cantidad de adultos en la reserva
     * @return
     */
    public int getAdultos() {
        return adultos;
    }

    /**
     * Método para establecer la cantidad de adultos en la reserva
     * @param a
     */
    public void setAdultos(int a) {
        this.adultos = a;
    }

    /**
     * Método para obtener la cantidad de niños en la reserva
     * @return
     */
    public int getNinos() {
        return ninos;
    }

    /**
     * Método para establecer la cantidad de niños en la reserva
     * @param n
     */
    public void setNinos(int n) {
        this.ninos = n;
    }

    /**
     * Método para obtener el estado de la reserva
     * @return
     */
    public EstadoReserva getEstadoReserva() {
        return estadoReserva;
    }

    /**
     * Método para establecer el estado de la reserva
     * @param e
     */
    public void setEstadoReserva(EstadoReserva e) {
        this.estadoReserva = e;
    }

    /**
     * Método para obtener el huésped asociado a la reserva
     * @return
     */
    public Huesped getHuesped() {
        return huesped;
    }

    /**
     * Método para establecer el huésped asociado a la reserva
     * @param h
     */
    public void setHuesped(Huesped h) {
        this.huesped = h;
    }

    /**
     * Método para obtener la habitación asociada a la reserva
     * @return
     */
    public Habitacion getHabitacion() {
        return habitacion;
    }

    /**
     * Método para establecer la habitación asociada a la reserva
     * @param h
     */
    public void setHabitacion(Habitacion h) {
        this.habitacion = h;
    }
}
