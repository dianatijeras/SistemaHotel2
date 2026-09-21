package co.edu.uniquindio.sistemahotel.backend.service;

import co.edu.uniquindio.sistemahotel.backend.dto.ReservaRequestDTO;
import co.edu.uniquindio.sistemahotel.backend.enums.EstadoHabitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Habitacion;
import co.edu.uniquindio.sistemahotel.backend.model.Huesped;
import co.edu.uniquindio.sistemahotel.backend.model.Reserva;
import co.edu.uniquindio.sistemahotel.backend.repository.HuespedRepository;
import co.edu.uniquindio.sistemahotel.backend.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HuespedRepository huespedRepository;
    private final HabitacionService habitacionService;

    public ReservaService(ReservaRepository reservaRepository, HuespedRepository huespedRepository, HabitacionService habitacionService) {
        this.reservaRepository = reservaRepository;
        this.huespedRepository = huespedRepository;
        this.habitacionService = habitacionService;
    }


    public Reserva crearReserva(ReservaRequestDTO dto) {

        validarDatosReserva(dto);

        habitacionService.validarRangoFechas(dto.getFechaInicio(), dto.getFechaFin());

        Huesped huesped = huespedRepository.findById(dto.getIdHuesped()).orElseThrow(() -> new IllegalArgumentException("No existe el huésped con id: " + dto.getIdHuesped()));

        Habitacion habitacion = habitacionService.obtenerOFallar(dto.getNumeroHabitacion());

        validarEstadoHabitacion(habitacion);

        validarDisponibilidad(habitacion, dto.getFechaInicio(), dto.getFechaFin());

        validarCapacidad(habitacion, dto.getAdultos(), dto.getNinos());

        String idReserva = UUID.randomUUID().toString();

        String codigoReserva = generarCodigoReserva();

        Reserva reserva = new Reserva(idReserva, codigoReserva, dto.getFechaInicio(), dto.getFechaFin(), dto.getAdultos(), dto.getNinos(), huesped, habitacion);


        return reservaRepository.save(reserva);
    }



    private void validarDatosReserva(ReservaRequestDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la reserva son obligatorios.");
        }

        if (dto.getIdHuesped() == null || dto.getIdHuesped().isBlank()) {
            throw new IllegalArgumentException("El huésped es obligatorio.");
        }

        if (dto.getNumeroHabitacion() == null) {
            throw new IllegalArgumentException("La habitación es obligatoria.");
        }

        if (dto.getAdultos() < 1) {
            throw new IllegalArgumentException("La reserva debe tener al menos un adulto.");
        }

        if (dto.getNinos() < 0) {
            throw new IllegalArgumentException("El número de niños no puede ser negativo.");
        }
    }



    private void validarEstadoHabitacion(Habitacion habitacion) {

        EstadoHabitacion estado = habitacion.getEstadoHabitacion();

        if (estado == EstadoHabitacion.MANTENIMIENTO || estado == EstadoHabitacion.FUERA_DE_SERVICIO || estado == EstadoHabitacion.EN_LIMPIEZA) {
            throw new IllegalStateException("La habitación " + habitacion.getNumeroHabitacion() + " no puede reservarse porque está " + estado);
        }
    }



    private void validarDisponibilidad(Habitacion habitacion, LocalDate fechaInicio, LocalDate fechaFin) {
        boolean existeCruce = habitacionService.tieneSolapamiento(habitacion.getNumeroHabitacion(), fechaInicio, fechaFin, null);

        if (existeCruce) {
            throw new IllegalStateException("La habitación " + habitacion.getNumeroHabitacion() + " ya tiene una reserva " + "que se cruza con esas fechas.");
        }
    }



    private void validarCapacidad(Habitacion habitacion, int adultos, int ninos) {
        int totalPersonas = adultos + ninos;

        if (totalPersonas > habitacion.getCapacidad()) {
            throw new IllegalArgumentException("La habitación " + habitacion.getNumeroHabitacion() + " tiene capacidad para " + habitacion.getCapacidad() + " personas. Se solicitaron " + totalPersonas + ".");
        }
    }



    private String generarCodigoReserva() {
        int anio = LocalDate.now().getYear();

        int consecutivo = reservaRepository.findAll().size() + 1;

        return String.format("RES-%d-%06d", anio, consecutivo);
    }


    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }


    public Reserva obtenerOFallar(String idReserva) {
        return reservaRepository.findById(idReserva).orElseThrow(() -> new IllegalArgumentException("No existe la reserva con id: " + idReserva));
    }
}