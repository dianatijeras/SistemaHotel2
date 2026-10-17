package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;


public class Habitacion {
    private int numeroHabitacion;
    private TipoHabitacion tipoHabitacion;
    private double precioNoche;
    private int piso;
    private String descripcion;
    private int capacidad;
    private EstadoHabitacion estadoHabitacion;
    private CategoriaHabitacion categoria;


    /**
     * Constructor para crear una nueva habitación. Por defecto, el estado de la habitación se establece como DISPONIBLE.
     * @param numeroHabitacion
     * @param tipoHabitacion
     * @param precioNoche
     * @param piso
     * @param descripcion
     * @param capacidad
     * @param categoria
     */
    public Habitacion(int numeroHabitacion, TipoHabitacion tipoHabitacion,
                      double precioNoche, int piso, String descripcion,
                      int capacidad, CategoriaHabitacion categoria) {
        this.numeroHabitacion = numeroHabitacion;
        this.tipoHabitacion = tipoHabitacion;
        this.precioNoche = precioNoche;
        this.piso = piso;
        this.descripcion = descripcion;
        this.capacidad = capacidad;
        this.categoria = categoria;
        this.estadoHabitacion = EstadoHabitacion.DISPONIBLE;
    }

    /**
     * Método para cambiar el estado de la habitación
     * @param nuevoEstado
     */
    public void cambiarEstado(EstadoHabitacion nuevoEstado) {

        this.estadoHabitacion = nuevoEstado;
    }

    /**
     * Metodo para obtener el numero de la habitacion
     * @return
     */
    public int getNumeroHabitacion() {
        return numeroHabitacion;
    }

    /**
     * Metodo para establecer el numero de la habitacion
     * @param n
     */
    public void setNumeroHabitacion(int n) {
        this.numeroHabitacion = n;
    }

    /**
     * Metodo para obtener el tipo de habitacion
     * @return
     */
    public TipoHabitacion getTipoHabitacion() {
        return tipoHabitacion;
    }

    /**
     * Metodo para establecer el tipo de habitacion
     * @param t
     */
    public void setTipoHabitacion(TipoHabitacion t) {
        this.tipoHabitacion = t;
    }

    /**
     * Metodo para obtener el precio por noche de la habitacion
     * @return
     */
    public double getPrecioNoche() {
        return precioNoche;
    }

    /**
     *  Metodo para establecer el precio por noche de la habitacion
     * @param p
     */
    public void setPrecioNoche(double p) {
        this.precioNoche = p;
    }

    /**
     * Metodo para obtener el piso donde se encuentra la habitacion
     * @return
     */
    public int getPiso() {
        return piso;
    }

    /**
     * Metodo para establecer el piso donde se encuentra la habitacion
     * @param piso
     */
    public void setPiso(int piso) {
        this.piso = piso;
    }

    /**
     * Metodo para obtener la descripcion de la habitacion
     * @return
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Metodo para establecer la descripcion de la habitacion
     * @param d
     */
    public void setDescripcion(String d) {
        this.descripcion = d;
    }

    /**
     * Metodo para obtener la capacidad de la habitacion
     * @return
     */
    public int getCapacidad() {
        return capacidad;
    }

    /**
     * Metodo para establecer la capacidad de la habitacion
     * @param c
     */
    public void setCapacidad(int c) {
        this.capacidad = c;
    }

    /**
     * Metodo para obtener el estado de la habitacion
     * @return
     */
    public EstadoHabitacion getEstadoHabitacion() {
        return estadoHabitacion;
    }

    /**
     * Metodo para establecer el estado de la habitacion
     * @param e
     */
    public void setEstadoHabitacion(EstadoHabitacion e) {
        this.estadoHabitacion = e;
    }

    /**
     * Metodo para obtener la categoria de la habitacion
     * @return
     */
    public CategoriaHabitacion getCategoria() {
        return categoria;
    }

    /**
     * Metodo para establecer la categoria de la habitacion
     * @param c
     */
    public void setCategoria(CategoriaHabitacion c) {
        this.categoria = c;
    }
}
