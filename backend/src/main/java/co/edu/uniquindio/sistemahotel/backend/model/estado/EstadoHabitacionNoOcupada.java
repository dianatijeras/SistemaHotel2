package co.edu.uniquindio.sistemahotel.backend.model.estado;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Clase abstracta que representa un estado de habitación no ocupada en el sistema de gestión hotelera.
 * Esta clase implementa la interfaz {@link EstadoHabitacionBase} y proporciona la lógica común para los estados
 * de habitación que no están ocupados, como DISPONIBLE, RESERVADA, EN_LIMPIEZA, MANTENIMIENTO y FUERA_DE_SERVICIO.
 * patron State. Los estados concretos que heredan de esta clase deben implementar los métodos abstractos para definir
 */
abstract class EstadoHabitacionNoOcupada implements EstadoHabitacionBase {

    /**
     * Los destinos que la vía administrativa admite desde un estado no ocupado, además del propio.
     */
    @Override
    public boolean puedeIrA(EstadoHabitacion destino) {
        return destino == estado() || destino == EstadoHabitacion.RESERVADA || destino == EstadoHabitacion.EN_LIMPIEZA || destino == EstadoHabitacion.MANTENIMIENTO || destino == EstadoHabitacion.FUERA_DE_SERVICIO;
    }

    /**
     * Mensaje de rechazo para los intentos de cambio de estado que no están permitidos desde un estado no ocupado.
     * @param hab
     * @param destino
     * @return
     */
    @Override
    public String mensajeDeRechazo(Habitacion hab, EstadoHabitacion destino) {
        if (destino == EstadoHabitacion.DISPONIBLE) {
            return mensajeDeLiberacion(hab);
        }
        return mensajeDeEntradaEnOcupada(hab);
    }

    /**
     * Mensaje de rechazo para los intentos de liberar una habitación sin pasar por la limpieza.
     * @param hab
     * @return
     */
    static String mensajeDeLiberacion(Habitacion hab) {
        return "No se puede liberar la habitación " + hab.getNumeroHabitacion() + " a DISPONIBLE desde el estado " + hab.getEstadoHabitacion() + ". La habitación solo se libera confirmando que terminó la limpieza.";
    }

    /**
     * Mensaje de rechazo para los intentos de pasar una habitación a OCUPADA sin pasar por el check-in.
     * @param hab
     * @return
     */
    static String mensajeDeEntradaEnOcupada(Habitacion hab) {
        return "No se puede pasar la habitación " + hab.getNumeroHabitacion() + " a OCUPADA por esta vía: el estado OCUPADA solo lo establece el check-in, " + "que además crea la estadía y registra al huésped.";
    }

    @Override
    public abstract boolean admiteHuespedes();

    @Override
    public abstract boolean admiteOcuparse();

    @Override
    public abstract boolean admiteMarcarEnLimpieza();

    @Override
    public abstract boolean admiteFinalizarLimpieza();

    @Override
    public abstract boolean admiteBloqueo();

    @Override
    public abstract String mensajeDeCheckIn(Habitacion hab);

    @Override
    public String mensajeDeBloqueo(Habitacion hab) {
        return null;
    }
}