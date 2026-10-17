package co.edu.uniquindio.sistemahotel.backend.dto;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;

public class EstadoHabitacionRequestDTO {

    private EstadoHabitacion estadoHabitacion;

    /**
     * Obtiene el estado de la habitación.
     * @return
     */
    public EstadoHabitacion getEstadoHabitacion() {
        return estadoHabitacion;
    }

    /**
     * Establece el estado de la habitación.
     * @param estadoHabitacion
     */
    public void setEstadoHabitacion(EstadoHabitacion estadoHabitacion) {
        this.estadoHabitacion = estadoHabitacion;
    }
}
