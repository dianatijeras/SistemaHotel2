package co.edu.uniquindio.sistemahotel.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Envía la notificación fuera del hilo de la petición HTTP.
 * Se registra únicamente si MAIL_ENABLED=true. No modifica reservas.
 */
@Component
@ConditionalOnProperty(name = "hotel.notificaciones.correo.habilitado", havingValue = "true")
public class ReservaConfirmadaEmailListener {

    private static final Logger LOG = LoggerFactory.getLogger(ReservaConfirmadaEmailListener.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final JavaMailSender mailSender;
    private final String remitente;

    public ReservaConfirmadaEmailListener(JavaMailSender mailSender,
                                          @Value("${spring.mail.username:}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Async("correoNotificacionesExecutor")
    @EventListener
    public void enviarAlConfirmar(ReservaConfirmadaEvent evento) {
        String destinatario = evento.correoHuesped();
        if (destinatario == null || !destinatario.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            LOG.warn("Se omite correo de confirmación {}: el huésped no tiene un email válido.",
                    evento.codigoReserva());
            return;
        }
        if (remitente == null || remitente.isBlank()) {
            LOG.warn("Se omite correo de confirmación {}: falta MAIL_USERNAME.",
                    evento.codigoReserva());
            return;
        }

        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setFrom(remitente);
        correo.setTo(destinatario);
        correo.setSubject("Reserva confirmada - SistemaHotelManager | " + evento.codigoReserva());
        correo.setText("Hola, " + evento.nombreHuesped() + ":\n\n"
                + "Tu reserva ha sido CONFIRMADA.\n\n"
                + "Código: " + evento.codigoReserva() + "\n"
                + "Habitación: " + evento.numeroHabitacion() + "\n"
                + "Fecha de entrada: " + evento.fechaEntrada().format(FECHA) + "\n"
                + "Fecha de salida: " + evento.fechaSalida().format(FECHA) + "\n\n"
                + "Gracias por elegir nuestro hotel.\n"
                + "SistemaHotelManager");
        try {
            mailSender.send(correo);
            LOG.info("Correo de confirmación solicitado al servidor SMTP para reserva {}.",
                    evento.codigoReserva());
        } catch (MailException ex) {
            // No afecta la reserva, que ya se confirmó. No registrar credenciales ni contenido.
            LOG.warn("Falló el envío SMTP de la confirmación {}: {}",
                    evento.codigoReserva(), ex.getMessage());
        }
    }
}
