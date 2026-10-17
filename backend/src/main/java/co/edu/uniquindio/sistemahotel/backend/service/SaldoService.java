package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.dto.SaldoPagoDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoPago;
import co.edu.uniquindio.sistemahotel.backend.model.Costos;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.repository.EstadiaRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.PagoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class SaldoService {

    private final EstadiaRepository estadiaRepository;
    private final PagoRepository pagoRepository;

    /**
     * @param estadiaRepository fuente de las estadías; nunca se accede ya al DataStore
     * @param pagoRepository     fuente de los pagos; nunca se accede ya al DataStore
     */
    public SaldoService(EstadiaRepository estadiaRepository, PagoRepository pagoRepository) {
        this.estadiaRepository = estadiaRepository;
        this.pagoRepository = pagoRepository;
    }

    /**
     * Calcula el saldo pendiente de una estadía, sin modificar la entidad ni publicar nada.
     * @param idEstadia
     * @return
     */
    public SaldoPagoDTO consultarSaldo(String idEstadia) {
        Estadia estadia = obtenerEstadia(idEstadia);

        synchronized (estadia) {
            // Instantánea del total desde las fuentes, sin publicar nada en la entidad.
            BigDecimal totalGeneral = Costos.aMoneda(estadia.calcularTotalFinal());

            List<Pago> pagos = pagosDe(idEstadia);
            List<BigDecimal> abonos = abonosAprobados(pagos);

            SaldoPagoDTO dto = new SaldoPagoDTO();
            dto.setIdEstadia(estadia.getIdEstadia());
            dto.setIdReserva(estadia.getReserva() == null ? null : estadia.getReserva().getIdReserva());
            dto.setNumeroHabitacion(estadia.getReserva() == null || estadia.getReserva().getHabitacion() == null
                    ? null
                    : estadia.getReserva().getHabitacion().getNumeroHabitacion());
            dto.setTotalGeneral(totalGeneral);
            dto.setTotalAprobado(Costos.sumar(abonos));
            dto.setSaldoPendiente(Costos.saldo(totalGeneral, abonos));
            dto.setCantidadPagosAprobados(abonos.size());
            dto.setCantidadPagosPendientes((int) pagos.stream()
                    .filter(p -> p.getEstadoPago() == EstadoPago.PENDIENTE)
                    .count());
            return dto;
        }
    }

    /**
     * Indica si la estadía tiene saldo pendiente, es decir, si el total general es mayor que el total aprobado.
     * @param idEstadia
     * @return
     */
    public boolean tieneSaldoPendiente(String idEstadia) {
        return consultarSaldo(idEstadia).getSaldoPendiente().compareTo(Costos.CERO) > 0;
    }

    /**
     * Calcula el importe máximo que se puede registrar ahora como nuevo pago: el saldo pendiente más lo que
     * @param idEstadia
     * @return
     */
    public BigDecimal disponibleParaRegistro(String idEstadia) {
        SaldoPagoDTO saldo = consultarSaldo(idEstadia);
        BigDecimal comprometido = Costos.sumar(pagosDe(idEstadia).stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.PENDIENTE)
                .map(p -> Costos.aMoneda(p.getMonto()))
                .collect(Collectors.toList()));
        return Costos.saldo(saldo.getTotalGeneral(),
                List.of(saldo.getTotalAprobado().add(comprometido)));
    }

    /**
     * Obtiene todos los pagos asociados a una estadía específica.
     * @param idEstadia
     * @return
     */
    public List<Pago> pagosDe(String idEstadia) {
        return pagoRepository.buscarPorEstadia(idEstadia);
    }

    /**
     * Filtra los pagos aprobados (estado PAGADO) y devuelve sus montos en una lista de BigDecimal.
     * @param pagos
     * @return
     */
    private List<BigDecimal> abonosAprobados(List<Pago> pagos) {
        return pagos.stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.PAGADO)
                .map(p -> Costos.aMoneda(p.getMonto()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una estadía por su ID, lanzando una excepción si no se encuentra.
     * @param idEstadia
     * @return
     */
    private Estadia obtenerEstadia(String idEstadia) {
        return estadiaRepository.buscarPorId(idEstadia)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la estadía con ID: " + idEstadia));
    }
}
