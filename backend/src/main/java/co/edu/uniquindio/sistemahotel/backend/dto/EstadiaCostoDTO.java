package co.edu.uniquindio.sistemahotel.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class EstadiaCostoDTO {

    private String idEstadia;
    private String idReserva;
    private Integer numeroHabitacion;
    private long noches;
    private BigDecimal precioNoche;
    private BigDecimal subtotalAlojamiento;
    private List<ConsumoCostoDTO> consumos;
    private BigDecimal subtotalConsumos;
    private BigDecimal totalGeneral;

    public String getIdEstadia() {
        return idEstadia;
    }

    public void setIdEstadia(String idEstadia) {
        this.idEstadia = idEstadia;
    }

    public String getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(String idReserva) {
        this.idReserva = idReserva;
    }

    public Integer getNumeroHabitacion() {
        return numeroHabitacion;
    }

    public void setNumeroHabitacion(Integer numeroHabitacion) {
        this.numeroHabitacion = numeroHabitacion;
    }

    public long getNoches() {
        return noches;
    }

    public void setNoches(long noches) {
        this.noches = noches;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public BigDecimal getSubtotalAlojamiento() {
        return subtotalAlojamiento;
    }

    public void setSubtotalAlojamiento(BigDecimal subtotalAlojamiento) {
        this.subtotalAlojamiento = subtotalAlojamiento;
    }

    public List<ConsumoCostoDTO> getConsumos() {
        return consumos;
    }

    public void setConsumos(List<ConsumoCostoDTO> consumos) {
        this.consumos = consumos;
    }

    public BigDecimal getSubtotalConsumos() {
        return subtotalConsumos;
    }

    public void setSubtotalConsumos(BigDecimal subtotalConsumos) {
        this.subtotalConsumos = subtotalConsumos;
    }

    public BigDecimal getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(BigDecimal totalGeneral) {
        this.totalGeneral = totalGeneral;
    }
}