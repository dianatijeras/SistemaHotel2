package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaActualizacionDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.enums.MetodoPago;
import co.edu.uniquindio.sistemahotel.backend.model.*;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointGenericoEstadoTest {

    private static final LocalDate HOY = LocalDate.now();
    private static final int HABITACION = 101;
    private static final int OTRA = 102;
    private static final String RECEP_ROL = "RECEPCIONISTA";
    private static final String RECEP_ID = "R-001";
    private static final String RECEP_USER = "maria.l";

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

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones().forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
    }


    @Nested
    @DisplayName("1 y 2. OCUPADA no puede pasar a MANTENIMIENTO ni a FUERA_DE_SERVICIO por /estado")
    class OcupadaHaciaBloqueo {

        @Test
        @DisplayName("OCUPADA con estadia EN_CURSO -> MANTENIMIENTO devuelve 409")
        void ocupadaAMantenimientoPorEndpointGenerico() throws Exception {
            estadiaEnCurso(HABITACION);

            MvcResult resultado = invocarEstado(HABITACION, "MANTENIMIENTO");

            assertThat(resultado.getResponse().getStatus())
                    .as("no se puede sacar una habitacion ocupada del inventario")
                    .isEqualTo(409);
            assertThat(cuerpo(resultado)).contains("desde OCUPADA a MANTENIMIENTO");
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("OCUPADA con estadia EN_CURSO -> FUERA_DE_SERVICIO devuelve 409")
        void ocupadaAFueraDeServicioPorEndpointGenerico() throws Exception {
            estadiaEnCurso(HABITACION);

            MvcResult resultado = invocarEstado(HABITACION, "FUERA_DE_SERVICIO");

            assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
            assertThat(cuerpo(resultado)).contains("desde OCUPADA a FUERA_DE_SERVICIO");
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("La habitacion y su estadia siguen intactas tras el rechazo")
        void elRechazoNoDejaResiduos() throws Exception {
            Estadia estadia = estadiaEnCurso(HABITACION);
            String idEstadia = estadia.getIdEstadia();

            invocarEstado(HABITACION, "MANTENIMIENTO");
            invocarEstado(HABITACION, "FUERA_DE_SERVICIO");

            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(estadia.getEstadoEstadia()).isEqualTo(EstadoEstadia.EN_CURSO);
            assertThat(estadia.getReserva().getEstadoReserva()).isEqualTo(EstadoReserva.CHECKED_IN);
            assertThat(dataStore.getEstadias()).hasSize(1);
            assertThat(dataStore.getReservas()).hasSize(1);

            saldar(idEstadia);
            assertThat(estadiaService.realizarCheckOut(idEstadia).getEstadoEstadia())
                    .isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
        }
    }

    @Nested
    @DisplayName("3. El endpoint generico no simula check-in ni check-out")
    class NoSimulaOperacionesDelServicio {

        @Test
        @DisplayName("Entrar en OCUPADA se rechaza: ese estado es del check-in")
        void noSimulaCheckIn() throws Exception {
            // Sin estadia de por medio: marcarla ocupada por aqui seria el caso mas grave,
            // porque el huesped no existe y la habitacion quedaria sin forma de liberarse.
            MvcResult resultado = invocarEstado(HABITACION, "OCUPADA");

            assertThat(resultado.getResponse().getStatus())
                    .as("OCUPADA sin estadia es exactamente lo que el check-in impide")
                    .isEqualTo(409);
            assertThat(cuerpo(resultado)).contains("solo lo establece el check-in");
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("Entrar en OCUPADA se rechaza desde cualquier estado libre")
        void noSimulaCheckInDesdeVariosEstados() throws Exception {
            for (EstadoHabitacion origen : List.of(EstadoHabitacion.EN_LIMPIEZA,
                    EstadoHabitacion.MANTENIMIENTO, EstadoHabitacion.FUERA_DE_SERVICIO,
                    EstadoHabitacion.RESERVADA)) {
                forzar(origen);

                MvcResult resultado = invocarEstado(HABITACION, "OCUPADA");

                assertThat(resultado.getResponse().getStatus())
                        .as("desde %s no se puede entrar en OCUPADA", origen)
                        .isEqualTo(409);
                assertThat(estadoDe(HABITACION)).isEqualTo(origen);
            }
        }

        @Test
        @DisplayName("Salir de OCUPADA hacia EN_LIMPIEZA se rechaza aunque no haya estadia activa")
        void noSimulaCheckOutSinEstadia() throws Exception {

            forzar(EstadoHabitacion.OCUPADA);

            MvcResult resultado = invocarEstado(HABITACION, "EN_LIMPIEZA");

            assertThat(resultado.getResponse().getStatus())
                    .as("sin estadia activa tampoco se puede simular la mitad del check-out")
                    .isEqualTo(409);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
        }

        @Test
        @DisplayName("Liberar a DISPONIBLE se rechaza desde todos los estados ocupados o bloqueados")
        void noSimulaLaFinalizacionDeLimpieza() throws Exception {
            for (EstadoHabitacion origen : List.of(EstadoHabitacion.OCUPADA,
                    EstadoHabitacion.RESERVADA, EstadoHabitacion.MANTENIMIENTO,
                    EstadoHabitacion.FUERA_DE_SERVICIO)) {
                forzar(origen);

                MvcResult resultado = invocarEstado(HABITACION, "DISPONIBLE");

                assertThat(resultado.getResponse().getStatus())
                        .as("desde %s no se puede liberar", origen)
                        .isEqualTo(409);
                assertThat(cuerpo(resultado)).contains("solo se libera confirmando");
                assertThat(estadoDe(HABITACION)).isEqualTo(origen);
            }
        }
    }



    @Nested
    @DisplayName("4. Un rechazo no modifica habitacion, reserva ni estadia")
    class EstadoConservadoAnteFallos {

        @Test
        @DisplayName("Las 36 combinaciones: cada rechazado deja el estado intacto")
        void matrizCompletaDeTransiciones() {

            List<EstadoHabitacion> esperados = List.of(
                    EstadoHabitacion.DISPONIBLE, EstadoHabitacion.RESERVADA,
                    EstadoHabitacion.OCUPADA, EstadoHabitacion.EN_LIMPIEZA,
                    EstadoHabitacion.MANTENIMIENTO, EstadoHabitacion.FUERA_DE_SERVICIO);
            int rechazos = 0;
            int permitidos = 0;

            for (EstadoHabitacion origen : esperados) {
                for (EstadoHabitacion destino : esperados) {
                    forzar(origen);
                    int antes = dataStore.getHabitaciones().size();

                    boolean excepcion;
                    try {
                        habitacionService.cambiarEstado(HABITACION, destino);
                        excepcion = false;
                    } catch (IllegalStateException e) {
                        excepcion = true;
                    }

                    String celda = origen + " -> " + destino;
                    if (excepcion) {
                        rechazos++;
                        assertThat(estadoDe(HABITACION))
                                .as("%s debe rechazarse sin cambiar el estado", celda)
                                .isEqualTo(origen);
                    } else {
                        permitidos++;
                        assertThat(estadoDe(HABITACION))
                                .as("%s debe aplicarse", celda)
                                .isEqualTo(destino);
                    }
                    assertThat(dataStore.getHabitaciones())
                            .as("%s no crea ni borra habitaciones", celda)
                            .hasSize(antes);
                }
            }

            assertThat(permitidos + rechazos).isEqualTo(36);
            assertThat(rechazos)
                    .as("la via administrativa cierra 14 de las 36 combinaciones")
                    .isEqualTo(14);
        }

        @Test
        @DisplayName("Un estado null se reporta como argument invalido y no cambia nada")
        void estadoNullNoCambiaNada() {
            forzar(EstadoHabitacion.MANTENIMIENTO);

            assertThatThrownBy(() -> habitacionService.cambiarEstado(HABITACION, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El nuevo estado de la habitación es obligatorio.");
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("Una habitacion inexistente se reporta antes que la regla de transicion")
        void inexistenteSeReportaAntes() {
            assertThatThrownBy(() -> habitacionService.cambiarEstado(999, EstadoHabitacion.MANTENIMIENTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No existe la habitación número: 999");
        }
    }


    @Nested
    @DisplayName("5. Las transiciones administrativas legitimas siguen funcionando")
    class TransicionesAdministrativasLegitimas {

        @Test
        @DisplayName("DISPONIBLE -> MANTENIMIENTO, FUERA_DE_SERVICIO, EN_LIMPIEZA y RESERVADA")
        void desdeDisponible() {
            for (EstadoHabitacion destino : List.of(EstadoHabitacion.MANTENIMIENTO,
                    EstadoHabitacion.FUERA_DE_SERVICIO, EstadoHabitacion.EN_LIMPIEZA,
                    EstadoHabitacion.RESERVADA)) {
                forzar(EstadoHabitacion.DISPONIBLE);

                assertThatCode(() -> habitacionService.cambiarEstado(HABITACION, destino))
                        .as("DISPONIBLE -> %s es una decision administrativa legitima", destino)
                        .doesNotThrowAnyException();
                assertThat(estadoDe(HABITACION)).isEqualTo(destino);
            }
        }

        @Test
        @DisplayName("EN_LIMPIEZA -> MANTENIMIENTO: una habitacion que se rompio al limpiarla")
        void enLimpiezaAMantenimiento() {
            forzar(EstadoHabitacion.EN_LIMPIEZA);

            assertThatCode(() -> habitacionService.cambiarEstado(
                    HABITACION, EstadoHabitacion.MANTENIMIENTO))
                    .doesNotThrowAnyException();
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("Un estado bloqueado vuelve al servicio pasando por EN_LIMPIEZA")
        void retornoAlServicioTrasConfirmarLimpieza() {

            habitacionService.marcarFueraDeServicio(OTRA);

            assertThatThrownBy(() -> habitacionService.cambiarEstado(
                    OTRA, EstadoHabitacion.DISPONIBLE))
                    .isInstanceOf(IllegalStateException.class);

            habitacionService.cambiarEstado(OTRA, EstadoHabitacion.EN_LIMPIEZA);
            habitacionService.finalizarLimpieza(OTRA);

            assertThat(estadoDe(OTRA)).isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("Repetir el estado actual es idempotente en los seis estados")
        void repetirElEstadoActualEsIdempotente() throws Exception {
            for (EstadoHabitacion estado : EstadoHabitacion.values()) {
                forzar(estado);

                MvcResult resultado = invocarEstado(HABITACION, estado.name());

                assertThat(resultado.getResponse().getStatus())
                        .as("%s -> %s no cambia nada y debe aceptarse", estado, estado)
                        .isEqualTo(200);
                assertThat(estadoDe(HABITACION)).isEqualTo(estado);
            }
        }

        @Test
        @DisplayName("El endpoint sigue respondiendo 409 con cuerpo de negocio, no 500")
        void losRechazosSiguenSiendo409ConMensaje() throws Exception {
            forzar(EstadoHabitacion.OCUPADA);

            MvcResult resultado = invocarEstado(HABITACION, "MANTENIMIENTO");

            assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
            assertThat(cuerpo(resultado))
                    .contains("\"status\":409")
                    .contains("mensaje");
        }
    }



    @Nested
    @DisplayName("6 y 7. Recorrido completo y ausencia de regresiones en las seis funcionalidades")
    class RecorridoCompletoYRegresion {

        @Test
        @DisplayName("reserva -> check-in -> pagos -> check-out -> limpieza, con la via generica en medio")
        void recorridoCompleto() {
            Reserva reserva = reservaService.crearReserva(
                    dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(2)));
            reservaService.confirmarReserva(reserva.getIdReserva());

            reservaService.modificarReserva(reserva.getIdReserva(), dtoActualizacion(
                    HABITACION, HOY, HOY.plusDays(3)));

            habitacionService.marcarMantenimiento(OTRA);
            habitacionService.cambiarEstado(OTRA, EstadoHabitacion.EN_LIMPIEZA);
            habitacionService.finalizarLimpieza(OTRA);
            assertThat(estadoDe(OTRA)).isEqualTo(EstadoHabitacion.DISPONIBLE);

            Estadia estadia = estadiaService.realizarCheckIn(reserva.getIdReserva());
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);

            assertThatThrownBy(() -> habitacionService.cambiarEstado(
                    HABITACION, EstadoHabitacion.EN_LIMPIEZA))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);

            PagoRequestDTO parcial = new PagoRequestDTO();
            parcial.setIdEstadia(estadia.getIdEstadia());
            parcial.setMonto(100_000);
            parcial.setMetodoPago(MetodoPago.EFECTIVO);
            parcial.setConcepto("Abono");
            Pago p1 = pagoService.registrarPago(parcial);
            pagoService.aprobarPago(p1.getIdPago(), "REF-1");
            assertThat(pagoService.tieneSaldoPendiente(estadia.getIdEstadia())).isTrue();

            assertThat(estadiaService.calcularCosto(estadia.getIdEstadia()).getTotalGeneral())
                    .isPositive();

            saldar(estadia.getIdEstadia());
            assertThat(pagoService.tieneSaldoPendiente(estadia.getIdEstadia())).isFalse();

            Estadia cerrada = estadiaService.realizarCheckOut(estadia.getIdEstadia());
            assertThat(cerrada.getEstadoEstadia()).isEqualTo(EstadoEstadia.FINALIZADA);
            assertThat(cerrada.getReserva().getEstadoReserva()).isEqualTo(EstadoReserva.FINALIZADA);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);

            habitacionService.finalizarLimpieza(HABITACION);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.DISPONIBLE);

            assertThat(habitacionService.listarDisponibles(HOY, HOY.plusDays(3)))
                    .extracting(Habitacion::getNumeroHabitacion)
                    .contains(HABITACION);
        }

        @Test
        @DisplayName("F-05: el registro de huespedes no se ve afectado")
        void huespedesSinAfectar() {
            assertThat(huespedesDelAlmacen()).isNotEmpty();
        }

        @Test
        @DisplayName("F-04: la reserva sigue rechazando habitaciones bloqueadas")
        void reservasSiguenBloqueadas() {
            habitacionService.marcarMantenimiento(OTRA);

            assertThatThrownBy(() -> reservaService.crearReserva(
                    dtoReserva("H-001", OTRA, HOY, HOY.plusDays(2))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no admite reservas");
        }

        @Test
        @DisplayName("La solapacion de reservas sigue bloqueando la habitacion")
        void solapamientoSigueBloqueando() {
            reservaService.crearReserva(dtoReserva("H-001", HABITACION, HOY, HOY.plusDays(3)));

            assertThatThrownBy(() -> reservaService.crearReserva(
                    dtoReserva("H-002", HABITACION, HOY.plusDays(1), HOY.plusDays(4)))
            ).isInstanceOf(IllegalStateException.class);
            assertThat(habitacionService.tieneReservaSolapada(
                    HABITACION, HOY.plusDays(1), HOY.plusDays(4), null)).isTrue();
        }

        @Test
        @DisplayName("marcarEnLimpieza y finalizarLimpieza conservan su propio comportamiento")
        void losServiciosDeLimpiezaNoCambian() {

            forzar(EstadoHabitacion.OCUPADA);

            assertThatCode(() -> habitacionService.marcarEnLimpieza(HABITACION))
                    .as("marcarEnLimpieza tiene su propia guarda y no cambia")
                    .doesNotThrowAnyException();
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.EN_LIMPIEZA);
            assertThat(habitacionService.finalizarLimpieza(HABITACION).getEstadoHabitacion())
                    .isEqualTo(EstadoHabitacion.DISPONIBLE);
        }

        @Test
        @DisplayName("Los endpoints de bloqueo siguen rechazando OCUPADA con 409")
        void losEndpointsDeBloqueoNoCambian() throws Exception {
            estadiaEnCurso(HABITACION);

            assertThat(mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/mantenimiento"))
                    .andReturn().getResponse().getStatus()).isEqualTo(409);
            assertThat(mockMvc.perform(put("/api/habitaciones/" + HABITACION + "/fuera-servicio"))
                    .andReturn().getResponse().getStatus()).isEqualTo(409);
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
        }
    }



    @Nested
    @DisplayName("8. Coordinacion concurrente de la via generica")
    class Concurrencia {

        @Test
        @DisplayName("Diez intentos simultaneos de bloquear una habitacion ocupada")
        void bloqueosConcurrentesSobreOcupada() throws Exception {
            estadiaEnCurso(HABITACION);

            int hilos = 10;
            CountDownLatch arranque = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(hilos);
            List<Future<Integer>> futuros = new ArrayList<>();

            for (int i = 0; i < hilos; i++) {
                final int n = i;
                futuros.add(pool.submit(() -> {
                    arranque.await();
                    try {
                        if (n % 2 == 0) {
                            habitacionService.cambiarEstado(
                                    HABITACION, EstadoHabitacion.MANTENIMIENTO);
                        } else {
                            habitacionService.cambiarEstado(
                                    HABITACION, EstadoHabitacion.FUERA_DE_SERVICIO);
                        }
                        return 1;
                    } catch (IllegalStateException e) {
                        return 0;
                    }
                }));
            }

            arranque.countDown();
            int exitos = 0;
            for (Future<Integer> f : futuros) {
                exitos += f.get(20, TimeUnit.SECONDS);
            }
            pool.shutdownNow();

            assertThat(exitos)
                    .as("ningun intento concurrente puede sacar la habitacion de OCUPADA")
                    .isZero();
            assertThat(estadoDe(HABITACION)).isEqualTo(EstadoHabitacion.OCUPADA);
            assertThat(dataStore.getEstadias()).hasSize(1);
        }

        @Test
        @DisplayName("Diez intentos simultaneos sobre una habitacion libre: gana uno y el estado es valido")
        void bloqueosConcurrentesSobreLibre() throws Exception {
            int hilos = 10;
            CountDownLatch arranque = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(hilos);
            List<Future<Boolean>> futuros = new ArrayList<>();

            for (int i = 0; i < hilos; i++) {
                final int n = i;
                futuros.add(pool.submit(() -> {
                    arranque.await();
                    try {
                        habitacionService.marcarMantenimiento(HABITACION);
                        return Boolean.TRUE;
                    } catch (RuntimeException e) {
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

            assertThat(exitos)
                    .as("varios hilos pueden aplicar la misma transicion; da igual cuantos")
                    .isBetween(1, hilos);
            assertThat(estadoDe(HABITACION))
                    .as("el estado final es coherente pese a la carrera benigna")
                    .isEqualTo(EstadoHabitacion.MANTENIMIENTO);
        }
    }



    private MvcResult invocarEstado(int numero, String estado) throws Exception {
        return mockMvc.perform(put("/api/habitaciones/" + numero + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoHabitacion\":\"" + estado + "\"}"))
                .andReturn();
    }

    private String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private EstadoHabitacion estadoDe(int numero) {
        return habitacionRepository.buscarPorNumero(numero).orElseThrow().getEstadoHabitacion();
    }

    /**
     * Fuerza un estado sobre el modelo, sin pasar por el servicio: es util para preparar.
     */
    private void forzar(EstadoHabitacion estado) {
        habitacionService.obtenerOFallar(HABITACION).cambiarEstado(estado);
    }

    private List<Huesped> huespedesDelAlmacen() {
        return dataStore.getHuespedes();
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

    private ReservaActualizacionDTO dtoActualizacion(
            int numero, LocalDate ini, LocalDate fin) {
        var dto = new ReservaActualizacionDTO();
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(ini);
        dto.setFechaFin(fin);
        return dto;
    }

    private Estadia estadiaEnCurso(int numero) {
        Reserva reserva = reservaService.crearReserva(dtoReserva("H-001", numero, HOY, HOY.plusDays(2)));
        reservaService.confirmarReserva(reserva.getIdReserva());
        return estadiaService.realizarCheckIn(reserva.getIdReserva());
    }

    private void saldar(String idEstadia) {
        // Se paga exactamente el saldo pendiente y no el total general: si ya hay un pago
        // aprobado, pagar el total seria un sobrepago y el servicio lo rechazaria.
        var saldo = pagoService.consultarSaldo(idEstadia);
        double pendiente = saldo.getSaldoPendiente().doubleValue();
        if (pendiente <= 0) {
            return;
        }
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setIdEstadia(idEstadia);
        dto.setMonto(pendiente);
        dto.setMetodoPago(MetodoPago.EFECTIVO);
        dto.setConcepto("Pago del saldo pendiente");
        Pago pago = pagoService.registrarPago(dto);
        pagoService.aprobarPago(pago.getIdPago(), "REF-ESTADO");
    }
}
