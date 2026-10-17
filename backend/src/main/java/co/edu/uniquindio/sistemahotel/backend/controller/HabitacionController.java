package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.data.DataStore;
import co.edu.uniquindio.sistemahotel.backend.dto.EstadoHabitacionRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.TipoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habitaciones")
@CrossOrigin(origins = "*")
public class HabitacionController {

    private final HabitacionService habitacionService;
    private final DataStore dataStore;

    /**
     * Constructor para inyectar el HabitacionService.
     * @param habitacionService
     * @param dataStore
     */
    public HabitacionController(HabitacionService habitacionService, DataStore dataStore) {
        this.habitacionService = habitacionService;
        this.dataStore = dataStore;
    }

    /**
     * Endpoint para listar todas las habitaciones.
     * @return
     */
    @GetMapping
    public ResponseEntity<List<Habitacion>> listarTodas() {
        return ResponseEntity.ok(habitacionService.listarTodas());
    }

    /**
     * Endpoint para listar habitaciones disponibles en un rango de fechas específico.
     * Si no se proporcionan fechas, devuelve todas las habitaciones disponibles.
     * @param fechaInicio
     * @param fechaFin
     * @param excluirReserva
     * @return
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Habitacion>> listarDisponibles(
            @RequestParam(name = "fechaInicio", required = false) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false) LocalDate fechaFin,
            @RequestParam(name = "excluirReserva", required = false) String excluirReserva) {

        if (fechaInicio == null && fechaFin == null) {
            return ResponseEntity.ok(habitacionService.listarDisponibles());
        }
        return ResponseEntity.ok(habitacionService.listarDisponibles(fechaInicio, fechaFin, excluirReserva));
    }

    /**
     * Endpoint para listar habitaciones que están pendientes de limpieza
     * (es decir, aquellas que han sido desocupadas pero aún no han sido marcadas como limpias).
     * @return
     */
    @GetMapping("/limpieza")
    public ResponseEntity<List<Habitacion>> listarPendientesLimpieza() {
        return ResponseEntity.ok(habitacionService.listarPendientesLimpieza());
    }

    /**
     * Endpoint para listar habitaciones filtradas por su tipo (SENCILLA, DOBLE, SUITE, etc.).
     * @param tipo
     * @return
     */
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Habitacion>> listarPorTipo(@PathVariable TipoHabitacion tipo) {
        return ResponseEntity.ok(habitacionService.listarPorTipo(tipo));
    }

