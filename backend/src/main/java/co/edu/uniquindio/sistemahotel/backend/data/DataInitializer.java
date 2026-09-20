package co.edu.uniquindio.sistemahotel.backend.data;

import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.repository.HabitacionRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final HuespedRepository huespedRepository;
    private final HabitacionRepository habitacionRepository;

    public DataInitializer(HuespedRepository huespedRepository, HabitacionRepository habitacionRepository) {
        this.huespedRepository = huespedRepository;
        this.habitacionRepository = habitacionRepository;
    }

    @Override
    public void run(String... args) {

        huespedRepository.save(new Huesped("1001", "Maria", "Garcia", "1001001001", "3001234567", "maria.garcia@gmail.com"));
        huespedRepository.save(new Huesped("1002", "Carlos", "Rodriguez", "1002002002", "3012345678", "carlos.rodriguez@gmail.com"));
        huespedRepository.save(new Huesped("1003", "Ana", "Lopez", "1003003003", "3023456789", "ana.lopez@gmail.com"));
        huespedRepository.save(new Huesped("1004", "Juan", "Perez", "1004004004", "3034567890", "juan.perez@gmail.com"));
        huespedRepository.save(new Huesped("1005", "Laura", "Martinez", "1005005005", "3045678901", "laura.martinez@gmail.com"));

        habitacionRepository.save(new Habitacion(1, TipoHabitacion.ESTANDAR, 30000, 1, "Habitacion con una cama sencilla y baño", 1));
        habitacionRepository.save(new Habitacion(2, TipoHabitacion.ESTANDAR, 50000, 1, "Habitacion con 2 camas sencillas y baño", 2));
        habitacionRepository.save(new Habitacion(3, TipoHabitacion.DELUXE, 100000, 2, "Habitacion con 2 camas matrimoniales y baño", 4));
        habitacionRepository.save(new Habitacion(4, TipoHabitacion.SUITE, 200000, 3, "Habitacion con 2 camas matrimoniales, baño y sala ", 4));


    }
}
