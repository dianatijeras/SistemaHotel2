package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;

/**
 * Clase que representa una categoría de habitación en el sistema de gestión hotelera.
 */
public class CategoriaHabitacion {

    private String idCategoria;
    private String nombre;
    private String descripcion;
    private int capacidadBase;
    private int cantidadPersonas;
    private double precioBase;
    private TipoHabitacion tipo;

    /**
     * Constructor para crear una nueva categoría de habitación
     * @param idCategoria
     * @param nombre
     * @param descripcion
     * @param capacidadBase
     * @param cantidadPersonas
     * @param precioBase
     * @param tipo
     */
    public CategoriaHabitacion(String idCategoria, String nombre, String descripcion, int capacidadBase, int cantidadPersonas, double precioBase, TipoHabitacion tipo) {
        this.idCategoria = idCategoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.capacidadBase = capacidadBase;
        this.cantidadPersonas = cantidadPersonas;
        this.precioBase = precioBase;
        this.tipo = tipo;
    }

    /**
     * Método para obtener el ID de la categoría de habitación
     * @return
     */
    public String getIdCategoria() {
        return idCategoria;
    }

    /**
     * Método para establecer el ID de la categoría de habitación
     * @param idCategoria
     */
    public void setIdCategoria(String idCategoria) {
        this.idCategoria = idCategoria;
    }

    /**
     * Método para obtener el nombre de la categoría de habitación
     * @return
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Método para establecer el nombre de la categoría de habitación
     * @param nombre
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Método para obtener la descripción de la categoría de habitación
     * @return
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Método para establecer la descripción de la categoría de habitación
     * @param descripcion
     */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Método para obtener la capacidad base de la categoría de habitación
     * @return
     */
    public int getCapacidadBase() {
        return capacidadBase;
    }

    /**
     * Método para establecer la capacidad base de la categoría de habitación
     * @param capacidadBase
     */
    public void setCapacidadBase(int capacidadBase) {
        this.capacidadBase = capacidadBase;
    }

    /**
     * Método para obtener la cantidad de personas permitida en la categoría de habitación
     * @return
     */
    public int getCantidadPersonas() {
        return cantidadPersonas;
    }

    /**
     * Método para establecer la cantidad de personas permitida en la categoría de habitación
     * @param cantidadPersonas
     */
    public void setCantidadPersonas(int cantidadPersonas) {
        this.cantidadPersonas = cantidadPersonas;
    }

    /**
     * Método para obtener el precio base de la categoría de habitación
     * @return
     */
    public double getPrecioBase() {
        return precioBase;
    }

    /**
     * Método para establecer el precio base de la categoría de habitación
     * @param precioBase
     */
    public void setPrecioBase(double precioBase) {
        this.precioBase = precioBase;
    }

    /**
     * Método para obtener el tipo de habitación asociado a la categoría de habitación
     * @return
     */
    public TipoHabitacion getTipo() {
        return tipo;
    }

    /**
     * Método para establecer el tipo de habitación asociado a la categoría de habitación
     * @param tipo
     */
    public void setTipo(TipoHabitacion tipo) {
        this.tipo = tipo;
    }
}

