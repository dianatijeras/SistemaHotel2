package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.PersonalLimpiezaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.RecepcionistaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.Rol;
import co.edu.uniquindio.sistemahotel.backend.enums.TurnoRecepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Administrador;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.PersonalLimpieza;
import co.edu.uniquindio.sistemahotel.backend.model.Recepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;
import co.edu.uniquindio.sistemahotel.backend.service.UsuarioService;
import co.edu.uniquindio.sistemahotel.backend.service.usuario.AltaUsuario;
import co.edu.uniquindio.sistemahotel.backend.service.usuario.PersonalLimpiezaCreator;
import co.edu.uniquindio.sistemahotel.backend.service.usuario.RecepcionistaCreator;
import co.edu.uniquindio.sistemahotel.backend.service.usuario.UsuarioCreator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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


@SpringBootTest
@AutoConfigureMockMvc
class FactoryMethodAltaUsuarioTest {

    private static final String ADMIN_ROL = "ADMIN";
    private static final String ADMIN_ID = "A-001";
    private static final String ADMIN_USER = "admin";

    @Autowired
    private DataStore dataStore;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HuespedRepository huespedRepository;

    @Autowired
    private MockMvc mockMvc;


    private List<Recepcionista> recepcionistasOriginales;
    private List<PersonalLimpieza> personalOriginal;
    private List<Administrador> administradoresOriginales;
    private List<Huesped> huespedesOriginales;

    @BeforeEach
    void prepararPersonalLimpio() {
        recepcionistasOriginales = List.copyOf(dataStore.getRecepcionistas());
        personalOriginal = List.copyOf(dataStore.getPersonalLimpieza());
        administradoresOriginales = List.copyOf(dataStore.getAdministradores());
        huespedesOriginales = List.copyOf(dataStore.getHuespedes());

        dataStore.getRecepcionistas().clear();
        dataStore.getPersonalLimpieza().clear();
        dataStore.getAdministradores().clear();
        dataStore.getAdministradores().add(
                new Administrador("A-001", "Admin", "Principal", ADMIN_USER, "admin2024"));
    }

    @AfterEach
    void restaurarPersonal() {
        dataStore.getRecepcionistas().clear();
        dataStore.getRecepcionistas().addAll(recepcionistasOriginales);
        dataStore.getPersonalLimpieza().clear();
        dataStore.getPersonalLimpieza().addAll(personalOriginal);
        dataStore.getAdministradores().clear();
        dataStore.getAdministradores().addAll(administradoresOriginales);

        dataStore.getHuespedes().clear();
        dataStore.getHuespedes().addAll(huespedesOriginales);
    }


    @Nested
    @DisplayName("1 y 2. Se crea cada tipo correctamente")
    class AltasBasicas {

        @Test
        @DisplayName("Crear una recepcionista la deja registrada con su turno")
        void creaRecepcionista() {
            Recepcionista r = usuarioService.crearRecepcionista(
                    dtoRecepcionista("Recep", "Nueva", "recep.nueva", TurnoRecepcionista.TARDE));

            assertThat(r.getIdRecepcionista()).startsWith("R-");
            assertThat(r.getRol()).isEqualTo(Rol.RECEPCIONISTA);
            assertThat(r.getTurno()).isEqualTo(TurnoRecepcionista.TARDE);
            assertThat(r.getNombre()).isEqualTo("Recep");
            assertThat(r.isActivo()).isTrue();

            assertThat(dataStore.getRecepcionistas()).contains(r);
            assertThat(usuarioRepository.buscarPorId(r.getIdRecepcionista())).contains(r);
        }