    /**
     * Endpoint para buscar una habitación por su número único.
     * Devuelve los detalles de la habitación, incluyendo su estado actual y tipo.
     * @param numero
     * @return
     */
    @GetMapping("/{numero}")
    public ResponseEntity<Habitacion> buscarPorNumero(@PathVariable int numero) {
        return habitacionService.buscarPorNumero(numero)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint para cambiar el estado de una habitación (DISPONIBLE, OCUPADA, EN_LIMPIEZA, MANTENIMIENTO, FUERA_DE_SERVICIO).
     * Esto es útil para actualizar el estado de la habitación después de un check-out,
     * durante la limpieza o cuando se reporta un problema.
     * @param numero
     * @param dto
     * @return
     */
    @PutMapping("/{numero}/estado")
    public ResponseEntity<Habitacion> cambiarEstado(@PathVariable int numero,
                                                    @RequestBody EstadoHabitacionRequestDTO dto) {
        return ResponseEntity.ok(habitacionService.cambiarEstado(numero, dto.getEstadoHabitacion()));
    }

    /**
     * Endpoint para marcar una habitación como "En Limpieza" después de que un huésped haya hecho check-out,
     * indicando que el personal de limpieza debe preparar la habitación antes de que pueda ser reservada nuevamente.
     * @param numero
     * @return
     */
    @PutMapping("/{numero}/limpieza")
    public ResponseEntity<Habitacion> marcarEnLimpieza(@PathVariable int numero) {
        return ResponseEntity.ok(habitacionService.marcarEnLimpieza(numero));
    }

    /**
     * Endpoint para marcar una habitación como "Disponible" después de que la limpieza haya sido completada,
     * indicando que la habitación está lista para ser reservada o ocupada nuevamente.
     * @param numero
     * @param rol
     * @param idUsuario
     * @param username
     * @return
     */
    @PutMapping("/{numero}/disponible")
    public ResponseEntity<Habitacion> marcarDisponible(
            @PathVariable int numero,
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
            @RequestHeader(value = "X-Username", required = false) String username) {
        validarRecepcionistaAutenticada(rol, idUsuario, username);
        return ResponseEntity.ok(habitacionService.marcarDisponible(numero));
    }

    /**
     * Endpoint para finalizar la limpieza de una habitación, marcándola como "Disponible" después de que el
     * personal de limpieza haya confirmado que la habitación está lista para ser ocupada nuevamente.
     * @param numero
     * @param rol
     * @param idUsuario
     * @param username
     * @return
     */
    @PutMapping("/{numero}/finalizar-limpieza")
    public ResponseEntity<Habitacion> finalizarLimpieza(
            @PathVariable int numero,
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-User-Id", required = false) String idUsuario,
            @RequestHeader(value = "X-Username", required = false) String username) {
        validarRecepcionistaAutenticada(rol, idUsuario, username);
        return ResponseEntity.ok(habitacionService.finalizarLimpieza(numero));
    }

    /**
     * Valida que el usuario autenticado tenga el rol de recepcionista o administrador y que exista en el sistema.
     * @param rol
     * @param idUsuario
     * @param username
     * @throws IllegalStateException si el usuario no tiene el rol adecuado o no existe en el sistema.
     */
    private void validarRecepcionistaAutenticada(String rol, String idUsuario, String username) {
        String rolNormalizado = rol == null ? "" : rol.trim().toUpperCase();
        boolean esRecepcionista = rolNormalizado.equals("RECEPCIONISTA")
                || rolNormalizado.equals("ROLE_RECEPCIONISTA")
                || rolNormalizado.equals("ADMIN")
                || rolNormalizado.equals("ROLE_ADMIN")
                || rolNormalizado.equals("ADMINISTRADOR");

        boolean recepcionistaExiste = dataStore.getRecepcionistas().stream()
                .anyMatch(r -> r.isActivo() && coincide(r.getIdRecepcionista(), idUsuario)
                        && coincide(r.getUsername(), username));
        boolean adminExiste = dataStore.getAdministradores().stream()
                .anyMatch(a -> a.isActivo() && coincide(a.getIdAdministrador(), idUsuario)
                        && coincide(a.getUsername(), username));

        if (!esRecepcionista || (!recepcionistaExiste && !adminExiste)) {
            throw new IllegalStateException(
                    "Solo la recepción puede confirmar que una habitación terminó la limpieza.");
        }
    }

    /** Compara dos textos ignorando mayúsculas y espacios sobrantes.
     * @param esperado
     * @param recibido
     * @return true si los textos coinciden, false en caso contrario.
     */
    private boolean coincide(String esperado, String recibido) {
        return esperado != null && recibido != null && esperado.equalsIgnoreCase(recibido.trim());
    }

    /**
     * Endpoint para marcar una habitación como "En Mantenimiento" cuando se reporta un problema que requiere
     * reparación, lo que indica que la habitación no está disponible para reservas o check-in hasta que se
     * resuelva el problema.
     * @param numero
     * @return
     */
    @PutMapping("/{numero}/mantenimiento")
    public ResponseEntity<Habitacion> marcarMantenimiento(@PathVariable int numero) {
        return ResponseEntity.ok(habitacionService.marcarMantenimiento(numero));
    }

    /**
     * Endpoint para marcar una habitación como "Fuera de Servicio" cuando se necesita retirar temporalmente
     * del inventario de habitaciones disponibles, ya sea por razones de seguridad, reparaciones mayores o
     * cualquier otra circunstancia que impida su uso.
     * @param numero
     * @return
     */
    @PutMapping("/{numero}/fuera-servicio")
    public ResponseEntity<Habitacion> marcarFueraDeServicio(@PathVariable int numero) {
        return ResponseEntity.ok(habitacionService.marcarFueraDeServicio(numero));
    }
}


