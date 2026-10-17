package co.edu.uniquindio.sistemahotel.backend.dto;

public class PersonalLimpiezaRequestDTO {

    private String nombre;
    private String apellido;
    private String username;
    private String password;

    /**
     * Metodo para obtener el nombre del personal de limpieza
     * @return
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Metodo para establecer el nombre del personal de limpieza
     * @param nombre
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Metodo para obtener el apellido del personal de limpieza
     * @return
     */
    public String getApellido() {
        return apellido;
    }

    /**
     * Metodo para establecer el apellido del personal de limpieza
     * @param apellido
     */
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    /**
     * Metodo para obtener el username del personal de limpieza
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * Metodo para establecer el username del personal de limpieza
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Metodo para obtener el password del personal de limpieza
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Metodo para establecer el password del personal de limpieza
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
