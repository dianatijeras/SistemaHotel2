import { apiRequest } from "./api";
import {
    Consumo,
    ConsumoCosto,
    DashboardStats,
    Estadia,
    EstadiaCosto,
    EstadoHabitacion,
    Habitacion,
    Huesped,
    Pago,
    Reserva,
    SaldoPago,
} from "../types";
import { NuevoPersonalLimpiezaRequest, NuevoRecepcionistaRequest, Usuario } from "../types/auth";

/**
 * Funciones de mapeo para convertir las respuestas de la API en los tipos definidos en la aplicación.
 * @param h
 */
const mapHuesped = (h: any): Huesped => ({
    id: h.idHuesped ?? h.id,
    nombre: h.nombre,
    apellido: h.apellido,
    documento: h.documento ?? "",
    email: h.correo ?? h.email ?? "",
    telefono: h.telefono ?? "",
    username: h.username,
});

const mapHabitacion = (h: any): Habitacion => ({
    id: String(h.numeroHabitacion ?? h.id ?? h.numero),
    numero: String(h.numeroHabitacion ?? h.numero),
    tipo: h.tipoHabitacion ?? h.tipo,
    capacidad: h.capacidad,
    precioNoche: h.precioNoche,
    estado: h.estadoHabitacion ?? h.estado,
    descripcion: h.descripcion,
    piso: h.piso,
});

const mapReserva = (r: any): Reserva => ({
    id: r.idReserva ?? r.id,
    codigoReserva: r.codigoReserva ?? r.idReserva ?? r.id,
    idHuesped: r.huesped?.idHuesped ?? r.idHuesped ?? "",
    numeroHabitacion: String(r.habitacion?.numeroHabitacion ?? r.numeroHabitacion ?? ""),
    fechaInicio: r.fechaInicio,
    fechaFin: r.fechaFin,
    adultos: r.adultos,
    ninos: r.ninos,
    estado: r.estadoReserva ?? r.estado,
    huesped: r.huesped ? mapHuesped(r.huesped) : undefined,
    habitacion: r.habitacion ? mapHabitacion(r.habitacion) : undefined,
});

const mapConsumo = (c: any): Consumo => ({
    id: c.idConsumo ?? c.id,
    descripcion: c.descripcion,
    cantidad: c.cantidad,
    precioUnitario: c.precioUnitario,
    total: c.subtotal ?? c.total ?? c.cantidad * c.precioUnitario,
    fecha: c.fecha,
});

const mapConsumoCosto = (c: any): ConsumoCosto => ({
    id: c.idConsumo ?? c.id,
    descripcion: c.descripcion,
    cantidad: c.cantidad,
    precioUnitario: c.precioUnitario,
    subtotal: c.subtotal,
    fecha: c.fecha,
});

