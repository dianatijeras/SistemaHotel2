package co.edu.uniquindio.sistemahotel.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Habitacion {
    private int numeroHabitacion;
    private TipoHabitacion tipoHabitacion;
    private double precio;
    private int piso;
    private String descripcion;
    private int capacidad;
    private EstadoHabitacion estadoHabitacion;

    public Habitacion(int numeroHabitacion, TipoHabitacion tipoHabitacion, double precio,
                      int piso, String descripcion, int capacidad){
        this.numeroHabitacion = numeroHabitacion;
        this.tipoHabitacion = tipoHabitacion;
        this.precio = precio;
        this.piso = piso;
        this.descripcion = descripcion;
        this.capacidad = capacidad;
        this.estadoHabitacion = EstadoHabitacion.DISPONIBLE;
    }

    public void cambiarEstado(EstadoHabitacion nuevoEstado){
        this.estadoHabitacion = nuevoEstado;
    }

    public boolean estaDisponible(){
        return this.estadoHabitacion == estadoHabitacion.DISPONIBLE;
    }
}
