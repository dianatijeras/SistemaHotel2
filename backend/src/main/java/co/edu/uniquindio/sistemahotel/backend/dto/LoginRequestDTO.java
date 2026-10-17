package co.edu.uniquindio.sistemahotel.backend.dto;


public class LoginRequestDTO {
    private String username;
    private String password;

    /**
     * Metodo para obtener el nombre de usuario del DTO de solicitud de inicio de sesión.
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * Metodo para establecer el nombre de usuario del DTO de solicitud de inicio de sesión.
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Metodo para obtener la contraseña del DTO de solicitud de inicio de sesión.
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Metodo para establecer la contraseña del DTO de solicitud de inicio de sesión.
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
