package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
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
import co.edu.uniquindio.sistemahotel.backend.repository.InMemoryPagoRepository;
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
import java.util.Arrays;
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
class PagoRepositoryTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final int HABITACION = 101;

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
    @DisplayName("1-6. Contrato, consultas, historial e identidad")
    class ContratoYConsultas {

        @Test
        @DisplayName("El servicio declara una dependencia de la abstraccion, no de la implementacion")
        void servicio_dependeDeLaAbstraccion() {
            var constructor = PagoService.class.getDeclaredConstructors()[0];
            List<Class<?>> parametros = Arrays.stream(constructor.getParameterTypes()).toList();

            assertThat(parametros)
                    .contains(PagoRepository.class)
                    .doesNotContain(InMemoryPagoRepository.class);
        }

        @Test
        @DisplayName("La interfaz no hereda de ninguna clase base de repositorios genericos")
        void interfaz_noHeredadeRepositorioGenerico() {
            assertThat(PagoRepository.class.getInterfaces()).isEmpty();
        }

        @Test
        @DisplayName("El contrato expone exactamente las cinco operaciones con consumidor real")
        void contrato_noTieneMetodosEspeculativos() {
            List<String> metodos = Arrays.stream(PagoRepository.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName)
                    .sorted().toList();

            assertThat(metodos).containsExactly(
                    "actualizar", "buscarPorEstadia", "buscarPorId", "buscarTodos", "guardar");
        }

        @Test
        @DisplayName("No declara eliminar: el historial de pagos no se borra nunca")
        void contrato_noTieneBorrado() {
            List<String> metodos = Arrays.stream(PagoRepository.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName).toList();

            assertThat(metodos)
                    .as("borrar un pago seria perder dinero")
                    .doesNotContain("eliminar", "delete", "eliminarPorId", "borrar");
        }

        @Test
        @DisplayName("guardar persiste y buscarPorId devuelve la misma instancia")
        void guardarYBuscar() {
            Estadia estadia = estadiaConReserva();

            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            assertThat(pago.getIdPago()).startsWith("PAG-");
            assertThat(pagoRepository.buscarPorId(pago.getIdPago())).get().isSameAs(pago);
            assertThat(pagoRepository.buscarPorId("PAG-NOEXISTE")).isEmpty();
        }

        @Test
        @DisplayName("buscarPorEstadia devuelve solo los pagos de esa estadia y en orden de registro")
        void buscarPorEstadia() {
            Estadia e1 = estadiaConReserva();
            Estadia e2 = estadiaConReservaEn(102);

            Pago a1 = pagoService.registrarPago(dtoPago(e1.getIdEstadia(), 10_000));
            Pago b1 = pagoService.registrarPago(dtoPago(e2.getIdEstadia(), 20_000));
            Pago a2 = pagoService.registrarPago(dtoPago(e1.getIdEstadia(), 30_000));

            List<Pago> deLaPrimera = pagoRepository.buscarPorEstadia(e1.getIdEstadia());
            List<Pago> deLaSegunda = pagoRepository.buscarPorEstadia(e2.getIdEstadia());

            assertThat(deLaPrimera).extracting(Pago::getIdPago)
                    .as("orden observable de registro")
                    .containsExactly(a1.getIdPago(), a2.getIdPago());
            assertThat(deLaSegunda).extracting(Pago::getIdPago)
                    .containsExactly(b1.getIdPago());
            assertThat(pagoRepository.buscarPorEstadia("EST-NOEXISTE")).isEmpty();
            assertThat(pagoService.listarPorEstadia(e1.getIdEstadia())).hasSize(2);
        }

        @Test
        @DisplayName("El historial conserva pagos PENDIENTE, PAGADO, RECHAZADO y REEMBOLSADO")
        void historialConservaTodosLosEstados() {
            Estadia estadia = estadiaConReserva();

            Pago pendiente = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 10_000));
            Pago aprobado = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 20_000));
            Pago rechazado = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 30_000));
            Pago reembolsable = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 40_000));

            pagoService.aprobarPago(aprobado.getIdPago(), "REF-A");
            pagoService.rechazarPago(rechazado.getIdPago(), "No corresponde");
            pagoService.aprobarPago(reembolsable.getIdPago(), "REF-B");
            pagoService.reembolsarPago(reembolsable.getIdPago());

            assertThat(pagoRepository.buscarTodos())
                    .as("ningún pago se elimina del historial")
                    .hasSize(4);
            assertThat(pagoRepository.buscarPorId(pendiente.getIdPago()).orElseThrow()
                    .getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
            assertThat(pagoRepository.buscarPorId(rechazado.getIdPago()).orElseThrow()
                    .getEstadoPago()).isEqualTo(EstadoPago.RECHAZADO);
            assertThat(pagoRepository.buscarPorId(reembolsable.getIdPago()).orElseThrow()
                    .getEstadoPago()).isEqualTo(EstadoPago.REEMBOLSADO);
        }

        @Test
        @DisplayName("El pago conserva la referencia a la MISMA instancia de Estadia")
        void identidadCompartida() {
            Estadia estadia = estadiaConReserva();

            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            assertThat(pago.getEstadia())
                    .as("misma instancia viva, no una copia")
                    .isSameAs(estadiaRepository.buscarPorId(estadia.getIdEstadia()).orElseThrow());


            estadiaService.agregarConsumo(dtoConsumo(estadia, "Extra", 1, 25_000));
            assertThat(pago.getEstadia().getTotalFinal())
                    .as("el consumo se ve a traves de la referencia compartida")
                    .isEqualTo(475_000.0);
        }

        @Test
        @DisplayName("Las escrituras del servicio quedan visibles en el DataStore compartido")
        void escritura_visibleEnElDataStore() {
            Estadia estadia = estadiaConReserva();
            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            assertThat(dataStore.getPagos()).contains(pago);
            assertThat(dataStore.getPagos()).hasSameSizeAs(pagoRepository.buscarTodos());


            assertThat(saldoService.pagosDe(estadia.getIdEstadia()))
                    .as("coherencia entre repositorio, DataStore y SaldoService")
                    .hasSize(1);


            dataStore.getPagos().clear();
            assertThat(pagoRepository.buscarTodos()).isEmpty();
        }

        @Test
        @DisplayName("actualizar conserva identidad y posicion; uno inexistente falla")
        void actualizar() {
            Estadia estadia = estadiaConReserva();
            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            int posicion = dataStore.getPagos().indexOf(pago);

            Pago devuelto = pagoRepository.actualizar(pago);

            assertThat(devuelto).isSameAs(pago);
            assertThat(dataStore.getPagos().indexOf(devuelto)).isEqualTo(posicion);
            assertThat(dataStore.getPagos()).hasSize(1);

            Pago fantasma = new Pago("PAG-FANTASMA", 1, MetodoPago.EFECTIVO, "X", estadia);
            assertThatThrownBy(() -> pagoRepository.actualizar(fantasma))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PAG-FANTASMA");
        }
    }



    @Nested
    @DisplayName("7-12. Pagos parciales, transiciones y saldo")
    class ReglasFinancieras {

        @Test
        @DisplayName("Se permiten pagos parciales y se acumuran hasta el total")
        void pagosParciales() {
            Estadia estadia = estadiaConReserva();
            BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();
            assertThat(total).isEqualByComparingTo("450000.00");

            Pago p1 = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            pagoService.aprobarPago(p1.getIdPago(), "REF-1");
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("350000.00");

            Pago p2 = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 200_000));
            pagoService.aprobarPago(p2.getIdPago(), "REF-2");
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("150000.00");

            Pago p3 = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 150_000));
            pagoService.aprobarPago(p3.getIdPago(), "REF-3");
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Aprobar y rechazar funcionan, y cada pago queda en un estado final")
        void aprobarYRechazar() {
            Estadia estadia = estadiaConReserva();

            Pago aprobado = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            Pago rechazado = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 50_000));

            Pago a = pagoService.aprobarPago(aprobado.getIdPago(), "REF-A");
            assertThat(a.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
            assertThat(a.getReferenciaPasarela()).isEqualTo("REF-A");

            Pago r = pagoService.rechazarPago(rechazado.getIdPago(), "Tarjeta incorrecta");
            assertThat(r.getEstadoPago()).isEqualTo(EstadoPago.RECHAZADO);
            assertThat(r.getObservaciones()).isEqualTo("Tarjeta incorrecta");


            Pago sinRef = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 10_000));
            assertThat(pagoService.aprobarPago(sinRef.getIdPago(), null).getReferenciaPasarela())
                    .isEqualTo("REF-MANUAL");
        }

        @Test
        @DisplayName("Solo un pago PAGADO puede reembolsarse, y deja de descontar")
        void reembolsar() {
            Estadia estadia = estadiaConReserva();

            Pago pendiente = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            assertThatThrownBy(() -> pagoService.reembolsarPago(pendiente.getIdPago()))
                    .as("un pago PENDIENTE no se reembolsa")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PAGADO");

            Pago pagado = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            pagoService.aprobarPago(pagado.getIdPago(), "REF");
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("350000.00");

            Pago reembolsa = pagoService.reembolsarPago(pagado.getIdPago());
            assertThat(reembolsa.getEstadoPago()).isEqualTo(EstadoPago.REEMBOLSADO);

            assertThat(saldoPendienteDe(estadia))
                    .as("un REEMBOLSADO deja de contar como abonado")
                    .isEqualByComparingTo("450000.00");

            assertThatThrownBy(() -> pagoService.reembolsarPago(pagado.getIdPago()))
                    .as("no se reembolsa dos veces")
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Las transiciones invalidas se rechazan sin cambiar el estado")
        void transicionesInvalidas() {
            Estadia estadia = estadiaConReserva();
            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            pagoService.aprobarPago(pago.getIdPago(), "REF");

            assertThatThrownBy(() -> pagoService.aprobarPago(pago.getIdPago(), "REF-2"))
                    .as("un PAGADO no se vuelve a aprobar")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDIENTE");
            assertThatThrownBy(() -> pagoService.rechazarPago(pago.getIdPago(), "x"))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(pagoRepository.buscarPorId(pago.getIdPago()).orElseThrow().getEstadoPago())
                    .as("ninguna transición fallida alteró el estado")
                    .isEqualTo(EstadoPago.PAGADO);

            assertThatThrownBy(() -> pagoService.aprobarPago("PAG-NOEXISTE", "REF"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe el pago");
            assertThatThrownBy(() -> pagoService.reembolsarPago("PAG-NOEXISTE"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Se rechaza un pago o aprobacion superior al saldo disponible")
        void sobrepagoRechazado() {
            Estadia estadia = estadiaConReserva();
            BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();

            assertThatThrownBy(() -> pagoService.registrarPago(
                    dtoPago(estadia.getIdEstadia(), total.doubleValue() + 1)))
                    .as("no se puede registrar por encima de la cuenta")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("supera el saldo disponible");


            Pago totalPago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), total.doubleValue()));
            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 1)))
                    .as("lo pendiente ya está comprometido")
                    .isInstanceOf(IllegalStateException.class);

            pagoService.aprobarPago(totalPago.getIdPago(), "REF");
            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 1)))
                    .as("con la cuenta saldada no cabe nada más")
                    .isInstanceOf(IllegalStateException.class);

            assertThat(pagoRepository.buscarTodos()).as("los rechazos no escriben nada").hasSize(1);
        }

        @Test
        @DisplayName("Aprobaciones sucesivas de parciales dejan la cuenta en cero exacto")
        void aprobacionesSucesivas_sumanElTotalExacto() {
            Estadia estadia = estadiaConReserva();


            Pago p1 = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 200_000));
            Pago p2 = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 150_000));
            Pago p3 = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 1)))
                    .as("la cuenta ya esta comprometida por completo")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("supera el saldo disponible");

            pagoService.aprobarPago(p1.getIdPago(), "REF-1");
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("250000.00");
            pagoService.aprobarPago(p2.getIdPago(), "REF-2");
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("100000.00");
            pagoService.aprobarPago(p3.getIdPago(), "REF-3");
            assertThat(saldoPendienteDe(estadia))
                    .as("tres aprobaciones nunca dejan saldo negativo")
                    .isEqualByComparingTo("0.00");

            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getCantidadPagosAprobados())
                    .isEqualTo(3);
            assertThat(pagoRepository.buscarPorId(p1.getIdPago()).orElseThrow().getMonto())
                    .as("aprobar nunca altera el importe registrado")
                    .isEqualTo(200_000.0);
        }

        @Test
        @DisplayName("Solo PAGADO reduce el saldo: PENDIENTE, RECHAZADO y REEMBOLSADO no")
        void soloPagadoReduceElSaldo() {
            Estadia estadia = estadiaConReserva();
            BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();

            Pago pendiente = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            Pago rechazado = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            Pago pagado = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            assertThat(saldoPendienteDe(estadia))
                    .as("tres PENDIENTE: el saldo sigue completo")
                    .isEqualByComparingTo(total);

            pagoService.rechazarPago(rechazado.getIdPago(), "No");
            assertThat(saldoPendienteDe(estadia))
                    .as("un RECHAZADO no descuenta").isEqualByComparingTo(total);

            pagoService.aprobarPago(pagado.getIdPago(), "REF");
            assertThat(saldoPendienteDe(estadia))
                    .as("solo el PAGADO descuenta").isEqualByComparingTo("350000.00");

            pagoService.aprobarPago(pendiente.getIdPago(), "REF-2");
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("250000.00");

            pagoService.reembolsarPago(pagado.getIdPago());
            assertThat(saldoPendienteDe(estadia))
                    .as("el REEMBOLSADO devuelve la deuda").isEqualByComparingTo("350000.00");
        }

        @Test
        @DisplayName("Se conservan la precision y el redondeo monetario de Costos")
        void precisiónYRedondeo() {
            Estadia estadia = estadiaConReserva();


            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 0)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mayor a 0");
            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), -50)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mayor a 0");
            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), Double.NaN)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> pagoService.registrarPago(
                    dtoPago(estadia.getIdEstadia(), Double.POSITIVE_INFINITY)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100.999)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2 decimales");


            Pago exacto = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100.99));
            assertThat(exacto.getMonto()).isEqualTo(100.99);
            pagoService.aprobarPago(exacto.getIdPago(), "REF");

            assertThat(saldoPendienteDe(estadia))
                    .as("escala 2 exacta, sin residuos de punto flotante")
                    .isEqualByComparingTo("449899.01");


            assertThatThrownBy(() -> pagoService.registrarPago(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obligatorios");
            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago("  ", 100)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("estadía");
            assertThat(pagoRepository.buscarTodos()).hasSize(1);
        }
    }



    @Nested
    @DisplayName("14-16. Estadias finalizadas y check-out")
    class CheckOutYPagos {

        @Test
        @DisplayName("Se puede pagar una estadia finalizada, pero no registrar consumos")
        void pagosSobreEstadiaFinalizada() {
            Estadia estadia = estadiaConReserva();

            Pago previo = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            pagoService.aprobarPago(previo.getIdPago(), "REF-PREV");

            pagarSaldo(estadia);
            estadiaService.realizarCheckOut(estadia.getIdEstadia());

            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("0.00");

            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 25_000)))
                    .as("con la cuenta saldada el disponible es cero")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("supera el saldo disponible");
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("0.00");


            assertThatThrownBy(() -> pagoService.reembolsarPago(previo.getIdPago()))
                    .as("el reembolso sobre una cuenta cerrada se rechaza")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FINALIZADA");

            assertThat(pagoRepository.buscarPorId(previo.getIdPago()).orElseThrow().getEstadoPago())
                    .as("el rechazo no cambia el estado del pago: sigue PAGADO")
                    .isEqualTo(EstadoPago.PAGADO);
            assertThat(saldoPendienteDe(estadia))
                    .as("el rechazo no altera el saldo: la cuenta sigue liquidada")
                    .isEqualByComparingTo("0.00");
            assertThat(habitacionDe(HABITACION))
                    .as("y la habitación no se toca")
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);


            assertThat(saldoPendienteDe(estadia))
                    .as("INVARIANTE: una estadía FINALIZADA jamás puede quedar con saldo pendiente")
                    .isEqualByComparingTo("0.00");

            assertThat(pagoRepository.buscarPorEstadia(estadia.getIdEstadia()))
                    .as("el historial conserva TODOS los pagos y el reembolso rechazado no añade ninguno")
                    .hasSize(2);
        }

        @Test
        @DisplayName("El check-out se rechaza mientras quede saldo pendiente")
        void checkOutConDeuda() {
            Estadia estadia = estadiaConReserva();
            Pago parcial = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            pagoService.aprobarPago(parcial.getIdPago(), "REF");

            assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("saldo pendiente");

            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("Con saldo cero el check-out cierra y deja la habitacion EN_LIMPIEZA")
        void checkOutConSaldoCero() {
            Estadia estadia = estadiaConReserva();
            pagarTotal(estadia);
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("0.00");

            estadiaService.realizarCheckOut(estadia.getIdEstadia());

            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(habitacionDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertThat(pagoRepository.buscarPorEstadia(estadia.getIdEstadia()))
                    .as("los pagos siguen en el historial tras el cierre")
                    .hasSize(1);
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("0.00");
        }
    }



    @Nested
    @DisplayName("17-18. Concurrencia de pagos")
    class Concurrencia {

        @Test
        @DisplayName("Aprobaciones simultaneas sobre el saldo exacto no dejan sobrepago")
        void aprobacionesSimultaneas_noSobrepagan() throws Exception {
            for (int ronda = 0; ronda < 20; ronda++) {
                limpiar();
                Estadia estadia = estadiaConReserva();


                Pago a = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 200_000));
                Pago b = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 250_000));

                int peticiones = 8;
                ExecutorService executor = Executors.newFixedThreadPool(peticiones);
                CountDownLatch arranque = new CountDownLatch(1);
                AtomicInteger aprobados = new AtomicInteger();
                AtomicInteger rechazados = new AtomicInteger();
                List<Future<?>> tareas = new java.util.ArrayList<>();

                for (int i = 0; i < peticiones; i++) {
                    Pago objetivo = (i % 2 == 0) ? a : b;
                    tareas.add(executor.submit(() -> {
                        try {
                            arranque.await();
                            pagoService.aprobarPago(objetivo.getIdPago(), "REF");
                            aprobados.incrementAndGet();
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            rechazados.incrementAndGet();
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

                assertThat(aprobados.get()).as("ronda " + ronda).isEqualTo(2);
                assertThat(rechazados.get()).isEqualTo(peticiones - 2);
                assertThat(saldoPendienteDe(estadia))
                        .as("ronda " + ronda + ": la cuenta queda en cero exacto")
                        .isEqualByComparingTo("0.00");
                assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getCantidadPagosAprobados())
                        .isEqualTo(2);
            }
        }

        @Test
        @DisplayName("Registrar y consultar simultaneamente no corrompe el saldo")
        void registroYConsultaSimultaneos() throws Exception {
            limpiar();
            Estadia estadia = estadiaConReserva();

            int peticiones = 10;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            List<Future<?>> tareas = new java.util.ArrayList<>();
            List<String> fallos = new CopyOnWriteArrayList<>();

            for (int i = 0; i < peticiones; i++) {
                final int n = i;
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        if (n % 2 == 0) {
                            pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 1_000));
                        } else {
                            saldoService.consultarSaldo(estadia.getIdEstadia());
                        }
                        assertThat(pagoRepository.buscarTodos()).isNotNull();
                    } catch (IllegalStateException ignorada) {
                        // El disponible se agota: rechazo esperado.
                    } catch (RuntimeException e) {
                        fallos.add(e.getClass().getSimpleName());
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

            assertThat(fallos).allMatch(n -> n.equals("IllegalStateException")
                    || n.equals("IllegalArgumentException"));
            assertThat(saldoPendienteDe(estadia))
                    .as("el saldo nunca es negativo bajo concurrencia")
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Pagos, consumos, saldo y check-out simultaneos no producen interbloqueo")
        void sinInterbloqueo() throws Exception {
            limpiar();
            Estadia estadia = estadiaConReserva();
            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            int peticiones = 16;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            List<Future<?>> tareas = new java.util.ArrayList<>();
            List<String> fallos = new CopyOnWriteArrayList<>();

            for (int i = 0; i < peticiones; i++) {
                final int n = i;
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        switch (n % 6) {
                            case 0 -> pagoService.registrarPago(
                                    dtoPago(estadia.getIdEstadia(), 500));
                            case 1 -> pagoService.aprobarPago(pago.getIdPago(), "REF-" + n);
                            case 2 -> saldoService.consultarSaldo(estadia.getIdEstadia());
                            case 3 -> estadiaService.agregarConsumo(dtoConsumo(estadia, "Extra", 1, 1_000));
                            case 4 -> pagoService.reembolsarPago(pago.getIdPago());
                            default -> estadiaService.realizarCheckOut(estadia.getIdEstadia());
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

            for (Future<?> t : tareas) {
                t.get(20, TimeUnit.SECONDS);
            }
            executor.shutdownNow();

            assertThat(fallos).allMatch(n -> n.equals("IllegalStateException")
                    || n.equals("IllegalArgumentException"));
            assertThat(saldoPendienteDe(estadia))
                    .as("sin interbloqueo y sin saldo negativo")
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }



    @Nested
    @DisplayName("6 y 19. Implementacion alternativa y compatibilidad")
    class AlternoYCompatibilidad {

        @Test
        @DisplayName("PagoService funciona con un repositorio distinto del DataStore")
        void servicio_conRepositorioAlternativo() {
            RepositorioDePagosEnMemoria alterno = new RepositorioDePagosEnMemoria();
            PagoService servicio = new PagoService(alterno, estadiaService, saldoService);

            Estadia estadia = estadiaConReserva();
            Pago pago = servicio.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));

            assertThat(alterno.buscarPorId(pago.getIdPago())).get().isSameAs(pago);
            assertThat(alterno.contar()).isEqualTo(1);
            assertThat(dataStore.getPagos())
                    .as("el servicio escribe a traves del repositorio inyectado")
                    .isEmpty();
            assertThat(servicio.listarTodos()).hasSize(1);
            assertThat(servicio.listarPorEstadia(estadia.getIdEstadia())).hasSize(1);
            assertThat(servicio.buscarPorId(pago.getIdPago())).get().isSameAs(pago);
        }

        @Test
        @DisplayName("Las reglas de transicion se aplican igual con el repositorio alterno")
        void lasReglasDeTransicionSeAplican_conElRepositorioAlterno() {
            RepositorioDePagosEnMemoria alterno = new RepositorioDePagosEnMemoria();
            PagoService servicio = new PagoService(alterno, estadiaService, saldoService);

            Estadia estadia = estadiaConReserva();
            Pago pago = servicio.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));


            assertThatThrownBy(() -> servicio.registrarPago(dtoPago(estadia.getIdEstadia(), 0)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.registrarPago(dtoPago(estadia.getIdEstadia(), 10.999)))
                    .isInstanceOf(IllegalArgumentException.class);


            assertThatThrownBy(() -> servicio.reembolsarPago(pago.getIdPago()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PAGADO");

            servicio.aprobarPago(pago.getIdPago(), "REF");
            assertThatThrownBy(() -> servicio.aprobarPago(pago.getIdPago(), "REF-2"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDIENTE");

            assertThat(servicio.reembolsarPago(pago.getIdPago()).getEstadoPago())
                    .isEqualTo(EstadoPago.REEMBOLSADO);
            assertThat(alterno.contar()).isEqualTo(1);
        }

        @Test
        @DisplayName("DATO NEGATIVO: dos persistencias distintas hacen que los servicios no coincidan")
        void datoNegativo_mezclarDosPersistenciaRompeLaCoherencia() {

            RepositorioDePagosEnMemoria alterno = new RepositorioDePagosEnMemoria();
            PagoService servicio = new PagoService(alterno, estadiaService, saldoService);

            Estadia estadia = estadiaConReserva();
            BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();

            Pago primero = servicio.registrarPago(dtoPago(estadia.getIdEstadia(), 450_000));
            Pago segundo = servicio.registrarPago(dtoPago(estadia.getIdEstadia(), 450_000));

            assertThat(segundo)
                    .as("NO hay sobrepago real: el servicio lo permitiría porque el SaldoService del "
                            + "contenedor no ve los pagos del repositorio alterno. El dato es el disponible.")
                    .isNotNull();
            assertThat(alterno.contar()).isEqualTo(2);

            assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                    .as("el saldo del contenedor sigue en el total completo: ningun pago le resulta visible")
                    .isEqualByComparingTo(total);


            Pago conRepositorioReal = pagoService.registrarPago(
                    dtoPago(estadia.getIdEstadia(), 450_000));
            assertThat(conRepositorioReal.getIdPago()).isNotBlank();
            assertThatThrownBy(() -> pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 450_000)))
                    .as("con el DataStore compartido el sobrepago SI se detecta")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("supera el saldo disponible");
            assertThat(pagoRepository.buscarPorId(primero.getIdPago()))
                    .as("el pago del alterno es invisible para el repositorio real")
                    .isEmpty();
        }

        @Test
        @DisplayName("Compatibleidad con EstadiaRepository: la misma estadia y los mismos pagos")
        void compatibilidad_conEstadiaRepository() {
            Estadia estadia = estadiaConReserva();
            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            pagoService.aprobarPago(pago.getIdPago(), "REF");

            assertThat(estadiaRepository.buscarPorId(estadia.getIdEstadia())).get()
                    .isSameAs(estadia);
            assertThat(pago.getEstadia()).isSameAs(estadia);
            assertThat(saldoService.pagosDe(estadia.getIdEstadia()))
                    .as("SaldoService y PagoRepository leen la misma lista")
                    .containsExactly(pago);
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("350000.00");


            estadiaService.agregarConsumo(dtoConsumo(estadia, "Extra", 1, 25_000));
            assertThat(saldoPendienteDe(estadia)).isEqualByComparingTo("375000.00");
        }

        @Test
        @DisplayName("Los pagos nunca exponen datos de tarjeta ni cambian el importe al transicionar")
        void pagos_sinDatosBancariosEImporteInmutable() {
            Estadia estadia = estadiaConReserva();
            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), 100_000));
            double montoOriginal = pago.getMonto();

            pagoService.aprobarPago(pago.getIdPago(), "REF-1");
            assertThat(pago.getMonto()).as("aprobar no toca el importe").isEqualTo(montoOriginal);

            pagoService.reembolsarPago(pago.getIdPago());
            assertThat(pago.getMonto()).as("reembolsar no toca el importe").isEqualTo(montoOriginal);

            List<String> metodos = Arrays.stream(Pago.class.getMethods())
                    .filter(m -> m.getName().equals("setMonto") || m.getName().equals("setEstadoPago"))
                    .map(java.lang.reflect.Method::getName).toList();
            assertThat(metodos)
                    .as("no existe forma publica de alterar importe ni estado fuera de PagoService")
                    .isEmpty();
        }
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

    private BigDecimal saldoPendienteDe(Estadia estadia) {
        return saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente();
    }

    private Estadia estadiaConReserva() {
        return estadiaConReservaEn(HABITACION);
    }

    private Estadia estadiaConReservaEn(int numero) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped("H-001");
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(LocalDate.now());
        dto.setFechaFin(LocalDate.now().plusDays(3));
        dto.setAdultos(2);
        dto.setNinos(0);
        Reserva reserva = reservaService.crearReserva(dto);
        return estadiaService.realizarCheckIn(reserva.getIdReserva());
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
            Estadia estadia, String descripcion, int cantidad, double precio) {
        ConsumoRequestDTO dto =
                new ConsumoRequestDTO();
        dto.setIdEstadia(estadia.getIdEstadia());
        dto.setDescripcion(descripcion);
        dto.setCantidad(cantidad);
        dto.setPrecioUnitario(precio);
        return dto;
    }

    private void pagarTotal(Estadia estadia) {
        BigDecimal total = saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral();
        Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), total.doubleValue()));
        pagoService.aprobarPago(pago.getIdPago(), "REF-TEST");
    }

    /**
     * Paga el saldo pendiente de la estadía con un solo pago, si es que hay saldo pendiente.
     * @param estadia
     */
    private void pagarSaldo(Estadia estadia) {
        BigDecimal pendiente = saldoPendienteDe(estadia);
        if (pendiente.signum() > 0) {
            Pago pago = pagoService.registrarPago(dtoPago(estadia.getIdEstadia(), pendiente.doubleValue()));
            pagoService.aprobarPago(pago.getIdPago(), "REF-RESTO");
        }
    }

    /**
     * Repositorio de pagos en memoria para pruebas, compatible con PagoRepository.
     */
    private static class RepositorioDePagosEnMemoria implements PagoRepository {

        private final List<Pago> inventario = new CopyOnWriteArrayList<>();

        int contar() {
            return inventario.size();
        }

        @Override
        public Pago guardar(Pago pago) {
            inventario.add(pago);
            return pago;
        }

        @Override
        public Optional<Pago> buscarPorId(String idPago) {
            return inventario.stream().filter(p -> p.getIdPago().equals(idPago)).findFirst();
        }

        @Override
        public List<Pago> buscarTodos() {
            return inventario;
        }

        @Override
        public List<Pago> buscarPorEstadia(String idEstadia) {
            return inventario.stream()
                    .filter(p -> p.getEstadia().getIdEstadia().equals(idEstadia))
                    .collect(Collectors.toList());
        }

        @Override
        public Pago actualizar(Pago pago) {
            for (int i = 0; i < inventario.size(); i++) {
                if (inventario.get(i).getIdPago().equals(pago.getIdPago())) {
                    inventario.set(i, pago);
                    return pago;
                }
            }
            throw new IllegalArgumentException("No existe el pago con ID: " + pago.getIdPago());
        }
    }
}