package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Huesped;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz que define las operaciones de persistencia para los huéspedes en el sistema de gestión hotelera.
 */
public interface HuespedRepository {

    /**
     * Persiste un huésped nuevo en el almacenamiento.
     * @param huesped huésped ya construido y validado por el servicio
     * @return el mismo huesped
     */
    Huesped guardar(Huesped huesped);

    /**
     * Busca un huésped por su identificador interno.
     * @param idHuesped
     * @return el huésped encontrado o un Optional vacío
     */
    Optional<Huesped> buscarPorId(String idHuesped);

    /**
     * Busca un huésped por su documento de identidad.
     * @param documento documento del huésped
     * @return el huésped que tiene ese documento o un Optional vacío
     */
    Optional<Huesped> buscarPorDocumento(String documento);

    /**
     * Devuelve el conjunto de huéspedes almacenados.
     * @return la colección de huéspedes almacenados
     */
    List<Huesped> buscarTodos();

    /**
     * Verifica si ya existe un huésped con el mismo documento de identidad.
     * @param username
     * @return true si algún huésped ya lo usa
     */
    boolean existePorUsername(String username);

    /**
     * Persiste los cambios de un huésped ya existente.
     * @param huesped huésped existente con los campos ya actualizados
     * @return el mismo huésped
     */
    Huesped actualizar(Huesped huesped);

    /**
     * Elimina un huésped por su identificador interno.
     * @param idHuesped
     * @return true si se eliminó, false si no existía ningún huésped con ese identificador
     */
    boolean eliminarPorId(String idHuesped);
}
