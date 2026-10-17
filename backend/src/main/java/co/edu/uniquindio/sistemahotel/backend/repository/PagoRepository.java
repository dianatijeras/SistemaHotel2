package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.model.Pago;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de pagos. La persistencia de pagos es responsabilidad del servicio de pagos, que
 * delega en este repositorio la tarea de almacenar y recuperar los pagos.
 */
public interface PagoRepository {

    /**
     * Persiste un pago nuevo. El pago nace siempre en estado {@code PENDIENTE}
     * @param pago pago ya construido y validado por el servicio
     * @return el mismo pago persistido
     */
    Pago guardar(Pago pago);

    /**
     * Busca un pago por su identificador
     * @param idPago identificador del pago
     * @return el pago encontrado o un Optional vacío
     */
    Optional<Pago> buscarPorId(String idPago);

    /**
     * Devuelve todos los pagos registrados, incluidos los rechazados y los reembolsados.
     * @return la colección de pagos almacenados
     */
    List<Pago> buscarTodos();

    /**
     * Devuelve los pagos de una estadía, en orden de registro. Un pago pertenece a una estadía por
     * la referencia que ya mantiene
     * @param idEstadia
     * @return los pagos de esa estadía
     */
    List<Pago> buscarPorEstadia(String idEstadia);

    /**
     * Persiste los cambios de un pago ya existente. Lo que cambia es siempre el estado
     * (aprobado, rechazado o reembolsado)
     * @param pago pago existente ya transitado
     * @return el mismo pago persistido
     */
    Pago actualizar(Pago pago);
}
