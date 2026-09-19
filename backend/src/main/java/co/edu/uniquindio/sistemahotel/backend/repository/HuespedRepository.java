package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Huesped;

import java.util.List;
import java.util.Optional;

public interface HuespedRepository {

    List<Huesped> findAll();

    Optional<Huesped> findById(String idHuesped);

    Huesped save(Huesped huesped);
}
