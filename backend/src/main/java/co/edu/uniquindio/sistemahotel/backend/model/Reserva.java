package co.edu.uniquindio.sistemahotel.backend.model;

import co.edu.uniquindio.sistemahotel.backend.enums.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa una reserva hecha por un huésped sobre una habitación,
 * en un rango de fechas determinado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    private String idReserva;
    private String codigoReserva;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int adultos;
    private int ninos;
    private Huesped huesped;
    private Habitacion habitacion;
    private EstadoReserva estadoReserva;
    private LocalDateTime fechaCreacion;

    public Reserva(String idReserva, String codigoReserva, LocalDate fechaInicio, LocalDate fechaFin, int adultos, int ninos, Huesped huesped, Habitacion habitacion) {
        this.idReserva = idReserva;
        this.codigoReserva = codigoReserva;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.adultos = adultos;
        this.ninos = ninos;
        this.huesped = huesped;
        this.habitacion = habitacion;
        this.estadoReserva = EstadoReserva.RESERVADA;
        this.fechaCreacion = LocalDateTime.now();
    }
}
