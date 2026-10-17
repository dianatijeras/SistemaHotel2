package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementación de repositorio de habitaciones en memoria.
 */
@Repository
public class InMemoryHabitacionRepository implements HabitacionRepository {

    private final DataStore dataStore;

    /**
     * @param dataStore almacén en memoria compartido por todo el sistema
     */
    public InMemoryHabitacionRepository(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Acceso directo a la colección viva compartida.
     * @return la misma lista de habitaciones que usa el resto del sistema
     */
    private List<Habitacion> habitaciones() {
        return dataStore.getHabitaciones();
    }

    /**
     * Devuelve la colección viva, no una copia, para no cambiar el comportamiento de los endpoints
     * y de las pruebas que ya dependen de ver los cambios al instante.
     * @return la lista de habitaciones almacenadas
     */
    @Override
    public List<Habitacion> buscarTodos() {
        return habitaciones();
    }

    @Override
    public Optional<Habitacion> buscarPorNumero(int numeroHabitacion) {
        return habitaciones().stream()
                .filter(h -> h.getNumeroHabitacion() == numeroHabitacion)
                .findFirst();
    }

    @Override
    public List<Habitacion> buscarPorTipo(TipoHabitacion tipo) {
        return habitaciones().stream()
                .filter(h -> h.getTipoHabitacion() == tipo)
                .collect(Collectors.toList());
    }

    @Override
    public List<Habitacion> buscarPorEstado(EstadoHabitacion estado) {
        return habitaciones().stream()
                .filter(h -> h.getEstadoHabitacion() == estado)
                .collect(Collectors.toList());
    }

    /**
     * Sustituye la habitación en su posición original. Como la instancia recibida es la misma que
     * ya está en la lista, en memoria el efecto visible es nulo.
     * @param habitacion habitación existente ya modificada
     * @return la misma habitación
     */
    @Override
    public Habitacion actualizar(Habitacion habitacion) {
        List<Habitacion> habitaciones = habitaciones();
        for (int i = 0; i < habitaciones.size(); i++) {
            if (habitaciones.get(i).getNumeroHabitacion() == habitacion.getNumeroHabitacion()) {
                habitaciones.set(i, habitacion);
                return habitacion;
            }
        }
        throw new IllegalArgumentException("No existe la habitación número: " + habitacion.getNumeroHabitacion());
    }
}
