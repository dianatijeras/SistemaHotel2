package co.edu.uniquindio.sistemahotel.backend.model.estado;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Clase que representa el estado de una habitación que está fuera de servicio.
 * Extiende de {@link EstadoHabitacionNoOcupada}, lo que indica que la habitación no está ocupada.
 */
final class HabitacionFueraDeServicio extends EstadoHabitacionNoOcupada {

    @Override
    public EstadoHabitacion estado() {
        return EstadoHabitacion.FUERA_DE_SERVICIO;
    }

    @Override
    public boolean admiteHuespedes() {
        return false;
    }

    @Override
    public boolean admiteOcuparse() {
        return false;
    }

    @Override
    public String mensajeDeCheckIn(Habitacion hab) {
        return "No se puede hacer check-in porque la habitación " + hab.getNumeroHabitacion() + " está en estado " + hab.getEstadoHabitacion() + ".";
    }

    @Override
    public boolean admiteMarcarEnLimpieza() {
        return false;
    }

    @Override
    public boolean admiteFinalizarLimpieza() {
        return false;
    }

    @Override
    public boolean admiteBloqueo() {
        return false;
    }

    @Override
    public String mensajeDeBloqueo(Habitacion hab) {
        return "La habitación " + hab.getNumeroHabitacion() + " no puede bloquearse desde: " + hab.getEstadoHabitacion() + ". Solo se bloquea desde DISPONIBLE o EN_LIMPIEZA.";
    }
}