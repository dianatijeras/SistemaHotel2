package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.SaldoPagoDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
import co.edu.uniquindio.sistemahotel.backend.service.PagoService;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import co.edu.uniquindio.sistemahotel.backend.service.SaldoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

/**
 * Pruebas de F-08: registro del check-out y transición de limpieza de la habitación.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckOutLimpiezaTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final String ID_HUESPED = "H-001";
    private static final int HABITACION = 101;

    /** Credenciales semilla de la recepcionista, usadas para el control de rol. */
    private static final String ID_RECEPCIONISTA = "R-001";
    private static final String USERNAME_RECEPCIONISTA = "maria.l";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private SaldoService saldoService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private HabitacionService habitacionService;

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
    @DisplayName("Realiza el check-out de una estadia con saldo cero")
    void checkOut_exitoso() {
        Estadia estadia = estadiaSaldada(3);

        Estadia finalizada = estadiaService.realizarCheckOut(estadia.getIdEstadia());

        assertThat(finalizada.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
        assertThat(finalizada.getFechaCheckOut()).as("registra fecha y hora de salida").isNotNull();
        assertThat(finalizada.getTotalFinal()).isEqualTo(450_000.0);
    }

    @Test
    @DisplayName("El check-out actualiza de forma coherente estadia, reserva y habitacion")
    void checkOut_estadosCoherentes() {
        Estadia estadia = estadiaSaldada(3);

        estadiaService.realizarCheckOut(estadia.getIdEstadia());

        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
        assertThat(estadia.getReserva().getEstadoReserva()).isEqualTo(EstadoReserva.FINALIZADA);
        assertThat(estadia.getReserva().getFechaEstadia()).as("la reserva conserva la fecha de estadia").isNotNull();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("El check-out NUNCA deja la habitacion directamente DISPONIBLE")
    void checkOut_noLiberaLaHabitacion() {
        Estadia estadia = estadiaSaldada(2);

        estadiaService.realizarCheckOut(estadia.getIdEstadia());

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("OCUPADA -> EN_LIMPIEZA, nunca -> DISPONIBLE")
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA)
                .isNotEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    @DisplayName("El check-out conserva el historial de reserva, estadia, consumos y pagos")
    void checkOut_conservaElHistorial() {
        Estadia estadia = crearEstadia(2);
        agregarConsumo(estadia.getIdEstadia(), "Minibar", 2, 25_000.50);

        assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                .as("el saldo abierto impide el check-out")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("saldo pendiente de 350001.00");

        pagarYAprobar(estadia, 300_000);
        pagarYAprobar(estadia, 50_001);

        estadiaService.realizarCheckOut(estadia.getIdEstadia());

        assertThat(dataStore.getReservas()).hasSize(1);
        assertThat(dataStore.getEstadias()).hasSize(1);
        assertThat(estadia.getConsumos()).as("los consumos no se borran").hasSize(1);
        assertThat(dataStore.getPagos()).as("los pagos no se borran").hasSize(2);
        assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("El check-out funciona con varios pagos parciales que suman el total")
    void checkOut_conVariosPagosParciales() {
        Estadia estadia = crearEstadia(4);
        assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getTotalGeneral())
                .isEqualByComparingTo("600000.00");

        pagarYAprobar(estadia, 200_000);
        pagarYAprobar(estadia, 250_000);
        pagarYAprobar(estadia, 150_000);

        SaldoPagoDTO saldo = saldoService.consultarSaldo(estadia.getIdEstadia());
        assertThat(saldo.getTotalAprobado()).isEqualByComparingTo("600000.00");
        assertThat(saldo.getSaldoPendiente()).isEqualByComparingTo("0.00");

        assertThat(estadiaService.realizarCheckOut(estadia.getIdEstadia()).getEstadoEstadia())
                .isEqualTo(EstadoEstadia.FINALIZADA);
    }


    @Test
    @DisplayName("Rechaza el check-out con saldo pendiente mayor que cero")
    void checkOut_conSaldoPendiente_esRechazado() {
        Estadia estadia = crearEstadia(2);

        assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("saldo pendiente de 300000.00");

        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
        assertThat(estadia.getFechaCheckOut()).isNull();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("sin cambios parciales: la habitacion sigue OCUPADA")
                .isEqualTo(EstadoHabitacion.OCUPADA);
    }

    @Test
    @DisplayName("Rechaza el check-out cuando hay pagos PENDIENTE que no cubren la cuenta")
    void checkOut_conPagosPendientes_esRechazado() {
        Estadia estadia = crearEstadia(2);
        registrarPago(estadia, 150_000);
        registrarPago(estadia, 150_000);

        SaldoPagoDTO saldo = saldoService.consultarSaldo(estadia.getIdEstadia());
        assertThat(saldo.getCantidadPagosPendientes()).isEqualTo(2);
        assertThat(saldo.getTotalAprobado()).isEqualByComparingTo("0.00");
        assertThat(saldo.getSaldoPendiente())
                .as("un pago PENDIENTE no descuenta nada")
                .isEqualByComparingTo("300000.00");

        assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("saldo pendiente");

        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
    }

    @Test
    @DisplayName("Rechaza el check-out cuando hay pagos RECHAZADOS")
    void checkOut_conPagosRechazados_esRechazado() {
        Estadia estadia = crearEstadia(1);
        Pago pago = registrarPago(estadia, 150_000);
        pagoService.rechazarPago(pago.getIdPago(), "Sin fondos");

        assertThat(saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                .as("un pago RECHAZADO no descuenta nada")
                .isEqualByComparingTo("150000.00");

        assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("saldo pendiente");
    }

    @Test
    @DisplayName("Rechaza el check-out cuando un consumo nuevo reabre la deuda")
    void checkOut_despuesDeAgregarConsumo_esRechazado() {
        Estadia estadia = crearEstadia(1);
        pagarYAprobar(estadia, 150_000);
        assertThat(saldoService.tieneSaldoPendiente(estadia.getIdEstadia())).isFalse();

        agregarConsumo(estadia.getIdEstadia(), "Room service", 1, 80_000);

        assertThat(saldoService.tieneSaldoPendiente(estadia.getIdEstadia())).isTrue();
        assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("80000.00");
    }

    @Test
    @DisplayName("El saldo del check-out coincide con el desglose de costos de F-09")
    void checkOut_saldoCoincideConElDesglose() {
        Estadia estadia = crearEstadia(3);
        agregarConsumo(estadia.getIdEstadia(), "Spa", 2, 45_000.25);

        BigDecimal desglose = estadiaService.calcularCosto(estadia.getIdEstadia()).getTotalGeneral();
        SaldoPagoDTO saldo = saldoService.consultarSaldo(estadia.getIdEstadia());

        assertThat(saldo.getTotalGeneral())
                .as("una sola fórmula de total: la del desglose")
                .isEqualByComparingTo(desglose);
        assertThat(saldo.getTotalGeneral()).isEqualByComparingTo("540000.50");
    }


    @Test
    @DisplayName("Rechaza el check-out de una estadia inexistente")
    void checkOut_estadiaInexistente_esRechazado() {
        assertThatThrownBy(() -> estadiaService.realizarCheckOut("EST-NO-EXISTE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe la estadía");
    }

    @Test
    @DisplayName("Rechaza el check-out de una estadia que no esta EN_CURSO")
    void checkOut_estadiaNoActiva_esRechazado() {
        Reserva reserva = crearReserva(HOY, HOY.plusDays(2));
        Estadia estadia = new Estadia("EST-CANCELADA", reserva);
        estadia.setEstadoEstadia(EstadoEstadia.CANCELADA);
        dataStore.getEstadias().add(estadia);

        assertThatThrownBy(() -> estadiaService.realizarCheckOut("EST-CANCELADA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Estado de la estadía: CANCELADA");
        assertThatThrownBy(() -> estadiaService.realizarCheckOut("EST-CANCELADA"))
                .as("tampoco existe un saldo que lo habilite")
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza un segundo check-out de la misma estadia")
    void checkOut_duplicado_esRechazado() {
        Estadia estadia = estadiaSaldada(2);
        estadiaService.realizarCheckOut(estadia.getIdEstadia());

        assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue finalizada");

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("no se repite la transicion de limpieza")
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("Rechaza el check-out si la habitacion ya no esta OCUPADA")
    void checkOut_habitacionIncoherente_esRechazado() {
        Estadia estadia = estadiaSaldada(2);
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.MANTENIMIENTO);

        assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está OCUPADA");

        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
    }


    @Test
    @DisplayName("Dos check-outs simultaneos cierran la estadia una sola vez")
    void checkOut_concurrente_cierraUnaSolaVez() throws Exception {
        Estadia estadia = estadiaSaldada(2);

        int peticiones = 8;
        ExecutorService executor = Executors.newFixedThreadPool(peticiones);
        CountDownLatch arranque = new CountDownLatch(1);
        AtomicInteger exitosos = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();
        List<Future<?>> tareas = new java.util.ArrayList<>();

        for (int i = 0; i < peticiones; i++) {
            tareas.add(executor.submit(() -> {
                try {
                    arranque.await();
                    estadiaService.realizarCheckOut(estadia.getIdEstadia());
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
            t.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertThat(exitosos.get()).as("una sola vez puede cerrar la estadia").isEqualTo(1);
        assertThat(rechazados.get()).isEqualTo(peticiones - 1);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("Un pago concurrente no puede dejar el check-out con saldo inconsistente")
    void checkOut_conPagoConcurrente_noDejaSaldoInconsistente() throws Exception {
        // Estadía de 150.000: se registra un pago por el total y, en paralelo, se intenta el
        // check-out. Cualquiera de los dos puede ganar, pero el resultado final debe ser coherente:
        // o el check-out falla por saldo, o se cierra con la cuenta saldada.
        Estadia estadia = crearEstadia(1);
        Pago pago = registrarPago(estadia, 150_000);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch arranque = new CountDownLatch(1);
        List<Future<?>> tareas = new java.util.ArrayList<>();

        tareas.add(executor.submit(() -> {
            try {
                arranque.await();
                pagoService.aprobarPago(pago.getIdPago(), "REF");
            } catch (Exception e) {
                // esperado si el check-out no procede
            }
        }));
        tareas.add(executor.submit(() -> {
            try {
                arranque.await();
                estadiaService.realizarCheckOut(estadia.getIdEstadia());
            } catch (Exception e) {
                // esperado si el pago aún no estaba contabilizado
            }
        }));

        arranque.countDown();
        for (Future<?> t : tareas) {
            t.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        BigDecimal saldo = saldoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente();
        assertThat(saldo)
                .as("nunca queda saldo negativo ni sobrepago")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(saldo).isEqualByComparingTo("0.00");

        if (estadia.getEstadoEstadia() == EstadoEstadia.FINALIZADA) {
            assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
        } else {
            assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.OCUPADA);
        }
    }


    @Test
    @DisplayName("La recepcionista confirma el fin de limpieza y la habitacion queda DISPONIBLE")
    void limpieza_finalizada_pasaADisponible() {
        Estadia estadia = estadiaSaldada(2);
        estadiaService.realizarCheckOut(estadia.getIdEstadia());
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

        Habitacion liberada = habitacionService.finalizarLimpieza(HABITACION);

        assertThat(liberada.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    @DisplayName("Rechaza finalizar la limpieza desde OCUPADA")
    void limpieza_desdeOcupada_esRechazada() {
        crearEstadia(1);

        assertThatThrownBy(() -> habitacionService.finalizarLimpieza(HABITACION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está EN_LIMPIEZA")
                .hasMessageContaining("OCUPADA");

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.OCUPADA);
    }

    @Test
    @DisplayName("Rechaza finalizar la limpieza desde MANTENIMIENTO y FUERA_DE_SERVICIO")
    void limpieza_desdeEstadosBloqueados_esRechazada() {
        for (EstadoHabitacion estado : List.of(EstadoHabitacion.MANTENIMIENTO,
                EstadoHabitacion.FUERA_DE_SERVICIO)) {
            habitacionService.obtenerOFallar(HABITACION).cambiarEstado(estado);

            assertThatThrownBy(() -> habitacionService.finalizarLimpieza(HABITACION))
                    .as("estado " + estado)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está EN_LIMPIEZA");

            assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                    .isEqualTo(estado);
        }
    }

    @Test
    @DisplayName("Rechaza finalizar la limpieza si existe una estadia activa en la habitacion")
    void limpieza_conEstadiaActiva_esRechazada() {
        crearEstadia(1);
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);
        assertThat(dataStore.getEstadias()).as("la estadia activa sigue existiendo").hasSize(1);

        assertThatThrownBy(() -> habitacionService.finalizarLimpieza(HABITACION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("todavía tiene una estadía activa");

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("Rechaza una segunda confirmacion de limpieza sin efectos secundarios")
    void limpieza_confirmacionRepetida_esRechazada() {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);
        habitacionService.finalizarLimpieza(HABITACION);

        assertThatThrownBy(() -> habitacionService.finalizarLimpieza(HABITACION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está EN_LIMPIEZA");

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("la habitacion sigue DISPONIBLE, sin rebotes")
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    @DisplayName("marcarDisponible aplica la misma regla que finalizarLimpieza")
    void limpieza_marcarDisponible_delega() {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        assertThat(habitacionService.marcarDisponible(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
        assertThatThrownBy(() -> habitacionService.marcarDisponible(HABITACION))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("El endpoint generico de estado no puede liberar una habitacion ocupada")
    void limpieza_endpointGenericoNoLibera() {
        crearEstadia(1);

        assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION, EstadoHabitacion.DISPONIBLE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("solo se libera confirmando");

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.OCUPADA);
    }


    @Test
    @DisplayName("PUT /api/habitaciones/{n}/finalizar-limpieza exige rol de recepcionista")
    void autorizacion_exigeRecepcionista() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult sinRol = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza"))
                .andReturn();
        assertThat(sinRol.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpoDe(sinRol)).contains("Solo la recepción");

        MvcResult rolFalso = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", "R-999")
                        .header("X-Username", "inexistente"))
                .andReturn();
        assertThat(rolFalso.getResponse().getStatus())
                .as("declarar el rol no basta: el usuario debe existir")
                .isEqualTo(409);
        assertThat(cuerpoDe(rolFalso)).contains("Solo la recepción");

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("ningun rechazo libera la habitacion")
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("PUT /api/habitaciones/{n}/finalizar-limpieza acepta a la recepcionista")
    void autorizacion_aceptaRecepcionista() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult resultado = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", ID_RECEPCIONISTA)
                        .header("X-Username", USERNAME_RECEPCIONISTA))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("\"estadoHabitacion\":\"DISPONIBLE\"");
    }


    @Test
    @DisplayName("PUT /api/estadias/{id}/checkout devuelve 200 con la estadia finalizada")
    void endpointCheckOut_devuelve200() throws Exception {
        Estadia estadia = estadiaSaldada(2);

        MvcResult resultado = mockMvc.perform(
                put("/api/estadias/" + estadia.getIdEstadia() + "/checkout")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"estadoEstadia\":\"FINALIZADA\"");
        assertThat(cuerpo).contains("\"estadoReserva\":\"FINALIZADA\"");
        assertThat(cuerpo).contains("\"estadoHabitacion\":\"EN_LIMPIEZA\"");
        assertThat(cuerpo).doesNotContain("\"password\"").doesNotContain("pass123");
    }

    @Test
    @DisplayName("PUT /api/estadias/{id}/checkout responde 409 con el saldo pendiente")
    void endpointCheckOut_conSaldo_devuelve409() throws Exception {
        Estadia estadia = crearEstadia(2);

        MvcResult resultado = mockMvc.perform(
                put("/api/estadias/" + estadia.getIdEstadia() + "/checkout")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("saldo pendiente de 300000.00");
    }

    @Test
    @DisplayName("PUT /api/estadias/{id}/checkout responde 400 si la estadia no existe")
    void endpointCheckOut_inexistente_devuelve400() throws Exception {
        MvcResult resultado = mockMvc.perform(put("/api/estadias/EST-X/checkout")).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Las respuestas del check-out no incluyen contrasenas")
    void checkOut_noExponeContrasenas() throws Exception {
        Estadia estadia = estadiaSaldada(2);
        mockMvc.perform(put("/api/estadias/" + estadia.getIdEstadia() + "/checkout")).andReturn();

        for (String ruta : List.of("/api/estadias", "/api/estadias/para-pagos", "/api/pagos")) {
            MvcResult resultado = mockMvc.perform(get(ruta)).andReturn();
            assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
            String cuerpo = resultado.getResponse().getContentAsString();
            assertThat(cuerpo).as(ruta).doesNotContain("\"password\"");
            assertThat(cuerpo).as(ruta).doesNotContain("pass123");
        }
    }


    @Test
    @DisplayName("El flujo completo check-in, consumos, pagos y check-out funciona encadenado")
    void regresion_flujoCompleto() {
        Reserva reserva = crearReserva(HOY, HOY.plusDays(2));
        Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.OCUPADA);

        agregarConsumo(estadia.getIdEstadia(), "Minibar", 2, 25_000.50);
        agregarConsumo(estadia.getIdEstadia(), "Spa", 1, 100_000.00);

        SaldoPagoDTO saldo = saldoService.consultarSaldo(estadia.getIdEstadia());
        assertThat(saldo.getTotalGeneral()).isEqualByComparingTo("450001.00");
        assertThat(saldo.getSaldoPendiente()).isEqualByComparingTo("450001.00");

        pagarYAprobar(estadia, 200_000);
        pagarYAprobar(estadia, 100_000);
        assertThat(saldoService.tieneSaldoPendiente(estadia.getIdEstadia())).isTrue();
        assertThatThrownBy(() -> estadiaService.realizarCheckOut(estadia.getIdEstadia()))
                .hasMessageContaining("saldo pendiente de 150001.00");

        pagarYAprobar(estadia, 150_001);
        assertThat(saldoService.tieneSaldoPendiente(estadia.getIdEstadia())).isFalse();

        estadiaService.realizarCheckOut(estadia.getIdEstadia());
        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

        habitacionService.finalizarLimpieza(HABITACION);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);

        assertThat(dataStore.getPagos()).hasSize(3);
        assertThat(estadia.getConsumos()).hasSize(2);
        assertThat(dataStore.getReservas()).hasSize(1);
    }

    @Test
    @DisplayName("Tras el check-out y la limpieza la habitacion puede volver a reservarse")
    void regresion_habitacionVuelveAReservarse() {
        Estadia estadia = estadiaSaldada(1);
        estadiaService.realizarCheckOut(estadia.getIdEstadia());
        habitacionService.finalizarLimpieza(HABITACION);

        Reserva nueva = reservaService.crearReserva(dtoReserva(HOY.plusDays(10), HOY.plusDays(12)));

        assertThat(nueva.getHabitacion().getNumeroHabitacion()).isEqualTo(HABITACION);
        assertThat(nueva.getEstadoReserva())
                .as("la habitacion liberada vuelve a estar disponible para reservar")
                .isEqualTo(EstadoReserva.RESERVADA);
    }

    @Test
    @DisplayName("No se puede hacer check-in en una habitacion EN_LIMPIEZA")
    void regresion_checkInBloqueadoEnLimpieza() {
        Reserva reserva = crearReserva(HOY, HOY.plusDays(2));
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EN_LIMPIEZA");

        assertThat(dataStore.getEstadias()).isEmpty();
        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);
    }

    @Test
    @DisplayName("Las consultas previas siguen respondiendo tras el cierre")
    void regresion_consultasSiguenIgual() throws Exception {
        Estadia estadia = estadiaSaldada(2);
        mockMvc.perform(put("/api/estadias/" + estadia.getIdEstadia() + "/checkout")).andReturn();

        assertThat(mockMvc.perform(get("/api/estadias")).andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(mockMvc.perform(get("/api/estadias/para-pagos")).andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(mockMvc.perform(get("/api/reservas")).andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(mockMvc.perform(get("/api/habitaciones")).andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(estadiaService.listarEnCurso())
                .as("la estadia cerrada ya no esta en curso")
                .hasSize(0);
        assertThat(estadiaService.listarParaPagos()).hasSize(1);
    }


    /**
     * Lee el cuerpo de la respuesta HTTP como String UTF-8.
     * @param resultado
     * @return
     * @throws Exception
     */
    private String cuerpoDe(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private ReservaRequestDTO dtoReserva(LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(ID_HUESPED);
        dto.setNumeroHabitacion(HABITACION);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return dto;
    }

    private Reserva crearReserva(LocalDate inicio, LocalDate fin) {
        return reservaService.crearReserva(dtoReserva(inicio, fin));
    }

    private Estadia crearEstadia(int noches) {
        return estadiaService.realizarCheckIn(crearReserva(HOY, HOY.plusDays(noches)).getIdReserva());
    }

    /** Registra un pago PENDIENTE por el importe indicado. */
    private Pago registrarPago(Estadia estadia, double monto) {
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(estadia.getIdEstadia());
        dto.setMonto(monto);
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Abono de prueba");
        return pagoService.registrarPago(dto);
    }

    /** Registra el pago indicado y lo aprueba de inmediato. */
    private void pagarYAprobar(Estadia estadia, double monto) {
        Pago pago = registrarPago(estadia, monto);
        pagoService.aprobarPago(pago.getIdPago(), "REF-TEST");
    }

    /** Salda la cuenta por el importe indicado en una sola operación. */
    private void pagar(Estadia estadia, double monto) {
        registrarPago(estadia, monto);
    }

    private void agregarConsumo(String idEstadia, String descripcion, int cantidad, double precio) {
        ConsumoRequestDTO dto = new ConsumoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setDescripcion(descripcion);
        dto.setCantidad(cantidad);
        dto.setPrecioUnitario(precio);
        estadiaService.agregarConsumo(dto);
    }

    /** Crea una estadía con la cuenta completamente saldada, lista para el check-out. */
    private Estadia estadiaSaldada(int noches) {
        Estadia estadia = crearEstadia(noches);
        pagarYAprobar(estadia, estadiaService.calcularCosto(estadia.getIdEstadia())
                .getTotalGeneral().doubleValue());
        assertThat(saldoService.tieneSaldoPendiente(estadia.getIdEstadia())).isFalse();
        return estadia;
    }
}