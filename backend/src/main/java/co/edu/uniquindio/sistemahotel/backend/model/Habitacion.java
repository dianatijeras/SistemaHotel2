package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class Habitacion {
    private int numeroHabitacion;
    private TipoHabitacion tipoHabitacion;
    private double precio;
    private int piso;
    private String descripcion;
    private int capacidad;
    private EstadoHabitacion estadoHabitacion;

    public void cambiarEstado(EstadoHabitacion nuevoEstado){
        this.estadoHabitacion = nuevoEstado;
    }

    public boolean estaDisponible(){
        return this.estadoHabitacion == EstadoHabitacion.DISPONIBLE;
    }
}
