package co.edu.uniquindio.sistemahotel.backend.service.usuario;

import co.edu.uniquindio.sistemahotel.backend.model.Recepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;


public final class RecepcionistaCreator extends UsuarioCreator {

    public RecepcionistaCreator(UsuarioRepository usuarios, HuespedRepository huespedes) {
        super(usuarios, huespedes);
    }

    @Override
    protected void validarAtributosPropios(AltaUsuario alta) {
        if (alta.turno() == null) {
            throw new IllegalArgumentException("El turno del recepcionista es obligatorio.");
        }
    }

    @Override
    protected Usuario crearEmpleado(AltaUsuario alta) {
        return new Recepcionista(
                generarIdLibre(),
                alta.nombre().trim(),
                alta.apellido().trim(),
                alta.turno(),
                alta.usernameNormalizado(),
                alta.password());
    }

    @Override
    protected String formatoDeId() {
        return "R-%03d";
    }

    @Override
    protected String nombreDelTipo() {
        return "recepcionista";
    }

    @Override
    protected long cantidadDeEsteTipo() {
        return usuarios.buscarTodos().stream().filter(u -> u instanceof Recepcionista).count();
    }
}