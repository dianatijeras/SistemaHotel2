package co.edu.uniquindio.sistemahotel.backend.model.estado;


import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Clase que representa el estado de una habitación en mantenimiento.
 * Extiende de {@link EstadoHabitacionNoOcupada}, lo que indica que la habitación no está ocupada.
 */
final class HabitacionMantenimiento extends EstadoHabitacionNoOcupada {

    @Override
    public EstadoHabitacion estado() {
        return EstadoHabitacion.MANTENIMIENTO;
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
        return "No se puede hacer check-in porque la habitación " + hab.getNumeroHabitacion()
                + " está en estado " + hab.getEstadoHabitacion() + ".";
    }

    @Override
    public boolean admiteMarcarEnLimpieza() {
        return false;
    }

    @Override
    public boolean admiteFinalizarLimpieza() {
        // La habitación no pasó por limpieza: no hay nada que confirmar.
        return false;
    }

    @Override
    public boolean admiteBloqueo() {
        // Ya está bloqueada. Repetir la operación no cambia nada y el servicio la rechaza
        return false;
    }

    @Override
    public String mensajeDeBloqueo(Habitacion hab) {
        return "La habitación " + hab.getNumeroHabitacion()
                + " no puede bloquearse desde: " + hab.getEstadoHabitacion()
                + ". Solo se bloquea desde DISPONIBLE o EN_LIMPIEZA.";
    }
}