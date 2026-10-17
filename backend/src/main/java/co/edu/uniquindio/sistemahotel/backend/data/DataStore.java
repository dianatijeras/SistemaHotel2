package co.edu.uniquindio.sistemahotel.backend.data;

import co.edu.uniquindio.sistemahotel.backend.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clase que actúa como un almacén de datos en memoria para el sistema de gestión hotelera.
 * Contiene listas para almacenar información sobre habitaciones, categorías, huéspedes, recepcionistas,
 * administradores, personal de limpieza, reservas, estadías y pagos.
 */
@Component
public class DataStore {

    /**
     * Listas de datos para almacenar información sobre las diferentes entidades del sistema.
     * Se utilizan listas de tipo CopyOnWriteArrayList para garantizar la seguridad en entornos concurrentes,
     * permitiendo lecturas y escrituras sin necesidad de sincronización explícita.
     */
    private final List<Habitacion> habitaciones   = new CopyOnWriteArrayList<>();
    private final List<CategoriaHabitacion> categorias  = new CopyOnWriteArrayList<>();
    private final List<Huesped>          huespedes      = new CopyOnWriteArrayList<>();
    private final List<Recepcionista>    recepcionistas = new CopyOnWriteArrayList<>();
    private final List<Administrador>    administradores= new CopyOnWriteArrayList<>();
    private final List<PersonalLimpieza> personalLimpieza = new CopyOnWriteArrayList<>();
    private final List<Reserva>          reservas       = new CopyOnWriteArrayList<>();
    private final List<Estadia>          estadias       = new CopyOnWriteArrayList<>();
    private final List<Pago>             pagos          = new CopyOnWriteArrayList<>();

    /**
     * Métodos getters para acceder a las listas de datos.
     * Estos métodos permiten obtener las listas completas de cada entidad, facilitando la manipulación y
     * consulta de los datos almacenados en el DataStore.
     * @return
     */
    public List<Habitacion>        getHabitaciones()    { return habitaciones; }
    public List<CategoriaHabitacion> getCategorias()    { return categorias; }
    public List<Huesped>           getHuespedes()       { return huespedes; }
    public List<Recepcionista>     getRecepcionistas()  { return recepcionistas; }
    public List<Administrador>     getAdministradores() { return administradores; }
    public List<PersonalLimpieza>  getPersonalLimpieza(){ return personalLimpieza; }
    public List<Reserva>           getReservas()        { return reservas; }
    public List<Estadia>           getEstadias()        { return estadias; }
    public List<Pago>              getPagos()           { return pagos; }
}
