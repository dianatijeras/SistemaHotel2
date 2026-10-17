import { EstadoReserva, EstadoHabitacion, EstadoPago } from "../../types";

interface StatusBadgeProps {
  status: EstadoReserva | EstadoHabitacion | EstadoPago | string;
  type?: 'reserva' | 'habitacion' | 'pago';
}

/**
 * Componente para mostrar el estado de una reserva, habitación o pago con un badge de colores.
 * @param status
 * @param type
 * @constructor
 */
export function StatusBadge({ status, type = 'reserva' }: StatusBadgeProps) {
  const getStyles = () => {
    if (type === 'habitacion') {
      switch (status) {
        case EstadoHabitacion.DISPONIBLE:
          return 'bg-green-100 text-green-800';
        case EstadoHabitacion.OCUPADA:
          return 'bg-red-100 text-red-800';
        case EstadoHabitacion.EN_LIMPIEZA:
          return 'bg-yellow-100 text-yellow-800';
        case EstadoHabitacion.MANTENIMIENTO:
          return 'bg-gray-100 text-gray-800';
        default:
          return 'bg-gray-100 text-gray-800';
      }
    }

    if (type === 'pago') {
      switch (status) {
        case EstadoPago.PAGADO:
          return 'bg-green-100 text-green-800';
        case EstadoPago.PENDIENTE:
          return 'bg-yellow-100 text-yellow-800';
        case EstadoPago.RECHAZADO:
          return 'bg-red-100 text-red-800';
        case EstadoPago.REEMBOLSADO:
          return 'bg-blue-100 text-blue-800';
        default:
          return 'bg-gray-100 text-gray-800';
      }
    }

    // reserva
    switch (status) {
      case EstadoReserva.CONFIRMADA:
        return 'bg-green-100 text-green-800';
      case EstadoReserva.RESERVADA:
        return 'bg-yellow-100 text-yellow-800';
      case EstadoReserva.CANCELADA:
        return 'bg-red-100 text-red-800';
      case EstadoReserva.NO_SHOW:
        return 'bg-gray-100 text-gray-800';
      case EstadoReserva.FINALIZADA:
      case EstadoReserva.CHECKED_IN:
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStyles()}`}>
      {status}
    </span>
  );
}
