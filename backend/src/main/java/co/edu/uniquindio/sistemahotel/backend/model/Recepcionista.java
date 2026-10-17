package co.edu.uniquindio.sistemahotel.backend.model;


import co.edu.uniquindio.sistemahotel.backend.enums.Rol;
import co.edu.uniquindio.sistemahotel.backend.enums.TurnoRecepcionista;

/**
 * La recepcionista del hotel. Es el único tipo de personal con un atributo propio: el turno.
 */
public class Recepcionista extends UsuarioAcceso {

    private String idRecepcionista;
    private String nombre;
    private String apellido;
    private TurnoRecepcionista turno;

    /**
     * @param idRecepcionista
     * @param nombre
     * @param apellido
     * @param turno
     * @param username
     * @param password
     */
    public Recepcionista(String idRecepcionista, String nombre, String apellido, TurnoRecepcionista turno, String username, String password) {
        super(idRecepcionista, username, password, Rol.RECEPCIONISTA);
        this.idRecepcionista = idRecepcionista;
        this.nombre = nombre;
        this.apellido = apellido;
        this.turno = turno;
    }

    /**
     * Obtiene el identificador interno del recepcionista.
     * @return
     */
    public String getIdRecepcionista() {
        return idRecepcionista;
    }

    /**
     * Establece el identificador interno del recepcionista.
     * @param id
     */
    public void setIdRecepcionista(String id) {
        this.idRecepcionista = id;
    }

    /**
     * Obtiene el turno asignado al recepcionista.
     * @return
     */
    public TurnoRecepcionista getTurno() {
        return turno;
    }

    /**
     * Establece el turno asignado al recepcionista.
     * @param turno
     */
    public void setTurno(TurnoRecepcionista turno) {
        this.turno = turno;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String getApellido() {
        return apellido;
    }

    @Override
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    @Override
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
