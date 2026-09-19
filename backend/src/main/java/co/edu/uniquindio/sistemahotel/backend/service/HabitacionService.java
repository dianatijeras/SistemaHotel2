package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar las operaciones relacionadas con las habitaciones.
 * Implementa RF002: consultar disponibilidad de habitaciones en tiempo real.
 */
@Service
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;
    private final ReservaRepository reservaRepository;

    private static final Set<EstadoReserva> ESTADOS_QUE_OCUPAN_FECHAS = Set.of(
            EstadoReserva.RESERVADA, EstadoReserva.CONFIRMADA, EstadoReserva.CHECKED_IN
    );

    public HabitacionService(HabitacionRepository habitacionRepository, ReservaRepository reservaRepository) {
        this.habitacionRepository = habitacionRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Habitacion> listarTodas() {
        return habitacionRepository.findAll();
    }

    public Habitacion obtenerOFallar(int numeroHabitacion) {
        return habitacionRepository.findByNumero(numeroHabitacion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la habitación número: " + numeroHabitacion));
    }

    /**
     * RF002 (versión simple): habitaciones cuyo estado actual es DISPONIBLE.
     * Se mantiene para casos donde no interesa un rango de fechas específico
     * (ej: vista general de estado del hotel en el dashboard).
     */
    public List<Habitacion> listarDisponiblesAhora() {
        return habitacionRepository.findAll().stream()
                .filter(Habitacion::estaDisponible)
                .collect(Collectors.toList());
    }

    /**
     * RF002 (versión completa, la que realmente responde "¿qué habitaciones
     * están libres para estas fechas?"): una habitación se considera
     * disponible en un rango de fechas si:
     *  - no está fuera de servicio ni en mantenimiento, y
     *  - no tiene ninguna reserva activa (RESERVADA/CONFIRMADA/CHECKED_IN)
     *    cuyas fechas se solapen con el rango solicitado.
     *
     * Esto es necesario porque el estado "DISPONIBLE" de una habitación es
     * solo una foto del momento actual; para saber si está libre del 10 al
     * 15 de un mes hay que mirar sus reservas en ese rango, no su estado hoy.
     */
    public List<Habitacion> listarDisponiblesEnFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        validarRangoFechas(fechaInicio, fechaFin);

        return habitacionRepository.findAll().stream()
                .filter(h -> h.getEstadoHabitacion() != EstadoHabitacion.FUERA_DE_SERVICIO
                        && h.getEstadoHabitacion() != EstadoHabitacion.MANTENIMIENTO)
                .filter(h -> !tieneSolapamiento(h.getNumeroHabitacion(), fechaInicio, fechaFin, null))
                .collect(Collectors.toList());
    }

    /**
     * Verifica si una habitación tiene alguna reserva activa que se solape
     * con el rango [inicio, fin). Se usa tanto para consultar disponibilidad
     * como para validar el anti-duplicados al crear una reserva (RF003).
     *
     * @param excluirIdReserva permite excluir una reserva puntual de la
     *                         validación (útil si más adelante se implementa
     *                         "modificar reserva" sin chocar contra sí misma).
     */
    public boolean tieneSolapamiento(int numeroHabitacion, LocalDate inicio, LocalDate fin, String excluirIdReserva) {
        return reservaRepository.findByNumeroHabitacion(numeroHabitacion).stream()
                .filter(r -> ESTADOS_QUE_OCUPAN_FECHAS.contains(r.getEstadoReserva()))
                .filter(r -> excluirIdReserva == null || !r.getIdReserva().equals(excluirIdReserva))
                .anyMatch(r -> inicio.isBefore(r.getFechaFin()) && fin.isAfter(r.getFechaInicio()));
    }

    public void validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
        if (inicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser en el pasado.");
        }
    }
}
