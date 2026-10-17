package co.edu.uniquindio.sistemahotel.backend.service.usuario;

import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;


public abstract class UsuarioCreator {

    protected final UsuarioRepository usuarios;
    protected final HuespedRepository huespedes;

    protected UsuarioCreator(UsuarioRepository usuarios, HuespedRepository huespedes) {
        this.usuarios = usuarios;
        this.huespedes = huespedes;
    }

    public Usuario crear(AltaUsuario alta) {
        validarCamposComunes(alta);
        validarAtributosPropios(alta);
        validarUsernameUnico(alta.usernameNormalizado());

        Usuario nuevo = crearEmpleado(alta);
        return usuarios.guardar(nuevo);
    }


    /**
     * Crea un empleado de este tipo a partir de los datos del DTO. Se asume que ya se validaron
     * @param alta
     * @return
     */
    protected abstract Usuario crearEmpleado(AltaUsuario alta);

    /**
     * Valida los atributos propios de este tipo, además de los campos comunes. Se lanza
     * @param alta
     */
    protected abstract void validarAtributosPropios(AltaUsuario alta);

    /**
     * Formato del identificador de este tipo, por ejemplo {@code R-%03d}
     * @return
     */
    protected abstract String formatoDeId();

    /**
     * Nombre con el que este tipo aparece en los mensajes de error
     * @return
     */
    protected abstract String nombreDelTipo();


    /**
     * Valida los campos obligatorios comunes a todos los tipos de empleado. Se lanza
     * @param alta
     */
    protected void validarCamposComunes(AltaUsuario alta) {
        if (alta == null) {
            throw new IllegalArgumentException("Los datos del " + nombreDelTipo() + " son obligatorios.");
        }
        if (estaVacio(alta.nombre())) {
            throw new IllegalArgumentException("El nombre del " + nombreDelTipo() + " es obligatorio.");
        }
        if (estaVacio(alta.apellido())) {
            throw new IllegalArgumentException("El apellido del " + nombreDelTipo() + " es obligatorio.");
        }
        if (estaVacio(alta.username())) {
            throw new IllegalArgumentException("El usuario de acceso es obligatorio.");
        }
        if (estaVacio(alta.password()) || alta.password().length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener mínimo 4 caracteres.");
        }
    }

    /**
     * Valida que el username no esté ya registrado en ningún tipo de usuario. Se lanza
     * @param username
     */
    protected void validarUsernameUnico(String username) {
        String normalizado = username == null ? null : username.trim();
        if (usuarios.existePorUsername(normalizado) || huespedes.existePorUsername(normalizado)) {
            throw new IllegalArgumentException("El username '" + normalizado + "' ya está registrado.");
        }
    }

    /**
     * Genera un identificador libre para este tipo de empleado, con el formato que corresponda y el
     * @return
     */
    protected String generarIdLibre() {
        int consecutivo = (int) cantidadDeEsteTipo() + 1;
        String id;
        do {
            id = String.format(formatoDeId(), consecutivo++);
        } while (usuarios.buscarPorId(id).isPresent());
        return id;
    }

    protected boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    /**
     * Cuenta cuántos empleados de este tipo hay en el sistema, para generar un identificador libre.
     * @return
     */
    protected abstract long cantidadDeEsteTipo();
}