/**
 * Presenta una fecha/hora de la API en el horario local del navegador.
 * Spring Boot envía LocalDateTime sin zona: se interpreta como hora local,
 * no se inventa un desplazamiento UTC. No modifica los valores del modelo.
 */
export function formatoFechaHoraLocal(valor?: string | null): string {
    if (!valor) return "—";
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return valor;
    return fecha.toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
}
