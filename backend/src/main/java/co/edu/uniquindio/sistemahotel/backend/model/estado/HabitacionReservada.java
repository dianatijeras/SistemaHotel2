package co.edu.uniquindio.sistemahotel.backend.model.estado;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Estado de habitación reservada. Se trata de un estado no ocupado, pero que admite check-in.
 */
final class HabitacionReservada extends EstadoHabitacionNoOcupada {

    @Override
    public EstadoHabitacion estado() {
        return EstadoHabitacion.RESERVADA;
    }

    @Override
    public boolean admiteHuespedes() {
        return true;
    }

    @Override
    public boolean admiteOcuparse() {
        // Una reserva viva es exactamente el caso en que el check-in procede.
        return true;
    }

    @Override
    public String mensajeDeCheckIn(Habitacion hab) {
        return null;
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
        // A diferencia de DISPONIBLE y EN_LIMPIEZA, este estado no admite bloqueo por el endpoint
        // específico: la reserva se volvería inejecutable sin que existiera salida para ella.
        return false;
    }

    @Override
    public String mensajeDeBloqueo(Habitacion hab) {
        return "La habitación " + hab.getNumeroHabitacion() + " no puede bloquearse desde: " + hab.getEstadoHabitacion() + ". Solo se bloquea desde DISPONIBLE o EN_LIMPIEZA.";
    }
}