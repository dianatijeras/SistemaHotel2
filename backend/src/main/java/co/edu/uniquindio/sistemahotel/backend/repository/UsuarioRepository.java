package co.edu.uniquindio.sistemahotel.backend.repository;


import co.edu.uniquindio.sistemahotel.backend.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de miembros del personal del hotel: administradores, recepcionistas y personal de limpieza.
 */
public interface UsuarioRepository {

    /**
     * Comprueba si existe un miembro del personal con el nombre de acceso dado.
     * @param username
     * @return
     */
    boolean existePorUsername(String username);

    /**
     * Busca un miembro del personal por su identificador interno.
     * @param idUsuario
     * @return
     */
    Optional<Usuario> buscarPorId(String idUsuario);

    /**
     * Busca un miembro del personal por su nombre de acceso.
     * @return
     */
    List<Usuario> buscarTodos();


    /**
     * Guarda un miembro del personal en el repositorio.
     * Si ya existe un miembro con el mismo identificador interno, se sobrescribe.
     * @param usuario
     */
    Usuario guardar(Usuario usuario);
}
