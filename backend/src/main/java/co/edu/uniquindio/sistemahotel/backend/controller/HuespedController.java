package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.HuespedRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.service.HuespedService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/huespedes")
@CrossOrigin(origins = "*")
public class HuespedController {

    private final HuespedService huespedService;
    private final DataStore dataStore;

    /**
     * Constructor para inyectar el HuespedService y el DataStore.
     * @param huespedService
     * @param dataStore
     */
    public HuespedController(HuespedService huespedService, DataStore dataStore) {
        this.huespedService = huespedService;
        this.dataStore = dataStore;
    }

    /**
     * Endpoint para registrar un nuevo huésped. Solo el recepcionista autenticado puede realizar esta acción.
     * @param rol
     * @param idUsuario
     * @param username
     * @param dto
     * @return
     */
    @PostMapping
    public ResponseEntity<Huesped> registrar(@RequestHeader(value = "X-User-Role", required = false) String rol,
                                             @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
                                             @RequestHeader(value = "X-Username", required = false) String username,
                                             @RequestBody HuespedRequestDTO dto) {
        validarRecepcionistaAutenticado(rol, idUsuario, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(huespedService.registrarHuesped(dto));
    }

    /**
     * Endpoint para listar todos los huéspedes registrados en el sistema.
     * @return
     */
    @GetMapping
    public ResponseEntity<List<Huesped>> listarTodos() {
        return ResponseEntity.ok(huespedService.listarTodos());
    }

    /**
     * Endpoint para buscar un huésped por su ID. Retorna 404 si no se encuentra el huésped.
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<Huesped> buscarPorId(@PathVariable String id) {
        return huespedService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint para actualizar los datos de un huésped existente. Solo el recepcionista autenticado puede realizar esta acción.
     * Los campos que no se envíen en el cuerpo se conservan tal como estaban, por lo que enviar
     * únicamente teléfono y correo mantiene el comportamiento anterior.
     * @param rol
     * @param idUsuario
     * @param username
     * @param id
     * @param body
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<Huesped> actualizar(@RequestHeader(value = "X-User-Role", required = false) String rol,
                                              @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
                                              @RequestHeader(value = "X-Username", required = false) String username,
                                              @PathVariable String id,
                                              @RequestBody Map<String, String> body) {
        validarRecepcionistaAutenticado(rol, idUsuario, username);
        return ResponseEntity.ok(huespedService.actualizarDatos(
                id, body.get("nombre"), body.get("apellido"), body.get("documento"),
                body.get("telefono"), body.get("correo")));
    }

    /**
     * Endpoint para eliminar un huésped del sistema.
     * Solo el recepcionista autenticado puede realizar esta acción.
     * @param rol
     * @param idUsuario
     * @param username
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@RequestHeader(value = "X-User-Role", required = false) String rol,
                                         @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
                                         @RequestHeader(value = "X-Username", required = false) String username,
                                         @PathVariable String id) {
        validarRecepcionistaAutenticado(rol, idUsuario, username);
        huespedService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Método auxiliar para validar que el usuario autenticado es un recepcionista activo en el sistema.
     * Verifica el rol y la existencia del recepcionista en el DataStore.
     * @param rol
     * @param idUsuario
     * @param username
     */
    private void validarRecepcionistaAutenticado(String rol, String idUsuario, String username) {
        boolean rolRecepcionista = esRolRecepcionista(rol);
        boolean recepcionistaExiste = dataStore.getRecepcionistas().stream()
                .anyMatch(recepcionista -> recepcionista.isActivo()
                        && coincide(recepcionista.getIdRecepcionista(), idUsuario)
                        && coincide(recepcionista.getUsername(), username));

        if (!rolRecepcionista || !recepcionistaExiste) {
            throw new IllegalStateException("Solo el recepcionista puede crear o administrar huéspedes.");
        }
    }

    /**
     * Método auxiliar para verificar si el rol proporcionado corresponde a un recepcionista,
     * normalizando el valor para evitar problemas de mayúsculas o espacios en blanco.
     * @param rol
     * @return
     */
    private boolean esRolRecepcionista(String rol) {
        if (rol == null) {
            return false;
        }
        String rolNormalizado = rol.trim().toUpperCase();
        return rolNormalizado.equals("RECEPCIONISTA")
                || rolNormalizado.equals("ROLE_RECEPCIONISTA");
    }

    /**
     * Método auxiliar para comparar dos cadenas de texto, ignorando mayúsculas y espacios en blanco al inicio
     * o al final. Retorna true si ambas cadenas son iguales después de la normalización.
     * @param esperado
     * @param recibido
     * @return
     */
    private boolean coincide(String esperado, String recibido) {
        return esperado != null && recibido != null && esperado.equalsIgnoreCase(recibido.trim());
    }
}
