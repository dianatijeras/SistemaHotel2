package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.LoginRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.UsuarioResponseDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Administrador;
import co.edu.uniquindio.sistemahotel.backend.model.Recepcionista;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final DataStore dataStore;

    /**
     * Constructor para inyectar el DataStore.
     * @param dataStore
     */
    public AuthController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Endpoint para autenticar usuarios (administradores y recepcionistas).
     * @param dto
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto) {
        if (dto == null || isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Usuario y contraseña son obligatorios."));
        }

        String username = dto.getUsername().trim();
        String password = dto.getPassword().trim();

        return dataStore.getAdministradores().stream()
                .filter(admin -> admin.autenticar(username, password))
                .findFirst()
                .<ResponseEntity<?>>map(admin -> ResponseEntity.ok(toResponse(admin)))
                .orElseGet(() -> dataStore.getRecepcionistas().stream()
                        .filter(recepcionista -> recepcionista.autenticar(username, password))
                        .findFirst()
                        .<ResponseEntity<?>>map(recepcionista -> ResponseEntity.ok(toResponse(recepcionista)))
                        .orElseGet(() -> ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("message", "Usuario o contraseña incorrectos."))));
    }

    /**
     * Método auxiliar para verificar si una cadena es nula o está vacía.
     * @param value
     * @return
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Métodos auxiliares para convertir entidades a DTOs de respuesta.
     * @param admin
     * @return
     */
    private UsuarioResponseDTO toResponse(Administrador admin) {
        return new UsuarioResponseDTO(
                admin.getIdAdministrador(),
                admin.getUsername(),
                admin.getNombre(),
                admin.getApellido(),
                admin.getRol(),
                admin.isActivo()
        );
    }

    /**
     * Métodos auxiliares para convertir entidades a DTOs de respuesta.
     * @param recepcionista
     * @return
     */
    private UsuarioResponseDTO toResponse(Recepcionista recepcionista) {
        return new UsuarioResponseDTO(
                recepcionista.getIdRecepcionista(),
                recepcionista.getUsername(),
                recepcionista.getNombre(),
                recepcionista.getApellido(),
                recepcionista.getRol(),
                recepcionista.isActivo()
        );
    }
}