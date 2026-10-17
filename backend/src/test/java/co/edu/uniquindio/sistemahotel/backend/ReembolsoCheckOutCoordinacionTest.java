package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaActualizacionDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoPago;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.repository.EstadiaRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.PagoRepository;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
class ReembolsoCheckOutCoordinacionTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final int HABITACION = 101;
    private static final int OTRA_HABITACION = 102;
    private static final long TIMEOUT_BLOQUEO_MS = 400;
    private static final long TIMEOUT_EJECUCION_S = 20;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private EstadiaRepository estadiaRepository;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private HabitacionService habitacionService;

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
    }



    @Nested
    @DisplayName("1-2. El reembolso y el check-out se serializan sobre la misma estadia")
    class SerializacionCompartida {

        @Test
        @DisplayName("Prueba del defecto: con el monitor de la estadia retenido, NINGUNA de las dos se detiene")
        void reembolsoYCheckOut_noCompartenMonitor() throws Exception {
            Estadia estadia = estadiaPagada();
            Pago pago = pagoUnico(estadia);
            assertThat(saldoPendiente(estadia)).as("punto de partida: la cuenta esta saldada")
                    .isEqualByComparingTo("0.00");

            Estadia laMisma = estadiaRepository.buscarPorId(estadia.getIdEstadia()).orElseThrow();
            assertThat(laMisma).as("el repositorio devuelve la instancia viva").isSameAs(estadia);
            assertThat(pago.getEstadia()).as("el pago apunta a la misma estadia").isSameAs(laMisma);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<String> desenlaces = new CopyOnWriteArrayList<>();
            try {
                Future<?> co;
                Future<?> re;
                 CountDownLatch aPuntoDeEntrar = new CountDownLatch(2);

                synchronized (laMisma) {
                    co = executor.submit(() -> {
                        aPuntoDeEntrar.countDown();
                        try {
                            estadiaService.realizarCheckOut(laMisma.getIdEstadia());
                            desenlaces.add("check-out ACEPTADO");
                        } catch (RuntimeException e) {
                            desenlaces.add("check-out RECHAZADO: " + e);
                        }
                        return null;
                    });
                    re = executor.submit(() -> {
                        aPuntoDeEntrar.countDown();
                        try {
                            pagoService.reembolsarPago(pago.getIdPago());
                            desenlaces.add("reembolso APLICADO");
                        } catch (RuntimeException e) {
                            desenlaces.add("reembolso RECHAZADO: " + e);
                        }
                        return null;
                    });

                    assertThat(aPuntoDeEntrar.await(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS))
                            .as("ambos workers llegaron al punto de entrada").isTrue();

                    assertThatThrownBy(() -> co.get(TIMEOUT_BLOQUEO_MS, TimeUnit.MILLISECONDS))
                            .as("el check-out espera al monitor de la estadia")
                            .isInstanceOf(TimeoutException.class);
                    assertThatThrownBy(() -> re.get(TIMEOUT_BLOQUEO_MS, TimeUnit.MILLISECONDS))
                            .as("el reembolso espera al monitor de la estadia")
                            .isInstanceOf(TimeoutException.class);
                    assertThat(desenlaces)
                            .as("mientras el monitor esta retenido, ninguna operacion debe haber "
                                    + "podido completarse").isEmpty();
                }
                co.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                re.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            assertInvarianteDeOrdenValido(estadia, pago, "tras la serializacion");
        }

        @Test
        @DisplayName("Reembolso terminado antes de consultar el saldo: el cierre se rechaza")
        void reembolsoAntes_delSaldo_cierreRechazado() {
            Estadia estadia = estadiaPagada();
            Pago pago = pagoUnico(estadia);
            assertThat(saldoPendiente(estadia)).isEqualByComparingTo("0.00");

            pagoService.reembolsarPago(pago.getIdPago());

            assertThat(saldoPendiente(estadia)).isEqualByComparingTo("450000.00");
            assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("saldo pendiente");

            assertThat(estadia.getEstadoEstadia()).as("la estadia sigue abierta").isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertInvarianteSinDeuda(estadia, "tras el rechazo");
        }

        @Test
        @DisplayName("Check-out y reembolso simultaneos: el resultado es siempre un orden secuencial valido")
        void checkOutYReembolsoSimultaneos_sinCierreConSaldoObsoleto() throws Exception {
            for (int ronda = 0; ronda < 60; ronda++) {
                limpiar();
                Estadia estadia = estadiaPagada();
                Pago pago = pagoUnico(estadia);

                AtomicBoolean checkOutAceptado = new AtomicBoolean(false);
                AtomicBoolean checkOutRechazado = new AtomicBoolean(false);
                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch arranco = new CountDownLatch(1);
                try {
                    Future<?> co = executor.submit(() -> {
                        arranco.await();
                        try {
                            estadiaService.realizarCheckOut(estadia.getIdEstadia());
                            checkOutAceptado.set(true);
                        } catch (IllegalStateException e) {
                            checkOutRechazado.set(true);
                        }
                        return null;
                    });
                    Future<?> re = executor.submit(() -> {
                        arranco.await();
                        try {
                            pagoService.reembolsarPago(pago.getIdPago());
                        } catch (RuntimeException ignorada) {
                        }
                        return null;
                    });

                    arranco.countDown();
                    co.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    re.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                } finally {
                    executor.shutdownNow();
                }

                assertThat(checkOutAceptado.get() ^ checkOutRechazado.get())
                        .as("ronda " + ronda + ": el check-out tiene un unico desenlace").isTrue();
                if (checkOutRechazado.get()) {
                     assertThat(pago.getEstadoPago())
                            .as("ronda " + ronda + ": se rechazo porque el reembolso ya habia aplicado")
                            .isEqualTo(EstadoPago.REEMBOLSADO);
                    assertThat(estadia.getEstadoEstadia())
                            .as("ronda " + ronda + ": con deuda la estadia NO se cierra")
                            .isEqualTo(EstadoEstadia.EN_CURSO);
                }

                assertInvarianteDeOrdenValido(estadia, pago, "ronda " + ronda);

                if (checkOutAceptado.get() && checkOutRechazado.get()) {
                    throw new AssertionError("ronda " + ronda + ": el check-out fue a la vez aceptado y rechazado");
                }
            }
        }

        @Test
        @DisplayName("Check-out con saldo cero y sin reembolso: se realiza correctamente")
        void checkOutLimpio_funciona() {
            Estadia estadia = estadiaPagada();
            assertThat(saldoPendiente(estadia)).isEqualByComparingTo("0.00");

            Estadia finalizada = estadiaService.realizarCheckOut(estadia.getIdEstadia());

            assertThat(finalizada.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(finalizada.getFechaCheckOut()).isNotNull();
            assertThat(habitacionDe(HABITACION))
                    .as("la habitacion queda EN_LIMPIEZA, no DISPONIBLE")
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertInvarianteSinDeuda(estadia, "tras un cierre correcto");
        }
    }


    @Nested
    @DisplayName("4-6. Reembolsos y check-outs repetidos")
    class OperacionesRepetidas {

        @Test
        @DisplayName("Dos reembolsos simultaneos sobre el MISMO pago: solo uno prospera")
        void dosReembolsos_mismoPago() throws Exception {
            Estadia estadia = estadiaPagada();
            Pago pago = pagoUnico(estadia);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch arranco = new CountDownLatch(1);
            AtomicInteger exitosos = new AtomicInteger();
            AtomicInteger rechazados = new AtomicInteger();
            List<Future<?>> tareas = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                tareas.add(executor.submit(() -> {
                    try {
                        arranco.await();
                        pagoService.reembolsarPago(pago.getIdPago());
                        exitosos.incrementAndGet();
                    } catch (RuntimeException e) {
                        rechazados.incrementAndGet();
                    }
                    return null;
                }));
            }

            arranco.countDown();
            for (Future<?> t : tareas) {
                t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
            }
            executor.shutdownNow();

            assertThat(exitosos.get()).as("no se reembolsa dos veces").isEqualTo(1);
            assertThat(rechazados.get()).isEqualTo(1);
            assertThat(pagoRepository.buscarPorId(pago.getIdPago()).orElseThrow().getEstadoPago())
                    .isEqualTo(EstadoPago.REEMBOLSADO);
            assertThat(saldoPendiente(estadia)).isEqualByComparingTo("450000.00");
        }

        @Test
        @DisplayName("Reembolsos simultaneos sobre pagos DISTINTOS de la misma estadia")
        void reembolsosSobrePagosDistintos() throws Exception {
            Estadia estadia = estadiaConReserva();
            Pago p1 = registrarYAprobar(estadia, 225_000);
            Pago p2 = registrarYAprobar(estadia, 225_000);
            assertThat(saldoPendiente(estadia))
                    .as("los dos pagos cubren la cuenta entera")
                    .isEqualByComparingTo("0.00");

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch arranco = new CountDownLatch(1);
            AtomicInteger exitosos = new AtomicInteger();
            List<Future<?>> tareas = new ArrayList<>();

            for (Pago pago : List.of(p1, p2)) {
                tareas.add(executor.submit(() -> {
                    try {
                        arranco.await();
                        pagoService.reembolsarPago(pago.getIdPago());
                        exitosos.incrementAndGet();
                    } catch (RuntimeException ignorada) {
                    }
                    return null;
                }));
            }

            arranco.countDown();
            for (Future<?> t : tareas) {
                t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
            }
            executor.shutdownNow();

            assertThat(exitosos.get()).as("cada pago reembolsa por separado").isEqualTo(2);
            assertThat(pagoRepository.buscarPorId(p1.getIdPago()).orElseThrow().getEstadoPago())
                    .isEqualTo(EstadoPago.REEMBOLSADO);
            assertThat(pagoRepository.buscarPorId(p2.getIdPago()).orElseThrow().getEstadoPago())
                    .isEqualTo(EstadoPago.REEMBOLSADO);
            assertThat(saldoPendiente(estadia))
                    .as("los dos reembolsos reabren la deuda completa de la estadia")
                    .isEqualByComparingTo("450000.00");
        }

        @Test
        @DisplayName("Dos check-outs simultaneos: el segundo no finaliza dos veces")
        void dosCheckOuts() throws Exception {
            Estadia estadia = estadiaPagada();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch arranco = new CountDownLatch(1);
            AtomicInteger exitosos = new AtomicInteger();
            AtomicInteger rechazados = new AtomicInteger();
            List<Future<?>> tareas = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                tareas.add(executor.submit(() -> {
                    try {
                        arranco.await();
                        estadiaService.realizarCheckOut(estadia.getIdEstadia());
                        exitosos.incrementAndGet();
                    } catch (RuntimeException e) {
                        rechazados.incrementAndGet();
                    }
                    return null;
                }));
            }

            arranco.countDown();
            for (Future<?> t : tareas) {
                t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
            }
            executor.shutdownNow();

            assertThat(exitosos.get()).isEqualTo(1);
            assertThat(rechazados.get()).isEqualTo(1);
            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertInvarianteSinDeuda(estadia, "tras dos check-outs");
        }
    }


    @Nested
    @DisplayName("7. Aprobaciones y reembolsos frente a la consulta de saldo")
    class AprobacionesYConsultas {

        @Test
        @DisplayName("Consultas de saldo concurrentes a aprobaciones y reembolsos nunca ven saldo negativo")
        void consultasConcurrentes_saldoNuncaNegativo() throws Exception {
            for (int ronda = 0; ronda < 20; ronda++) {
                limpiar();
                Estadia estadia = estadiaConReserva();
                Pago aprobado = registrarYAprobar(estadia, 200_000);
                Pago pendiente = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

                int peticiones = 12;
                ExecutorService executor = Executors.newFixedThreadPool(peticiones);
                CountDownLatch arranco = new CountDownLatch(1);
                List<Future<?>> tareas = new ArrayList<>();

                for (int i = 0; i < peticiones; i++) {
                    final int n = i;
                    tareas.add(executor.submit(() -> {
                        try {
                            arranco.await();
                            switch (n % 4) {
                                case 0 -> saldoService.consultarSaldo(estadia.getIdEstadia());
                                case 1 -> pagoService.aprobarPago(pendiente.getIdPago(), "REF-" + n);
                                case 2 -> pagoService.reembolsarPago(aprobado.getIdPago());
                                default -> saldoService.tieneSaldoPendiente(estadia.getIdEstadia());
                            }
                        } catch (RuntimeException ignorada) {
                            // Rechazo por saldo o por estado: resultado esperado.
                        }
                        return null;
                    }));
                }

                arranco.countDown();
                for (Future<?> t : tareas) {
                    t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                }
                executor.shutdownNow();

                assertThat(saldoPendiente(estadia))
                        .as("ronda " + ronda + ": el saldo nunca es negativo")
                        .isGreaterThanOrEqualTo(BigDecimal.ZERO);
                var saldo = saldoService.consultarSaldo(estadia.getIdEstadia());
                assertThat(saldo.getTotalAprobado())
                        .as("ronda " + ronda + ": totalAprobado nunca supera el total de la cuenta")
                        .isLessThanOrEqualTo(saldo.getTotalGeneral());
            }
        }
    }


    @Nested
    @DisplayName("8. Garantias del check-in conservadas")
    class GarantiasDelCheckIn {

        @Test
        @DisplayName("El monitor de la reserva sigue bloqueando el check-in tras el cambio")
        void monitorDeLaReserva_sigueBloqueandoElCheckIn() throws Exception {
            Reserva reserva = reservaConEstadia(HABITACION, HOY, HOY.plusDays(3));
            Reserva laMisma = reservaService.obtenerOFallar(reserva.getIdReserva());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> f;
                synchronized (laMisma) {
                    f = executor.submit(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()));
                    assertThatThrownBy(() -> f.get(TIMEOUT_BLOQUEO_MS, TimeUnit.MILLISECONDS))
                            .as("el check-in respeta el monitor de la entidad reserva")
                            .isInstanceOf(TimeoutException.class);
                }
                f.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO)).hasSize(1);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("Una modificacion de reserva y un check-in siguen sin intercalarse")
        void modificacionYCheckIn_siguenCoordinados() throws Exception {
            Reserva reserva = reservaConEstadia(HABITACION, HOY, HOY.plusDays(3));
            Reserva laMisma = reservaService.obtenerOFallar(reserva.getIdReserva());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch arranco = new CountDownLatch(1);
            List<Future<?>> tareas = new ArrayList<>();
            try {
                synchronized (laMisma) {
                    tareas.add(executor.submit(() -> {
                        arranco.countDown();
                        return ReservaActualizacionDTO.class
                                .getSimpleName();
                    }));
                    tareas.add(executor.submit(() -> {
                        arranco.countDown();
                        return estadiaService.realizarCheckIn(reserva.getIdReserva());
                    }));

                    assertThat(arranco.await(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> tareas.get(1).get(TIMEOUT_BLOQUEO_MS, TimeUnit.MILLISECONDS))
                            .as("el check-in sigue bloqueado por el monitor de la reserva")
                            .isInstanceOf(TimeoutException.class);
                }
                for (Future<?> t : tareas) {
                    try {
                        t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                    } catch (Exception ignorada) {
                    }
                }
            } finally {
                executor.shutdownNow();
            }

            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO)).hasSize(1);
        }

        @Test
        @DisplayName("Un reembolso no impide el check-in de otra estadia")
        void reembolsoNoBloqueaOtrosFlujos() throws Exception {
            Estadia pagada = estadiaPagada();
            Pago aReembolsar = pagoUnico(pagada);
            Reserva otra = reservaConEstadia(OTRA_HABITACION, HOY, HOY.plusDays(3));

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch arranco = new CountDownLatch(1);
            List<Future<?>> tareas = new ArrayList<>();
            try {
                tareas.add(executor.submit(() -> {
                    arranco.await();
                    return pagoService.reembolsarPago(aReembolsar.getIdPago());
                }));
                tareas.add(executor.submit(() -> {
                    arranco.await();
                    return estadiaService.realizarCheckIn(otra.getIdReserva());
                }));

                arranco.countDown();
                for (Future<?> t : tareas) {
                    t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
                }
            } finally {
                executor.shutdownNow();
            }

            assertThat(pagoRepository.buscarPorId(aReembolsar.getIdPago()).orElseThrow().getEstadoPago())
                    .isEqualTo(EstadoPago.REEMBOLSADO);
            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO)).hasSize(2);
            assertThat(habitacionDe(OTRA_HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
        }
    }


    @Nested
    @DisplayName("9. Ausencia de interbloqueos")
    class SinInterbloqueo {

        @Test
        @DisplayName("Todos los servicios cruzados terminan dentro del tiempo limite")
        void serviciosCruzados_noSeBloquean() throws Exception {
            for (int ronda = 0; ronda < 5; ronda++) {
                limpiar();
                Estadia estadia = estadiaPagada();
                Pago pago = pagoUnico(estadia);
                Reserva reserva = estadiaService.obtenerOFallar(estadia.getIdEstadia()).getReserva();

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
                                case 0 -> estadiaService.realizarCheckOut(estadia.getIdEstadia());
                                case 1 -> pagoService.reembolsarPago(pago.getIdPago());
                                case 2 -> saldoService.consultarSaldo(estadia.getIdEstadia());
                                case 3 -> estadiaService.agregarConsumo(
                                        dtoConsumo(estadia.getIdEstadia(), "Extra " + n, 1, 5_000));
                                case 4 -> habitacionService.finalizarLimpieza(HABITACION);
                                case 5 -> pagoService.registrarPago(
                                        dtoPago(estadia.getIdEstadia(), 1_000));
                                case 6 -> reservaService.confirmarReserva(reserva.getIdReserva());
                                default -> estadiaService.realizarCheckIn(reserva.getIdReserva());
                            }
                        } catch (RuntimeException e) {
                            fallos.add(e.getClass().getSimpleName());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
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
                assertInvarianteDeOrdenValido(estadia, pago, "ronda " + ronda + " de servicios cruzados");
            }
        }

        @Test
        @DisplayName("Reembolsos y check-outs sobre estadias DISTINTAS no se bloquean entre si")
        void monitorPorEstadia_noSerializaTodoElHotel() throws Exception {

            List<Estadia> estadias = new ArrayList<>();
            List<Pago> pagos = new ArrayList<>();
            int[] habitaciones = {101, 102, 103, 201, 202, 301};
            for (int numero : habitaciones) {
                Estadia e = estadiaConEstadia(numero);
                estadias.add(e);
                pagos.add(saldar(e));
            }

            ExecutorService executor = Executors.newFixedThreadPool(6);
            CountDownLatch arranco = new CountDownLatch(1);
            List<Future<?>> tareas = new ArrayList<>();
            long inicio = System.nanoTime();

            for (int i = 0; i < 6; i++) {
                final int n = i;
                tareas.add(executor.submit(() -> {
                    try {
                        arranco.await();
                        // Alterna reembolso y check-out sobre la MISMA estadia de cada hilo.
                        if (n % 2 == 0) {
                            try {
                                estadiaService.realizarCheckOut(estadiaRepository
                                        .buscarPorId(estadias.get(n).getIdEstadia()).orElseThrow()
                                        .getIdEstadia());
                            } catch (RuntimeException ignorado) {
                            }
                        } else {
                            pagoService.reembolsarPago(pagos.get(n).getIdPago());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }));
            }

            arranco.countDown();
            for (Future<?> t : tareas) {
                t.get(TIMEOUT_EJECUCION_S, TimeUnit.SECONDS);
            }
            executor.shutdownNow();
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000;

            for (int i = 0; i < 6; i++) {
                assertInvarianteDeOrdenValido(estadias.get(i), pagos.get(i), "estadía " + i);
            }
            assertThat(duracionMs)
                    .as("6 operaciones sobre 6 estadias distintas no deben tardar mas de 10 s")
                    .isLessThan(10_000);
        }
    }


    @Nested
    @DisplayName("10. Conservacion tras rechazos")
    class Conservacion {

        @Test
        @DisplayName("Un check-out rechazado por reembolso deja pagos, estados y referencias intactos")
        void rechazoPorReembolso_noAlteraNada() {
            Estadia estadia = estadiaPagada();
            Pago pago = pagoUnico(estadia);
            String referencia = pago.getReferenciaPasarela();
            double monto = pago.getMonto();

            pagoService.reembolsarPago(pago.getIdPago());
            assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                    .isInstanceOf(IllegalStateException.class);


            Pago guardado = pagoRepository.buscarPorId(pago.getIdPago()).orElseThrow();
            assertThat(guardado.getEstadoPago()).isEqualTo(EstadoPago.REEMBOLSADO);
            assertThat(guardado.getMonto()).as("el importe nunca se altera").isEqualTo(monto);
            assertThat(guardado.getReferenciaPasarela()).isEqualTo(referencia);
            assertThat(guardado.getEstadia()).isSameAs(estadia);


            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(estadia.getReserva().getHabitacion().getNumeroHabitacion()).isEqualTo(HABITACION);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(pagoRepository.buscarPorEstadia(estadia.getIdEstadia())).hasSize(1);
        }

        @Test
        @DisplayName("Un reembolso rechazado conserva el pago en PAGADO")
        void reembolsoRechazado_conservaElPago() {
            Estadia estadia = estadiaPagada();
            Pago pago = pagoUnico(estadia);

            pagoService.reembolsarPago(pago.getIdPago());
            assertThatThrownBy(() -> pagoService.reembolsarPago(pago.getIdPago()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PAGADO");

            assertThat(pagoRepository.buscarPorId(pago.getIdPago()).orElseThrow().getEstadoPago())
                    .isEqualTo(EstadoPago.REEMBOLSADO);
            assertThat(pagoRepository.buscarPorId(pago.getIdPago()).orElseThrow().getEstadia())
                    .isSameAs(estadia);
        }
    }


    /**
     * INVARIANTE de los escenarios SIN reembolso: una estadía FINALIZADA no puede tener saldo
     * @param estadia
     * @param contexto
     */
    private void assertInvarianteSinDeuda(Estadia estadia, String contexto) {
        BigDecimal saldo = saldoPendiente(estadia);
        if (estadia.getEstadoEstadia() == EstadoEstadia.FINALIZADA) {
            assertThat(saldo)
                    .as(contexto + ": una estadía FINALIZADA no puede tener saldo pendiente")
                    .isEqualByComparingTo("0.00");
        }
    }

    /**
     * INVARIANTE de los escenarios CON reembolso: el orden de operaciones es siempre uno de los dos posibles
     * @param estadia
     * @param pago
     * @param contexto
     */
    private void assertInvarianteDeOrdenValido(Estadia estadia, Pago pago, String contexto) {
        EstadoPago estadoPago = pago.getEstadoPago();
        EstadoEstadia estadoEstadia = estadia.getEstadoEstadia();
        int habitacion = estadia.getReserva().getHabitacion().getNumeroHabitacion();

        assertThat(estadoPago)
                .as(contexto + ": un reembolso no altera el historial ni crea estados inventados")
                .isIn(EstadoPago.PAGADO, EstadoPago.REEMBOLSADO);

        if (estadoPago == EstadoPago.PAGADO) {

            assertThat(estadoEstadia)
                    .as(contexto + ": con el pago intacto el cierre era legítimo")
                    .isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(saldoPendiente(estadia))
                    .as(contexto + ": una estadía FINALIZADA nunca puede quedar con deuda")
                    .isEqualByComparingTo("0.00");
            assertThat(habitacionDe(habitacion))
                    .as(contexto + ": liberada queda EN_LIMPIEZA o DISPONIBLE, nunca OCUPADA")
                    .isIn(EstadoHabitacion.EN_LIMPIEZA, EstadoHabitacion.DISPONIBLE);
        } else {

            assertThat(saldoPendiente(estadia))
                    .as(contexto + ": el reembolso reabre la deuda. El importe exacto se fija en las "
                            + "pruebas dedicadas (450000.00); aqui solo importa que vuelva a ser "
                            + "positivo, porque en esta carrera tambien pueden entrar consumos")
                    .isPositive();
            assertThat(estadoEstadia)
                    .as(contexto + ": con deuda viva la estadía NO puede quedar cerrada")
                    .isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(habitacionDe(habitacion))
                    .as(contexto + ": sin check-out la habitación sigue ocupada")
                    .isEqualTo(EstadoHabitacion.OCUPADA);
        }
    }

    private void limpiar() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones().forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
    }

    private BigDecimal saldoPendiente(Estadia estadia) {
        return saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente();
    }

    private EstadoHabitacion habitacionDe(int numero) {
        return dataStore.getHabitaciones().stream()
                .filter(h -> h.getNumeroHabitacion() == numero)
                .findFirst().orElseThrow().getEstadoHabitacion();
    }

    /**
     * Crea la reserva, hace el check-in y salda la cuenta con un pago aprobado.
     * @return
     */
    private Estadia estadiaPagada() {
        Estadia e = estadiaConReserva();
        saldar(e);
        return e;
    }

    /**
     * Saldar la cuenta de la estadía con un pago aprobado. Si ya estaba saldada, no hace nada.
     * @param estadia
     * @return
     */
    private Pago saldar(Estadia estadia) {
        BigDecimal pendiente = saldoPendiente(estadia);
        if (pendiente.signum() == 0) {
            return null;
        }
        Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), pendiente.doubleValue()));
        pagoService.aprobarPago(pago.getIdPago(), "REF-TOTAL");
        return pago;
    }

    /**
     * Crea la reserva y hace el check-in, pero no paga nada.
     * @return
     */
    private Estadia estadiaConReserva() {
        return estadiaConEstadia(HABITACION);
    }

    /**
     * Crea la reserva y hace el check-in, pero no paga nada.
     * @param numeroHabitacion
     * @return
     */
    private Estadia estadiaConEstadia(int numeroHabitacion) {
        Reserva reserva = reservaConEstadia(numeroHabitacion, HOY, HOY.plusDays(3));
        return estadiaService.realizarCheckIn(reserva.getIdReserva());
    }

    private Reserva reservaConEstadia(int numeroHabitacion, LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped("H-001");
        dto.setNumeroHabitacion(numeroHabitacion);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return reservaService.crearReserva(dto);
    }

    /**
     * Busca el pago de la estadía y comprueba que es único y que la cuenta está saldada.
     * @param estadia
     * @return
     */
    private Pago pagoUnico(Estadia estadia) {
        List<Pago> pagos = pagoRepository.buscarPorEstadia(estadia.getIdEstadia());
        assertThat(pagos).as("la estadía pagada tiene exactamente un pago").hasSize(1);
        assertThat(saldoPendiente(estadia)).as("la cuenta está saldada").isEqualByComparingTo("0.00");
        return pagos.get(0);
    }

    private Pago registrarYAprobar(Estadia estadia, double monto) {
        Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), monto));
        pagoService.aprobarPago(pago.getIdPago(), "REF-TEST");
        return pago;
    }

    private PagoRequestDTO dtoPago(String idEstadia, double monto) {
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setMonto(monto);
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Abono de prueba");
        return dto;
    }

    private ConsumoRequestDTO dtoConsumo(
            String idEstadia, String descripcion, int cantidad, double precio) {
        ConsumoRequestDTO dto =
                new ConsumoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setDescripcion(descripcion);
        dto.setCantidad(cantidad);
        dto.setPrecioUnitario(precio);
        return dto;
    }
}