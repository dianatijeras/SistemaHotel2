package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
import co.edu.uniquindio.sistemahotel.backend.service.PagoService;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
class BloqueoDeHabitacionesTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final int HABITACION = 101;
    private static final int OTRA = 102;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private PagoService pagoService;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones()
                .forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
    }


    @Nested
    @DisplayName("1 a 4. Desde DISPONIBLE y EN_LIMPIEZA el bloqueo se permite")
    class TransicionesPermitidas {

        @Test
        @DisplayName("DISPONIBLE -> MANTENIMIENTO")
        void disponibleAMantenimiento() {
            Habitacion resultado = habitacionService.marcarMantenimiento(HABITACION);

            assertThat(resultado.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("DISPONIBLE -> FUERA_DE_SERVICIO")
        void disponibleAFueraDeServicio() {
            Habitacion resultado = habitacionService.marcarFueraDeServicio(HABITACION);

            assertThat(resultado.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
        }

        @Test
        @DisplayName("EN_LIMPIEZA -> MANTENIMIENTO")
        void enLimpiezaAMantenimiento() {
            habitacionService.marcarEnLimpieza(HABITACION);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

            Habitacion resultado = habitacionService.marcarMantenimiento(HABITACION);

            assertThat(resultado.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("EN_LIMPIEZA -> FUERA_DE_SERVICIO")
        void enLimpiezaAFueraDeServicio() {
            habitacionService.marcarEnLimpieza(HABITACION);

            Habitacion resultado = habitacionService.marcarFueraDeServicio(HABITACION);

            assertThat(resultado.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
        }
    }


    @Nested
    @DisplayName("5 y 6. Una habitacion OCUPADA con estadia EN_CURSO no puede bloquearse")
    class OcupadaConEstadiaActiva {

        @Test
        @DisplayName("OCUPADA con estadia EN_CURSO -> MANTENIMIENTO se rechaza")
        void ocupadaAMantenimientoSeRechaza() {
            Estadia estadia = estadiaEnCurso(HABITACION);

            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(estadia.getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.EN_CURSO);

            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: OCUPADA")
                    .hasMessageContaining("Solo se bloquea desde DISPONIBLE o EN_LIMPIEZA");
        }

        @Test
        @DisplayName("OCUPADA con estadia EN_CURSO -> FUERA_DE_SERVICIO se rechaza")
        void ocupadaAFueraDeServicioSeRechaza() {
            estadiaEnCurso(HABITACION);

            assertThatThrownBy(() -> habitacionService.marcarFueraDeServicio(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: OCUPADA");
        }

        @Test
        @DisplayName("Una habitacion OCUPADA sin estadia activa tampoco se puede bloquear")
        void ocupadaSinEstadiaActiva() {
            habitacionService.obtenerOFallar(HABITACION)
                    .cambiarEstado(EstadoHabitacion.OCUPADA);

            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: OCUPADA");
        }
    }


    @Nested
    @DisplayName("7. El rechazo conserva el estado y permite cerrar la estadia")
    class RechazoSinResiduos {

        @Test
        @DisplayName("Tras el rechazo, la habitacion sigue OCUPADA y el check-out funciona")
        void elCheckoutSigueSiendoPosible() {
            Estadia estadia = estadiaEnCurso(HABITACION);
            String idEstadia = estadia.getIdEstadia();

            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> habitacionService.marcarFueraDeServicio(HABITACION))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(estadia.getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.EN_CURSO);

            saldar(idEstadia);
            assertThat(pagoService.tieneSaldoPendiente(idEstadia)).isFalse();

            Estadia cerrada = estadiaService.realizarCheckOut(idEstadia);

            assertThat(cerrada.getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

            habitacionService.finalizarLimpieza(HABITACION);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("Tras el rechazo no queda ninguna habitacion tocada")
        void ningunaHabitacionQuedaTocada() {
            List<Integer> antes = dataStore.getHabitaciones().stream()
                    .map(Habitacion::getNumeroHabitacion).toList();

            habitacionService.obtenerOFallar(HABITACION)
                    .cambiarEstado(EstadoHabitacion.OCUPADA);
            habitacionService.obtenerOFallar(OTRA)
                    .cambiarEstado(EstadoHabitacion.MANTENIMIENTO);

            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> habitacionService.marcarFueraDeServicio(OTRA))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(estadoDe(OTRA)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
            assertThat(dataStore.getHabitaciones())
                    .as("no se crean ni desaparecen habitaciones")
                    .extracting(Habitacion::getNumeroHabitacion)
                    .containsExactlyInAnyOrderElementsOf(antes);
        }

        @Test
        @DisplayName("Una habitacion inexistente se reporta antes que la regla de estado")
        void inexistenteSeReportaAntes() {
            // obtenerOFallar va antes de la validacion de estado, así que el mensaje sigue siendo
            // el de habitacion inexistente y no el de bloqueo: no se cambia el contrato de errores.
            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe la habitación número: 999");

            assertThatThrownBy(() -> habitacionService.marcarFueraDeServicio(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe la habitación número: 999");
        }
    }


    @Nested
    @DisplayName("8. RESERVADA, MANTENIMIENTO y FUERA_DE_SERVICIO se rechazan")
    class EstadosNoPermitidos {

        @Test
        @DisplayName("RESERVADA -> MANTENIMIENTO y -> FUERA_DE_SERVICIO se rechazan")
        void reservadaSeRechaza() {
            forzar(EstadoHabitacion.RESERVADA);

            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: RESERVADA");
            assertThatThrownBy(() -> habitacionService.marcarFueraDeServicio(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: RESERVADA");

            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.RESERVADA);
        }

        @Test
        @DisplayName("MANTENIMIENTO no se vuelve a marcar, y tampoco pasa a FUERA_DE_SERVICIO")
        void mantenimientoSeRechaza() {
            habitacionService.marcarMantenimiento(HABITACION);

            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: MANTENIMIENTO");
            assertThatThrownBy(() -> habitacionService.marcarFueraDeServicio(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: MANTENIMIENTO");

            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("FUERA_DE_SERVICIO no se vuelve a marcar, y tampoco pasa a MANTENIMIENTO")
        void fueraDeServicioSeRechaza() {
            habitacionService.marcarFueraDeServicio(HABITACION);

            assertThatThrownBy(() -> habitacionService.marcarFueraDeServicio(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: FUERA_DE_SERVICIO");
            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede bloquearse desde: FUERA_DE_SERVICIO");

            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
        }

        @Test
        @DisplayName("La lista de estados admitidos es exactamente DISPONIBLE y EN_LIMPIEZA")
        void soloDosEstadosAdmitidos() {
            // Tabla completa de la regla: de los seis estados del enum, solo dos admiten bloqueo.
            List<EstadoHabitacion> admitidos = List.of(
                    EstadoHabitacion.DISPONIBLE, EstadoHabitacion.EN_LIMPIEZA);

            for (EstadoHabitacion estado : EstadoHabitacion.values()) {
                forzar(estado);
                if (admitidos.contains(estado)) {
                    assertThatCode(() -> habitacionService.marcarMantenimiento(HABITACION))
                            .as("%s debe admitir bloqueo", estado)
                            .doesNotThrowAnyException();
                } else {
                    assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                            .as("%s no debe admitir bloqueo", estado)
                            .isInstanceOf(IllegalStateException.class);
                }
                habitacionService.obtenerOFallar(HABITACION)
                        .cambiarEstado(EstadoHabitacion.DISPONIBLE);
            }
        }
    }


    @Nested
    @DisplayName("9. Regresión de disponibilidad, reservas, check-in, check-out, limpieza y pagos")
    class RegresionDeFuncionalidades {

        @Test
        @DisplayName("Una habitacion bloqueada desaparece de la disponibilidad de reservas")
        void bloqueadaNoSeReserva() {
            List<Integer> antes = habitacionService.listarDisponibles(HOY, HOY.plusDays(3))
                    .stream().map(Habitacion::getNumeroHabitacion).toList();
            assertThat(antes).contains(HABITACION);

            habitacionService.marcarMantenimiento(HABITACION);

            assertThat(habitacionService.listarDisponibles(HOY, HOY.plusDays(3)))
                    .extracting(Habitacion::getNumeroHabitacion)
                    .doesNotContain(HABITACION);
            assertThat(habitacionService.estaHabilitadaParaReservar(EstadoHabitacion.MANTENIMIENTO))
                    .isFalse();
        }

        @Test
        @DisplayName("Una habitacion bloqueada impide crear una reserva nueva con 409")
        void bloqueadaImpideReserva() {
            habitacionService.marcarFueraDeServicio(OTRA);

            assertThatThrownBy(() -> reservaService.crearReserva(
                    dtoReserva("H-001", OTRA, HOY, HOY.plusDays(2))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no admite reservas");
        }

        @Test
        @DisplayName("Una habitacion bloqueada impide el check-in y no deja estado parcial")
        void bloqueadaImpideCheckIn() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(2)));
            habitacionService.marcarMantenimiento(HABITACION);

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MANTENIMIENTO");

            assertThat(dataStore.getEstadias()).isEmpty();
            assertThat(reserva.getEstadoReserva())
                    .isEqualTo(EstadoReserva.RESERVADA);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("Finalizar la limpieza sigue exigiendo EN_LIMPIEZA tras el bloqueo")
        void finalizarLimpiezaSigueExigiendoEnLimpieza() {
            habitacionService.marcarMantenimiento(HABITACION);

            assertThatThrownBy(() -> habitacionService.finalizarLimpieza(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está EN_LIMPIEZA");
        }

        @Test
        @DisplayName("Marcar en limpieza sigue rechazando una habitacion ya bloqueada")
        void marcarEnLimpiezaSigueRechazandoBloqueada() {
            habitacionService.marcarMantenimiento(HABITACION);

            assertThatThrownBy(() -> habitacionService.marcarEnLimpieza(HABITACION))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("La regla unica de liberacion sigue vigente: /estado no libera una bloqueada")
        void laLiberacionSigueSiendoUnica() {
            habitacionService.marcarMantenimiento(OTRA);

            assertThatThrownBy(() -> habitacionService.cambiarEstado(
                    OTRA, EstadoHabitacion.DISPONIBLE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solo se libera confirmando que terminó la limpieza");
            assertThat(estadoDe(OTRA)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("Pagos y saldo de una estadia no se ven afectados por el bloqueo")
        void pagosNoSeVenAfectados() {
            Estadia estadia = estadiaEnCurso(HABITACION);
            String idEstadia = estadia.getIdEstadia();

            PagoRequestDTO dto = new PagoRequestDTO();
            dto.setIdEstadia(idEstadia);
            dto.setMonto(100_000);
            dto.setMetodoPago(MetodoPago.EFECTIVO);
            dto.setConcepto("Abono de prueba");
            Pago pago = pagoService.registrarPago(dto);

            assertThat(pago.getIdPago()).isNotBlank();
            assertThat(pagoService.consultarSaldo(idEstadia).getSaldoPendiente()).isPositive();

            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(pagoService.tieneSaldoPendiente(idEstadia)).isTrue();
        }
    }


    private EstadoHabitacion estadoDe(int numero) {
        return habitacionRepository.buscarPorNumero(numero).orElseThrow().getEstadoHabitacion();
    }

    private void forzar(EstadoHabitacion estado) {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(estado);
    }

    private ReservaRequestDTO dtoReserva(String idHuesped, int numero, LocalDate ini, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(idHuesped);
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(ini);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return dto;
    }

    private Estadia estadiaEnCurso(int numero) {
        Reserva reserva = reservaService.crearReserva(dtoReserva("H-001", numero, HOY, HOY.plusDays(2)));
        reservaService.confirmarReserva(reserva.getIdReserva());
        return estadiaService.realizarCheckIn(reserva.getIdReserva());
    }

    /**
     * Saldar la estadia con un pago total para poder cerrar el check-out.
     * @param idEstadia
     */
    private void saldar(String idEstadia) {
        double total = estadiaService.calcularCosto(idEstadia).getTotalGeneral().doubleValue();
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setMonto(total);
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Pago total de la prueba");
        Pago pago = pagoService.registrarPago(dto);
        pagoService.aprobarPago(pago.getIdPago(), "REF-BLOQUEO");
    }
}