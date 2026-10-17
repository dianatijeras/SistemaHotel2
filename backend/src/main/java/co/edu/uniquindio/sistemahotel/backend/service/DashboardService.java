package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoPago;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.PersonalLimpieza;
import co.edu.uniquindio.sistemahotel.backend.model.Recepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.repository.EstadiaRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.PagoRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.ReservaRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class DashboardService {

    private final HabitacionRepository habitaciones;
    private final ReservaRepository reservas;
    private final EstadiaRepository estadias;
    private final PagoRepository pagos;
    private final HuespedRepository huespedes;
    private final UsuarioRepository usuarios;

    /**
     * @param habitaciones repositorio de habitaciones
     * @param reservas     repositorio de reservas
     * @param estadias     repositorio de estadias
     * @param pagos        repositorio de pagos
     * @param huespedes    repositorio de huespedes
     * @param usuarios     repositorio del personal
     */
    public DashboardService(HabitacionRepository habitaciones,
                            ReservaRepository reservas,
                            EstadiaRepository estadias,
                            PagoRepository pagos,
                            HuespedRepository huespedes,
                            UsuarioRepository usuarios) {
        this.habitaciones = habitaciones;
        this.reservas = reservas;
        this.estadias = estadias;
        this.pagos = pagos;
        this.huespedes = huespedes;
        this.usuarios = usuarios;
    }

    /**
     * Calcula las metricas del panel de control y las devuelve en un mapa.
     * @return
     */
    public Map<String, Object> estadisticas() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Habitacion> inventario = habitaciones.buscarTodos();
        List<Reserva> todasLasReservas = reservas.buscarTodas();
        List<Usuario> personal = usuarios.buscarTodos();

        long habitacionesDisponibles = inventario.stream()
                .filter(h -> h.getEstadoHabitacion() == EstadoHabitacion.DISPONIBLE)
                .count();
        long habitacionesOcupadas = inventario.stream()
                .filter(h -> h.getEstadoHabitacion() == EstadoHabitacion.OCUPADA)
                .count();
        long habitacionesEnLimpieza = inventario.stream()
                .filter(h -> h.getEstadoHabitacion() == EstadoHabitacion.EN_LIMPIEZA)
                .count();

        long reservasActivas = todasLasReservas.stream()
                .filter(r -> r.getEstadoReserva() == EstadoReserva.RESERVADA
                        || r.getEstadoReserva() == EstadoReserva.CONFIRMADA
                        || r.getEstadoReserva() == EstadoReserva.CHECKED_IN)
                .count();
        long reservasEnProceso = todasLasReservas.stream()
                .filter(r -> r.getEstadoReserva() == EstadoReserva.RESERVADA
                        || r.getEstadoReserva() == EstadoReserva.CONFIRMADA)
                .count();

        long estadiasEnCurso = estadias.buscarTodas().stream()
                .filter(e -> e.getEstadoEstadia() == EstadoEstadia.EN_CURSO)
                .count();
        long pagosPendientes = pagos.buscarTodos().stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.PENDIENTE)
                .count();
        double totalPagado = pagos.buscarTodos().stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.PAGADO)
                .mapToDouble(Pago::getMonto)
                .sum();

        long checkoutsHoy = estadias.buscarTodas().stream()
                .filter(e -> e.getEstadoEstadia() == EstadoEstadia.EN_CURSO)
                .filter(e -> e.getReserva().getFechaFin().isEqual(LocalDate.now()))
                .count();

        stats.put("totalHabitaciones", inventario.size());
        stats.put("habitacionesDisponibles", habitacionesDisponibles);
        stats.put("habitacionesOcupadas", habitacionesOcupadas);
        stats.put("habitacionesEnLimpieza", habitacionesEnLimpieza);
        stats.put("totalHuespedes", huespedes.buscarTodos().size());
        stats.put("totalReservas", todasLasReservas.size());
        stats.put("reservasActivas", reservasActivas);
        stats.put("reservasEnProceso", reservasEnProceso);
        stats.put("estadiasEnCurso", estadiasEnCurso);
        stats.put("pagosPendientes", pagosPendientes);
        stats.put("totalPagado", totalPagado);
        stats.put("checkoutsHoy", checkoutsHoy);
        stats.put("totalUsuarios", personal.size());
        stats.put("totalRecepcionistas",
                personal.stream().filter(u -> u instanceof Recepcionista).count());
        stats.put("totalPersonalLimpieza",
                personal.stream().filter(u -> u instanceof PersonalLimpieza).count());

        return stats;
    }
}