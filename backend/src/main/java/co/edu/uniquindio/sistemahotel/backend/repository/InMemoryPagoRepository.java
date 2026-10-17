package co.edu.uniquindio.sistemahotel.backend.repository;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de {@link PagoRepository} que almacena los pagos en memoria.
 */
@Repository
public class InMemoryPagoRepository implements PagoRepository {

    private final DataStore dataStore;

    /**
     * @param dataStore
     */
    public InMemoryPagoRepository(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Acceso directo a la colección viva compartida.
     * @return la misma lista de pagos que usa el resto del sistema
     */
    private List<Pago> pagos() {
        return dataStore.getPagos();
    }

    /**
     * Devuelve la colección viva, no una copia, para no cambiar el comportamiento de los endpoints
     * y de las pruebas que ya dependen de ver las inserciones al instante.
     * @return la lista de pagos almacenados
     */
    @Override
    public List<Pago> buscarTodos() {
        return pagos();
    }

    @Override
    public Pago guardar(Pago pago) {
        pagos().add(pago);
        return pago;
    }

    @Override
    public Optional<Pago> buscarPorId(String idPago) {
        return pagos().stream()
                .filter(p -> p.getIdPago().equals(idPago))
                .findFirst();
    }

    /**
     * El filtro replica exactamente el que tenía el servicio, incluida su semántica con una estadía nula
     * @param idEstadia
     * @return los pagos de esa estadía, en orden de registro
     */
    @Override
    public List<Pago> buscarPorEstadia(String idEstadia) {
        return pagos().stream()
                .filter(p -> p.getEstadia().getIdEstadia().equals(idEstadia))
                .collect(Collectors.toList());
    }

    /**
     * Sustituye el pago en su posición original. Como la instancia recibida es la misma que ya está
     * en la lista, en memoria el efecto visible es nulo, pero el contrato de la interfaz exige que se devuelva el mismo pago.
     * @param pago pago existente ya transitado
     * @return el mismo pago
     */
    @Override
    public Pago actualizar(Pago pago) {
        List<Pago> pagos = pagos();
        for (int i = 0; i < pagos.size(); i++) {
            if (pagos.get(i).getIdPago().equals(pago.getIdPago())) {
                pagos.set(i, pago);
                return pago;
            }
        }
        throw new IllegalArgumentException("No existe el pago con ID: " + pago.getIdPago());
    }
}
