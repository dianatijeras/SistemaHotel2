package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryReservaRepository
        implements ReservaRepository {

    private final Map<String, Reserva> reservas = new ConcurrentHashMap<>();


    @Override
    public List<Reserva> findByNumeroHabitacion(int numeroHabitacion) {
        return reservas.values()
                .stream()
                .filter(r -> r.getHabitacion().getNumeroHabitacion() == numeroHabitacion)
                .toList();
    }


    @Override
    public Reserva save(Reserva reserva) {

        reservas.put(reserva.getIdReserva(), reserva);
        return reserva;
    }


    @Override
    public List<Reserva> findAll() {
        return List.copyOf(reservas.values());
    }


    @Override
    public Optional<Reserva> findById(String idReserva) {

        return Optional.ofNullable(reservas.get(idReserva));
    }


    @Override
    public List<Reserva> findByIdHuesped(String idHuesped) {

        return reservas.values()
                .stream()
                .filter(r ->
                        r.getHuesped()
                                .getIdHuesped()
                                .equals(idHuesped)
                )
                .toList();
    }
}
