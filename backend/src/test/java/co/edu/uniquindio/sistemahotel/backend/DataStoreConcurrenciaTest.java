package co.edu.uniquindio.sistemahotel.backend;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Pruebas de concurrencia para el DataStore y los servicios relacionados.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DataStoreConcurrenciaTest {

    private static final int[] HABITACIONES = {101, 102, 103, 201, 202, 301, 302, 401};
    private static final LocalDate BASE = LocalDate.now().plusDays(400);

    @Autowired
    private DataStore dataStore;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reiniciarAlmacen() {
        dataStore.getReservas().clear();
        dataStore.getEstadias().clear();
        dataStore.getPagos().clear();
        dataStore.getHabitaciones().forEach(h -> h.cambiarEstado(EstadoHabitacion.DISPONIBLE));
    }

    @Test
    @DisplayName("Consultar reservas mientras se registran nuevas no produce errores")
    void consultarYRegistrarSimultaneamente_noProduceErrores() throws Exception {
        for (int i = 0; i < 150; i++) {
            registrar(HABITACIONES[i % HABITACIONES.length], BASE.plusDays(i * 3L));
        }

        AtomicBoolean seguir = new AtomicBoolean(true);
        AtomicInteger cme = new AtomicInteger();
        AtomicInteger recorridosOk = new AtomicInteger();
        AtomicInteger http200 = new AtomicInteger();
        AtomicInteger http500 = new AtomicInteger();

        Thread escritor = new Thread(() -> {
            int i = 0;
            while (seguir.get() && i < 3000) {
                try {
                    registrar(HABITACIONES[i % HABITACIONES.length], BASE.plusDays(600 + i * 3L));
                } catch (RuntimeException e) {
                }
                i++;
            }
        });

        Thread lectorEndpoint = new Thread(() -> {
            while (seguir.get()) {
                try {
                    int estado = mockMvc.perform(get("/api/reservas")).andReturn()
                            .getResponse().getStatus();
                    if (estado == 200) {
                        http200.incrementAndGet();
                    } else {
                        http500.incrementAndGet();
                    }
                } catch (Exception e) {
                    http500.incrementAndGet();
                }
            }
        });

        List<Thread> lectores = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Thread t = new Thread(() -> {
                while (seguir.get()) {
                    try {
                        for (Reserva r : dataStore.getReservas()) {
                            r.getCodigoReserva();
                        }
                        recorridosOk.incrementAndGet();
                    } catch (ConcurrentModificationException e) {
                        cme.incrementAndGet();
                    }
                }
            });
            lectores.add(t);
        }

        try {
            escritor.start();
            lectorEndpoint.start();
            for (Thread t : lectores) {
                t.start();
            }
            Thread.sleep(4000);
        } finally {
            seguir.set(false);
        }

        escritor.join();
        lectorEndpoint.join();
        for (Thread t : lectores) {
            t.join();
        }

        System.out.println(">>> Recorridos de la lista sin error: " + recorridosOk.get());
        System.out.println(">>> ConcurrentModificationException:  " + cme.get());
        System.out.println(">>> GET /api/reservas -> 200:       " + http200.get());
        System.out.println(">>> GET /api/reservas -> 500:       " + http500.get());

        assertThat(cme.get()).as("recorrer la lista no debe lanzar ConcurrentModificationException").isZero();
        assertThat(http500.get()).as("consultar reservas no debe devolver 500").isZero();
        assertThat(http200.get()).as("debe haberse consultado reservas al menos una vez").isPositive();
    }

    @Test
    @DisplayName("Las reservas registradas simultáneamente no se duplican ni se pierden")
    void registrarSimultaneamente_noDuplicaNIpierde() throws Exception {
        int peticiones = 20;
        AtomicBoolean seguir = new AtomicBoolean(true);
        AtomicInteger creadas = new AtomicInteger();
        List<Thread> hilos = new ArrayList<>();

        for (int i = 0; i < peticiones; i++) {
            final int numero = i;
            Thread t = new Thread(() -> {
                while (seguir.get()) {
                    try {
                        registrar(HABITACIONES[numero % HABITACIONES.length],
                                BASE.plusDays(900 + numero * 5L));
                        creadas.incrementAndGet();
                    } catch (RuntimeException e) {
                    }
                }
            });
            hilos.add(t);
        }

        try {
            for (Thread t : hilos) {
                t.start();
            }
            Thread.sleep(3000);
        } finally {
            seguir.set(false);
        }
        for (Thread t : hilos) {
            t.join();
        }

        Set<String> codigos = new HashSet<>();
        for (Reserva r : dataStore.getReservas()) {
            codigos.add(r.getCodigoReserva());
        }

        System.out.println(">>> Reservas creadas: " + creadas.get());
        System.out.println(">>> Reservas almacenadas: " + dataStore.getReservas().size());
        System.out.println(">>> Codigos distintos: " + codigos.size());

        assertThat(dataStore.getReservas())
                .as("no debe haber reservas perdidas ni duplicadas")
                .hasSize(creadas.get());
        assertThat(codigos)
                .as("cada reserva debe tener un codigo unico")
                .hasSize(dataStore.getReservas().size());
    }

    @Test
    @DisplayName("Los demas servicios siguen respondiendo sin errores bajo concurrencia")
    void demasServicios_sinErroresBajoConcurrencia() throws Exception {
        AtomicBoolean seguir = new AtomicBoolean(true);
        AtomicInteger errores = new AtomicInteger();
        List<Thread> hilos = new ArrayList<>();

        String[] rutas = {
                "/api/habitaciones", "/api/habitaciones/disponibles", "/api/huespedes",
                "/api/estadias", "/api/estadias/en-curso", "/api/pagos",
                "/api/usuarios", "/api/dashboard/stats", "/api/reservas"
        };

        for (String ruta : rutas) {
            Thread t = new Thread(() -> {
                while (seguir.get()) {
                    try {
                        int estado = mockMvc.perform(get(ruta)).andReturn().getResponse().getStatus();
                        if (estado != 200) {
                            errores.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errores.incrementAndGet();
                    }
                }
            });
            hilos.add(t);
        }

        Thread escritor = new Thread(() -> {
            int i = 0;
            while (seguir.get() && i < 1500) {
                try {
                    registrar(HABITACIONES[i % HABITACIONES.length], BASE.plusDays(1200 + i * 3L));
                } catch (RuntimeException e) {
                    // Ignorado.
                }
                i++;
            }
        });

        try {
            for (Thread t : hilos) {
                t.start();
            }
            escritor.start();
            Thread.sleep(3000);
        } finally {
            seguir.set(false);
        }

        escritor.join();
        for (Thread t : hilos) {
            t.join();
        }

        System.out.println(">>> Respuestas no-200 en los demas servicios: " + errores.get());
        assertThat(errores.get()).as("todos los endpoints de lectura deben responder 200").isZero();
    }

    private void registrar(int numero, LocalDate inicio) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.setIdHuesped("H-001");
        dto.setNumeroHabitacion(numero);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(inicio.plusDays(2));
        dto.setAdultos(2);
        dto.setNinos(0);
        reservaService.crearReserva(dto);
    }

}