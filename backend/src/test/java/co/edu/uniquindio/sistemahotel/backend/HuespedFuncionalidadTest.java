package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.controller.GlobalExceptionHandler;
import co.edu.uniquindio.sistemahotel.backend.controller.HuespedController;
import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.HuespedRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.service.HuespedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
class HuespedFuncionalidadTest {

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HuespedService huespedService;

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getHuespedes().clear();
        dataStore.getHuespedes().add(new Huesped("H-001", "Carlos", "Ramírez", "CC10000001",
                "3001234567", "carlos@email.com", "carlos.r", "pass123"));
        dataStore.getHuespedes().add(new Huesped("H-002", "Laura", "Gómez", "CC10000002",
                "3109876543", "laura@email.com", "laura.g", "pass123"));
        dataStore.getHuespedes().add(new Huesped("H-003", "Andrés", "Torres", "CC10000003",
                "3204567890", "andres@email.com", "andres.t", "pass123"));
    }


    @Test
    @DisplayName("Registra un huesped con todos sus datos personales, incluido el documento")
    void registrarHuesped_devuelveTodosLosDatos() {
        HuespedRequestDTO dto = solicitud("CC90000001", "test.registro", "pass123");

        Huesped huesped = huespedService.registrarHuesped(dto);

        assertThat(huesped.getDocumento()).isEqualTo("CC90000001");
        assertThat(huesped.getNombre()).isEqualTo("Ana");
        assertThat(huesped.getApellido()).isEqualTo("Restrepo");
        assertThat(huesped.getTelefono()).isEqualTo("3001112222");
        assertThat(huesped.getCorreo()).isEqualTo("test.registro@email.com");
        assertThat(huesped.getIdHuesped()).startsWith("H-");
        assertThat(huesped.getDocumento())
                .as("el documento no es el identificador interno")
                .isNotEqualTo(huesped.getIdHuesped());
        assertThat(dataStore.getHuespedes()).hasSize(4);
    }

    @Test
    @DisplayName("El identificador interno y el documento son datos distintos e independientes")
    void identificadorInterno_esDistintoDelDocumento() {
        Huesped huesped = huespedService.registrarHuesped(solicitud("CC90000002", "test.id", "pass123"));

        assertThat(huesped.getIdHuesped()).isNotBlank();
        assertThat(huesped.getDocumento()).isNotBlank();
        assertThat(huesped.getIdHuesped()).isNotEqualTo(huesped.getDocumento());
    }


    @Test
    @DisplayName("Rechaza un huesped sin documento")
    void registrarHuesped_sinDocumento_esRechazado() {
        HuespedRequestDTO dto = solicitud("CC90000003", "test.sinDoc", "pass123");
        dto.setDocumento(null);

        assertThatThrownBy(() -> huespedService.registrarHuesped(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documento");
        assertThat(dataStore.getHuespedes()).hasSize(3);
    }

    @Test
    @DisplayName("Rechaza un documento con solo espacios")
    void registrarHuesped_documentoEnBlanco_esRechazado() {
        HuespedRequestDTO dto = solicitud("   ", "test.espacios", "pass123");

        assertThatThrownBy(() -> huespedService.registrarHuesped(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documento");
        assertThat(dataStore.getHuespedes()).hasSize(3);
    }


    @Test
    @DisplayName("Rechaza un documento ya registrado")
    void registrarHuesped_documentoDuplicado_esRechazado() {
        HuespedRequestDTO dto = solicitud("CC10000001", "test.duplicado", "pass123");

        assertThatThrownBy(() -> huespedService.registrarHuesped(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrado");
        assertThat(dataStore.getHuespedes()).hasSize(3);
    }

    @Test
    @DisplayName("La comparacion de documentos ignora espacios al inicio y al final")
    void registrarHuesped_documentoConEspaciosDuplicado_esRechazado() {
        HuespedRequestDTO dto = solicitud("   CC10000002   ", "test.espaciosDup", "pass123");

        assertThatThrownBy(() -> huespedService.registrarHuesped(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrado");
    }

    @Test
    @DisplayName("La comparacion de documentos ignora mayusculas y minusculas")
    void registrarHuesped_documentoEnMinusculas_esRechazado() {
        HuespedRequestDTO dto = solicitud("cc10000003", "test.minusculas", "pass123");

        assertThatThrownBy(() -> huespedService.registrarHuesped(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrado");
    }


    @Test
    @DisplayName("Rechaza si falta el nombre, el apellido, el telefono o el correo")
    void registrarHuesped_datosObligatoriosFaltantes_sonRechazados() {
        assertThatThrownBy(() -> huespedService.registrarHuesped(
                vaciando(solicitud("CC90000004", "test.a", "pass123"), "nombre")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nombre");

        assertThatThrownBy(() -> huespedService.registrarHuesped(
                vaciando(solicitud("CC90000005", "test.b", "pass123"), "apellido")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("apellido");

        assertThatThrownBy(() -> huespedService.registrarHuesped(
                vaciando(solicitud("CC90000006", "test.c", "pass123"), "telefono")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("teléfono");

        assertThatThrownBy(() -> huespedService.registrarHuesped(
                vaciando(solicitud("CC90000007", "test.d", "pass123"), "correo")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("correo");

        assertThat(dataStore.getHuespedes()).hasSize(3);
    }



    @Test
    @DisplayName("Conserva la validacion de username unico")
    void registrarHuesped_usernameDuplicado_esRechazado() {
        HuespedRequestDTO yaExistente = solicitud("CC90000008", "carlos.r", "pass123");

        assertThatThrownBy(() -> huespedService.registrarHuesped(yaExistente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    @DisplayName("El username no puede repetir uno del personal del hotel")
    void registrarHuesped_usernameDelPersonal_esRechazado() {
        HuespedRequestDTO dto = solicitud("CC90000009", "admin", "pass123");

        assertThatThrownBy(() -> huespedService.registrarHuesped(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }



    @Test
    @DisplayName("Consulta los huespedes con su documento real")
    void listarHuespedes_incluyeElDocumentoReal() {
        huespedService.registrarHuesped(solicitud("CC90000010", "test.consulta", "pass123"));

        Huesped registrado = huespedService.listarTodos().stream()
                .filter(h -> "test.consulta".equals(h.getUsername()))
                .findFirst()
                .orElseThrow();

        assertThat(registrado.getDocumento()).isEqualTo("CC90000010");
        assertThat(registrado.getIdHuesped())
                .as("el documento no reemplaza al identificador interno")
                .isNotEqualTo("CC90000010");
    }



    @Test
    @DisplayName("Actualiza los datos personales conservando el identificador interno")
    void actualizarDatos_conservaElIdentificadorInterno() {
        String idAntes = huespedService.buscarPorId("H-001").orElseThrow().getIdHuesped();

        Huesped actualizado = huespedService.actualizarDatos(
                "H-001", "Carlos Andrés", "Ramírez Pérez", "CC10000999",
                "3009998888", "carlos.nuevo@email.com");

        assertThat(actualizado.getIdHuesped()).isEqualTo(idAntes);
        assertThat(actualizado.getNombre()).isEqualTo("Carlos Andrés");
        assertThat(actualizado.getApellido()).isEqualTo("Ramírez Pérez");
        assertThat(actualizado.getDocumento()).isEqualTo("CC10000999");
        assertThat(actualizado.getTelefono()).isEqualTo("3009998888");
        assertThat(actualizado.getCorreo()).isEqualTo("carlos.nuevo@email.com");
    }

    @Test
    @DisplayName("Actualizar solo telefono y correo conserva el resto, como antes")
    void actualizarDatos_soloTelefonoYCorreo_conservaElDocumento() {
        Huesped actualizado = huespedService.actualizarDatos(
                "H-002", null, null, null, "3112223333", "laura.nueva@email.com");

        assertThat(actualizado.getDocumento()).isEqualTo("CC10000002");
        assertThat(actualizado.getNombre()).isEqualTo("Laura");
        assertThat(actualizado.getTelefono()).isEqualTo("3112223333");
        assertThat(actualizado.getCorreo()).isEqualTo("laura.nueva@email.com");
    }

    @Test
    @DisplayName("No permite asignar a un huesped un documento ya registrado")
    void actualizarDatos_documentoDeOtroHuesped_esRechazado() {
        assertThatThrownBy(() -> huespedService.actualizarDatos(
                "H-001", null, null, "CC10000002", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está registrado");

        assertThat(huespedService.buscarPorId("H-001").orElseThrow().getDocumento())
                .isEqualTo("CC10000001");
    }

    @Test
    @DisplayName("Un huesped puede volver a guardar su propio documento")
    void actualizarDatos_propioDocumento_esPermitido() {
        Huesped actualizado = huespedService.actualizarDatos(
                "H-003", null, null, "CC10000003", null, null);

        assertThat(actualizado.getDocumento()).isEqualTo("CC10000003");
    }



    @Test
    @DisplayName("Los tres huespedes iniciales conservan id, username y documento propio")
    void huespedesIniciales_sonCompatibles() {
        List<Huesped> iniciales = huespedService.listarTodos();

        assertThat(iniciales).hasSize(3);
        assertThat(iniciales).extracting(Huesped::getIdHuesped)
                .containsExactly("H-001", "H-002", "H-003");
        assertThat(iniciales).extracting(Huesped::getUsername)
                .containsExactly("carlos.r", "laura.g", "andres.t");
        assertThat(iniciales).extracting(Huesped::getDocumento)
                .containsExactly("CC10000001", "CC10000002", "CC10000003");
        assertThat(iniciales).extracting(Huesped::getDocumento).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("No se puede registrar un huesped con el documento de un huesped inicial")
    void huespedInicial_bloqueaSuDocumento() {
        assertThatThrownBy(() -> huespedService.registrarHuesped(
                solicitud("CC10000001", "test.nuevo", "pass123")))
                .isInstanceOf(IllegalArgumentException.class);
    }



    @Test
    @DisplayName("Dos registros simultaneos con el mismo documento solo deja un huesped")
    void registrarHuesped_simultaneo_conMismoDocumento_soloDejaUno() throws Exception {
        int peticiones = 12;
        ExecutorService executor = Executors.newFixedThreadPool(peticiones);
        CountDownLatch arranque = new CountDownLatch(1);
        AtomicInteger creados = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < peticiones; i++) {
            final int n = i;
            tareas.add(executor.submit(() -> {
                try {
                    arranque.await();
                    huespedService.registrarHuesped(
                            solicitud("CC80000001", "concurrente." + n, "pass123"));
                    creados.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    rechazados.incrementAndGet();
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

        long conEseDocumento = huespedService.listarTodos().stream()
                .filter(h -> "CC80000001".equals(h.getDocumento()))
                .count();

        assertThat(conEseDocumento).as("solo puede existir un huesped con ese documento").isEqualTo(1);
        assertThat(creados.get()).isEqualTo(1);
        assertThat(rechazados.get()).isEqualTo(peticiones - 1);
    }



    @Test
    @DisplayName("El endpoint POST expone el documento en la respuesta")
    void endpointRegistrar_exponeElDocumento() throws Exception {
        String cuerpo = objectMapper.writeValueAsString(
                solicitud("CC70000001", "test.http", "pass123"));

        mockMvc().perform(post("/api/huespedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo)
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", "R-001")
                        .header("X-Username", "maria.l"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento").value("CC70000001"))
                .andExpect(jsonPath("$.idHuesped").isNotEmpty());
    }

    @Test
    @DisplayName("El endpoint POST responde 400 cuando falta el documento")
    void endpointRegistrar_sinDocumento_responde400() throws Exception {
        HuespedRequestDTO dto = solicitud("CC70000002", "test.http2", "pass123");
        dto.setDocumento("  ");

        mockMvc().perform(post("/api/huespedes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", "R-001")
                        .header("X-Username", "maria.l"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("El endpoint PUT conserva el identificador interno al actualizar el documento")
    void endpointActualizar_conservaElIdentificadorInterno() throws Exception {
        mockMvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/huespedes/H-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documento\":\"CC10000555\",\"telefono\":\"3000001111\"}")
                        .header("X-User-Role", "RECEPCIONISTA")
                        .header("X-User-Id", "R-001")
                        .header("X-Username", "maria.l"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idHuesped").value("H-001"))
                .andExpect(jsonPath("$.documento").value("CC10000555"))
                .andExpect(jsonPath("$.telefono").value("3000001111"));
    }



    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new HuespedController(huespedService, dataStore))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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

    private HuespedRequestDTO vaciando(HuespedRequestDTO dto, String campo) {
        switch (campo) {
            case "nombre" -> dto.setNombre("  ");
            case "apellido" -> dto.setApellido(null);
            case "telefono" -> dto.setTelefono("");
            case "correo" -> dto.setCorreo("   ");
            default -> throw new IllegalArgumentException(campo);
        }
        return dto;
    }
}
