package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Administrador;
import co.edu.uniquindio.sistemahotel.backend.model.PersonalLimpieza;
import co.edu.uniquindio.sistemahotel.backend.model.Recepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link UsuarioRepository} que almacena los datos en memoria, en listas
 * separadas por tipo de {@link Usuario}
 */
@Repository
public class InMemoryUsuarioRepository implements UsuarioRepository {

    private final DataStore dataStore;

    /**
     * @param dataStore almacén en memoria compartido por todo el sistema
     */
    public InMemoryUsuarioRepository(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Obtiene la lista completa de personal, sin importar su tipo concreto.
     * @return
     */
    private List<Usuario> personal() {
        return buscarTodos();
    }

    @Override
    public boolean existePorUsername(String username) {
        return personal().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    @Override
    public Optional<Usuario> buscarPorId(String idUsuario) {
        return personal().stream()
                .filter(u -> idUsuario.equals(u.getIdUsuario()))
                .findFirst();
    }

    /**
     * Los tres tipos de personal, en el orden en que los presenta el sistema.
     * Se construye una lista nueva porque el orden importa en el listado, y porque los
     * iteradores de {@code CopyOnWriteArrayList} son instantáneas que no se pueden componer entre sí.
     */
    @Override
    public List<Usuario> buscarTodos() {
        List<Usuario> todos = new ArrayList<>();
        dataStore.getAdministradores().forEach(todos::add);
        dataStore.getRecepcionistas().forEach(todos::add);
        dataStore.getPersonalLimpieza().forEach(todos::add);
        return todos;
    }

    /**
     * Coloca al empleado en la lista que corresponde a su clase concreta.
     * Es el unico punto donde el repositorio decide «en qué lista va».
     */
    @Override
    public Usuario guardar(Usuario usuario) {
        if (usuario instanceof Administrador) {
            dataStore.getAdministradores().add((Administrador) usuario);
        } else if (usuario instanceof Recepcionista) {
            dataStore.getRecepcionistas().add((Recepcionista) usuario);
        } else if (usuario instanceof PersonalLimpieza) {
            dataStore.getPersonalLimpieza().add((PersonalLimpieza) usuario);
        } else {
            throw new IllegalArgumentException(
                    "No se puede guardar un usuario de tipo " + usuario.getClass().getSimpleName());
        }
        return usuario;
    }
}
