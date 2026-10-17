package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.estado.EstadoHabitacionBase;
import co.edu.uniquindio.sistemahotel.backend.model.estado.EstadosHabitacion;
import co.edu.uniquindio.sistemahotel.backend.repository.EstadiaRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;
    private final ReservaRepository reservaRepository;
    private final EstadiaRepository estadiaRepository;

    /**
     * @param habitacionRepository abstracción de acceso a datos del inventario de habitaciones
     * @param reservaRepository   fuente de las reservas, para detectar conflictos de fechas
     * @param estadiaRepository   fuente de las estadías, para detectar una estadía en curso
     */
    public HabitacionService(HabitacionRepository habitacionRepository,
                             ReservaRepository reservaRepository,
                             EstadiaRepository estadiaRepository) {
        this.habitacionRepository = habitacionRepository;
        this.reservaRepository = reservaRepository;
        this.estadiaRepository = estadiaRepository;
    }

    /**
     * Método para listar todas las habitaciones del inventario, sin filtrar por estado ni por fechas.
     * @return
     */
    public List<Habitacion> listarTodas() {
        return habitacionRepository.buscarTodos();
    }

    /**
     * Método para buscar una habitación por su número, devolviendo un Optional que puede estar vacío si no existe.
     * @param numero
     * @return
     */
    public Optional<Habitacion> buscarPorNumero(int numero) {
        return habitacionRepository.buscarPorNumero(numero);
    }

    /**
     * Método para listar las habitaciones que están disponibles hoy. Se considera disponible una habitación
     * @return
     */
    public List<Habitacion> listarDisponibles() {
        LocalDate hoy = LocalDate.now();
        return listarDisponibles(hoy, hoy.plusDays(1));
    }

    /**
     * Método para listar las habitaciones disponibles dentro de un rango de fechas, sin excluir ninguna reserva concreta.
     * @param inicio
     * @param fin
     * @return
     */
    public List<Habitacion> listarDisponibles(LocalDate inicio, LocalDate fin) {
        return listarDisponibles(inicio, fin, null);
    }

    /**
     * Método para listar las habitaciones disponibles dentro de un rango de fechas, excluyendo una reserva concreta (útil para modificar reservas existentes).
     * @param inicio
     * @param fin
     * @param excluirIdReserva
     * @return
     */
    public List<Habitacion> listarDisponibles(LocalDate inicio, LocalDate fin, String excluirIdReserva) {
        validarRangoDeFechas(inicio, fin);
        return habitacionRepository.buscarTodos().stream()
                .filter(h -> estaHabilitadaParaReservar(h.getEstadoHabitacion()))
                .filter(h -> !tieneReservaSolapada(h.getNumeroHabitacion(), inicio, fin, excluirIdReserva))
                .collect(Collectors.toList());
    }

    /**
     * Método que indica si una habitación está en un estado que permite reservarla.
     * @param estado
     * @return
     */
    public boolean estaHabilitadaParaReservar(EstadoHabitacion estado) {
        return EstadosHabitacion.admiteHuespedes(estado);
    }

    /**
     * Método que indica si una habitación está disponible para reservar en un rango de fechas, sin excluir ninguna reserva concreta.
     * @param habitacion
     * @param inicio
     * @param fin
     * @return
     */
    public boolean estaDisponibleEnFechas(Habitacion habitacion, LocalDate inicio, LocalDate fin) {
        return estaHabilitadaParaReservar(habitacion.getEstadoHabitacion())
                && !tieneReservaSolapada(habitacion.getNumeroHabitacion(), inicio, fin, null);
    }

    /**
     * Método que indica si una habitación tiene alguna reserva que se solape con un rango de fechas dado, excluyendo una reserva concreta (útil para modificar reservas existentes).
     * @param numeroHabitacion
     * @param inicio
     * @param fin
     * @param excluirIdReserva
     * @return
     */
    public boolean tieneReservaSolapada(int numeroHabitacion,
                                        LocalDate inicio,
                                        LocalDate fin,
                                        String excluirIdReserva) {
        return reservaRepository.buscarTodas().stream()
                .filter(r -> r.getHabitacion().getNumeroHabitacion() == numeroHabitacion)
                .filter(r -> r.getEstadoReserva() != EstadoReserva.CANCELADA
                        && r.getEstadoReserva() != EstadoReserva.NO_SHOW
                        && r.getEstadoReserva() != EstadoReserva.FINALIZADA)
                .filter(r -> excluirIdReserva == null || !r.getIdReserva().equals(excluirIdReserva))
                .anyMatch(r -> inicio.isBefore(r.getFechaFin()) && fin.isAfter(r.getFechaInicio()));
    }

    /**
     * Método para listar habitaciones por tipo, sin filtrar por estado ni por fechas.
     * @param tipo
     * @return
     */
    public List<Habitacion> listarPorTipo(TipoHabitacion tipo) {
        return habitacionRepository.buscarPorTipo(tipo);
    }

    /**
     * Método para listar habitaciones que están pendientes de limpieza, es decir, que están en estado EN_LIMPIEZA.
     * @return
     */
    public List<Habitacion> listarPendientesLimpieza() {
        return habitacionRepository.buscarPorEstado(EstadoHabitacion.EN_LIMPIEZA);
    }

    /**
     * Método para cambiar el estado de una habitación, validando que la transición sea válida según las reglas administrativas.
     * @param numeroHabitacion
     * @param nuevoEstado
     * @return
     */
    public Habitacion cambiarEstado(int numeroHabitacion, EstadoHabitacion nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado de la habitación es obligatorio.");
        }
        Habitacion hab = obtenerOFallar(numeroHabitacion);

        validarTransicionAdministrativa(hab, nuevoEstado);

        hab.cambiarEstado(nuevoEstado);
        return habitacionRepository.actualizar(hab);
    }

    /**
     * Método privado que valida si una habitación puede cambiar de estado según las reglas administrativas.
     * @param hab
     * @param destino
     */
    private void validarTransicionAdministrativa(Habitacion hab, EstadoHabitacion destino) {
        EstadoHabitacionBase actual = EstadosHabitacion.de(hab);
        if (actual.puedeIrA(destino)) {
            return;
        }
        // El motivo y el mensaje los decide el propio estado, no este servicio.
        throw new IllegalStateException(actual.mensajeDeRechazo(hab, destino));
    }

    /**
     * Método para marcar una habitación como en limpieza, validando que solo se pueda marcar desde estado OCUPADA y que no exista una estadía activa ocupándola.
     * @param numeroHabitacion
     * @return
     */
    public Habitacion marcarEnLimpieza(int numeroHabitacion) {
        Habitacion hab = obtenerOFallar(numeroHabitacion);
        EstadoHabitacionBase actual = EstadosHabitacion.de(hab);
        if (!actual.admiteMarcarEnLimpieza()) {
            throw new IllegalStateException(
                    "La habitación " + numeroHabitacion + " no puede pasar a limpieza desde: "
                            + hab.getEstadoHabitacion());
        }
        validarNoSimulaLimpiezaDeOcupada(hab, EstadoHabitacion.EN_LIMPIEZA);
        hab.cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);
        return habitacionRepository.actualizar(hab);
    }

    /**
     * Método privado que valida que no se intente simular la limpieza de una habitación ocupada con estadía activa.
     * @param hab
     * @param nuevoEstado
     */
    private void validarNoSimulaLimpiezaDeOcupada(Habitacion hab, EstadoHabitacion nuevoEstado) {
        if (nuevoEstado == EstadoHabitacion.EN_LIMPIEZA
                && hab.getEstadoHabitacion() == EstadoHabitacion.OCUPADA
                && tieneEstadiaActiva(hab.getNumeroHabitacion())) {
            throw new IllegalStateException(
                    "La habitación " + hab.getNumeroHabitacion() + " está OCUPADA con una estadía activa, "
                            + "así que no puede pasar a EN_LIMPIEZA manualmente. "
                            + "Ese cambio lo realiza el check-out.");
        }
    }

    /**
     * Método privado que indica si una habitación tiene una estadía activa (EN_CURSO) ocupándola.
     * @param numeroHabitacion
     * @return
     */
    private boolean tieneEstadiaActiva(int numeroHabitacion) {
        return estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO).stream()
                .anyMatch(e -> e.getReserva() != null
                        && e.getReserva().getHabitacion() != null
                        && e.getReserva().getHabitacion().getNumeroHabitacion() == numeroHabitacion);
    }

    /**
     * Método para marcar una habitación como disponible, validando que solo se pueda marcar desde estado EN_LIMPIEZA y que no exista una estadía activa ocupándola.
     * @param numeroHabitacion
     * @return
     */
    public Habitacion marcarDisponible(int numeroHabitacion) {
        return finalizarLimpieza(numeroHabitacion);
    }

    /**
     * Método para finalizar la limpieza de una habitación, validando que solo se pueda marcar desde estado EN_LIMPIEZA y que no exista una estadía activa ocupándola.
     * @param numeroHabitacion
     * @return
     */
    public synchronized Habitacion finalizarLimpieza(int numeroHabitacion) {
        Habitacion hab = obtenerOFallar(numeroHabitacion);

        if (!EstadosHabitacion.de(hab).admiteFinalizarLimpieza()) {
            throw new IllegalStateException(
                    "No se puede finalizar la limpieza de la habitación " + numeroHabitacion
                            + " porque no está EN_LIMPIEZA. Estado actual: "
                            + hab.getEstadoHabitacion() + ".");
        }

        boolean ocupadaPorEstadiaActiva = tieneEstadiaActiva(numeroHabitacion);
        if (ocupadaPorEstadiaActiva) {
            throw new IllegalStateException(
                    "No se puede finalizar la limpieza de la habitación " + numeroHabitacion
                            + " porque todavía tiene una estadía activa.");
        }

        hab.cambiarEstado(EstadoHabitacion.DISPONIBLE);
        return habitacionRepository.actualizar(hab);
    }

    /**
     * Método para marcar una habitación como en mantenimiento, validando que solo se pueda marcar desde estados DISPONIBLE o EN_LIMPIEZA.
     * @param numeroHabitacion
     * @return
     */
    public Habitacion marcarMantenimiento(int numeroHabitacion) {
        Habitacion hab = obtenerOFallar(numeroHabitacion);
        validarBloqueoDesde(hab);
        hab.cambiarEstado(EstadoHabitacion.MANTENIMIENTO);
        return habitacionRepository.actualizar(hab);
    }

    /**
     * Método para marcar una habitación como fuera de servicio, validando que solo se pueda marcar desde estados DISPONIBLE o EN_LIMPIEZA.
     * @param numeroHabitacion
     * @return
     */
    public Habitacion marcarFueraDeServicio(int numeroHabitacion) {
        Habitacion hab = obtenerOFallar(numeroHabitacion);
        validarBloqueoDesde(hab);
        hab.cambiarEstado(EstadoHabitacion.FUERA_DE_SERVICIO);
        return habitacionRepository.actualizar(hab);
    }

    /**
     * Método privado que valida que una habitación pueda ser bloqueada (marcada como en mantenimiento o fuera de servicio) desde su estado actual.
     * @param hab
     */
    private void validarBloqueoDesde(Habitacion hab) {
        EstadoHabitacionBase actual = EstadosHabitacion.de(hab);
        if (!actual.admiteBloqueo()) {
            throw new IllegalStateException(actual.mensajeDeBloqueo(hab));
        }
    }

    /**
     * Método privado que obtiene una habitación por su número, o lanza una excepción si no existe.
     * @param numero
     * @return
     */
    public Habitacion obtenerOFallar(int numero) {
        return buscarPorNumero(numero)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la habitación número: " + numero));
    }

    /**
     * Método privado que valida que un rango de fechas sea válido: que no sea nulo y que la fecha de inicio sea anterior a la fecha de fin.
     * @param inicio
     * @param fin
     */
    private void validarRangoDeFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Debe indicar las fechas de entrada y salida a consultar.");
        }
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada.");
        }
    }
}