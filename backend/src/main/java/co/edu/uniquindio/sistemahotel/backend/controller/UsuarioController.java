package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.PersonalLimpiezaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.RecepcionistaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.UsuarioResponseDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Usuario;
import co.edu.uniquindio.sistemahotel.backend.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final DataStore dataStore;
    private final UsuarioService usuarioService;

    /**
     * Constructor de la clase UsuarioController.
     * @param dataStore
     * @param usuarioService
     */
    public UsuarioController(DataStore dataStore, UsuarioService usuarioService) {
        this.dataStore = dataStore;
        this.usuarioService = usuarioService;
    }

    /**
     * Endpoint para listar todos los usuarios del sistema (administradores, recepcionistas y personal de limpieza).
     * @return
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        List<UsuarioResponseDTO> usuarios = new ArrayList<>();
        usuarioService.listarTodos().forEach(u -> usuarios.add(toResponse(u)));
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Endpoint para crear un nuevo recepcionista. Solo el administrador autenticado puede realizar esta acción.
     * @param rol
     * @param idUsuario
     * @param username
     * @param dto
     * @return
     */
    @PostMapping("/recepcionistas")
    public ResponseEntity<UsuarioResponseDTO> crearRecepcionista(@RequestHeader(value = "X-User-Role", required = false) String rol,
                                                                 @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
                                                                 @RequestHeader(value = "X-Username", required = false) String username,
                                                                 @RequestBody RecepcionistaRequestDTO dto) {
        validarAdminAutenticado(rol, idUsuario, username);
        Usuario recepcionista = usuarioService.crearRecepcionista(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(recepcionista));
    }

    /**
     * Endpoint para crear un nuevo personal de limpieza.
     * Solo el administrador autenticado puede realizar esta acción.
     * @param rol
     * @param idUsuario
     * @param username
     * @param dto
     * @return
     */
    @PostMapping("/personal-limpieza")
    public ResponseEntity<UsuarioResponseDTO> crearPersonalLimpieza(@RequestHeader(value = "X-User-Role", required = false) String rol,
                                                                    @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
                                                                    @RequestHeader(value = "X-Username", required = false) String username,
                                                                    @RequestBody PersonalLimpiezaRequestDTO dto) {
        validarAdminAutenticado(rol, idUsuario, username);
        Usuario personal = usuarioService.crearPersonalLimpieza(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(personal));
    }

    /**
     * Endpoint para cambiar el estado (activo/inactivo) de un usuario
     * (administrador, recepcionista o personal de limpieza).
     * Solo el administrador autenticado puede realizar esta acción.
     * @param rol
     * @param idUsuario
     * @param username
     * @param id
     * @return
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(@RequestHeader(value = "X-User-Role", required = false) String rol,
                                                            @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
                                                            @RequestHeader(value = "X-Username", required = false) String username,
                                                            @PathVariable String id) {
        validarAdminAutenticado(rol, idUsuario, username);
        return usuarioService.alternarEstado(id)
                .map(u -> ResponseEntity.ok(toResponse(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Método auxiliar para validar que el usuario autenticado es un administrador activo.
     * Si no se cumple esta condición, se lanza una excepción indicando que solo el administrador puede
     * realizar la acción solicitada.
     * @param rol
     * @param idUsuario
     * @param username
     */
    private void validarAdminAutenticado(String rol, String idUsuario, String username) {
        boolean rolAdmin = esRolAdmin(rol);
        boolean adminExiste = dataStore.getAdministradores().stream()
                .anyMatch(admin -> admin.isActivo()
                        && coincide(admin.getIdAdministrador(), idUsuario)
                        && coincide(admin.getUsername(), username));

        if (!rolAdmin || !adminExiste) {
            throw new IllegalStateException("Solo el administrador puede crear o administrar personal del hotel.");
        }
    }

    /**
     * Método auxiliar para verificar si el rol proporcionado corresponde a un administrador.
     * Se normaliza el valor del rol eliminando espacios en blanco y convirtiéndolo a mayúsculas para
     * permitir diferentes formatos de entrada (por ejemplo, "admin", "ADMIN", "Role_Admin", etc.).
     * Si el rol es nulo o no coincide con ninguna de las variantes reconocidas, se devuelve false.
     * @param rol
     * @return
     */
    private boolean esRolAdmin(String rol) {
        if (rol == null) {
            return false;
        }
        String rolNormalizado = rol.trim().toUpperCase();
        return rolNormalizado.equals("ADMIN") || rolNormalizado.equals("ROLE_ADMIN") || rolNormalizado.equals("ADMINISTRADOR");
    }

    /**
     * Compara dos cadenas ignorando mayúsculas y espacios al principio y al final.
     * @param esperado valor contra el que se compara
     * @param recibido valor recibido
     * @return true si coinciden tras normalizar
     */
    private boolean coincide(String esperado, String recibido) {
        return esperado != null && recibido != null && esperado.equalsIgnoreCase(recibido.trim());
    }

    /**
     * Convierte un objeto Usuario a su representación DTO para la respuesta de la API.
     * @param usuario
     * @return
     */
    private UsuarioResponseDTO toResponse(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getIdUsuario(), usuario.getUsername(), usuario.getNombre(), usuario.getApellido(), usuario.getRol(), usuario.isActivo());
    }
}
