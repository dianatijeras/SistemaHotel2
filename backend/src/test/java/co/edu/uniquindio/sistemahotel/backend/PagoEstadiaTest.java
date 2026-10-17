package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.SaldoPagoDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoPago;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
import co.edu.uniquindio.sistemahotel.backend.service.PagoService;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;


@SpringBootTest
@AutoConfigureMockMvc
class PagoEstadiaTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final LocalDate MAÑANA = LocalDate.now().plusDays(1);
    private static final String ID_HUESPED = "H-001";
    private static final int HABITACION = 101;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones().forEach(h -> {
            h.cambiarEstado(EstadoHabitacion.DISPONIBLE);
            h.setPrecioNoche(150_000.0);
        });
    }



    @Test
    @DisplayName("Registra un pago valido en estado PENDIENTE")
    void registro_pagoValido_nacePendiente() {
        Estadia estadia = crearEstadia(3);

        Pago pago = registrar(estadia.getIdEstadia(), 100_000);

        assertThat(pago.getIdPago()).startsWith("PAG-");
        assertThat(pago.getEstadoPago())
                .as("el pago nace pendiente; la aprobacion es una accion posterior")
                .isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pago.getMonto()).isEqualTo(100_000.0);
        assertThat(pago.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(pago.getConcepto()).isEqualTo("Abono inicial");
        assertThat(pago.getFechaPago()).isNotNull();
        assertThat(pago.getEstadia().getIdEstadia()).isEqualTo(estadia.getIdEstadia());
    }

    @Test
    @DisplayName("Usa el monto recibido y no lo sustituye por el total de la estadia")
    void registro_usaElMontoReal() {

        Estadia estadia = crearEstadia(3);
        assertThat(saldo(estadia).getTotalGeneral()).isEqualByComparingTo("450000.00");

        Pago pago = registrar(estadia.getIdEstadia(), 37_500.25);

        assertThat(pago.getMonto()).isEqualTo(37_500.25);
        assertThat(saldo(estadia).getTotalGeneral()).isEqualByComparingTo("450000.00");
    }

    @Test
    @DisplayName("Permite varios pagos parciales que van cubriendo el total")
    void registro_variosPagosParciales() {
        Estadia estadia = crearEstadia(2);
        assertThat(saldo(estadia).getTotalGeneral()).isEqualByComparingTo("300000.00");

        registrar(estadia.getIdEstadia(), 200_000);
        registrar(estadia.getIdEstadia(), 100_000);

        Pago a = pagoAprobar(estadia, 0);
        Pago b = pagoAprobar(estadia, 1);

        SaldoPagoDTO saldo = saldo(estadia);
        assertThat(saldo.getTotalAprobado()).isEqualByComparingTo("300000.00");
        assertThat(saldo.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(a.getMonto()).isEqualTo(200_000.0);
        assertThat(b.getMonto()).isEqualTo(100_000.0);
    }

    @Test
    @DisplayName("Permite pagar exactamente el saldo pendiente")
    void registro_pagoPorElSaldoExacto() {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 200_000);
        aprobar(pago(estadia, 0));
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("100000.00");

        Pago cierre = registrar(estadia.getIdEstadia(), 100_000);
        aprobar(cierre);

        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(saldo(estadia).getTotalAprobado()).isEqualByComparingTo("300000.00");
    }

    @Test
    @DisplayName("Rechaza un pago mayor que el saldo disponible")
    void registro_pagoSobreElSaldo_esRechazado() {
        Estadia estadia = crearEstadia(2);

        assertThatThrownBy(() -> registrar(estadia.getIdEstadia(), 300_001))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("supera el saldo disponible");

        assertThat(dataStore.getPagos()).isEmpty();
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("300000.00");
    }

    @Test
    @DisplayName("Rechaza un pago que junto a los pendientes supera el saldo")
    void registro_pagoPendienteAcumulado_esRechazado() {
        Estadia estadia = crearEstadia(1);
        registrar(estadia.getIdEstadia(), 120_000);

        assertThatThrownBy(() -> registrar(estadia.getIdEstadia(), 60_000))
                .as("120.000 pendientes + 60.000 nuevos superan los 150.000 totales")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("supera el saldo disponible");

        assertThat(dataStore.getPagos()).hasSize(1);
    }

    @Test
    @DisplayName("Rechaza monto cero o negativo")
    void registro_montoInvalido_esRechazado() {
        Estadia estadia = crearEstadia(1);

        for (double monto : new double[]{0.0, -1.0, -50_000.0}) {
            assertThatThrownBy(() -> registrar(estadia.getIdEstadia(), monto))
                    .as("monto " + monto)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto del pago debe ser mayor a 0");
        }
        assertThat(dataStore.getPagos()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza NaN e Infinity en el monto")
    void registro_montoNoFinito_esRechazado() {
        Estadia estadia = crearEstadia(1);

        for (double monto : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThatThrownBy(() -> registrar(estadia.getIdEstadia(), monto))
                    .as("monto " + monto)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monto del pago debe ser mayor a 0");
        }
        assertThat(dataStore.getPagos()).isEmpty();
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("150000.00");
    }

    @Test
    @DisplayName("Rechaza montos con mas de dos decimales")
    void registro_precisionInvalida_esRechazada() {
        Estadia estadia = crearEstadia(1);

        assertThatThrownBy(() -> registrar(estadia.getIdEstadia(), 10_000.005))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo 2 decimales");

        assertThat(dataStore.getPagos()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un metodo de pago ausente o invalido")
    void registro_metodoInvalido_esRechazado() {
        Estadia estadia = crearEstadia(1);

        assertThatThrownBy(() -> {
            PagoRequestDTO dto = dtoBase(estadia.getIdEstadia(), 10_000);
            dto.setMetodoPago(null);
            pagoService.registrarPago(dto);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("método de pago");

        assertThat(dataStore.getPagos()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un concepto ausente o demasiado largo")
    void registro_conceptoInvalido_esRechazado() {
        Estadia estadia = crearEstadia(1);

        assertThatThrownBy(() -> {
            PagoRequestDTO dto = dtoBase(estadia.getIdEstadia(), 10_000);
            dto.setConcepto("   ");
            pagoService.registrarPago(dto);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("concepto del pago");

        assertThatThrownBy(() -> {
            PagoRequestDTO dto = dtoBase(estadia.getIdEstadia(), 10_000);
            dto.setConcepto("x".repeat(201));
            pagoService.registrarPago(dto);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200 caracteres");

        assertThat(dataStore.getPagos()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza pagos sobre una estadia inexistente")
    void registro_estadiaInexistente_esRechazado() {
        assertThatThrownBy(() -> registrar("EST-NO-EXISTE", 10_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe la estadía");
        assertThat(dataStore.getPagos()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza pagos sobre una estadia no habilitada para cobro")
    void registro_estadiaNoHabilitada_esRechazado() {
        Reserva reserva = crearReserva(HOY, MAÑANA);
        Estadia estadia = new Estadia("EST-CANCELADA", reserva);
        estadia.setEstadoEstadia(EstadoEstadia.CANCELADA);
        dataStore.getEstadias().add(estadia);

        assertThatThrownBy(() -> registrar(estadia.getIdEstadia(), 10_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EN_CURSO o FINALIZADAS");

        assertThat(dataStore.getPagos()).isEmpty();
    }



    @Test
    @DisplayName("Un pago PENDIENTE no reduce el saldo")
    void saldo_pagoPendiente_noReduce() {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 200_000);

        SaldoPagoDTO saldo = saldo(estadia);
        assertThat(saldo.getSaldoPendiente()).isEqualByComparingTo("300000.00");
        assertThat(saldo.getTotalAprobado()).isEqualByComparingTo("0.00");
        assertThat(saldo.getCantidadPagosPendientes()).isEqualTo(1);
        assertThat(saldo.getCantidadPagosAprobados()).isZero();
    }

    @Test
    @DisplayName("Un pago RECHAZADO no reduce el saldo")
    void saldo_pagoRechazado_noReduce() {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 200_000);
        rechazar(pago(estadia, 0), "Tarjeta rechazada");

        SaldoPagoDTO saldo = saldo(estadia);
        assertThat(saldo.getSaldoPendiente()).isEqualByComparingTo("300000.00");
        assertThat(saldo.getTotalAprobado()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Un pago APROBADO si reduce el saldo")
    void saldo_pagoAprobado_reduce() {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 200_000);
        aprobar(pago(estadia, 0));

        SaldoPagoDTO saldo = saldo(estadia);
        assertThat(saldo.getSaldoPendiente()).isEqualByComparingTo("100000.00");
        assertThat(saldo.getTotalAprobado()).isEqualByComparingTo("200000.00");
        assertThat(saldo.getCantidadPagosAprobados()).isEqualTo(1);
    }

    @Test
    @DisplayName("Aprobar un pago cambia su estado y registra la referencia")
    void transicion_aprobar() {
        Estadia estadia = crearEstadia(2);
        Pago pendiente = registrar(estadia.getIdEstadia(), 50_000);

        Pago aprobado = pagoService.aprobarPago(pendiente.getIdPago(), "REF-001");

        assertThat(aprobado.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(aprobado.getReferenciaPasarela()).isEqualTo("REF-001");
        assertThat(aprobado.getMonto())
                .as("el monto es inmutable durante la transicion")
                .isEqualTo(50_000.0);
    }

    @Test
    @DisplayName("Rechazar un pago cambia su estado y guarda las observaciones")
    void transicion_rechazar() {
        Estadia estadia = crearEstadia(2);
        Pago pendiente = registrar(estadia.getIdEstadia(), 50_000);

        Pago rechazado = pagoService.rechazarPago(pendiente.getIdPago(), "Fondos insuficientes");

        assertThat(rechazado.getEstadoPago()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(rechazado.getObservaciones()).isEqualTo("Fondos insuficientes");
        assertThat(rechazado.getMonto()).isEqualTo(50_000.0);
    }

    @Test
    @DisplayName("Rechaza transiciones de estado invalidas")
    void transicion_invalidas_sonRechazadas() {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 100_000);
        Pago aprobado = aprobar(pago(estadia, 0));

        assertThatThrownBy(() -> pagoService.aprobarPago(aprobado.getIdPago(), "REF"))
                .as("aprobar dos veces")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden aprobar pagos PENDIENTES");

        assertThatThrownBy(() -> pagoService.rechazarPago(aprobado.getIdPago(), "x"))
                .as("rechazar un pago ya aprobado")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden rechazar pagos PENDIENTES");

        Pago pendiente = registrar(estadia.getIdEstadia(), 10_000);
        pagoService.rechazarPago(pendiente.getIdPago(), "no");
        assertThatThrownBy(() -> pagoService.aprobarPago(pendiente.getIdPago(), "REF"))
                .as("aprobar un pago ya rechazado")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RECHAZADO");

        assertThat(aprobado.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(saldo(estadia).getTotalAprobado()).isEqualByComparingTo("100000.00");
    }

    @Test
    @DisplayName("Rechaza aprobar o rechazar un pago inexistente")
    void transicion_pagoInexistente_esRechazado() {
        assertThatThrownBy(() -> pagoService.aprobarPago("PAG-NO-EXISTE", "REF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe el pago");
        assertThatThrownBy(() -> pagoService.rechazarPago("PAG-NO-EXISTE", "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe el pago");
    }

    @Test
    @DisplayName("La aprobacion vuelve a comprobar el saldo y evita el sobrepago")
    void transicion_aprobacionRevalidaElSaldo() {

        Estadia estadia = crearEstadia(1);
        Pago p1 = registrar(estadia.getIdEstadia(), 100_000);
        Pago p2 = insertarPagoPendiente("PAG-INCONSISTENTE", estadia, 100_000);

        aprobar(p1);
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("50000.00");

        assertThatThrownBy(() -> pagoService.aprobarPago(p2.getIdPago(), "REF"))
                .as("el saldo bajo desde que p2 quedo pendiente")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("el saldo pendiente de la estadía es solo");

        assertThat(p2.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(saldo(estadia).getTotalAprobado()).isEqualByComparingTo("100000.00");
    }



    @Test
    @DisplayName("Dos aprobaciones simultaneas no pueden sobrepagar la cuenta")
    void concurrencia_dosAprobacionesNoSobrepagan() throws Exception {

        Estadia estadia = crearEstadia(1);
        registrar(estadia.getIdEstadia(), 100_000);
        Pago p2 = insertarPagoPendiente("PAG-INCONSISTENTE-2", estadia, 100_000);
        String p1Id = pagoService.listarPorEstadia(estadia.getIdEstadia()).get(0).getIdPago();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch arranque = new CountDownLatch(1);
        AtomicInteger aprobadas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        List<Future<?>> tareas = new java.util.ArrayList<>();

        for (String id : List.of(p1Id, p2.getIdPago())) {
            tareas.add(executor.submit(() -> {
                try {
                    arranque.await();
                    pagoService.aprobarPago(id, "REF");
                    aprobadas.incrementAndGet();
                } catch (IllegalStateException e) {
                    rechazadas.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        arranque.countDown();
        for (Future<?> t : tareas) {
            t.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertThat(aprobadas.get()).as("solo una aprobacion puede prosperar").isEqualTo(1);
        assertThat(rechazadas.get()).isEqualTo(1);
        assertThat(saldo(estadia).getTotalAprobado()).isEqualByComparingTo("100000.00");
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("50000.00");
    }

    @Test
    @DisplayName("Many aprobaciones simultaneas sobre el saldo exacto dejan la cuenta en cero")
    void concurrencia_muchasAprobaciones() throws Exception {
        Estadia estadia = crearEstadia(1);
        List<Pago> pagos = List.of(
                registrar(estadia.getIdEstadia(), 50_000),
                registrar(estadia.getIdEstadia(), 50_000),
                registrar(estadia.getIdEstadia(), 50_000));

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch arranque = new CountDownLatch(1);
        AtomicInteger aprobadas = new AtomicInteger();
        List<Future<?>> tareas = new java.util.ArrayList<>();

        for (Pago p : pagos) {
            tareas.add(executor.submit(() -> {
                try {
                    arranque.await();
                    pagoService.aprobarPago(p.getIdPago(), "REF");
                    aprobadas.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Sobrecobro rechazado: es el resultado correcto.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        arranque.countDown();
        for (Future<?> t : tareas) {
            t.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertThat(aprobadas.get()).isEqualTo(3);
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(saldo(estadia).getTotalAprobado()).isEqualByComparingTo("150000.00");
    }



    @Test
    @DisplayName("El saldo se actualiza al agregar consumos despues de un pago")
    void saldo_cambiaAlAgregarConsumos() {
        Estadia estadia = crearEstadia(2);
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("300000.00");

        registrar(estadia.getIdEstadia(), 300_000);
        aprobar(pago(estadia, 0));
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("0.00");

        agregarConsumo(estadia.getIdEstadia(), "Room service", 2, 45_000.25);

        SaldoPagoDTO saldo = saldo(estadia);
        assertThat(saldo.getTotalGeneral()).isEqualByComparingTo("390000.50");
        assertThat(saldo.getTotalAprobado()).isEqualByComparingTo("300000.00");
        assertThat(saldo.getSaldoPendiente())
                .as("el consumo nuevo vuelve a abrir la deuda")
                .isEqualByComparingTo("90000.50");
    }

    @Test
    @DisplayName("Tras agregar consumos la cuenta vuelve a admitir pagos por el saldo nuevo")
    void saldo_pagoTrasConsumos() {
        Estadia estadia = crearEstadia(1);
        agregarConsumo(estadia.getIdEstadia(), "Spa", 1, 50_000.00);

        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("200000.00");
        registrar(estadia.getIdEstadia(), 200_000);
        aprobar(pago(estadia, 0));

        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(pagoService.tieneSaldoPendiente(estadia.getIdEstadia())).isFalse();
    }

    @Test
    @DisplayName("tieneSaldoPendiente distingue una cuenta saldada de otra que no")
    void saldo_indicadorParaCheckOut() {
        Estadia estadia = crearEstadia(1);
        assertThat(pagoService.tieneSaldoPendiente(estadia.getIdEstadia())).isTrue();

        registrar(estadia.getIdEstadia(), 150_000);
        assertThat(pagoService.tieneSaldoPendiente(estadia.getIdEstadia()))
                .as("un pago pendiente no salda la cuenta")
                .isTrue();

        aprobar(pago(estadia, 0));
        assertThat(pagoService.tieneSaldoPendiente(estadia.getIdEstadia())).isFalse();
    }

    @Test
    @DisplayName("Una estadia nueva no tiene pagos y su saldo es el total completo")
    void saldo_estadiaSinPagos() {
        Estadia estadia = crearEstadia(2);
        SaldoPagoDTO saldo = saldo(estadia);

        assertThat(saldo.getTotalGeneral()).isEqualByComparingTo("300000.00");
        assertThat(saldo.getTotalAprobado()).isEqualByComparingTo("0.00");
        assertThat(saldo.getSaldoPendiente()).isEqualByComparingTo("300000.00");
        assertThat(pagoService.listarPorEstadia(estadia.getIdEstadia())).isEmpty();
    }



    @Test
    @DisplayName("El historial conserva todos los pagos aunque se rechacen")
    void historial_seConserva() {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 100_000);
        registrar(estadia.getIdEstadia(), 100_000);
        registrar(estadia.getIdEstadia(), 100_000);

        aprobar(pago(estadia, 0));
        rechazar(pago(estadia, 1), "Rechazado");

        List<Pago> historial = pagoService.listarPorEstadia(estadia.getIdEstadia());
        assertThat(historial).as("ningun pago se borra").hasSize(3);
        assertThat(historial.stream().map(Pago::getEstadoPago))
                .containsExactlyInAnyOrder(EstadoPago.PAGADO, EstadoPago.RECHAZADO, EstadoPago.PENDIENTE);
        assertThat(pagoService.listarTodos()).hasSize(3);
    }

    @Test
    @DisplayName("Los pagos registrados conservan todos los datos exigidos")
    void historial_conservaLosDatos() {
        Estadia estadia = crearEstadia(1);
        PagoRequestDTO dto = dtoBase(estadia.getIdEstadia(), 25_000.50);
        dto.setMetodoPago(MetodoPago.TARJETA_CREDITO);
        Pago pago = pagoService.registrarPago(dto);

        assertThat(pago.getIdPago()).isNotBlank();
        assertThat(pago.getMonto()).isEqualTo(25_000.50);
        assertThat(pago.getFechaPago()).isNotNull();
        assertThat(pago.getMetodoPago()).isEqualTo(MetodoPago.TARJETA_CREDITO);
        assertThat(pago.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pago.getConcepto()).isEqualTo("Abono inicial");
        assertThat(pago.getEstadia()).isNotNull();
    }

    @Test
    @DisplayName("Cada pago registrado tiene un identificador unico")
    void historial_identificadoresUnicos() {
        Estadia estadia = crearEstadia(3);
        List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ids.add(registrar(estadia.getIdEstadia(), 10_000).getIdPago());
        }
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(ids).allMatch(id -> id.startsWith("PAG-"));
    }



    @Test
    @DisplayName("POST /api/pagos registra y devuelve el pago pendiente")
    void endpointRegistrar_devuelve201() throws Exception {
        Estadia estadia = crearEstadia(2);

        MvcResult resultado = mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPago(estadia.getIdEstadia(), 100_000, "EFECTIVO", "Abono")))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"estadoPago\":\"PENDIENTE\"");
        assertThat(cuerpo).contains("\"monto\":100000.0");
        assertThat(cuerpo).contains("\"concepto\":\"Abono\"");
        assertThat(cuerpo).doesNotContain("\"password\"").doesNotContain("pass123");
    }

    @Test
    @DisplayName("POST /api/pagos responde 400 ante monto invalido")
    void endpointRegistrar_montoInvalido_devuelve400() throws Exception {
        Estadia estadia = crearEstadia(2);

        MvcResult resultado = mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPago(estadia.getIdEstadia(), -5, "EFECTIVO", "Abono")))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("monto del pago");
    }

    @Test
    @DisplayName("POST /api/pagos responde 409 ante un pago que supera el saldo")
    void endpointRegistrar_sobrepago_devuelve409() throws Exception {
        Estadia estadia = crearEstadia(1);

        MvcResult resultado = mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPago(estadia.getIdEstadia(), 999_999, "EFECTIVO", "Abono")))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString()).contains("supera el saldo");
    }

    @Test
    @DisplayName("GET /api/pagos/estadia/{id}/saldo devuelve total, aprobado y saldo")
    void endpointSaldo_devuelveElEstadoDeCuenta() throws Exception {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 200_000);
        aprobar(pago(estadia, 0));

        MvcResult resultado = mockMvc.perform(
                get("/api/pagos/estadia/" + estadia.getIdEstadia() + "/saldo")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"totalGeneral\":300000.00");
        assertThat(cuerpo).contains("\"totalAprobado\":200000.00");
        assertThat(cuerpo).contains("\"saldoPendiente\":100000.00");
        assertThat(cuerpo).doesNotContain("\"password\"").doesNotContain("pass123");
    }

    @Test
    @DisplayName("PUT /api/pagos/{id}/aprobar y /rechazar funcionan por HTTP")
    void endpointTransiciones_funcionan() throws Exception {
        Estadia estadia = crearEstadia(2);
        String idAprobar = registrar(estadia.getIdEstadia(), 100_000).getIdPago();
        String idRechazar = registrar(estadia.getIdEstadia(), 50_000).getIdPago();

        MvcResult aprobado = mockMvc.perform(put("/api/pagos/" + idAprobar + "/aprobar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referencia\":\"REF-9\"}"))
                .andReturn();
        assertThat(aprobado.getResponse().getStatus()).isEqualTo(200);
        assertThat(aprobado.getResponse().getContentAsString()).contains("\"estadoPago\":\"PAGADO\"");

        MvcResult rechazado = mockMvc.perform(put("/api/pagos/" + idRechazar + "/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"Sin fondos\"}"))
                .andReturn();
        assertThat(rechazado.getResponse().getStatus()).isEqualTo(200);
        assertThat(rechazado.getResponse().getContentAsString()).contains("\"estadoPago\":\"RECHAZADO\"");

        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("200000.00");
    }

    @Test
    @DisplayName("PUT /api/pagos/{id}/aprobar responde 409 ante una transicion invalida")
    void endpointAprobar_invalido_devuelve409() throws Exception {
        Estadia estadia = crearEstadia(2);
        String id = registrar(estadia.getIdEstadia(), 100_000).getIdPago();
        mockMvc.perform(put("/api/pagos/" + id + "/aprobar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"REF\"}")).andReturn();

        MvcResult segunda = mockMvc.perform(put("/api/pagos/" + id + "/aprobar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"referencia\":\"REF\"}")).andReturn();

        assertThat(segunda.getResponse().getStatus()).isEqualTo(409);
        assertThat(segunda.getResponse().getContentAsString()).contains("PENDIENTES");
    }

    @Test
    @DisplayName("El historial por HTTP mantiene los consumidores anteriores")
    void endpointHistorialCompatible() throws Exception {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 100_000);

        MvcResult todos = mockMvc.perform(get("/api/pagos")).andReturn();
        MvcResult porEstadia = mockMvc.perform(
                get("/api/pagos/estadia/" + estadia.getIdEstadia())).andReturn();

        assertThat(todos.getResponse().getStatus()).isEqualTo(200);
        assertThat(porEstadia.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = porEstadia.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"idPago\":\"PAG-");
        assertThat(cuerpo).contains("\"monto\":");
        assertThat(cuerpo).contains("\"estadoPago\":");
        assertThat(cuerpo).contains("\"fechaPago\":");
        assertThat(cuerpo).contains("\"metodoPago\":");
    }

    @Test
    @DisplayName("El saldo de una estadia inexistente responde 400")
    void endpointSaldo_invalido_devuelve400() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/pagos/estadia/EST-X/saldo")).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }



    @Test
    @DisplayName("Los pagos no alteran el desglose de costos de F-09")
    void compatibilidad_desgloseNoCambia() throws Exception {
        Estadia estadia = crearEstadia(2);
        registrar(estadia.getIdEstadia(), 100_000);
        aprobar(pago(estadia, 0));

        var costo = estadiaService.calcularCosto(estadia.getIdEstadia());
        assertThat(costo.getNoches()).isEqualTo(2);
        assertThat(costo.getSubtotalAlojamiento()).isEqualByComparingTo("300000.00");
        assertThat(costo.getSubtotalConsumos()).isEqualByComparingTo("0.00");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo("300000.00");
    }

    @Test
    @DisplayName("El check-in y el check-out siguen funcionando junto a los pagos")
    void compatibilidad_checkInYCheckOut() throws Exception {
        Reserva reserva = crearReserva(HOY, HOY.plusDays(2));
        Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

        registrar(estadia.getIdEstadia(), 300_000);
        aprobar(pago(estadia, 0));
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("0.00");

        var finalizada = estadiaService.realizarCheckOut(estadia.getIdEstadia());
        assertThat(finalizada.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
        assertThat(reserva.getEstadoReserva().name()).isEqualTo("FINALIZADA");
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

        // Y una vez finalizada se puede seguir consultando su cuenta.
        assertThat(saldo(estadia).getSaldoPendiente()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Los datos iniciales del hotel siguen intactos")
    void compatibilidad_datosIniciales() {
        assertThat(dataStore.getHabitaciones()).hasSizeGreaterThanOrEqualTo(8);
        assertThat(dataStore.getHuespedes()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(dataStore.getPagos()).as("el historial arranca vacio").isEmpty();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getPrecioNoche()).isEqualTo(150_000.0);
        assertThat(pagoService.listarTodos()).isEmpty();
    }



    private String jsonPago(String idEstadia, double monto, String metodo, String concepto) {
        return "{\"idEstadia\":\"" + idEstadia + "\",\"monto\":" + monto
                + ",\"metodoPago\":\"" + metodo + "\",\"concepto\":\"" + concepto + "\"}";
    }

    private MockHttpServletRequestBuilder put(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url);
    }

    private SaldoPagoDTO saldo(Estadia estadia) {
        return pagoService.consultarSaldo(estadia.getIdEstadia());
    }

    private PagoRequestDTO dtoBase(String idEstadia, double monto) {
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setMonto(monto);
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Abono inicial");
        return dto;
    }

    private Pago registrar(String idEstadia, double monto) {
        return pagoService.registrarPago(dtoBase(idEstadia, monto));
    }

    /**
     * Inserta un pago PENDIENTE directamente en el DataStore, saltándose la validación del
     * registro. Sirve para crear el estado incoherente (varios pendientes que suman más que el
     * total) que la revalidación de la aprobación debe saber impedir.
     */
    private Pago insertarPagoPendiente(String id, Estadia estadia, double monto) {
        Pago pago = new Pago(id, monto, MetodoPago.EFECTIVO, "Abono directo", estadia);
        dataStore.getPagos().add(pago);
        return pago;
    }

    /** Devuelve el pago n-esimo registrado de la estadía. */
    private Pago pago(Estadia estadia, int indice) {
        return pagoService.listarPorEstadia(estadia.getIdEstadia()).get(indice);
    }

    private Pago aprobar(Pago pago) {
        return pagoService.aprobarPago(pago.getIdPago(), "REF-TEST");
    }

    private Pago rechazar(Pago pago, String motivo) {
        return pagoService.rechazarPago(pago.getIdPago(), motivo);
    }

    /** Registra y aprueba de una vez el pago n-esimo de la estadía. */
    private Pago pagoAprobar(Estadia estadia, int indice) {
        return aprobar(pago(estadia, indice));
    }

    private void agregarConsumo(String idEstadia, String descripcion, int cantidad, double precio) {
        ConsumoRequestDTO dto = new ConsumoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setDescripcion(descripcion);
        dto.setCantidad(cantidad);
        dto.setPrecioUnitario(precio);
        estadiaService.agregarConsumo(dto);
    }

    private Estadia crearEstadia(int noches) {
        return estadiaService.realizarCheckIn(crearReserva(HOY, HOY.plusDays(noches)).getIdReserva());
    }

    private Reserva crearReserva(LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(ID_HUESPED);
        dto.setNumeroHabitacion(HABITACION);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return reservaService.crearReserva(dto);
    }
}