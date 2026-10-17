package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación de repositorio de huéspedes en memoria.
 * No es un repositorio "real", sino un almacén de datos compartido por todo el sistema para pruebas y desarrollo.
 * La lista de huéspedes es la misma que usan los servicios y controladores, de modo que cualquier cambio se refleja inmediatamente en todas las capas.
 */
@Repository
public class InMemoryHuespedRepository implements HuespedRepository {

    private final DataStore dataStore;

    /**
     * Se recibe el DataStore ya configurado por Spring, de modo que este repositorio y el resto
     * del sistema comparten exactamente la misma instancia y, por tanto, los mismos datos.
     * @param dataStore
     */
    public InMemoryHuespedRepository(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Acceso directo a la colección viva compartida.
     * @return la misma lista de huespedes que usa el resto del sistema
     */
    private List<Huesped> huespedes() {
        return dataStore.getHuespedes();
    }

    @Override
    public Huesped guardar(Huesped huesped) {
        huespedes().add(huesped);
        return huesped;
    }

    @Override
    public Optional<Huesped> buscarPorId(String idHuesped) {
        return huespedes().stream()
                .filter(h -> h.getIdHuesped().equals(idHuesped))
                .findFirst();
    }

    @Override
    public Optional<Huesped> buscarPorDocumento(String documento) {
        return huespedes().stream()
                .filter(h -> h.getDocumento() != null
                        && h.getDocumento().trim().equalsIgnoreCase(documento))
                .findFirst();
    }

    /**
     * Devuelve la colección viva, no una copia, para no cambiar el comportamiento de los endpoints
     * y de las pruebas que ya dependen de ver las inserciones al instante
     * @return la lista de huespedes almacenados
     */
    @Override
    public List<Huesped> buscarTodos() {
        return huespedes();
    }

    @Override
    public boolean existePorUsername(String username) {
        return huespedes().stream()
                .anyMatch(h -> h.getUsername().equalsIgnoreCase(username));
    }

    /**
     * Sustituye el huésped en su posición original. Si no existe, lanza IllegalArgumentException.
     * @param huesped
     * @return el mismo huesped
     */
    @Override
    public Huesped actualizar(Huesped huesped) {
        List<Huesped> huespedes = huespedes();
        for (int i = 0; i < huespedes.size(); i++) {
            if (huespedes.get(i).getIdHuesped().equals(huesped.getIdHuesped())) {
                huespedes.set(i, huesped);
                return huesped;
            }
        }
        throw new IllegalArgumentException("No existe el huésped con ID: " + huesped.getIdHuesped());
    }

    @Override
    public boolean eliminarPorId(String idHuesped) {
        Optional<Huesped> existente = buscarPorId(idHuesped);
        // No se usa removeIf: CopyOnWriteArrayList no admite eliminación a través del iterador.
        return existente.isPresent() && huespedes().remove(existente.get());
    }
}
