package co.edu.uniquindio.sistemahotel.backend.dto;


public class HuespedRequestDTO {

    private String nombre;
    private String apellido;
    private String documento;
    private String telefono;
    private String correo;
    private String username;
    private String password;


    /**
     * Metodo para obtener el nombre del huésped
     * @return
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Metodo para establecer el nombre del huésped
     * @param n
     */
    public void setNombre(String n) {
        this.nombre = n;
    }

    /**
     * Metodo para obtener el apellido del huésped
     * @return
     */
    public String getApellido() {
        return apellido;
    }

    /**
     * Metodo para establecer el apellido del huésped
     * @param a
     */
    public void setApellido(String a) {
        this.apellido = a;
    }

    /**
     * Metodo para obtener el documento de identificación del huésped
     * @return
     */
    public String getDocumento() {
        return documento;
    }

    /**
     * Metodo para establecer el documento de identificación del huésped
     * @param d
     */
    public void setDocumento(String d) {
        this.documento = d;
    }

    /**
     * Metodo para obtener el teléfono del huésped
     * @return
     */
    public String getTelefono() {
        return telefono;
    }

    /**
     * Metodo para establecer el teléfono del huésped
     * @param t
     */
    public void setTelefono(String t) {
        this.telefono = t;
    }

    /**
     * Metodo para obtener el correo del huésped
     * @return
     */
    public String getCorreo() {
        return correo;
    }

    /**
     * Metodo para establecer el correo del huésped
     * @param c
     */
    public void setCorreo(String c) {
        this.correo = c;
    }

    /**
     * Metodo para obtener el nombre de usuario del huésped
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * Metodo para establecer el nombre de usuario del huésped
     * @param u
     */
    public void setUsername(String u) {
        this.username = u;
    }

    /**
     * Metodo para obtener la contraseña del huésped
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Metodo para establecer la contraseña del huésped
     * @param p
     */
    public void setPassword(String p) {
        this.password = p;
    }
}
