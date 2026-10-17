package co.edu.uniquindio.sistemahotel.backend.model;
import co.edu.uniquindio.sistemahotel.backend.enums.Rol;


/**
 * El personal de limpieza del hotel es un usuario con acceso al sistema, pero con privilegios limitados.
 */
public class PersonalLimpieza extends UsuarioAcceso {

    private String idPersonalLimpieza;
    private String nombre;
    private String apellido;

    /**
     * @param idPersonalLimpieza
     * @param nombre
     * @param apellido
     * @param username
     * @param password
     */
    public PersonalLimpieza(String idPersonalLimpieza, String nombre, String apellido, String username, String password) {
        super(idPersonalLimpieza, username, password, Rol.PERSONAL_LIMPIEZA);
        this.idPersonalLimpieza = idPersonalLimpieza;
        this.nombre = nombre;
        this.apellido = apellido;
    }

    /**
     * Obtiene el identificador interno del personal de limpieza.
     * @return
     */
    public String getIdPersonalLimpieza() {
        return idPersonalLimpieza;
    }

    /**
     * Establece el identificador interno del personal de limpieza.
     * @param id
     */
    public void setIdPersonalLimpieza(String id) {
        this.idPersonalLimpieza = id;
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
