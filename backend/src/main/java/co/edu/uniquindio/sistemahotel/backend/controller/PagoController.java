package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.dto.PagoRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.SaldoPagoDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Pago;
import co.edu.uniquindio.sistemahotel.backend.service.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class PagoController {

    private final PagoService pagoService;

    /**
     * Constructor para inyectar el PagoService.
     * @param pagoService
     */
    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * Endpoint para registrar un nuevo pago.
     * Este endpoint es utilizado principalmente por el sistema de pagos externo, pero también puede ser utilizado
     * por el recepcionista para registrar pagos manuales.
     * @param dto
     * @return
     */
    @PostMapping
    public ResponseEntity<Pago> registrarPago(@RequestBody PagoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.registrarPago(dto));
    }

    /**
     * Endpoint para listar todos los pagos registrados en el sistema.
     * Esto es útil para que el gerente pueda revisar el historial completo de transacciones.
     * @return
     */
    @GetMapping
    public ResponseEntity<List<Pago>> listarTodos() {
        return ResponseEntity.ok(pagoService.listarTodos());
    }

    /**
     * Endpoint para listar todos los pagos asociados a una estadia específica.
     * Esto es útil para que el recepcionista pueda revisar los pagos realizados por un huésped durante su estadia,
     * o para que el gerente pueda auditar las transacciones relacionadas con una estadia en particular.
     * @param idEstadia
     * @return
     */
    @GetMapping("/estadia/{idEstadia}")
    public ResponseEntity<List<Pago>> pagosPorEstadia(@PathVariable String idEstadia) {
        return ResponseEntity.ok(pagoService.listarPorEstadia(idEstadia));
    }

    /**
     * Endpoint para consultar el estado de cuenta de una estadía: total general, total
     * aprobado y saldo pendiente. Es la fuente de verdad del saldo; el frontend solo lo muestra.
     * @param idEstadia
     * @return
     */
    @GetMapping("/estadia/{idEstadia}/saldo")
    public ResponseEntity<SaldoPagoDTO> consultarSaldo(@PathVariable String idEstadia) {
        return ResponseEntity.ok(pagoService.consultarSaldo(idEstadia));
    }

    /**
     * Endpoint para aprobar un pago pendiente. Solo el gerente autenticado puede realizar esta acción.
     * El administrador puede proporcionar una referencia de pago (por ejemplo
     * @param id
     * @param body
     * @return
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<Pago> aprobar(@PathVariable String id,
                                        @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(pagoService.aprobarPago(id,
                body.getOrDefault("referencia", "REF-MANUAL")));
    }

    /**
     * Endpoint para rechazar un pago pendiente. Solo el gerente autenticado puede realizar esta acción.
     * El Administrador puede proporcionar observaciones sobre el motivo del rechazo,
     * lo cual es útil para mantener un registro claro de las decisiones tomadas sobre cada pago.
     * @param id
     * @param body
     * @return
     */
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<Pago> rechazar(@PathVariable String id,
                                         @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(pagoService.rechazarPago(id,
                body.getOrDefault("observaciones", "")));
    }

    /**
     * Endpoint para reembolsar un pago aprobado.
     * Solo el gerente autenticado puede realizar esta acción.
     * Esto es útil en casos donde un huésped solicita un reembolso por una estadia o servicio,
     * o cuando se detecta un error en la transacción que requiere revertir el pago.
     * @param id
     * @return
     */
    @PutMapping("/{id}/reembolsar")
    public ResponseEntity<Pago> reembolsar(@PathVariable String id) {
        return ResponseEntity.ok(pagoService.reembolsarPago(id));
    }
}
