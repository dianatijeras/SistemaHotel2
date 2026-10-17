package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementación en memoria de la interfaz ReservaRepository.
 */
@Repository
public class InMemoryReservaRepository implements ReservaRepository {

    private final DataStore dataStore;

    /**
     * @param dataStore
     */
    public InMemoryReservaRepository(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Acceso directo a la colección viva compartida. No se copia: ver la nota de la clase.
     * @return la misma lista de reservas que usa el resto del sistema
     */
    private List<Reserva> reservas() {
        return dataStore.getReservas();
    }

    /**
     * Devuelve la colección viva, no una copia, para no cambiar el comportamiento de los endpoints
     * y de las pruebas que ya dependen de ver las inserciones al instante.
     * @return la lista de reservas almacenadas
     */
    @Override
    public List<Reserva> buscarTodas() {
        return reservas();
    }

    @Override
    public Reserva guardar(Reserva reserva) {
        reservas().add(reserva);
        return reserva;
    }

    @Override
    public Optional<Reserva> buscarPorId(String idReserva) {
        return reservas().stream()
                .filter(r -> r.getIdReserva().equals(idReserva))
                .findFirst();
    }

    @Override
    public List<Reserva> buscarPorHuesped(String idHuesped) {
        return reservas().stream()
                .filter(r -> r.getHuesped().getIdHuesped().equals(idHuesped))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existePorCodigoOId(String codigo) {
        return reservas().stream()
                .anyMatch(r -> codigo.equals(r.getCodigoReserva()) || codigo.equals(r.getIdReserva()));
    }

    @Override
    public int contarTodas() {
        return reservas().size();
    }

    /**
     * Sustituye la reserva en su posición original. Como la instancia recibida es la misma que ya
     * está en la lista, en memoria el efecto visible es nulo, pero es necesario para que el repositorio
     * cumpla con la interfaz y para que los tests de integración
     * @param reserva
     * @return la misma reserva
     */
    @Override
    public Reserva actualizar(Reserva reserva) {
        List<Reserva> reservas = reservas();
        for (int i = 0; i < reservas.size(); i++) {
            if (reservas.get(i).getIdReserva().equals(reserva.getIdReserva())) {
                reservas.set(i, reserva);
                return reserva;
            }
        }
        throw new IllegalArgumentException("No existe la reserva con ID: " + reserva.getIdReserva());
    }
}
