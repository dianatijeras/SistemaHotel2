package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaActualizacionDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.model.estado.EstadoHabitacionBase;
import co.edu.uniquindio.sistemahotel.backend.model.estado.EstadosHabitacion;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
import co.edu.uniquindio.sistemahotel.backend.service.PagoService;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
class PatronStateHabitacionTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final int HABITACION = 101;
    private static final int OTRA = 102;
    private static final int NUMERO_DE_ESTADOS = 6;

    private static final Path FUENTE_SERVICIO_HABITACION = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotel/backend/service/HabitacionService.java");
    private static final Path FUENTE_SERVICIO_ESTADIA = Path.of(
            "src/main/java/co/edu/uniquindio/sistemahotel/backend/service/EstadiaService.java");

    @Autowired
    private DataStore dataStore;

    @Autowired
    private HabitacionService habitacionService;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EstadiaService estadiaService;

    @Autowired
    private PagoService pagoService;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones().forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
    }


    @Nested
    @DisplayName("1. La infraestructura del patron")
    class Infraestructura {

        @Test
        @DisplayName("Hay un objeto por cada estado del enum y se identifica a si mismo")
        void unObjetoPorEstado() {
            for (EstadoHabitacion estado : EstadoHabitacion.values()) {
                EstadoHabitacionBase comportamiento = EstadosHabitacion.de(estado);

                assertThat(comportamiento).as("el estado %s tiene comportamiento", estado).isNotNull();
                assertThat(comportamiento.estado())
                        .as("el comportamiento se identifica como %s", estado)
                        .isEqualTo(estado);
            }
        }

        @Test
        @DisplayName("Las instancias son unicas: no se crea un objeto por peticion")
        void instanciasUnicas() {
            for (EstadoHabitacion estado : EstadoHabitacion.values()) {
                assertThat(EstadosHabitacion.de(estado))
                        .as("%s devuelve siempre la misma instancia", estado)
                        .isSameAs(EstadosHabitacion.de(estado));
            }
        }

        @Test
        @DisplayName("Las clases concretas no guardan estado mutable ni sincronizan")
        void sinEstadoNiBloqueos() throws Exception {
            Class<?>[] concretas = new Class<?>[NUMERO_DE_ESTADOS];
            String[] nombres = {"HabitacionDisponible", "HabitacionReservada", "HabitacionOcupada",
                    "HabitacionEnLimpieza", "HabitacionMantenimiento", "HabitacionFueraDeServicio"};
            for (int i = 0; i < nombres.length; i++) {
                concretas[i] = Class.forName(
                        "co.edu.uniquindio.sistemahotel.backend.model.estado." + nombres[i]);
                assertThat(EstadoHabitacionBase.class.isAssignableFrom(concretas[i]))
                        .as("%s implementa el contrato del patron", nombres[i])
                        .isTrue();
            }

            for (Class<?> clase : concretas) {
                assertThat(Modifier.isAbstract(clase.getModifiers()))
                        .as("%s es concreta", clase.getSimpleName())
                        .isFalse();

                for (Field campo : clase.getDeclaredFields()) {
                    boolean inmutable = Modifier.isStatic(campo.getModifiers())
                            || Modifier.isFinal(campo.getModifiers());
                    assertThat(inmutable)
                            .as("%s solo tiene campos estaticos o finales", clase.getSimpleName())
                            .isTrue();
                    assertThat(campo.getType().isArray())
                            .as("%s no guarda arrays", clase.getSimpleName())
                            .isFalse();
                }

                for (Method metodo : clase.getDeclaredMethods()) {
                    assertThat(Modifier.isSynchronized(metodo.getModifiers()))
                            .as("%s no sincroniza: los bloqueos son del servicio",
                                    clase.getSimpleName())
                            .isFalse();
                }
            }
        }

        @Test
        @DisplayName("Las clases concretas no reciben repositorios ni almacenes")
        void sinDependenciasDeDatos() {
            for (Class<?> clase : EstadosHabitacion.class.getDeclaredClasses()) {
                for (Field campo : clase.getDeclaredFields()) {
                    String tipo = campo.getType().getSimpleName();
                    assertThat(tipo)
                            .as("%s no guarda un repositorio ni un almacén", clase.getSimpleName())
                            .doesNotContain("Repository")
                            .doesNotContain("DataStore")
                            .doesNotContain("Service");
                }
            }
        }
    }


    @Nested
    @DisplayName("2. Cada estado se comporta de forma distinta")
    class ComportamientoDiferencial {

        @Test
        @DisplayName("Ningun par de estados responde igual en las seis preguntas")
        void cadaEstadoSeDistingueDelResto() {
            Set<String> firmas = new LinkedHashSet<>();

            for (EstadoHabitacion estado : EstadoHabitacion.values()) {
                EstadoHabitacionBase c = EstadosHabitacion.de(estado);
                List<String> firma = new ArrayList<>();
                for (EstadoHabitacion destino : EstadoHabitacion.values()) {
                    firma.add(String.valueOf(c.puedeIrA(destino)));
                }
                firma.add(String.valueOf(c.admiteHuespedes()));
                firma.add(String.valueOf(c.admiteOcuparse()));
                firma.add(String.valueOf(c.admiteMarcarEnLimpieza()));
                firma.add(String.valueOf(c.admiteFinalizarLimpieza()));
                firma.add(String.valueOf(c.admiteBloqueo()));
                firmas.add(String.join(",", firma));
            }

            assertThat(firmas)
                    .as("cada estado aporta comportamiento propio, salvo el par bloqueado")
                    .hasSize(NUMERO_DE_ESTADOS - 1);
        }

        @Test
        @DisplayName("MANTENIMIENTO y FUERA_DE_SERVICIO son equivalentes ante todas las preguntas")
        void mantenimientoYFueraDeServicioSonEquivalentes() {
            EstadoHabitacionBase mantenimiento = EstadosHabitacion.de(EstadoHabitacion.MANTENIMIENTO);
            EstadoHabitacionBase fuera = EstadosHabitacion.de(EstadoHabitacion.FUERA_DE_SERVICIO);

            for (EstadoHabitacion destino : EstadoHabitacion.values()) {
                assertThat(mantenimiento.puedeIrA(destino))
                        .as("puedeIrA(%s)", destino)
                        .isEqualTo(fuera.puedeIrA(destino));
            }
            assertThat(mantenimiento.admiteHuespedes()).isEqualTo(fuera.admiteHuespedes());
            assertThat(mantenimiento.admiteOcuparse()).isEqualTo(fuera.admiteOcuparse());
            assertThat(mantenimiento.admiteMarcarEnLimpieza())
                    .isEqualTo(fuera.admiteMarcarEnLimpieza());
            assertThat(mantenimiento.admiteFinalizarLimpieza())
                    .isEqualTo(fuera.admiteFinalizarLimpieza());
            assertThat(mantenimiento.admiteBloqueo()).isEqualTo(fuera.admiteBloqueo());

            assertThat(mantenimiento.estado()).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
            assertThat(fuera.estado()).isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
        }

        @Test
        @DisplayName("DISPONIBLE: admite huespedes, check-in, limpieza y bloqueo")
        void disponible() {
            EstadoHabitacionBase c = EstadosHabitacion.de(EstadoHabitacion.DISPONIBLE);

            assertThat(c.admiteHuespedes()).isTrue();
            assertThat(c.admiteOcuparse()).isTrue();
            assertThat(c.admiteMarcarEnLimpieza()).isTrue();
            assertThat(c.admiteFinalizarLimpieza()).isFalse();
            assertThat(c.admiteBloqueo()).isTrue();
            assertThat(c.mensajeDeCheckIn(habDummy())).as("no hay rechazo").isNull();
        }

        @Test
        @DisplayName("OCUPADA: no puede salir hacia ningun estado salvo repetirse")
        void ocupada() {
            EstadoHabitacionBase c = EstadosHabitacion.de(EstadoHabitacion.OCUPADA);

            for (EstadoHabitacion destino : EstadoHabitacion.values()) {
                assertThat(c.puedeIrA(destino))
                        .as("OCUPADA -> %s solo si se repite el estado", destino)
                        .isEqualTo(destino == EstadoHabitacion.OCUPADA);
            }
            assertThat(c.admiteOcuparse()).isFalse();
            assertThat(c.admiteMarcarEnLimpieza()).isTrue();
            assertThat(c.admiteBloqueo()).isFalse();
            assertThat(c.mensajeDeCheckIn(habDummy())).contains("ya está OCUPADA");
        }

        @Test
        @DisplayName("EN_LIMPIEZA: es el unico que puede confirmar que termino la limpieza")
        void enLimpieza() {
            EstadoHabitacionBase c = EstadosHabitacion.de(EstadoHabitacion.EN_LIMPIEZA);

            assertThat(c.admiteFinalizarLimpieza()).isTrue();
            assertThat(c.admiteHuespedes()).isFalse();
            assertThat(c.admiteOcuparse()).isFalse();
            assertThat(c.admiteBloqueo()).isTrue();
            assertThat(c.mensajeDeCheckIn(habDummy())).contains("EN_LIMPIEZA");
        }

        @Test
        @DisplayName("MANTENIMIENTO y FUERA_DE_SERVICIO bloquean huespedes y check-in")
        void estadosBloqueados() {
            for (EstadoHabitacion estado : List.of(EstadoHabitacion.MANTENIMIENTO,
                    EstadoHabitacion.FUERA_DE_SERVICIO)) {
                EstadoHabitacionBase c = EstadosHabitacion.de(estado);

                assertThat(c.admiteHuespedes()).as("%s", estado).isFalse();
                assertThat(c.admiteOcuparse()).as("%s", estado).isFalse();
                assertThat(c.admiteBloqueo()).as("%s ya esta bloqueada", estado).isFalse();
                assertThat(c.admiteFinalizarLimpieza()).as("%s", estado).isFalse();
                assertThat(c.admiteMarcarEnLimpieza()).as("%s", estado).isFalse();
                assertThat(c.mensajeDeCheckIn(habDummy())).contains("está en estado");
            }
        }

        @Test
        @DisplayName("RESERVADA: admite huespedes y check-in, pero no bloqueo por el endpoint")
        void reservada() {
            EstadoHabitacionBase c = EstadosHabitacion.de(EstadoHabitacion.RESERVADA);

            assertThat(c.admiteHuespedes()).isTrue();
            assertThat(c.admiteOcuparse()).isTrue();
            assertThat(c.admiteBloqueo()).isFalse();
            assertThat(c.admiteFinalizarLimpieza()).isFalse();
            assertThat(c.puedeIrA(EstadoHabitacion.MANTENIMIENTO))
                    .as("por la via administrativa si puede pasar a mantenimiento")
                    .isTrue();
        }

        @Test
        @DisplayName("RESERVADA es alcanzable por la via administrativa y se comprueba aqui")
        void reservadaEsAlcanzablePorLaViaAdministrativa() {
            Habitacion hab = habitacionService.obtenerOFallar(HABITACION);
            assertThat(hab.getEstadoHabitacion())
                    .as("parte de DISPONIBLE")
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);

            Habitacion cambiada = habitacionService.cambiarEstado(HABITACION,
                    EstadoHabitacion.RESERVADA);

            assertThat(cambiada.getEstadoHabitacion())
                    .as("la via administrativa si alcanza RESERVADA")
                    .isEqualTo(EstadoHabitacion.RESERVADA);
            assertThat(habitacionRepository.buscarPorNumero(HABITACION))
                    .as("y el repositorio ve el mismo cambio")
                    .hasValueSatisfying(h -> assertThat(h.getEstadoHabitacion())
                            .isEqualTo(EstadoHabitacion.RESERVADA));
        }

        @Test
        @DisplayName("RESERVADA no es un callejon sin salida: se sale por limpieza y liberacion")
        void reservadaNoEsCallejonSinSalida() {
            habitacionService.cambiarEstado(HABITACION, EstadoHabitacion.RESERVADA);

            assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION,
                    EstadoHabitacion.DISPONIBLE))
                    .as("no se puede volver a DISPONIBLE sin confirmar la limpieza")
                    .isInstanceOf(IllegalStateException.class);

            habitacionService.cambiarEstado(HABITACION, EstadoHabitacion.EN_LIMPIEZA);
            Habitacion liberada = habitacionService.marcarDisponible(HABITACION);

            assertThat(liberada.getEstadoHabitacion())
                    .as("si hay salida, y es la de siempre: limpiar y luego liberar")
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("El endpoint de bloqueo si rechaza RESERVADA, aunque la tabla lo admita")
        void elEndpointDeBloqueoRechazaReservada() {
            habitacionService.cambiarEstado(HABITACION, EstadoHabitacion.RESERVADA);

            assertThat(EstadosHabitacion.de(EstadoHabitacion.RESERVADA).puedeIrA(
                    EstadoHabitacion.MANTENIMIENTO))
                    .as("la tabla administrativa si lo admite")
                    .isTrue();
            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .as("pero el endpoint de bloqueo lo rechaza")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RESERVADA");

            assertThat(habitacionService.obtenerOFallar(HABITACION).getEstadoHabitacion())
                    .as("el rechazo no movio la habitacion")
                    .isEqualTo(EstadoHabitacion.RESERVADA);
        }

        @Test
        @DisplayName("Los cinco estados no ocupados comparten la tabla administrativa")
        void estadosNoOcupadosCompartenTabla() {
            Set<EstadoHabitacion> noOcupados = EnumSet.of(
                    EstadoHabitacion.DISPONIBLE, EstadoHabitacion.RESERVADA,
                    EstadoHabitacion.EN_LIMPIEZA, EstadoHabitacion.MANTENIMIENTO,
                    EstadoHabitacion.FUERA_DE_SERVICIO);

            for (EstadoHabitacion origen : noOcupados) {
                EstadoHabitacionBase c = EstadosHabitacion.de(origen);
                for (EstadoHabitacion destino : EstadoHabitacion.values()) {
                    boolean esperado = noOcupados.contains(destino)
                            && (destino != EstadoHabitacion.DISPONIBLE || origen == EstadoHabitacion.DISPONIBLE);

                    assertThat(c.puedeIrA(destino))
                            .as("%s -> %s", origen, destino)
                            .isEqualTo(esperado);
                }
            }
        }
    }


    @Nested
    @DisplayName("3. Las 36 combinaciones, respondidas por el patron y por el servicio")
    class MatrizDeTransiciones {

        @Test
        @DisplayName("El patron y el servicio coinciden en las 36 celdas")
        void elPatronYElServicioCoinciden() {
            for (EstadoHabitacion origen : EstadoHabitacion.values()) {
                for (EstadoHabitacion destino : EstadoHabitacion.values()) {
                    forzar(origen);
                    boolean elPatronLoAdmite = EstadosHabitacion.de(origen).puedeIrA(destino);

                    boolean elServicioLoAdmitio;
                    try {
                        habitacionService.cambiarEstado(HABITACION, destino);
                        elServicioLoAdmitio = true;
                    } catch (IllegalStateException e) {
                        elServicioLoAdmitio = false;
                    }

                    assertThat(elServicioLoAdmitio)
                            .as("celda %s -> %s", origen, destino)
                            .isEqualTo(elPatronLoAdmite);
                }
            }
        }

        @Test
        @DisplayName("El reparto es de 22 admitidas y 14 rechazadas")
        void repartoDeLaMatriz() {
            int admitidas = 0;
            int rechazadas = 0;

            for (EstadoHabitacion origen : EstadoHabitacion.values()) {
                for (EstadoHabitacion destino : EstadoHabitacion.values()) {
                    forzar(origen);
                    try {
                        habitacionService.cambiarEstado(HABITACION, destino);
                        admitidas++;
                    } catch (IllegalStateException e) {
                        rechazadas++;
                    }
                }
            }
            assertThat(admitidas).isEqualTo(22);
            assertThat(rechazadas).isEqualTo(14);
            assertThat(admitidas + rechazadas).isEqualTo(36);
        }
    }


    @Nested
    @DisplayName("4. Los servicios de produccion utilizan los objetos State")
    class UsoEnProduccion {

        @Test
        @DisplayName("HabitacionService consulta el patron desde su codigo")
        void habitacionServiceUsaElPatron() throws Exception {
            String fuente = Files.readString(FUENTE_SERVICIO_HABITACION);

            assertThat(fuente).contains("EstadosHabitacion.de(");
            assertThat(fuente).contains("EstadoHabitacionBase");
            assertThat(fuente.split("EstadosHabitacion\\.", -1).length - 1)
                    .as("HabitacionService consulta el patron en varios metodos")
                    .isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("EstadiaService consulta el patron en vez de enumerar estados")
        void estadiaServiceUsaElPatron() throws Exception {
            String fuente = Files.readString(FUENTE_SERVICIO_ESTADIA);

            assertThat(fuente).contains("EstadosHabitacion.de(");
            assertThat(fuente).contains("EstadoHabitacionBase");
        }

        @Test
        @DisplayName("Solo queda en el servicio la comprobacion que consulta otras entidades")
        void sinListasDeExclusion() throws Exception {
            String habitacion = Files.readString(FUENTE_SERVICIO_HABITACION);
            String estadia = Files.readString(FUENTE_SERVICIO_ESTADIA);

            assertThat(habitacion)
                    .as("ya no hay listas de exclusion de estados para decidir reglas")
                    .doesNotContain("!= EstadoHabitacion.MANTENIMIENTO")
                    .doesNotContain("!= EstadoHabitacion.FUERA_DE_SERVICIO")
                    .doesNotContain("!= EstadoHabitacion.EN_LIMPIEZA")
                    .doesNotContain("!= EstadoHabitacion.DISPONIBLE")
                    .doesNotContain("!= EstadoHabitacion.OCUPADA")
                    .doesNotContain("== EstadoHabitacion.MANTENIMIENTO")
                    .doesNotContain("== EstadoHabitacion.FUERA_DE_SERVICIO");

            assertThat(estadia)
                    .as("la validacion del check-in ya no enumera estados")
                    .doesNotContain("== EstadoHabitacion.OCUPADA")
                    .doesNotContain("== EstadoHabitacion.EN_LIMPIEZA")
                    .doesNotContain("== EstadoHabitacion.MANTENIMIENTO")
                    .doesNotContain("EstadoHabitacion.FUERA_DE_SERVICIO");

            assertThat(habitacion)
                    .as("permanece solo la guarda que mira si hay estadia activa")
                    .containsOnlyOnce("hab.getEstadoHabitacion() == EstadoHabitacion.OCUPADA");
            assertThat(habitacion).contains("tieneEstadiaActiva(");
        }

        @Test
        @DisplayName("El enum sigue siendo lo que viajan por el JSON: el contrato no cambio")
        void elEnumSigueSiendoElContrato() throws Exception {
            Habitacion hab = habitacionService.obtenerOFallar(HABITACION);
            habitacionService.marcarMantenimiento(HABITACION);

            assertThat(habitacionRepository.buscarPorNumero(HABITACION).orElseThrow()
                    .getEstadoHabitacion()).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
            assertThat(hab.getEstadoHabitacion()).isInstanceOf(EstadoHabitacion.class);
        }
    }


    @Nested
    @DisplayName("5. Un rechazo no modifica habitacion, reserva ni estadia")
    class RechazosSinResiduos {

        @Test
        @DisplayName("Ningun rechazo del patron deja estado parcial")
        void rechazosNoMutanNada() {
            for (EstadoHabitacion origen : EstadoHabitacion.values()) {
                for (EstadoHabitacion destino : EstadoHabitacion.values()) {
                    if (EstadosHabitacion.de(origen).puedeIrA(destino)) {
                        continue;
                    }
                    forzar(origen);
                    int antes = dataStore.getHabitaciones().size();

                    assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION, destino))
                            .as("%s -> %s debe rechazarse", origen, destino)
                            .isInstanceOf(IllegalStateException.class);

                    assertThat(estadoDe(HABITACION)).as("%s -> %s", origen, destino).isEqualTo(origen);
                    assertThat(dataStore.getHabitaciones()).hasSize(antes);
                    assertThat(dataStore.getEstadias()).isEmpty();
                    assertThat(dataStore.getReservas()).isEmpty();
                }
            }
        }

        @Test
        @DisplayName("Con una estadia viva, los rechazos dejan intactos habitacion, reserva y estadia")
        void conEstadiaViva() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(2)));
            reservaService.confirmarReserva(reserva.getIdReserva());
            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());

            for (EstadoHabitacion destino : List.of(EstadoHabitacion.MANTENIMIENTO,
                    EstadoHabitacion.FUERA_DE_SERVICIO, EstadoHabitacion.EN_LIMPIEZA,
                    EstadoHabitacion.DISPONIBLE)) {
                assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION, destino))
                        .isInstanceOf(IllegalStateException.class);
            }

            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(estadia.getReserva().getEstadoReserva()).isEqualTo(EstadoReserva.CHECKED_IN);

            saldar(estadia.getIdEstadia());
            assertThat(estadiaService.realizarCheckOut(estadia.getIdEstadia()).getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.FINALIZADA);
        }
    }


    @Nested
    @DisplayName("6. Las seis funcionalidades obligatorias siguen operando")
    class FuncionalidadesIntactas {

        @Test
        @DisplayName("reserva -> check-in -> pagos -> check-out -> limpieza")
        void recorridoCompleto() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(2)));
            reservaService.confirmarReserva(reserva.getIdReserva());

            ReservaActualizacionDTO actualizacion = new ReservaActualizacionDTO();
            actualizacion.setNumeroHabitacion(HABITACION);
            actualizacion.setFechaInicio(HOY);
            actualizacion.setFechaFin(HOY.plusDays(3));
            reservaService.modificarReserva(reserva.getIdReserva(), actualizacion);

            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);

            PagoRequestDTO dto = new PagoRequestDTO();
            dto.setIdEstadia(estadia.getIdEstadia());
            dto.setMonto(100_000);
            dto.setMetodoPago(MetodoPago.EFECTIVO);
            dto.setConcepto("Abono");
            Pago pago = pagoService.registrarPago(dto);
            pagoService.aprobarPago(pago.getIdPago(), "REF-STATE");

            assertThat(pagoService.consultarSaldo(estadia.getIdEstadia()).getSaldoPendiente())
                    .isPositive();
            assertThat(estadiaService.calcularCosto(estadia.getIdEstadia()).getTotalGeneral())
                    .isPositive();

            saldar(estadia.getIdEstadia());
            assertThat(estadiaService.realizarCheckOut(estadia.getIdEstadia()).getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(habitacionService.finalizarLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("F-05: los huespedes siguen presentes y su username sigue siendo unico")
        void huespedes() {
            List<Huesped> huespedes = dataStore.getHuespedes();
            assertThat(huespedes).isNotEmpty();

            String documento = "CC99555001";
            forzar(EstadoHabitacion.DISPONIBLE);
            List<String> usernames = huespedes.stream().map(Huesped::getUsername).toList();

            assertThat(usernames).doesNotHaveDuplicates();
            assertThat(documento).isNotBlank();
        }

        @Test
        @DisplayName("F-04: disponibilidad y solapamiento siguen aplicando")
        void disponibilidadYSolapamiento() {
            assertThat(habitacionService.listarDisponibles(HOY, HOY.plusDays(3)))
                    .extracting(Habitacion::getNumeroHabitacion)
                    .contains(HABITACION);

            reservaService.crearReserva(dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(3)));

            assertThat(habitacionService.tieneReservaSolapada(
                    HABITACION, HOY.plusDays(1), HOY.plusDays(4), null)).isTrue();
        }

        @Test
        @DisplayName("El patron no ablanda la regla de bloqueo")
        void elPatronNoAblandaElBloqueo() {
            habitacionService.marcarMantenimiento(HABITACION);

            assertThat(habitacionService.estaHabilitadaParaReservar(EstadoHabitacion.MANTENIMIENTO))
                    .isFalse();
            assertThatThrownBy(() -> habitacionService.marcarMantenimiento(HABITACION))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(habitacionService.marcarFueraDeServicio(OTRA).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.FUERA_DE_SERVICIO);
        }

        @Test
        @DisplayName("El patron no ablanda la regla unica de liberacion")
        void elPatronNoAblandaLaLiberacion() {
            habitacionService.marcarFueraDeServicio(OTRA);

            assertThatThrownBy(() -> habitacionService.cambiarEstado(
                    OTRA, EstadoHabitacion.DISPONIBLE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("solo se libera confirmando");

            habitacionService.cambiarEstado(OTRA, EstadoHabitacion.EN_LIMPIEZA);
            assertThat(habitacionService.finalizarLimpieza(OTRA).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("marcar en limpieza conserva su propio comportamiento")
        void marcarEnLimpiezaSinCambios() {
            forzar(EstadoHabitacion.OCUPADA);

            assertThat(habitacionService.marcarEnLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertThat(habitacionService.finalizarLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }
    }


    /** Habitacion auxiliar para construir mensajes sin tocar el almacen. */
    private Habitacion habDummy() {
        return new Habitacion(HABITACION, TipoHabitacion.ESTANDAR, 100_000, 1, "Auxiliar", 2, null);
    }

    private EstadoHabitacion estadoDe(int numero) {
        return habitacionRepository.buscarPorNumero(numero).orElseThrow().getEstadoHabitacion();
    }

    private void forzar(EstadoHabitacion estado) {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(estado);
    }

    private ReservaRequestDTO dtoReserva(String idHuesped, int numero, LocalDate ini, LocalDate fin) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped(idHuesped);
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(ini);
        dto.setFechaFin(fin);
        dto.setAdultos(2);
        dto.setNinos(0);
        return dto;
    }

    private void saldar(String idEstadia) {
        double pendiente = pagoService.consultarSaldo(idEstadia).getSaldoPendiente().doubleValue();
        if (pendiente <= 0) {
            return;
        }
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setMonto(pendiente);
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Pago del saldo");
        Pago pago = pagoService.registrarPago(dto);
        pagoService.aprobarPago(pago.getIdPago(), "REF-STATE");
    }
}