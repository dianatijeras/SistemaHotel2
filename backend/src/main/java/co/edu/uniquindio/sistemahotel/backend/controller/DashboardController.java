package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para manejar las solicitudes relacionadas con el dashboard del sistema hotelero.
 * Proporciona un endpoint para obtener estadísticas clave del hotel, como número de habitaciones disponibles,
 * ocupadas, reservas activas, pagos pendientes, entre otros.
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * @param dashboardService servicio que calcula las métricas
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Endpoint para obtener estadísticas clave del hotel, como número de habitaciones disponibles,
     * ocupadas, reservas activas, pagos pendientes, entre otros.
     * @return el cuadro de métricas
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        return ResponseEntity.ok(dashboardService.estadisticas());
    }
}
