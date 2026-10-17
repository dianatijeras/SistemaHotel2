package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.dto.HuespedRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class HuespedService {

    private final HuespedRepository huespedRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * @param huespedRepository abstracción de acceso a datos de huéspedes
     * @param usuarioRepository abstracción de acceso a datos del personal del hotel; aporta la
     *                          segunda mitad de la unicidad global del nombre de usuario
     */
    public HuespedService(HuespedRepository huespedRepository, UsuarioRepository usuarioRepository) {
        this.huespedRepository = huespedRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra un nuevo huésped en el sistema. La operación es atómica: si falla alguna validación, no se guarda nada.
     * @param dto
     * @return
     */
    public synchronized Huesped registrarHuesped(HuespedRequestDTO dto) {
        validarDatosHuesped(dto);

        // El registro no exige credenciales: el identificador interno se genera automáticamente.
        String usernameNormalizado = estaVacio(dto.getUsername())
                ? "huesped-" + UUID.randomUUID()
                : dto.getUsername().trim();
        if (existeUsername(usernameNormalizado)) {
            throw new IllegalArgumentException(
                    "El username '" + usernameNormalizado + "' ya está registrado.");
        }

        String documentoNormalizado = dto.getDocumento().trim();
        Optional<Huesped> conEseDocumento = huespedRepository.buscarPorDocumento(documentoNormalizado);
        if (conEseDocumento.isPresent()) {
            throw new IllegalArgumentException(
                    "El documento '" + documentoNormalizado + "' ya está registrado para otro huésped.");
        }

        String idHuesped = "H-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Huesped huesped = new Huesped(idHuesped, dto.getNombre().trim(), dto.getApellido().trim(),
                documentoNormalizado, dto.getTelefono().trim(), dto.getCorreo().trim(),
                usernameNormalizado, estaVacio(dto.getPassword())
                ? UUID.randomUUID().toString()
                : dto.getPassword());
        return huespedRepository.guardar(huesped);
    }

    /**
     * Verifica si un nombre de usuario ya está registrado en el sistema, ya sea para un huésped o para un miembro del personal.
     * @param username
     * @return
     */
    private boolean existeUsername(String username) {
        return huespedRepository.existePorUsername(username)
                || usuarioRepository.existePorUsername(username);
    }

    /**
     * Verifica si un documento de identificación ya está registrado en el sistema para otro huésped distinto al que se está actualizando.
     * @param documento
     * @param excluirIdHuesped
     * @return
     */
    private boolean existeDocumento(String documento, String excluirIdHuesped) {
        return huespedRepository.buscarPorDocumento(documento)
                .filter(h -> !h.getIdHuesped().equals(excluirIdHuesped))
                .isPresent();
    }

    /**
     * Valida los datos del DTO de registro de huésped, lanzando una excepción si algún campo obligatorio está vacío o nulo.
     * @param dto
     */
    private void validarDatosHuesped(HuespedRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos del huésped son obligatorios.");
        }
        if (estaVacio(dto.getNombre())) {
            throw new IllegalArgumentException("El nombre del huésped es obligatorio.");
        }
        if (estaVacio(dto.getApellido())) {
            throw new IllegalArgumentException("El apellido del huésped es obligatorio.");
        }
        if (estaVacio(dto.getDocumento())) {
            throw new IllegalArgumentException("El documento de identificación del huésped es obligatorio.");
        }
        if (estaVacio(dto.getTelefono())) {
            throw new IllegalArgumentException("El teléfono del huésped es obligatorio.");
        }
        if (estaVacio(dto.getCorreo())) {
            throw new IllegalArgumentException("El correo del huésped es obligatorio.");
        }
        if (!estaVacio(dto.getPassword()) && dto.getPassword().length() < 4) {
            throw new IllegalArgumentException("La contraseña del huésped debe tener mínimo 4 caracteres.");
        }
    }

    /**
     * Verifica si un valor de cadena está vacío o nulo.
     * @param valor
     * @return
     */
    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    /**
     * Lista todos los huéspedes registrados en el sistema.
     * @return
     */
    public List<Huesped> listarTodos() {
        return huespedRepository.buscarTodos();
    }

    /**
     * Busca un huésped por su identificador interno (idHuesped). Devuelve un Optional que puede estar vacío si no se encuentra el huésped.
     * @param idHuesped
     * @return
     */
    public Optional<Huesped> buscarPorId(String idHuesped) {
        return huespedRepository.buscarPorId(idHuesped);
    }

    /**
     * Actualiza los datos de un huésped existente. Solo se actualizan los campos que no estén vacíos; los campos vacíos se ignoran.
     * @param idHuesped
     * @param nombre
     * @param apellido
     * @param documento
     * @param telefono
     * @param correo
     * @return
     */
    public Huesped actualizarDatos(String idHuesped, String nombre, String apellido,
                                   String documento, String telefono, String correo) {
        Huesped huesped = huespedRepository.buscarPorId(idHuesped)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el huésped con ID: " + idHuesped));

        if (!estaVacio(nombre)) {
            huesped.setNombre(nombre.trim());
        }
        if (!estaVacio(apellido)) {
            huesped.setApellido(apellido.trim());
        }
        if (!estaVacio(documento)) {
            String documentoNormalizado = documento.trim();
            if (existeDocumento(documentoNormalizado, idHuesped)) {
                throw new IllegalArgumentException(
                        "El documento '" + documentoNormalizado + "' ya está registrado para otro huésped.");
            }
            huesped.setDocumento(documentoNormalizado);
        }
        if (!estaVacio(telefono)) {
            huesped.setTelefono(telefono.trim());
        }
        if (!estaVacio(correo)) {
            huesped.setCorreo(correo.trim());
        }

        return huespedRepository.actualizar(huesped);
    }

    /**
     * Elimina un huésped del sistema por su identificador interno (idHuesped). Lanza una excepción si no se encuentra el huésped.
     * @param idHuesped
     */
    public void eliminar(String idHuesped) {
        if (!huespedRepository.eliminarPorId(idHuesped)) {
            throw new IllegalArgumentException("No existe el huésped con ID: " + idHuesped);
        }
    }
}