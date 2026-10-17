package co.edu.uniquindio.sistemahotel.backend.model.estado;


import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

/**
 * Clase de utilidad que permite obtener el comportamiento asociado a un estado de habitación.
 * Se implementa como una clase de solo constantes, para evitar la creación de instancias
 * de los objetos de comportamiento cada vez que se necesita uno.
 */

public final class EstadosHabitacion {

    private static final EstadoHabitacionBase DISPONIBLE = new HabitacionDisponible();
    private static final EstadoHabitacionBase RESERVADA = new HabitacionReservada();
    private static final EstadoHabitacionBase OCUPADA = new HabitacionOcupada();
    private static final EstadoHabitacionBase EN_LIMPIEZA = new HabitacionEnLimpieza();
    private static final EstadoHabitacionBase MANTENIMIENTO = new HabitacionMantenimiento();
    private static final EstadoHabitacionBase FUERA_DE_SERVICIO = new HabitacionFueraDeServicio();

    private EstadosHabitacion() {
        // Clase de solo constantes: no se instancia.
    }

    /**
     * Devuelve el comportamiento asociado a un estado del enum.
     * @param estado estado de la habitación; no nulo
     * @return el objeto de comportamiento de ese estado
     */
    public static EstadoHabitacionBase de(EstadoHabitacion estado) {
        switch (estado) {
            case DISPONIBLE:
                return DISPONIBLE;
            case RESERVADA:
                return RESERVADA;
            case OCUPADA:
                return OCUPADA;
            case EN_LIMPIEZA:
                return EN_LIMPIEZA;
            case MANTENIMIENTO:
                return MANTENIMIENTO;
            case FUERA_DE_SERVICIO:
                return FUERA_DE_SERVICIO;
            default:
                throw new IllegalArgumentException("Estado de habitación no soportado: " + estado);
        }
    }

    /**
     * Atajo para la pregunta más frecuente: ¿la habitación en ese estado admite huéspedes?
     * @param estado estado de la habitación
     * @return true si el estado admite huéspedes
     */
    public static boolean admiteHuespedes(EstadoHabitacion estado) {
        return de(estado).admiteHuespedes();
    }

    /**
     * Objeto de comportamiento de la habitación recibida.
     * @param hab habitación no nula
     * @return el comportamiento de su estado actual
     */
    public static EstadoHabitacionBase de(Habitacion hab) {
        return de(hab.getEstadoHabitacion());
    }
}