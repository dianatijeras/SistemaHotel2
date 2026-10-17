package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoPago;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;

import java.time.LocalDateTime;

/**
 * Clase que representa un pago asociado a una estadía en el sistema de gestión hotelera.
 */
public class Pago {

    private String idPago;
    private double monto;
    private LocalDateTime fechaPago;
    private MetodoPago metodoPago;
    private EstadoPago estadoPago;
    private String referenciaPasarela;
    private String observaciones;
    private String concepto;
    private Estadia estadia;

    /**
     * Constructor para crear un nuevo pago. El estado inicial siempre es PENDIENTE: la recepción
     * registra el pago y luego lo aprueba o rechaza mediante una acción explícita
     * @param idPago
     * @param monto
     * @param metodoPago
     * @param concepto
     * @param estadia
     */
    public Pago(String idPago, double monto, MetodoPago metodoPago, String concepto, Estadia estadia) {
        this.idPago = idPago;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.concepto = concepto;
        this.estadia = estadia;
        this.fechaPago = LocalDateTime.now();
        this.estadoPago = EstadoPago.PENDIENTE;
    }

    /**
     * Método para aprobar un pago, cambiando su estado a PAGADO y registrando la referencia interna de la operación.
     * @param referencia
     */
    public void aprobar(String referencia) {
        this.estadoPago = EstadoPago.PAGADO;
        this.referenciaPasarela = referencia;
    }

    /**
     * Método para rechazar un pago, cambiando su estado a RECHAZADO y registrando las observaciones correspondientes.
     * @param observaciones
     */
    public void rechazar(String observaciones) {
        this.estadoPago = EstadoPago.RECHAZADO;
        this.observaciones = observaciones;
    }

    /**
     * Método para reembolsar un pago, cambiando su estado a REEMBOLSADO. Solo se pueden reembolsar pagos que estén en estado PAGADO.
     */
    public void reembolsar() {
        if (this.estadoPago != EstadoPago.PAGADO) {
            throw new IllegalStateException("Solo se pueden reembolsar pagos con estado PAGADO.");
        }
        this.estadoPago = EstadoPago.REEMBOLSADO;
    }

    /**
     * Método para obtener el ID del pago
     * @return
     */
    public String getIdPago() {
        return idPago;
    }

    /**
     * Método para establecer el ID del pago
     * @param id
     */
    public void setIdPago(String id) {
        this.idPago = id;
    }

    /**
     * Método para obtener el monto del pago. El monto es INMUTABLE: se fija al registrar el pago
     * y ninguna transición de estado lo modifica, de modo que aprobar o rechazar nunca altera lo que ya/abonó el huésped.
     * @return
     */
    public double getMonto() {
        return monto;
    }

    /**
     * Método para obtener el concepto o descripción del pago.
     * @return
     */
    public String getConcepto() {
        return concepto;
    }

    /**
     * Método para establecer el concepto o descripción del pago.
     * @param c
     */
    public void setConcepto(String c) {
        this.concepto = c;
    }

    /**
     * Método para obtener la fecha del pago
     * @return
     */
    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    /**
     * Método para establecer la fecha del pago
     * @param f
     */
    public void setFechaPago(LocalDateTime f) {
        this.fechaPago = f;
    }

    /**
     * Método para obtener el método de pago
     * @return
     */
    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    /**
     * Método para establecer el método de pago
     * @param m
     */
    public void setMetodoPago(MetodoPago m) {
        this.metodoPago = m;
    }

    /**
     * Método para obtener el estado del pago
     * @return
     */
    public EstadoPago getEstadoPago() {
        return estadoPago;
    }


    /**
     * Método para obtener la referencia de la pasarela de pago
     * @return
     */
    public String getReferenciaPasarela() {
        return referenciaPasarela;
    }

    /**
     * Método para establecer la referencia de la pasarela de pago
     * @param r
     */
    public void setReferenciaPasarela(String r) {
        this.referenciaPasarela = r;
    }

    /**
     * Método para obtener las observaciones del pago
     * @return
     */
    public String getObservaciones() {
        return observaciones;
    }

    /**
     * Método para establecer las observaciones del pago
     * @param o
     */
    public void setObservaciones(String o) {
        this.observaciones = o;
    }

    /**
     * Método para obtener la estadía asociada al pago
     * @return
     */
    public Estadia getEstadia() {
        return estadia;
    }

    /**
     * Método para establecer la estadía asociada al pago
     * @param e
     */
    public void setEstadia(Estadia e) {
        this.estadia = e;
    }

}