/** Convierte un importe numérico en texto con separador de miles y dos decimales (F-09). */
export const formatMoneda = (valor: number | string) => {
    const numero = typeof valor === "string" ? Number(valor) : valor;
    if (!Number.isFinite(numero)) return "$0.00";
    return "$" + numero.toLocaleString("es-CO", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
};

const mapEstadia = (e: any): Estadia => ({
    id: e.idEstadia ?? e.id,
    idReserva: e.reserva?.idReserva ?? e.idReserva ?? "",
    fechaInicio: e.fechaCheckIn ?? e.fechaInicio ?? "",
    fechaFin: e.fechaCheckOut ?? e.fechaFin ?? null,
    totalHabitacion: e.totalHabitacion,
    totalConsumos: e.totalConsumos,
    totalFinal: e.totalFinal,
    estado: e.estadoEstadia ?? e.estado,
    reserva: e.reserva ? mapReserva(e.reserva) : undefined,
    consumos: (e.consumos ?? []).map(mapConsumo),
});

const mapPago = (p: any): Pago => ({
    id: p.idPago ?? p.id,
    idEstadia: p.estadia?.idEstadia ?? p.idEstadia ?? "",
    monto: p.monto,
    metodoPago: p.metodoPago,
    referencia: p.referenciaPasarela ?? p.referencia ?? "",
    estado: p.estadoPago ?? p.estado,
    fecha: p.fechaPago ?? p.fecha,
    concepto: p.concepto ?? "",
    observaciones: p.observaciones ?? "",
});

/** Estado de cuenta de una estadía (F-10). Los importes llegan ya calculados del backend. */
const mapSaldo = (s: any): SaldoPago => ({
    idEstadia: s.idEstadia,
    idReserva: s.idReserva,
    numeroHabitacion: s.numeroHabitacion,
    totalGeneral: Number(s.totalGeneral ?? 0),
    totalAprobado: Number(s.totalAprobado ?? 0),
    saldoPendiente: Number(s.saldoPendiente ?? 0),
    cantidadPagosAprobados: s.cantidadPagosAprobados ?? 0,
    cantidadPagosPendientes: s.cantidadPagosPendientes ?? 0,
});

export const hotelService = {
    listarHuespedes: async () => (await apiRequest<any[]>("/huespedes")).map(mapHuesped),
    crearHuesped: async (data: { nombre: string; apellido: string; documento: string; telefono: string; correo: string; }) =>
        mapHuesped(await apiRequest<any>("/huespedes", { method: "POST", body: JSON.stringify(data) })),
    actualizarHuesped: async (id: string, data: { nombre?: string; apellido?: string; documento?: string; telefono: string; correo: string }) =>
        mapHuesped(await apiRequest<any>(`/huespedes/${id}`, { method: "PUT", body: JSON.stringify(data) })),
    eliminarHuesped: (id: string) => apiRequest<void>(`/huespedes/${id}`, { method: "DELETE" }),

    listarHabitaciones: async () => (await apiRequest<any[]>("/habitaciones")).map(mapHabitacion),
    /** Consulta la disponibilidad. Si se indican ambas fechas, el backend devuelve solo las
     *  habitaciones libres para ese intervalo; sin fechas devuelve las libres por estado. */
    listarHabitacionesDisponibles: async (fechaInicio?: string, fechaFin?: string, excluirReserva?: string) => {
        const parametros = fechaInicio && fechaFin
            ? `?fechaInicio=${encodeURIComponent(fechaInicio)}&fechaFin=${encodeURIComponent(fechaFin)}`
                + (excluirReserva ? `&excluirReserva=${encodeURIComponent(excluirReserva)}` : "")
            : "";
        return (await apiRequest<any[]>(`/habitaciones/disponibles${parametros}`)).map(mapHabitacion);
    },
    cambiarEstadoHabitacion: async (numero: string, estado: EstadoHabitacion) => {
        return mapHabitacion(await apiRequest<any>(`/habitaciones/${numero}/estado`, {
            method: "PUT",
            body: JSON.stringify({ estadoHabitacion: estado }),
        }));
    },

    listarReservas: async () => (await apiRequest<any[]>("/reservas")).map(mapReserva),
    /** Modifica fechas y habitación de una reserva existente (F-04). Los campos omitidos
     *  conservan su valor actual. */
    modificarReserva: async (id: string, data: { numeroHabitacion?: number; fechaInicio?: string; fechaFin?: string }) =>
        mapReserva(await apiRequest<any>(`/reservas/${id}`, { method: "PUT", body: JSON.stringify(data) })),
    crearReserva: async (data: { idHuesped: string; numeroHabitacion: number; fechaInicio: string; fechaFin: string; adultos: number; ninos: number; }) =>
        mapReserva(await apiRequest<any>("/reservas", { method: "POST", body: JSON.stringify(data) })),
    confirmarReserva: async (id: string) => mapReserva(await apiRequest<any>(`/reservas/${id}/confirmar`, { method: "PUT" })),
    cancelarReserva: async (id: string) => mapReserva(await apiRequest<any>(`/reservas/${id}/cancelar`, { method: "PUT" })),
    noShowReserva: async (id: string) => mapReserva(await apiRequest<any>(`/reservas/${id}/no-show`, { method: "PUT" })),

    listarEstadias: async () => (await apiRequest<any[]>("/estadias")).map(mapEstadia),
    listarEstadiasParaPago: async () => (await apiRequest<any[]>("/estadias/para-pagos")).map(mapEstadia),
    listarEstadiasEnCurso: async () => (await apiRequest<any[]>("/estadias/en-curso")).map(mapEstadia),
    buscarEstadia: async (id: string) => mapEstadia(await apiRequest<any>(`/estadias/${id}`)),
    /** Confirma que la limpieza de una habitación terminó y la libera (F-08). */
    finalizarLimpieza: async (numero: string) =>
        mapHabitacion(await apiRequest<any>(`/habitaciones/${numero}/finalizar-limpieza`, { method: "PUT" })),
    /** Desglose de costos de la estadía (F-09). El backend es la fuente de verdad de las fórmulas. */
    obtenerCostoEstadia: async (id: string): Promise<EstadiaCosto> => {
        const c = await apiRequest<any>(`/estadias/${id}/costo`);
        return {
            idEstadia: c.idEstadia,
            idReserva: c.idReserva,
            numeroHabitacion: c.numeroHabitacion,
            noches: c.noches,
            precioNoche: c.precioNoche,
            subtotalAlojamiento: c.subtotalAlojamiento,
            consumos: (c.consumos ?? []).map(mapConsumoCosto),
            subtotalConsumos: c.subtotalConsumos,
            totalGeneral: c.totalGeneral,
        };
    },
    checkIn: async (idReserva: string) => mapEstadia(await apiRequest<any>(`/estadias/checkin/${idReserva}`, { method: "POST" })),
    checkOut: async (idEstadia: string) => mapEstadia(await apiRequest<any>(`/estadias/${idEstadia}/checkout`, { method: "PUT" })),
    agregarConsumo: async (data: { idEstadia: string; descripcion: string; cantidad: number; precioUnitario: number; }) =>
        mapEstadia(await apiRequest<any>("/estadias/consumo", { method: "POST", body: JSON.stringify(data) })),

    listarPagos: async () => (await apiRequest<any[]>("/pagos")).map(mapPago),
    /** Estado de cuenta de una estadía: total, total aprobado y saldo pendiente (F-10). */
    consultarSaldo: async (idEstadia: string): Promise<SaldoPago> =>
        mapSaldo(await apiRequest<any>(`/pagos/estadia/${idEstadia}/saldo`)),
    crearPago: async (data: { idEstadia: string; monto: number; metodoPago: string; concepto: string; referenciaPasarela?: string; }) =>
        mapPago(await apiRequest<any>("/pagos", { method: "POST", body: JSON.stringify(data) })),
    aprobarPago: async (id: string, referencia: string) => mapPago(await apiRequest<any>(`/pagos/${id}/aprobar`, { method: "PUT", body: JSON.stringify({ referencia }) })),
    rechazarPago: async (id: string, observaciones: string) => mapPago(await apiRequest<any>(`/pagos/${id}/rechazar`, { method: "PUT", body: JSON.stringify({ observaciones }) })),
    reembolsarPago: async (id: string) => mapPago(await apiRequest<any>(`/pagos/${id}/reembolsar`, { method: "PUT" })),

    listarUsuarios: () => apiRequest<Usuario[]>("/usuarios"),
    crearRecepcionista: (data: NuevoRecepcionistaRequest) => apiRequest<Usuario>("/usuarios/recepcionistas", { method: "POST", body: JSON.stringify(data) }),
    crearPersonalLimpieza: (data: NuevoPersonalLimpiezaRequest) => apiRequest<Usuario>("/usuarios/personal-limpieza", { method: "POST", body: JSON.stringify(data) }),
    cambiarEstadoUsuario: (id: string) => apiRequest<Usuario>(`/usuarios/${id}/estado`, { method: "PUT" }),

    obtenerDashboardStats: () => apiRequest<DashboardStats>("/dashboard/stats"),
};
