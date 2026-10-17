package co.edu.uniquindio.sistemahotel.backend.data;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.TurnoRecepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.*;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Clase encargada de inicializar los datos del sistema de gestión hotelera al iniciar la aplicación.
 * Carga categorías de habitaciones, habitaciones, usuarios (huéspedes, recepcionistas, administradores y personal de limpieza) en el DataStore.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final DataStore dataStore;

    /**
     * Constructor que recibe el DataStore para cargar los datos iniciales.
     * @param dataStore
     */
    public DataInitializer(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Método que se ejecuta al iniciar la aplicación para cargar los datos iniciales en el DataStore.
     * @param args
     */
    @Override
    public void run(String... args) {
        cargarCategorias();
        cargarHabitaciones();
        cargarUsuarios();
        System.out.println(" Datos iniciales cargados correctamente.");
        System.out.println("   Habitaciones : " + dataStore.getHabitaciones().size());
        System.out.println("   Huéspedes    : " + dataStore.getHuespedes().size());
        System.out.println("   Recepcionistas: " + dataStore.getRecepcionistas().size());
    }

    /**
     * Método para cargar las categorías de habitaciones en el DataStore.
     * Se crean varias categorías con diferentes características y precios.
     */
    private void cargarCategorias() {
        dataStore.getCategorias().add(new CategoriaHabitacion(
                "CAT-01", "Estándar Simple", "Habitación básica con cama doble",
                1, 2, 150_000.0, TipoHabitacion.ESTANDAR));
        dataStore.getCategorias().add(new CategoriaHabitacion(
                "CAT-02", "Superior", "Habitación con vista y amenidades premium",
                1, 2, 220_000.0, TipoHabitacion.SUPERIOR));
        dataStore.getCategorias().add(new CategoriaHabitacion(
                "CAT-03", "Deluxe", "Habitación amplia con jacuzzi",
                1, 3, 350_000.0, TipoHabitacion.DELUXE));
        dataStore.getCategorias().add(new CategoriaHabitacion(
                "CAT-04", "Suite Presidencial", "Suite de lujo con sala y comedor",
                2, 4, 600_000.0, TipoHabitacion.SUITE));
    }

    /**
     * Método para cargar las habitaciones en el DataStore.
     * Se crean varias habitaciones asignándoles una categoría y características específicas como
     * número, tipo, precio, piso, descripción y capacidad.
     */
    private void cargarHabitaciones() {
        CategoriaHabitacion estandar = dataStore.getCategorias().get(0);
        CategoriaHabitacion superior = dataStore.getCategorias().get(1);
        CategoriaHabitacion deluxe   = dataStore.getCategorias().get(2);
        CategoriaHabitacion suite    = dataStore.getCategorias().get(3);

        dataStore.getHabitaciones().add(new Habitacion(101, TipoHabitacion.ESTANDAR, 150_000, 1, "Estándar, cama doble",    2, estandar));
        dataStore.getHabitaciones().add(new Habitacion(102, TipoHabitacion.ESTANDAR, 150_000, 1, "Estándar, dos camas",      2, estandar));
        dataStore.getHabitaciones().add(new Habitacion(103, TipoHabitacion.ESTANDAR, 150_000, 1, "Estándar, vista jardín",   2, estandar));
        dataStore.getHabitaciones().add(new Habitacion(201, TipoHabitacion.SUPERIOR, 220_000, 2, "Superior con balcón",      2, superior));
        dataStore.getHabitaciones().add(new Habitacion(202, TipoHabitacion.SUPERIOR, 220_000, 2, "Superior vista piscina",   2, superior));
        dataStore.getHabitaciones().add(new Habitacion(301, TipoHabitacion.DELUXE,   350_000, 3, "Deluxe jacuzzi y terraza", 3, deluxe));
        dataStore.getHabitaciones().add(new Habitacion(302, TipoHabitacion.DELUXE,   350_000, 3, "Deluxe vista panorámica",  3, deluxe));
        dataStore.getHabitaciones().add(new Habitacion(401, TipoHabitacion.SUITE,    600_000, 4, "Suite presidencial",       4, suite));
    }

    /**
     * Método para cargar los usuarios en el DataStore.
     * Se crean varios usuarios de diferentes tipos (huéspedes, recepcionistas, administradores y personal de limpieza)
     * con sus respectivos datos de identificación, contacto y credenciales de acceso.
     */
    private void cargarUsuarios() {
        dataStore.getHuespedes().add(new Huesped("H-001", "Carlos",  "Ramírez", "CC10000001", "3001234567", "carlos@email.com",  "carlos.r", "pass123"));
        dataStore.getHuespedes().add(new Huesped("H-002", "Laura",   "Gómez",   "CC10000002", "3109876543", "laura@email.com",   "laura.g",  "pass123"));
        dataStore.getHuespedes().add(new Huesped("H-003", "Andrés",  "Torres",  "CC10000003", "3204567890", "andres@email.com",  "andres.t", "pass123"));

        dataStore.getRecepcionistas().add(new Recepcionista("R-001", "María", "López",   TurnoRecepcionista.MANANA, "maria.l",  "admin123"));
        dataStore.getRecepcionistas().add(new Recepcionista("R-002", "Pedro", "Herrera", TurnoRecepcionista.NOCHE,  "pedro.h",  "admin123"));

        dataStore.getAdministradores().add(new Administrador("A-001", "Admin", "Principal", "admin", "admin2024"));

        dataStore.getPersonalLimpieza().add(new PersonalLimpieza("PL-001", "Rosa",  "Medina", "rosa.m",  "limpieza1"));
        dataStore.getPersonalLimpieza().add(new PersonalLimpieza("PL-002", "Jorge", "Ríos",   "jorge.r", "limpieza1"));
    }
}
