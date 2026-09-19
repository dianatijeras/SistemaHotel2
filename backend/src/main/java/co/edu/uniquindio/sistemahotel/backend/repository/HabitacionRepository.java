package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

import java.util.List;
import java.util.Optional;

public interface HabitacionRepository {

    List<Habitacion> findAll();

    Optional<Habitacion> findByNumero(int numeroHabitacion);

    boolean existsByNumero(int numeroHabitacion);

    Habitacion save(Habitacion habitacion);
}
