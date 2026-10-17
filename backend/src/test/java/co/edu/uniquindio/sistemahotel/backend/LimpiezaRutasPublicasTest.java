package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
class LimpiezaRutasPublicasTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final String ID_HUESPED = "H-001";
    private static final int HABITACION = 101;

    private static final String RECEP_ROL = "RECEPCIONISTA";
    private static final String RECEP_ID = "R-001";
    private static final String RECEP_USER = "maria.l";
    private static final String ADMIN_ROL = "ADMIN";
    private static final String ADMIN_ID = "A-001";
    private static final String ADMIN_USER = "admin";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private SaldoService saldoService;

    @Autowired
    private ReservaService reservaService;

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
    @DisplayName("PUT /api/habitaciones/{n}/estado no libera una habitacion EN_LIMPIEZA")
    void rutaEstado_noLiberaDesdeEnLimpieza() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult resultado = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEstado("DISPONIBLE")))
                .andReturn();

        assertThat(resultado.getResponse().getStatus())
                .as("la operacion generica no puede liberar")
                .isEqualTo(409);
        assertThat(cuerpoDe(resultado)).contains("solo se libera confirmando");
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("la habitacion sigue EN_LIMPIEZA")
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("La ruta /estado no libera una habitacion OCUPADA, MANTENIMIENTO o FUERA_DE_SERVICIO")
    void rutaEstado_noLiberaDesdeOtrosEstados() throws Exception {
        for (EstadoHabitacion estado : List.of(EstadoHabitacion.OCUPADA,
                EstadoHabitacion.MANTENIMIENTO, EstadoHabitacion.FUERA_DE_SERVICIO)) {
            habitacionService.obtenerOFallar(HABITACION).cambiarEstado(estado);

            MvcResult resultado = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/estado")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonEstado("DISPONIBLE")))
                    .andReturn();

            assertThat(resultado.getResponse().getStatus()).as("estado " + estado).isEqualTo(409);
            assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                    .isEqualTo(estado);
        }
    }

    @Test
    @DisplayName("La ruta /estado acepta dejar una habitacion que ya esta DISPONIBLE")
    void rutaEstado_sinCambio_realEsIdempotente() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.DISPONIBLE);

        MvcResult resultado = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEstado("DISPONIBLE")))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }


    @Test
    @DisplayName("No se puede pasar una habitacion OCUPADA con estadia activa a EN_LIMPIEZA por /estado")
    void rutaEstado_noSimulaLimpiezaDeOcupada() throws Exception {
        crearEstadia(2);

        MvcResult resultado = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEstado("EN_LIMPIEZA")))
                .andReturn();

        assertThat(resultado.getResponse().getStatus())
                .as("si se permitiera, se cerraria la estadia por la puerta de atras")
                .isEqualTo(409);
        // El mensaje cambio al reforzarse la regla: antes el rechazo dependia de que existiera una
        // estadia activa y por eso lo decia; ahora la transicion se prohibe siempre desde OCUPADA,
        // asi que el mensaje explica que solo el check-out saca una habitacion de ese estado.
        assertThat(cuerpoDe(resultado))
                .contains("desde OCUPADA a EN_LIMPIEZA")
                .contains("solo deja ese estado mediante el check-out");
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("la habitacion sigue OCUPADA")
                .isEqualTo(EstadoHabitacion.OCUPADA);
    }

    @Test
    @DisplayName("No se puede pasar a EN_LIMPIEZA por /limpieza una habitacion OCUPADA con estadia activa")
    void rutaLimpieza_noSimulaLimpiezaDeOcupada() throws Exception {
        crearEstadia(2);

        MvcResult resultado = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/limpieza")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpoDe(resultado)).contains("estadía activa");
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.OCUPADA);
        assertThat(dataStore.getEstadias().get(0).getEstadoEstadia())
                .as("la estadia sigue EN_CURSO: no se forzo el cierre")
                .isEqualTo(EstadoEstadia.EN_CURSO);
    }

    @Test
    @DisplayName("Una habitaria OCUPADA sin estadia activa si puede pasar a EN_LIMPIEZA a mano")
    void rutaLimpieza_siPermiteSiNoHayEstadia() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.OCUPADA);

        MvcResult resultado = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/limpieza")).andReturn();

        assertThat(resultado.getResponse().getStatus())
                .as("sin estadia activa no hay check-out que simular")
                .isEqualTo(200);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }



    @Test
    @DisplayName("PUT /api/habitaciones/{n}/disponible sin identidad es rechazada")
    void rutaDisponible_sinIdentidad_esRechazada() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult resultado = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/disponible")).andReturn();

        assertThat(resultado.getResponse().getStatus())
                .as("la ruta heredada ya no es una via alternativa")
                .isEqualTo(409);
        assertThat(cuerpoDe(resultado)).contains("Solo la recepción");
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("PUT /disponible con identidad invalida es rechazada")
    void rutaDisponible_identidadInvalida_esRechazada() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult resultado = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/disponible")
                        .header("X-User-Role", RECEP_ROL)
                        .header("X-User-Id", "R-999")
                        .header("X-Username", "nadie"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("PUT /disponible con recepcionista admitida aplica las mismas reglas de negocio")
    void rutaDisponible_recepcionista_aplicaLasMismasReglas() throws Exception {
        // Desde OCUPADA la ruta heredada también debe rechazarse: solo se libera desde EN_LIMPIEZA.
        estadiASaldada(2);
        MvcResult desdeOcupada = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/disponible")
                        .header("X-User-Role", RECEP_ROL)
                        .header("X-User-Id", RECEP_ID)
                        .header("X-Username", RECEP_USER))
                .andReturn();
        assertThat(desdeOcupada.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpoDe(desdeOcupada)).contains("no está EN_LIMPIEZA");
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.OCUPADA);


        estadiaService.realizarCheckOut(dataStore.getEstadias().get(0).getIdEstadia());
        MvcResult desdeEnLimpieza = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/disponible")
                        .header("X-User-Role", RECEP_ROL)
                        .header("X-User-Id", RECEP_ID)
                        .header("X-Username", RECEP_USER))
                .andReturn();
        assertThat(desdeEnLimpieza.getResponse().getStatus()).isEqualTo(200);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }



    @Test
    @DisplayName("PUT /finalizar-limpieza funciona para la recepcionista admitida")
    void rutaFinalizarLimpieza_recepcionista_admitida() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult resultado = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", RECEP_ROL)
                        .header("X-User-Id", RECEP_ID)
                        .header("X-Username", RECEP_USER)).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"estadoHabitacion\":\"DISPONIBLE\"");
    }

    @Test
    @DisplayName("PUT /finalizar-limpieza acepta al administrador, igual que la ruta heredada")
    void rutaFinalizarLimpieza_administrador() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult resultado = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", ADMIN_ROL)
                        .header("X-User-Id", ADMIN_ID)
                        .header("X-Username", ADMIN_USER)).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    @DisplayName("PUT /finalizar-limpieza se rechaza sin identidad o con identidad invalida")
    void rutaFinalizarLimpieza_sinIdentidad() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult sinCabeceras = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")).andReturn();
        assertThat(sinCabeceras.getResponse().getStatus()).isEqualTo(409);

        MvcResult recepInexistente = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", RECEP_ROL)
                        .header("X-User-Id", "R-999")
                        .header("X-Username", "nadie")).andReturn();
        assertThat(recepInexistente.getResponse().getStatus()).isEqualTo(409);

        MvcResult adminInexistente = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", ADMIN_ROL)
                        .header("X-User-Id", "A-999")
                        .header("X-Username", "nadie")).andReturn();
        assertThat(adminInexistente.getResponse().getStatus()).isEqualTo(409);

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("ningun rechazo libera la habitacion")
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
    }

    @Test
    @DisplayName("No se puede confirmar dos veces la limpieza")
    void rutaFinalizarLimpieza_noSePuedeRepetir() throws Exception {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult primera = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", RECEP_ROL)
                        .header("X-User-Id", RECEP_ID)
                        .header("X-Username", RECEP_USER)).andReturn();
        assertThat(primera.getResponse().getStatus()).isEqualTo(200);

        MvcResult segunda = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", RECEP_ROL)
                        .header("X-User-Id", RECEP_ID)
                        .header("X-Username", RECEP_USER)).andReturn();
        assertThat(segunda.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpoDe(segunda)).contains("no está EN_LIMPIEZA");

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("sin efectos secundarios: sigue DISPONIBLE")
                .isEqualTo(EstadoHabitacion.DISPONIBLE);
    }



    @Test
    @DisplayName("Ninguna ruta libera una habitacion con estadia activa")
    void ningunaRuta_liberaConEstadiaActiva() throws Exception {
        crearEstadia(2);
        // Estado incoherente que solo se puede provocar por la vía interna del modelo.
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

        MvcResult viaFinalizar = mockMvc.perform(
                put("/api/habitaciones/" + HABITACION + "/finalizar-limpieza")
                        .header("X-User-Role", RECEP_ROL)
                        .header("X-User-Id", RECEP_ID)
                        .header("X-Username", RECEP_USER)).andReturn();
        assertThat(viaFinalizar.getResponse().getStatus()).isEqualTo(409);
        assertThat(cuerpoDe(viaFinalizar)).contains("todavía tiene una estadía activa");

        MvcResult viaDisponible = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/disponible")
                .header("X-User-Role", RECEP_ROL)
                .header("X-User-Id", RECEP_ID)
                .header("X-Username", RECEP_USER)).andReturn();
        assertThat(viaDisponible.getResponse().getStatus()).isEqualTo(409);

        MvcResult viaEstado = mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonEstado("DISPONIBLE"))).andReturn();
        assertThat(viaEstado.getResponse().getStatus()).isEqualTo(409);

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .as("ninguna via la libero")
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
        assertThat(dataStore.getEstadias().get(0).getEstadoEstadia())
                .isEqualTo(EstadoEstadia.EN_CURSO);
    }



    @Test
    @DisplayName("El check-out sigue dejando la habitacion EN_LIMPIEZA y no DISPONIBLE")
    void checkOut_sigueDejandoEnLimpieza() {
        estadiASaldada(2);

        estadiaService.realizarCheckOut(dataStore.getEstadias().get(0).getIdEstadia());

        assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
        assertThat(dataStore.getEstadias().get(0).getEstadoEstadia())
                .isEqualTo(EstadoEstadia.FINALIZADA);
    }

    @Test
    @DisplayName("Tras el check-out la habitacion no se ofrece ni admite check-in hasta limpiarla")
    void trasCheckOut_bloqueadaHastaLimpiar() throws Exception {
        estadiASaldada(2);
        estadiaService.realizarCheckOut(dataStore.getEstadias().get(0).getIdEstadia());

        MvcResult disponibles = mockMvc.perform(get("/api/habitaciones/disponibles")).andReturn();
        assertThat(disponibles.getResponse().getStatus()).isEqualTo(200);
        assertThat(disponibles.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .doesNotContain("\"numeroHabitacion\":" + HABITACION);

        ReservaRequestDTO dto = dtoReserva(HOY.plusDays(20), HOY.plusDays(22));
        MvcResult reserva = mockMvc.perform(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReserva(dto))).andReturn();
        assertThat(reserva.getResponse().getStatus())
                .as("EN_LIMPIEZA no admite reservas")
                .isEqualTo(409);
    }

    @Test
    @DisplayName("Al confirmar la limpieza la habitacion vuelve a ofrecerse y admite reservas")
    void trasConfirmarLimpieza_vuelveADisponible() throws Exception {
        estadiASaldada(2);
        estadiaService.realizarCheckOut(dataStore.getEstadias().get(0).getIdEstadia());
        habitacionService.finalizarLimpieza(HABITACION);

        assertThat(habitacionService.listarDisponibles(HOY.plusDays(20), HOY.plusDays(22), null))
                .extracting(Habitacion::getNumeroHabitacion)
                .contains(HABITACION);

        ReservaRequestDTO dto = dtoReserva(HOY.plusDays(20), HOY.plusDays(22));
        MvcResult reserva = mockMvc.perform(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonReserva(dto))).andReturn();
        assertThat(reserva.getResponse().getStatus()).isEqualTo(201);
    }



    private String cuerpoDe(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String jsonEstado(String estado) {
        return "{\"estadoHabitacion\":\"" + estado + "\"}";
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

    private String jsonReserva(ReservaRequestDTO dto) {
        return "{\"idHuesped\":\"" + dto.getIdHuesped() + "\",\"numeroHabitacion\":" + dto.getNumeroHabitacion()
                + ",\"fechaInicio\":\"" + dto.getFechaInicio() + "\",\"fechaFin\":\"" + dto.getFechaFin()
                + "\",\"adultos\":" + dto.getAdultos() + ",\"ninos\":" + dto.getNinos() + "}";
    }

    private Estadia crearEstadia(int noches) {
        return estadiaService.realizarCheckIn(
                reservaService.crearReserva(dtoReserva(HOY, HOY.plusDays(noches))).getIdReserva());
    }

    /** Crea una estadía con la cuenta saldada, lista para el check-out. */
    private void estadiASaldada(int noches) {
        Estadia estadia = crearEstadia(noches);
        PagoRequestDTO pago = new PagoRequestDTO();
        pago.setIdEstadia(estadia.getIdEstadia());
        pago.setMonto(estadiaService.calcularCosto(estadia.getIdEstadia()).getTotalGeneral().doubleValue());
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        pago.setConcepto("Pago total");
        Pago registrado = pagoService.registrarPago(pago);
        pagoService.aprobarPago(registrado.getIdPago(), "REF-TEST");
    }
}