package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.service.HabitacionService;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

@Controller
public class HabitacionController {

    private final HabitacionService habitacionService;

    public HabitacionController(HabitacionService habitacionService) {
        this.habitacionService = habitacionService;
    }

    public List<Habitacion> consultarDisponibilidad(LocalDate fechaInicio, LocalDate fechaFin) {
        return habitacionService.listarDisponiblesEnFechas(fechaInicio, fechaFin);
    }

    public List<Habitacion> listarTodas() {
        return habitacionService.listarTodas();
    }

    public Habitacion buscarPorNumero(int numeroHabitacion) {
        return habitacionService.obtenerOFallar(numeroHabitacion);
    }
}


