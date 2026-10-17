package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaActualizacionDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
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

import java.time.LocalDate;
import java.util.ArrayList;
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
class ModificarReservaTest {

    private static final LocalDate ENTRADA = LocalDate.now().plusDays(30);
    private static final LocalDate SALIDA = LocalDate.now().plusDays(33);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

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
        dataStore.getHabitaciones().forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
    }


    @Test
    @DisplayName("Modifica correctamente las fechas de una reserva")
    void modificar_fechas_devuelve201() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        Reserva modificada = reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(0, ENTRADA.plusDays(5), SALIDA.plusDays(8)));

        assertThat(modificada.getFechaInicio()).isEqualTo(ENTRADA.plusDays(5));
        assertThat(modificada.getFechaFin()).isEqualTo(SALIDA.plusDays(8));
        assertThat(dataStore.getReservas()).hasSize(1);
    }

    @Test
    @DisplayName("Al modificar fechas se conservan id, codigo y huesped")
    void modificar_conservaIdCodigoYHuesped() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);
        String idAntes = reserva.getIdReserva();
        String codigoAntes = reserva.getCodigoReserva();
        String huespedAntes = reserva.getHuesped().getIdHuesped();

        Reserva modificada = reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(0, ENTRADA.plusDays(2), SALIDA.plusDays(4)));

        assertThat(modificada.getIdReserva()).isEqualTo(idAntes);
        assertThat(modificada.getCodigoReserva()).isEqualTo(codigoAntes);
        assertThat(modificada.getHuesped().getIdHuesped()).isEqualTo(huespedAntes);
        assertThat(modificada.getHabitacion().getNumeroHabitacion()).isEqualTo(101);
    }

    @Test
    @DisplayName("Modificar solo la fecha de salida conserva la de entrada")
    void modificar_soloUnaFecha() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        Reserva modificada = reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(0, null, SALIDA.plusDays(4)));

        assertThat(modificada.getFechaInicio()).isEqualTo(ENTRADA);
        assertThat(modificada.getFechaFin()).isEqualTo(SALIDA.plusDays(4));
    }



    @Test
    @DisplayName("Cambia la habitacion asignada si esta disponible")
    void modificar_habitacion_devuelve201() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        Reserva modificada = reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(102, ENTRADA, SALIDA));

        assertThat(modificada.getHabitacion().getNumeroHabitacion()).isEqualTo(102);
        assertThat(modificada.getCantidadPersonas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cambia fechas y habitacion a la vez")
    void modificar_fechasYHabitacion() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        Reserva modificada = reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(201, ENTRADA.plusDays(10), SALIDA.plusDays(12)));

        assertThat(modificada.getHabitacion().getNumeroHabitacion()).isEqualTo(201);
        assertThat(modificada.getFechaInicio()).isEqualTo(ENTRADA.plusDays(10));
        assertThat(modificada.getFechaFin()).isEqualTo(SALIDA.plusDays(12));
    }

    @Test
    @DisplayName("Modificar la misma habitacion con otras fechas no genera conflicto consigo misma")
    void modificar_mismaHabitacion_noSeChocaConsigoMisma() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        Reserva modificada = reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(101, ENTRADA.plusDays(1), SALIDA.plusDays(2)));

        assertThat(modificada.getHabitacion().getNumeroHabitacion()).isEqualTo(101);
        assertThat(modificada.getFechaInicio()).isEqualTo(ENTRADA.plusDays(1));
    }


    @Test
    @DisplayName("Rechaza fechas invalidas: salida no posterior a la entrada")
    void modificar_fechasInvalidas_esRechazado() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(0, ENTRADA, ENTRADA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @DisplayName("Rechaza una fecha de entrada en el pasado")
    void modificar_fechaEnElPasado_esRechazada() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(0, LocalDate.now().minusDays(1), SALIDA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pasado");
    }

    @Test
    @DisplayName("Rechaza una habitacion que no existe")
    void modificar_habitacionInexistente_esRechazada() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(999, ENTRADA, SALIDA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Rechaza una habitacion sin capacidad para la reserva")
    void modificar_habitacionSinCapacidad_esRechazada() {
        // La 301 tiene capacidad 3, así que una reserva de 3 personas sí se puede crear ahí,
        // pero no cabe en la 101 ni en la 102, que tienen capacidad 2.
        Reserva reserva = crearReserva(301, ENTRADA, SALIDA, 3, 0);

        assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(101, ENTRADA, SALIDA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacidad");

        assertThat(reservaService.obtenerOFallar(reserva.getIdReserva())
                .getHabitacion().getNumeroHabitacion()).isEqualTo(301);
    }

    @Test
    @DisplayName("Rechaza una modificacion que produce solapamiento con otra reserva")
    void modificar_solapamiento_esRechazado() {
        Reserva primera = crearReserva(101, ENTRADA, SALIDA);
        crearReserva(101, ENTRADA.plusDays(10), SALIDA.plusDays(12));

        assertThatThrownBy(() -> reservaService.modificarReserva(primera.getIdReserva(),
                actualizacion(101, ENTRADA.plusDays(11), SALIDA.plusDays(13))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reserva activa");
    }

    @Test
    @DisplayName("Una modificacion rechazada no altera parcialmente la reserva")
    void modificar_rechazada_noDejaCambiosParciales() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        crearReserva(201, ENTRADA.plusDays(10), SALIDA.plusDays(12));

        assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(201, ENTRADA.plusDays(11), SALIDA.plusDays(13))))
                .isInstanceOf(IllegalStateException.class);

        Reserva intacta = reservaService.obtenerOFallar(reserva.getIdReserva());
        assertThat(intacta.getFechaInicio()).isEqualTo(ENTRADA);
        assertThat(intacta.getFechaFin()).isEqualTo(SALIDA);
        assertThat(intacta.getHabitacion().getNumeroHabitacion()).isEqualTo(101);
    }

    @Test
    @DisplayName("Rechaza modificar o cancelar una reserva inexistente")
    void reservaInexistente_esRechazada() {
        assertThatThrownBy(() -> reservaService.modificarReserva("RES-NO-EXISTE",
                actualizacion(102, ENTRADA, SALIDA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe la reserva");

        assertThatThrownBy(() -> reservaService.cancelarReserva("RES-NO-EXISTE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe la reserva");
    }



    @Test
    @DisplayName("Rechaza modificar reservas CANCELADA, NO_SHOW, CHECKED_IN y FINALIZADA")
    void modificar_estadosNoPermitidos_sonRechazados() {
        List<EstadoReserva> bloqueados = List.of(
                EstadoReserva.CANCELADA, EstadoReserva.NO_SHOW,
                EstadoReserva.CHECKED_IN, EstadoReserva.FINALIZADA);

        for (EstadoReserva estado : bloqueados) {
            dataStore.getReservas().clear();
            Reserva reserva = crearReserva(101, ENTRADA, SALIDA);
            reserva.setEstadoReserva(estado);

            assertThatThrownBy(() -> reservaService.modificarReserva(reserva.getIdReserva(),
                    actualizacion(102, ENTRADA.plusDays(3), SALIDA.plusDays(5))))
                    .as("estado " + estado)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RESERVADA o CONFIRMADA");

            assertThat(reservaService.obtenerOFallar(reserva.getIdReserva()).getHabitacion()
                    .getNumeroHabitacion()).isEqualTo(101);
        }
    }

    @Test
    @DisplayName("Permite modificar una reserva CONFIRMADA")
    void modificar_reservaConfirmada_esPermitido() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);
        reservaService.confirmarReserva(reserva.getIdReserva());
        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);

        Reserva modificada = reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(102, ENTRADA.plusDays(1), SALIDA.plusDays(3)));

        assertThat(modificada.getHabitacion().getNumeroHabitacion()).isEqualTo(102);
        assertThat(modificada.getEstadoReserva())
                .as("modificar no debe cambiar el estado")
                .isEqualTo(EstadoReserva.CONFIRMADA);
    }



    @Test
    @DisplayName("Cancela una reserva y conserva el registro historico")
    void cancelar_conservaElHistorial() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        Reserva cancelada = reservaService.cancelarReserva(reserva.getIdReserva());

        assertThat(cancelada.getEstadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(dataStore.getReservas())
                .as("la reserva no se elimina, solo cambia de estado")
                .hasSize(1);
        assertThat(reservaService.buscarPorId(reserva.getIdReserva())).isPresent();
    }

    @Test
    @DisplayName("Cancelar libera el intervalo para una nueva reserva")
    void cancelar_liberaElIntervalo() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);
        reservaService.cancelarReserva(reserva.getIdReserva());

        Reserva nueva = reservaService.crearReserva(solicitud(101, ENTRADA, SALIDA));

        assertThat(nueva.getIdReserva()).isNotEqualTo(reserva.getIdReserva());
        assertThat(dataStore.getReservas()).hasSize(2);
    }

    @Test
    @DisplayName("Una segunda cancelacion se rechaza y no produce efectos adicionales")
    void cancelar_repetida_esRechazada() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);
        reservaService.cancelarReserva(reserva.getIdReserva());

        assertThatThrownBy(() -> reservaService.cancelarReserva(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESERVADA o CONFIRMADA");

        assertThat(dataStore.getReservas()).hasSize(1);
        assertThat(reservaService.obtenerOFallar(reserva.getIdReserva()).getEstadoReserva())
                .isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("No se puede cancelar una reserva con check-in, finalizada o no-show")
    void cancelar_estadosNoPermitidos_sonRechazados() {
        List<EstadoReserva> bloqueados = List.of(
                EstadoReserva.NO_SHOW, EstadoReserva.CHECKED_IN, EstadoReserva.FINALIZADA);

        for (EstadoReserva estado : bloqueados) {
            dataStore.getReservas().clear();
            Reserva reserva = crearReserva(101, ENTRADA, SALIDA);
            reserva.setEstadoReserva(estado);

            assertThatThrownBy(() -> reservaService.cancelarReserva(reserva.getIdReserva()))
                    .as("estado " + estado)
                    .isInstanceOf(IllegalStateException.class);

            assertThat(reservaService.obtenerOFallar(reserva.getIdReserva()).getEstadoReserva())
                    .isEqualTo(estado);
        }
    }

    @Test
    @DisplayName("Cancelar no cambia el estado de una habitacion OCUPADA ni EN_LIMPIEZA")
    void cancelar_noCambiaElEstadoDeLaHabitacion() {
        for (EstadoHabitacion estado : List.of(EstadoHabitacion.OCUPADA, EstadoHabitacion.EN_LIMPIEZA)) {
            Reserva reserva = crearReserva(101, ENTRADA, SALIDA);
            Habitacion habitacion = habitacionService.obtenerOFallar(101);
            habitacion.cambiarEstado(estado);

            reservaService.cancelarReserva(reserva.getIdReserva());

            assertThat(habitacion.getEstadoHabitacion())
                    .as("cancelar no debe liberar físicamente la habitación")
                    .isEqualTo(estado);
        }
    }

    @Test
    @DisplayName("Una reserva cancelable nunca tiene estadias ni pagos asociados")
    void cancelar_noAfectaEstadiasNiPagos() {
        Reserva cancelable = crearReserva(101, ENTRADA, SALIDA);

        assertThat(dataStore.getEstadias()).as("una reserva sin check-in no tiene estadias").isEmpty();
        assertThat(dataStore.getPagos()).as("no existen pagos sin estadia").isEmpty();

        reservaService.cancelarReserva(cancelable.getIdReserva());

        assertThat(dataStore.getEstadias()).isEmpty();
        assertThat(dataStore.getPagos()).isEmpty();
    }



    @Test
    @DisplayName("Tras una modificacion se puede reservar el intervalo liberado")
    void modificar_liberaElIntervaloAnterior() {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);

        reservaService.modificarReserva(reserva.getIdReserva(),
                actualizacion(101, ENTRADA.plusDays(20), SALIDA.plusDays(23)));

        Reserva nueva = reservaService.crearReserva(solicitud(101, ENTRADA, SALIDA));

        assertThat(nueva.getIdReserva()).isNotEqualTo(reserva.getIdReserva());
    }



    @Test
    @DisplayName("Dos modificaciones simultaneas al mismo intervalo dejan una sola reserva")
    void modificar_simultaneo_noCreaSolapamientos() throws Exception {
        // Dos reservas en habitaciones distintas pero con las MISMAS fechas. Diez hilos intentan
        // llevar cada una a la habitacion 302 en ese mismo intervalo: solo una puede ganar.
        Reserva primera = crearReserva(101, ENTRADA, SALIDA);
        Reserva segunda = crearReserva(102, ENTRADA, SALIDA);

        int peticiones = 10;
        ExecutorService executor = Executors.newFixedThreadPool(peticiones);
        CountDownLatch arranque = new CountDownLatch(1);
        AtomicInteger exitosas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < peticiones; i++) {
            final String id = (i % 2 == 0) ? primera.getIdReserva() : segunda.getIdReserva();
            tareas.add(executor.submit(() -> {
                try {
                    arranque.await();
                    reservaService.modificarReserva(id, actualizacion(302, ENTRADA, SALIDA));
                    exitosas.incrementAndGet();
                } catch (IllegalStateException e) {
                    rechazadas.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        arranque.countDown();
        for (Future<?> t : tareas) {
            t.get(20, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        long enLa302 = dataStore.getReservas().stream()
                .filter(r -> r.getHabitacion().getNumeroHabitacion() == 302)
                .count();


        long solapamientos = dataStore.getReservas().stream()
                .filter(r -> r.getEstadoReserva() == EstadoReserva.RESERVADA
                        || r.getEstadoReserva() == EstadoReserva.CONFIRMADA)
                .filter(a -> dataStore.getReservas().stream().anyMatch(b ->
                        !a.getIdReserva().equals(b.getIdReserva())
                                && a.getHabitacion().getNumeroHabitacion() == b.getHabitacion().getNumeroHabitacion()
                                && ENTRADA.isBefore(b.getFechaFin()) && SALIDA.isAfter(b.getFechaInicio())))
                .count();

        assertThat(solapamientos)
                .as("ninguna habitacion puede quedar con dos reservas activas superpuestas")
                .isZero();
        assertThat(enLa302).as("solo una reserva puede terminar en la 302").isEqualTo(1);
        assertThat(exitosas.get()).as("al menos una modificacion debe prosperar").isGreaterThanOrEqualTo(1);
        assertThat(rechazadas.get()).as("debe detectarse la contencion").isGreaterThanOrEqualTo(1);
        assertThat(dataStore.getReservas())
                .as("no se pueden perder ni duplicar reservas")
                .hasSize(2);
    }



    @Test
    @DisplayName("PUT /api/reservas/{id} modifica y devuelve 200")
    void endpointModificar_devuelve200() throws Exception {
        String id = crearReservaPorHttp(101, ENTRADA, SALIDA);

        MvcResult resultado = mockMvc.perform(put("/api/reservas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroHabitacion\":102,\"fechaInicio\":\"" + ENTRADA.plusDays(3)
                                + "\",\"fechaFin\":\"" + SALIDA.plusDays(5) + "\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"numeroHabitacion\":102");
        assertThat(cuerpo).contains("\"idReserva\":\"" + id + "\"");
    }

    @Test
    @DisplayName("PUT /api/reservas/{id} responde 400 con fechas invalidas")
    void endpointModificar_fechasInvalidas_devuelve400() throws Exception {
        String id = crearReservaPorHttp(101, ENTRADA, SALIDA);

        MvcResult resultado = mockMvc.perform(put("/api/reservas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\":\"" + ENTRADA + "\",\"fechaFin\":\"" + ENTRADA + "\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("mensaje");
    }

    @Test
    @DisplayName("PUT /api/reservas/{id} responde 409 con solapamiento")
    void endpointModificar_solapamiento_devuelve409() throws Exception {
        String id = crearReservaPorHttp(101, ENTRADA, SALIDA);
        crearReservaPorHttp(101, ENTRADA.plusDays(10), SALIDA.plusDays(12));

        MvcResult resultado = mockMvc.perform(put("/api/reservas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroHabitacion\":101,\"fechaInicio\":\"" + ENTRADA.plusDays(11)
                                + "\",\"fechaFin\":\"" + SALIDA.plusDays(13) + "\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("PUT /api/reservas/{id} responde 409 si el estado no lo permite")
    void endpointModificar_estadoInvalido_devuelve409() throws Exception {
        Reserva reserva = crearReserva(101, ENTRADA, SALIDA);
        reservaService.cancelarReserva(reserva.getIdReserva());

        MvcResult resultado = mockMvc.perform(put("/api/reservas/" + reserva.getIdReserva())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroHabitacion\":102}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("PUT /api/reservas/{id} responde 400 si la reserva no existe")
    void endpointModificar_noExiste_devuelve400() throws Exception {
        MvcResult resultado = mockMvc.perform(put("/api/reservas/RES-NO-EXISTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroHabitacion\":102}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("PUT /api/reservas/{id}/cancelar responde 409 al repetir la cancelacion")
    void endpointCancelar_repetida_devuelve409() throws Exception {
        String id = crearReservaPorHttp(101, ENTRADA, SALIDA);

        mockMvc.perform(put("/api/reservas/" + id + "/cancelar")).andReturn();
        MvcResult repetida = mockMvc.perform(put("/api/reservas/" + id + "/cancelar")).andReturn();

        assertThat(repetida.getResponse().getStatus()).isEqualTo(409);
        assertThat(dataStore.getReservas()).hasSize(1);
    }

    @Test
    @DisplayName("La disponibilidad de F-02 sigue intacta y admite excluir una reserva")
    void disponibilidad_retrocompatibleYadmiteExclusion() throws Exception {
        String id = crearReservaPorHttp(101, ENTRADA, SALIDA);


        String sinExcluir = cuerpo(200, get("/api/habitaciones/disponibles")
                .param("fechaInicio", ENTRADA.toString())
                .param("fechaFin", SALIDA.toString()));
        assertThat(sinExcluir).doesNotContain("\"numeroHabitacion\":101");


        String sinFechas = cuerpo(200, get("/api/habitaciones/disponibles"));
        assertThat(sinFechas).contains("\"numeroHabitacion\":101");


        String excluyendo = cuerpo(200, get("/api/habitaciones/disponibles")
                .param("fechaInicio", ENTRADA.toString())
                .param("fechaFin", SALIDA.toString())
                .param("excluirReserva", id));
        assertThat(excluyendo).contains("\"numeroHabitacion\":101");
    }

    @Test
    @DisplayName("Las respuestas de reservas y habitaciones no incluyen contrasenas")
    void respuestas_noExponeContrasenas() throws Exception {
        String id = crearReservaPorHttp(101, ENTRADA, SALIDA);

        for (String ruta : List.of("/api/reservas", "/api/reservas/" + id, "/api/habitaciones")) {
            String cuerpo = cuerpo(200, get(ruta));
            assertThat(cuerpo).as(ruta).doesNotContain("\"password\"");
            assertThat(cuerpo).as(ruta).doesNotContain("pass123");
        }
    }



    private String cuerpo(int esperado, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion)
            throws Exception {
        MvcResult resultado = mockMvc.perform(peticion).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(esperado);
        return resultado.getResponse().getContentAsString();
    }

    private String crearReservaPorHttp(int numero, LocalDate inicio, LocalDate fin) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud(numero, inicio, fin))))
                .andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("idReserva").asText();
    }

    private Reserva crearReserva(int numero, LocalDate inicio, LocalDate fin) {
        return crearReserva(numero, inicio, fin, 2, 0);
    }

    private Reserva crearReserva(int numero, LocalDate inicio, LocalDate fin, int adultos, int ninos) {
        ReservaRequestDTO dto =
                new ReservaRequestDTO();
        dto.setIdHuesped("H-001");
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(adultos);
        dto.setNinos(ninos);
        return reservaService.crearReserva(dto);
    }

    private ReservaRequestDTO solicitud(
            int numero, LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto =
                new ReservaRequestDTO();
        dto.setIdHuesped("H-001");
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return dto;
    }

    private ReservaActualizacionDTO actualizacion(int numero, LocalDate inicio, LocalDate fin) {
        ReservaActualizacionDTO dto = new ReservaActualizacionDTO();
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        return dto;
    }
}