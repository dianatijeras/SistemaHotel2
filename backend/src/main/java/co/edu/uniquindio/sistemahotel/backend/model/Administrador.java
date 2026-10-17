package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.Rol;

/**
 * El administrador del hotel: el único que da de alta personal y alterna su estado.
 * Extiende de {@link UsuarioAcceso}, de modo que hereda el algoritmo de autenticación sin
 * sobrescribirlo.
 */
public class Administrador extends UsuarioAcceso {

    private String idAdministrador;
    private String nombre;
    private String apellido;

    /**
     * @param idAdministrador
     * @param nombre
     * @param apellido
     * @param username
     * @param password
     */
    public Administrador(String idAdministrador, String nombre, String apellido, String username, String password) {
        super(idAdministrador, username, password, Rol.ADMIN);
        this.idAdministrador = idAdministrador;
        this.nombre = nombre;
        this.apellido = apellido;
    }

    /**
     * Obtiene el identificador interno del administrador.
     * @return
     */
    public String getIdAdministrador() {
        return idAdministrador;
    }

    /**
     * Establece el identificador interno del administrador.
     * @param id
     */
    public void setIdAdministrador(String id) {
        this.idAdministrador = id;
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
