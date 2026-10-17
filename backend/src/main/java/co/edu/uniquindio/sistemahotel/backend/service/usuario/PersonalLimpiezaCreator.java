package co.edu.uniquindio.sistemahotel.backend.service.usuario;

import co.edu.uniquindio.sistemahotel.backend.model.PersonalLimpieza;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;


public final class PersonalLimpiezaCreator extends UsuarioCreator {

    public PersonalLimpiezaCreator(UsuarioRepository usuarios, HuespedRepository huespedes) {
        super(usuarios, huespedes);
    }

    @Override
    protected void validarAtributosPropios(AltaUsuario alta) {
        // Sin requisitos adicionales más allá de los campos comunes.
    }

    @Override
    protected Usuario crearEmpleado(AltaUsuario alta) {
        return new PersonalLimpieza(
                generarIdLibre(),
                alta.nombre().trim(),
                alta.apellido().trim(),
                alta.usernameNormalizado(),
                alta.password());
    }

    @Override
    protected String formatoDeId() {
        return "PL-%03d";
    }

    @Override
    protected String nombreDelTipo() {
        return "personal de limpieza";
    }

    @Override
    protected long cantidadDeEsteTipo() {
        return usuarios.buscarTodos().stream().filter(u -> u instanceof PersonalLimpieza).count();
    }
}