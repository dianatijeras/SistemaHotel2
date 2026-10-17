package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.HuespedRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.InMemoryHuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import co.edu.uniquindio.sistemahotel.backend.service.HuespedService;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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


@SpringBootTest
@AutoConfigureMockMvc
class HuespedRepositoryTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final String RECEP_ID = "R-001";
    private static final String RECEP_USER = "maria.l";
    private static final String RECEP_PASS = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HuespedRepository huespedRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HuespedService huespedService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EstadiaService estadiaService;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();

        dataStore.getHuespedes().clear();
        dataStore.getHuespedes().add(new Huesped("H-001", "Carlos", "Ramírez", "CC10000001",
                "3001234567", "carlos@email.com", "carlos.r", "pass123"));
        dataStore.getHuespedes().add(new Huesped("H-002", "Laura", "Gómez", "CC10000002",
                "3109876543", "laura@email.com", "laura.g", "pass123"));
        dataStore.getHuespedes().add(new Huesped("H-003", "Andrés", "Torres", "CC10000003",
                "3204567890", "andres@email.com", "andres.t", "pass123"));
    }



    @Nested
    @DisplayName("1. Contrato HuespedRepository")
    class Contrato {

        @Test
        @DisplayName("El servicio declara una dependencia de la abstraccion, no de la implementacion")
        void servicio_dependeDeLaAbstraccion() {
            var constructor = HuespedService.class.getDeclaredConstructors()[0];
            List<Class<?>> parametros = Arrays.stream(constructor.getParameterTypes()).toList();

            assertThat(parametros)
                    .as("la dependencia de huespedes es la interfaz HuespedRepository")
                    .contains(HuespedRepository.class)
                    .doesNotContain(InMemoryHuespedRepository.class);
        }

        @Test
        @DisplayName("La interfaz no hereda de ninguna clase base de repositorios genericos")
        void interfaz_noHeredadeRepositorioGenerico() {
            assertThat(HuespedRepository.class.getInterfaces())
                    .as("ISP: el servicio no arrastra operaciones de otros modulos")
                    .isEmpty();
        }

        @Test
        @DisplayName("El contrato expone exactamente las siete operaciones con consumidor real")
        void contrato_noTieneMetodosEspeculativos() {
            List<String> metodos = Arrays.stream(HuespedRepository.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName)
                    .sorted()
                    .toList();

            assertThat(metodos).containsExactly(
                    "actualizar", "buscarPorDocumento", "buscarPorId", "buscarTodos",
                    "eliminarPorId", "existePorUsername", "guardar");
        }
    }


    @Nested
    @DisplayName("2. Consultas del repositorio")
    class Consultas {

        @Test
        @DisplayName("buscarPorId devuelve la misma instancia almacenada y un vacio si no existe")
        void buscarPorId() {
            Huesped carlos = dataStore.getHuespedes().get(0);

            Optional<Huesped> encontrado = huespedRepository.buscarPorId("H-001");

            assertThat(encontrado).isPresent()
                    .get()
                    .as("devuelve la misma instancia, no una copia")
                    .isSameAs(carlos);
            assertThat(huespedRepository.buscarPorId("H-999")).isEmpty();
        }

        @Test
        @DisplayName("buscarPorDocumento no distingue mayusculas de minusculas")
        void buscarPorDocumento() {
            Optional<Huesped> porMayusculas = huespedRepository.buscarPorDocumento("CC10000002");
            Optional<Huesped> porMinusculas = huespedRepository.buscarPorDocumento("cc10000002");

            assertThat(porMayusculas).get().isSameAs(dataStore.getHuespedes().get(1));
            assertThat(porMinusculas).isPresent();
            assertThat(huespedRepository.buscarPorDocumento("CC99999999")).isEmpty();
        }

        @Test
        @DisplayName("buscarPorDocumento tolera espacios en el dato ya almacenado")
        void buscarPorDocumento_toleraEspaciosAlmacenados() {
            Huesped conEspacios = huesped("H-ESPACIOS", "CC10000009", "espacios.repo");
            conEspacios.setDocumento("  CC10000009  ");
            huespedRepository.guardar(conEspacios);

            assertThat(huespedRepository.buscarPorDocumento("CC10000009")).get()
                    .as("ignora los espacios del documento guardado")
                    .isSameAs(conEspacios);
        }

        @Test
        @DisplayName("El servicio, y no el repositorio, es quien normaliza el documento recibido")
        void elServicio_normalizaElDocumento() {
            Huesped creado = huespedService.registrarHuesped(solicitud("  CC10000010  ", "normalizado.repo"));

            assertThat(creado.getDocumento()).as("se guarda ya normalizado").isEqualTo("CC10000010");
            assertThat(huespedRepository.buscarPorDocumento("CC10000010")).get().isSameAs(creado);
        }

        @Test
        @DisplayName("buscarTodos devuelve los huespedes almacenados")
        void buscarTodos() {
            List<Huesped> todos = huespedRepository.buscarTodos();

            assertThat(todos).hasSize(3);
            assertThat(todos).extracting(Huesped::getIdHuesped)
                    .containsExactly("H-001", "H-002", "H-003");
        }

        @Test
        @DisplayName("existePorUsername no distingue mayusculas de minusculas")
        void existePorUsername() {
            assertThat(huespedRepository.existePorUsername("carlos.r")).isTrue();
            assertThat(huespedRepository.existePorUsername("CARLOS.R")).isTrue();
            assertThat(huespedRepository.existePorUsername("nadie")).isFalse();
        }
    }



    @Nested
    @DisplayName("3. Escrituras del repositorio")
    class Escrituras {

        @Test
        @DisplayName("guardar persiste el huesped y lo devuelve")
        void guardar() {
            Huesped nuevo = huesped("H-NUEVO", "CC21000001", "nuevo.repo");

            Huesped devuelto = huespedRepository.guardar(nuevo);

            assertThat(devuelto).isSameAs(nuevo);
            assertThat(huespedRepository.buscarPorId("H-NUEVO")).get().isSameAs(nuevo);
            assertThat(huespedRepository.buscarTodos()).hasSize(4);
        }

        @Test
        @DisplayName("actualizar conserva la identidad y la posicion del huesped")
        void actualizar() {
            Huesped carlos = huespedRepository.buscarPorId("H-001").orElseThrow();
            int posicionOriginal = huespedRepository.buscarTodos().indexOf(carlos);

            carlos.setTelefono("3009999999");
            Huesped persistido = huespedRepository.actualizar(carlos);

            assertThat(persistido).as("se devuelve la misma instancia").isSameAs(carlos);
            assertThat(huespedRepository.buscarPorId("H-001")).get()
                    .isSameAs(carlos)
                    .extracting(Huesped::getTelefono).isEqualTo("3009999999");
            assertThat(huespedRepository.buscarTodos().indexOf(carlos))
                    .as("no se crea una fila nueva ni se pierde el orden")
                    .isEqualTo(posicionOriginal);
            assertThat(huespedRepository.buscarTodos()).hasSize(3);
        }

        @Test
        @DisplayName("actualizar un huesped que no existe falla de forma explicita")
        void actualizar_huespedInexistente() {
            Huesped fantasma = huesped("H-FANTASMA", "CC21000002", "fantasma");

            assertThatThrownBy(() -> huespedRepository.actualizar(fantasma))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("H-FANTASMA");
        }

        @Test
        @DisplayName("eliminarPorId informa si elimino y no falla si el huesped no existe")
        void eliminarPorId() {
            assertThat(huespedRepository.eliminarPorId("H-002")).isTrue();
            assertThat(huespedRepository.buscarPorId("H-002")).isEmpty();
            assertThat(huespedRepository.buscarTodos()).hasSize(2);

            assertThat(huespedRepository.eliminarPorId("H-002"))
                    .as("eliminar dos veces no es un error, simplemente no hay nada que eliminar")
                    .isFalse();
        }
    }



    @Nested
    @DisplayName("4. Consistencia con el DataStore")
    class Consistencia {

        @Test
        @DisplayName("El repositorio escribe en la misma lista que usa el resto del sistema")
        void repositorio_escribeEnLaListaCompartida() {
            Huesped nuevo = huesped("H-COMPARTIDO", "CC31000001", "compartido.repo");

            huespedRepository.guardar(nuevo);

            assertThat(dataStore.getHuespedes())
                    .as("no existe una segunda lista de huespedes")
                    .contains(nuevo);
            assertThat(dataStore.getHuespedes()).hasSize(4);
        }

        @Test
        @DisplayName("Lo que escribe el servicio aparece de inmediato en el repositorio y en el DataStore")
        void servicioYRepositorio_venLoMismo() {
            Huesped creado = huespedService.registrarHuesped(solicitud("CC31000002", "via.servicio"));

            assertThat(huespedRepository.buscarPorId(creado.getIdHuesped())).get().isSameAs(creado);
            assertThat(huespedRepository.buscarPorDocumento("CC31000002")).get().isSameAs(creado);
            assertThat(dataStore.getHuespedes()).contains(creado);

            assertThat(huespedRepository.buscarTodos()).hasSameSizeAs(dataStore.getHuespedes());
        }

        @Test
        @DisplayName("Lo que escribe el DataStore se ve de inmediato en el repositorio")
        void dataStore_esVisibleEnElRepositorio() {
            Huesped insertadoDirectamente = huesped("H-DIRECTO", "CC31000003", "directo");
            dataStore.getHuespedes().add(insertadoDirectamente);

            assertThat(huespedRepository.buscarPorId("H-DIRECTO")).get().isSameAs(insertadoDirectamente);
        }

        @Test
        @DisplayName("El servicio sigue funcionando con una implementacion distinta del repositorio")
        void servicio_funcionaConOtroRepositorio() {

            HuespedRepository alterno = new RepositorioEnMapa();
            HuespedService servicio = new HuespedService(alterno, usuarioRepository);

            Huesped creado = servicio.registrarHuesped(solicitud("CC41000001", "otro.repo"));

            assertThat(alterno.buscarPorId(creado.getIdHuesped())).get().isSameAs(creado);
            assertThat(dataStore.getHuespedes())
                    .as("el servicio no toca el almacenamiento concreto")
                    .noneMatch(h -> "CC41000001".equals(h.getDocumento()));


            assertThatThrownBy(() -> servicio.registrarHuesped(solicitud("CC41000001", "otro.repo")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya está registrado");
        }
    }



    @Nested
    @DisplayName("5. Unicidad del documento y concurrencia")
    class Unicidad {

        @Test
        @DisplayName("El servicio sigue rechazando un documento ya registrado")
        void documento_unicoEnRegistro() {
            huespedService.registrarHuesped(solicitud("CC51000001", "primero.repo"));

            assertThatThrownBy(() -> huespedService.registrarHuesped(solicitud("CC51000001", "segundo.repo")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya está registrado");

            assertThat(huespedRepository.buscarTodos())
                    .filteredOn(h -> "CC51000001".equals(h.getDocumento()))
                    .hasSize(1);
        }

        @Test
        @DisplayName("El servicio sigue rechazando asignar el documento de otro huesped")
        void documento_unicoEnActualizacion() {
            assertThatThrownBy(() -> huespedService.actualizarDatos(
                    "H-001", null, null, "CC10000002", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya está registrado");

            assertThat(huespedService.actualizarDatos("H-003", null, null, "CC10000003", null, null)
                    .getDocumento())
                    .as("un huesped puede volver a guardar su propio documento")
                    .isEqualTo("CC10000003");
        }

        @Test
        @DisplayName("Veinte registros simultaneos con el mismo documento solo dejan un huesped")
        void registrosSimultaneos_noProducenDocumentosDuplicados() throws Exception {
            int peticiones = 20;
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
                        huespedService.registrarHuesped(solicitud("CC61000001", "concurrente." + n));
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
                t.get(30, TimeUnit.SECONDS);
            }
            executor.shutdownNow();

            assertThat(creados.get()).as("solo uno gana la carrera").isEqualTo(1);
            assertThat(rechazados.get()).isEqualTo(peticiones - 1);
            assertThat(huespedRepository.buscarTodos())
                    .filteredOn(h -> "CC61000001".equals(h.getDocumento()))
                    .as("la comprobacion y la escritura separadas siguen siendo atomicas")
                    .hasSize(1);
        }

        @Test
        @DisplayName("Lecturas y escrituras simultaneas no producen documentos duplicados")
        void lecturasYEscriturasSimultaneas() throws Exception {
            int peticiones = 16;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            List<Future<?>> tareas = new ArrayList<>();


            for (int i = 0; i < peticiones; i++) {
                final int n = i;
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        if (n % 2 == 0) {
                            huespedRepository.guardar(huesped("H-T" + n, "CC7100000" + n, "lectura." + n));
                        } else {
                            huespedService.registrarHuesped(solicitud("CC7200000" + n, "escritura." + n));
                        }
                        huespedRepository.buscarTodos().forEach(h -> assertThat(h).isNotNull());
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

            assertThat(huespedRepository.buscarTodos())
                    .extracting(Huesped::getDocumento)
                    .doesNotHaveDuplicates();
        }
    }



    @Nested
    @DisplayName("6. Almacenamiento en memoria")
    class AlmacenamientoEnMemoria {

        @Test
        @DisplayName("Los datos sobreviven durante la ejecucion del programa")
        void datos_disponiblesDuranteLaEjecucion() {
            huespedService.registrarHuesped(solicitud("CC81000001", "durante.ejecucion"));

            assertThat(huespedRepository.buscarPorDocumento("CC81000001")).isPresent();
            assertThat(huespedRepository.buscarPorId(
                    huespedRepository.buscarPorDocumento("CC81000001").orElseThrow().getIdHuesped()))
                    .isPresent();
        }

        @Test
        @DisplayName("No hay almacenamiento permanente: al reiniciar el almacen no queda nada")
        void sinAlmacenamientoPermanente() {
            huespedService.registrarHuesped(solicitud("CC81000002", "volatil.repo"));
            assertThat(huespedRepository.buscarPorDocumento("CC81000002")).isPresent();


            dataStore.getHuespedes().clear();

            assertThat(huespedRepository.buscarTodos())
                    .as("no hay disco, archivo ni base de datos: todo se pierde al reiniciar")
                    .isEmpty();
            assertThat(huespedRepository.buscarPorDocumento("CC81000002")).isEmpty();
        }

        @Test
        @DisplayName("No hay JPA, Hibernate ni driver de base de datos en el proyecto")
        void sinTecnologiaDePersistencia() {
            for (String clase : List.of("jakarta.persistence.EntityManager",
                    "org.hibernate.Session",
                    "org.springframework.orm.jpa.JpaRepository",
                    "org.postgresql.Driver",
                    "com.mysql.cj.jdbc.Driver")) {
                assertThatThrownBy(() -> Class.forName(clase))
                        .as(clase + " no debe estar disponible")
                        .isInstanceOf(ClassNotFoundException.class);
            }
        }
    }



    @Nested
    @DisplayName("7. Asociaciones con reservas y estadias")
    class Asociaciones {

        @Test
        @DisplayName("Las reservas siguen apuntando a la misma instancia que guarda el repositorio")
        void reservas_conservanLaMismaInstancia() {
            Huesped carlos = huespedRepository.buscarPorId("H-001").orElseThrow();

            Reserva reserva = reservaService.crearReserva(solicitudReserva("H-001", 101));

            assertThat(reserva.getHuesped())
                    .as("no se crea una copia desincronizada del huesped")
                    .isSameAs(carlos);
        }

        @Test
        @DisplayName("Un cambio guardado por el repositorio se ve en la reserva que lo referencia")
        void cambiosDelRepositorio_seVenEnLasReservas() {
            Reserva reserva = reservaService.crearReserva(solicitudReserva("H-001", 101));

            huespedRepository.buscarPorId("H-001").orElseThrow().setTelefono("3007777777");
            huespedRepository.actualizar(huespedRepository.buscarPorId("H-001").orElseThrow());

            assertThat(reserva.getHuesped().getTelefono())
                    .as("la reserva ve el cambio porque comparte la instancia")
                    .isEqualTo("3007777777");
        }

        @Test
        @DisplayName("Las reservas siguen asociandose al huesped correcto tras migrar al repositorio")
        void reservas_asociadasAlHuespedCorrecto() {
            Reserva deCarlos = reservaService.crearReserva(solicitudReserva("H-001", 101));
            Reserva deLaura = reservaService.crearReserva(solicitudReserva("H-002", 102));

            assertThat(deCarlos.getHuesped().getDocumento()).isEqualTo("CC10000001");
            assertThat(deLaura.getHuesped().getDocumento()).isEqualTo("CC10000002");
            assertThat(reservaService.listarPorHuesped("H-001")).hasSize(1);
            assertThat(reservaService.listarPorHuesped("H-002")).hasSize(1);
        }

        @Test
        @DisplayName("El check-in sigue verificando el documento persistido en el repositorio")
        void checkIn_usaElDocumentoDelRepositorio() {
            Reserva reserva = reservaService.crearReserva(solicitudReserva("H-002", 102));

            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            Huesped enEstadia = estadia.getReserva().getHuesped();
            Huesped enRepositorio = huespedRepository.buscarPorId("H-002").orElseThrow();

            assertThat(enEstadia).isSameAs(enRepositorio);
            assertThat(enEstadia.getDocumento()).isEqualTo("CC10000002");
        }

        @Test
        @DisplayName("Un huesped creado por el repositorio puede reservarse y hacer check-in")
        void huespedNuevo_puedeUsarseEnElFlujoCompleto() {
            Huesped nuevo = huesped("H-NUEVO-FLUJO", "CC91000001", "nuevo.flujo");
            huespedRepository.guardar(nuevo);

            Reserva reserva = reservaService.crearReserva(solicitudReserva("H-NUEVO-FLUJO", 103));
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThat(estadia.getReserva().getHuesped().getDocumento()).isEqualTo("CC91000001");
            assertThat(huespedRepository.buscarPorId("H-NUEVO-FLUJO")).get()
                    .isSameAs(estadia.getReserva().getHuesped());
        }
    }



    @Nested
    @DisplayName("8. Compatibilidad HTTP")
    class CompatibilidadHttp {

        @Test
        @DisplayName("GET /api/huespedes sigue devolviendo los huespedes del repositorio")
        void listarPorHttp() throws Exception {
            MvcResult resultado = mockMvc.perform(get("/api/huespedes")).andReturn();
            assertThat(resultado.getResponse().getStatus()).isEqualTo(200);

            String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(cuerpo)
                    .contains("\"idHuesped\":\"H-001\"")
                    .contains("\"documento\":\"CC10000001\"")
                    .contains("\"rol\":\"HUESPED\"")
                    .doesNotContain("\"password\"");
        }

        @Test
        @DisplayName("POST /api/huespedes sigue registrando y persiste en el repositorio")
        void registrarPorHttp() throws Exception {
            String cuerpo = "{\"nombre\":\"Http\",\"apellido\":\"Repositorio\",\"documento\":\"CC92000001\","
                    + "\"telefono\":\"3001111111\",\"correo\":\"http@email.com\","
                    + "\"username\":\"http.repo\",\"password\":\"clave123\"}";

            MvcResult resultado = mockMvc.perform(post("/api/huespedes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Role", "RECEPCIONISTA")
                    .header("X-User-Id", RECEP_ID)
                    .header("X-Username", RECEP_USER)
                    .content(cuerpo)).andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
            assertThat(huespedRepository.buscarPorDocumento("CC92000001"))
                    .as("lo que entra por HTTP queda en el repositorio")
                    .isPresent();
        }
    }



    private Huesped huesped(String id, String documento, String username) {
        return new Huesped(id, "Nombre", "Apellido", documento,
                "3000000000", "correo@email.com", username, "pass123");
    }

    private HuespedRequestDTO solicitud(String documento, String username) {
        HuespedRequestDTO dto = new HuespedRequestDTO();
        dto.setNombre("Ana");
        dto.setApellido("Restrepo");
        dto.setDocumento(documento);
        dto.setTelefono("3001112222");
        dto.setCorreo(username + "@email.com");
        dto.setUsername(username);
        dto.setPassword("pass123");
        return dto;
    }

    private ReservaRequestDTO solicitudReserva(String idHuesped, int numeroHabitacion) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(idHuesped);
        dto.setNumeroHabitacion(numeroHabitacion);
        dto.setFechaInicio(HOY);
        dto.setFechaFin(HOY.plusDays(2));
        dto.setAdultos(2);
        dto.setNinos(0);
        return dto;
    }

    /**
     * Implementación alternativa del contrato, escrita solo para la prueba. Sirve para demostrar
     * que {@link HuespedService} depende de la abstracción y no de una implementación concreta:
     * funciona con esta sin haber oído hablar de {@code DataStore}.
     */
    private static class RepositorioEnMapa implements HuespedRepository {

        private final Map<String, Huesped> memoria = new LinkedHashMap<>();

        @Override
        public Huesped guardar(Huesped huesped) {
            memoria.put(huesped.getIdHuesped(), huesped);
            return huesped;
        }

        @Override
        public Optional<Huesped> buscarPorId(String idHuesped) {
            return Optional.ofNullable(memoria.get(idHuesped));
        }

        @Override
        public Optional<Huesped> buscarPorDocumento(String documento) {
            return memoria.values().stream()
                    .filter(h -> h.getDocumento() != null && h.getDocumento().trim().equalsIgnoreCase(documento))
                    .findFirst();
        }

        @Override
        public List<Huesped> buscarTodos() {
            return new CopyOnWriteArrayList<>(memoria.values());
        }

        @Override
        public boolean existePorUsername(String username) {
            return memoria.values().stream().anyMatch(h -> h.getUsername().equalsIgnoreCase(username));
        }

        @Override
        public Huesped actualizar(Huesped huesped) {
            if (!memoria.containsKey(huesped.getIdHuesped())) {
                throw new IllegalArgumentException("No existe el huésped con ID: " + huesped.getIdHuesped());
            }
            memoria.put(huesped.getIdHuesped(), huesped);
            return huesped;
        }

        @Override
        public boolean eliminarPorId(String idHuesped) {
            return memoria.remove(idHuesped) != null;
        }
    }
}