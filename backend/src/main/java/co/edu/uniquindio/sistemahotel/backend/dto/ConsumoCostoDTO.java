package co.edu.uniquindio.sistemahotel.backend.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta con el detalle de una línea de consumo dentro del desglose de costos (F-09).
 * Expone la cantidad, el precio unitario y el subtotal ya calculado por el backend, de modo que
 * React nunca tiene que multiplicar por su cuenta.
 */
public class ConsumoCostoDTO {

    private String idConsumo;
    private String descripcion;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private String fecha;

    public String getIdConsumo() {
        return idConsumo;
    }

    public void setIdConsumo(String idConsumo) {
        this.idConsumo = idConsumo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}