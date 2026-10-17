package co.edu.uniquindio.sistemahotel.backend.dto;

import co.edu.uniquindio.sistemahotel.backend.enums.Rol;

public class UsuarioResponseDTO {
    private String id;
    private String username;
    private String nombre;
    private String apellido;
    private Rol rol;
    private boolean activo;

    /**
     * Constructor para crear un nuevo UsuarioResponseDTO
     * @param id
     * @param username
     * @param nombre
     * @param apellido
     * @param rol
     * @param activo
     */
    public UsuarioResponseDTO(String id, String username, String nombre, String apellido, Rol rol, boolean activo) {
        this.id = id;
        this.username = username;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
        this.activo = activo;
    }

    /**
     * Método para obtener el ID del usuario
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Método para obtener el nombre de usuario
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * Método para obtener el nombre del usuario
     * @return
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Método para obtener el apellido del usuario
     * @return
     */
    public String getApellido() {
        return apellido;
    }

    /**
     * Método para obtener el rol del usuario
     * @return
     */
    public Rol getRol() {
        return rol;
    }

    /**
     * Método para verificar si el usuario está activo
     * @return
     */
    public boolean isActivo() {
        return activo;
    }

}

