package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoCostoDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.EstadiaCostoDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.SaldoPagoDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.model.ConsumoEstadia;
import co.edu.uniquindio.sistemahotel.backend.model.Costos;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.model.estado.EstadosHabitacion;
import co.edu.uniquindio.sistemahotel.backend.repository.EstadiaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class EstadiaService {
    private final EstadiaRepository estadiaRepository;
    private final ReservaService reservaService;
    private final SaldoService saldoService;

    /**
     * Constructor del servicio.
     * @param estadiaRepository abstracción de acceso a datos de las estadías
     * @param reservaService servicio de reservas, del que se reutiliza la búsqueda por identificador
     * @param saldoService servicio de saldo, única fuente de la fórmula de dinero
     */
    public EstadiaService(EstadiaRepository estadiaRepository,
                          ReservaService reservaService,
                          SaldoService saldoService) {
        this.estadiaRepository = estadiaRepository;
        this.reservaService = reservaService;
        this.saldoService = saldoService;
    }

    /**
     * Método para realizar el check-in de una reserva, creando una nueva estadía y actualizando los estados correspondientes de la reserva y la habitación.
     * @param idReserva
     * @return
     */
    public synchronized Estadia realizarCheckIn(String idReserva) {
        if (idReserva == null || idReserva.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar la reserva para realizar el check-in.");
        }

        String idReservaLimpio = idReserva.trim();
        Reserva reserva = reservaService.obtenerOFallar(idReservaLimpio);

        synchronized (reserva) {
            validarEstadoDeReservaParaCheckIn(reserva);
            validarHuespedDeLaReserva(reserva);
            validarPeriodoDelCheckIn(reserva);
            Habitacion habitacion = reserva.getHabitacion();
            validarEstadoDeHabitacionParaCheckIn(habitacion);
            validarSinEstadiaActiva(idReservaLimpio, habitacion);

            // Todas las validaciones pasaron: recién ahora se registra la estadía y se cambian estados.
            String idEstadia = "EST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Estadia estadia = new Estadia(idEstadia, reserva);
            estadia.iniciar();

            reserva.setEstadoReserva(EstadoReserva.CHECKED_IN);
            reserva.setFechaEstadia(LocalDate.now());
            habitacion.cambiarEstado(EstadoHabitacion.OCUPADA);

            // Punto explícito de persistencia: el servicio decide si la estadía puede existir,
            // el repositorio decide dónde queda.
            return estadiaRepository.guardar(estadia);
        }
    }

    /**
     * Método privado que valida que la reserva esté en un estado que permita hacer check-in: reservada o confirmada.
     * @param reserva
     */
    private void validarEstadoDeReservaParaCheckIn(Reserva reserva) {
        if (reserva.getEstadoReserva() != EstadoReserva.RESERVADA
                && reserva.getEstadoReserva() != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException(
                    "No se puede hacer check-in. Estado de reserva: " + reserva.getEstadoReserva());
        }
    }

    /**
     * Método privado que valida que la reserva tenga un huésped asociado y que el huésped tenga un documento de identidad válido.
     * @param reserva
     */
    private void validarHuespedDeLaReserva(Reserva reserva) {
        Huesped huesped = reserva.getHuesped();
        if (huesped == null) {
            throw new IllegalStateException(
                    "La reserva " + reserva.getIdReserva() + " no tiene un huésped asociado.");
        }
        if (huesped.getDocumento() == null || huesped.getDocumento().trim().isEmpty()) {
            throw new IllegalStateException(
                    "El huésped de la reserva " + reserva.getIdReserva() + " no tiene documento de identidad.");
        }
    }

    /**
     * Método privado que valida que la fecha actual esté dentro del período de la reserva para permitir el check-in.
     * @param reserva
     */
    private void validarPeriodoDelCheckIn(Reserva reserva) {
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(reserva.getFechaInicio())) {
            throw new IllegalStateException(
                    "No se puede hacer check-in antes de la fecha de entrada: " + reserva.getFechaInicio());
        }
        if (!hoy.isBefore(reserva.getFechaFin())) {
            throw new IllegalStateException(
                    "No se puede hacer check-in el día de salida ni después. La estadía termina el: "
                            + reserva.getFechaFin());
        }
    }

    /**
     * Método privado que valida que la habitación esté en un estado que permita hacer check-in: disponible o en limpieza.
     * @param habitacion
     */
    private void validarEstadoDeHabitacionParaCheckIn(Habitacion habitacion) {
        EstadoHabitacionBase comportamiento = EstadosHabitacion.de(habitacion);
        if (!comportamiento.admiteOcuparse()) {
            throw new IllegalStateException(comportamiento.mensajeDeCheckIn(habitacion));
        }
    }

    /**
     * Método privado que valida que no exista otra estadía activa para la misma reserva ni para la misma habitación.
     * @param idReserva
     * @param habitacion
     */
    private void validarSinEstadiaActiva(String idReserva, Habitacion habitacion) {
        // Se consulta el estado EN_CURSO, que es un atributo de la propia estadía, y por eso la
        // consulta es del repositorio. La REGLA —que no puede haber dos estadías activas para la
        // misma reserva ni para la misma habitación— sigue siendo de este servicio.
        List<Estadia> enCurso = estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO);

        boolean yaExisteEstadiaParaReserva = enCurso.stream()
                .anyMatch(e -> e.getReserva().getIdReserva().equals(idReserva));
        if (yaExisteEstadiaParaReserva) {
            throw new IllegalStateException("La reserva " + idReserva + " ya tiene una estadía activa.");
        }

        boolean habitacionOcupadaPorOtraEstadia = enCurso.stream()
                .anyMatch(e -> e.getReserva().getHabitacion().getNumeroHabitacion() == habitacion.getNumeroHabitacion()
                        && !e.getReserva().getIdReserva().equals(idReserva));
        if (habitacionOcupadaPorOtraEstadia) {
            throw new IllegalStateException(
                    "La habitación " + habitacion.getNumeroHabitacion() + " ya tiene una estadía activa.");
        }
    }

    /**
     * Método para realizar el check-out de una estadía, finalizando la estadía y actualizando los estados correspondientes de la reserva y la habitación.
     * @param idEstadia
     * @return
     */
    public synchronized Estadia realizarCheckOut(String idEstadia) {
        Estadia estadia = obtenerOFallar(idEstadia);

         synchronized (estadia) {
            validarEstadiaActivaParaCheckOut(estadia);

            Reserva reserva = estadia.getReserva();
            Habitacion habitacion = reserva.getHabitacion();

             SaldoPagoDTO saldo = saldoService.consultarSaldo(estadia.getIdEstadia());
            if (saldo.getSaldoPendiente().compareTo(Costos.CERO) > 0) {
                throw new IllegalStateException(
                        "No se puede hacer check-out de la estadía " + estadia.getIdEstadia()
                                + ": queda un saldo pendiente de " + saldo.getSaldoPendiente()
                                + ". Total de la cuenta: " + saldo.getTotalGeneral()
                                + ", pagos aprobados: " + saldo.getTotalAprobado() + ".");
            }

            validarHabitacionParaCheckOut(estadia, habitacion);

            estadia.finalizar();
            reserva.setEstadoReserva(EstadoReserva.FINALIZADA);

            habitacion.cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

             return estadiaRepository.actualizar(estadia);
        }
    }

    /**
     * Método privado que valida que la estadía esté en un estado que permita hacer check-out: debe estar en curso y no haber sido finalizada previamente.
     * @param estadia
     */
    private void validarEstadiaActivaParaCheckOut(Estadia estadia) {
        if (estadia.getEstadoEstadia() == EstadoEstadia.FINALIZADA) {
            throw new IllegalStateException(
                    "La estadía " + estadia.getIdEstadia() + " ya fue finalizada. No se puede repetir el check-out.");
        }
        if (estadia.getEstadoEstadia() != EstadoEstadia.EN_CURSO) {
            throw new IllegalStateException(
                    "No se puede hacer check-out. Estado de la estadía: " + estadia.getEstadoEstadia());
        }
        if (estadia.getReserva() == null) {
            throw new IllegalStateException(
                    "La estadía " + estadia.getIdEstadia() + " no tiene una reserva asociada.");
        }
        if (estadia.getReserva().getHabitacion() == null) {
            throw new IllegalStateException(
                    "La reserva " + estadia.getReserva().getIdReserva() + " no tiene una habitación asociada.");
        }
    }

    /**
     * Método privado que valida que la habitación asociada a la estadía esté en estado OCUPADA antes de permitir el check-out.
     * @param estadia
     * @param habitacion
     */
    private void validarHabitacionParaCheckOut(Estadia estadia, Habitacion habitacion) {
        if (habitacion.getEstadoHabitacion() != EstadoHabitacion.OCUPADA) {
            throw new IllegalStateException(
                    "No se puede hacer check-out: la habitación " + habitacion.getNumeroHabitacion()
                            + " de la estadía " + estadia.getIdEstadia() + " no está OCUPADA. Estado: "
                            + habitacion.getEstadoHabitacion() + ".");
        }
    }

    /**
     * Método para agregar un consumo a una estadía, validando los datos del consumo y asegurando que la estadía esté en curso antes de agregarlo.
     * @param dto
     * @return
     */
    public synchronized Estadia agregarConsumo(ConsumoRequestDTO dto) {
        validarConsumo(dto);
        Estadia estadia = obtenerOFallar(dto.getIdEstadia());

        if (estadia.getEstadoEstadia() != EstadoEstadia.EN_CURSO) {
            throw new IllegalStateException(
                    "Solo se agregan consumos a estadías EN_CURSO. Estado: " + estadia.getEstadoEstadia());
        }

        String idConsumo = "CON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ConsumoEstadia consumo = new ConsumoEstadia(
                idConsumo, dto.getDescripcion().trim(), LocalDate.now(),
                dto.getCantidad(), dto.getPrecioUnitario());

        estadia.agregarConsumo(consumo);
        return estadiaRepository.actualizar(estadia);
    }

    /**
     * Método privado que valida los datos de un consumo antes de agregarlo a una estadía. Se asegura de que el DTO no sea nulo, que la estadía esté indicada, que la descripción no esté vacía, que la cantidad sea mayor a cero y que el precio unitario sea válido y tenga la precisión adecuada.
     * @param dto
     */
    private void validarConsumo(ConsumoRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos del consumo son obligatorios.");
        }
        if (dto.getIdEstadia() == null || dto.getIdEstadia().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar la estadía para registrar el consumo.");
        }
        if (dto.getDescripcion() == null || dto.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del consumo es obligatoria.");
        }
        if (dto.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad del consumo debe ser mayor a 0.");
        }
        if (!Costos.esPrecioValido(dto.getPrecioUnitario())) {
            throw new IllegalArgumentException("El precio unitario del consumo debe ser mayor a 0.");
        }
        if (!Costos.tienePrecisionValida(dto.getPrecioUnitario())) {
            throw new IllegalArgumentException(
                    "El precio unitario del consumo debe tener máximo 2 decimales.");
        }
    }

    /**
     * Método para listar todas las estadías registradas en el sistema, sin aplicar ningún filtro de estado.
     * @return
     */
    public List<Estadia> listarTodas() {
        return estadiaRepository.buscarTodas();
    }

    /**
     * Método para listar las estadías que están en curso o finalizadas, lo que indica que los huéspedes ya hicieron check-in y pueden estar listos para realizar pagos.
     * @return
     */
    public List<Estadia> listarParaPagos() {
        return estadiaRepository.buscarTodas().stream()
                .filter(e -> e.getEstadoEstadia() == EstadoEstadia.EN_CURSO
                        || e.getEstadoEstadia() == EstadoEstadia.FINALIZADA)
                .collect(Collectors.toList());
    }

    /**
     * Método para listar las estadías que están actualmente en curso, es decir, aquellas que han sido iniciadas mediante check-in y aún no han sido finalizadas mediante check-out.
     * @return
     */
    public List<Estadia> listarEnCurso() {
        return estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO);
    }

    /**
     * Método para buscar una estadía por su ID, devolviendo un Optional que puede estar vacío si no se encuentra la estadía con el ID proporcionado.
     * @param idEstadia
     * @return
     */
    public Optional<Estadia> buscarPorId(String idEstadia) {
        return estadiaRepository.buscarPorId(idEstadia);
    }

    /**
     * Método para obtener una estadía por su ID, lanzando una excepción si no se encuentra la estadía con el ID proporcionado.
     * @param idEstadia
     * @return
     */
    public Estadia obtenerOFallar(String idEstadia) {
        return buscarPorId(idEstadia)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la estadía con ID: " + idEstadia));
    }

    /**
     * Método para calcular el costo total de una estadía, incluyendo el subtotal del alojamiento y los consumos asociados. Se valida que la estadía tenga un número válido de noches y un precio por noche válido antes de realizar el cálculo.
     * @param idEstadia
     * @return
     */
    public EstadiaCostoDTO calcularCosto(String idEstadia) {
        Estadia estadia = obtenerOFallar(idEstadia);

        long noches = estadia.getNoches();
        if (noches <= 0) {
            throw new IllegalStateException(
                    "No se puede calcular el costo de la estadía " + idEstadia
                            + ": el período reservado no permite calcular el número de noches ("
                            + detalleDeFechas(estadia) + ").");
        }

        if (!Costos.esPrecioValido(estadia.getPrecioNoche())) {
            throw new IllegalStateException(
                    "No se puede calcular el costo de la estadía " + idEstadia
                            + ": la habitación no tiene un precio por noche válido.");
        }

        BigDecimal precioNoche = Costos.aMoneda(estadia.getPrecioNoche());
        BigDecimal subtotalAlojamiento = Costos.subtotalAlojamiento(noches, estadia.getPrecioNoche());

        List<ConsumoCostoDTO> consumos = new ArrayList<>();
        List<BigDecimal> subtotalesConsumo = new ArrayList<>();
        for (ConsumoEstadia consumo : estadia.getConsumos()) {
            BigDecimal subtotal = Costos.subtotalConsumo(consumo.getCantidad(), consumo.getPrecioUnitario());
            subtotalesConsumo.add(subtotal);

            ConsumoCostoDTO item = new ConsumoCostoDTO();
            item.setIdConsumo(consumo.getIdConsumo());
            item.setDescripcion(consumo.getDescripcion());
            item.setCantidad(consumo.getCantidad());
            item.setPrecioUnitario(Costos.aMoneda(consumo.getPrecioUnitario()));
            item.setSubtotal(subtotal);
            item.setFecha(consumo.getFecha() == null ? null : consumo.getFecha().toString());
            consumos.add(item);
        }

        BigDecimal subtotalConsumos = Costos.sumar(subtotalesConsumo);
        BigDecimal totalGeneral = Costos.sumar(List.of(subtotalAlojamiento, subtotalConsumos));

        estadia.recalcularTotales();

        EstadiaCostoDTO dto = new EstadiaCostoDTO();
        dto.setIdEstadia(estadia.getIdEstadia());
        dto.setIdReserva(estadia.getReserva() == null ? null : estadia.getReserva().getIdReserva());
        dto.setNumeroHabitacion(estadia.getReserva() == null || estadia.getReserva().getHabitacion() == null
                ? null
                : estadia.getReserva().getHabitacion().getNumeroHabitacion());
        dto.setNoches(noches);
        dto.setPrecioNoche(precioNoche);
        dto.setSubtotalAlojamiento(subtotalAlojamiento);
        dto.setConsumos(consumos);
        dto.setSubtotalConsumos(subtotalConsumos);
        dto.setTotalGeneral(totalGeneral);
        return dto;
    }

    /**
     * Método privado que devuelve un detalle de las fechas de entrada y salida de una estadía, o un mensaje indicando que la estadía no tiene una reserva asociada si es el caso.
     * @param estadia
     * @return
     */
    private String detalleDeFechas(Estadia estadia) {
        if (estadia.getReserva() == null) {
            return "la estadía no tiene reserva asociada";
        }
        return "entrada " + estadia.getReserva().getFechaInicio()
                + ", salida " + estadia.getReserva().getFechaFin();
    }
}

