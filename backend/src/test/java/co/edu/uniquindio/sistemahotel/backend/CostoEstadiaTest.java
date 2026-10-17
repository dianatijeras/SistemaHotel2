package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoCostoDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.EstadiaCostoDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.ConsumoEstadia;
import co.edu.uniquindio.sistemahotel.backend.model.Costos;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Pruebas de F-09: cálculo del costo total de la estadía.
 * Verifica el desglose (noches, alojamiento, consumos, total general), las reglas de validación,
 * la precisión numérica y la ausencia de cambios parciales.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CostoEstadiaTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final LocalDate MAÑANA = LocalDate.now().plusDays(1);
    private static final String ID_HUESPED = "H-001";
    private static final String ID_HUESPED_PRUEBA = "H-PRUEBA";
    private static final int HABITACION = 101;
    /** Precio configurado de la habitación 101 en el DataInitializer. */
    private static final double PRECIO_NOCHE_101 = 150_000.0;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();

        dataStore.getHabitaciones().forEach(h -> {
            h.cambiarEstado(EstadoHabitacion.DISPONIBLE);
            h.setPrecioNoche(preciosSemilla().getOrDefault(h.getNumeroHabitacion(), PRECIO_NOCHE_101));
        });
        dataStore.getHuespedes().removeIf(h -> h.getIdHuesped().startsWith(ID_HUESPED_PRUEBA));
    }

    /** Precios por noche con los que el DataInitializer creo cada habitación. */
    private java.util.Map<Integer, Double> preciosSemilla() {
        return java.util.Map.of(101, 150_000.0, 102, 150_000.0, 103, 150_000.0,
                201, 220_000.0, 202, 220_000.0,
                301, 350_000.0, 302, 350_000.0, 401, 600_000.0);
    }



    @Test
    @DisplayName("Calcula una estadía de una noche")
    void costo_unaNoche() {
        EstadiaCostoDTO costo = costoDe(crearEstadia(HABITACION, HOY, HOY.plusDays(1)));

        assertThat(costo.getNoches()).isEqualTo(1);
        assertThat(costo.getPrecioNoche()).isEqualByComparingTo("150000.00");
        assertThat(costo.getSubtotalAlojamiento()).isEqualByComparingTo("150000.00");
        assertThat(costo.getSubtotalConsumos()).isEqualByComparingTo("0.00");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo("150000.00");
    }

    @Test
    @DisplayName("Calcula una estadía de varias noches")
    void costo_variasNoches() {
        EstadiaCostoDTO costo = costoDe(crearEstadia(HABITACION, HOY, HOY.plusDays(6)));

        assertThat(costo.getNoches()).isEqualTo(6);
        assertThat(costo.getSubtotalAlojamiento()).isEqualByComparingTo("900000.00");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo("900000.00");
    }

    @Test
    @DisplayName("La fecha de salida es el limite exclusivo del periodo reservado")
    void costo_fechaDeSalidaEsExclusiva() {
        LocalDate inicio = LocalDate.of(2026, 3, 10);
        LocalDate fin = LocalDate.of(2026, 3, 11);
        Reserva reserva = insertarReservaDirecta("RES-LIMITE", HABITACION, inicio, fin);

        assertThat(reserva.calcularNoches()).isEqualTo(1);
        assertThat(reserva.calcularTotalHabitacion()).isEqualTo(150_000.0);
    }

    @Test
    @DisplayName("Calcula corretamente un cruce de mes")
    void costo_cruceDeMes() {
        LocalDate inicio = LocalDate.of(2026, 3, 28);
        LocalDate fin = LocalDate.of(2026, 4, 2);
        Reserva reserva = insertarReservaDirecta("RES-MES", HABITACION, inicio, fin);

        assertThat(reserva.calcularNoches()).isEqualTo(5);
        assertThat(reserva.calcularTotalHabitacion()).isEqualTo(750_000.0);
    }

    @Test
    @DisplayName("Calcula correctamente un cruce de anio")
    void costo_cruceDeAnio() {
        LocalDate inicio = LocalDate.of(2026, 12, 30);
        LocalDate fin = LocalDate.of(2027, 1, 3);
        Reserva reserva = insertarReservaDirecta("RES-ANIO", HABITACION, inicio, fin);

        assertThat(reserva.calcularNoches()).isEqualTo(4);
        assertThat(reserva.calcularTotalHabitacion()).isEqualTo(600_000.0);
    }

    @Test
    @DisplayName("Un anio bisiesto se cuenta correctamente")
    void costo_anioBisiesto() {
        LocalDate inicio = LocalDate.of(2028, 2, 28);
        LocalDate fin = LocalDate.of(2028, 3, 1);
        Reserva reserva = insertarReservaDirecta("RES-BISIESTO", HABITACION, inicio, fin);

        assertThat(reserva.calcularNoches()).isEqualTo(2);
    }

    @Test
    @DisplayName("Rechaza calcular el costo de un periodo con cero noches")
    void costo_ceroNoches_esRechazado() {
        Reserva reserva = insertarReservaDirecta("RES-CERO", HABITACION, HOY, HOY);
        assertThat(reserva.calcularNoches()).isZero();
        assertThat(reserva.calcularTotalHabitacion()).as("no produce totales negativos").isZero();

        Estadia estadia = registrarEstadia("EST-CERO", reserva);

        assertThat(estadia.getTotalHabitacion()).isZero();
        assertThatThrownBy(() -> estadiaService.calcularCosto("EST-CERO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("noches");
    }

    @Test
    @DisplayName("Rechaza calcular el costo de una estadia con fechas invertidas")
    void costo_fechasInvertidas_esRechazado() {
        Reserva reserva = insertarReservaDirecta("RES-INVERTIDA", HABITACION,
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 5));
        assertThat(reserva.calcularNoches()).isNegative();

        Estadia estadia = registrarEstadia("EST-INVERTIDA", reserva);

        assertThat(estadia.getTotalHabitacion()).as("nunca negativo").isZero();
        assertThatThrownBy(() -> estadiaService.calcularCosto("EST-INVERTIDA"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza calcular el costo de una habitacion sin precio valido")
    void costo_precioNocheInvalido_esRechazado() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        habitacionService.obtenerOFallar(HABITACION).setPrecioNoche(Double.NaN);

        assertThatThrownBy(() -> estadiaService.calcularCosto(estadia.getIdEstadia()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("precio por noche");
    }



    @Test
    @DisplayName("Una estadia sin consumos tiene subtotal y total en cero")
    void costo_sinConsumos() {
        EstadiaCostoDTO costo = costoDe(crearEstadia(HABITACION, HOY, MAÑANA));

        assertThat(costo.getConsumos()).isEmpty();
        assertThat(costo.getSubtotalConsumos()).isEqualByComparingTo("0.00");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo(costo.getSubtotalAlojamiento());
    }

    @Test
    @DisplayName("Calcula el subtotal de un consumo como cantidad por precio unitario")
    void costo_unConsumo() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        agregarConsumo(estadia.getIdEstadia(), "Minibar", 3, 25_500.50);

        EstadiaCostoDTO costo = costoDe(estadia);

        assertThat(costo.getConsumos()).hasSize(1);
        ConsumoCostoDTO consumo = costo.getConsumos().get(0);
        assertThat(consumo.getDescripcion()).isEqualTo("Minibar");
        assertThat(consumo.getCantidad()).isEqualTo(3);
        assertThat(consumo.getPrecioUnitario()).isEqualByComparingTo("25500.50");
        assertThat(consumo.getSubtotal()).isEqualByComparingTo("76501.50");
        assertThat(costo.getSubtotalConsumos()).isEqualByComparingTo("76501.50");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo("226501.50");
    }

    @Test
    @DisplayName("Suma correctamente varios consumos")
    void costo_variosConsumos() {
        Estadia estadia = crearEstadia(HABITACION, HOY, HOY.plusDays(3));
        agregarConsumo(estadia.getIdEstadia(), "Room service", 1, 45_000.00);
        agregarConsumo(estadia.getIdEstadia(), "Minibar", 2, 12_500.25);
        agregarConsumo(estadia.getIdEstadia(), "Lavanderia", 3, 8_000.10);

        EstadiaCostoDTO costo = costoDe(estadia);

        assertThat(costo.getNoches()).isEqualTo(3);
        assertThat(costo.getSubtotalAlojamiento()).isEqualByComparingTo("450000.00");
        assertThat(costo.getSubtotalConsumos()).isEqualByComparingTo("94000.80");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo("544000.80");
        assertThat(costo.getConsumos()).hasSize(3);
        assertThat(costo.getConsumos().get(1).getSubtotal()).isEqualByComparingTo("25000.50");
    }

    @Test
    @DisplayName("El total general se actualiza despues de agregar consumos")
    void costo_seActualizaAlAgregarConsumos() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        assertThat(costoDe(estadia).getTotalGeneral()).isEqualByComparingTo("150000.00");

        agregarConsumo(estadia.getIdEstadia(), "Minibar", 2, 10_000.00);
        assertThat(costoDe(estadia).getTotalGeneral()).isEqualByComparingTo("170000.00");

        agregarConsumo(estadia.getIdEstadia(), "Spa", 1, 30_000.00);
        EstadiaCostoDTO costo = costoDe(estadia);
        assertThat(costo.getSubtotalConsumos()).isEqualByComparingTo("50000.00");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo("200000.00");

        assertThat(estadia.getTotalFinal()).isEqualTo(200_000.0);
        assertThat(estadia.getTotalHabitacion()).isEqualTo(150_000.0);
        assertThat(estadia.getTotalConsumos()).isEqualTo(50_000.0);
    }

    @Test
    @DisplayName("El subtotal de cada consumo coincide con lo que suma el total")
    void costo_subtotalCoincideConElTotal() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        agregarConsumo(estadia.getIdEstadia(), "A", 2, 1_111.11);
        agregarConsumo(estadia.getIdEstadia(), "B", 7, 999.99);

        EstadiaCostoDTO costo = costoDe(estadia);
        BigDecimal suma = costo.getConsumos().stream()
                .map(ConsumoCostoDTO::getSubtotal)
                .reduce(Costos.CERO, BigDecimal::add);

        assertThat(suma).isEqualByComparingTo(costo.getSubtotalConsumos());
        assertThat(costo.getTotalGeneral())
                .isEqualByComparingTo(costo.getSubtotalAlojamiento().add(costo.getSubtotalConsumos()));
    }



    @Test
    @DisplayName("Rechaza cantidad cero o negativa")
    void costo_cantidadInvalida_esRechazada() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        for (int cantidad : new int[]{0, -1, -50}) {
            assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "X", cantidad, 10_000))
                    .as("cantidad " + cantidad)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cantidad");
        }
        assertThat(estadia.getConsumos()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza precio unitario cero, negativo o no finito")
    void costo_precioInvalido_esRechazado() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        for (double precio : new double[]{0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY}) {
            assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "X", 1, precio))
                    .as("precio " + precio)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("precio unitario");
        }
        assertThat(estadia.getConsumos()).isEmpty();
        assertThat(costoDe(estadia).getTotalGeneral()).isEqualByComparingTo("150000.00");
    }

    @Test
    @DisplayName("Rechaza precios con mas de dos decimales")
    void costo_precisionInvalida_esRechazada() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);

        assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "Fraccion", 1, 10.005))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 decimales");

        assertThat(estadia.getConsumos()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza consumos en una estadia que no esta EN_CURSO")
    void costo_consumoEnEstadiaNoHabilitada_esRechazado() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        estadia.finalizar();
        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);

        assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "Minibar", 1, 10_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EN_CURSO");

        assertThat(estadia.getConsumos()).isEmpty();
        assertThat(estadia.getTotalFinal()).isEqualTo(150_000.0);
    }

    @Test
    @DisplayName("Rechaza consumos en una estadia inexistente o cancelada")
    void costo_consumoEnEstadiaInvalida_esRechazado() {
        assertThatThrownBy(() -> agregarConsumo("EST-NO-EXISTE", "Minibar", 1, 10_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe la estadía");

        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        Estadia estadia = registrarEstadia("EST-CANCELADA", reserva);
        estadia.setEstadoEstadia(EstadoEstadia.CANCELADA);

        assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "Minibar", 1, 10_000))
                .isInstanceOf(IllegalStateException.class);
    }



    @Test
    @DisplayName("Un consumo rechazado no modifica los totales")
    void costo_rechazo_noDejaCambiosParciales() {
        Estadia estadia = crearEstadia(HABITACION, HOY, HOY.plusDays(2));
        agregarConsumo(estadia.getIdEstadia(), "Room service", 1, 40_000.00);
        BigDecimal totalAntes = costoDe(estadia).getTotalGeneral();

        assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "", 1, 10_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "X", 0, 10_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "X", 1, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> agregarConsumo(estadia.getIdEstadia(), "X", 1, -5))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(estadia.getConsumos()).hasSize(1);
        assertThat(costoDe(estadia).getTotalGeneral()).isEqualByComparingTo(totalAntes);
        assertThat(estadia.getTotalFinal()).isEqualTo(340_000.0);
    }



    @Test
    @DisplayName("Evita el error de coma flotante clasico (0.1 + 0.2)")
    void costo_evitaErrorDeComaFlotante() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        agregarConsumo(estadia.getIdEstadia(), "A", 1, 0.10);
        agregarConsumo(estadia.getIdEstadia(), "B", 1, 0.20);

        EstadiaCostoDTO costo = costoDe(estadia);

        assertThat(costo.getSubtotalConsumos())
                .as("en double 0.1 + 0.2 = 0.30000000000000004")
                .isEqualByComparingTo("0.30");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo("150000.30");
    }

    @Test
    @DisplayName("Acumula consumos con centimos sin perder precision")
    void costo_acumulaCentimos() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        for (int i = 0; i < 10; i++) {
            agregarConsumo(estadia.getIdEstadia(), "Cafe", 1, 0.07);
        }

        assertThat(costoDe(estadia).getSubtotalConsumos())
                .as("0.07 x 10 debe ser exactamente 0.70")
                .isEqualByComparingTo("0.70");
        assertThat(costoDe(estadia).getTotalGeneral()).isEqualByComparingTo("150000.70");
    }

    @Test
    @DisplayName("Todos los importes del desglose se entregan con dos decimales")
    void costo_importesConDosDecimales() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        agregarConsumo(estadia.getIdEstadia(), "Minibar", 3, 1_234.56);

        EstadiaCostoDTO costo = costoDe(estadia);

        assertThat(costo.getPrecioNoche().scale()).isEqualTo(Costos.ESCALA);
        assertThat(costo.getSubtotalAlojamiento().scale()).isEqualTo(Costos.ESCALA);
        assertThat(costo.getSubtotalConsumos().scale()).isEqualTo(Costos.ESCALA);
        assertThat(costo.getTotalGeneral().scale()).isEqualTo(Costos.ESCALA);
        costo.getConsumos().forEach(c -> {
            assertThat(c.getPrecioUnitario().scale()).isEqualTo(Costos.ESCALA);
            assertThat(c.getSubtotal().scale()).isEqualTo(Costos.ESCALA);
        });
    }

    @Test
    @DisplayName("Los totales nunca son negativos ni NaN, ni con datos corruptos")
    void costo_totalesSiempreValidos() {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        estadia.getConsumos().add(new ConsumoEstadia("CON-X", "Corrupto", HOY, 2, Double.NaN));

        BigDecimal subtotal = costoDe(estadia).getSubtotalConsumos();

        assertThat(subtotal).as("NaN nunca llega al total").isEqualByComparingTo("0.00");
        assertThat(costoDe(estadia).getTotalGeneral()).isEqualByComparingTo("150000.00");
        assertThat(estadia.getTotalFinal()).isEqualTo(150_000.0);
    }

    @Test
    @DisplayName("Las consultas repetidas devuelven exactamente el mismo calculo")
    void costo_consultasRepetidasSonEstables() {
        Estadia estadia = crearEstadia(HABITACION, HOY, HOY.plusDays(4));
        agregarConsumo(estadia.getIdEstadia(), "Spa", 2, 75_000.55);

        EstadiaCostoDTO primero = costoDe(estadia);
        for (int i = 0; i < 5; i++) {
            EstadiaCostoDTO repetido = costoDe(estadia);
            assertThat(repetido.getNoches()).isEqualTo(primero.getNoches());
            assertThat(repetido.getSubtotalAlojamiento()).isEqualByComparingTo(primero.getSubtotalAlojamiento());
            assertThat(repetido.getSubtotalConsumos()).isEqualByComparingTo(primero.getSubtotalConsumos());
            assertThat(repetido.getTotalGeneral()).isEqualByComparingTo(primero.getTotalGeneral());
            assertThat(repetido.getConsumos()).hasSameSizeAs(primero.getConsumos());
        }
        assertThat(primero.getNoches()).isEqualTo(4);
        assertThat(primero.getTotalGeneral()).isEqualByComparingTo("750001.10");
    }


    @Test
    @DisplayName("Consumos simultaneos producen un total exacto")
    void costo_consumosSimultaneos_sonExactos() throws Exception {
        Estadia estadia = crearEstadia(HABITACION, HOY, MAÑANA);
        int peticiones = 20;

        ExecutorService executor = Executors.newFixedThreadPool(peticiones);
        CountDownLatch arranque = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        List<Future<?>> tareas = new java.util.ArrayList<>();

        for (int i = 0; i < peticiones; i++) {
            tareas.add(executor.submit(() -> {
                try {
                    arranque.await();
                    agregarConsumo(estadia.getIdEstadia(), "Minibar", 1, 1_000.55);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    error.compareAndSet(null, e);
                }
            }));
        }

        arranque.countDown();
        for (Future<?> t : tareas) {
            t.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertThat(error.get()).as("ningún consumo simultáneo debe fallar").isNull();
        assertThat(estadia.getConsumos()).hasSize(peticiones);

        EstadiaCostoDTO costo = costoDe(estadia);
        assertThat(costo.getSubtotalConsumos())
                .as("20 x 1000.55 = 20011.00 exacto, sin centimos perdidos")
                .isEqualByComparingTo("20011.00");
        assertThat(costo.getTotalGeneral()).isEqualByComparingTo("170011.00");
    }


    @Test
    @DisplayName("GET /api/estadias/{id}/costo devuelve el desglose completo")
    void endpointCosto_devuelveElDesglose() throws Exception {
        String idEstadia = crearEstadiaPorHttp(HABITACION, HOY, HOY.plusDays(2)).getIdEstadia();
        agregarConsumo(idEstadia, "Minibar", 2, 15_000.25);

        MvcResult resultado = mockMvc.perform(get("/api/estadias/" + idEstadia + "/costo")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"noches\":2");
        assertThat(cuerpo).contains("\"precioNoche\":150000.00");
        assertThat(cuerpo).contains("\"subtotalAlojamiento\":300000.00");
        assertThat(cuerpo).contains("\"subtotalConsumos\":30000.50");
        assertThat(cuerpo).contains("\"totalGeneral\":330000.50");
        assertThat(cuerpo).contains("\"descripcion\":\"Minibar\"");
        assertThat(cuerpo).doesNotContain("\"password\"").doesNotContain("pass123");
    }

    @Test
    @DisplayName("GET /api/estadias/{id}/costo responde 400 si la estadia no existe")
    void endpointCosto_noExiste_devuelve400() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/estadias/EST-NO-EXISTE/costo")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("mensaje");
    }

    @Test
    @DisplayName("POST /api/estadias/consumo responde 409 fuera del estado permitido")
    void endpointConsumo_estadoInvalido_devuelve409() throws Exception {
        String idEstadia = crearEstadiaPorHttp(HABITACION, HOY, MAÑANA).getIdEstadia();


        agregarConsumo(idEstadia, "Room service", 1, 20_000);
        long total = Math.round(estadiaService.calcularCosto(idEstadia).getTotalGeneral().doubleValue());
        PagoRequestDTO pago = new PagoRequestDTO();
        pago.setIdEstadia(idEstadia);
        pago.setMonto(total);
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setConcepto("Pago total para el check-out");
        pagoService.registrarPago(pago);
        pagoService.aprobarPago(pagoService.listarPorEstadia(idEstadia).get(0).getIdPago(), "REF");

        mockMvc.perform(put("/api/estadias/" + idEstadia + "/checkout")).andReturn();

        MvcResult resultado = mockMvc.perform(post("/api/estadias/consumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConsumo(idEstadia, "Minibar", 1, 10_000)))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("POST /api/estadias/consumo rechaza un precio no finito con 400")
    void endpointConsumo_precioNoFinito_devuelve400() throws Exception {
        String idEstadia = crearEstadiaPorHttp(HABITACION, HOY, MAÑANA).getIdEstadia();

        MvcResult resultado = mockMvc.perform(post("/api/estadias/consumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEstadia\":\"" + idEstadia + "\",\"descripcion\":\"X\","
                                + "\"cantidad\":1,\"precioUnitario\":\"NaN\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("precio unitario");
    }

    @Test
    @DisplayName("El JSON de la estadia mantiene los campos de total de siempre")
    void compatibilidad_elJsonDeEstadiaNoCambia() throws Exception {
        String idEstadia = crearEstadiaPorHttp(HABITACION, HOY, HOY.plusDays(2)).getIdEstadia();
        agregarConsumo(idEstadia, "Minibar", 2, 15_000.25);

        MvcResult resultado = mockMvc.perform(get("/api/estadias/" + idEstadia)).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"totalHabitacion\":300000.0");
        assertThat(cuerpo).contains("\"totalConsumos\":30000.5");
        assertThat(cuerpo).contains("\"totalFinal\":330000.5");
        assertThat(cuerpo).contains("\"subtotal\":30000.5");
        assertThat(cuerpo).contains("\"estadoEstadia\":");
    }

    @Test
    @DisplayName("El check-in sigue funcionando y la estadia nace con su costo calculado")
    void compatibilidad_checkIn_conservaElCosto() throws Exception {
        String idReserva = crearReservaPorHttp(HABITACION, HOY, HOY.plusDays(3));

        MvcResult resultado = mockMvc.perform(post("/api/estadias/checkin/" + idReserva)).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"totalHabitacion\":450000.0");
        assertThat(cuerpo).contains("\"totalConsumos\":0.0");
        assertThat(cuerpo).contains("\"totalFinal\":450000.0");
    }

    @Test
    @DisplayName("Registrar y consultar una reserva no se ven afectadas por los cambios de costo")
    void compatibilidad_reservasSiguenIgual() throws Exception {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);

        assertThat(reserva.getEstadoReserva()).isNotNull();
        assertThat(reserva.calcularNoches()).isEqualTo(1);
        assertThat(reserva.calcularTotalHabitacion()).isEqualTo(150_000.0);
        assertThat(reservaService.listarTodas()).hasSize(1);

        reservaService.confirmarReserva(reserva.getIdReserva());
        assertThat(reserva.getEstadoReserva().name()).isEqualTo("CONFIRMADA");

        assertThat(reserva.calcularTotalHabitacion()).isEqualTo(150_000.0);
    }

    @Test
    @DisplayName("Los datos iniciales del DataInitializer se conservan")
    void compatibilidad_datosInicialesSeConservan() {

        List<Habitacion> habitaciones =
                dataStore.getHabitaciones();

        assertThat(habitaciones).hasSizeGreaterThanOrEqualTo(8);
        assertThat(habitaciones.stream().anyMatch(h -> h.getNumeroHabitacion() == HABITACION)).isTrue();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getPrecioNoche()).isEqualTo(PRECIO_NOCHE_101);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
        assertThat(dataStore.getHuespedes().stream()
                .anyMatch(h -> ID_HUESPED.equals(h.getIdHuesped()))).isTrue();
        assertThat(dataStore.getHuespedes().stream()
                .filter(h -> ID_HUESPED.equals(h.getIdHuesped()))
                .findFirst().orElseThrow().getDocumento())
                .isEqualTo("CC10000001");
    }



    private MockHttpServletRequestBuilder put(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url);
    }

    /** Construye una estadía y la registra en el DataStore, sin pasar por el check-in. */
    private Estadia registrarEstadia(String id, Reserva reserva) {
        Estadia estadia = new Estadia(id, reserva);
        dataStore.getEstadias().add(estadia);
        return estadia;
    }

    private String jsonConsumo(String idEstadia, String descripcion, int cantidad, double precio) {
        return "{\"idEstadia\":\"" + idEstadia + "\",\"descripcion\":\"" + descripcion
                + "\",\"cantidad\":" + cantidad + ",\"precioUnitario\":" + precio + "}";
    }

    private EstadiaCostoDTO costoDe(Estadia estadia) {
        return estadiaService.calcularCosto(estadia.getIdEstadia());
    }

    private void agregarConsumo(String idEstadia, String descripcion, int cantidad, double precio) {
        ConsumoRequestDTO dto = new ConsumoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setDescripcion(descripcion);
        dto.setCantidad(cantidad);
        dto.setPrecioUnitario(precio);
        estadiaService.agregarConsumo(dto);
    }

    /** Crea una reserva y le hace check-in, devolviendo la estadía resultante. */
    private Estadia crearEstadia(int numero, LocalDate inicio, LocalDate fin) {
        Reserva reserva = crearReserva(numero, inicio, fin);
        return estadiaService.realizarCheckIn(reserva.getIdReserva());
    }

    private Reserva crearReserva(int numero, LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(ID_HUESPED);
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return reservaService.crearReserva(dto);
    }

    private String crearReservaPorHttp(int numero, LocalDate inicio, LocalDate fin) throws Exception {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(ID_HUESPED);
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);

        MvcResult resultado = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("idReserva").asText();
    }

    private Estadia crearEstadiaPorHttp(int numero, LocalDate inicio, LocalDate fin) throws Exception {
        return estadiaService.realizarCheckIn(crearReservaPorHttp(numero, inicio, fin));
    }

    /**
     * Inserta una reserva directamente en el DataStore, sin pasar por los servicios ni validaciones.
     * @param id
     * @param numero
     * @param inicio
     * @param fin
     * @return
     */
    private Reserva insertarReservaDirecta(String id, int numero, LocalDate inicio, LocalDate fin) {
        Huesped huesped = dataStore.getHuespedes().stream()
                .filter(h -> ID_HUESPED.equals(h.getIdHuesped()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontro el huesped semilla"));
        habitacionService.obtenerOFallar(numero).cambiarEstado(EstadoHabitacion.DISPONIBLE);

        Reserva reserva = new Reserva(id, inicio, fin, 2, 0, huesped,
                habitacionService.obtenerOFallar(numero));
        dataStore.getReservas().add(reserva);
        return reserva;
    }
}