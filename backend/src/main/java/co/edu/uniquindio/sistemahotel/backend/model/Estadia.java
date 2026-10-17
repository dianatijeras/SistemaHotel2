package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clase que representa una estadía en el sistema de gestión hotelera.
 */
public class Estadia {

    private String idEstadia;
    private LocalDateTime fechaCheckIn;
    private LocalDateTime fechaCheckOut;
    private double totalHabitacion;
    private double totalConsumos;
    private double totalFinal;
    private EstadoEstadia estadoEstadia;
    private Reserva reserva;
    private List<ConsumoEstadia> consumos;

    /**
     * Constructor para crear una nueva estadía a partir de una reserva. El estado inicial será PROGRAMADA, se calculará el total de la habitación y se inicializarán los consumos.
     * @param idEstadia
     * @param reserva
     */
    public Estadia(String idEstadia, Reserva reserva) {
        this.idEstadia = idEstadia;
        this.reserva = reserva;
        this.consumos = new CopyOnWriteArrayList<>();
        this.estadoEstadia = EstadoEstadia.PROGRAMADA;
        this.totalHabitacion = reserva.calcularTotalHabitacion();
        this.totalConsumos = 0.0;
        this.totalFinal = this.totalHabitacion;
    }

    /**
     * Número de noches de la estadía, tomado de las fechas de la reserva que la originó.
     * La fecha de salida es el límite exclusivo del período reservado
     * @return número de noches, o 0 si las fechas no permiten calcularlo
     */
    public long getNoches() {
        return reserva == null ? 0 : reserva.calcularNoches();
    }

    /**
     * Precio por noche configurado para la habitación de la reserva que originó la estadía.
     * @return precio por noche, o 0.0 si no hay habitación asociada
     */
    public double getPrecioNoche() {
        return reserva == null || reserva.getHabitacion() == null
                ? 0.0
                : reserva.getHabitacion().getPrecioNoche();
    }

    /**
     * Método para iniciar la estadía, registrando la fecha de check-in y cambiando el estado a EN_CURSO.
     */
    public void iniciar() {
        this.fechaCheckIn = LocalDateTime.now();
        this.estadoEstadia = EstadoEstadia.EN_CURSO;
    }

    /**
     * Método para finalizar la estadía, registrando la fecha de check-out, cambiando el estado a FINALIZADA y recalculando los totales para
     * reflejar cualquier consumo adicional registrado durante la estadía.
     */
    public void finalizar() {
        this.fechaCheckOut = LocalDateTime.now();
        this.estadoEstadia = EstadoEstadia.FINALIZADA;
        recalcularTotales();
    }

    /**
     * Método para agregar un consumo a la estadía, añadiendo el consumo a la lista de consumos y recalculando los totales para
     * reflejar el nuevo consumo.
     * @param consumo
     */
    public void agregarConsumo(ConsumoEstadia consumo) {
        this.consumos.add(consumo);
        recalcularTotales();
    }

    /**
     * Método para calcular el total final de la estadía, sumando el total de la habitación y el total de consumos.
     * @return el total general de la estadía, sin modificar la entidad
     */
    public double calcularTotalFinal() {
        return Costos.aDouble(Costos.sumar(List.of(
                Costos.aMoneda(reserva.calcularTotalHabitacion()),
                Costos.aMoneda(Costos.aDouble(totalConsumosCalculado())))));
    }

    /**
     * Método para calcular el total de consumos de la estadía, sumando los subtotales de cada consumo registrado.
     * @return
     */
    private BigDecimal totalConsumosCalculado() {
        return Costos.sumar(consumos.stream()
                .map(c -> Costos.subtotalConsumo(c.getCantidad(), c.getPrecioUnitario()))
                .toList());
    }

    /**
     * Método para recalcular los TRES totales de la estadía a partir de sus fuentes: el
     * alojamiento se recalcula desde las fechas y el precio de la habitación de la reserva, y los consumos desde la suma de sus subtotales.
     */
    public void recalcularTotales() {
        double totalHabitacion = reserva.calcularTotalHabitacion();
        double totalConsumos = Costos.aDouble(totalConsumosCalculado());
        double totalFinal = Costos.aDouble(Costos.sumar(List.of(
                Costos.aMoneda(totalHabitacion),
                Costos.aMoneda(totalConsumos))));

        this.totalHabitacion = totalHabitacion;
        this.totalConsumos = totalConsumos;
        this.totalFinal = totalFinal;
    }

    /**
     * Método para obtener el ID de la estadía
     * @return
     */
    public String getIdEstadia() {
        return idEstadia;
    }

    /**
     * Método para establecer el ID de la estadía
     * @param id
     */
    public void setIdEstadia(String id) {
        this.idEstadia = id;
    }

    /**
     * Método para obtener la fecha de check-in de la estadía
     * @return
     */
    public LocalDateTime getFechaCheckIn() {
        return fechaCheckIn;
    }

    /**
     * Método para establecer la fecha de check-in de la estadía
     * @param f
     */
    public void setFechaCheckIn(LocalDateTime f) {
        this.fechaCheckIn = f;
    }

    /**
     * Método para obtener la fecha de check-out de la estadía
     * @return
     */
    public LocalDateTime getFechaCheckOut() {
        return fechaCheckOut;
    }

    /**
     * Método para establecer la fecha de check-out de la estadía
     * @param f
     */
    public void setFechaCheckOut(LocalDateTime f) {
        this.fechaCheckOut = f;
    }

    /**
     * Método para obtener el total de la habitación de la estadía
     * @return
     */
    public double getTotalHabitacion() {
        return totalHabitacion;
    }

    /**
     * Método para establecer el total de la habitación de la estadía
     * @param t
     */
    public void setTotalHabitacion(double t) {
        this.totalHabitacion = t;
    }

    /**
     * Método para obtener el total de consumos de la estadía
     * @return
     */
    public double getTotalConsumos() {
        return totalConsumos;
    }

    /**
     * Método para establecer el total de consumos de la estadía
     * @param t
     */
    public void setTotalConsumos(double t) {
        this.totalConsumos = t;
    }

    /**
     * Método para obtener el total final de la estadía (total habitación + total consumos)
     * @return
     */
    public double getTotalFinal() {
        return totalFinal;
    }

    /**
     * Método para establecer el total final de la estadía (total habitación + total consumos)
     * @param t
     */
    public void setTotalFinal(double t) {
        this.totalFinal = t;
    }

    /**
     * Método para obtener el estado de la estadía
     * @return
     */
    public EstadoEstadia getEstadoEstadia() {
        return estadoEstadia;
    }

    /**
     * Método para establecer el estado de la estadía
     * @param e
     */
    public void setEstadoEstadia(EstadoEstadia e) {
        this.estadoEstadia = e;
    }

    /**
     * Método para obtener la reserva asociada a la estadía
     * @return
     */
    public Reserva getReserva() {
        return reserva;
    }

    /**
     * Método para establecer la reserva asociada a la estadía
     * @param r
     */
    public void setReserva(Reserva r) {
        this.reserva = r;
    }

    /**
     * Método para obtener la lista de consumos asociados a la estadía
     * @return
     */
    public List<ConsumoEstadia> getConsumos() {
        return consumos;
    }

    /**
     * Método para establecer la lista de consumos asociados a la estadía
     * @param c
     */
    public void setConsumos(List<ConsumoEstadia> c) {
        this.consumos = c;
    }
}

