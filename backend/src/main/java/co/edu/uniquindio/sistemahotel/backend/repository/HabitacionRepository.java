package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de habitaciones. Se encarga de almacenar y recuperar las habitaciones del hotel.
 * Se trata de un repositorio de objetos, no de datos: no se expone la estructura de almacenamiento ni
 * se devuelven copias de los objetos, sino referencias a los mismos.
 */
public interface HabitacionRepository {

    /**
     * Devuelve el inventario completo de habitaciones. Las instancias devueltas son las mismas
     * objetos que las reservas y las estadías ya tienen referenciadas, nunca copias
     * @return la colección de habitaciones almacenadas
     */
    List<Habitacion> buscarTodos();

    /**
     * Busca una habitación por su número, que es su identificador natural en el hotel.
     * @param numeroHabitacion
     * @return la habitación encontrada o un Optional vacío
     */
    Optional<Habitacion> buscarPorNumero(int numeroHabitacion);

    /**
     * Busca las habitaciones de un tipo concreto.
     * @param tipo tipo de habitación
     * @return las habitaciones de ese tipo
     */
    List<Habitacion> buscarPorTipo(TipoHabitacion tipo);

    /**
     * Busca las habitaciones que están en un estado concreto.
     * @param estado estado de la habitación
     * @return las habitaciones en ese estado
     */
    List<Habitacion> buscarPorEstado(EstadoHabitacion estado);

    /**
     * Persiste los cambios de una habitación ya existente, en particular su estado físico.
     * @param habitacion habitación existente con los cambios ya aplicados
     * @return la misma habitación persistida
     */
    Habitacion actualizar(Habitacion habitacion);
}
