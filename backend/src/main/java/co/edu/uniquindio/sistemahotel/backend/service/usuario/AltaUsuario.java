package co.edu.uniquindio.sistemahotel.backend.service.usuario;

import co.edu.uniquindio.sistemahotel.backend.enums.TurnoRecepcionista;

public record AltaUsuario(
        String nombre,
        String apellido,
        String username,
        String password,
        TurnoRecepcionista turno) {

    /**
     * Devuelve el username ya recortado, que es como se compara en las reglas de unicidad.
     * @return
     */
    public String usernameNormalizado() {
        return username == null ? null : username.trim();
    }
}