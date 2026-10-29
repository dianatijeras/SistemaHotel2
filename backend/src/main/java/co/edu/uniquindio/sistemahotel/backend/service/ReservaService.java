package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.dto.ReservaActualizacionDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.notification.ReservaConfirmadaEvent;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.ReservaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de reservas que encapsula la lógica de negocio relacionada con la creación, modificación, confirmación y cancelación de reservas.
 */
@Service
public class ReservaService implements ApplicationEventPublisherAware {

    private static final Logger LOG = LoggerFactory.getLogger(ReservaService.class);

    private ApplicationEventPublisher publicadorEventos;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publicadorEventos) {
        this.publicadorEventos = publicadorEventos;
    }

    private final ReservaRepository reservaRepository;
    private final HabitacionService habitacionService;
    private final HuespedRepository huespedRepository;

    /**
     * @param reservaRepository  abstracción de acceso a datos de las reservas
     * @param habitacionService  servicio de habitaciones, del que se reutilizan sus reglas
     * @param huespedRepository  fuente del huésped al que pertenece la reserva
     */
    public ReservaService(ReservaRepository reservaRepository,
                          HabitacionService habitacionService,
                          HuespedRepository huespedRepository) {
        this.reservaRepository = reservaRepository;
        this.habitacionService = habitacionService;
        this.huespedRepository = huespedRepository;
    }

    /**
     * Método para crear una nueva reserva (F-03).
     * @param dto
     * @return
     */
    public synchronized Reserva crearReserva(ReservaRequestDTO dto) {
        validarSolicitud(dto);

        Huesped huesped = buscarHuespedOFallar(dto.getIdHuesped().trim());
        Habitacion habitacion = habitacionService.obtenerOFallar(dto.getNumeroHabitacion());

        validarEstadoDeHabitacion(habitacion);
        validarCantidadDePersonas(dto, habitacion);
        verificarDisponibilidadEnFechas(
                dto.getNumeroHabitacion(), dto.getFechaInicio(), dto.getFechaFin(), null);

        String codigoReserva = generarCodigoReserva();
        Reserva reserva = new Reserva(codigoReserva, dto.getFechaInicio(), dto.getFechaFin(),
                dto.getAdultos(), dto.getNinos(), huesped, habitacion);

        return reservaRepository.guardar(reserva);
    }

    /**
     * Método privado que valida los datos de la solicitud de reserva.
     * @param dto
     */
    private void validarSolicitud(ReservaRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la reserva son obligatorios.");
        }
        if (dto.getIdHuesped() == null || dto.getIdHuesped().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el huésped de la reserva.");
        }
        validarRangoDeFechasDeReserva(dto.getFechaInicio(), dto.getFechaFin());
    }

    /**
     * Método privado que valida el estado de la habitación para permitir reservas.
     * @param habitacion
     */
    private void validarEstadoDeHabitacion(Habitacion habitacion) {
        if (!habitacionService.estaHabilitadaParaReservar(habitacion.getEstadoHabitacion())) {
            throw new IllegalStateException(
                    "La habitación " + habitacion.getNumeroHabitacion()
                            + " no admite reservas porque está en estado "
                            + habitacion.getEstadoHabitacion() + ".");
        }
    }

    /**
     * Método privado que valida la cantidad de personas en la reserva contra la capacidad de la habitación.
     * @param dto
     * @param habitacion
     */
    private void validarCantidadDePersonas(ReservaRequestDTO dto, Habitacion habitacion) {
        if (dto.getAdultos() < 1) {
            throw new IllegalArgumentException("La reserva debe incluir al menos 1 adulto.");
        }
        if (dto.getNinos() < 0) {
            throw new IllegalArgumentException("La cantidad de niños no puede ser negativa.");
        }

        int totalPersonas = dto.getAdultos() + dto.getNinos();
        if (totalPersonas > habitacion.getCapacidad()) {
            throw new IllegalArgumentException(
                    "La habitación " + habitacion.getNumeroHabitacion() + " tiene una capacidad de "
                            + habitacion.getCapacidad() + " personas y se solicitaron "
                            + totalPersonas + ".");
        }
    }

    /**
     * Método para modificar una reserva existente (F-04).
     * @param idReserva
     * @param dto
     * @return
     */
    public synchronized Reserva modificarReserva(String idReserva, ReservaActualizacionDTO dto) {
        Reserva reserva = obtenerOFallar(idReserva);

        synchronized (reserva) {
            validarModificable(reserva);

            if (dto == null) {
                throw new IllegalArgumentException("Los datos de la modificación son obligatorios.");
            }

            LocalDate inicio = dto.getFechaInicio() != null ? dto.getFechaInicio() : reserva.getFechaInicio();
            LocalDate fin = dto.getFechaFin() != null ? dto.getFechaFin() : reserva.getFechaFin();
            int numeroHabitacion = dto.getNumeroHabitacion() > 0
                    ? dto.getNumeroHabitacion()
                    : reserva.getHabitacion().getNumeroHabitacion();

            validarRangoDeModificacion(inicio, fin);

            Habitacion habitacion = habitacionService.obtenerOFallar(numeroHabitacion);
            validarEstadoDeHabitacion(habitacion);
            validarCapacidadDeLaReserva(reserva, habitacion);

            verificarDisponibilidadEnFechas(numeroHabitacion, inicio, fin, idReserva);

            reserva.setFechaInicio(inicio);
            reserva.setFechaFin(fin);
            reserva.setHabitacion(habitacion);
            reserva.setCantidadPersonas(reserva.getAdultos() + reserva.getNinos());

            return reservaRepository.actualizar(reserva);
        }
    }

    /**
     * Método privado que valida si una reserva puede ser modificada según su estado.
     * @param reserva
     */
    private void validarModificable(Reserva reserva) {
        if (reserva.getEstadoReserva() != EstadoReserva.RESERVADA
                && reserva.getEstadoReserva() != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException(
                    "Solo se pueden modificar reservas en estado RESERVADA o CONFIRMADA. Estado actual: "
                            + reserva.getEstadoReserva() + ".");
        }
    }

    /**
     * Método privado que valida el rango de fechas para la modificación de una reserva.
     * @param inicio
     * @param fin
     */
    private void validarRangoDeModificacion(LocalDate inicio, LocalDate fin) {
        validarRangoDeFechasDeReserva(inicio, fin);
    }

    private void validarRangoDeFechasDeReserva(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas de entrada y salida son obligatorias.");
        }
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException(
                    "La fecha de salida debe ser posterior a la fecha de entrada.");
        }
        if (inicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrada no puede estar en el pasado.");
        }
    }

    /**
     * Método privado que valida la capacidad de la reserva contra la capacidad de la habitación.
     * @param reserva
     * @param habitacion
     */
    private void validarCapacidadDeLaReserva(Reserva reserva, Habitacion habitacion) {
        if (reserva.getCantidadPersonas() > habitacion.getCapacidad()) {
            throw new IllegalArgumentException(
                    "La habitación " + habitacion.getNumeroHabitacion() + " tiene una capacidad de "
                            + habitacion.getCapacidad() + " personas y la reserva es para "
                            + reserva.getCantidadPersonas() + ".");
        }
    }

    /**
     * Método para confirmar una reserva (F-05).
     * @param idReserva
     * @return
     */
    public Reserva confirmarReserva(String idReserva) {
        Reserva reserva = obtenerOFallar(idReserva);
        ReservaConfirmadaEvent evento;
        synchronized (reserva) {
            if (reserva.getEstadoReserva() != EstadoReserva.RESERVADA) {
                throw new IllegalStateException(
                        "Solo se confirman reservas en estado RESERVADA. Estado actual: "
                                + reserva.getEstadoReserva());
            }
            reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);

            var huesped = reserva.getHuesped();
            evento = new ReservaConfirmadaEvent(
                    huesped == null ? null : huesped.getCorreo(),
                    huesped == null ? "Huésped" : huesped.getNombreCompleto(),
                    reserva.getCodigoReserva(),
                    reserva.getHabitacion().getNumeroHabitacion(),
                    reserva.getFechaInicio(),
                    reserva.getFechaFin()
            );
        }

        if (publicadorEventos != null) {
            try {
                publicadorEventos.publishEvent(evento);
            } catch (RuntimeException ex) {
                // Un error al encolar un mensaje no anula una confirmación exitosa.
                LOG.warn("No se pudo solicitar el correo para la reserva {}: {}",
                        reserva.getCodigoReserva(), ex.getMessage());
            }
        }
        return reserva;
    }

    /**
     * Método para cancelar una reserva.
     * @param idReserva
     * @return
     */
    public Reserva cancelarReserva(String idReserva) {
        Reserva reserva = obtenerOFallar(idReserva);

        synchronized (reserva) {
            if (reserva.getEstadoReserva() != EstadoReserva.RESERVADA
                    && reserva.getEstadoReserva() != EstadoReserva.CONFIRMADA) {
                throw new IllegalStateException(
                        "Solo se pueden cancelar reservas en estado RESERVADA o CONFIRMADA. Estado actual: "
                                + reserva.getEstadoReserva() + ".");
            }
            reserva.setEstadoReserva(EstadoReserva.CANCELADA);
            return reserva;
        }
    }

    /**
     * Método para marcar una reserva como NO_SHOW.
     * @param idReserva
     * @return
     */
    public Reserva marcarNoShow(String idReserva) {
        Reserva reserva = obtenerOFallar(idReserva);
        // Mismo monitor de entidad y mismo motivo que cancelarReserva.
        synchronized (reserva) {
            if (reserva.getEstadoReserva() != EstadoReserva.CONFIRMADA
                    && reserva.getEstadoReserva() != EstadoReserva.RESERVADA) {
                throw new IllegalStateException("Solo se marca NO_SHOW en reservas RESERVADA o CONFIRMADA.");
            }
            reserva.setEstadoReserva(EstadoReserva.NO_SHOW);
            // Igual que al cancelar, la habitación conserva su estado: solo se liberan las fechas.
            return reserva;
        }
    }

    /**
     * Método para listar todas las reservas registradas en el sistema.
     * @return
     */
    public List<Reserva> listarTodas() {
        return reservaRepository.buscarTodas();
    }

    /**
     * Método para listar todas las reservas de un huésped específico.
     * @param idHuesped
     * @return
     */
    public List<Reserva> listarPorHuesped(String idHuesped) {
        return reservaRepository.buscarPorHuesped(idHuesped);
    }

    public Optional<Reserva> buscarPorId(String idReserva) {
        return reservaRepository.buscarPorId(idReserva);
    }

    /**
     * Método para obtener una reserva por su ID. Si no se encuentra, lanza una excepción con un mensaje claro.
     * @param idReserva
     * @return
     */
    public Reserva obtenerOFallar(String idReserva) {
        return buscarPorId(idReserva)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la reserva con ID: " + idReserva));
    }

    /**
     * Método para buscar un huésped por su ID. Si no se encuentra, lanza una excepción con un mensaje claro.
     * @param idHuesped
     * @return
     */
    private Huesped buscarHuespedOFallar(String idHuesped) {
        return huespedRepository.buscarPorId(idHuesped)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el huésped con ID: " + idHuesped));
    }

    /**
     * Método para generar un código único para una nueva reserva. El formato es "RES-YYYY-NNNNNN", donde YYYY es el año actual y NNNNNN es un número consecutivo de 6 dígitos.
     * @return
     */
    private String generarCodigoReserva() {
        int anio = LocalDate.now().getYear();
        int consecutivo = reservaRepository.contarTodas() + 1;
        String codigo;
        do {
            codigo = String.format("RES-%d-%06d", anio, consecutivo++);
        } while (reservaRepository.existePorCodigoOId(codigo));
        return codigo;
    }

    /**
     * Método para verificar si una habitación tiene reservas solapadas en un rango de fechas. Si existe una reserva que se solapa, lanza una excepción con un mensaje claro.
     * @param numeroHabitacion
     * @param inicio
     * @param fin
     * @param excluirIdReserva
     */
    public void verificarDisponibilidadEnFechas(int numeroHabitacion,
                                                LocalDate inicio,
                                                LocalDate fin,
                                                String excluirIdReserva) {
        if (habitacionService.tieneReservaSolapada(numeroHabitacion, inicio, fin, excluirIdReserva)) {
            throw new IllegalStateException(
                    "La habitación " + numeroHabitacion
                            + " ya tiene una reserva activa en las fechas: " + inicio + " → " + fin + ".");
        }
    }
}