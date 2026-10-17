package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoPago;
import co.edu.uniquindio.sistemahotel.backend.enums.TurnoRecepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Administrador;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.PersonalLimpieza;
import co.edu.uniquindio.sistemahotel.backend.model.Recepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;
import co.edu.uniquindio.sistemahotel.backend.service.DashboardService;
import co.edu.uniquindio.sistemahotel.backend.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class DashboardYsrpTest {

    private static final Path CTRL_DASHBOARD = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotelmanager/Controller/DashboardController.java");
    private static final Path CTRL_USUARIOS = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotelmanager/Controller/UsuarioController.java");

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DataStore dataStore;

    @BeforeEach
    void reiniciar() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones()
                .forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
    }


    @Nested
    @DisplayName("DIP: los controladores no dependen del almacenamiento")
    class DipEnControladores {

        @Test
        @DisplayName("DashboardController no menciona DataStore")
        void dashboardSinAlmacen() throws Exception {
            String fuente = Files.readString(CTRL_DASHBOARD);

            assertThat(fuente)
                    .as("el controlador del panel solo recibe y devuelve")
                    .doesNotContain("DataStore")
                    .doesNotContain("dataStore");
            assertThat(fuente)
                    .as("delega el calculo en el servicio")
                    .contains("DashboardService")
                    .contains("dashboardService.estadisticas()");
        }

        @Test
        @DisplayName("UsuarioController solo conserva el acceso al almacen para autorizar")
        void usuariosSoloMantieneElDeAutorizacion() throws Exception {
            String fuente = Files.readString(CTRL_USUARIOS);

            int lecturas = (int) fuente.lines()
                    .filter(l -> !l.trim().startsWith("*") && !l.trim().startsWith("//")
                            && !l.trim().startsWith("/*"))
                    .filter(l -> l.contains("dataStore."))
                    .count();

            assertThat(lecturas)
                    .as("solo queda la lectura de la autorizacion")
                    .isEqualTo(1);
            assertThat(fuente)
                    .as("listado y estado ya no se resuelven en el controlador")
                    .contains("usuarioService.listarTodos()")
                    .contains("usuarioService.alternarEstado(id)");
        }

        @Test
        @DisplayName("Los servicios no dependen del almacen")
        void serviciosSinAlmacen() throws Exception {
            for (String nombre : List.of("DashboardService", "UsuarioService")) {
                Path p = Path.of("src/main/java/co/edu/uniquindio/sistemahotelmanager/service/"
                        + nombre + ".java");
                assertThat(Files.readString(p))
                        .as("%s se apoya en repositorios", nombre)
                        .doesNotContain("dataStore");
            }
        }
    }


    @Nested
    @DisplayName("SRP: la logica de negocio esta en los servicios")
    class SrpEnServicios {

        @Test
        @DisplayName("El panel devuelve las quince claves, en el mismo orden")
        void elPanelMantieneSusClaves() {
            Map<String, Object> stats = dashboardService.estadisticas();

            assertThat(stats.keySet()).containsExactly(
                    "totalHabitaciones", "habitacionesDisponibles", "habitacionesOcupadas",
                    "habitacionesEnLimpieza", "totalHuespedes", "totalReservas",
                    "reservasActivas", "reservasEnProceso", "estadiasEnCurso",
                    "pagosPendientes", "totalPagado", "checkoutsHoy",
                    "totalUsuarios", "totalRecepcionistas", "totalPersonalLimpieza");
        }

        @Test
        @DisplayName("Las cifras coinciden con las que se contaban directamente en el almacen")
        void lasCifrasNoCambian() {
            Map<String, Object> stats = dashboardService.estadisticas();

            assertThat(((Number) stats.get("totalHabitaciones")).longValue())
                    .isEqualTo(dataStore.getHabitaciones().size());
            assertThat(((Number) stats.get("totalReservas")).longValue())
                    .isEqualTo(dataStore.getReservas().size());
            assertThat(((Number) stats.get("totalHuespedes")).longValue())
                    .isEqualTo(dataStore.getHuespedes().size());
            assertThat(((Number) stats.get("totalUsuarios")).intValue())
                    .isEqualTo(dataStore.getAdministradores().size()
                            + dataStore.getRecepcionistas().size()
                            + dataStore.getPersonalLimpieza().size());
            assertThat(((Number) stats.get("totalRecepcionistas")).intValue())
                    .isEqualTo(dataStore.getRecepcionistas().size());
            assertThat(((Number) stats.get("totalPersonalLimpieza")).intValue())
                    .isEqualTo(dataStore.getPersonalLimpieza().size());
            assertThat(((Number) stats.get("pagosPendientes")).longValue())
                    .isEqualTo(dataStore.getPagos().stream()
                            .filter(p -> p.getEstadoPago()
                                    == EstadoPago.PENDIENTE)
                            .count());
        }

        @Test
        @DisplayName("El listado del personal conserva el orden de siempre")
        void elListadoConservaElOrden() {
            dataStore.getAdministradores().add(
                    new Administrador("A-777", "Ord", "Uno", "ord.uno", "clave123"));
            dataStore.getPersonalLimpieza().add(
                    new PersonalLimpieza("PL-777", "Ord", "Dos", "ord.dos", "clave123"));

            List<Usuario> personal = usuarioService.listarTodos();

            assertThat(personal).extracting(Usuario::getIdUsuario)
                    .containsSubsequence("A-777", "PL-777");
            assertThat(personal.stream().filter(u -> u instanceof Administrador).count())
                    .isEqualTo(dataStore.getAdministradores().size());
        }

        @Test
        @DisplayName("Alternar estado devuelve el empleado y no existe si el id no esta")
        void alternarEstado() {
            Recepcionista r = new Recepcionista("R-777", "Alt", "Uno",
                    TurnoRecepcionista.MANANA,
                    "alt.uno", "clave123");
            dataStore.getRecepcionistas().add(r);
            boolean antes = r.isActivo();

            Optional<Usuario> resultado = usuarioService.alternarEstado("R-777");

            assertThat(resultado).contains(r);
            assertThat(r.isActivo()).as("el estado se invierte").isEqualTo(!antes);

            assertThat(usuarioService.alternarEstado("R-999")).isEmpty();
            assertThat(usuarioService.listarTodos())
                    .as("un id inexistente no crea ni altera a nadie")
                    .filteredOn(u -> "R-999".equals(u.getIdUsuario()))
                    .isEmpty();
        }

        @Test
        @DisplayName("El servicio de personal y el repositorio ven la misma instancia")
        void identidadCompartida() {
            Recepcionista r = new Recepcionista("R-778", "Id", "Uno",
                    TurnoRecepcionista.MANANA,
                    "id.uno", "clave123");
            dataStore.getRecepcionistas().add(r);

            usuarioService.alternarEstado("R-778");

            assertThat(usuarioRepository.buscarPorId("R-778"))
                    .contains(r)
                    .get()
                    .satisfies(u -> assertThat(u.isActivo()).isFalse());
        }
    }


    @Nested
    @DisplayName("Lo que no se toco en esta etapa")
    class LoQueNoSeToco {

        @Test
        @DisplayName("AuthController sigue leyendo el almacen: la autenticacion no se mueve")
        void authControllerIntacto() throws Exception {
            Path auth = Path.of(
                    "src/main/java/co/edu/uniquindio/sistemahotelmanager/Controller/AuthController.java");

            assertThat(Files.readString(auth))
                    .as("el login se deja como estaba")
                    .contains("DataStore")
                    .contains("autenticar(username, password)");
        }

        @Test
        @DisplayName("El patron State sigue decidiendo el estado de las habitaciones")
        void stateSigueParticipando() throws Exception {
            Path patron = Path.of(
                    "src/main/java/co/edu/uniquindio/sistemahotelmanager/model/estado/"
                            + "EstadoHabitacionBase.java");
            assertThat(Files.readString(patron)).contains("boolean puedeIrA");

            String controller = Files.readString(CTRL_DASHBOARD);
            assertThat(controller)
                    .as("el panel lee el estado, no lo decide")
                    .doesNotContain("EstadoHabitacion");
        }

        @Test
        @DisplayName("El patron Factory Method sigue siendo quien construye el personal")
        void factoryMethodSigueParticipando() throws Exception {
            Path creator = Path.of(
                    "src/main/java/co/edu/uniquindio/sistemahotelmanager/service/usuario/"
                            + "UsuarioCreator.java");
            assertThat(Files.readString(creator)).contains("protected abstract Usuario crearEmpleado");

            String servicio = Files.readString(
                    Path.of("src/main/java/co/edu/uniquindio/sistemahotelmanager/service/UsuarioService.java"));
            assertThat(servicio)
                    .as("el alta sigue pasando por los creadores")
                    .contains("recepcionistaCreator.crear(")
                    .contains("personalLimpiezaCreator.crear(")
                    .doesNotContain("new Recepcionista(")
                    .doesNotContain("new PersonalLimpieza(");
        }

        @Test
        @DisplayName("Los huespedes siguen registrados y con username unico")
        void huespedesIntactos() {
            dataStore.getHuespedes().add(new Huesped(
                    "H-990", "Hue", "Sped", "CC99000099", "3000000000",
                    "h@e.com", "hue.sped", "pass123"));

            assertThat(usuarioService.listarTodos())
                    .as("un huesped no aparece en el personal")
                    .extracting(Usuario::getUsername)
                    .doesNotContain("hue.sped");
            assertThat(usuarioRepository.existePorUsername("hue.sped")).isFalse();
        }
    }
}