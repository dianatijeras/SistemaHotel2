package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.dto.PersonalLimpiezaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.RecepcionistaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.TurnoRecepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.PersonalLimpieza;
import co.edu.uniquindio.sistemahotel.backend.model.Recepcionista;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;
import co.edu.uniquindio.sistemahotel.backend.service.usuario.AltaUsuario;
import co.edu.uniquindio.sistemahotel.backend.service.usuario.PersonalLimpiezaCreator;
import co.edu.uniquindio.sistemahotel.backend.service.usuario.RecepcionistaCreator;
import co.edu.uniquindio.sistemahotel.backend.service.usuario.UsuarioCreator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final RecepcionistaCreator recepcionistaCreator;
    private final PersonalLimpiezaCreator personalLimpiezaCreator;
    private final UsuarioRepository usuarios;

    /**
     * @param usuarios  repositorio del personal
     * @param huespedes repositorio de huéspedes, necesario para la unicidad global del username
     */
    public UsuarioService(UsuarioRepository usuarios, HuespedRepository huespedes) {
        this.recepcionistaCreator = new RecepcionistaCreator(usuarios, huespedes);
        this.personalLimpiezaCreator = new PersonalLimpiezaCreator(usuarios, huespedes);
        this.usuarios = usuarios;
    }

    /**
     * Lista todos los miembros del personal, sin filtrar por tipo ni estado.
     * @return
     */
    public List<Usuario> listarTodos() {
        return usuarios.buscarTodos();
    }

    /**
     * Alterna el estado activo/inactivo de un miembro del personal.
     * @param id
     * @return
     */
    public Optional<Usuario> alternarEstado(String id) {
        Optional<Usuario> encontrado = usuarios.buscarPorId(id);
        encontrado.ifPresent(u -> u.setActivo(!u.isActivo()));
        return encontrado;
    }

    /**
     * Da de alta a una recepcionista.
     * @param dto
     * @return
     */
    public synchronized Recepcionista crearRecepcionista(RecepcionistaRequestDTO dto) {
        Usuario creado = recepcionistaCreator.crear(altaDe(dto));
        return (Recepcionista) creado;
    }

    /**
     * Da de alta a un miembro del personal de limpieza.
     * @param dto
     * @return
     */
    public synchronized PersonalLimpieza crearPersonalLimpieza(PersonalLimpiezaRequestDTO dto) {
        Usuario creado = personalLimpiezaCreator.crear(altaDe(dto));
        return (PersonalLimpieza) creado;
    }

    /**
     * Convierte un DTO de recepcionista en un objeto de alta de usuario.
     * @param dto
     * @return
     */
    private AltaUsuario altaDe(RecepcionistaRequestDTO dto) {
        TurnoRecepcionista turno = dto == null ? null : dto.getTurno();
        return new AltaUsuario(
                dto == null ? null : dto.getNombre(),
                dto == null ? null : dto.getApellido(),
                dto == null ? null : dto.getUsername(),
                dto == null ? null : dto.getPassword(),
                turno);
    }

    /**
     * Convierte un DTO de personal de limpieza en un objeto de alta de usuario.
     * @param dto
     * @return
     */
    private AltaUsuario altaDe(PersonalLimpiezaRequestDTO dto) {

        String username = dto == null ? null : dto.getUsername();
        if (username == null || username.isBlank()) {
            username = "limpieza-" + java.util.UUID.randomUUID();
        }
        String password = dto == null ? null : dto.getPassword();
        if (password == null || password.isBlank()) {
            password = java.util.UUID.randomUUID().toString();
        }
        return new AltaUsuario(
                dto == null ? null : dto.getNombre(),
                dto == null ? null : dto.getApellido(),
                username,
                password,
                null);
    }
}
