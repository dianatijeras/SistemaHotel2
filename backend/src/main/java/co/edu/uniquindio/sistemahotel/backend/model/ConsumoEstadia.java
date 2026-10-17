package co.edu.uniquindio.sistemahotel.backend.model;

import java.time.LocalDate;

/**
 * Clase que representa un consumo asociado a una estadía en el sistema de gestión hotelera.
 */
public class ConsumoEstadia {

    private String idConsumo;
    private String descripcion;
    private LocalDate fecha;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;

    /**
     * Constructor para crear un consumo asociado a una estadía.
     * @param idConsumo
     * @param descripcion
     * @param fecha
     * @param cantidad
     * @param precioUnitario
     */
    public ConsumoEstadia(String idConsumo, String descripcion, LocalDate fecha, int cantidad, double precioUnitario) {
        this.idConsumo = idConsumo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = calcularSubtotal();
    }

    /**
     * Método para calcular el subtotal del consumo multiplicando la cantidad por el precio unitario.
     * @return
     */
    public double calcularSubtotal() {
        return Costos.aDouble(Costos.subtotalConsumo(cantidad, precioUnitario));
    }

    /**
     * Métodos getters y setters para los atributos de la clase ConsumoEstadia.
     * @return
     */
    public String getIdConsumo() {
        return idConsumo;
    }

    /**
     * Método para establecer el ID del consumo.
     * @param id
     */
    public void setIdConsumo(String id) {
        this.idConsumo = id;
    }

    /**
     * Método para obtener la descripción del consumo.
     * @return
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Método para establecer la descripción del consumo.
     * @param d
     */
    public void setDescripcion(String d) {
        this.descripcion = d;
    }

    /**
     * Método para obtener la fecha del consumo.
     * @return
     */
    public LocalDate getFecha() {
        return fecha;
    }

    /**
     * Método para establecer la fecha del consumo.
     * @param f
     */
    public void setFecha(LocalDate f) {
        this.fecha = f;
    }

    /**
     * Método para obtener la cantidad del consumo.
     * @return
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * Método para establecer la cantidad del consumo. Recalcula el subtotal para que el valor
     * sea consistente con la cantidad y el precio unitario.
     * @param c
     */
    public void setCantidad(int c) {
        this.cantidad = c;
        this.subtotal = calcularSubtotal();
    }

    /**
     * Método para obtener el precio unitario del consumo.
     * @return
     */
    public double getPrecioUnitario() {
        return precioUnitario;
    }

    /**
     * Método para establecer el precio unitario del consumo. Recalcula el subtotal por la misma
     * razón que {@link #setCantidad(int)}.
     * @param p
     */
    public void setPrecioUnitario(double p) {
        this.precioUnitario = p;
        this.subtotal = calcularSubtotal();
    }

    /**
     * Método para obtener el subtotal del consumo.
     * @return
     */
    public double getSubtotal() {
        return subtotal;
    }

}

