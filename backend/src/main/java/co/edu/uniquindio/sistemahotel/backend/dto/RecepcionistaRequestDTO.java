package co.edu.uniquindio.sistemahotel.backend.dto;

import co.edu.uniquindio.sistemahotel.backend.enums.TurnoRecepcionista;

public class RecepcionistaRequestDTO {

    private String nombre;
    private String apellido;
    private TurnoRecepcionista turno;
    private String username;
    private String password;

    /**
     * Metodo para obtener el nombre del recepcionista
     * @return
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Metodo para establecer el nombre del recepcionista
     * @param nombre
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Metodo para obtener el apellido del recepcionista
     * @return
     */
    public String getApellido() {
        return apellido;
    }

    /**
     * Metodo para establecer el apellido del recepcionista
     * @param apellido
     */
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    /**
     * Metodo para obtener el turno del recepcionista
     * @return
     */
    public TurnoRecepcionista getTurno() {
        return turno;
    }

    /**
     * Metodo para establecer el turno del recepcionista
     * @param turno
     */
    public void setTurno(TurnoRecepcionista turno) {
        this.turno = turno;
    }

    /**
     * Metodo para obtener el username del recepcionista
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * Metodo para establecer el username del recepcionista
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Metodo para obtener el password del recepcionista
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Metodo para establecer el password del recepcionista
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}

