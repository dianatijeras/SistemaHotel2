package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.SaldoPagoDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoPago;
import co.edu.uniquindio.sistemahotel.backend.model.Costos;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.repository.PagoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PagoService {

    /** Longitud máxima del concepto de un pago, para no ensuciar el historial. */
    private static final int CONCEPTO_MAXIMO = 200;

    private final PagoRepository pagoRepository;
    private final EstadiaService estadiaService;
    private final SaldoService saldoService;

    /**
     * Constructor del servicio de pagos.
     * @param pagoRepository abstracción de acceso a datos de los pagos
     * @param estadiaService servicio de estadías, del que se reutiliza la búsqueda y el desglose de costo
     * @param saldoService servicio de saldo, única fuente de la fórmula de dinero
     */
    public PagoService(PagoRepository pagoRepository,
                       EstadiaService estadiaService,
                       SaldoService saldoService) {
        this.pagoRepository = pagoRepository;
        this.estadiaService = estadiaService;
        this.saldoService = saldoService;
    }

    /**
     * Registra un nuevo pago en el sistema. La operación es atómica: si falla alguna validación, no se guarda nada.
     * @param dto
     * @return
     */
    public synchronized Pago registrarPago(PagoRequestDTO dto) {
        validarSolicitud(dto);
        Estadia estadia = estadiaService.obtenerOFallar(dto.getIdEstadia().trim());

        synchronized (monitorDe(estadia, dto)) {
            validarEstadoDeEstadia(estadia);
            BigDecimal totalGeneral = totalGeneralDe(estadia);

            // No se admite un pago mayor que lo que queda por pagar, contando también lo que ya está
            // pendiente de aprobación. Si no, se podrían registrar tres pagos pendientes que en total
            // superan la cuenta y luego solo se podrían aprobar algunos de forma incoherente.
            BigDecimal disponible = disponibleParaRegistro(estadia);
            BigDecimal monto = Costos.aMoneda(dto.getMonto());
            if (monto.compareTo(disponible) > 0) {
                throw new IllegalStateException(
                        "El monto del pago (" + monto + ") supera el saldo disponible ("
                                + disponible + ") de la estadía " + estadia.getIdEstadia() + ".");
            }

            Pago pago = new Pago(generarIdPago(), dto.getMonto(), dto.getMetodoPago(),
                    dto.getConcepto().trim(), estadia);
            return pagoRepository.guardar(pago);
        }
    }

    /**
     * Aprueba un pago pendiente, cambiando su estado a PAGADO. Solo se pueden aprobar pagos que estén en estado PENDIENTE.
     * @param idPago
     * @param referencia
     * @return
     */
    public synchronized Pago aprobarPago(String idPago, String referencia) {
        Pago pago = obtenerOFallar(idPago);

        synchronized (monitorDe(pago.getEstadia(), pago)) {
            validarTransicion(pago, EstadoPago.PAGADO);

            BigDecimal saldo = consultarSaldo(pago.getEstadia().getIdEstadia()).getSaldoPendiente();
            BigDecimal monto = Costos.aMoneda(pago.getMonto());
            if (monto.compareTo(saldo) > 0) {
                throw new IllegalStateException(
                        "No se puede aprobar el pago " + pago.getIdPago() + " por " + monto
                                + ": el saldo pendiente de la estadía es solo " + saldo + ".");
            }

            pago.aprobar(referencia == null || referencia.isBlank() ? "REF-MANUAL" : referencia.trim());
            // Punto explícito de persistencia de la transición. El importe no cambia: nunca se toca.
            return pagoRepository.actualizar(pago);
        }
    }

    /**
     * Rechaza un pago pendiente, cambiando su estado a RECHAZADO. Solo se pueden rechazar pagos que estén en estado PENDIENTE.
     * @param idPago
     * @param observaciones
     * @return
     */
    public synchronized Pago rechazarPago(String idPago, String observaciones) {
        Pago pago = obtenerOFallar(idPago);

        synchronized (monitorDe(pago.getEstadia(), pago)) {
            validarTransicion(pago, EstadoPago.RECHAZADO);

            pago.rechazar(observaciones == null ? "" : observaciones.trim());
            return pagoRepository.actualizar(pago);
        }
    }

    /**
     * Consulta el saldo pendiente de una estadía, devolviendo un DTO con el total general, el total pagado y el saldo pendiente.
     * @param idEstadia
     * @return
     */
    public synchronized SaldoPagoDTO consultarSaldo(String idEstadia) {
        return saldoService.consultarSaldo(idEstadia);
    }

    /**
     * Indica si una estadía tiene saldo pendiente, es decir, si el saldo pendiente es mayor que cero.
     * @param idEstadia
     * @return
     */
    public synchronized boolean tieneSaldoPendiente(String idEstadia) {
        return saldoService.tieneSaldoPendiente(idEstadia);
    }

    /**
     * Reembolsa un pago, cambiando su estado a REEMBOLSADO. Solo se pueden reembolsar pagos que estén en estado PAGADO y cuya estadía no esté finalizada.
     * @param idPago
     * @return
     */
    public synchronized Pago reembolsarPago(String idPago) {
        Pago pago = obtenerOFallar(idPago);
        synchronized (monitorDe(pago)) {
            validarEstadiaAbiertaParaReembolso(pago.getEstadia());
            pago.reembolsar();
            return pagoRepository.actualizar(pago);
        }
    }

    /**
     * Valida que la estadía asociada a un pago no esté finalizada antes de permitir un reembolso. Si la estadía está finalizada, lanza una excepción IllegalStateException con un mensaje indicando que no se puede reembolsar el pago.
     * @param estadia
     */
    private void validarEstadiaAbiertaParaReembolso(Estadia estadia) {
        if (estadia != null && estadia.getEstadoEstadia() == EstadoEstadia.FINALIZADA) {
            throw new IllegalStateException(
                    "No se puede reembolsar el pago de la estadía " + estadia.getIdEstadia()
                            + ": la estadía ya está FINALIZADA y su cuenta quedó cerrada con saldo cero."
                            + " Un reembolso sobre una cuenta liquidada devolvería dinero de un"
                            + " servicio ya prestado y reabriría una deuda sobre un check-out"
                            + " que el sistema dio por completado.");
        }
    }

    /**
     * Devuelve un objeto que sirve como monitor de coordinación para operaciones sobre un pago. Si el pago tiene una estadía asociada, se utiliza la estadía como monitor; de lo contrario, se utiliza el propio pago.
     * @param pago
     * @return
     */
    private Object monitorDe(Pago pago) {
        return monitorDe(pago.getEstadia(), pago);
    }

    /**
     * Devuelve un objeto que sirve como monitor de coordinación para operaciones sobre una estadía. Si la estadía no es nula, se utiliza la estadía como monitor; de lo contrario, se utiliza el objeto de respaldo proporcionado.
     * @param estadia
     * @param respaldo
     * @return
     */
    private Object monitorDe(Estadia estadia, Object respaldo) {
        return estadia != null ? estadia : respaldo;
    }

    /**
     * Lista todos los pagos registrados en el sistema.
     * @return
     */
    public List<Pago> listarTodos() {
        return pagoRepository.buscarTodos();
    }

    /**
     * Lista los pagos asociados a una estadía específica, filtrando por el ID de la estadía.
     * @param idEstadia
     * @return
     */
    public List<Pago> listarPorEstadia(String idEstadia) {
        return pagoRepository.buscarPorEstadia(idEstadia);
    }

    /**
     * Busca un pago por su ID, devolviendo un Optional que puede contener el pago encontrado o estar vacío si no se encuentra ningún pago con ese ID.
     * @param idPago
     * @return
     */
    public Optional<Pago> buscarPorId(String idPago) {
        return pagoRepository.buscarPorId(idPago);
    }

    /**
     * Obtiene un pago por su ID o lanza una excepción si no se encuentra. Si el pago con el ID proporcionado no existe, se lanza una IllegalArgumentException con un mensaje indicando que no existe el pago con ese ID.
     * @param idPago
     * @return
     */
    private Pago obtenerOFallar(String idPago) {
        return buscarPorId(idPago)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el pago con ID: " + idPago));
    }

    /**
     * Valida los datos de la solicitud de pago, asegurándose de que todos los campos requeridos estén presentes y sean válidos. Si algún campo no cumple con las condiciones, se lanza una IllegalArgumentException con un mensaje descriptivo del error.
     * @param dto
     */
    private void validarSolicitud(PagoRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos del pago son obligatorios.");
        }
        if (dto.getIdEstadia() == null || dto.getIdEstadia().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar la estadía a la que corresponde el pago.");
        }
        if (!Costos.esPrecioValido(dto.getMonto())) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a 0.");
        }
        if (!Costos.tienePrecisionValida(dto.getMonto())) {
            throw new IllegalArgumentException("El monto del pago debe tener máximo 2 decimales.");
        }
        if (dto.getMetodoPago() == null) {
            throw new IllegalArgumentException("Debe indicar el método de pago.");
        }
        if (dto.getConcepto() == null || dto.getConcepto().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el concepto del pago.");
        }
        if (dto.getConcepto().trim().length() > CONCEPTO_MAXIMO) {
            throw new IllegalArgumentException(
                    "El concepto del pago puede tener máximo " + CONCEPTO_MAXIMO + " caracteres.");
        }
    }

    /**
     * Valida que la estadía esté en un estado que permita registrar pagos. Solo se permiten pagos para estadías que estén EN_CURSO o FINALIZADAS. Si la estadía está en otro estado, se lanza una IllegalStateException con un mensaje indicando el estado actual de la estadía.
     * @param estadia
     */
    private void validarEstadoDeEstadia(Estadia estadia) {
        if (estadia.getEstadoEstadia() != EstadoEstadia.EN_CURSO
                && estadia.getEstadoEstadia() != EstadoEstadia.FINALIZADA) {
            throw new IllegalStateException(
                    "Solo se registran pagos para estadías EN_CURSO o FINALIZADAS. Estado actual: "
                            + estadia.getEstadoEstadia() + ".");
        }
    }

    /**
     * Valida que la transición de estado de un pago sea válida. Solo se permiten transiciones de PENDIENTE a PAGADO o RECHAZADO. Si el pago no está en estado PENDIENTE, se lanza una IllegalStateException con un mensaje indicando que solo se pueden aprobar o rechazar pagos PENDIENTES.
     * @param pago
     * @param destino
     */
    private void validarTransicion(Pago pago, EstadoPago destino) {
        if (pago.getEstadoPago() != EstadoPago.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se pueden " + (destino == EstadoPago.PAGADO ? "aprobar" : "rechazar")
                            + " pagos PENDIENTES. El pago " + pago.getIdPago()
                            + " está en estado " + pago.getEstadoPago() + ".");
        }
    }

    /**
     * Calcula el total general de una estadía utilizando el servicio de estadías. Devuelve el total general del costo de la estadía correspondiente al ID proporcionado.
     * @param estadia
     * @return
     */
    private BigDecimal totalGeneralDe(Estadia estadia) {
        return estadiaService.calcularCosto(estadia.getIdEstadia()).getTotalGeneral();
    }

    /**
     * Calcula el saldo disponible para registrar un nuevo pago en una estadía, utilizando el servicio de saldo. Devuelve el monto disponible para registro de pagos en la estadía correspondiente al ID proporcionado.
     * @param estadia
     * @return
     */
    private BigDecimal disponibleParaRegistro(Estadia estadia) {
        return saldoService.disponibleParaRegistro(estadia.getIdEstadia());
    }

    /**
     * Genera un ID único para un nuevo pago, asegurándose de que no exista ya en el repositorio. El ID tiene el formato "PAG-" seguido de 8 caracteres alfanuméricos aleatorios. Si el ID generado ya existe, se genera uno nuevo hasta encontrar uno único.
     * @return
     */
    private String generarIdPago() {
        String idPago;
        do {
            idPago = "PAG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (buscarPorId(idPago).isPresent());
        return idPago;
    }
}

