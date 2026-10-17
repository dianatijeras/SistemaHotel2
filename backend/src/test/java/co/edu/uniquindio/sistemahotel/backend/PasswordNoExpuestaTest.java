package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.HuespedRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.Rol;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.UsuarioAcceso;
import co.edu.uniquindio.sistemahotel.backend.service.HuespedService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;


@SpringBootTest
@AutoConfigureMockMvc
class PasswordNoExpuestaTest {


    private static final String PASSWORD_HUESPED = "pass123";
    private static final String PASSWORD_ADMIN = "admin2024";
    private static final String PASSWORD_RECEPCIONISTA = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HuespedService huespedService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
    }



    @Test
    @DisplayName("GET /api/huespedes no expone contrasenas")
    void listarHuespedes_noExponeContrasenas() throws Exception {
        String cuerpo = cuerpoOk(get("/api/huespedes"));

        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_HUESPED);
        assertThat(cuerpo).doesNotContain(PASSWORD_ADMIN);
        assertThat(cuerpo).contains("H-001");
    }

    @Test
    @DisplayName("GET /api/huespedes/{id} no expone la contrasena")
    void buscarHuespedPorId_noExponeContrasena() throws Exception {
        String cuerpo = cuerpoOk(get("/api/huespedes/H-001"));

        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_HUESPED);
    }

    @Test
    @DisplayName("POST /api/huespedes registra el huesped y no devuelve la contrasena")
    void registrarHuesped_noExponeLaContrasena() throws Exception {
        HuespedRequestDTO dto = solicitud("CC66000001", "test.password", "claveSecreta123");

        MvcResult resultado = mockMvc.perform(post("/api/huespedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", "R-001")
                        .header("X-Username", "maria.l"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain("claveSecreta123");
        assertThat(cuerpo).contains("CC66000001");
    }

    @Test
    @DisplayName("PUT /api/huespedes/{id} actualiza y no expone la contrasena")
    void actualizarHuesped_noExponeLaContrasena() throws Exception {
        MvcResult resultado = mockMvc.perform(put("/api/huespedes/H-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documento\":\"CC10000444\"}")
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", "R-001")
                        .header("X-Username", "maria.l"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_HUESPED);
        assertThat(cuerpo).contains("CC10000444");
    }

    @Test
    @DisplayName("La contrasena si se guarda y no se devuelve, aunque el huesped no pueda autenticarse")
    void laContrasenaSeGuardaAunqueNoSeDevuelva() {
        HuespedRequestDTO dto = solicitud("CC66000002", "test.almacenada", "claveSecreta456");

        Huesped creado = huespedService.registrarHuesped(dto);

        assertThat(dto.getPassword()).isEqualTo("claveSecreta456");
        assertThat(creado.getPassword())
                .as("la contrasena se conserva en el modelo, aunque nunca se devuelva por la API")
                .isEqualTo("claveSecreta456");

        assertThat(declaraAutenticar(creado.getClass()))
                .as("el huesped no hereda el metodo de autenticacion")
                .isFalse();
        assertThat(declaraAutenticar(UsuarioAcceso.class))
                .as("el metodo vive en la clase que representa a quienes acceden")
                .isTrue();
        assertThat(UsuarioAcceso.class.isAssignableFrom(creado.getClass()))
                .as("y el huesped no es de esa clase")
                .isFalse();

        assertThat(creado.getRol()).isEqualTo(Rol.HUESPED);
    }

    /**
     * Verifica si la clase dada declara un metodo autenticar(String, String).
     * @param tipo
     * @return
     */
    private boolean declaraAutenticar(Class<?> tipo) {
        try {
            tipo.getDeclaredMethod("autenticar", String.class, String.class);
            return true;
        } catch (NoSuchMethodException sinMetodo) {
            return false;
        }
    }


    @Test
    @DisplayName("GET /api/reservas no expone contrasenas del huesped anidado")
    void listarReservas_noExponeContrasenasDelHuespedAnidado() throws Exception {
        crearReserva();

        String cuerpo = cuerpoOk(get("/api/reservas"));

        assertThat(cuerpo).contains("H-001");
        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_HUESPED);
    }

    @Test
    @DisplayName("GET /api/estadias no expone contrasenas anidadas en reserva y huesped")
    void listarEstadias_noExponeContrasenasAnidadas() throws Exception {
        String cuerpo = cuerpoOk(get("/api/estadias"));

        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_HUESPED);
    }

    @Test
    @DisplayName("GET /api/pagos no expone contrasenas anidadas en tres niveles")
    void listarPagos_noExponeContrasenasAnidadas() throws Exception {
        String cuerpo = cuerpoOk(get("/api/pagos"));

        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_HUESPED);
    }

    @Test
    @DisplayName("GET /api/reservas/{id} no expone contrasenas del huesped anidado")
    void buscarReservaPorId_noExponeContrasenasAnidadas() throws Exception {
        String id = crearReserva();

        String cuerpo = cuerpoOk(get("/api/reservas/" + id));

        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_HUESPED);
    }


    @Test
    @DisplayName("GET /api/usuarios no expone contrasenas")
    void listarUsuarios_noExponeContrasenas() throws Exception {
        String cuerpo = cuerpoOk(get("/api/usuarios"));

        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_ADMIN);
        assertThat(cuerpo).contains("maria.l");
    }

    @Test
    @DisplayName("El login de la recepcionista sigue funcionando y no expone la contrasena")
    void login_sigueFuncionandoYNoExponeContrasena() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"maria.l\",\"password\":\"" + PASSWORD_RECEPCIONISTA + "\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"username\":\"maria.l\"");
        assertThat(cuerpo).contains("\"rol\":\"RECEPCIONISTA\"");
        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain(PASSWORD_RECEPCIONISTA);
    }

    @Test
    @DisplayName("El login del administrador sigue funcionando")
    void loginAdmin_sigueFuncionando() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + PASSWORD_ADMIN + "\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).contains("\"rol\":\"ADMIN\"");
        assertThat(cuerpo).doesNotContain("\"password\"");
    }

    @Test
    @DisplayName("El login sigue rechazando credenciales incorrectas")
    void login_conCredencialesIncorrectas_sigueFallando() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"maria.l\",\"password\":\"incorrecta\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }


    @Test
    @DisplayName("GET /api/dashboard/stats no expone contrasenas")
    void dashboard_noExponeContrasenas() throws Exception {
        String cuerpo = cuerpoOk(get("/api/dashboard/stats"));

        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).contains("totalHabitaciones");
    }

    @Test
    @DisplayName("Las habitaciones siguen respondiendo igual")
    void habitaciones_siguenFuncionando() throws Exception {
        cuerpoOk(get("/api/habitaciones"));
        cuerpoOk(get("/api/habitaciones/disponibles"));
    }


    /**
     * Ejecuta la petición dada y verifica que el código de respuesta sea 200 OK.
     * @param peticion
     * @return
     * @throws Exception
     */
    private String cuerpoOk(MockHttpServletRequestBuilder peticion) throws Exception {
        MvcResult resultado = mockMvc.perform(peticion).andReturn();
        assertThat(resultado.getResponse().getStatus())
                .as("codigo de respuesta de " + peticion)
                .isEqualTo(200);
        return resultado.getResponse().getContentAsString();
    }

    private String crearReserva() throws Exception {
        String cuerpo = objectMapper.writeValueAsString(Map.of(
                "idHuesped", "H-001",
                "numeroHabitacion", 101,
                "fechaInicio", LocalDate.now().plusDays(30).toString(),
                "fechaFin", LocalDate.now().plusDays(33).toString(),
                "adultos", 2,
                "ninos", 0));
        MvcResult resultado = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("idReserva").asText();
    }

    private HuespedRequestDTO solicitud(String documento, String username, String password) {
        HuespedRequestDTO dto = new HuespedRequestDTO();
        dto.setNombre("Ana");
        dto.setApellido("Restrepo");
        dto.setDocumento(documento);
        dto.setTelefono("3001112222");
        dto.setCorreo(username + "@email.com");
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }
}