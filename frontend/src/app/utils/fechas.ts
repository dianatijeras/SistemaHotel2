/**
 * Utilidades de fecha compartidas por las pantallas.
 */

/**
 * Devuelve la fecha de hoy en formato AAAA-MM-DD usando la fecha local del equipo.
 *
 * No se usa `new Date().toISOString()` a propósito: ese método convierte a UTC, de modo que en
 * cualquier zona horaria detrás de UTC (por ejemplo, UTC-5) el resultado es un día adelantado
 * entre las 19:00 y la medianoche. Con ese valor, el campo de fechas imponía `min` con el día
 * siguiente y la validación rechazaba la entrada de hoy como "en el pasado", impidiendo registrar
 * la reserva de un huésped que llega en el momento.
 * @returns la fecha local actual en formato AAAA-MM-DD
 */
export function fechaLocalISO(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/**
 * Ajusta una fecha y hora local que el backend entrega como `AAAA-MM-DDTHH:MM:SS.nnnnnnn`,
 * donde la fracción de segundo tiene siete dígitos, a una forma que todos los navegadores
 * interpretan igual. Se recorta la fracción a tres dígitos porque el motor de JavaScript solo
 * garantiza hasta esa precisión y algunos navegadores rechazan la cadena completa.
 *
 * No se añade ninguna zona horaria a propósito: la cadena no la trae y, según la norma, una
 * fecha y hora sin zona se interpreta como hora local, que es exactamente el valor de negocio
 * registrado por el hotel. Añadir `Z` desplazaría la hora y mostraría un momento equivocado.
 * @param iso fecha y hora tal como la entrega el backend
 * @returns la misma fecha y hora en un formato que el motor de JavaScript siempre acepta
 */
function normalizarFechaHoraLocal(iso: string): string {
    return iso
        .trim()
        .replace("T", " ")
        .replace(/(\.\d{3})\d+/, "$1");
}

/**
 * Presenta una fecha y hora del backend en formato legible y en la zona horaria local del
 * equipo, sin alterar el instante de negocio que representa.
 * @param iso fecha y hora tal como la entrega el backend
 * @returns la fecha y hora formateada, o una cadena vacía si no se recibió ninguna
 */
export function formatoFechaHoraLocal(iso: string): string {
    if (!iso) return "";
    const d = new Date(normalizarFechaHoraLocal(iso));
    if (isNaN(d.getTime())) return iso;
    return d.toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
}
