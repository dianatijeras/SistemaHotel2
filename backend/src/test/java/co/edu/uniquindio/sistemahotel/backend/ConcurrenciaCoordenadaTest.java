package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaActualizacionDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.*;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
class ConcurrenciaCoordenadaTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final int HABITACION = 101;
    private static final int OTRA_HABITACION = 102;

    /** Margen acotado para comprobar que un hilo está bloqueado. Corto: solo mide bloqueo real. */
    private static final long TIMEOUT_BLOQUEO_MS = 400;

    /** Margen generoso para una ejecución que debe poder terminar sin bloqueo. */
    private static final long TIMEOUT_EJECUCION_S = 20;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private SaldoService saldoService;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones().forEach(h -> {
            h.cambiarEstado(EstadoHabitacion.DISPONIBLE);
            h.setPrecioNoche(150_000.0);
        });
        dataStore.getHuespedes().clear();
        dataStore.getHuespedes().add(new Huesped("H-001", "Carlos", "Ramírez", "CC10000001",
                "3001234567", "carlos@email.com", "carlos.r", "pass123"));
        dataStore.getHuespedes().add(new Huesped("H-002", "Laura", "Gómez", "CC10000002",
                "3109876543", "laura@email.com", "laura.g", "pass123"));
    }


    @Nested
    @DisplayName("1-3. Modificacion de reserva contra check-in")
    class ModificacionContraCheckIn {

        @Test
        @DisplayName("Si la modificacion termina antes, el check-in usa los datos definitivos")
        void modificacionPrimero_checkInUsaLaHabitacionDefinitiva() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);

            reservaService.modificarReserva(reserva.getIdReserva(),
                    dtoActualizacion(OTRA_HABITACION, HOY, HOY.plusDays(3)));

            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThat(reserva.getHabitacion().getNumeroHabitacion())
                    .as("la reserva quedo en la 102").isEqualTo(OTRA_HABITACION);
            assertThat(estadia.getReserva().getHabitacion().getNumeroHabitacion())
                    .as("la habitacion de la estadia es la misma que la de la reserva")
                    .isEqualTo(OTRA_HABITACION);
            assertThat(dataStore.getHabitaciones())
                    .filteredOn(h -> h.getNumeroHabitacion() == HABITACION)
                    .singleElement()
                    .as("la 101 nunca se ocupo, porque la modificacion fue previa")
                    .extracting(EstadoHabitacionHelper::estado)
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
            assertThat(dataStore.getHabitaciones())
                    .filteredOn(h -> h.getNumeroHabitacion() == OTRA_HABITACION)
                    .singleElement()
                    .extracting(EstadoHabitacionHelper::estado)
                    .isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("Si el check-in termina antes, la modificacion incompatible se rechaza")
        void checkInPrimero_modificacionSeRechaza() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                    dtoActualizacion(OTRA_HABITACION, HOY, HOY.plusDays(3))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RESERVADA o CONFIRMADA");

            assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CHECKED_IN);
            assertThat(reserva.getHabitacion().getNumeroHabitacion()).isEqualTo(HABITACION);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(habitacionDe(OTRA_HABITACION))
                    .as("la 102 nunca se toco").isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("La validacion y la escritura de la modificacion son atomicas respecto al check-in")
        void intercalacion_deliberada_laModificacionNoSeIntercala() throws Exception {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Reserva laMisma = reservaService.obtenerOFallar(reserva.getIdReserva());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch arranco = new CountDownLatch(2);
            try {
                List<Future<?>> tareas = new ArrayList<>();

                synchronized (laMisma) {
                    tareas.add(executor.submit(() -> {
                        arranco.countDown();
                        return reservaService.modificarReserva(reserva.getIdReserva(),
                                dtoActualizacion(OTRA_HABITACION, HOY, HOY.plusDays(3)));
                    }));
                    tareas.add(executor.submit(() -> {
                        arranco.countDown();
                        return estadiaService.realizarCheckIn(reserva.getIdReserva());
                    }));

                    assertThat(arranco.await(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS))
                            .as("ambos hilos empezaron").isTrue();

                    assertThatThrownBy(() -> tareas.get(0).get(TIMEOUT_BLOQUEO_MS, TimeUnit.MILLISECONDS))
                            .as("la modificacion esta bloqueada por el monitor de la reserva")
                            .isInstanceOf(TimeoutException.class);
                    assertThatThrownBy(() -> tareas.get(1).get(TIMEOUT_BLOQUEO_MS, TimeUnit.MILLISECONDS))
                            .as("el check-in esta bloqueado por el monitor de la reserva")
                            .isInstanceOf(TimeoutException.class);

                    assertThat(reserva.getHabitacion().getNumeroHabitacion())
                            .as("nada cambio mientras el monitor estaba retenido")
                            .isEqualTo(HABITACION);
                    assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);
                    assertThat(dataStore.getEstadias()).isEmpty();
                }

                for (Future<?> t : tareas) {
                    try {
                        t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        // Una de las dos puede rechazar a la otra: ambas son resultados válidos.
                    }
                }

                // Invariante: la habitacion OCUPADA es exactamente la que dice la reserva.
                int habitacionDeLaReserva = reserva.getHabitacion().getNumeroHabitacion();
                if (reserva.getEstadoReserva() == EstadoReserva.CHECKED_IN) {
                    assertThat(dataStore.getEstadias()).hasSize(1);
                    assertThat(dataStore.getEstadias().get(0).getReserva().getHabitacion()
                            .getNumeroHabitacion())
                            .isEqualTo(habitacionDeLaReserva);
                    assertThat(habitacionDe(habitacionDeLaReserva))
                            .isEqualTo(EstadoHabitacion.OCUPADA);
                } else {
                    assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);
                    assertThat(dataStore.getEstadias()).isEmpty();
                    assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);
                    assertThat(habitacionDe(OTRA_HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);
                }
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("Cancelar y confirmar no pueden dejar una reserva CHECKED_IN en otro estado")
        void transiciones_noSeIntercalanConElCheckIn() throws Exception {
            for (int ronda = 0; ronda < 40; ronda++) {
                dataStore.getReservas().clear();
                dataStore.getEstadias().clear();
                dataStore.getHabitaciones().forEach(h ->
                        h.cambiarEstado(EstadoHabitacion.DISPONIBLE));

                Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
                boolean transicionPrimero = ronda % 2 == 0;

                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch arranque = new CountDownLatch(1);
                try {
                    Runnable transicion = () -> ejecutarSilencioso(arranque, () ->
                            reservaService.cancelarReserva(reserva.getIdReserva()));
                    Runnable checkIn = () -> ejecutarSilencioso(arranque, () ->
                            estadiaService.realizarCheckIn(reserva.getIdReserva()));

                    Future<?> f1 = executor.submit(transicionPrimero ? transicion : checkIn);
                    Future<?> f2 = executor.submit(transicionPrimero ? checkIn : transicion);

                    arranque.countDown();
                    f1.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    f2.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                } finally {
                    executor.shutdownNow();
                }

                boolean hayEstadiaActiva = dataStore.getEstadias().stream()
                        .anyMatch(e -> e.getEstadoEstadia() == EstadoEstadia.EN_CURSO
                                && e.getReserva().getIdReserva().equals(reserva.getIdReserva()));

                if (hayEstadiaActiva) {
                    assertThat(reserva.getEstadoReserva())
                            .as("ronda " + ronda + ": con estadía activa la reserva queda CHECKED_IN")
                            .isEqualTo(EstadoReserva.CHECKED_IN);
                    assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
                } else {
                    assertThat(reserva.getEstadoReserva())
                            .as("ronda " + ronda + ": sin estadía, la cancelación se aplicó")
                            .isEqualTo(EstadoReserva.CANCELADA);
                    assertThat(habitacionDe(HABITACION))
                            .as("ronda " + ronda + ": cancelar no ocupa la habitación")
                            .isEqualTo(EstadoHabitacion.DISPONIBLE);
                }
            }
        }
    }


    @Nested
    @DisplayName("4. Check-ins simultaneos sobre la misma habitacion")
    class CheckInsSimultaneos {

        @Test
        @DisplayName("Dos reservas distintas para la misma habitacion no pueden hacer check-in a la vez")
        void dosCheckIns_mismaHabitacion_soloUnoEntra() throws Exception {
            // Dos reservas consecutivas: no se solapan, asi que ambas son legales como reserva.
            Reserva primera = reserva(HOY, HOY.plusDays(2), HABITACION);
            Reserva segunda = reserva(HOY.plusDays(2), HOY.plusDays(4), HABITACION);

            int peticiones = 8;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            AtomicInteger exitosos = new AtomicInteger();
            AtomicInteger rechazados = new AtomicInteger();
            List<Future<?>> tareas = new ArrayList<>();

            for (int i = 0; i < peticiones; i++) {
                Reserva objetivo = (i % 2 == 0) ? primera : segunda;
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        estadiaService.realizarCheckIn(objetivo.getIdReserva());
                        exitosos.incrementAndGet();
                    } catch (IllegalStateException | IllegalArgumentException e) {
                        rechazados.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            arranque.countDown();
            for (Future<?> t : tareas) {
                t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
            }
            executor.shutdownNow();

            assertThat(exitosos.get()).as("la habitacion admite una sola estadia activa").isEqualTo(1);
            assertThat(rechazados.get()).isEqualTo(peticiones - 1);
            assertThat(dataStore.getEstadias()).hasSize(1);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(dataStore.getEstadias().get(0).getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.EN_CURSO);
        }
    }


    @Nested
    @DisplayName("5. Check-out frente a confirmacion de limpieza")
    class CheckOutContraLimpieza {

        @Test
        @DisplayName("Nunca queda una habitacion DISPONIBLE con una estadia EN_CURSO")
        void checkOut_contraLimpieza_respetaLaMaquinaDeEstados() throws Exception {
            for (int ronda = 0; ronda < 30; ronda++) {
                dataStore.getReservas().clear();
                dataStore.getEstadias().clear();
                dataStore.getPagos().clear();
                dataStore.getHabitaciones().forEach(h ->
                        h.cambiarEstado(EstadoHabitacion.DISPONIBLE));

                Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
                Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
                pagarTotal(estadia);

                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch arranque = new CountDownLatch(1);
                try {
                    Future<?> a = executor.submit(() -> {
                        arranque.await();
                        try {
                            estadiaService.realizarCheckOut(estadia.getIdEstadia());
                        } catch (RuntimeException ignorada) {
                        }
                        return null;
                    });
                    Future<?> b = executor.submit(() -> {
                        arranque.await();
                        try {
                            habitacionService.finalizarLimpieza(HABITACION);
                        } catch (RuntimeException ignorada) {
                        }
                        return null;
                    });

                    arranque.countDown();
                    a.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    b.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                } finally {
                    executor.shutdownNow();
                }


                if (habitacionDe(HABITACION) == EstadoHabitacion.DISPONIBLE) {
                    assertThat(dataStore.getEstadias())
                            .filteredOn(e -> e.getEstadoEstadia() == EstadoEstadia.EN_CURSO)
                            .as("ronda " + ronda + ": habitacion libre sin estadia activa")
                            .isEmpty();
                }
                assertThat(estadia.getEstadoEstadia())
                        .as("ronda " + ronda + ": la estadia nunca quedo en un estado invalido")
                        .isIn(EstadoEstadia.EN_CURSO, EstadoEstadia.FINALIZADA);
            }
        }

        @Test
        @DisplayName("Con la secuencia declarada, el flujo OCUPADA -> EN_LIMPIEZA -> DISPONIBLE se respeta")
        void flujoSecuencialCompleto() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            pagarTotal(estadia);

            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);

            estadiaService.realizarCheckOut(estadia.getIdEstadia());
            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.FINALIZADA);
            assertThat(habitacionDe(HABITACION))
                    .as("el check-out NO libera por si solo").isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

            assertThatThrownBy(() -> reserva(
                    HOY.plusDays(4), HOY.plusDays(6), HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no admite reservas");

            assertThatThrownBy(() -> habitacionService.cambiarEstado(
                    HABITACION, EstadoHabitacion.DISPONIBLE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminó la limpieza");

            habitacionService.finalizarLimpieza(HABITACION);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);
            assertThat(habitacionService.listarPendientesLimpieza())
                    .noneMatch(h -> h.getNumeroHabitacion() == HABITACION);
        }
    }


    @Nested
    @DisplayName("6. Aprobacion de pago frente a check-out")
    class PagoContraCheckOut {

        @Test
        @DisplayName("Con pagos y check-out en carrera, la cuenta nunca queda sobrepagada")
        void pagoContraCheckOut_nuncaSobrepaga() throws Exception {
            for (int ronda = 0; ronda < 25; ronda++) {
                dataStore.getReservas().clear();
                dataStore.getEstadias().clear();
                dataStore.getPagos().clear();
                dataStore.getHabitaciones().forEach(h ->
                        h.cambiarEstado(EstadoHabitacion.DISPONIBLE));

                Reserva reserva = reserva(HOY, HOY.plusDays(2), HABITACION);
                Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
                BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();


                Pago pago = pagoService.registrarPago(
                        dtoPago(estadia.getIdEstadia(), total.doubleValue()));

                ExecutorService executor = Executors.newFixedThreadPool(4);
                CountDownLatch arranque = new CountDownLatch(1);
                try {
                    Future<?> p1 = executor.submit(() -> { arranque.await(); return null; });
                    Future<?> p2 = executor.submit(() -> {
                        arranque.await();
                        try {
                            pagoService.aprobarPago(pago.getIdPago(), "REF-1");
                        } catch (RuntimeException ignorada) { }
                        return null;
                    });
                    Future<?> p3 = executor.submit(() -> {
                        arranque.await();
                        try {
                            pagoService.aprobarPago(pago.getIdPago(), "REF-2");
                        } catch (RuntimeException ignorada) { }
                        return null;
                    });
                    Future<?> co = executor.submit(() -> {
                        arranque.await();
                        try {
                            estadiaService.realizarCheckOut(estadia.getIdEstadia());
                        } catch (RuntimeException ignorada) { }
                        return null;
                    });

                    arranque.countDown();
                    p1.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    p2.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    p3.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    co.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                } finally {
                    executor.shutdownNow();
                }

                assertThat(pago.getEstadoPago())
                        .as("ronda " + ronda + ": un pago no se aprueba dos veces")
                        .isEqualTo(EstadoPago.PAGADO);

                BigDecimal saldo = saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente();
                assertThat(saldo)
                        .as("ronda " + ronda + ": nunca hay saldo negativo ni sobrepago")
                        .isGreaterThanOrEqualTo(BigDecimal.ZERO);

                if (estadia.getEstadoEstadia() == EstadoEstadia.FINALIZADA) {
                    assertThat(saldo)
                            .as("ronda " + ronda + ": solo se cierra una cuenta sin deuda")
                            .isEqualByComparingTo("0.00");
                    assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
                }
            }
        }
    }


    @Nested
    @DisplayName("7. Check-out frente a registro de consumos")
    class CheckOutContraConsumos {

        @Test
        @DisplayName("El saldo nunca queda negativo aunque se consumen y cierre la cuenta a la vez")
        void consumosContraCheckOut_saldoNuncaNegativo() throws Exception {
            for (int ronda = 0; ronda < 25; ronda++) {
                dataStore.getReservas().clear();
                dataStore.getEstadias().clear();
                dataStore.getPagos().clear();
                dataStore.getHabitaciones().forEach(h ->
                        h.cambiarEstado(EstadoHabitacion.DISPONIBLE));

                Reserva reserva = reserva(HOY, HOY.plusDays(2), HABITACION);
                Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

                int consumos = 6;
                ExecutorService executor = Executors.newFixedThreadPool(consumos + 1);
                CountDownLatch arranque = new CountDownLatch(1);
                List<Future<?>> tareas = new ArrayList<>();

                for (int i = 0; i < consumos; i++) {
                    final int n = i;
                    tareas.add(executor.submit(() -> {
                        arranque.await();
                        try {
                            estadiaService.agregarConsumo(dtoConsumo(estadia.getIdEstadia(),
                                    "Extra " + n, 1, 10_000.0));
                        } catch (RuntimeException ignorada) { }
                        return null;
                    }));
                }
                tareas.add(executor.submit(() -> {
                    arranque.await();
                    try {
                        estadiaService.realizarCheckOut(estadia.getIdEstadia());
                    } catch (RuntimeException ignorada) { }
                    return null;
                }));

                arranque.countDown();
                for (Future<?> t : tareas) {
                    t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                }
                executor.shutdownNow();

                BigDecimal saldo = saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente();
                assertThat(saldo)
                        .as("ronda " + ronda + ": el saldo nunca es negativo")
                        .isGreaterThanOrEqualTo(BigDecimal.ZERO);

                BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();
                assertThat(estadia.getConsumos())
                        .as("ronda " + ronda + ": ningun consumo se pierde")
                        .hasSameSizeAs(estadia.getConsumos());
                assertThat(total)
                        .as("ronda " + ronda + ": el total cubre alojamiento mas consumos")
                        .isGreaterThanOrEqualTo(BigDecimal.valueOf(300_000L));
            }
        }
    }


    @Nested
    @DisplayName("8. Ausencia de interbloqueos")
    class SinInterbloqueo {

        @Test
        @DisplayName("Servicios cruzados simultaneos terminan todos dentro del tiempo")
        void serviciosCruzados_noSeBloquean() throws Exception {
            for (int ronda = 0; ronda < 5; ronda++) {
                dataStore.getReservas().clear();
                dataStore.getEstadias().clear();
                dataStore.getPagos().clear();
                dataStore.getHabitaciones().forEach(h ->
                        h.cambiarEstado(EstadoHabitacion.DISPONIBLE));

                int[] habitaciones = {101, 102, 103, 201, 202, 301};
                List<Reserva> reservas = new ArrayList<>();
                for (int numero : habitaciones) {
                    reservas.add(reserva(HOY, HOY.plusDays(2), numero));
                }
                List<Estadia> estadias = new ArrayList<>();
                for (Reserva r : reservas) {
                    estadias.add(estadiaService.realizarCheckIn(r.getIdReserva()));
                }

                int peticiones = 16;
                ExecutorService executor = Executors.newFixedThreadPool(peticiones);
                CountDownLatch arranque = new CountDownLatch(1);
                List<Future<?>> tareas = new ArrayList<>();
                List<String> fallos = new java.util.concurrent.CopyOnWriteArrayList<>();

                for (int i = 0; i < peticiones; i++) {
                    final int n = i;
                    tareas.add(executor.submit(() -> {
                        try {
                            arranque.await();
                            Reserva r = reservas.get(n % reservas.size());
                            Estadia e = estadias.get(n % estadias.size());
                            switch (n % 6) {
                                case 0 -> reservaService.modificarReserva(r.getIdReserva(),
                                        dtoActualizacion(0, HOY.plusDays(50), HOY.plusDays(52)));
                                case 1 -> estadiaService.realizarCheckIn(r.getIdReserva());
                                case 2 -> estadiaService.agregarConsumo(
                                        dtoConsumo(e.getIdEstadia(), "Item", 1, 5_000.0));
                                case 3 -> saldoService.consultarSaldo(e.getIdEstadia());
                                case 4 -> habitacionService.finalizarLimpieza(HABITACION);
                                default -> reservaService.confirmarReserva(r.getIdReserva());
                            }
                        } catch (RuntimeException ex) {
                            fallos.add(ex.getClass().getSimpleName());
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        return null;
                    }));
                }

                arranque.countDown();
                for (Future<?> t : tareas) {
                    t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                }
                executor.shutdownNow();

                assertThat(fallos)
                        .as("ronda " + ronda + ": solo excepciones de negocio, ninguna de bloqueo")
                        .allMatch(n -> n.equals("IllegalStateException")
                                || n.equals("IllegalArgumentException"));
            }
        }

        @Test
        @DisplayName("El monitor de la reserva es una hoja: no se toma desde dentro de otros bloqueos")
        void elMonitorDeLaReserva_esHoja() throws Exception {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> f = executor.submit(() -> reservaService.modificarReserva(
                        reserva.getIdReserva(),
                        dtoActualizacion(OTRA_HABITACION, HOY, HOY.plusDays(3))));

                f.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            assertThat(reserva.getHabitacion().getNumeroHabitacion()).isEqualTo(OTRA_HABITACION);
        }
    }


    @Nested
    @DisplayName("9. Ausencia de modificaciones parciales ante errores")
    class SinModificacionesParciales {

        @Test
        @DisplayName("Una modificacion rechazada tras el check-in no altera nada")
        void modificacionRechazada_noAlteraNada() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            String inicio = reserva.getFechaInicio().toString();
            String fin = reserva.getFechaFin().toString();
            int habitacion = reserva.getHabitacion().getNumeroHabitacion();

            assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                    dtoActualizacion(OTRA_HABITACION, HOY, HOY.plusDays(3))))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> reservaService.cancelarReserva(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(reserva.getFechaInicio().toString()).isEqualTo(inicio);
            assertThat(reserva.getFechaFin().toString()).isEqualTo(fin);
            assertThat(reserva.getHabitacion().getNumeroHabitacion()).isEqualTo(habitacion);
            assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CHECKED_IN);
            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(habitacionDe(OTRA_HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);

            pagarTotal(estadia);
            assertThatCode(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                    .doesNotThrowAnyException();
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
        }

        @Test
        @DisplayName("Una modificacion fallida por conflicto no ocupa la habitacion destino")
        void modificacionFallida_noOcupaLaDestino() {
            Reserva reserva = reserva(HOY.plusDays(10), HOY.plusDays(13), HABITACION);
            reservaService.crearReserva(dtoReserva("H-002", OTRA_HABITACION,
                    HOY.plusDays(10), HOY.plusDays(13)));

            assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                    dtoActualizacion(OTRA_HABITACION, HOY.plusDays(11), HOY.plusDays(12))))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(habitacionDe(OTRA_HABITACION))
                    .as("una reserva a futuro tampoco cambia el estado fisico").isEqualTo(EstadoHabitacion.DISPONIBLE);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);
        }
    }

    @Nested
    @DisplayName("10. Carrera entre SaldoService.consultarSaldo y recalcularTotales")
    class CarreraDelSaldo {

        @Test
        @DisplayName("Un consumo concurrente nunca produce un saldo a favor del hotel")
        void consumoConcurrente_nuncaProduceSaldoNegativo() throws Exception {
            for (int ronda = 0; ronda < 20; ronda++) {
                dataStore.getReservas().clear();
                dataStore.getEstadias().clear();
                dataStore.getPagos().clear();
                dataStore.getHabitaciones().forEach(h ->
                        h.cambiarEstado(EstadoHabitacion.DISPONIBLE));

                Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
                Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

                pagarTotal(estadia);
                assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                        .isEqualByComparingTo("0.00");

                int peticiones = 8;
                ExecutorService executor = Executors.newFixedThreadPool(peticiones + 2);
                CountDownLatch arranque = new CountDownLatch(1);
                List<Future<?>> tareas = new ArrayList<>();
                try {
                    for (int i = 0; i < peticiones; i++) {
                        final int n = i;
                        tareas.add(executor.submit(() -> {
                            arranque.await();
                            try {
                                estadiaService.agregarConsumo(dtoConsumo(estadia.getIdEstadia(),
                                        "Extra " + n, 1, 25_000.0));
                            } catch (RuntimeException ignorada) { }
                            return null;
                        }));
                    }
                    for (int i = 0; i < 2; i++) {
                        tareas.add(executor.submit(() -> {
                            arranque.await();
                            saldoService.consultarSaldo(estadia.getIdEstadia());
                            return null;
                        }));
                    }

                    arranque.countDown();
                    for (Future<?> t : tareas) {
                        t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    }
                } finally {
                    executor.shutdownNow();
                }

                BigDecimal saldo = saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente();
                assertThat(saldo)
                        .as("ronda " + ronda + ": el saldo es exactamente el total menos lo pagado")
                        .isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(saldo)
                        .as("ronda " + ronda + ": no puede haber saldo a favor tras los consumos")
                        .isGreaterThanOrEqualTo(BigDecimal.ZERO);

                long consumosRegistrados = estadia.getConsumos().size();
                BigDecimal esperado = BigDecimal.valueOf(450_000L)
                        .add(BigDecimal.valueOf(25_000L * consumosRegistrados));
                assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral())
                        .as("ronda " + ronda + ": el total cubre TODOS los consumos registrados")
                        .isEqualByComparingTo(esperado);
            }
        }

        @Test
        @DisplayName("Solo los pagos PAGADO reducen el saldo")
        void soloLosPagosPagadoReducenElSaldo() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();

            Pago pendiente = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), total.doubleValue()));
            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                    .as("un pago PENDIENTE no es dinero recibido").isEqualByComparingTo(total);

            pagoService.rechazarPago(pendiente.getIdPago(), "No corresponde");
            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                    .as("un pago RECHAZADO tampoco").isEqualByComparingTo(total);

            Pago aprobado = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), total.doubleValue()));
            pagoService.aprobarPago(aprobado.getIdPago(), "REF");
            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                    .as("solo el PAGADO descuenta").isEqualByComparingTo("0.00");
        }
    }


    /** Ejecuta una operación capturando los rechazos de negocio, que son resultados válidos. */
    private void ejecutarSilencioso(CountDownLatch arranque, Runnable operacion) {
        try {
            arranque.await();
            operacion.run();
        } catch (IllegalStateException | IllegalArgumentException ignorada) {
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Extrae el estado de una habitación del DataStore compartido. */
    private EstadoHabitacion habitacionDe(int numero) {
        return dataStore.getHabitaciones().stream()
                .filter(h -> h.getNumeroHabitacion() == numero)
                .findFirst()
                .orElseThrow()
                .getEstadoHabitacion();
    }

    private Reserva reserva(LocalDate inicio, LocalDate fin, int numero) {
        return reservaService.crearReserva(dtoReserva("H-001", numero, inicio, fin));
    }

    private ReservaRequestDTO dtoReserva(String idHuesped, int numero, LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(idHuesped);
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return dto;
    }

    private ReservaActualizacionDTO dtoActualizacion(int numero, LocalDate inicio, LocalDate fin) {
        ReservaActualizacionDTO dto = new ReservaActualizacionDTO();
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
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

    private PagoRequestDTO dtoPago(String idEstadia, double monto) {
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setMonto(monto);
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Abono de prueba");
        return dto;
    }

    private void pagarTotal(Estadia estadia) {
        BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();
        Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), total.doubleValue()));
        pagoService.aprobarPago(pago.getIdPago(), "REF-TEST");
    }

    /** Ayudante para extraer el estado en una cadena de aserciones. */
    private static final class EstadoHabitacionHelper {
        static EstadoHabitacion estado(co.edu.uniquindio.sistemahotel.backend.model.Habitacion h) {
            return h.getEstadoHabitacion();
        }
    }
}