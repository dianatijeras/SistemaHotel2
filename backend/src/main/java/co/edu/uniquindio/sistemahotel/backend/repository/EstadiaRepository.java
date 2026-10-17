package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoEstadia;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz que define las operaciones de persistencia para las estadías en el sistema de gestión hotelera.
 */
public interface EstadiaRepository {

    /**
     * Persiste una estadía nueva, normalmente creada por el check-in.
     * @param estadia estadía ya construida, iniciada y validada por el servicio
     * @return la misma estadía persistida
     */
    Estadia guardar(Estadia estadia);

    /**
     * Busca una estadía por su identificador.
     * @param idEstadia
     * @return la estadía encontrada o un Optional vacío
     */
    Optional<Estadia> buscarPorId(String idEstadia);

    /**
     * Devuelve todas las estadías almacenadas, en el orden en que se insertaron.
     * @return la colección de estadías almacenadas
     */
    List<Estadia> buscarTodas();

    /**
     * Devuelve las estadías que están en un estado concreto. El estado se recibe como parámetro en
     * lugar de declarar un método por estado
     * @param estado estado de la estadía
     * @return las estadías en ese estado
     */
    List<Estadia> buscarPorEstado(EstadoEstadia estado);

    /**
     * Persiste los cambios de una estadía ya existente: su estado, sus consumos o sus totales.
     * @param estadia estadía existente con los cambios ya aplicados
     * @return la misma estadía persistida
     */
    Estadia actualizar(Estadia estadia);
}
