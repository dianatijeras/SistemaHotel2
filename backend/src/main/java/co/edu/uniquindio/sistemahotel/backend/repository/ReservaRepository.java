package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Reserva;

import java.util.List;
import java.util.Optional;

public interface ReservaRepository {

    List<Reserva> findAll();

    Optional<Reserva> findById(String idReserva);

    List<Reserva> findByNumeroHabitacion(int numeroHabitacion);

    List<Reserva> findByIdHuesped(String idHuesped);

    Reserva save(Reserva reserva);
}
