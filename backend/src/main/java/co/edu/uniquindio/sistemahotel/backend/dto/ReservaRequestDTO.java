package co.edu.uniquindio.sistemahotel.backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservaRequestDTO {

    private String idHuesped;

    private Integer numeroHabitacion;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private int adultos;

    private int ninos;
}
