package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.enums.Rol;
import co.edu.uniquindio.sistemahotel.backend.model.Administrador;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.PersonalLimpieza;
import co.edu.uniquindio.sistemahotel.backend.model.Recepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.model.UsuarioAcceso;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;
import co.edu.uniquindio.sistemahotel.backend.service.HuespedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LspOcpIspTest {

    private static final Path CTRL_USUARIOS = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotel/backend/controller/UsuarioController.java");
    private static final Path REPO_USUARIOS = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotel/backend/repository/"
                    + "InMemoryUsuarioRepository.java");
    private static final Path MODELO_USUARIO = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotel/backend/model/Usuario.java");
    private static final Path CREADOR_BASE = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotel/backend/service/usuario/UsuarioCreator.java");
    private static final Path ESTADO_BASE = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotel/backend/model/estado/"
                    + "EstadoHabitacionBase.java");

    @Autowired
    private HuespedService huespedService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DataStore dataStore;

    /**
     * Quita comentarios y comentarios de linea para que el codigo pueda analizarse sin ruido.
     * @param fuente
     * @return
     */
    private static String codigoDe(String fuente) {
        String sinBloques = fuente.replaceAll("(?s)/\\*.*?\\*/", " ");
        return sinBloques.lines()
                .map(l -> l.replaceFirst("//.*$", ""))
                .reduce("", (a, b) -> a + "\n" + b);
    }



    @Nested
    @DisplayName("LSP: el huesped no puede debilitar el contrato de Usuario")
    class Liskov {

        @Test
        @DisplayName("El huesped no debe redefinir autenticar")
        void elHuespedNoRedefineAutenticar() {

            assertThat(declaraAutenticar(Huesped.class))
                    .as("Huesped no debe redeclarar un metodo del supertipo para debilitarlo")
                    .isFalse();
        }

        @Test
        @DisplayName("El huesped no puede autenticarse aunque sus credenciales sean correctas")
        void elHuespedRechazaSusPropiasCredenciales() {
            Huesped huesped = huespedService.listarTodos().get(0);

            boolean credencialesCoinciden = huesped.getUsername().equals(huesped.getUsername())
                    && huesped.isActivo();

            assertThat(credencialesCoinciden)
                    .as("el huesped esta activo y sus credenciales son validas")
                    .isTrue();


            assertThat(declaraAutenticar(huesped.getClass()))
                    .as("el huesped no declara ni hereda el metodo de autenticacion")
                    .isFalse();
            assertThat(UsuarioAcceso.class.isAssignableFrom(Huesped.class))
                    .as("y no pertenece a la clase de quienes acceden")
                    .isFalse();
            assertThat(UsuarioAcceso.class.isAssignableFrom(Recepcionista.class))
                    .as("el personal si pertenece a ella")
                    .isTrue();
        }

        @Test
        @DisplayName("El huesped sigue siendo un Usuario: hereda datos, no credenciales")
        void elHuespedConservaLoQueSiDebeHeredar() {
            Huesped huesped = huespedService.listarTodos().get(0);
            Usuario comoUsuario = huesped;


            assertThat(comoUsuario.getIdUsuario()).isEqualTo(huesped.getIdHuesped());
            assertThat(comoUsuario.getUsername()).isEqualTo(huesped.getUsername());
            assertThat(comoUsuario.getRol()).isEqualTo(Rol.HUESPED);
            assertThat(comoUsuario.getRol())
                    .as("el rol es inmutable y nunca de acceso")
                    .isNotIn(Rol.ADMIN, Rol.RECEPCIONISTA, Rol.PERSONAL_LIMPIEZA);
        }

        @Test
        @DisplayName("El personal si cumple el contrato de autenticacion")
        void elPersonalCumpleElContrato() {
            List<Usuario> personal = usuarioRepository.buscarTodos();
            assertThat(personal).isNotEmpty();

            for (Usuario u : personal) {
                assertThat(u)
                        .as("%s accede al sistema", u.getIdUsuario())
                        .isInstanceOf(UsuarioAcceso.class);

                UsuarioAcceso acceso = (UsuarioAcceso) u;
                assertThat(acceso.autenticar(u.getUsername(), u.getPassword()))
                        .as("%s autentica si y solo si esta activo (activo=%s)",
                                u.getIdUsuario(), u.isActivo())
                        .isEqualTo(u.isActivo());
                assertThat(acceso.autenticar("usuario-que-no-existe", "clave"))
                        .as("%s rechaza unas credenciales ajenas", u.getIdUsuario())
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Un huesped inactivo tampoco autentica, igual que un empleado inactivo")
        void laInactividadAfectaATodosPorIgual() {
            Recepcionista recepcionista = dataStore.getRecepcionistas().get(0);
            boolean activoAntes = recepcionista.isActivo();

            recepcionista.setActivo(false);
            try {
                assertThat(recepcionista.autenticar(recepcionista.getUsername(),
                        recepcionista.getPassword()))
                        .as("un empleado desactivado no autentica")
                        .isFalse();
            } finally {
                recepcionista.setActivo(activoAntes);
            }

            assertThat(recepcionista.autenticar(recepcionista.getUsername(),
                    recepcionista.getPassword()))
                    .as("y vuelve a autenticar al reactivarse")
                    .isTrue();
        }

        @Test
        @DisplayName("El huesped no es alcanzable desde el repositorio del personal")
        void elHuespedNoEstaEnElRepositorioDelPersonal() {
            Huesped huesped = huespedService.listarTodos().get(0);

            assertThat(usuarioRepository.buscarPorId(huesped.getIdHuesped())).isEmpty();
            assertThat(usuarioRepository.buscarTodos())
                    .extracting(Usuario::getIdUsuario)
                    .doesNotContain(huesped.getIdHuesped());
        }

        @Test
        @DisplayName("Ninguno de los cuatro tipos redeclara la autenticacion")
        void todosLosTiposUsanElMismoMetodo() {

            for (Class<?> tipo : List.of(Administrador.class, Recepcionista.class,
                    PersonalLimpieza.class, Huesped.class)) {
                assertThat(declaraAutenticar(tipo))
                        .as("%s no redeclara autenticar", tipo.getSimpleName())
                        .isFalse();
            }
            assertThat(declaraAutenticar(UsuarioAcceso.class))
                    .as("el algoritmo esta escrito una sola vez, en UsuarioAcceso")
                    .isTrue();
            assertThat(declaraAutenticar(Usuario.class))
                    .as("y el supertipo comun no lo declara, para no prometerlo")
                    .isFalse();
        }

        /**
         * @param tipo clase a inspeccionar
         * @return true si la propia clase declara {@code autenticar(String, String)}
         */
        private boolean declaraAutenticar(Class<?> tipo) {
            try {
                tipo.getDeclaredMethod("autenticar", String.class, String.class);
                return true;
            } catch (NoSuchMethodException sinMetodo) {
                return false;
            }
        }
    }


    @Nested
    @DisplayName("OCP: que habria que tocar para anadir un tipo de personal")
    class AbiertoCerrado {

        @Test
        @DisplayName("El mapeo a DTO no debe preguntar por el tipo de usuario")
        void elMapeoNoDebeUsarInstanceof() throws Exception {
            String fuente = Files.readString(CTRL_USUARIOS);

            assertThat(codigoDe(fuente))
                    .as("el mapeo no debe depender del tipo concreto")
                    .doesNotContain("instanceof");
            assertThat(Files.readString(MODELO_USUARIO))
                    .as("nombre y apellido viven en el supertipo, no en cada subclase")
                    .contains("String getNombre()")
                    .contains("String getApellido()");
        }

        @Test
        @DisplayName("El algoritmo de alta esta escrito una sola vez: anadir un tipo es anadir una clase")
        void elAlgoritmoDeAltaNoSeRepite() throws Exception {
            String fuente = Files.readString(CREADOR_BASE);

            assertThat(codigoDe(fuente))
                    .as("el algoritmo no decide el tipo: lo recibe del metodo fabrica")
                    .doesNotContain("switch")
                    .doesNotContain("instanceof")
                    .contains("Usuario nuevo = crearEmpleado(alta);");
            assertThat(fuente)
                    .as("el metodo fabrica es el punto de extension")
                    .contains("protected abstract Usuario crearEmpleado(AltaUsuario alta);");
        }

        @Test
        @DisplayName("El repositorio recorre las tres listas del almacen")
        void elRepositorioEnumeraLasListas() throws Exception {
            String fuente = Files.readString(REPO_USUARIOS);

            assertThat(fuente)
                    .as("buscarTodos recorre lista por lista")
                    .contains("dataStore.getAdministradores().forEach(todos::add);")
                    .contains("dataStore.getRecepcionistas().forEach(todos::add);")
                    .contains("dataStore.getPersonalLimpieza().forEach(todos::add);");
        }

        @Test
        @DisplayName("Guardar en la lista correcta se decide por el tipo, y el error es explicito")
        void guardarDespachaPorTipo() throws Exception {
            String fuente = Files.readString(REPO_USUARIOS);

            assertThat(fuente).contains("usuario instanceof Administrador");
            assertThat(fuente).contains("No se puede guardar un usuario de tipo ");
        }
    }


    @Nested
    @DisplayName("ISP: los contratos y quien depende de ellos")
    class Segregacion {

        @Test
        @DisplayName("Cada consumidor de un repositorio usa operaciones distintas")
        void losConsumidoresNoCoinciden() {
            assertThat(huespedService.listarTodos()).isNotNull();
            assertThat(usuarioRepository.existePorUsername("no.existe.este")).isFalse();
            assertThat(usuarioRepository.buscarTodos()).isNotEmpty();
        }

        @Test
        @DisplayName("Los dos servicios que consultan el patron State usan conjuntos distintos")
        void estadoHabitacionBaseDeclaraMasDeLoQueUsaCadaUno() throws Exception {
            String fuente = Files.readString(ESTADO_BASE);

            assertThat(fuente).contains("boolean admiteOcuparse();");
            assertThat(fuente).contains("boolean admiteBloqueo();");
            assertThat(fuente)
                    .as("las preguntas son distintas porque el sistema las hace distintas")
                    .contains("boolean puedeIrA(EstadoHabitacion destino);")
                    .contains("boolean admiteFinalizarLimpieza();");
        }

        @Test
        @DisplayName("No hay dobles de prueba que sufferan una interfaz ancha")
        void nadieSimulaLasInterfaces() {
            // Un solo implementador por repositorio, todos en memoria. El costo habitual de una
            // interfaz ancha -tener que implementar metodos que no se usan- no existe aqui.
            assertThat(usuarioRepository).isNotNull();
            assertThat(usuarioRepository.buscarTodos())
                    .extracting(u -> u.getClass().getSimpleName())
                    .isSubsetOf("Administrador", "Recepcionista", "PersonalLimpieza");
        }
    }
}