package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.HuespedRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.repository.EstadiaRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.InMemoryHabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.ReservaRepository;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
import co.edu.uniquindio.sistemahotel.backend.service.HuespedService;
import co.edu.uniquindio.sistemahotel.backend.service.PagoService;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
@AutoConfigureMockMvc
class HabitacionRepositoryTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final String ID_HUESPED = "H-001";
    private static final int HABITACION = 101;
    private static final int OTRA_HABITACION = 102;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private HuespedRepository huespedRepository;

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private HuespedService huespedService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EstadiaRepository estadiaRepository;

    @Autowired
    private PagoService pagoService;

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



    @Nested
    @DisplayName("1. Contrato HabitacionRepository")
    class Contrato {

        @Test
        @DisplayName("El servicio declara una dependencia de la abstraccion, no de la implementacion")
        void servicio_dependeDeLaAbstraccion() {
            var constructor = HabitacionService.class.getDeclaredConstructors()[0];
            List<Class<?>> parametros = Arrays.stream(constructor.getParameterTypes()).toList();

            assertThat(parametros)
                    .contains(HabitacionRepository.class)
                    .doesNotContain(InMemoryHabitacionRepository.class);
        }

        @Test
        @DisplayName("La interfaz no hereda de ninguna clase base de repositorios genericos")
        void interfaz_noHeredadeRepositorioGenerico() {
            assertThat(HabitacionRepository.class.getInterfaces()).isEmpty();
        }

        @Test
        @DisplayName("El contrato expone exactamente las cinco operaciones con consumidor real")
        void contrato_noTieneMetodosEspeculativos() {
            List<String> metodos = Arrays.stream(HabitacionRepository.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName)
                    .sorted()
                    .toList();

            assertThat(metodos).containsExactly(
                    "actualizar", "buscarPorEstado", "buscarPorNumero", "buscarPorTipo", "buscarTodos");
        }

        @Test
        @DisplayName("No declara guardar ni eliminar: el hotel no crea ni borra habitaciones en ejecucion")
        void contrato_noEsUnCrudSimetrico() {
            List<String> metodos = Arrays.stream(HabitacionRepository.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName)
                    .toList();

            assertThat(metodos)
                    .as("un CRUD generico obligaria al cliente a depender de operaciones que no usa")
                    .doesNotContain("guardar", "save", "eliminar", "delete", "eliminarPorId");
        }
    }



    @Nested
    @DisplayName("2. Consultas por identificador y listado")
    class Consultas {

        @Test
        @DisplayName("buscarPorNumero devuelve la misma instancia y un vacio si no existe")
        void buscarPorNumero() {
            Habitacion almacenada = dataStore.getHabitaciones().get(0);

            Optional<Habitacion> encontrada = habitacionRepository.buscarPorNumero(HABITACION);

            assertThat(encontrada).get().isSameAs(almacenada);
            assertThat(habitacionRepository.buscarPorNumero(999)).isEmpty();
        }

        @Test
        @DisplayName("buscarTodos devuelve el inventario completo")
        void buscarTodos() {
            assertThat(habitacionRepository.buscarTodos()).hasSizeGreaterThanOrEqualTo(8);
            assertThat(habitacionRepository.buscarTodos())
                    .extracting(Habitacion::getNumeroHabitacion)
                    .contains(HABITACION, OTRA_HABITACION, 201, 202, 301, 302, 401);
        }

        @Test
        @DisplayName("buscarPorTipo y buscarPorEstado filtran por atributos de la propia habitacion")
        void buscarPorTipoYEstado() {
            assertThat(habitacionRepository.buscarPorTipo(TipoHabitacion.SUITE))
                    .extracting(Habitacion::getNumeroHabitacion).containsExactly(401);
            assertThat(habitacionRepository.buscarPorTipo(TipoHabitacion.DELUXE))
                    .extracting(Habitacion::getNumeroHabitacion).containsExactlyInAnyOrder(301, 302);
            assertThat(habitacionRepository.buscarPorEstado(EstadoHabitacion.DISPONIBLE))
                    .hasSize(habitacionRepository.buscarTodos().size());

            habitacionRepository.buscarPorNumero(HABITACION).orElseThrow()
                    .cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

            assertThat(habitacionRepository.buscarPorEstado(EstadoHabitacion.EN_LIMPIEZA))
                    .extracting(Habitacion::getNumeroHabitacion).containsExactly(HABITACION);
            assertThat(habitacionRepository.buscarPorEstado(EstadoHabitacion.MANTENIMIENTO)).isEmpty();
        }

        @Test
        @DisplayName("El servicio expone las mismas consultas a traves del repositorio")
        void servicio_delegaLasConsultas() {
            assertThat(habitacionService.listarTodas()).isSameAs(habitacionRepository.buscarTodos());
            assertThat(habitacionService.buscarPorNumero(HABITACION))
                    .get().isSameAs(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow());
            assertThat(habitacionService.listarPorTipo(TipoHabitacion.SUITE))
                    .extracting(Habitacion::getNumeroHabitacion).containsExactly(401);
            assertThat(habitacionService.obtenerOFallar(HABITACION).getNumeroHabitacion())
                    .isEqualTo(HABITACION);
        }
    }


    @Nested
    @DisplayName("3. Escrituras, DataStore compartido e identidad")
    class Escrituras {

        @Test
        @DisplayName("actualizar conserva la identidad y la posicion dentro del DataStore")
        void actualizar_conservaIdentidadYPosicion() {
            Habitacion habitacion = habitacionRepository.buscarPorNumero(HABITACION).orElseThrow();
            int posicionOriginal = dataStore.getHabitaciones().indexOf(habitacion);

            habitacion.cambiarEstado(EstadoHabitacion.MANTENIMIENTO);
            Habitacion persistida = habitacionRepository.actualizar(habitacion);

            assertThat(persistida).as("devuelve la misma instancia").isSameAs(habitacion);
            assertThat(dataStore.getHabitaciones()).hasSameSizeAs(habitacionRepository.buscarTodos());
            assertThat(dataStore.getHabitaciones().indexOf(persistida))
                    .as("no se crea una fila nueva ni se pierde el orden")
                    .isEqualTo(posicionOriginal);
            assertThat(dataStore.getHabitaciones()).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("El cambio de estado del servicio es visible de inmediato en el DataStore")
        void cambioDeEstado_visibleEnElDataStore() {
            habitacionService.marcarMantenimiento(HABITACION);

            assertThat(dataStore.getHabitaciones())
                    .filteredOn(h -> h.getNumeroHabitacion() == HABITACION)
                    .singleElement()
                    .extracting(Habitacion::getEstadoHabitacion)
                    .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("actualizar una habitacion que no existe falla de forma explicita")
        void actualizar_habitacionInexistente() {
            Habitacion fantasma = new Habitacion(999, TipoHabitacion.SUITE, 1, 1, "Fantasma", 2, null);

            assertThatThrownBy(() -> habitacionRepository.actualizar(fantasma))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Las reservas y las estadias siguen apuntando a la misma instancia de habitacion")
        void identidadCompartidaConReservas() {
            Reserva reserva = reservaService.crearReserva(dtoReserva(ID_HUESPED, HABITACION, HOY, HOY.plusDays(3)));

            Habitacion enRepositorio = habitacionRepository.buscarPorNumero(HABITACION).orElseThrow();

            assertThat(reserva.getHabitacion()).as("misma instancia, no una copia").isSameAs(enRepositorio);

            habitacionService.marcarMantenimiento(HABITACION);

            assertThat(reserva.getHabitacion().getEstadoHabitacion())
                    .as("el cambio se ve en la reserva sin resincronizar nada")
                    .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }
    }


    @Nested
    @DisplayName("4. Dependencia invertida en la practica")
    class DependenciaInvertida {

        @Test
        @DisplayName("El servicio funciona con un repositorio distinto del DataStore")
        void servicio_conRepositorioAlternativo() {
            RepositorioDeHabitacionesEnMemoria alterno = new RepositorioDeHabitacionesEnMemoria();
            alterno.cargar(new Habitacion(301, TipoHabitacion.DELUXE, 350_000, 3, "Solo en el alterno", 3, null));

            HabitacionService servicio =
                    new HabitacionService(alterno, reservaRepository, estadiaRepository);

            assertThat(servicio.listarTodas())
                    .as("lee del repositorio alterno, no del DataStore")
                    .hasSize(1);
            assertThat(servicio.obtenerOFallar(301).getDescripcion()).isEqualTo("Solo en el alterno");
            assertThatThrownBy(() -> servicio.obtenerOFallar(HABITACION))
                    .as("la habitacion 101 existe en el DataStore pero no en el repositorio alterno")
                    .isInstanceOf(IllegalArgumentException.class);

            servicio.marcarMantenimiento(301);

            assertThat(alterno.buscarPorNumero(301).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
            assertThat(dataStore.getHabitaciones())
                    .filteredOn(h -> h.getNumeroHabitacion() == 301)
                    .singleElement()
                    .as("el DataStore queda intacto: prueba de que no se accedio a el")
                    .extracting(Habitacion::getEstadoHabitacion)
                    .isNotEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("Las reglas de negocio siguen aplicando con el repositorio alterno")
        void reglasDeNegocio_conRepositorioAlterno() {
            RepositorioDeHabitacionesEnMemoria alterno = new RepositorioDeHabitacionesEnMemoria();
            Habitacion ocupada = new Habitacion(401, TipoHabitacion.SUITE, 600_000, 4, "Suite", 4, null);
            alterno.cargar(ocupada);
            HabitacionService servicio =
                    new HabitacionService(alterno, reservaRepository, estadiaRepository);

            servicio.marcarMantenimiento(401);
            assertThat(servicio.listarDisponibles(HOY, HOY.plusDays(5)))
                    .as("el filtro de estados sigue en el servicio, no en el repositorio")
                    .isEmpty();

            Reserva reserva = reservaService.crearReserva(dtoReserva(ID_HUESPED, HABITACION, HOY, HOY.plusDays(3)));
            assertThat(servicio.tieneReservaSolapada(HABITACION, HOY.plusDays(1), HOY.plusDays(2), null))
                    .as("la regla de solapamiento sigue usando el DataStore de reservas")
                    .isTrue();
            assertThat(servicio.tieneReservaSolapada(OTRA_HABITACION, HOY, HOY.plusDays(1), null))
                    .isFalse();
            assertThat(reserva.getIdReserva()).startsWith("RES-");
        }
    }


    @Nested
    @DisplayName("5. Disponibilidad por fechas y solapamiento")
    class Disponibilidad {

        @Test
        @DisplayName("Una habitacion con reserva futura no aparece como disponible en esas fechas")
        void reservaFutura_bloqueaEsasFechas() {
            LocalDate entrada = HOY.plusDays(10);
            reservaService.crearReserva(dtoReserva(ID_HUESPED, HABITACION, entrada, entrada.plusDays(3)));

            List<Integer> enEsasFechas = habitacionService.listarDisponibles(entrada, entrada.plusDays(1))
                    .stream().map(Habitacion::getNumeroHabitacion).toList();
            List<Integer> enOtrasFechas = habitacionService.listarDisponibles(HOY, HOY.plusDays(1))
                    .stream().map(Habitacion::getNumeroHabitacion).toList();

            assertThat(enEsasFechas).as("ocupada en el rango reservado").doesNotContain(HABITACION);
            assertThat(enOtrasFechas).as("libre fuera del rango reservado").contains(HABITACION);
        }

        @Test
        @DisplayName("Se excluye la propia reserva al consultar disponibilidad para modificarla")
        void excluirPropiaReserva() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva(ID_HUESPED, HABITACION, HOY.plusDays(5), HOY.plusDays(8)));

            List<Integer> sinExcluir = habitacionService.listarDisponibles(HOY.plusDays(5), HOY.plusDays(8), null)
                    .stream().map(Habitacion::getNumeroHabitacion).toList();
            List<Integer> excluyendoPropia = habitacionService.listarDisponibles(
                            HOY.plusDays(5), HOY.plusDays(8), reserva.getIdReserva())
                    .stream().map(Habitacion::getNumeroHabitacion).toList();

            assertThat(sinExcluir)
                    .as("sin excluir, la habitacion aparece ocupada por si misma")
                    .doesNotContain(HABITACION);
            assertThat(excluyendoPropia)
                    .as("excluyendo su propia reserva, vuelve a estar disponible")
                    .contains(HABITACION);
        }

        @Test
        @DisplayName("Las reservas solapadas se rechazan y las contiguas se aceptan")
        void solapamiento() {
            reservaService.crearReserva(dtoReserva(ID_HUESPED, HABITACION, HOY.plusDays(5), HOY.plusDays(8)));

            assertThatThrownBy(() -> reservaService.crearReserva(
                    dtoReserva("H-002", HABITACION, HOY.plusDays(7), HOY.plusDays(10))))
                    .as("el solapamiento se rechaza con IllegalStateException")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("reserva activa en las fechas");

            assertThat(reservaService.crearReserva(
                    dtoReserva("H-002", HABITACION, HOY.plusDays(8), HOY.plusDays(10))))
                    .as("una reserva que empieza al terminar la anterior no se solapa")
                    .isNotNull();
        }

        @Test
        @DisplayName("MANTENIMIENTO, FUERA_DE_SERVICIO y EN_LIMPIEZA quedan fuera de la disponibilidad")
        void estadosBloqueantes() {
            habitacionService.marcarMantenimiento(HABITACION);
            habitacionService.marcarFueraDeServicio(OTRA_HABITACION);
            Habitacion enLimpieza = habitacionService.obtenerOFallar(103);
            enLimpieza.cambiarEstado(EstadoHabitacion.EN_LIMPIEZA);

            List<Integer> disponibles = habitacionService.listarDisponibles(HOY.plusDays(30), HOY.plusDays(32))
                    .stream().map(Habitacion::getNumeroHabitacion).toList();

            assertThat(disponibles)
                    .doesNotContain(HABITACION, OTRA_HABITACION, 103);

            assertThat(habitacionService.estaHabilitadaParaReservar(EstadoHabitacion.MANTENIMIENTO)).isFalse();
            assertThat(habitacionService.estaHabilitadaParaReservar(EstadoHabitacion.FUERA_DE_SERVICIO)).isFalse();
            assertThat(habitacionService.estaHabilitadaParaReservar(EstadoHabitacion.EN_LIMPIEZA)).isFalse();
            assertThat(habitacionService.estaHabilitadaParaReservar(EstadoHabitacion.DISPONIBLE)).isTrue();
            assertThat(habitacionService.estaHabilitadaParaReservar(EstadoHabitacion.OCUPADA)).isTrue();
        }

        @Test
        @DisplayName("Los rangos de fechas invalidos se siguen rechazando")
        void rangoInvalido() {
            assertThatThrownBy(() -> habitacionService.listarDisponibles(HOY.plusDays(5), HOY.plusDays(5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("posterior");
            assertThatThrownBy(() -> habitacionService.listarDisponibles(null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }



    @Nested
    @DisplayName("6. Check-in, check-out y limpieza")
    class CicloDeEstadia {

        @Test
        @DisplayName("El check-in deja la habitacion OCUPADA y visible en el repositorio")
        void checkIn_ocupa() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva(ID_HUESPED, HABITACION, HOY, HOY.plusDays(3)));

            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(dataStore.getHabitaciones())
                    .filteredOn(h -> h.getNumeroHabitacion() == HABITACION)
                    .singleElement()
                    .extracting(Habitacion::getEstadoHabitacion)
                    .isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("El check-in exige que la habitacion este habilitada")
        void checkIn_habitacionNoHabilitada() {

            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-002", HABITACION, HOY, HOY.plusDays(3)));
            habitacionService.marcarMantenimiento(HABITACION);

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MANTENIMIENTO");

            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .as("no se toca el estado si el check-in falla")
                    .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
            assertThat(dataStore.getEstadias()).isEmpty();
        }

        @Test
        @DisplayName("El check-out deja la habitacion EN_LIMPIEZA y no la libera sola")
        void checkOut_dejaEnLimpieza() {
            Estadia estadia = estadiaSaldada();

            estadiaService.realizarCheckOut(estadia.getIdEstadia());

            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .as("la liberacion exige confirmacion explicita de la recepcion")
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertThat(habitacionService.listarPendientesLimpieza())
                    .extracting(Habitacion::getNumeroHabitacion).contains(HABITACION);
        }

        @Test
        @DisplayName("Finalizar la limpieza deja la habitacion DISPONIBLE y sale de pendientes")
        void finalizarLimpieza_libera() {
            Estadia estadia = estadiaSaldada();
            estadiaService.realizarCheckOut(estadia.getIdEstadia());

            Habitacion liberada = habitacionService.finalizarLimpieza(HABITACION);

            assertThat(liberada.getEstadoHabitacion()).isEqualTo(EstadoHabitacion.DISPONIBLE);
            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
            assertThat(habitacionService.listarPendientesLimpieza())
                    .extracting(Habitacion::getNumeroHabitacion).doesNotContain(HABITACION);
            assertThat(habitacionService.listarDisponibles(HOY.plusDays(1), HOY.plusDays(2)))
                    .extracting(Habitacion::getNumeroHabitacion).contains(HABITACION);
        }
    }


    @Nested
    @DisplayName("7. Transiciones invalidas")
    class TransicionesInvalidas {

        @Test
        @DisplayName("No se puede liberar a DISPONIBLE por la via generica de cambio de estado")
        void liberarPorCambiarEstado() {
            habitacionService.marcarEnLimpieza(HABITACION);

            assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION, EstadoHabitacion.DISPONIBLE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminó la limpieza");

            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
        }

        @Test
        @DisplayName("No se puede simular la limpieza de una habitacion ocupada con estadia activa")
        void noSimularLimpiezaDeOcupada() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva(ID_HUESPED, HABITACION, HOY, HOY.plusDays(3)));
            estadiaService.realizarCheckIn(reserva.getIdReserva());

            assertThatThrownBy(() -> habitacionService.marcarEnLimpieza(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("estadía activa");

            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .as("sigue OCUPADA: el error no dejo un estado parcial")
                    .isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("No se puede finalizar la limpieza dos veces ni desde un estado invalido")
        void finalizarLimpieza_invalidas() {
            habitacionService.marcarEnLimpieza(HABITACION);
            habitacionService.finalizarLimpieza(HABITACION);

            assertThatThrownBy(() -> habitacionService.finalizarLimpieza(HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está EN_LIMPIEZA");

            habitacionService.marcarMantenimiento(OTRA_HABITACION);
            assertThatThrownBy(() -> habitacionService.finalizarLimpieza(OTRA_HABITACION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está EN_LIMPIEZA");
        }

        @Test
        @DisplayName("No se puede marcar en limpieza una habitacion bloqueada")
        void marcarEnLimpieza_desdeBloqueada() {
            habitacionService.marcarMantenimiento(HABITACION);

            assertThatThrownBy(() -> habitacionService.marcarEnLimpieza(HABITACION))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("Una habitacion inexistente falla con el mensaje de siempre")
        void habitacionInexistente() {
            assertThatThrownBy(() -> habitacionService.obtenerOFallar(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe la habitación número: 999");
            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(999))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obligatorio");
        }
    }



    @Nested
    @DisplayName("8. Ausencia de cambios parciales ante errores")
    class SinCambiosParciales {

        @Test
        @DisplayName("Una transicion rechazada no modifica ninguna habitacion del inventario")
        void transicionRechazada_noDejaEstadosParciales() {
            habitacionService.marcarEnLimpieza(HABITACION);
            habitacionService.marcarFueraDeServicio(OTRA_HABITACION);
            List<EstadoHabitacion> antes = inventarioComoLista();

            assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION, EstadoHabitacion.DISPONIBLE))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> habitacionService.marcarEnLimpieza(OTRA_HABITACION))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> habitacionService.finalizarLimpieza(103))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está EN_LIMPIEZA");
            assertThatThrownBy(() -> habitacionService.marcarFueraDeServicio(999))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(inventarioComoLista())
                    .as("el inventario queda exactamente igual tras cuatro errores")
                    .isEqualTo(antes);
        }

        @Test
        @DisplayName("Un check-in fallido no ocupa la habitacion")
        void checkInFallido_noOcupa() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-002", HABITACION, HOY, HOY.plusDays(3)));
            habitacionService.marcarFueraDeServicio(HABITACION);

            assertThatThrownBy(() -> estadiaService.realizarCheckIn(reserva.getIdReserva()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FUERA_DE_SERVICIO");

            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
            assertThat(dataStore.getEstadias()).as("no se creo ninguna estadia").isEmpty();
        }

        @Test
        @DisplayName("Actualizar no inserta ni elimina filas del inventario")
        void actualizar_noCambiaElTamano() {
            int antes = dataStore.getHabitaciones().size();

            habitacionRepository.actualizar(
                    habitacionRepository.buscarPorNumero(HABITACION).orElseThrow());

            assertThat(dataStore.getHabitaciones()).hasSize(antes);
        }
    }



    @Nested
    @DisplayName("9. Concurrencia")
    class Concurrencia {

        @Test
        @DisplayName("Finalizar la limpieza simultaneamente libera la habitacion una sola vez")
        void finalizarLimpiezaConcurrente_liberaUnaSolaVez() throws Exception {
            habitacionService.marcarEnLimpieza(HABITACION);

            int peticiones = 12;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            AtomicInteger exitosos = new AtomicInteger();
            AtomicInteger rechazados = new AtomicInteger();
            List<Future<?>> tareas = new ArrayList<>();

            for (int i = 0; i < peticiones; i++) {
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        habitacionService.finalizarLimpieza(HABITACION);
                        exitosos.incrementAndGet();
                    } catch (IllegalStateException | IllegalArgumentException e) {
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

            assertThat(exitosos.get()).as("el monitor de finalizarLimpieza se mantiene").isEqualTo(1);
            assertThat(rechazados.get()).isEqualTo(peticiones - 1);
            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("Reservas simultaneas sobre la misma habitacion y fechas dejan una sola reserva")
        void reservasSimultaneas_soloDejaUna() throws Exception {
            int peticiones = 12;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            AtomicInteger creadas = new AtomicInteger();
            AtomicInteger rechazadas = new AtomicInteger();
            List<Future<?>> tareas = new ArrayList<>();

            for (int i = 0; i < peticiones; i++) {
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        reservaService.crearReserva(
                                dtoReserva(ID_HUESPED, HABITACION, HOY.plusDays(20), HOY.plusDays(23)));
                        creadas.incrementAndGet();
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        rechazadas.incrementAndGet();
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

            assertThat(creadas.get()).isEqualTo(1);
            assertThat(rechazadas.get()).isEqualTo(peticiones - 1);
            assertThat(habitacionService.tieneReservaSolapada(HABITACION, HOY.plusDays(21), HOY.plusDays(22), null))
                    .isTrue();
        }

        @Test
        @DisplayName("Operaciones mixtas de reservas y habitaciones no producen interbloqueo")
        void sinInterbloqueo() throws Exception {
            int peticiones = 8;
            ExecutorService executor = Executors.newFixedThreadPool(peticiones);
            CountDownLatch arranque = new CountDownLatch(1);
            List<Future<?>> tareas = new ArrayList<>();
            List<String> fallos = new CopyOnWriteArrayList<>();


            int[] paraReservar = {101, 102, 103, 201};
            int[] paraBloquear = {202, 301, 302, 401};


            for (int i = 0; i < peticiones; i++) {
                final int n = i;
                tareas.add(executor.submit(() -> {
                    try {
                        arranque.await();
                        if (n % 2 == 0) {
                            reservaService.crearReserva(dtoReserva(
                                    ID_HUESPED, paraReservar[n / 2],
                                    HOY.plusDays(40), HOY.plusDays(43)));
                        } else {
                            int numero = paraBloquear[(n - 1) / 2];
                            habitacionService.marcarFueraDeServicio(numero);
                            habitacionService.buscarPorNumero(numero);
                        }
                    } catch (RuntimeException e) {
                        fallos.add(n + ": " + e.getMessage());
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

            assertThat(fallos)
                    .as("ninguna tarea quedo bloqueada ni lanzo por una regla de negocio")
                    .isEmpty();
            assertThat(dataStore.getReservas())
                    .as("las cuatro reservas se crearon")
                    .hasSize(4);
        }

        @Test
        @DisplayName("Lecturas del repositorio mientras se cambian estados no fallan")
        void lecturasConcurrentes() throws Exception {
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
                            habitacionRepository.buscarPorEstado(EstadoHabitacion.DISPONIBLE);
                            habitacionRepository.buscarPorTipo(TipoHabitacion.SUITE);
                        } else {
                            habitacionRepository.buscarPorNumero(301).orElseThrow()
                                    .cambiarEstado(EstadoHabitacion.DISPONIBLE);
                        }
                        // Recorrer el inventario vivo nunca debe lanzar ConcurrentModificationException.
                        assertThat(habitacionRepository.buscarTodos()).isNotNull();
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

            assertThat(habitacionRepository.buscarTodos()).hasSizeGreaterThanOrEqualTo(8);
        }
    }


    @Nested
    @DisplayName("10. Compatibilidad con HuespedRepository")
    class CompatibilidadEntreRepositorios {

        @Test
        @DisplayName("Un huesped registrado por su repositorio recorre reserva, check-in y limpieza")
        void flujoCompletoEntreLosDosRepositorios() {
            HuespedRequestDTO dto = new HuespedRequestDTO();
            dto.setNombre("Rita");
            dto.setApellido("Molina");
            dto.setDocumento("CC13000001");
            dto.setTelefono("3007777777");
            dto.setCorreo("rita@email.com");
            dto.setUsername("rita.molina");
            dto.setPassword("pass123");

            Huesped huesped = huespedService.registrarHuesped(dto);
            assertThat(huespedRepository.buscarPorId(huesped.getIdHuesped())).get().isSameAs(huesped);

            Reserva reserva = reservaService.crearReserva(
                    dtoReserva(huesped.getIdHuesped(), HABITACION, HOY, HOY.plusDays(2)));
            assertThat(reserva.getHuesped()).isSameAs(huesped);
            assertThat(reserva.getHabitacion())
                    .isSameAs(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow());

            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.OCUPADA);

            pagarYAprobar(estadia);

            estadiaService.realizarCheckOut(estadia.getIdEstadia());
            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

            habitacionService.finalizarLimpieza(HABITACION);
            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow().getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);

            assertThat(huespedRepository.buscarPorDocumento("CC13000001")).get()
                    .isSameAs(huesped)
                    .extracting(Huesped::getDocumento).isEqualTo("CC13000001");
        }

        @Test
        @DisplayName("Los dos repositorios leen y escriben sobre el mismo DataStore sin interferir")
        void losDosRepositorios_compartenElDataStore() {
            HuespedRequestDTO dto = new HuespedRequestDTO();
            dto.setNombre("Nico");
            dto.setApellido("Salas");
            dto.setDocumento("CC13000002");
            dto.setTelefono("3006666666");
            dto.setCorreo("nico@email.com");
            dto.setUsername("nico.salas");
            dto.setPassword("pass123");

            huespedService.registrarHuesped(dto);
            habitacionService.marcarMantenimiento(401);

            assertThat(dataStore.getHuespedes())
                    .filteredOn(h -> "CC13000002".equals(h.getDocumento())).hasSize(1);
            assertThat(dataStore.getHabitaciones())
                    .filteredOn(h -> h.getNumeroHabitacion() == 401)
                    .singleElement()
                    .extracting(Habitacion::getEstadoHabitacion)
                    .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }
    }



    private List<EstadoHabitacion> inventarioComoLista() {
        return habitacionRepository.buscarTodos().stream()
                .map(Habitacion::getEstadoHabitacion)
                .collect(Collectors.toList());
    }

    private ReservaRequestDTO dtoReserva(String idHuesped, int numero, LocalDate inicio, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(idHuesped);
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return dto;
    }

    /** Crea una estadía con la cuenta saldada, lista para el check-out. */
    private Estadia estadiaSaldada() {
        Reserva reserva = reservaService.crearReserva(
                dtoReserva(ID_HUESPED, HABITACION, HOY, HOY.plusDays(2)));
        Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
        pagarYAprobar(estadia);
        return estadia;
    }

    private void pagarYAprobar(Estadia estadia) {
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(estadia.getIdEstadia());
        dto.setMonto(estadiaService.calcularCosto(estadia.getIdEstadia()).getTotalGeneral().doubleValue());
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Abono de prueba");
        Pago pago = pagoService.registrarPago(dto);
        pagoService.aprobarPago(pago.getIdPago(), "REF-TEST");
    }

    /**
     * Un repositorio de habitaciones en memoria para probar la inversión de dependencias.
     */
    private static class RepositorioDeHabitacionesEnMemoria implements HabitacionRepository {

        private final List<Habitacion> inventario = new CopyOnWriteArrayList<>();

        void cargar(Habitacion habitacion) {
            inventario.add(habitacion);
        }

        @Override
        public List<Habitacion> buscarTodos() {
            return inventario;
        }

        @Override
        public Optional<Habitacion> buscarPorNumero(int numeroHabitacion) {
            return inventario.stream().filter(h -> h.getNumeroHabitacion() == numeroHabitacion).findFirst();
        }

        @Override
        public List<Habitacion> buscarPorTipo(TipoHabitacion tipo) {
            return inventario.stream().filter(h -> h.getTipoHabitacion() == tipo).collect(Collectors.toList());
        }

        @Override
        public List<Habitacion> buscarPorEstado(EstadoHabitacion estado) {
            return inventario.stream().filter(h -> h.getEstadoHabitacion() == estado).collect(Collectors.toList());
        }

        @Override
        public Habitacion actualizar(Habitacion habitacion) {
            for (int i = 0; i < inventario.size(); i++) {
                if (inventario.get(i).getNumeroHabitacion() == habitacion.getNumeroHabitacion()) {
                    inventario.set(i, habitacion);
                    return habitacion;
                }
            }
            throw new IllegalArgumentException("No existe la habitación número: " + habitacion.getNumeroHabitacion());
        }
    }
}