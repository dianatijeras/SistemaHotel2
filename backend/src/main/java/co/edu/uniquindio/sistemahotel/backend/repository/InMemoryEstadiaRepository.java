package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de {@link EstadiaRepository} que almacena las estadías en memoria.
 */
@Repository
public class InMemoryEstadiaRepository implements EstadiaRepository {

    private final DataStore dataStore;

    /**
     * @param dataStore almacén en memoria compartido por todo el sistema
     */
    public InMemoryEstadiaRepository(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Acceso directo a la colección viva compartida. No se copia
     * @return la misma lista de estadías que usa el resto del sistema
     */
    private List<Estadia> estadias() {
        return dataStore.getEstadias();
    }

    /**
     * Devuelve la colección viva, no una copia, para no cambiar el comportamiento de los endpoints
     * y de las pruebas que ya dependen de ver las inserciones al instante.
     * @return la lista de estadías almacenadas
     */
    @Override
    public List<Estadia> buscarTodas() {
        return estadias();
    }

    @Override
    public Estadia guardar(Estadia estadia) {
        estadias().add(estadia);
        return estadia;
    }

    @Override
    public Optional<Estadia> buscarPorId(String idEstadia) {
        return estadias().stream()
                .filter(e -> e.getIdEstadia().equals(idEstadia))
                .findFirst();
    }

    @Override
    public List<Estadia> buscarPorEstado(EstadoEstadia estado) {
        return estadias().stream()
                .filter(e -> e.getEstadoEstadia() == estado)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza una estadía existente en la lista de estadías.
     * Si no se encuentra la estadía con el mismo ID, lanza una excepción.
     * @param estadia estadía existente ya modificada
     * @return la misma estadía
     */
    @Override
    public Estadia actualizar(Estadia estadia) {
        List<Estadia> estadias = estadias();
        for (int i = 0; i < estadias.size(); i++) {
            if (estadias.get(i).getIdEstadia().equals(estadia.getIdEstadia())) {
                estadias.set(i, estadia);
                return estadia;
            }
        }
        throw new IllegalArgumentException("No existe la estadía con ID: " + estadia.getIdEstadia());
    }

}
