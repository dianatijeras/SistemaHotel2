package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryHuespedRepository implements HuespedRepository {

    private final Map<String, Huesped> huespedes = new ConcurrentHashMap<>();

    @Override
    public List<Huesped> findAll() {
        return List.copyOf(huespedes.values());
    }

    @Override
    public Optional<Huesped> findById(String idHuesped) {
        return Optional.ofNullable(huespedes.get(idHuesped));
    }

    @Override
    public Huesped save(Huesped huesped) {
        huespedes.put(huesped.getIdHuesped(), huesped);
        return huesped;
    }
}
