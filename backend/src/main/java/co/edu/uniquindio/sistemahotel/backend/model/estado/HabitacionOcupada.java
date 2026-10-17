package co.edu.uniquindio.sistemahotel.backend.model.estado;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Estado de una habitación que está ocupada por un huésped. Se sale de este estado únicamente
 * mediante el check-out, que liquida la cuenta y cierra la estadía.
 */
final class HabitacionOcupada implements EstadoHabitacionBase {

    @Override
    public EstadoHabitacion estado() {
        return EstadoHabitacion.OCUPADA;
    }

    /**
     * A diferencia de los estados no ocupados, de aquí no se sale hacia ningún otro estado: la
     * habitación queda como está, o la deja el check-out.
     */
    @Override
    public boolean puedeIrA(EstadoHabitacion destino) {
        return destino == EstadoHabitacion.OCUPADA;
    }

    /**
     * Los dos motivos de rechazo posibles. Liberar a {@code DISPONIBLE} conserva su mensaje
     * propio, porque así lo responden desde antes de este patrón y hay pruebas que lo comprueban;
     * cualquier otro destino usa el motivo del check-out.
     */
    @Override
    public String mensajeDeRechazo(Habitacion hab, EstadoHabitacion destino) {
        if (destino == EstadoHabitacion.DISPONIBLE) {
            return EstadoHabitacionNoOcupada.mensajeDeLiberacion(hab);
        }
        return "No se puede cambiar la habitación " + hab.getNumeroHabitacion() + " desde OCUPADA a " + destino + ": una habitación ocupada solo deja ese estado mediante el check-out, " + "que liquida la cuenta y cierra la estadía.";
    }

    /**
     * Indica que la habitación admite huéspedes, porque está ocupada por uno.
     * @return
     */
    @Override
    public boolean admiteHuespedes() {
        return true;
    }

    @Override
    public boolean admiteOcuparse() {
        // Ya hay alguien dentro.
        return false;
    }

    @Override
    public String mensajeDeCheckIn(Habitacion hab) {
        return "No se puede hacer check-in porque la habitación " + hab.getNumeroHabitacion()
                + " ya está OCUPADA.";
    }

    @Override
    public boolean admiteMarcarEnLimpieza() {
        // Tras un check-out ya cerrado la habitación sigue OCUPADA y hay que pasarla a limpieza.
        // El servicio añade su propia comprobación de que no quede ninguna estadía activa.
        return true;
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