package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.dto.ReservaActualizacionDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "*")
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * Constructor para inyectar el ReservaService.
     * @param reservaService
     */
    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * Endpoint para crear una nueva reserva.
     * El DTO debe contener el ID del huésped, el ID de la habitación, la fecha de check-in y la fecha de check-out.
     * @param dto
     * @return
     */
    @PostMapping
    public ResponseEntity<Reserva> crearReserva(@RequestBody ReservaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crearReserva(dto));
    }

    /**
     * Endpoint para modificar una reserva existente.
     * @param id
     * @param dto
     * @return la reserva ya modificada
     */
    @PutMapping("/{id}")
    public ResponseEntity<Reserva> modificar(@PathVariable String id,
                                             @RequestBody ReservaActualizacionDTO dto) {
        return ResponseEntity.ok(reservaService.modificarReserva(id, dto));
    }

    /**
     * Endpoint para listar todas las reservas registradas en el sistema.
     * @return
     */
    @GetMapping
    public ResponseEntity<List<Reserva>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    /**
     * Endpoint para buscar una reserva por su ID. Retorna 404 si no se encuentra la reserva.
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable String id) {
        return reservaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint para listar todas las reservas asociadas a un huésped específico,
     * identificado por su ID. Esto permite a los usuarios ver el historial de reservas de un huésped en particular.
     * @param idHuesped
     * @return
     */
    @GetMapping("/huesped/{idHuesped}")
    public ResponseEntity<List<Reserva>> reservasPorHuesped(@PathVariable String idHuesped) {
        return ResponseEntity.ok(reservaService.listarPorHuesped(idHuesped));
    }

    /**
     * Endpoint para confirmar una reserva pendiente. Esto cambia el estado de la reserva a "confirmada".
     * Solo el recepcionista autenticado puede realizar esta acción.
     * @param id
     * @return
     */
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<Reserva> confirmar(@PathVariable String id) {
        return ResponseEntity.ok(reservaService.confirmarReserva(id));
    }

    /**
     * Endpoint para cancelar una reserva pendiente o confirmada.
     * Esto cambia el estado de la reserva a "cancelada".
     * Solo el recepcionista autenticado puede realizar esta acción.
     * @param id
     * @return
     */
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Reserva> cancelar(@PathVariable String id) {
        return ResponseEntity.ok(reservaService.cancelarReserva(id));
    }

    /**
     * Endpoint para marcar una reserva como "no-show".
     * Esto cambia el estado de la reserva a "no-show".
     * Solo el recepcionista autenticado puede realizar esta acción.
     * Esto es útil para llevar un registro de los huéspedes que no se presentaron a su reserva,
     * lo cual puede ser importante para futuras decisiones sobre políticas de cancelación o penalizaciones.
     * @param id
     * @return
     */
    @PutMapping("/{id}/no-show")
    public ResponseEntity<Reserva> noShow(@PathVariable String id) {
        return ResponseEntity.ok(reservaService.marcarNoShow(id));
    }
}
