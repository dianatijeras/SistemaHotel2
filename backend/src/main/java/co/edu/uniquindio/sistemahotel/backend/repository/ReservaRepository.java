package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Reserva;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de reservas. Define las operaciones de persistencia y consulta que el servicio de reservas necesita.
 * La implementación concreta puede usar cualquier mecanismo de almacenamiento (memoria, base de datos, etc.).
 */
public interface ReservaRepository {

    /**
     * Persiste una reserva nueva.
     * @param reserva
     * @return la misma reserva persistida
     */
    Reserva guardar(Reserva reserva);

    /**
     * Busca una reserva por su identificador.
     * @param idReserva
     * @return la reserva encontrada o un Optional vacío
     */
    Optional<Reserva> buscarPorId(String idReserva);

    /**
     * Devuelve todas las reservas almacenadas. Las instancias devueltas son las mismas objetos que se guardaron, no copias.
     * @return la colección de reservas almacenadas
     */
    List<Reserva> buscarTodas();

    /**
     * Devuelve las reservas que pertenecen a un huésped. El filtrado por un atributo propio de la
     * reserva es una consulta.
     * @param idHuesped
     * @return las reservas de ese huésped
     */
    List<Reserva> buscarPorHuesped(String idHuesped);

    /**
     * Indica si ya existe una reserva con ese código o con ese identificador
     * @param codigo
     * @return true si ya existe una reserva con ese valor
     */
    boolean existePorCodigoOId(String codigo);

    /**
     * Cuenta las reservas almacenadas
     * @param idHuesped
     * @return cuántas reservas hay almacenadas
     */
    int contarTodas();

    /**
     * Persiste los cambios de una reserva ya existente (fechas, habitación o estado).
     * @param reserva
     * @return la misma reserva persistida
     */
    Reserva actualizar(Reserva reserva);
}
