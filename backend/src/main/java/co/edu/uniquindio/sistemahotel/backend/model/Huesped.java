package co.edu.uniquindio.sistemahotel.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Huesped {

    private String idHuesped;
    private String nombre;
    private String apellido;
    private String documentoIdentidad;
    private String telefono;
    private String email;
}