        @Test
        @DisplayName("Crear personal de limpieza lo deja registrado sin turno")
        void creaPersonalLimpieza() {
            PersonalLimpieza p = usuarioService.crearPersonalLimpieza(
                    dtoLimpieza("Limpio", "Nuevo", "limpieza.nuevo"));

            assertThat(p.getIdPersonalLimpieza()).startsWith("PL-");
            assertThat(p.getRol()).isEqualTo(Rol.PERSONAL_LIMPIEZA);
            assertThat(p.getNombre()).isEqualTo("Limpio");

            assertThat(dataStore.getPersonalLimpieza()).contains(p);
            assertThat(usuarioRepository.buscarPorId(p.getIdPersonalLimpieza())).contains(p);
        }

        @Test
        @DisplayName("Cada alta va a la lista que le corresponde")
        void cadaTipoVaASuLista() {
            Recepcionista r = usuarioService.crearRecepcionista(
                    dtoRecepcionista("A", "Uno", "a.uno", TurnoRecepcionista.MANANA));
            PersonalLimpieza p = usuarioService.crearPersonalLimpieza(
                    dtoLimpieza("B", "Dos", "b.dos"));

            assertThat(dataStore.getRecepcionistas()).containsExactly(r);
            assertThat(dataStore.getPersonalLimpieza()).containsExactly(p);
            assertThat(dataStore.getRecepcionistas())
                    .as("la recepcionista no aparece en la lista de limpieza")
                    .noneMatch(u -> u.getIdUsuario().equals(p.getIdUsuario()));
            assertThat(dataStore.getPersonalLimpieza())
                    .as("el personal de limpieza no aparece en la lista de recepcion")
                    .noneMatch(u -> u.getIdUsuario().equals(r.getIdUsuario()));
        }
    }


    @Nested
    @DisplayName("3. Unicidad de usernames")
    class UnicidadDeUsernames {

