package co.edu.uniquindio.sistemahotel.backend.model.estado;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Interfaz que define el comportamiento de los estados de una habitación en el sistema de gestión hotelera.
 * Cada estado debe implementar esta interfaz para definir sus transiciones y reglas de negocio.
 * Pattern State: cada estado es un objeto que implementa esta interfaz, y la habitación delega en él las operaciones relacionadas con el estado.
 */
public interface EstadoHabitacionBase {


    EstadoHabitacion estado();

    /**
     * Indica si la habitación puede pasar del estado actual al estado destino. Se llama antes de intentar cambiar el estado.
     * @param destino
     * @return
     */
    boolean puedeIrA(EstadoHabitacion destino);

    /**
     * Indica el mensaje que explica por qué la habitación no puede pasar del estado actual al estado destino.
     * Se llama si puedeIrA(destino) devuelve false.
     * @param hab
     * @param destino
     * @return
     */
    String mensajeDeRechazo(Habitacion hab, EstadoHabitacion destino);

    /**
     * Indica si la habitación puede recibir huéspedes en este estado. Se llama antes de intentar hacer check-in.
     * @return
     */
    boolean admiteHuespedes();

    /**
     * Indica si la habitación puede ocuparse en este estado. Se llama antes de intentar hacer check-in.
     * @return
     */
    boolean admiteOcuparse();

    /**
     * Indica el mensaje que explica por qué la habitación no puede recibir huéspedes en este estado.
     * @param hab
     * @return
     */
    String mensajeDeCheckIn(Habitacion hab);

    /**
     * Indica si la habitación puede marcarse como en limpieza en este estado.
     * Se llama antes de intentar marcarla como en limpieza.
     * @return
     */
    boolean admiteMarcarEnLimpieza();

    /**
     * Indica si la habitación puede marcarse como disponible en este estado.
     * Se llama antes de intentar marcarla como disponible.
     */
    boolean admiteFinalizarLimpieza();

    /**
     * Indica el mensaje que explica por qué la habitación no puede marcarse como en limpieza en este estado.
     * @return
     */
    boolean admiteBloqueo();

    /**
     * Indica el mensaje que explica por qué la habitación no puede marcarse como bloqueada en este estado.
     * @param hab
     * @return
     */
    String mensajeDeBloqueo(Habitacion hab);
}