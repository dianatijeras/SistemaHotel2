package co.edu.uniquindio.sistemahotel.backend.model;


import co.edu.uniquindio.sistemahotel.backend.enums.Rol;

public class Huesped extends Usuario{

    private String idHuesped;
    private String nombre;
    private String apellido;
    private String documento;
    private String telefono;
    private String correo;

    public Huesped(String idHuesped, String nombre, String apellido, String documento, String telefono, String correo, String username, String password) {
        super(idHuesped, username, password, Rol.HUESPED);
        this.idHuesped = idHuesped;
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.telefono = telefono;
        this.correo = correo;
    }

    /**
     * Actualiza los datos personales del huésped. El identificador interno nunca se modifica:
     * es responsabilidad exclusiva del sistema.
     * @param nombre
     * @param apellido
     * @param documento
     * @param telefono
     * @param correo
     */
    public void actualizarDatos(String nombre, String apellido, String documento, String telefono, String correo) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.telefono = telefono;
        this.correo = correo;
    }

    /*
     * Obtiene el documento de identidad del huésped.
     * @return
     */
    public String getDocumento() {
        return documento;
    }

    /*
     * Establece el documento de identidad del huésped.
     * @param documento
     */
    public void setDocumento(String documento) {
        this.documento = documento;
    }

    /**
     * Obtiene el identificador interno del huésped.
     * @return
     */
    public String getIdHuesped() {
        return idHuesped;
    }

    /**
     * Establece el identificador interno del huésped.
     * @param idHuesped
     */
    public void setIdHuesped(String idHuesped) {
        this.idHuesped = idHuesped;
    }

    /**
     * Obtiene el teléfono de contacto.
     * @return
     */
    public String getTelefono() {
        return telefono;
    }

    /**
     * Establece el teléfono de contacto.
     * @param telefono
     */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /**
     *
     * @return
     */
    public String getCorreo() {
        return correo;
    }

    /**
     * Establece el correo de contacto.
     * @param correo
     */
    public void setCorreo(String correo) {
        this.correo = correo;
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
