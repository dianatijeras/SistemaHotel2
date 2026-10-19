package co.edu.uniquindio.sistemahotel.backend.notification;

import java.time.LocalDate;

/** Foto inmutable de los datos necesarios cuando una reserva PASA a CONFIRMADA. */
public record ReservaConfirmadaEvent(
        String correoHuesped,
        String nombreHuesped,
        String codigoReserva,
        int numeroHabitacion,
        LocalDate fechaEntrada,
        LocalDate fechaSalida
) { }