        @Test
        @DisplayName("Rechaza un username ya usado por otro empleado")
        void rechazaUsernameDeEmpleado() {
            usuarioService.crearPersonalLimpieza(dtoLimpieza("Uno", "A", "repetido.user"));

            assertThatThrownBy(() -> usuarioService.crearRecepcionista(
                    dtoRecepcionista("Dos", "B", "repetido.user", TurnoRecepcionista.MANANA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El username 'repetido.user' ya está registrado.");

            assertThat(dataStore.getRecepcionistas()).isEmpty();
        }

        @Test
        @DisplayName("Rechaza un username ya usado por un huésped")
        void rechazaUsernameDeHuesped() {
            dataStore.getHuespedes().clear();
            dataStore.getHuespedes().add(new Huesped(
                    "H-900", "Inv", "Ador", "CC99000001", "3000000000",
                    "i@e.com", "tomado.huesped", "pass123"));

            assertThatThrownBy(() -> usuarioService.crearRecepcionista(
                    dtoRecepcionista("A", "B", "tomado.huesped", TurnoRecepcionista.MANANA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tomado.huesped");
        }

        @Test
        @DisplayName("Rechaza el mismo username sin importar las mayúsculas")
        void comparaSinDistinguirMayusculas() {
            usuarioService.crearPersonalLimpieza(dtoLimpieza("Uno", "A", "mayusculas.user"));

            assertThatThrownBy(() -> usuarioService.crearPersonalLimpieza(
                    dtoLimpieza("Dos", "B", "MAYUSCULAS.USER")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rechaza el username de un administrador")
        void rechazaUsernameDeAdministrador() {
            assertThatThrownBy(() -> usuarioService.crearPersonalLimpieza(
                    dtoLimpieza("A", "B", ADMIN_USER)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }


    @Nested
    @DisplayName("4. Contrato HTTP conservado")
    class ContratoHttp {

        @Test
        @DisplayName("Alta correcta responde 201 con el cuerpo de siempre")
        void altaResponde201() throws Exception {
            String cuerpo = mockMvc.perform(post("/api/usuarios/recepcionistas")
                            .header("X-User-Role", ADMIN_ROL)
                            .header("X-User-Id", ADMIN_ID)
                            .header("X-Username", ADMIN_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Http\",\"apellido\":\"Alta\","
                                    + "\"turno\":\"NOCHE\",\"username\":\"http.alta\","
                                    + "\"password\":\"clave123\"}"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(cuerpo).contains("\"username\":\"http.alta\"");
            assertThat(cuerpo).contains("\"rol\":\"RECEPCIONISTA\"");
            assertThat(cuerpo).doesNotContain("password");
            assertThat(mockMvc.perform(post("/api/usuarios/recepcionistas")
                            .header("X-User-Role", ADMIN_ROL)
                            .header("X-User-Id", ADMIN_ID)
                            .header("X-Username", ADMIN_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Http\",\"apellido\":\"Alta\","
                                    + "\"turno\":\"NOCHE\",\"username\":\"otro.http\","
                                    + "\"password\":\"clave123\"}"))
                    .andReturn().getResponse().getStatus()).isEqualTo(201);
        }

        @Test
        @DisplayName("Falta el turno: responde 400 con el mensaje de siempre")
        void faltaTurnoResponde400() throws Exception {
            int codigo = mockMvc.perform(post("/api/usuarios/recepcionistas")
                            .header("X-User-Role", ADMIN_ROL)
                            .header("X-User-Id", ADMIN_ID)
                            .header("X-Username", ADMIN_USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Sin\",\"apellido\":\"Turno\","
                                    + "\"turno\":null,\"username\":\"sin.turno\","
                                    + "\"password\":\"clave123\"}"))
                    .andReturn().getResponse().getStatus();

            assertThat(codigo).isEqualTo(400);
            assertThat(dataStore.getRecepcionistas()).isEmpty();
        }

        @Test
        @DisplayName("Sin administrador: responde 409 y no crea nada")
        void sinAdministradorResponde409() throws Exception {
            int codigo = mockMvc.perform(post("/api/usuarios/personal-limpieza")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Sin\",\"apellido\":\"Admin\","
                                    + "\"username\":\"sin.admin\",\"password\":\"clave123\"}"))
                    .andReturn().getResponse().getStatus();

            assertThat(codigo).isEqualTo(409);
            assertThat(dataStore.getPersonalLimpieza()).isEmpty();
        }

        @Test
        @DisplayName("Mensajes de validación no han cambiado")
        void mensajesDeValidacionIntactos() {
            assertThatThrownBy(() -> usuarioService.crearRecepcionista(
                    dtoRecepcionista("", "B", "u", TurnoRecepcionista.MANANA)))
                    .hasMessage("El nombre del recepcionista es obligatorio.");
            assertThatThrownBy(() -> usuarioService.crearRecepcionista(
                    dtoRecepcionista("A", "", "u", TurnoRecepcionista.MANANA)))
                    .hasMessage("El apellido del recepcionista es obligatorio.");
            assertThatThrownBy(() -> usuarioService.crearPersonalLimpieza(
                    dtoLimpieza("", "B", "u")))
                    .hasMessage("El nombre del personal de limpieza es obligatorio.");
            assertThatThrownBy(() -> usuarioService.crearPersonalLimpieza(
                    dtoLimpieza("A", "", "u")))
                    .hasMessage("El apellido del personal de limpieza es obligatorio.");
            assertThatThrownBy(() -> usuarioService.crearPersonalLimpieza(
                    dtoLimpieza("A", "B", "")))
                    .hasMessage("El usuario de acceso es obligatorio.");

            // La contraseña corta solo se puede probar construyendo el DTO a mano: el ayudante
            PersonalLimpiezaRequestDTO corta = dtoLimpieza("A", "B", "u");
            corta.setPassword("abc");
            assertThatThrownBy(() -> usuarioService.crearPersonalLimpieza(corta))
                    .hasMessage("La contraseña debe tener mínimo 4 caracteres.");
        }
    }


    @Nested
    @DisplayName("5. Polimorfismo genuino del Factory Method")
    class Polimorfismo {

        /**
         * Tercer creador que produce un tipo <b>que el repositorio no conoce</b>. Sirve para
         */
        private final class SupervisorCreator extends UsuarioCreator {

            SupervisorCreator(UsuarioRepository usuarios, HuespedRepository huespedes) {
                super(usuarios, huespedes);
            }

            @Override
            protected void validarAtributosPropios(AltaUsuario alta) {
                if (alta.turno() != null) {
                    throw new IllegalArgumentException("Un supervisor no tiene turno.");
                }
            }

            @Override
            protected Usuario crearEmpleado(AltaUsuario alta) {
                return new Supervisor(generarIdLibre(), alta.nombre(), alta.apellido());
            }

            @Override
            protected String formatoDeId() {
                return "SUP-%02d";
            }

            @Override
            protected String nombreDelTipo() {
                return "supervisor";
            }

            @Override
            protected long cantidadDeEsteTipo() {
                return 0;
            }
        }

        /**
         * Clase auxiliar que representa un supervisor, un tipo de empleado que el repositorio no
         */
        static final class Supervisor extends Usuario {
            private final String idSupervisor;
            private final String nombre;
            private final String apellido;

            Supervisor(String id, String nombre, String apellido) {
                super(id, "sup." + nombre.toLowerCase(), "clave123", Rol.ADMIN);
                this.idSupervisor = id;
                this.nombre = nombre;
                this.apellido = apellido;
            }

            @Override
            public String getNombre() {
                return nombre;
            }

            @Override
            public void setNombre(String nombre) {
            }

            @Override
            public String getApellido() {
                return apellido;
            }

            @Override
            public void setApellido(String apellido) {
            }

            @Override
            public String getNombreCompleto() {
                return nombre + " " + apellido;
            }
        }

        /**
         * Creador que produce un tipo de empleado que el repositorio SI conoce, para mostrar que
         */
        private final class SupervisorSobreAdminCreator extends UsuarioCreator {

            SupervisorSobreAdminCreator(UsuarioRepository usuarios, HuespedRepository huespedes) {
                super(usuarios, huespedes);
            }

            @Override
            protected void validarAtributosPropios(AltaUsuario alta) {
                if (alta.turno() != null) {
                    throw new IllegalArgumentException("Un supervisor no tiene turno.");
                }
            }

            @Override
            protected Usuario crearEmpleado(AltaUsuario alta) {
                return new Administrador(generarIdLibre(), alta.nombre(), alta.apellido(),
                        alta.usernameNormalizado(), alta.password());
            }

            @Override
            protected String formatoDeId() {
                return "SUP-%03d";
            }

            @Override
            protected String nombreDelTipo() {
                return "supervisor";
            }

            @Override
            protected long cantidadDeEsteTipo() {
                return 0;
            }
        }

        @Test
        @DisplayName("La jerarquia esta abierta: un creador nuevo dirige el algoritmo sin cambios")
        void laJerarquiaEstaAbierta() {
            SupervisorCreator supervisor = new SupervisorCreator(usuarioRepository, huespedRepository);

            assertThatThrownBy(() -> supervisor.crear(
                    new AltaUsuario("Sofía", "Nueva", "sofia.nueva", "clave123", TurnoRecepcionista.MANANA)))
                    .as("la validacion propia del creador nuevo se ejecuta")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Un supervisor no tiene turno.");

            assertThatThrownBy(() -> supervisor.crear(
                    new AltaUsuario("Sofía", "Nueva", "sofia.nueva", "clave123", null)))
                    .as("el producto lo decide el creador, no un switch")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Supervisor");

            Usuario creado = new SupervisorSobreAdminCreator(usuarioRepository, huespedRepository)
                    .crear(new AltaUsuario("Sofía", "Nueva", "sofia.admin", "clave123", null));
            assertThat(creado).isInstanceOf(Administrador.class);
            assertThat(creado.getIdUsuario()).startsWith("SUP-");
            assertThat(dataStore.getAdministradores())
                    .as("el producto se guardo donde correspondia")
                    .anyMatch(a -> a.getIdUsuario().equals(creado.getIdUsuario()));
        }

        @Test
        @DisplayName("Cada creador concreto produce un producto y un identificador distintos")
        void cadaCreadorProduceSuProducto() {
            UsuarioCreator recepcionista = new RecepcionistaCreator(usuarioRepository, huespedRepository);
            UsuarioCreator limpieza = new PersonalLimpiezaCreator(usuarioRepository, huespedRepository);

            Usuario a = recepcionista.crear(
                    new AltaUsuario("A", "Uno", "pol.a", "clave123", TurnoRecepcionista.MANANA));
            Usuario b = limpieza.crear(
                    new AltaUsuario("B", "Dos", "pol.b", "clave123", null));

            assertThat(a).isInstanceOf(Recepcionista.class);
            assertThat(b).isInstanceOf(PersonalLimpieza.class);
            assertThat(a.getIdUsuario()).startsWith("R-");
            assertThat(b.getIdUsuario()).startsWith("PL-");
        }

        @Test
        @DisplayName("La validacion propia de cada creador se ejecuta")
        void cadaCreadorValidaLoSuyo() {
            UsuarioCreator recepcionista = new RecepcionistaCreator(usuarioRepository, huespedRepository);

            assertThatThrownBy(() -> recepcionista.crear(
                    new AltaUsuario("A", "Uno", "sin.turno", "clave123", null)))
                    .as("la recepcionista exige turno")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El turno del recepcionista es obligatorio.");
        }

        @Test
        @DisplayName("El algoritmo vive en la clase abstracta, no en cada creador")
        void elAlgoritmoEstaEnLaAbstraccion() throws Exception {
            assertThat(UsuarioCreator.class.getDeclaredMethod(
                    "crear", AltaUsuario.class)).isNotNull();
            assertThat(RecepcionistaCreator.class.getDeclaredMethods())
                    .as("el creador concreto no redefine el algoritmo")
                    .noneMatch(m -> m.getName().equals("crear"));
            assertThat(PersonalLimpiezaCreator.class.getDeclaredMethods())
                    .as("el creador concreto no redefine el algoritmo")
                    .noneMatch(m -> m.getName().equals("crear"));
        }
    }


    @Nested
    @DisplayName("6 y 7. Identificadores unicos ante concurrencia y eliminaciones")
    class Identificadores {

        @Test
        @DisplayName("Doce altas simultaneas de recepcionistas no repiten identificador")
        void altasConcurrentesSinRepetirId() throws Exception {
            int hilos = 12;
            CountDownLatch arranque = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(hilos);
            List<Future<Recepcionista>> futuros = new ArrayList<>();
            AtomicInteger creditos = new AtomicInteger(hilos);

            for (int i = 0; i < hilos; i++) {
                final int n = i;
                futuros.add(pool.submit(() -> {
                    try {
                        arranque.await();
                        return usuarioService.crearRecepcionista(
                                dtoRecepcionista("Conc", "User" + n, "conc." + n,
                                        TurnoRecepcionista.MANANA));
                    } finally {
                        creditos.decrementAndGet();
                    }
                }));
            }

            arranque.countDown();
            List<String> ids = new ArrayList<>();
            for (Future<Recepcionista> f : futuros) {
                ids.add(f.get(20, TimeUnit.SECONDS).getIdRecepcionista());
            }
            pool.shutdownNow();

            assertThat(creditos.get())
                    .as("todos los hilos pudieron volver: el monitor se libera")
                    .isZero();
            assertThat(ids).as("12 identificadores distintos").doesNotHaveDuplicates();
            assertThat(dataStore.getRecepcionistas()).hasSize(hilos);
            assertThat(ids).allMatch(id -> id.matches("R-\\d{3}"));
        }

        @Test
        @DisplayName("Doce altas simultaneas con el MISMO username: solo uno prospera")
        void usernamesConcurrentesSinRepeticion() throws Exception {
            int hilos = 12;
            CountDownLatch arranque = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(hilos);
            List<Future<Boolean>> futuros = new ArrayList<>();

            for (int i = 0; i < hilos; i++) {
                final int n = i;
                futuros.add(pool.submit(() -> {
                    arranque.await();
                    try {
                        usuarioService.crearPersonalLimpieza(
                                dtoLimpieza("Igual", "User", "unico.compartido"));
                        return Boolean.TRUE;
                    } catch (IllegalArgumentException e) {
                        return Boolean.FALSE;
                    }
                }));
            }

            arranque.countDown();
            int exitos = 0;
            for (Future<Boolean> f : futuros) {
                if (Boolean.TRUE.equals(f.get(20, TimeUnit.SECONDS))) {
                    exitos++;
                }
            }
            pool.shutdownNow();

            assertThat(exitos).as("la unicidad es una regla de negocio, atomica").isEqualTo(1);
            assertThat(dataStore.getPersonalLimpieza()).hasSize(1);
        }

        @Test
        @DisplayName("Tras eliminar un empleado, el siguiente no reutiliza su identificador")
        void noReutilizaIdTrasEliminar() {
            Recepcionista primera = usuarioService.crearRecepcionista(
                    dtoRecepcionista("Uno", "A", "eliminar.uno", TurnoRecepcionista.MANANA));
            Recepcionista segunda = usuarioService.crearRecepcionista(
                    dtoRecepcionista("Dos", "B", "eliminar.dos", TurnoRecepcionista.MANANA));

            assertThat(List.of(primera.getIdRecepcionista(), segunda.getIdRecepcionista()))
                    .doesNotHaveDuplicates();

            dataStore.getRecepcionistas().remove(primera);
            assertThat(dataStore.getRecepcionistas()).hasSize(1);

            Recepcionista tercera = usuarioService.crearRecepcionista(
                    dtoRecepcionista("Tres", "C", "eliminar.tres", TurnoRecepcionista.MANANA));

            assertThat(tercera.getIdRecepcionista())
                    .as("el identificador liberado no se vuelve a usar")
                    .isNotEqualTo(primera.getIdRecepcionista());
            assertThat(usuarioRepository.buscarPorId(primera.getIdRecepcionista()))
                    .as("el hueco queda libre y el siguiente lo toma")
                    .isEmpty();
        }

        @Test
        @DisplayName("El formato publico del identificador no cambia")
        void formatoDeIdentificador() {
            Recepcionista r = usuarioService.crearRecepcionista(
                    dtoRecepcionista("A", "Uno", "formato.r", TurnoRecepcionista.MANANA));
            PersonalLimpieza p = usuarioService.crearPersonalLimpieza(
                    dtoLimpieza("B", "Dos", "formato.l"));

            assertThat(r.getIdRecepcionista()).matches("^R-\\d{3}$");
            assertThat(p.getIdPersonalLimpieza()).matches("^PL-\\d{3}$");
        }
    }


    private RecepcionistaRequestDTO dtoRecepcionista(String nombre, String apellido,
                                                     String username, TurnoRecepcionista turno) {
        RecepcionistaRequestDTO dto = new RecepcionistaRequestDTO();
        dto.setNombre(nombre);
        dto.setApellido(apellido);
        dto.setTurno(turno);
        dto.setUsername(username);
        dto.setPassword("clave123");
        return dto;
    }

    private PersonalLimpiezaRequestDTO dtoLimpieza(String nombre, String apellido, String username) {
        PersonalLimpiezaRequestDTO dto = new PersonalLimpiezaRequestDTO();
        dto.setNombre(nombre);
        dto.setApellido(apellido);
        dto.setUsername(username);
        dto.setPassword("clave123");
        return dto;
    }
}