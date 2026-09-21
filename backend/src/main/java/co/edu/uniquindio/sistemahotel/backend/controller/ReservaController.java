package co.edu.uniquindio.sistemahotel.backend.controller;

import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.dto.ReservaResponseDTO;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.service.ReservaService;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }


    public ReservaResponseDTO registrarReserva(ReservaRequestDTO dto) {
        Reserva reserva = reservaService.crearReserva(dto);
        return ReservaResponseDTO.desde(reserva);
    }

    public List<ReservaResponseDTO> listarReservas() {

        return reservaService.listarTodas()
                .stream()
                .map(ReservaResponseDTO::desde)
                .toList();
    }

    public ReservaResponseDTO buscarReserva(String idReserva) {

        Reserva reserva = reservaService.obtenerOFallar(idReserva);

        return ReservaResponseDTO.desde(reserva);
    }
}
