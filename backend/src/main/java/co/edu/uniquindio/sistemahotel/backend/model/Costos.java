package co.edu.uniquindio.sistemahotel.backend.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Fuente ÚNICA de verdad de la aritmética de costos del sistema
 * Ninguna otra clase debe reimplementar estas fórmulas: la reserva, el consumo, la estadía y el
 * desglose de costos llaman a estos métodos, de modo que siempre coinciden entre sí.
 */
public final class Costos {

    //Decimales de precisión monetaria del prototipo
    public static final int ESCALA = 2;

    //Redondeo usado en todos los importes
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    //Cero ya expresado como importe monetario
    public static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA, REDONDEO);

    private Costos() {
    }

    /**
     * Indica si un importe es utilizable como precio. Rechaza NaN, infinitos, cero y negativos.
     * @param valor importe candidato
     * @return true si el importe puede usarse como precio
     */
    public static boolean esPrecioValido(double valor) {
        return Double.isFinite(valor) && valor > 0;
    }

    /**
     * Indica si un importe tiene como máximo dos decimales, es decir, si encaja en la precisión
     * monetaria del sistema.
     * @param valor importe candidato
     * @return true si el importe no tiene más de dos decimales significativos
     */
    public static boolean tienePrecisionValida(double valor) {
        if (!Double.isFinite(valor)) {
            return false;
        }
        return aMoneda(valor).compareTo(BigDecimal.valueOf(valor)) == 0;
    }

    /**
     * Convierte un importe double a un importe monetario de dos decimales.
     * @param valor importe de origen
     * @return importe monetario a escala 2
     */
    public static BigDecimal aMoneda(double valor) {
        if (!Double.isFinite(valor)) {
            return CERO;
        }
        return BigDecimal.valueOf(valor).setScale(ESCALA, REDONDEO);
    }

    /**
     * Convierte un importe monetario a double, para los campos de los modelos que ya son double.
     * @param valor importe monetario
     * @return equivalente en double, o 0.0 si es null
     */
    public static double aDouble(BigDecimal valor) {
        return valor == null ? 0.0 : valor.doubleValue();
    }

    /**
     * Calcula el número de noches entre la entrada y la salida.
     * @param inicio
     * @param fin
     * @return número de noches, o 0 si las fechas no permiten calcularlo
     */
    public static long noches(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(inicio, fin);
    }

    /**
     * Calcula el subtotal de alojamiento: número de noches x precio por noche.
     * @param noches      número de noches de la estadía
     * @param precioNoche precio configurado de la habitación
     * @return subtotal de alojamiento a escala 2
     */
    public static BigDecimal subtotalAlojamiento(long noches, double precioNoche) {
        if (noches <= 0 || !Double.isFinite(precioNoche)) {
            return CERO;
        }
        return aMoneda(precioNoche)
                .multiply(BigDecimal.valueOf(noches))
                .setScale(ESCALA, REDONDEO);
    }

    /**
     * Calcula el subtotal de una línea de consumo: cantidad x precio unitario.
     * El producto se redondea una sola vez, al final.
     * @param cantidad      cantidad de unidades
     * @param precioUnitario precio unitario del consumo
     * @return subtotal de la línea a escala 2
     */
    public static BigDecimal subtotalConsumo(int cantidad, double precioUnitario) {
        if (cantidad <= 0 || !Double.isFinite(precioUnitario)) {
            return CERO;
        }
        return BigDecimal.valueOf(precioUnitario)
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(ESCALA, REDONDEO);
    }

    /**
     * Suma una lista de importes
     * @param valores importes a sumar; los nulos se ignoran
     * @return suma exacta a escala 2
     */
    public static BigDecimal sumar(List<BigDecimal> valores) {
        if (valores == null || valores.isEmpty()) {
            return CERO;
        }
        BigDecimal total = CERO;
        for (BigDecimal valor : valores) {
            if (valor != null) {
                total = total.add(valor);
            }
        }
        return total.setScale(ESCALA, REDONDEO);
    }

    /**
     * Calcula el saldo pendiente de una cuenta
     * @param total  total general de la estadía
     * @param abonos importes ya APROBADOS; los pendientes, rechazados y reembolsados NO se incluyen, porque no son dinero recibido
     * @return saldo pendiente a escala 2
     */
    public static BigDecimal saldo(BigDecimal total, List<BigDecimal> abonos) {
        BigDecimal totalEscalado = total == null ? CERO : total.setScale(ESCALA, REDONDEO);
        BigDecimal diferencia = totalEscalado.subtract(sumar(abonos));
        return diferencia.max(CERO).setScale(ESCALA, REDONDEO);
    }
}
