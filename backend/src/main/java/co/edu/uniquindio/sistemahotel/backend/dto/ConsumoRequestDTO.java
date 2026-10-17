package co.edu.uniquindio.sistemahotel.backend.dto;

/**
 * DTO para representar la información de un consumo realizado durante una estadía en el sistema de gestión hotelera.
 */
public class ConsumoRequestDTO {

    private String idEstadia;
    private String descripcion;
    private int cantidad;
    private double precioUnitario;

    /**
     * Metodo para obtener el ID de la estadía a la que está asociado el consumo.
     * @return
     */
    public String getIdEstadia() {
        return idEstadia;
    }

    /**
     * Metodo para establecer el ID de la estadía a la que está asociado el consumo.
     * @param id
     */
    public void setIdEstadia(String id) {
        this.idEstadia = id;
    }

    /**
     * Metodo para obtener la descripción del consumo.
     * @return
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Metodo para establecer la descripción del consumo.
     * @param d
     */
    public void setDescripcion(String d) {
        this.descripcion = d;
    }

    /**
     * Metodo para obtener la cantidad del consumo.
     * @return
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * Metodo para establecer la cantidad del consumo.
     * @param c
     */
    public void setCantidad(int c) {
        this.cantidad = c;
    }

    /**
     * Metodo para obtener el precio unitario del consumo.
     * @return
     */
    public double getPrecioUnitario() {
        return precioUnitario;
    }

    /**
     * Metodo para establecer el precio unitario del consumo.
     * @param p
     */
    public void setPrecioUnitario(double p) {
        this.precioUnitario = p;
    }
}
