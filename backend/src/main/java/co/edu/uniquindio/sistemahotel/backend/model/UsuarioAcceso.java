package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.Rol;

/**
 * Clase abstracta que representa un usuario con acceso al sistema.
 * Extiende de {@link Usuario}, de modo que hereda los atributos y métodos comunes a todos los usuarios.
 * Esta clase no puede ser instanciada directamente, sino que debe ser extendida por subclases concretas
 * que representen tipos específicos de usuarios con acceso al sistema.
 */
public abstract class UsuarioAcceso extends Usuario {

    /**
     * @param idUsuario
     * @param username
     * @param password
     * @param rol
     */
    protected UsuarioAcceso(String idUsuario, String username, String password, Rol rol) {
        super(idUsuario, username, password, rol);
    }

    /**
     * Autentica comparando las credenciales recibidas con las almacenadas.
     * @param username
     * @param password
     * @return true
     */
    public boolean autenticar(String username, String password) {
        return this.username.equals(username) && this.password.equals(password) && this.activo;
    }
}
