package co.edu.uniquindio.sistemahotel.backend.dto;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo que se le devuelve al cliente tras registrar/consultar una reserva.
 * Se "aplana" la información de huésped y habitación para que el frontend
 * no tenga que navegar objetos anidados innecesariamente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponseDTO {

    private String idReserva;
    private String codigoReserva;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int adultos;
    private int ninos;
    private EstadoReserva estadoReserva;
    private LocalDateTime fechaCreacion;

    private String idHuesped;
    private String nombreHuesped;

    private int numeroHabitacion;
    private String tipoHabitacion;

    public static ReservaResponseDTO desde(Reserva r) {
        return new ReservaResponseDTO(
                r.getIdReserva(),
                r.getCodigoReserva(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.getAdultos(),
                r.getNinos(),
                r.getEstadoReserva(),
                r.getFechaCreacion(),
                r.getHuesped().getIdHuesped(),
                r.getHuesped().getNombre() + " " + r.getHuesped().getApellido(),
                r.getHabitacion().getNumeroHabitacion(),
                r.getHabitacion().getTipoHabitacion().name()
        );
    }
}
