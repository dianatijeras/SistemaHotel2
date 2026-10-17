package co.edu.uniquindio.sistemahotel.backend.model.estado;


import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Clase que representa el estado de una habitación que está en limpieza.
 * Extiende de {@link EstadoHabitacionNoOcupada}.
 */
final class HabitacionEnLimpieza extends EstadoHabitacionNoOcupada {

    @Override
    public EstadoHabitacion estado() {
        return EstadoHabitacion.EN_LIMPIEZA;
    }

    @Override
    public boolean admiteHuespedes() {
        // Ni una reserva nueva ni un check-in: todavía no se sabe si quedó en condiciones.
        return false;
    }

    @Override
    public boolean admiteOcuparse() {
        return false;
    }

    @Override
    public String mensajeDeCheckIn(Habitacion hab) {
        return "No se puede hacer check-in porque la habitación " + hab.getNumeroHabitacion()
                + " está EN_LIMPIEZA y debe pasar a DISPONIBLE antes de recibir huéspedes.";
    }

    @Override
    public boolean admiteMarcarEnLimpieza() {
        // Ya está en limpieza: repetir la operación no aporta nada.
        return false;
    }

    @Override
    public boolean admiteFinalizarLimpieza() {
        return true;
    }

    @Override
    public boolean admiteBloqueo() {
        return true;
    }
}