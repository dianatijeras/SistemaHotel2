package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.dto.ConsumoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.EstadiaCostoDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Estadia;
import co.edu.uniquindio.sistemahotel.backend.service.EstadiaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estadias")
@CrossOrigin(origins = "*")
public class EstadiaController {

    private final EstadiaService estadiaService;

    /**
     * Constructor para inyectar el EstadiaService.
     * @param estadiaService
     */
    public EstadiaController(EstadiaService estadiaService) {
        this.estadiaService = estadiaService;
    }

    /**
     * Endpoint para listar todas las estadias.
     * @return
     */
    @GetMapping
    public ResponseEntity<List<Estadia>> listarTodas() {
        return ResponseEntity.ok(estadiaService.listarTodas());
    }

    /**
     * Endpoint para listar estadias que están listas para pagos (check-out realizado pero pago pendiente).
     * @return
     */
    @GetMapping("/para-pagos")
    public ResponseEntity<List<Estadia>> listarParaPagos() {
        return ResponseEntity.ok(estadiaService.listarParaPagos());
    }

    /**
     * Endpoint para listar estadias que están actualmente en curso (check-in realizado pero check-out no realizado).
     * @return
     */
    @GetMapping("/en-curso")
    public ResponseEntity<List<Estadia>> listarEnCurso() {
        return ResponseEntity.ok(estadiaService.listarEnCurso());
    }

    /**
     * Endpoint para buscar una estadia por su ID.
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<Estadia> buscarPorId(@PathVariable String id) {
        return estadiaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint para realizar el check-in de una reserva, creando una nueva estadia asociada a esa reserva.
     * @param idReserva
     * @return
     */
    @PostMapping("/checkin/{idReserva}")
    public ResponseEntity<Estadia> checkIn(@PathVariable String idReserva) {
        return ResponseEntity.ok(estadiaService.realizarCheckIn(idReserva));
    }

    /**
     * Endpoint para consultar el desglose de costos de una estadía (F-09): noches, precio por
     * noche, subtotal de alojamiento, detalle de consumos, subtotal de consumos y total general.
     * Es de solo lectura: no modifica la estadía ni altera los JSON ya usados por el resto de la API.
     * @param id
     * @return
     */
    @GetMapping("/{id}/costo")
    public ResponseEntity<EstadiaCostoDTO> consultarCosto(@PathVariable String id) {
        return ResponseEntity.ok(estadiaService.calcularCosto(id));
    }

    /**
     * Endpoint para realizar el check-out de una estadia, actualizando su estado y calculando el total a pagar.
     * @param id
     * @return
     */
    @PutMapping("/{id}/checkout")
    public ResponseEntity<Estadia> checkOut(@PathVariable String id) {
        return ResponseEntity.ok(estadiaService.realizarCheckOut(id));
    }

    /**
     * Endpoint para agregar un consumo adicional a una estadia en curso, como servicios de restaurante o minibar.
     * @param dto
     * @return
     */
    @PostMapping("/consumo")
    public ResponseEntity<Estadia> agregarConsumo(@RequestBody ConsumoRequestDTO dto) {
        return ResponseEntity.ok(estadiaService.agregarConsumo(dto));
    }
}
