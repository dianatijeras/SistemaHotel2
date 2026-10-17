package co.edu.uniquindio.sistemahotel.backend.model.estado;


import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Clase que representa el estado de una habitación disponible en el sistema de gestión hotelera.
 * Extiende de {@link EstadoHabitacionNoOcupada}, lo que indica que la habitación no está ocupada.
 */
final class HabitacionDisponible extends EstadoHabitacionNoOcupada {

    @Override
    public EstadoHabitacion estado() {
        return EstadoHabitacion.DISPONIBLE;
    }

    @Override
    public boolean admiteHuespedes() {
        return true;
    }

    @Override
    public boolean admiteOcuparse() {
        return true;
    }

    @Override
    public String mensajeDeCheckIn(Habitacion hab) {
        return null;
    }

    @Override
    public boolean admiteMarcarEnLimpieza() {
        // Una habitación libre puede quedarse pendiente de limpieza: es el caso de una revisión o de un aviso de mantenimiento del que nadie se ha ocupado todavía.
        return true;
    }

    @Override
    public boolean admiteFinalizarLimpieza() {
        // Nada que confirmar: todavía nadie ha marcado la limpieza.
        return false;
    }

    @Override
    public boolean admiteBloqueo() {
        return true;
    }
}