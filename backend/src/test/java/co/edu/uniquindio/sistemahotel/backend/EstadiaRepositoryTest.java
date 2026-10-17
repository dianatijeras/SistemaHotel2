package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.*;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.*;
import co.edu.uniquindio.sistemahotel.backend.repository.*;
import co.edu.uniquindio.sistemahotel.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class EstadiaRepositoryTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final int HABITACION = 101;
    private static final int OTRA_HABITACION = 102;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private EstadiaRepository estadiaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private HuespedRepository huespedRepository;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private ReservaService reservaService;

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
    @DisplayName("1-2. Contrato, guardar, buscar y listar")
    class ContratoYConsultas {

        @Test
        @DisplayName("El servicio declara una dependencia de la abstraccion, no de la implementacion")
        void servicio_dependeDeLaAbstraccion() {
            var constructor = EstadiaService.class.getDeclaredConstructors()[0];
            List<Class<?>> parametros = Arrays.stream(constructor.getParameterTypes()).toList();

            assertThat(parametros)
                    .contains(EstadiaRepository.class)
                    .doesNotContain(InMemoryEstadiaRepository.class);
        }

        @Test
        @DisplayName("La interfaz no hereda de ninguna clase base de repositorios genericos")
        void interfaz_noHeredadeRepositorioGenerico() {
            assertThat(EstadiaRepository.class.getInterfaces()).isEmpty();
        }

        @Test
        @DisplayName("El contrato expone exactamente las cinco operaciones con consumidor real")
        void contrato_noTieneMetodosEspeculativos() {
            List<String> metodos = Arrays.stream(EstadiaRepository.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName)
                    .sorted()
                    .toList();

            assertThat(metodos).containsExactly(
                    "actualizar", "buscarPorEstado", "buscarPorId", "buscarTodas", "guardar");
        }

        @Test
        @DisplayName("No declara eliminar: una estadia se cierra, nunca se borra")
        void contrato_noTieneBorrado() {
            List<String> metodos = Arrays.stream(EstadiaRepository.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName).toList();

            assertThat(metodos)
                    .as("el historial de estadias es un requisito del sistema")
                    .doesNotContain("eliminar", "delete", "eliminarPorId", "borrar");
        }

        @Test
        @DisplayName("guardar persiste y buscarPorId devuelve la misma instancia")
        void guardarYBuscar() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            Optional<Estadia> encontrada = estadiaRepository.buscarPorId(estadia.getIdEstadia());

            assertThat(encontrada).get().isSameAs(estadia);
            assertThat(estadiaRepository.buscarPorId("EST-NOEXISTE")).isEmpty();
        }

        @Test
        @DisplayName("buscarPorId de una estadia inexistente conserva el mensaje de siempre")
        void obtenerOFallar_mensaje() {
            assertThatThrownBy(() -> estadiaService.obtenerOFallar("EST-NOEXISTE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe la estadía con ID: EST-NOEXISTE");
        }

        @Test
        @DisplayName("buscarTodas devuelve el inventario completo y en orden de insercion")
        void buscarTodas_conservaElOrden() {
            Reserva r1 = reserva(HOY, HOY.plusDays(2), HABITACION);
            Reserva r2 = reserva(HOY, HOY.plusDays(3), OTRA_HABITACION);
            // Se cierra la primera para que exista una FINALIZADA antes de la segunda EN_CURSO.
            Estadia e1 = estadiaService.realizarCheckIn(r1.getIdReserva());
            pagarTotal(e1);
            estadiaService.realizarCheckOut(e1.getIdEstadia());
            habitacionService.finalizarLimpieza(HABITACION);
            Estadia e2 = estadiaService.realizarCheckIn(r2.getIdReserva());

            List<Estadia> todas = estadiaRepository.buscarTodas();

            assertThat(todas).hasSize(2);
            assertThat(todas).extracting(Estadia::getIdEstadia)
                    .containsExactly(e1.getIdEstadia(), e2.getIdEstadia());

            // El filtro de listarParaPagos debe conservar ese mismo orden de insercion.
            assertThat(estadiaService.listarParaPagos())
                    .extracting(Estadia::getIdEstadia)
                    .containsExactly(e1.getIdEstadia(), e2.getIdEstadia());
        }

        @Test
        @DisplayName("buscarPorEstado filtra por atributo de la propia estadia")
        void buscarPorEstado() {
            Reserva r1 = reserva(HOY, HOY.plusDays(2), HABITACION);
            Estadia e1 = estadiaService.realizarCheckIn(r1.getIdReserva());
            pagarTotal(e1);
            estadiaService.realizarCheckOut(e1.getIdEstadia());
            habitacionService.finalizarLimpieza(HABITACION);
            Reserva r2 = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia e2 = estadiaService.realizarCheckIn(r2.getIdReserva());

            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO))
                    .extracting(Estadia::getIdEstadia).containsExactly(e2.getIdEstadia());
            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.FINALIZADA))
                    .extracting(Estadia::getIdEstadia).containsExactly(e1.getIdEstadia());
            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.PROGRAMADA)).isEmpty();
            assertThat(estadiaService.listarEnCurso())
                    .extracting(Estadia::getIdEstadia).containsExactly(e2.getIdEstadia());
        }

        @Test
        @DisplayName("actualizar conserva la identidad y la posicion; una inexistente falla")
        void actualizar() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            int posicion = dataStore.getEstadias().indexOf(estadia);

            Estadia devuelta = estadiaRepository.actualizar(estadia);

            assertThat(devuelta).as("misma instancia").isSameAs(estadia);
            assertThat(dataStore.getEstadias().indexOf(devuelta)).isEqualTo(posicion);
            assertThat(dataStore.getEstadias()).hasSize(1);

            Estadia fantasma = new Estadia("EST-FANTASMA",
                    reservaRepository.buscarPorId(reserva.getIdReserva()).orElseThrow());
            assertThatThrownBy(() -> estadiaRepository.actualizar(fantasma))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("EST-FANTASMA");
        }
    }


    @Nested
    @DisplayName("3-4. DataStore compartido e identidad de instancias")
    class DataStoreEIdentidad {

        @Test
        @DisplayName("La estadia creada por el check-in queda en el DataStore compartido")
        void escritura_visibleEnElDataStore() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThat(dataStore.getEstadias())
                    .as("no hay una segunda lista de estadias")
                    .contains(estadia);
            assertThat(estadiaRepository.buscarPorId(estadia.getIdEstadia())).get().isSameAs(estadia);
            assertThat(dataStore.getEstadias()).hasSameSizeAs(estadiaRepository.buscarTodas());

            dataStore.getEstadias().clear();
            assertThat(estadiaRepository.buscarTodas()).isEmpty();
        }

        @Test
        @DisplayName("La estadia conserva las mismas instancias de Reserva, Habitacion y Huesped")
        void identidadCompartida() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThat(estadia.getReserva())
                    .as("misma reserva, no una copia")
                    .isSameAs(reservaRepository.buscarPorId(reserva.getIdReserva()).orElseThrow());
            assertThat(estadia.getReserva().getHabitacion())
                    .isSameAs(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow());
            assertThat(estadia.getReserva().getHuesped())
                    .isSameAs(huespedRepository.buscarPorId("H-001").orElseThrow());


            habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.MANTENIMIENTO);
            assertThat(estadia.getReserva().getHabitacion().getEstadoHabitacion())
                    .as("el cambio se ve en la estadia sin resincronizar nada")
                    .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("SaldoService y PagoService ven la MISMA instancia que el repositorio")
        void losServiciosCompartenLaInstancia() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 50_000));

            assertThat(pago.getEstadia())
                    .as("el pago referencia la misma estadia viva")
                    .isSameAs(estadia);
            assertThat(saldoService.pagosDe(estadia.getIdEstadia())).hasSize(1);

            // Un consumo agregado se refleja de inmediato en el saldo calculado por SaldoService.
            int consumosAntes = estadia.getConsumos().size();
            estadiaService.agregarConsumo(dtoConsumo(estadia.getIdEstadia(), "Minibar", 1, 20_000));

            assertThat(estadia.getConsumos()).hasSize(consumosAntes + 1);
            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral())
                    .isEqualByComparingTo(BigDecimal.valueOf(470_000L));
        }
    }



    @Nested
    @DisplayName("5. Implementacion alternativa del repositorio")
    class RepositorioAlternativo {

        @Test
        @DisplayName("EstadiaService funciona con un repositorio distinto del DataStore")
        void servicio_conRepositorioAlternativo() {
            RepositorioDeEstadiasEnMemoria alterno = new RepositorioDeEstadiasEnMemoria();
            EstadiaService servicio = new EstadiaService(alterno, reservaService, saldoService);

            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = servicio.realizarCheckIn(reserva.getIdReserva());

            assertThat(alterno.buscarPorId(estadia.getIdEstadia())).get().isSameAs(estadia);
            assertThat(alterno.contar()).isEqualTo(1);
            assertThat(dataStore.getEstadias())
                    .as("el servicio escribe a traves del repositorio inyectado")
                    .isEmpty();
            assertThat(servicio.listarTodas()).hasSize(1);
            assertThat(servicio.listarEnCurso()).hasSize(1);
            assertThat(servicio.buscarPorId(estadia.getIdEstadia())).get().isSameAs(estadia);
        }

        @Test
        @DisplayName("Las REGLAS siguen aplicando aunque el repositorio sea otro")
        void reglasDeNegocio_conRepositorioAlterno() {
            RepositorioDeEstadiasEnMemoria alterno = new RepositorioDeEstadiasEnMemoria();
            EstadiaService servicio = new EstadiaService(alterno, reservaService, saldoService);

            Reserva reserva1 = reserva(HOY, HOY.plusDays(3), HABITACION);
            servicio.realizarCheckIn(reserva1.getIdReserva());


            Reserva reserva2 = reserva(HOY, HOY.plusDays(3), OTRA_HABITACION);
            reserva2.setHabitacion(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow());
            habitacionService.buscarPorNumero(HABITACION).orElseThrow()
                    .cambiarEstado(EstadoHabitacion.DISPONIBLE);
            assertThatThrownBy(() -> servicio.realizarCheckIn(reserva2.getIdReserva()))
                    .as("la regla de habitacion ocupada sigue en el servicio, no en el repositorio")
                    .isInstanceOf(IllegalStateException.class);
            assertThat(alterno.contar()).isEqualTo(1);
        }

        @Test
        @DisplayName("DATO NEGATIVO: con un SaldoService cableado a otra persistencia no encuentra la estadia")
        void checkOut_conPersistenciaDistintaExponeLaReglaDeUnaSolaFuente() {

            RepositorioDeEstadiasEnMemoria alterno = new RepositorioDeEstadiasEnMemoria();
            EstadiaService servicio = new EstadiaService(alterno, reservaService, saldoService);

            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = servicio.realizarCheckIn(reserva.getIdReserva());

            assertThat(alterno.contar()).isEqualTo(1);
            assertThat(dataStore.getEstadias())
                    .as("la estadia solo existe en el repositorio alterno")
                    .isEmpty();

            assertThatThrownBy(() -> servicio.realizarCheckOut(estadia.getIdEstadia()))
                    .as("el SaldoService del contenedor busca en el repositorio real y no la encuentra")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe la estadía");

            assertThat(estadia.getEstadoEstadia())
                    .as("y nada se modifica: el fallo ocurre en la validacion")
                    .isEqualTo(EstadoEstadia.EN_CURSO);
        }
    }



    @Nested
    @DisplayName("6-7. Check-in correcto y check-in rechazado")
    class CheckIn {

        @Test
        @DisplayName("El check-in crea la estadia, ocupa la habitacion y marca la reserva")
        void checkIn_correcto() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);

            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(estadia.getIdEstadia()).startsWith("EST-");
            assertThat(estadia.getFechaCheckIn()).isNotNull();
            assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CHECKED_IN);
            assertThat(reserva.getFechaEstadia()).isNotNull();
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO))
                    .extracting(Estadia::getIdEstadia).containsExactly(estadia.getIdEstadia());
        }

        @Test
        @DisplayName("Un check-in repetido sobre la misma reserva se rechaza sin duplicar estadias")
        void checkInRepetido() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia primera = estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(estadiaRepository.buscarTodas()).hasSize(1);
            assertThat(estadiaRepository.buscarPorId(primera.getIdEstadia())).isPresent();
        }

        @Test
        @DisplayName("Una reserva cancelada no puede ingresar a check-in")
        void checkIn_deReservaCancelada() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            reservaService.cancelarReserva(reserva.getIdReserva());

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(estadiaRepository.buscarTodas())
                    .as("ninguna estadia parcial")
                    .isEmpty();
            assertThat(habitacionDe(HABITACION))
                    .as("la habitacion nunca se ocupa")
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("Una habitacion EN_LIMPIEZA no admite check-in")
        void checkIn_habitacionEnLimpieza() {

            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            habitacionService.marcarEnLimpieza(HABITACION);

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("EN_LIMPIEZA");
            assertThat(estadiaRepository.buscarTodas()).isEmpty();
        }

        @Test
        @DisplayName("El check-in del dia de salida se rechaza: la salida es exclusiva")
        void checkIn_enElDiaDeSalida() {

            Reserva reserva = reservaRepository.guardar(new Reserva(
                    "RES-2020-000001", HOY.minusDays(2), HOY, 2, 0,
                    huespedRepository.buscarPorId("H-001").orElseThrow(),
                    habitacionRepository.buscarPorNumero(HABITACION).orElseThrow()));

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("día de salida");
            assertThat(estadiaRepository.buscarTodas()).isEmpty();
        }

        @Test
        @DisplayName("Una reserva sin documento de identidad no puede hacer check-in")
        void checkIn_sinDocumento() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            reserva.getHuesped().setDocumento("  ");

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("documento de identidad");
            assertThat(estadiaRepository.buscarTodas()).isEmpty();
        }

        @Test
        @DisplayName("Un check-in fallido no deja habitacion ocupada ni reserva modificada")
        void checkInFallido_sinEstadoParcial() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            habitacionService.marcarFueraDeServicio(HABITACION);

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(estadiaRepository.buscarTodas()).isEmpty();
            assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);
            assertThat(reserva.getFechaEstadia()).isNull();
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
        }
    }



    @Nested
    @DisplayName("8-9. Concurrencia entre modificacion de reserva y check-in")
    class Concurrencia {

        @Test
        @DisplayName("La modificacion y el check-in no se intercalan: ambos quedan bloqueados por el monitor")
        void modificacionYCheckIn_noSeIntercalan() throws Exception {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Reserva laMisma = reservaService.obtenerOFallar(reserva.getIdReserva());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch arranco = new CountDownLatch(2);
            try {
                java.util.List<Future<?>> tareas = new java.util.ArrayList<>();
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

                    assertThat(arranco.await(20, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> tareas.get(0).get(400, TimeUnit.MILLISECONDS))
                            .isInstanceOf(TimeoutException.class);
                    assertThatThrownBy(() -> tareas.get(1).get(400, TimeUnit.MILLISECONDS))
                            .isInstanceOf(TimeoutException.class);
                    assertThat(reserva.getHabitacion().getNumeroHabitacion()).isEqualTo(HABITACION);
                }

                for (Future<?> t : tareas) {
                    try {
                        t.get(20, TimeUnit.SECONDS);
                    } catch (Exception ignorada) {
                        // Una de las dos puede rechazar a la otra: ambos son resultados validos.
                    }
                }

                if (reserva.getEstadoReserva() == EstadoReserva.CHECKED_IN) {
                    int habitacionDeLaReserva = reserva.getHabitacion().getNumeroHabitacion();
                    assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO))
                            .hasSize(1);
                    assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO).get(0)
                            .getReserva().getHabitacion().getNumeroHabitacion())
                            .isEqualTo(habitacionDeLaReserva);
                    assertThat(habitacionDe(habitacionDeLaReserva))
                            .isEqualTo(EstadoHabitacion.OCUPADA);
                } else {
                    assertThat(estadiaRepository.buscarTodas()).isEmpty();
                    assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);
                    assertThat(habitacionDe(OTRA_HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);
                }
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("El monitor de la reserva sigue presente tras migrar al repositorio")
        void elMonitorDeLaReserva_sigueVivo() throws Exception {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Reserva laMisma = reservaService.obtenerOFallar(reserva.getIdReserva());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {

                Future<?> f;
                synchronized (laMisma) {
                    f = executor.submit(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()));
                    assertThatThrownBy(() -> f.get(400, TimeUnit.MILLISECONDS))
                            .as("el check-in respeta el monitor de la entidad reserva")
                            .isInstanceOf(TimeoutException.class);
                }
                f.get(20, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }

            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO)).hasSize(1);
        }

        @Test
        @DisplayName("Dos check-ins incompatibles simultaneos dejan una sola estadia")
        void dosCheckInIncompatibles_soloUnaEstadia() throws Exception {
            Reserva primera = reserva(HOY, HOY.plusDays(2), HABITACION);
            Reserva segunda = reserva(HOY.plusDays(2), HOY.plusDays(4), HABITACION);

            int peticiones = 8;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            AtomicInteger exitosos = new AtomicInteger();
            List<Future<?>> tareas = new java.util.ArrayList<>();

            for (int i = 0; i < peticiones; i++) {
                Reserva objetivo = (i % 2 == 0) ? primera : segunda;
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        estadiaService.realizarCheckIn(objetivo.getIdReserva());
                        exitosos.incrementAndGet();
                    } catch (IllegalStateException | IllegalArgumentException ignorada) {
                        // Rechazado por una regla: resultado esperado.
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }));
            }

            arranque.countDown();
            for (Future<?> t : tareas) {
                t.get(20, TimeUnit.SECONDS);
            }
            executor.shutdownNow();

            assertThat(exitosos.get()).isEqualTo(1);
            assertThat(estadiaRepository.buscarPorEstado(EstadoEstadia.EN_CURSO)).hasSize(1);
            assertThat(estadiaRepository.buscarTodas()).hasSize(1);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("Servicios de reservas, estadias, pagos y habitaciones no se bloquean entre si")
        void sinInterbloqueo() throws Exception {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            int peticiones = 12;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            List<Future<?>> tareas = new java.util.ArrayList<>();
            List<String> fallos = new CopyOnWriteArrayList<>();

            for (int i = 0; i < peticiones; i++) {
                final int n = i;
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        switch (n % 5) {
                            case 0 -> estadiaService.agregarConsumo(
                                    dtoConsumo(estadia.getIdEstadia(), "Item " + n, 1, 5_000));
                            case 1 -> saldoService.consultarSaldo(estadia.getIdEstadia());
                            case 2 -> pagoService.registrarPago(
                                    dtoPago(estadia.getIdEstadia(), 1_000));
                            case 3 -> estadiaService.realizarCheckIn(reserva.getIdReserva());
                            default -> reservaService.confirmarReserva(reserva.getIdReserva());
                        }
                    } catch (RuntimeException e) {
                        fallos.add(e.getClass().getSimpleName());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }));
            }

            arranque.countDown();
            // Si hubiera un ciclo, get() lanzaria TimeoutException.
            for (Future<?> t : tareas) {
                t.get(20, TimeUnit.SECONDS);
            }
            executor.shutdownNow();

            assertThat(fallos).allMatch(n -> n.equals("IllegalStateException")
                    || n.equals("IllegalArgumentException"));
            assertThat(pago.getIdPago()).startsWith("PAG-");
        }
    }



    @Nested
    @DisplayName("10-16. Consumos, costos, pagos y check-out")
    class ConsumosPagosYCheckOut {

        @Test
        @DisplayName("Los consumos se registran y se reflejan en el desglose de costos")
        void consumosYCosto() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            agregarConsumo(estadia, "Room service", 2, 45_000);
            agregarConsumo(estadia, "Minibar", 1, 20_000);

            EstadiaCostoDTO costo = estadiaService.calcularCosto(estadia.getIdEstadia());

            assertThat(costo.getNoches()).as("salida exclusiva: 3 noches").isEqualTo(3L);
            assertThat(costo.getSubtotalAlojamiento()).isEqualByComparingTo("450000.00");
            assertThat(costo.getSubtotalConsumos()).isEqualByComparingTo("110000.00");
            assertThat(costo.getTotalGeneral()).isEqualByComparingTo("560000.00");
            assertThat(costo.getConsumos()).hasSize(2);
            assertThat(estadia.getConsumos()).hasSize(2);
            assertThat(estadiaRepository.buscarPorId(estadia.getIdEstadia())).get()
                    .isSameAs(estadia)
                    .extracting(Estadia::getTotalFinal).isEqualTo(560_000.0);
        }

        @Test
        @DisplayName("No se admiten consumos despues de finalizar la estadia")
        void consumoDespuesDeFinalizar() {
            Reserva reserva = reserva(HOY, HOY.plusDays(2), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            agregarConsumo(estadia, "Antes de cerrar", 1, 30_000);
            pagarTotal(estadia);
            estadiaService.realizarCheckOut(estadia.getIdEstadia());

            int consumosAlCerrar = estadia.getConsumos().size();

            assertThatThrownBy(() -> estadiaService.agregarConsumo(
                    dtoConsumo(estadia.getIdEstadia(), "Tarde", 1, 30_000)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("EN_CURSO");

            assertThat(estadia.getConsumos())
                    .as("no se agrega el consumo rechazado")
                    .hasSize(consumosAlCerrar);
            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral())
                    .as("el total del historial no cambia")
                    .isEqualByComparingTo(BigDecimal.valueOf(330_000L));
        }

        @Test
        @DisplayName("Los pagos parciales dejan saldo pendiente y solo PAGADO lo reduce")
        void pagosParciales() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();

            Pago parcial = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                    .as("un pago PENDIENTE no descuenta")
                    .isEqualByComparingTo(total);

            pagoService.aprobarPago(parcial.getIdPago(), "REF-1");
            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                    .isEqualByComparingTo(BigDecimal.valueOf(350_000L));

            pagoService.aprobarPago(
                    pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 350_000)).getIdPago(),
                    "REF-2");
            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("El check-out se rechaza mientras quede saldo pendiente")
        void checkOutConDeuda() {
            Reserva reserva = reserva(HOY, HOY.plusDays(2), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("saldo pendiente");

            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CHECKED_IN);
            assertThat(habitacionDe(HABITACION))
                    .as("un check-out rechazado no toca la habitacion")
                    .isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("El check-out sin deuda finaliza y deja la habitacion EN_LIMPIEZA")
        void checkOutCorrecto() {
            Reserva reserva = reserva(HOY, HOY.plusDays(2), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            agregarConsumo(estadia, "Extra", 1, 10_000);
            pagarTotal(estadia);

            Estadia finalizada = estadiaService.realizarCheckOut(estadia.getIdEstadia());

            assertThat(finalizada.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(finalizada.getFechaCheckOut()).isNotNull();
            assertThat(finalizada.getTotalFinal()).isEqualTo(310_000.0);
            assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.FINALIZADA);
            assertThat(habitacionDe(HABITACION))
                    .as("la habitacion queda EN_LIMPIEZA, nunca DISPONIBLE sola")
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertThat(habitacionService.listarPendientesLimpieza())
                    .extracting(Habitacion::getNumeroHabitacion).contains(HABITACION);
        }

        @Test
        @DisplayName("El check-out duplicado se rechaza y no repite la transicion")
        void checkOutDuplicado() {
            Reserva reserva = reserva(HOY, HOY.plusDays(2), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            pagarTotal(estadia);
            estadiaService.realizarCheckOut(estadia.getIdEstadia());

            assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya fue finalizada");
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertThat(estadiaRepository.buscarTodas()).hasSize(1);
        }

        @Test
        @DisplayName("La habitacion solo se libera con la confirmacion explicita de limpieza")
        void liberacionExplicita() {
            Reserva reserva = reserva(HOY, HOY.plusDays(2), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            pagarTotal(estadia);
            estadiaService.realizarCheckOut(estadia.getIdEstadia());

            // Vía genérica de cambio de estado: debe rechazarse.
            assertThatThrownBy(() -> habitacionService.cambiarEstado(
                    HABITACION, EstadoHabitacion.DISPONIBLE))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

            // Confirmación explícita.
            Habitacion liberada = habitacionService.finalizarLimpieza(HABITACION);
            assertThat(liberada.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
            assertThat(habitacionService.listarPendientesLimpieza())
                    .extracting(Habitacion::getNumeroHabitacion).doesNotContain(HABITACION);
        }

        @Test
        @DisplayName("El historial completo se conserva tras el cierre")
        void historialConservado() {
            Reserva reserva = reserva(HOY, HOY.plusDays(2), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            agregarConsumo(estadia, "Minibar", 1, 15_000);
            pagarTotal(estadia);
            estadiaService.realizarCheckOut(estadia.getIdEstadia());
            habitacionService.finalizarLimpieza(HABITACION);

            assertThat(estadiaRepository.buscarTodas())
                    .as("la estadia finalizada no se borra")
                    .hasSize(1);
            assertThat(estadiaRepository.buscarPorId(estadia.getIdEstadia())).get()
                    .extracting(Estadia::getEstadoEstadia).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(estadia.getConsumos()).as("los consumos se conservan").hasSize(1);
            assertThat(estadiaService.listarParaPagos()).hasSize(1);
            assertThat(estadiaService.listarEnCurso()).isEmpty();
            assertThat(reservaRepository.buscarPorId(reserva.getIdReserva())).isPresent();
            assertThat(saldoService.pagosDe(estadia.getIdEstadia())).hasSize(1);
        }
    }



    @Nested
    @DisplayName("18. Compatibilidad con los otros repositorios")
    class Compatibilidad {

        @Test
        @DisplayName("Flujo completo huesped -> reserva -> check-in -> consumo -> pago -> check-out -> limpieza")
        void flujoCompleto() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThat(reserva.getHuesped())
                    .isSameAs(huespedRepository.buscarPorId("H-001").orElseThrow());
            assertThat(reserva.getHabitacion())
                    .isSameAs(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow());
            assertThat(estadia.getReserva()).isSameAs(reserva);

            agregarConsumo(estadia, "Cena", 1, 60_000);
            pagarTotal(estadia);

            estadiaService.realizarCheckOut(estadia.getIdEstadia());
            habitacionService.finalizarLimpieza(HABITACION);

            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow()
                    .getEstadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
            assertThat(huespedRepository.buscarPorId("H-001")).isPresent();
            assertThat(reservaRepository.buscarPorId(reserva.getIdReserva())).get()
                    .extracting(Reserva::getEstadoReserva).isEqualTo(EstadoReserva.FINALIZADA);
            assertThat(estadiaRepository.buscarTodas()).hasSize(1);
        }

        @Test
        @DisplayName("HabitacionService ve las estadias que escribe el repositorio")
        void habitacionService_veElRepositorioDeEstadias() {
            Reserva reserva = reserva(HOY, HOY.plusDays(3), HABITACION);
            habitacionService.marcarEnLimpieza(HABITACION);
            assertThat(habitacionService.listarPendientesLimpieza())
                    .extracting(Habitacion::getNumeroHabitacion).contains(HABITACION);
            assertThat(habitacionService.finalizarLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);

            Reserva r2 = reserva(HOY, HOY.plusDays(3), OTRA_HABITACION);
            // Se le asigna la 101 directamente para aislar la regla de habitacion ocupada.
            r2.setHabitacion(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow());
            Estadia estadia = estadiaService.realizarCheckIn(r2.getIdReserva());
            habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

            assertThatThrownBy(() -> habitacionService.finalizarLimpieza(HABITACION))
                    .as("HabitacionService sigue leyendo el DataStore y ve la estadia nueva")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("estadía activa");
        }
    }



    private EstadoHabitacion habitacionDe(int numero) {
        return dataStore.getHabitaciones().stream()
                .filter(h -> h.getNumeroHabitacion() == numero)
                .findFirst().orElseThrow()
                .getEstadoHabitacion();
    }

    private Reserva reserva(LocalDate inicio, LocalDate fin, int numero) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped("H-001");
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return reservaService.crearReserva(dto);
    }

    private ReservaActualizacionDTO dtoActualizacion(
            int numero, LocalDate inicio, LocalDate fin) {
        ReservaActualizacionDTO dto =
                new ReservaActualizacionDTO();
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

    private void agregarConsumo(Estadia estadia, String descripcion, int cantidad, double precio) {
        estadiaService.agregarConsumo(dtoConsumo(estadia.getIdEstadia(), descripcion, cantidad, precio));
    }

    private void pagarTotal(Estadia estadia) {
        BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();
        Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), total.doubleValue()));
        pagoService.aprobarPago(pago.getIdPago(), "REF-TEST");
    }

    /**
     * Repositorio de estadias alternativo para probar la inyección de dependencias y la independencia de reglas de negocio.
     */
    private static class RepositorioDeEstadiasEnMemoria implements EstadiaRepository {

        private final List<Estadia> inventario = new CopyOnWriteArrayList<>();

        int contar() {
            return inventario.size();
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
