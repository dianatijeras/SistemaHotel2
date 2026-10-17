export enum EstadoReserva {
    RESERVADA = 'RESERVADA',
    CONFIRMADA = 'CONFIRMADA',
    CANCELADA = 'CANCELADA',
    NO_SHOW = 'NO_SHOW',
    CHECKED_IN = 'CHECKED_IN',
    FINALIZADA = 'FINALIZADA'
}

export enum EstadoHabitacion {
    DISPONIBLE = 'DISPONIBLE',
    RESERVADA = 'RESERVADA',
    OCUPADA = 'OCUPADA',
    EN_LIMPIEZA = 'EN_LIMPIEZA',
    MANTENIMIENTO = 'MANTENIMIENTO',
    FUERA_DE_SERVICIO = 'FUERA_DE_SERVICIO'
}

export enum EstadoPago {
    PENDIENTE = 'PENDIENTE',
    PAGADO = 'PAGADO',
    RECHAZADO = 'RECHAZADO',
    REEMBOLSADO = 'REEMBOLSADO'
}

export enum MetodoPago {
    TARJETA_DEBITO = 'TARJETA_DEBITO',
    TARJETA_CREDITO = 'TARJETA_CREDITO',
    TRANSFERENCIA = 'TRANSFERENCIA',
    EFECTIVO = 'EFECTIVO',
    PSE = 'PSE'
}

export enum TipoHabitacion {
    ESTANDAR = 'ESTANDAR',
    SUPERIOR = 'SUPERIOR',
    DELUXE = 'DELUXE',
    SUITE = 'SUITE'
}

export interface Huesped {
    id: string;
    nombre: string;
    apellido: string;
    documento: string;
    email: string;
    telefono: string;
    username?: string;
}

export interface Habitacion {
    id: string;
    numero: string;
    tipo: TipoHabitacion;
    capacidad: number;
    precioNoche: number;
    estado: EstadoHabitacion;
    descripcion?: string;
    piso?: number;
}

export interface Reserva {
    id: string;
    codigoReserva: string;
    idHuesped: string;
    numeroHabitacion: string;
    fechaInicio: string;
    fechaFin: string;
    adultos: number;
    ninos: number;
    estado: EstadoReserva;
    huesped?: Huesped;
    habitacion?: Habitacion;
}

export interface Consumo {
    id: string;
    descripcion: string;
    cantidad: number;
    precioUnitario: number;
    total: number;
    fecha?: string;
}

export interface Estadia {
    id: string;
    idReserva: string;
    fechaInicio: string;
    fechaFin: string | null;
    totalHabitacion: number;
    totalConsumos: number;
    totalFinal: number;
    estado: string;
    reserva?: Reserva;
    consumos?: Consumo[];
}

/**
 * Línea de consumo dentro del desglose de costos (F-09). El subtotal lo calcula el backend:
 * React nunca multiplica por su cuenta. El identificador se llama `id` (como en `Consumo`) para
 * que la tabla compartida lo use como clave de React.
 */
export interface ConsumoCosto {
    id: string;
    descripcion: string;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
    fecha?: string;
}

/**
 * Desglose de costos de una estadía (F-09), devuelto por GET /api/estadias/{id}/costo.
 * Muestra cada componente del cálculo por separado para que el total general sea explicable.
 */
export interface EstadiaCosto {
    idEstadia: string;
    idReserva: string;
    numeroHabitacion?: number;
    noches: number;
    precioNoche: number;
    subtotalAlojamiento: number;
    consumos: ConsumoCosto[];
    subtotalConsumos: number;
    totalGeneral: number;
}

export interface Pago {
    id: string;
    idEstadia: string;
    monto: number;
    metodoPago: MetodoPago;
    referencia: string;
    estado: EstadoPago;
    fecha: string;
    concepto?: string;
    observaciones?: string;
}

/**
 * Estado de cuenta de una estadía (F-10), devuelto por
 * GET /api/pagos/estadia/{id}/saldo. Todos los importes los calcula el backend; React solo
 * los presenta.
 */
export interface SaldoPago {
    idEstadia: string;
    idReserva?: string;
    numeroHabitacion?: number;
    totalGeneral: number;
    totalAprobado: number;
    saldoPendiente: number;
    cantidadPagosAprobados: number;
    cantidadPagosPendientes: number;
}


export interface DashboardStats {
    totalHabitaciones: number;
    habitacionesDisponibles: number;
    habitacionesOcupadas: number;
    habitacionesEnLimpieza: number;
    totalHuespedes: number;
    totalReservas: number;
    reservasActivas: number;
    reservasEnProceso: number;
    estadiasEnCurso: number;
    pagosPendientes: number;
    totalPagado: number;
    checkoutsHoy: number;
    totalUsuarios: number;
    totalRecepcionistas: number;
    totalPersonalLimpieza: number;
}
