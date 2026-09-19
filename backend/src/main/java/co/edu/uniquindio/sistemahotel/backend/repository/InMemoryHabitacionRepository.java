package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Repository
public class InMemoryHabitacionRepository implements HabitacionRepository {

    private final Map<Integer, Habitacion> habitaciones = new ConcurrentHashMap<>();

    @Override
    public List<Habitacion> findAll() {
        return List.copyOf(habitaciones.values());
    }

    @Override
    public Optional<Habitacion> findByNumero(int numeroHabitacion) {
        return Optional.ofNullable(habitaciones.get(numeroHabitacion));
    }

    @Override
    public boolean existsByNumero(int numeroHabitacion) {
        return habitaciones.containsKey(numeroHabitacion);
    }

    @Override
    public Habitacion save(Habitacion habitacion) {
        habitaciones.put(habitacion.getNumeroHabitacion(), habitacion);
        return habitacion;
    }
}
