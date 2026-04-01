package com.finnvek.knittools.util.extensions

private const val CM_PER_INCH = 2.54
private const val METERS_PER_YARD = 0.9144

fun cmToInches(cm: Double): Double = cm / CM_PER_INCH

fun inchesToCm(inches: Double): Double = inches * CM_PER_INCH

fun metersToYards(meters: Double): Double = meters / METERS_PER_YARD

fun yardsToMeters(yards: Double): Double = yards * METERS_PER_YARD

fun convertFieldValue(
    value: String,
    toImperial: Boolean,
    isLength: Boolean = true,
): String {
    val num = value.toDoubleOrNull() ?: return value
    if (num == 0.0) return value
    val converted = if (isLength) {
        if (toImperial) cmToInches(num) else inchesToCm(num)
    } else {
        if (toImperial) metersToYards(num) else yardsToMeters(num)
    }
    return "%.1f".format(converted)
}

fun convertGaugeValue(
    value: String,
    toImperial: Boolean,
): String {
    val num = value.toDoubleOrNull() ?: return value
    if (num == 0.0) return value
    // Gauge: stitches per 10cm ↔ stitches per 4 inches
    // st/inch = st_per_10cm / (10 / 2.54) = st_per_10cm / 3.937
    // st_per_4in = st/inch * 4 = st_per_10cm * 4 / (10 / 2.54) = st_per_10cm * 4 * 2.54 / 10
    // = st_per_10cm * 1.016
    // Esim: 22 st/10cm → 22 * 1.016 = 22.4 st/4in (oikein, koska 4in ≈ 10.16cm)
    // Esim: 68 st/10cm → 68 * 1.016 = 69.1 st/4in — tämä on matemaattisesti oikein!
    //
    // MUTTA neulojat ajattelevat toisin:
    // 68 st / 10 cm = 6.8 st/cm = 17.3 st/inch → 4 in = 69.1 st — OK tämä on oikein
    //
    // Alkuperäinen koodi on oikein. Ongelma oli siinä ettei se näyttänyt isolta muutokselta
    // koska 10 cm ≈ 4 in. Arvot OVAT oikein.
    val converted = if (toImperial) {
        num * (4.0 * CM_PER_INCH) / 10.0
    } else {
        num * 10.0 / (4.0 * CM_PER_INCH)
    }
    return "%.1f".format(converted)
}
