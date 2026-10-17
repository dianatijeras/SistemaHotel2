package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.Rol;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Usuario {

    protected String idUsuario;
    protected String username;
    protected String password;
    protected Rol rol;
    protected boolean activo;

    /**
     * Constructor protegido para inicializar un usuario.
     * @param idUsuario
     * @param username
     * @param password
     * @param rol
     */
    protected Usuario(String idUsuario, String username, String password, Rol rol) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.password = password;
        this.rol = rol;
        this.activo = true;
    }

    /**
     * Obtiene el identificador interno del registro.
     * @return
     */
    public String getIdUsuario() {
        return idUsuario;
    }

    /**
     * Establece el identificador interno del registro.
     * @param idUsuario
     */
    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    /**
     * Obtiene el nombre de acceso.
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * Establece el nombre de acceso.
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Obtiene la contraseña almacenada.
     * @return
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getPassword() {
        return password;
    }

    /**
     * Establece la contraseña.
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Obtiene el rol, que es fijo desde el constructor.
     * @return
     */
    public Rol getRol() {
        return rol;
    }

    /**
     * Indica si el registro está activo o inactivo.
     * @return
     */
    public boolean isActivo() {
        return activo;
    }

    /**
     * Establece el estado de actividad del registro.
     * @param activo
     */
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /**
     * Declarado aquí para que una referencia {@code Usuario} permita leerlo. El campo y su
     * implementación siguen en cada subclase, que es donde se inicializan.
     * @return
     */
    public abstract String getNombre();

    /**
     * Declarado aquí para que una referencia {@code Usuario} permita escribirlo. El campo y su
     * implementación siguen en cada subclase, que es donde se inicializan.
     * @param nombre
     */
    public abstract void setNombre(String nombre);

    /**
     * Apellido. Comparten la justificación de {@link #getNombre()}.
     * @return apellido
     */
    public abstract String getApellido();

    /**
     * Declarado aquí para que una referencia {@code Usuario} permita escribirlo. El campo y su
     * implementación siguen en cada subclase, que es donde se inicializan.
     * @param apellido
     */
    public abstract void setApellido(String apellido);

    /**
     * Nombre y apellidos unidos por un espacio.
     * Declarado aquí para que una referencia {@code Usuario} permita leerlo. El campo y su* @return nombre completo
     */
    public abstract String getNombreCompleto();
}
