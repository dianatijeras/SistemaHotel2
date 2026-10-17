package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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

/**
 * Pruebas de F-06: realizar el check-in del huésped.
 * Verifica el registro de la estadía, los cambios de estado de reserva/estadía/habitación
 * y todos los rechazos exigidos por la regla de negocio.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CheckInTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final LocalDate MAÑANA = LocalDate.now().plusDays(1);
    private static final LocalDate PASADO = LocalDate.now().minusDays(10);

    private static final String ID_HUESPED = "H-001";
    private static final int HABITACION = 101;
    private static final String ID_HUESPED_PRUEBA = "H-PRUEBA";

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones().forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
        dataStore.getHuespedes().removeIf(h -> h.getIdHuesped().startsWith(ID_HUESPED_PRUEBA));
    }


    @Test
    @DisplayName("Realiza el check-in y devuelve la estadia creada")
    void checkIn_exitoso_devuelveEstadia() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);

        Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(estadia.getIdEstadia()).startsWith("EST-");
        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
        assertThat(estadia.getFechaCheckIn()).as("registra fecha y hora reales").isNotNull();
        assertThat(estadia.getReserva().getIdReserva()).isEqualTo(reserva.getIdReserva());
        assertThat(dataStore.getEstadias()).hasSize(1);
    }

    @Test
    @DisplayName("El check-in actualiza los estados de reserva, estadia y habitacion")
    void checkIn_actualizaLosTresEstados() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);

        estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CHECKED_IN);
        assertThat(reserva.getFechaEstadia()).isEqualTo(HOY);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("la habitacion queda OCUPADA")
                .isEqualTo(EstadoHabitacion.OCUPADA);
        assertThat(dataStore.getEstadias().get(0).getEstadoEstadia())
                .isEqualTo(EstadoEstadia.EN_CURSO);
    }

    @Test
    @DisplayName("El check-in no crea una segunda estadia activa para la misma reserva")
    void checkIn_noCreaDosEstadias() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        estadiaService.realizarCheckIn(reserva.getIdReserva());

        long activas = dataStore.getEstadias().stream()
                .filter(e -> e.getReserva().getIdReserva().equals(reserva.getIdReserva())
                        && e.getEstadoEstadia() == EstadoEstadia.EN_CURSO)
                .count();

        assertThat(activas).isEqualTo(1);
    }

    @Test
    @DisplayName("El check-in expone el documento real del huesped y no su id interno")
    void checkIn_verificaElDocumentoDelHuesped() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);

        assertThat(reserva.getHuesped().getDocumento())
                .as("el documento NO es el identificador interno")
                .isEqualTo("CC10000001")
                .isNotEqualTo(reserva.getHuesped().getIdHuesped());

        Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(estadia.getReserva().getHuesped().getDocumento()).isEqualTo("CC10000001");
    }

    @Test
    @DisplayName("Rechaza el check-in si el huesped no tiene documento de identidad")
    void checkIn_sinDocumento_esRechazado() {
        Huesped huesped = registrarHuespedDePrueba(ID_HUESPED_PRUEBA + "-SIN-DOC", "   ");
        Reserva reserva = crearReservaConHuesped(huesped, HABITACION, HOY, MAÑANA);

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("documento");

        assertThat(dataStore.getEstadias()).isEmpty();
        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    @DisplayName("Rechaza el check-in si la reserva no tiene huesped asociado")
    void checkIn_sinHuesped_esRechazado() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        reserva.setHuesped(null);

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("huésped asociado");

        assertThat(dataStore.getEstadias()).isEmpty();
    }

    @Test
    @DisplayName("El check-in no cambia el huesped vinculado a la reserva")
    void checkIn_noCambiaElHuesped() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        String huespedAntes = reserva.getHuesped().getIdHuesped();

        estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(reserva.getHuesped().getIdHuesped()).isEqualTo(huespedAntes);
    }


    @Test
    @DisplayName("Rechaza el check-in en reservas CANCELADA, NO_SHOW, CHECKED_IN y FINALIZADA")
    void checkIn_estadosNoPermitidos_sonRechazados() {
        List<EstadoReserva> bloqueados = List.of(
                EstadoReserva.CANCELADA, EstadoReserva.NO_SHOW,
                EstadoReserva.CHECKED_IN, EstadoReserva.FINALIZADA);

        for (EstadoReserva estado : bloqueados) {
            reiniciarAlmacen();

            Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
            reserva.setEstadoReserva(estado);

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .as("estado " + estado)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No se puede hacer check-in");

            assertThat(dataStore.getEstadias()).as("estado " + estado).isEmpty();
            assertThat(reserva.getEstadoReserva()).as("estado " + estado).isEqualTo(estado);
        }
    }

    @Test
    @DisplayName("Permite el check-in de una reserva CONFIRMADA")
    void checkIn_reservaConfirmada_esPermitido() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        reservaService.confirmarReserva(reserva.getIdReserva());

        Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(estadia.getIdEstadia()).isNotBlank();
    }

    @Test
    @DisplayName("Rechaza el check-in de una reserva inexistente")
    void checkIn_reservaInexistente_esRechazado() {
        assertThatThrownBy(() -> estadiaService.realizarCheckIn("RES-NO-EXISTE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe la reserva");
    }

    @Test
    @DisplayName("Rechaza el check-in sin indicar la reserva")
    void checkIn_sinId_esRechazado() {
        assertThatThrownBy(() -> estadiaService.realizarCheckIn("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Debe indicar la reserva");
    }


    @Test
    @DisplayName("Rechaza un segundo check-in de la misma reserva")
    void checkIn_duplicado_esRechazado() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(dataStore.getEstadias()).hasSize(1);
    }

    @Test
    @DisplayName("Rechaza el check-in si la habitacion ya tiene otra estadia activa")
    void checkIn_habitacionConOtraEstadia_esRechazado() {
        Reserva primera = crearReserva(HABITACION, HOY, MAÑANA);
        estadiaService.realizarCheckIn(primera.getIdReserva());

        Reserva segunda = insertarReservaDirecta("RES-COINCIDENTE", HABITACION, HOY, MAÑANA);
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.DISPONIBLE);

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(segunda.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya tiene una estadía activa");

        assertThat(dataStore.getEstadias()).hasSize(1);
        assertThat(segunda.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);
    }


    @Test
    @DisplayName("Rechaza el check-in antes de la fecha de entrada")
    void checkIn_antesDeLaFechaDeEntrada_esRechazado() {
        Reserva reserva = crearReserva(HABITACION, MAÑANA, MAÑANA.plusDays(2));

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("antes de la fecha de entrada");

        assertThat(dataStore.getEstadias()).isEmpty();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("la habitacion no debe marcarse OCUPADA")
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    @DisplayName("Rechaza el check-in cuando la fecha de salida ya paso")
    void checkIn_despuesDeLaFechaDeSalida_esRechazado() {
        Reserva reserva = insertarReservaDirecta("RES-VENCIDA", HABITACION, PASADO, PASADO.plusDays(1));

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("día de salida ni después");

        assertThat(dataStore.getEstadias()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza el check-in el mismo dia de la fecha de salida (ajuste F-09)")
    void checkIn_elDiaDeLaFechaDeSalida_esRechazado() {
        Reserva reserva = insertarReservaDirecta("RES-SALIDA-HOY", HABITACION, PASADO, HOY);

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede hacer check-in el día de salida ni después")
                .hasMessageContaining(HOY.toString());

        assertThat(dataStore.getEstadias()).isEmpty();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    @DisplayName("Admite el check-in el ultimo dia que NO es la fecha de salida")
    void checkIn_ultimoDiaAntesDeLaSalida_esPermitido() {
        Reserva reserva = crearReserva(HABITACION, HOY, HOY.plusDays(2));

        Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
        assertThat(estadia.getNoches()).isEqualTo(2);
    }

    @Test
    @DisplayName("Admite el check-in el mismo dia de la fecha de entrada")
    void checkIn_elDiaDeEntrada_esPermitido() {
        Reserva reserva = crearReserva(HABITACION, HOY, HOY.plusDays(1));

        Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
    }

    @Test
    @DisplayName("El check-in no modifica las fechas de la reserva")
    void checkIn_noAlteraLasFechasDeLaReserva() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        LocalDate inicioAntes = reserva.getFechaInicio();
        LocalDate finAntes = reserva.getFechaFin();

        estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(reserva.getFechaInicio()).isEqualTo(inicioAntes);
        assertThat(reserva.getFechaFin()).isEqualTo(finAntes);
    }


    @Test
    @DisplayName("Rechaza el check-in con la habitacion EN_LIMPIEZA")
    void checkIn_habitacionEnLimpieza_esRechazado() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EN_LIMPIEZA");

        assertThat(dataStore.getEstadias()).isEmpty();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("la habitacion NO debe quedar OCUPADA")
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("Rechaza el check-in con la habitacion en MANTENIMIENTO o FUERA_DE_SERVICIO")
    void checkIn_habitacionBloqueada_esRechazado() {
        for (EstadoHabitacion estado : List.of(EstadoHabitacion.MANTENIMIENTO, EstadoHabitacion.FUERA_DE_SERVICIO)) {
            reiniciarAlmacen();

            Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
            habitacionService.obtenerOFallar(HABITACION).cambiarEstado(estado);

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .as("estado " + estado)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(estado.toString());

            assertThat(dataStore.getEstadias()).isEmpty();
        }
    }

    @Test
    @DisplayName("Rechaza el check-in con la habitacion ya OCUPADA")
    void checkIn_habitacionOcupada_esRechazado() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.OCUPADA);

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OCUPADA");

        assertThat(dataStore.getEstadias()).isEmpty();
    }

    @Test
    @DisplayName("El check-in no usa el estado OCUPADA para invalidar reservas futuras")
    void checkIn_noAfectaLaDisponibilidadPorFechas() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.OCUPADA);

        assertThat(habitacionService.listarDisponibles(MAÑANA.plusDays(5), MAÑANA.plusDays(7), null))
                .as("la habitacion OCUPADA sigue ofreciendose para fechas sin solape")
                .extracting(Habitacion::getNumeroHabitacion)
                .contains(HABITACION);

        Reserva futura = crearReserva(HABITACION, MAÑANA.plusDays(5), MAÑANA.plusDays(7));

        assertThat(futura.getHabitacion().getNumeroHabitacion()).isEqualTo(HABITACION);
        assertThat(futura.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);
    }


    @Test
    @DisplayName("Un rechazo no deja estadias parciales ni habitaciones marcadas")
    void checkIn_rechazado_noDejaCambiosParciales() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(dataStore.getEstadias()).isEmpty();
        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);
        assertThat(reserva.getFechaEstadia()).as("no debe haberse fijado la fecha de estadia").isNull();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("Un rechazo por fecha no fija la fecha de estadia ni ocupa la habitacion")
    void checkIn_rechazadoPorFecha_noDejaCambiosParciales() {
        Reserva reserva = crearReserva(HABITACION, MAÑANA, MAÑANA.plusDays(2));

        assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(dataStore.getEstadias()).isEmpty();
        assertThat(reserva.getFechaEstadia()).isNull();
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }


    @Test
    @DisplayName("Check-ins simultaneos sobre la misma reserva dejan una sola estadia")
    void checkIn_simultaneo_mismaReserva_unaEstadia() throws Exception {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);

        ResultadoCarrera resultado = ejecutarEnParalelo(12,
                i -> estadiaService.realizarCheckIn(reserva.getIdReserva()));

        assertThat(resultado.exitosas.get()).as("solo un check-in puede prosperar").isEqualTo(1);
        assertThat(resultado.rechazadas.get()).isEqualTo(11);
        assertThat(dataStore.getEstadias()).hasSize(1);
    }

    @Test
    @DisplayName("Check-ins simultaneos sobre la misma habitacion dejan una sola estadia")
    void checkIn_simultaneo_mismaHabitacion_unaEstadia() throws Exception {
        String[] ids = {
                insertarReservaDirecta("RES-A", HABITACION, HOY, MAÑANA).getIdReserva(),
                insertarReservaDirecta("RES-B", HABITACION, HOY, MAÑANA).getIdReserva()
        };

        ResultadoCarrera resultado = ejecutarEnParalelo(10,
                i -> estadiaService.realizarCheckIn(ids[i % ids.length]));

        assertThat(resultado.exitosas.get())
                .as("la habitacion no puede hospedar dos estadias activas simultaneas")
                .isEqualTo(1);
        assertThat(dataStore.getEstadias()).hasSize(1);
    }

    @Test
    @DisplayName("Check-ins simultaneos en habitaciones distintas no se rechazan entre si")
    void checkIn_simultaneo_habitacionesDistintas_todasProsperan() throws Exception {
        int[] habitaciones = {101, 102, 201};
        String[] ids = new String[habitaciones.length];
        for (int i = 0; i < habitaciones.length; i++) {
            ids[i] = insertarReservaDirecta("RES-LIBRE-" + i, habitaciones[i], HOY, MAÑANA).getIdReserva();
        }

        ResultadoCarrera resultado = ejecutarEnParalelo(ids.length,
                i -> estadiaService.realizarCheckIn(ids[i]));

        assertThat(resultado.exitosas.get())
                .as("el control de concurrencia no debe rechazar operaciones validas en habitaciones distintas")
                .isEqualTo(ids.length);
        assertThat(dataStore.getEstadias()).hasSize(ids.length);
    }


    @Test
    @DisplayName("POST /api/estadias/checkin/{id} devuelve 200 con la estadia creada")
    void endpointCheckIn_devuelve200() throws Exception {
        String id = crearReservaPorHttp(HABITACION, HOY, MAÑANA);

        MvcResult resultado = mockMvc.perform(post("/api/estadias/checkin/" + id)).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"idEstadia\":\"EST-");
        assertThat(cuerpo).contains("\"estadoEstadia\":\"EN_CURSO\"");
        assertThat(cuerpo).contains("\"estadoReserva\":\"CHECKED_IN\"");
        assertThat(cuerpo).as("expone el documento del huesped").contains("\"documento\":\"CC10000001\"");
        assertThat(cuerpo).as("no expone contrasenas").doesNotContain("\"password\"").doesNotContain("pass123");
    }

    @Test
    @DisplayName("POST /api/estadias/checkin/{id} responde 400 si la reserva no existe")
    void endpointCheckIn_noExiste_devuelve400() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/estadias/checkin/RES-NO-EXISTE")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("mensaje");
    }

    @Test
    @DisplayName("POST /api/estadias/checkin/{id} responde 409 con el motivo del bloqueo")
    void endpointCheckIn_bloqueado_devuelve409() throws Exception {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult resultado = mockMvc.perform(
                post("/api/estadias/checkin/" + reserva.getIdReserva())).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(resultado.getResponse().getContentAsString()).contains("EN_LIMPIEZA");
    }

    @Test
    @DisplayName("POST /api/estadias/checkin/{id} responde 409 ante un check-in duplicado")
    void endpointCheckIn_duplicado_devuelve409() throws Exception {
        String id = crearReservaPorHttp(HABITACION, HOY, MAÑANA);
        mockMvc.perform(post("/api/estadias/checkin/" + id)).andReturn();

        MvcResult resultado = mockMvc.perform(post("/api/estadias/checkin/" + id)).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(dataStore.getEstadias()).hasSize(1);
    }

    @Test
    @DisplayName("Las respuestas del check-in no incluyen contrasenas")
    void checkIn_noExponeContrasenas() throws Exception {
        String id = crearReservaPorHttp(HABITACION, HOY, MAÑANA);
        mockMvc.perform(post("/api/estadias/checkin/" + id)).andReturn();

        for (String ruta : List.of("/api/estadias", "/api/estadias/en-curso", "/api/reservas")) {
            String cuerpo = cuerpo(200, get(ruta));
            assertThat(cuerpo).as(ruta).doesNotContain("\"password\"");
            assertThat(cuerpo).as(ruta).doesNotContain("pass123");
        }
    }


    @Test
    @DisplayName("Tras el check-in siguen funcionando listar reservas, estadias y disponibilidad")
    void checkIn_noAfectaLasConsultasExistentes() {
        Reserva reserva = crearReserva(HABITACION, HOY, MAÑANA);
        Reserva posterior = crearReserva(102, MAÑANA.plusDays(5), MAÑANA.plusDays(7));
        estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(reservaService.listarTodas()).hasSize(2);
        assertThat(estadiaService.listarEnCurso()).hasSize(1);
        assertThat(estadiaService.listarTodas()).hasSize(1);
        assertThat(posterior.getEstadoReserva()).isEqualTo(EstadoReserva.RESERVADA);

        assertThat(habitacionService.listarDisponibles(MAÑANA.plusDays(5), MAÑANA.plusDays(7), null))
                .extracting(Habitacion::getNumeroHabitacion)
                .contains(HABITACION)
                .doesNotContain(102);
    }


    /** Resultado de una carrera de peticiones paralelas. */
    private static class ResultadoCarrera {
        final AtomicInteger exitosas = new AtomicInteger();
        final AtomicInteger rechazadas = new AtomicInteger();
    }

    /** Ejecuta la misma operacion desde varios hilos a la vez y cuenta exitos y rechazos. */
    private ResultadoCarrera ejecutarEnParalelo(int peticiones, java.util.function.IntConsumer operacion)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(peticiones);
        CountDownLatch arranque = new CountDownLatch(1);
        ResultadoCarrera resultado = new ResultadoCarrera();
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < peticiones; i++) {
            final int indice = i;
            tareas.add(executor.submit(() -> {
                try {
                    arranque.await();
                    operacion.accept(indice);
                    resultado.exitosas.incrementAndGet();
                } catch (IllegalStateException | IllegalArgumentException e) {
                    resultado.rechazadas.incrementAndGet();
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
        return resultado;
    }

    /** Crea la reserva usando el servicio real, que aplica todas las validaciones de F-01/F-02. */
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

    /** Igual que crearReserva pero para un huésped concreto distinto del semilla. */
    private Reserva crearReservaConHuesped(Huesped huesped, int numero, LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(huesped.getIdHuesped());
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return reservaService.crearReserva(dto);
    }

    /** Registra un huesped aislado para una prueba, sin tocar los huespedes semilla. */
    private Huesped registrarHuespedDePrueba(String id, String documento) {
        Huesped huesped = new Huesped(id, "Huésped", "De Prueba", documento,
                "3009999999", id.toLowerCase() + "@email.com", id.toLowerCase(), "pass123");
        dataStore.getHuespedes().add(huesped);
        return huesped;
    }

    /**
     * Inserta una reserva directamente en el DataStore, saltandose la validacion de solapamiento.
     * Sirve unicamente para reproducir estados incoherentes (fechas pasadas o solapadas) que la
     * API no permite crear pero que el servicio debe saber rechazar en el check-in.
     */
    private Reserva insertarReservaDirecta(String id, int numero, LocalDate inicio, LocalDate fin) {
        Huesped huesped = dataStore.getHuespedes().stream()
                .filter(h -> ID_HUESPED.equals(h.getIdHuesped()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontro el huesped semilla"));
        Habitacion habitacion = habitacionService.obtenerOFallar(numero);
        habitacion.cambiarEstado(EstadoHabitacion.DISPONIBLE);

        Reserva reserva = new Reserva(id, inicio, fin, 2, 0, huesped, habitacion);
        dataStore.getReservas().add(reserva);
        return reserva;
    }

    private String cuerpo(int esperado, MockHttpServletRequestBuilder peticion) throws Exception {
        MvcResult resultado = mockMvc.perform(peticion).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(esperado);
        return resultado.getResponse().getContentAsString();
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
}