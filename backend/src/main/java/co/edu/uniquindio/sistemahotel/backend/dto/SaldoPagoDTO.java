package co.edu.uniquindio.sistemahotel.backend.dto;

import java.math.BigDecimal;

public class SaldoPagoDTO {

    private String idEstadia;
    private String idReserva;
    private Integer numeroHabitacion;
    private BigDecimal totalGeneral;
    private BigDecimal totalAprobado;
    private BigDecimal saldoPendiente;
    private int cantidadPagosAprobados;
    private int cantidadPagosPendientes;

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

    public BigDecimal getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(BigDecimal totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public BigDecimal getTotalAprobado() {
        return totalAprobado;
    }

    public void setTotalAprobado(BigDecimal totalAprobado) {
        this.totalAprobado = totalAprobado;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public int getCantidadPagosAprobados() {
        return cantidadPagosAprobados;
    }

    public void setCantidadPagosAprobados(int cantidadPagosAprobados) {
        this.cantidadPagosAprobados = cantidadPagosAprobados;
    }

    public int getCantidadPagosPendientes() {
        return cantidadPagosPendientes;
    }

    public void setCantidadPagosPendientes(int cantidadPagosPendientes) {
        this.cantidadPagosPendientes = cantidadPagosPendientes;
    }
}