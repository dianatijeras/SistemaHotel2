package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaActualizacionDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoPago;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.repository.EstadiaRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.PagoRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.ReservaRepository;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
import co.edu.uniquindio.sistemahotel.backend.service.PagoService;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import co.edu.uniquindio.sistemahotel.backend.service.SaldoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class HabitacionServiceRepositoryTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final int HABITACION = 101;
    private static final int OTRA_HABITACION = 102;
    private static final long TIMEOUT_EJECUCION_S = 20;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EstadiaRepository estadiaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private SaldoService saldoService;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHuespedes().clear();
        dataStore.getHabitaciones().forEach(h -> {
            h.cambiarEstado(EstadoHabitacion.DISPONIBLE);
            h.setPrecioNoche(150_000.0);
        });
        dataStore.getHuespedes().add(huesped("H-001", "Carlos", "Ramírez", "carlos.r"));
        dataStore.getHuespedes().add(huesped("H-002", "Laura", "Gómez", "laura.g"));
    }


    @Nested
    @DisplayName("1. La disponibilidad se consulta en el repositorio inyectado")
    class DisponibilidadDesacoplada {

        @Test
        @DisplayName("1a. Un solapamiento que solo existe en ReservaRepository se rechaza")
        void solapamientoSoloEnElRepositorioAlternativo_seRechaza() {
            RepoDeReservas alterno = new RepoDeReservas();
            HabitacionService servicio = new HabitacionService(
                    habitacionRepository, alterno, estadiaRepository);

            Reserva conflictiva = reservaSinGuardar("RES-ALT-1", HABITACION,
                    HOY.plusDays(10), HOY.plusDays(13));
            conflictiva.setEstadoReserva(EstadoReserva.CONFIRMADA);
            alterno.cargar(conflictiva);

            assertThat(dataStore.getReservas())
                    .as("precondicion real: la reserva NO esta en el DataStore")
                    .isEmpty();

            assertThat(servicio.tieneReservaSolapada(HABITACION, HOY.plusDays(11), HOY.plusDays(12), null))
                    .as("la disponibilidad ve la reserva del repositorio inyectado")
                    .isTrue();
            assertThat(servicio.listarDisponibles(HOY.plusDays(10), HOY.plusDays(13)))
                    .as("y por tanto la habitacion no se ofrece en ese rango")
                    .noneMatch(h -> h.getNumeroHabitacion() == HABITACION);
            assertThat(servicio.tieneReservaSolapada(OTRA_HABITACION, HOY.plusDays(10), HOY.plusDays(13), null))
                    .as("otra habitacion no se ve afectada")
                    .isFalse();
        }

        @Test
        @DisplayName("1b. Una estadia activa que solo existe en EstadiaRepository impide liberar")
        void estadiaActivaSoloEnElRepositorioAlternativo_bloqueaLaLiberacion() {
            RepoDeEstadias alterno = new RepoDeEstadias();
            HabitacionService servicio = new HabitacionService(
                    habitacionRepository, reservaRepository, alterno);

            Habitacion hab = habitacionRepository.buscarPorNumero(HABITACION).orElseThrow();
            hab.cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);


            Reserva reserva = reservaSinGuardar("RES-ALT-2", HABITACION, HOY, HOY.plusDays(3));
            Estadia estadia = new Estadia("EST-ALT-1", reserva);
            estadia.iniciar();
            alterno.cargar(estadia);

            assertThat(dataStore.getEstadias()).as("precondicion real: no esta en el DataStore").isEmpty();

            assertThatThrownBy(() -> servicio.finalizarLimpieza(HABITACION))
                    .as("no se libera una habitacion con estadia activa, este donde viva esta")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("todavía tiene una estadía activa");
            assertThat(hab.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);


            alterno.retirar(estadia);
            assertThat(servicio.finalizarLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("1c. El servicio NO consulta ninguna coleccion oculta")
        void elServicioNoLeeColeccionesOcultas() {

            RepoDeReservas vacio = new RepoDeReservas();
            HabitacionService servicio = new HabitacionService(
                    habitacionRepository, vacio, estadiaRepository);

            reservaService.crearReserva(dtoReserva("H-001", HABITACION, HOY.plusDays(5), HOY.plusDays(8)));
            assertThat(reservaRepository.buscarTodas()).hasSize(1);

            assertThat(servicio.tieneReservaSolapada(HABITACION, HOY.plusDays(6), HOY.plusDays(7), null))
                    .as("el repositorio inyectado esta vacio: no hay conflicto")
                    .isFalse();
            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(6), HOY.plusDays(7), null))
                    .as("el servicio del contenedor si lo ve, porque consulta el repositorio real")
                    .isTrue();
        }
    }



    @Nested
    @DisplayName("2-3. Reglas de disponibilidad conservadas")
    class ReglasConservadas {

        @Test
        @DisplayName("2. Detecta solapamientos por cualquier combinacion de fechas")
        void solapamientosPorTodosLosExtremos() {
            reservaService.crearReserva(dtoReserva("H-001", HABITACION, HOY.plusDays(10), HOY.plusDays(20)));

            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(12), HOY.plusDays(14), null))
                    .as("contenida dentro").isTrue();
            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(5), HOY.plusDays(12), null))
                    .as("empieza antes y termina dentro").isTrue();
            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(18), HOY.plusDays(25), null))
                    .as("empieza dentro y termina despues").isTrue();
            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(1), HOY.plusDays(30), null))
                    .as("contiene la reserva base").isTrue();
            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(20), HOY.plusDays(25), null))
                    .as("la fecha de salida es limite exclusivo: no hay solapamiento")
                    .isFalse();
        }

        @Test
        @DisplayName("3. Reservas consecutivas sin solapamiento se permiten")
        void reservasConsecutivasSePermiten() {
            Reserva primera = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY.plusDays(1), HOY.plusDays(4)));
            Reserva segunda = reservaService.crearReserva(
                    dtoReserva("H-002", HABITACION, HOY.plusDays(4), HOY.plusDays(7)));

            assertThat(primera).isNotNull();
            assertThat(segunda).as("la habitacion se reutiliza en cuanto termina la anterior").isNotNull();
            assertThat(reservasDeLaHabitacion(HABITACION)).hasSize(2);

            assertThatThrownBy(() -> reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY.plusDays(5), HOY.plusDays(6))))
                    .as("una tercera que pisa a la segunda si se rechaza")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("reserva activa en las fechas");
            assertThat(reservasDeLaHabitacion(HABITACION)).hasSize(2);
        }

        @Test
        @DisplayName("3b. Una reserva cancelada libera las fechas")
        void unaReservaCanceladaLiberaLasFechas() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY.plusDays(1), HOY.plusDays(4)));
            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(1), HOY.plusDays(4), null))
                    .isTrue();

            reservaService.cancelarReserva(reserva.getIdReserva());

            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(1), HOY.plusDays(4), null))
                    .as("una reserva CANCELADA libera las fechas")
                    .isFalse();
            assertThat(reservaService.crearReserva(
                    dtoReserva("H-002", HABITACION, HOY.plusDays(1), HOY.plusDays(4))))
                    .as("y la habitacion vuelve a estar disponible en ese rango")
                    .isNotNull();
        }
    }



    @Nested
    @DisplayName("4. La edicion de una reserva se excluye a si misma")
    class ModificacionDeReserva {

        @Test
        @DisplayName("4a. Modificar una reserva ignora su propio solapamiento")
        void modificarIgnoraLaPropiaReserva() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY.plusDays(1), HOY.plusDays(4)));

            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(1), HOY.plusDays(4), null))
                    .as("sin excluirla, la habitacion aparece ocupada por si misma")
                    .isTrue();
            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(1), HOY.plusDays(4),
                    reserva.getIdReserva()))
                    .as("excluyendola, la habitacion esta libre para ampliar el rango")
                    .isFalse();

            Reserva modificada = reservaService.modificarReserva(reserva.getIdReserva(),
                    dtoModificacion(HABITACION, HOY.plusDays(1), HOY.plusDays(9)));

            assertThat(modificada).isNotNull();
            assertThat(modificada.getIdReserva()).isEqualTo(reserva.getIdReserva());
            assertThat(reservaRepository.buscarPorId(reserva.getIdReserva()).orElseThrow().getFechaFin())
                    .as("se amplia la reserva existente")
                    .isEqualTo(HOY.plusDays(9));
            assertThat(reservasDeLaHabitacion(HABITACION))
                    .as("no se crea una segunda reserva")
                    .hasSize(1);
        }

        @Test
        @DisplayName("4b. Ampliar una reserva no choca consigo misma")
        void ampliarNoChocaContigoMisma() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY.plusDays(1), HOY.plusDays(4)));


            reservaService.modificarReserva(reserva.getIdReserva(),
                    dtoModificacion(HABITACION, HOY.plusDays(1), HOY.plusDays(6)));
            Reserva segunda = reservaService.modificarReserva(reserva.getIdReserva(),
                    dtoModificacion(HABITACION, HOY.plusDays(1), HOY.plusDays(10)));

            assertThat(segunda.getFechaFin()).isEqualTo(HOY.plusDays(10));
            assertThat(reservasDeLaHabitacion(HABITACION)).hasSize(1);
        }
    }



    @Nested
    @DisplayName("5-6. Ciclo de la habitacion: check-in, check-out y limpieza")
    class CicloDeLaHabitacion {

        @Test
        @DisplayName("5. Check-in y check-out recorren OCUPADA -> EN_LIMPIEZA -> DISPONIBLE")
        void cicloCompletoDeLaHabitacion() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(3)));
            reservaService.confirmarReserva(reserva.getIdReserva());

            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);

            assertThatThrownBy(() -> habitacionService.marcarEnLimpieza(HABITACION))
                    .as("una habitacion OCUPADA con estadia activa no pasa a limpieza a mano")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("estadía activa");
            assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION, EstadoHabitacion.DISPONIBLE))
                    .as("y DISPONIBLE solo se alcanza confirmando la limpieza")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("La habitación solo se libera confirmando que terminó la limpieza");

            pagarSaldo(estadia);
            assertThat(estadiaService.realizarCheckOut(estadia.getIdEstadia()).getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

            assertThat(habitacionService.finalizarLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("6. Los estados fisicos que bloquean reservas siguen igual")
        void estadosFisicosQueBloqueanReservas() {
            for (EstadoHabitacion estado : List.of(EstadoHabitacion.MANTENIMIENTO,
                    EstadoHabitacion.FUERA_DE_SERVICIO, EstadoHabitacion.EN_LIMPIEZA)) {
                Habitacion hab = habitacionRepository.buscarPorNumero(HABITACION).orElseThrow();
                hab.cambiarEstado(estado);

                assertThat(habitacionService.estaHabilitadaParaReservar(estado))
                        .as(estado + " no habilita reservas")
                        .isFalse();
                assertThat(habitacionService.listarDisponibles(HOY, HOY.plusDays(5)))
                        .as("y la habitacion no se ofrece con estado " + estado)
                        .noneMatch(h -> h.getNumeroHabitacion() == HABITACION);
                assertThatThrownBy(() -> reservaService.crearReserva(
                        dtoReserva("H-001", HABITACION, HOY.plusDays(1), HOY.plusDays(3))))
                        .isInstanceOf(IllegalStateException.class);
            }

            for (EstadoHabitacion estado : List.of(EstadoHabitacion.DISPONIBLE,
                    EstadoHabitacion.RESERVADA, EstadoHabitacion.OCUPADA)) {
                assertThat(habitacionService.estaHabilitadaParaReservar(estado))
                        .as(estado + " si habilita reservas")
                        .isTrue();
            }
        }

        @Test
        @DisplayName("6b. Finalizar la limpieza dos veces se rechaza sin efectos")
        void finalizarLimpiezaNoEsIdempotente() {
            Habitacion hab = habitacionRepository.buscarPorNumero(HABITACION).orElseThrow();
            hab.cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

            assertThat(habitacionService.finalizarLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
            assertThatThrownBy(() -> habitacionService.finalizarLimpieza(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está EN_LIMPIEZA");
            assertThat(hab.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
        }
    }



    @Nested
    @DisplayName("7-8. Concurrencia")
    class Concurrencia {

        @Test
        @DisplayName("7. Dos reservas concurrentes para la misma habitacion no se solapan")
        void dosReservasConcurrentesNoSeSolapan() throws Exception {
            for (int ronda = 0; ronda < 20; ronda++) {
                limpiar();

                int peticiones = 6;
                ExecutorService executor = Executors.newFixedThreadPool(peticiones);
                CountDownLatch arranco = new CountDownLatch(1);
                AtomicInteger aceptadas = new AtomicInteger();
                AtomicInteger rechazadas = new AtomicInteger();
                List<Future<?>> tareas = new ArrayList<>();

                for (int i = 0; i < peticiones; i++) {
                    final String huesped = i % 2 == 0 ? "H-001" : "H-002";
                    tareas.add(executor.submit(() -> {
                        try {
                            arranco.await();
                            reservaService.crearReserva(
                                    dtoReserva(huesped, HABITACION, HOY.plusDays(1), HOY.plusDays(4)));
                            aceptadas.incrementAndGet();
                        } catch (RuntimeException e) {
                            rechazadas.incrementAndGet();
                        }
                        return null;
                    }));
                }

                arranco.countDown();
                for (Future<?> t : tareas) {
                    t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                }
                executor.shutdownNow();

                assertThat(aceptadas.get() + rechazadas.get()).isEqualTo(peticiones);
                assertThat(reservasDeLaHabitacion(HABITACION))
                        .as("ronda " + ronda + ": como maximo una reserva sobrevive en el rango")
                        .hasSizeLessThanOrEqualTo(1);
                assertThat(aceptadas.get())
                        .as("ronda " + ronda + ": crearReserva es synchronized, decide una sola")
                        .isLessThanOrEqualTo(1);
            }
        }

        @Test
        @DisplayName("8. Cadena de servicios sin interbloqueos")
        void cadenaDeServiciosSinInterbloqueos() throws Exception {
            for (int ronda = 0; ronda < 5; ronda++) {
                limpiar();
                Reserva reserva = reservaService.crearReserva(
                        dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(3)));
                reservaService.confirmarReserva(reserva.getIdReserva());
                Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
                Pago pago = pagarSaldo(estadia);

                int peticiones = 16;
                ExecutorService executor = Executors.newFixedThreadPool(peticiones);
                CountDownLatch arranco = new CountDownLatch(1);
                List<Future<?>> tareas = new ArrayList<>();
                List<String> fallos = new CopyOnWriteArrayList<>();

                for (int i = 0; i < peticiones; i++) {
                    final int n = i;
                    tareas.add(executor.submit(() -> {
                        try {
                            arranco.await();
                            switch (n % 8) {
                                case 0 -> habitacionService.listarDisponibles(HOY.plusDays(5), HOY.plusDays(8));
                                case 1 -> habitacionService.tieneReservaSolapada(
                                        HABITACION, HOY.plusDays(5), HOY.plusDays(8), null);
                                case 2 -> habitacionService.finalizarLimpieza(HABITACION);
                                case 3 -> estadiaService.realizarCheckOut(estadia.getIdEstadia());
                                case 4 -> estadiaService.agregarConsumo(
                                        dtoConsumo(estadia.getIdEstadia(), "Extra " + n, 1, 5_000));
                                case 5 -> pagoService.reembolsarPago(pago.getIdPago());
                                case 6 -> reservaService.crearReserva(
                                        dtoReserva("H-002", OTRA_HABITACION,
                                                HOY.plusDays(1), HOY.plusDays(2)));
                                default -> habitacionService.buscarPorNumero(HABITACION);
                            }
                        } catch (RuntimeException e) {
                            fallos.add(e.getClass().getSimpleName());
                        }
                        return null;
                    }));
                }

                arranco.countDown();

                for (Future<?> t : tareas) {
                    t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                }
                executor.shutdownNow();

                assertThat(fallos).allMatch(n -> n.equals("IllegalStateException")
                        || n.equals("IllegalArgumentException"));
            }
        }
    }



    @Nested
    @DisplayName("9-11. Integridad de repositorios y regresion financiera")
    class IntegridadYRegresion {

        @Test
        @DisplayName("9. Los repositorios devuelven las MISMAS instancias vivas")
        void losRepositoriosCompartenIdentidad() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(3)));
            reservaService.confirmarReserva(reserva.getIdReserva());
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            Pago pago = registrarYAprobar(estadia.getIdEstadia(), 100_000);

            Habitacion hab = habitacionRepository.buscarPorNumero(HABITACION).orElseThrow();
            Huesped huesped = dataStore.getHuespedes().get(0);

            assertThat(reservaRepository.buscarPorId(reserva.getIdReserva())).get().isSameAs(reserva);
            assertThat(habitacionRepository.buscarPorNumero(HABITACION)).get().isSameAs(hab);
            assertThat(estadiaRepository.buscarPorId(estadia.getIdEstadia())).get().isSameAs(estadia);
            assertThat(pagoRepository.buscarPorId(pago.getIdPago())).get().isSameAs(pago);

            assertThat(reserva.getHuesped()).isSameAs(huesped);
            assertThat(reserva.getHabitacion()).isSameAs(hab);
            assertThat(estadia.getReserva()).isSameAs(reserva);
            assertThat(pago.getEstadia()).isSameAs(estadia);

            hab.cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);
            assertThat(habitacionService.listarPendientesLimpieza())
                    .extracting(Habitacion::getNumeroHabitacion)
                    .contains(HABITACION);
            assertThat(reservaService.obtenerOFallar(reserva.getIdReserva()).getEstadoReserva())
                    .isEqualTo(EstadoReserva.CHECKED_IN);
        }

        @Test
        @DisplayName("10. El flujo financiero no se ha visto afectado")
        void regresionFinanciera() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(3)));
            reservaService.confirmarReserva(reserva.getIdReserva());
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            String id = estadia.getIdEstadia();

            assertThat(saldoService.consultarSaldo(id).getSaldoPendiente().signum()).isPositive();

            registrarYAprobar(id, 450_000);
            assertThat(saldoService.consultarSaldo(id).getSaldoPendiente()).isEqualByComparingTo("0.00");
            assertThat(estadiaService.realizarCheckOut(id).getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(habitacionDe(HABITACION))
                    .as("el ciclo de la habitacion sigue igual tras la migracion")
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertThat(habitacionService.finalizarLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("11. REGRESION: el reembolso tras el check-out sigue prohibido")
        void elReembolsoTrasElCheckOutSigueProhibido() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(3)));
            reservaService.confirmarReserva(reserva.getIdReserva());
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            String id = estadia.getIdEstadia();

            Pago aprobado = registrarYAprobar(id, 450_000);
            estadiaService.realizarCheckOut(id);

            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThatThrownBy(() -> pagoService.reembolsarPago(aprobado.getIdPago()))
                    .as("la regla de la etapa 2F-2 sigue vigente tras integrar HabitacionService")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FINALIZADA");
            assertThat(pagoRepository.buscarPorId(aprobado.getIdPago()).orElseThrow().getEstadoPago())
                    .isEqualTo(EstadoPago.PAGADO);
            assertThat(saldoService.consultarSaldo(id).getSaldoPendiente()).isEqualByComparingTo("0.00");


            limpiar();
            Reserva otra = reservaService.crearReserva(
                    dtoReserva("H-002", OTRA_HABITACION, HOY, HOY.plusDays(3)));
            reservaService.confirmarReserva(otra.getIdReserva());
            Estadia abierta = estadiaService.realizarCheckIn(otra.getIdReserva());
            Pago abierto = registrarYAprobar(abierta.getIdEstadia(), 450_000);

            assertThat(pagoService.reembolsarPago(abierto.getIdPago()).getEstadoPago())
                    .isEqualTo(EstadoPago.REEMBOLSADO);
        }
    }



    private static Huesped huesped(String id, String nombre, String apellido, String usuario) {
        return new Huesped(id, nombre, apellido, "CC" + id.substring(2),
                "3000000000", id.toLowerCase() + "@correo.com", usuario, "pass123");
    }

    private void limpiar() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones().forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
    }

    private EstadoHabitacion habitacionDe(int numero) {
        return dataStore.getHabitaciones().stream()
                .filter(h -> h.getNumeroHabitacion() == numero)
                .findFirst().orElseThrow().getEstadoHabitacion();
    }

    private List<Reserva> reservasDeLaHabitacion(int numeroHabitacion) {
        return reservaRepository.buscarTodas().stream()
                .filter(r -> r.getHabitacion() != null
                        && r.getHabitacion().getNumeroHabitacion() == numeroHabitacion)
                .collect(Collectors.toList());
    }

    /** Reserva construida con el constructor del modelo, sin persistir en ningún sitio. */
    private Reserva reservaSinGuardar(String id, int numeroHabitacion, LocalDate inicio, LocalDate fin) {
        return new Reserva(id, inicio, fin, 2, 0,
                dataStore.getHuespedes().get(0),
                habitacionRepository.buscarPorNumero(numeroHabitacion).orElseThrow());
    }

    private Pago registrarYAprobar(String idEstadia, double monto) {
        Pago pago = pagoService.registrarPago(dtoPago(idEstadia, monto));
        return pagoService.aprobarPago(pago.getIdPago(), "REF-TEST");
    }

    private Pago pagarSaldo(Estadia estadia) {
        return registrarYAprobar(estadia.getIdEstadia(), 450_000);
    }

    private ReservaRequestDTO dtoReserva(String idHuesped, int numeroHabitacion,
                                         LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(idHuesped);
        dto.setNumeroHabitacion(numeroHabitacion);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return dto;
    }

    private ReservaActualizacionDTO dtoModificacion(int numeroHabitacion,
                                                    LocalDate inicio, LocalDate fin) {
        ReservaActualizacionDTO dto = new ReservaActualizacionDTO();
        dto.setNumeroHabitacion(numeroHabitacion);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        return dto;
    }

    private PagoRequestDTO dtoPago(String idEstadia, double monto) {
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setMonto(monto);
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Abono de prueba");
        return dto;
    }

    private ConsumoRequestDTO dtoConsumo(String idEstadia, String descripcion, int cantidad, double precio) {
        ConsumoRequestDTO dto = new ConsumoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setDescripcion(descripcion);
        dto.setCantidad(cantidad);
        dto.setPrecioUnitario(precio);
        return dto;
    }



    /** Implementación alternativa de {@link ReservaRepository} con almacén propio. */
    private static class RepoDeReservas implements ReservaRepository {

        private final List<Reserva> inventario = new CopyOnWriteArrayList<>();

        void cargar(Reserva reserva) {
            inventario.add(reserva);
        }

        @Override
        public Reserva guardar(Reserva reserva) {
            inventario.add(reserva);
            return reserva;
        }

        @Override
        public Optional<Reserva> buscarPorId(String idReserva) {
            return inventario.stream().filter(r -> r.getIdReserva().equals(idReserva)).findFirst();
        }

        @Override
        public List<Reserva> buscarTodas() {
            return inventario;
        }

        @Override
        public List<Reserva> buscarPorHuesped(String idHuesped) {
            return inventario.stream()
                    .filter(r -> r.getHuesped() != null && r.getHuesped().getIdHuesped().equals(idHuesped))
                    .collect(Collectors.toList());
        }

        @Override
        public boolean existePorCodigoOId(String codigo) {
            return buscarPorId(codigo).isPresent();
        }

        @Override
        public int contarTodas() {
            return inventario.size();
        }

        @Override
        public Reserva actualizar(Reserva reserva) {
            for (int i = 0; i < inventario.size(); i++) {
                if (inventario.get(i).getIdReserva().equals(reserva.getIdReserva())) {
                    inventario.set(i, reserva);
                    return reserva;
                }
            }
            throw new IllegalArgumentException("No existe la reserva con ID: " + reserva.getIdReserva());
        }
    }

    /**
     * Implementación alternativa de {@link EstadiaRepository} con almacén propio.
     */
    private static class RepoDeEstadias implements EstadiaRepository {

        private final List<Estadia> inventario = new CopyOnWriteArrayList<>();

        void cargar(Estadia estadia) {
            inventario.add(estadia);
        }

        void retirar(Estadia estadia) {
            inventario.remove(estadia);
        }

        @Override
        public Estadia guardar(Estadia estadia) {
            inventario.add(estadia);
            return estadia;
        }

        @Override
        public Optional<Estadia> buscarPorId(String idEstadia) {
            return inventario.stream().filter(e -> e.getIdEstadia().equals(idEstadia)).findFirst();
        }

        @Override
        public List<Estadia> buscarTodas() {
            return inventario;
        }

        @Override
        public List<Estadia> buscarPorEstado(EstadoEstadia estado) {
            return inventario.stream()
                    .filter(e -> e.getEstadoEstadia() == estado)
                    .collect(Collectors.toList());
        }

        @Override
        public Estadia actualizar(Estadia estadia) {
            for (int i = 0; i < inventario.size(); i++) {
                if (inventario.get(i).getIdEstadia().equals(estadia.getIdEstadia())) {
                    inventario.set(i, estadia);
                    return estadia;
                }
            }
            throw new IllegalArgumentException("No existe la estadía con ID: " + estadia.getIdEstadia());
        }
    }
}
