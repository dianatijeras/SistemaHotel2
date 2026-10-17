package co.edu.uniquindio.sistemahotel.backend.dto;

import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;

public class PagoRequestDTO {

    private String idEstadia;
    private double monto;
    private MetodoPago metodoPago;
    private String referenciaPasarela;
    private String concepto;

    /**
     * Metodo para obtener el ID de la estadía asociada al pago.
     * @return
     */
    public String getIdEstadia() {
        return idEstadia;
    }

    /**
     * Metodo para establecer el ID de la estadía asociada al pago.
     * @param id
     */
    public void setIdEstadia(String id) {
        this.idEstadia = id;
    }

    /**
     * Metodo para obtener el monto del pago.
     * @return
     */
    public double getMonto() {
        return monto;
    }

    /**
     * Metodo para establecer el monto del pago.
     * @param m
     */
    public void setMonto(double m) {
        this.monto = m;
    }

    /**
     * Metodo para obtener el método de pago utilizado en la solicitud de pago.
     * @return
     */
    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    /**
     * Metodo para establecer el método de pago utilizado en la solicitud de pago.
     * @param m
     */
    public void setMetodoPago(MetodoPago m) {
        this.metodoPago = m;
    }

    /**
     * Metodo para obtener la referencia de la pasarela de pago, si el método de pago es una pasarela.
     * @return
     */
    public String getReferenciaPasarela() {
        return referenciaPasarela;
    }

    /**
     * Metodo para establecer la referencia de la pasarela de pago, si el método de pago es una pasarela.
     * @param r
     */
    public void setReferenciaPasarela(String r) {
        this.referenciaPasarela = r;
    }

    /**
     * Metodo para obtener el concepto o descripción del pago, que queda registrado en el
     * historial junto al importe (F-10).
     * @return
     */
    public String getConcepto() {
        return concepto;
    }

    /**
     * Metodo para establecer el concepto o descripción del pago.
     * @param c
     */
    public void setConcepto(String c) {
        this.concepto = c;
    }
}
