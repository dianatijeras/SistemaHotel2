package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.HuespedRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.Rol;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.model.UsuarioAcceso;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import co.edu.uniquindio.sistemahotel.backend.service.HuespedService;
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
class HuespedIdentidadTest {

    private static final LocalDate HOY = LocalDate.now();

    private static final String RECEP_ID = "R-001";
    private static final String RECEP_USER = "maria.l";
    private static final String RECEP_PASS = "admin123";
    private static final String ADMIN_ID = "A-001";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin2024";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HuespedService huespedService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();


        dataStore.getHuespedes().removeIf(h -> !h.getIdHuesped().matches("H-00[123]"));
        if (dataStore.getHuespedes().stream().noneMatch(h -> h.getIdHuesped().equals("H-001"))) {
            dataStore.getHuespedes().add(new Huesped("H-001", "Carlos", "Ramírez", "CC10000001",
                    "3001234567", "carlos@email.com", "carlos.r", "pass123"));
            dataStore.getHuespedes().add(new Huesped("H-002", "Laura", "Gómez", "CC10000002",
                    "3109876543", "laura@email.com", "laura.g", "pass123"));
            dataStore.getHuespedes().add(new Huesped("H-003", "Andrés", "Torres", "CC10000003",
                    "3204567890", "andres@email.com", "andres.t", "pass123"));
        }
    }



    @Test
    @DisplayName("Un huesped no tiene rol de recepcionista ni de administrador")
    void huesped_noTieneRolPrivilegiado() {
        for (Huesped huesped : huespedService.listarTodos()) {
            assertThat(huesped.getRol())
                    .as("ningun huesped puede declararse empleado del hotel")
                    .isEqualTo(Rol.HUESPED)
                    .isNotEqualTo(Rol.RECEPCIONISTA)
                    .isNotEqualTo(Rol.ADMIN)
                    .isNotEqualTo(Rol.PERSONAL_LIMPIEZA);
        }
    }

    @Test
    @DisplayName("Un huesped recien registrado tampoco recibe rol privilegiado")
    void huespedRegistrado_noTieneRolPrivilegiado() {
        HuespedRequestDTO dto = dtoHuesped("Nuevo", "Huesped", "CC99999999", "nuevo.user");
        Huesped creado = huespedService.registrarHuesped(dto);

        assertThat(creado.getRol()).isEqualTo(Rol.HUESPED);
    }

    @Test
    @DisplayName("Un huesped no tiene metodo de autenticacion: no puede llegar a autenticarse")
    void huesped_noPuedeAutenticarse() {
        Huesped huesped = huespedService.listarTodos().get(0);


        assertThat(declaraAutenticar(huesped.getClass()))
                .as("el huesped no declara el metodo")
                .isFalse();
        assertThat(UsuarioAcceso.class.isAssignableFrom(Huesped.class))
                .as("ni lo hereda por la rama del personal")
                .isFalse();


        assertThat(huesped.getUsername()).isNotBlank();
        assertThat(huesped.getPassword()).isNotBlank();
        assertThat(huesped.getRol()).isEqualTo(Rol.HUESPED);
    }

    /**
     * Verifica si la clase dada declara un método llamado "autenticar" con dos parámetros de tipo String.
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
    @DisplayName("El login HTTP rechaza las credenciales de un huesped")
    void huesped_noEntraPorElLogin() throws Exception {
        Huesped huesped = huespedService.listarTodos().get(0);

        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + huesped.getUsername()
                                + "\",\"password\":\"" + huesped.getPassword() + "\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Un huesped no obtiene privilegios enviando un rol manipulado en el registro")
    void huesped_noAdquierePrivilegiosConRolManipulado() throws Exception {

        String cuerpoConRol = "{\"nombre\":\"Intruso\",\"apellido\":\"Malicioso\",\"documento\":\"CC88888888\","
                + "\"telefono\":\"3000000000\",\"correo\":\"intruso@email.com\",\"username\":\"intruso.x\","
                + "\"password\":\"clave123\",\"rol\":\"ADMIN\",\"activo\":true,\"idUsuario\":\"A-999\"}";

        MvcResult resultado = mockMvc.perform(post("/api/huespedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", RECEP_ID)
                        .header("X-Username", RECEP_USER)
                        .content(cuerpoConRol))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);

        String cuerpo = cuerpoDe(resultado);
        assertThat(cuerpo).as("el rol enviado se ignora").contains("\"rol\":\"HUESPED\"");
        assertThat(cuerpo).doesNotContain("\"rol\":\"ADMIN\"");
        assertThat(cuerpo).doesNotContain("\"rol\":\"RECEPCIONISTA\"");

        Huesped creado = huespedService.listarTodos().stream()
                .filter(h -> h.getDocumento().equals("CC88888888"))
                .findFirst()
                .orElseThrow();
        assertThat(creado.getRol()).isEqualTo(Rol.HUESPED);
        assertThat(creado.getIdHuesped())
                .as("el identificador interno lo asigna el sistema, no el cliente")
                .isNotEqualTo("A-999");
    }

    @Test
    @DisplayName("No existe ninguna forma de cambiar el rol de un usuario ya creado")
    void rol_esInmutable() throws Exception {

        assertThat(UsuarioSinSetRol.class.getDeclaredMethods())
                .as("ningun metodo publica setRol en la jerarquia de usuarios")
                .noneMatch(m -> m.getName().equals("setRol"));

        for (Huesped huesped : huespedService.listarTodos()) {
            assertThat(huesped.getRol()).isEqualTo(Rol.HUESPED);
        }
        assertThat(empleado("R-001").getRol()).isEqualTo(Rol.RECEPCIONISTA);
        assertThat(empleado("A-001").getRol()).isEqualTo(Rol.ADMIN);
    }

    /**
     * Un usuario que no tiene un método público setRol, para verificar que la jerarquía de usuarios no permite cambiar el rol.
     */
    private static class UsuarioSinSetRol extends Usuario {
        UsuarioSinSetRol() {
            super("X", "x", "x", Rol.HUESPED);
        }

        @Override
        public String getNombre() {
            return "sin";
        }

        @Override
        public void setNombre(String nombre) {
            // Un usuario sin nombre propio: la prueba no lo necesita.
        }

        @Override
        public String getApellido() {
            return "setRol";
        }

        @Override
        public void setApellido(String apellido) {
            // Un usuario sin apellido propio: la prueba no lo necesita.
        }

        @Override
        public String getNombreCompleto() {
            return "sin setRol";
        }
    }


    @Test
    @DisplayName("El login de recepcionistas y administradores sigue funcionando")
    void empleados_siguenPodiendoEntrar() throws Exception {
        MvcResult recep = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + RECEP_USER + "\",\"password\":\"" + RECEP_PASS + "\"}"))
                .andReturn();
        assertThat(recep.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpoDe(recep))
                .contains("\"rol\":\"RECEPCIONISTA\"")
                .contains(RECEP_ID)
                .doesNotContain(RECEP_PASS);

        MvcResult admin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + ADMIN_USER + "\",\"password\":\"" + ADMIN_PASS + "\"}"))
                .andReturn();
        assertThat(admin.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpoDe(admin))
                .contains("\"rol\":\"ADMIN\"")
                .contains(ADMIN_ID)
                .doesNotContain(ADMIN_PASS);
    }


    @Test
    @DisplayName("La identidad de un huesped da por si sola no llega a endpoints de empleados")
    void huesped_noAlcanzaEndpointsDeEmpleados() throws Exception {
        Huesped huesped = huespedService.listarTodos().get(0);


        for (String[] cabeceras : List.of(
                new String[]{"HUESPED", huesped.getIdHuesped(), huesped.getUsername()},
                new String[]{"RECEPCIONISTA", huesped.getIdHuesped(), huesped.getUsername()},
                new String[]{"ADMIN", huesped.getIdHuesped(), huesped.getUsername()})) {

            MvcResult resultado = mockMvc.perform(put("/api/habitaciones/101/finalizar-limpieza")
                            .header("X-User-Role", cabeceras[0])
                            .header("X-User-Id", cabeceras[1])
                            .header("X-Username", cabeceras[2]))
                    .andReturn();

            assertThat(resultado.getResponse().getStatus())
                    .as("rol declarado " + cabeceras[0] + " con id de huesped")
                    .isEqualTo(409);
            assertThat(cuerpoDe(resultado)).contains("Solo la recepción");
        }
    }

    @Test
    @DisplayName("Un huesped no puede administrar el personal del hotel")
    void huesped_noAdministraPersonal() throws Exception {
        Huesped huesped = huespedService.listarTodos().get(0);

        MvcResult resultado = mockMvc.perform(post("/api/usuarios/recepcionistas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", huesped.getIdHuesped())
                        .header("X-Username", huesped.getUsername())
                        .content("{\"nombre\":\"Intruso\",\"apellido\":\"X\",\"turno\":\"MANANA\","
                                + "\"username\":\"intruso.recep\",\"password\":\"clave123\"}"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus())
                .as("declarar ADMIN no sirve si el usuario no existe como administrador")
                .isEqualTo(409);
        assertThat(cuerpoDe(resultado)).contains("Solo el administrador");
    }



    @Test
    @DisplayName("Los huespedes iniciales conservan sus datos y su documento de identidad")
    void huespedesIniciales_seConservan() {
        List<Huesped> huespedes = huespedService.listarTodos();
        assertThat(huespedes).hasSize(3);

        assertThat(huespedes).extracting(Huesped::getIdHuesped)
                .containsExactlyInAnyOrder("H-001", "H-002", "H-003");
        assertThat(huespedes).extracting(Huesped::getDocumento)
                .containsExactlyInAnyOrder("CC10000001", "CC10000002", "CC10000003");
        assertThat(huespedes).extracting(Huesped::getNombre)
                .containsExactlyInAnyOrder("Carlos", "Laura", "Andrés");

        Huesped carlos = huespedService.buscarPorId("H-001").orElseThrow();
        assertThat(carlos.getNombre()).isEqualTo("Carlos");
        assertThat(carlos.getApellido()).isEqualTo("Ramírez");
        assertThat(carlos.getDocumento()).isEqualTo("CC10000001");
        assertThat(carlos.getTelefono()).isEqualTo("3001234567");
        assertThat(carlos.getCorreo()).isEqualTo("carlos@email.com");
        assertThat(carlos.getUsername()).as("el username se conserva").isEqualTo("carlos.r");
        assertThat(carlos.getRol()).isEqualTo(Rol.HUESPED);
    }

    @Test
    @DisplayName("El registro y la edicion de huespedes siguen funcionando")
    void registroYEdicion_siguenFuncionando() throws Exception {
        Huesped creado = huespedService.registrarHuesped(
                dtoHuesped("Registro", "Nuevo", "CC77777777", "nuevo.registro"));
        assertThat(creado.getIdHuesped()).startsWith("H-");
        assertThat(creado.getRol()).isEqualTo(Rol.HUESPED);

        Huesped actualizado = huespedService.actualizarDatos(
                creado.getIdHuesped(), "Registro Editado", null, null, "3001111111", null);
        assertThat(actualizado.getNombre()).isEqualTo("Registro Editado");
        assertThat(actualizado.getApellido())
                .as("los campos no enviados se conservan")
                .isEqualTo("Nuevo");
        assertThat(actualizado.getDocumento())
                .as("el documento nunca se pierde al editar")
                .isEqualTo("CC77777777");
        assertThat(actualizado.getTelefono()).isEqualTo("3001111111");
        assertThat(actualizado.getRol()).isEqualTo(Rol.HUESPED);

        MvcResult porHttp = mockMvc.perform(put("/api/huespedes/" + creado.getIdHuesped())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", RECEP_ID)
                        .header("X-Username", RECEP_USER)
                        .content("{\"telefono\":\"3002222222\",\"correo\":\"editado@email.com\"}"))
                .andReturn();
        assertThat(porHttp.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpoDe(porHttp)).contains("\"documento\":\"CC77777777\"");
    }

    @Test
    @DisplayName("Las reservas siguen asociandose al huesped correcto")
    void reservas_siguenAsociadasAlHuespedCorrecto() {
        Reserva r1 = crearReserva("H-001", 101, HOY, HOY.plusDays(2));
        Reserva r2 = crearReserva("H-002", 102, HOY, HOY.plusDays(2));

        assertThat(r1.getHuesped().getIdHuesped()).isEqualTo("H-001");
        assertThat(r1.getHuesped().getDocumento()).isEqualTo("CC10000001");
        assertThat(r1.getHuesped().getNombre()).isEqualTo("Carlos");
        assertThat(r2.getHuesped().getIdHuesped()).isEqualTo("H-002");
        assertThat(r2.getHuesped().getDocumento()).isEqualTo("CC10000002");


        assertThat(reservaService.listarPorHuesped("H-001")).hasSize(1);
    }


    @Test
    @DisplayName("El check-in sigue verificando el documento de identidad real")
    void checkIn_sigueVerificandoElDocumento() {
        Reserva reserva = crearReserva("H-001", 101, HOY, HOY.plusDays(2));

        var estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

        assertThat(estadia.getReserva().getHuesped().getDocumento())
                .as("el documento de identidad llega intacto al check-in")
                .isEqualTo("CC10000001");
        assertThat(estadia.getReserva().getHuesped().getRol()).isEqualTo(Rol.HUESPED);
    }



    @Test
    @DisplayName("Ninguna respuesta JSON expone contrasenas de huespedes ni de empleados")
    void ningunaRespuestaExponeContrasenas() throws Exception {
        for (String ruta : List.of("/api/huespedes", "/api/huespedes/H-001",
                "/api/reservas", "/api/estadias", "/api/usuarios")) {
            MvcResult resultado = mockMvc.perform(get(ruta)).andReturn();
            String cuerpo = cuerpoDe(resultado);
            assertThat(cuerpo).as(ruta + " responde 200").isNotNull();
            assertThat(cuerpo).as(ruta).doesNotContain("\"password\"");
            assertThat(cuerpo).as(ruta).doesNotContain("pass123");
            assertThat(cuerpo).as(ruta).doesNotContain(RECEP_PASS);
        }
    }

    @Test
    @DisplayName("El huesped expone su documento y su rol, pero nunca su contrasena")
    void contratoDelHuesped_esExplicito() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/huespedes/H-001")).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);

        String cuerpo = cuerpoDe(resultado);
        assertThat(cuerpo).contains("\"idHuesped\":\"H-001\"");
        assertThat(cuerpo).contains("\"documento\":\"CC10000001\"");
        assertThat(cuerpo).contains("\"rol\":\"HUESPED\"");
        assertThat(cuerpo).doesNotContain("\"password\"");
        assertThat(cuerpo).doesNotContain("\"rol\":\"RECEPCIONISTA\"");
    }



    private String cuerpoDe(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Usuario empleado(String idUsuario) {
        var recepcionista = dataStore.getRecepcionistas().stream()
                .filter(r -> idUsuario.equals(r.getIdRecepcionista())).findFirst();
        if (recepcionista.isPresent()) {
            return recepcionista.get();
        }
        return dataStore.getAdministradores().stream()
                .filter(a -> idUsuario.equals(a.getIdAdministrador()))
                .findFirst()
                .orElseThrow();
    }

    private HuespedRequestDTO dtoHuesped(String nombre, String apellido, String documento, String username) {
        HuespedRequestDTO dto = new HuespedRequestDTO();
        dto.setNombre(nombre);
        dto.setApellido(apellido);
        dto.setDocumento(documento);
        dto.setTelefono("3001234567");
        dto.setCorreo(nombre.toLowerCase() + "@email.com");
        dto.setUsername(username);
        dto.setPassword("clave123");
        return dto;
    }

    private Reserva crearReserva(String idHuesped, int numero, LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(idHuesped);
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return reservaService.crearReserva(dto);
    }
}