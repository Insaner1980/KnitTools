package com.finnvek.knittools.domain.calculator

import com.finnvek.knittools.domain.model.KnittingAbbreviation

object AbbreviationData {
    val abbreviations: List<KnittingAbbreviation> =
        listOf(
            // Perussilmukat
            KnittingAbbreviation("K", "Knit", "Insert needle knitwise, wrap yarn, pull through"),
            KnittingAbbreviation("P", "Purl", "Insert needle purlwise, wrap yarn, pull through"),
            KnittingAbbreviation("St(s)", "Stitch(es)", "One or more loops on the needle"),
            KnittingAbbreviation("RS", "Right side", "The public-facing side of the work"),
            KnittingAbbreviation("WS", "Wrong side", "The private/back side of the work"),
            KnittingAbbreviation(
                "YO",
                "Yarn over",
                "Wrap yarn around needle to create a new stitch and decorative hole",
            ),
            KnittingAbbreviation("Kwise", "Knitwise", "Insert needle as if to knit"),
            KnittingAbbreviation("Pwise", "Purlwise", "Insert needle as if to purl"),
            // Vähenykset
            KnittingAbbreviation("K2tog", "Knit 2 together", "Right-leaning decrease: knit two stitches as one"),
            KnittingAbbreviation("P2tog", "Purl 2 together", "Purl two stitches together as one"),
            KnittingAbbreviation(
                "SSK",
                "Slip, slip, knit",
                "Left-leaning decrease: slip 2 stitches knitwise, knit them together through back loops",
            ),
            KnittingAbbreviation("SSP", "Slip, slip, purl", "Left-leaning purl decrease"),
            KnittingAbbreviation(
                "SKP",
                "Slip, knit, pass slipped stitch over",
                "Left-leaning decrease, alternative to SSK",
            ),
            KnittingAbbreviation("K3tog", "Knit 3 together", "Right-leaning double decrease"),
            KnittingAbbreviation("P3tog", "Purl 3 together", "Purl three stitches together"),
            KnittingAbbreviation("S2KP", "Slip 2, knit 1, pass slipped stitches over", "Centered double decrease"),
            KnittingAbbreviation(
                "SK2P",
                "Slip 1, knit 2 together, pass slipped stitch over",
                "Left-leaning double decrease",
            ),
            KnittingAbbreviation(
                "CDD",
                "Central double decrease",
                "Slip 2 together knitwise, K1, pass slipped stitches over",
            ),
            KnittingAbbreviation("Psso", "Pass slipped stitch over", "Lift slipped stitch over the last worked stitch"),
            KnittingAbbreviation("Dec", "Decrease", "Reduce the number of stitches"),
            // Lisäykset
            KnittingAbbreviation("M1", "Make 1", "Pick up bar between stitches and knit through back loop to increase"),
            KnittingAbbreviation("M1L", "Make 1 left", "Left-leaning increase using the bar between stitches"),
            KnittingAbbreviation("M1R", "Make 1 right", "Right-leaning increase using the bar between stitches"),
            KnittingAbbreviation("KFB", "Knit front and back", "Knit into front and back of same stitch to increase"),
            KnittingAbbreviation("PFB", "Purl front and back", "Purl into front and back of same stitch to increase"),
            KnittingAbbreviation("Inc", "Increase", "Add stitches to the work"),
            // Siirtäminen
            KnittingAbbreviation("SL", "Slip", "Move stitch from left to right needle without working it"),
            KnittingAbbreviation("SL1K", "Slip 1 knitwise", "Slip one stitch as if to knit"),
            KnittingAbbreviation("SL1P", "Slip 1 purlwise", "Slip one stitch as if to purl"),
            // Aloitus ja lopetus
            KnittingAbbreviation("CO", "Cast on", "Create initial stitches on the needle"),
            KnittingAbbreviation("BO", "Bind off", "Secure stitches to prevent unraveling"),
            KnittingAbbreviation("PU", "Pick up", "Pick up stitches along an edge"),
            // Merkit ja toisto
            KnittingAbbreviation("PM", "Place marker", "Place a stitch marker on the needle"),
            KnittingAbbreviation("SM", "Slip marker", "Move marker from left to right needle"),
            KnittingAbbreviation("Rep", "Repeat", "Work the specified section again"),
            KnittingAbbreviation("Rnd", "Round", "One complete circuit in circular knitting"),
            KnittingAbbreviation("EOR", "Every other row", "Work on alternate rows"),
            // Neulat
            KnittingAbbreviation(
                "DPN",
                "Double-pointed needles",
                "Short needles pointed at both ends, used for small circumference knitting",
            ),
            KnittingAbbreviation("CN", "Cable needle", "Short needle used to hold stitches when working cables"),
            KnittingAbbreviation("Circ", "Circular needle", "Two needle tips connected by a cable"),
            // Twisted-silmukat
            KnittingAbbreviation("Tbl", "Through back loop", "Work into the back leg of the stitch"),
            KnittingAbbreviation("Ktbl", "Knit through back loop", "Twisted knit stitch"),
            KnittingAbbreviation("Ptbl", "Purl through back loop", "Twisted purl stitch"),
            // Kaapelit
            KnittingAbbreviation("C4F", "Cable 4 front", "Slip 2 to CN and hold in front, K2, K2 from CN"),
            KnittingAbbreviation("C4B", "Cable 4 back", "Slip 2 to CN and hold in back, K2, K2 from CN"),
            KnittingAbbreviation("C6F", "Cable 6 front", "Slip 3 to CN and hold in front, K3, K3 from CN"),
            KnittingAbbreviation("C6B", "Cable 6 back", "Slip 3 to CN and hold in back, K3, K3 from CN"),
            // Lankasijainti
            KnittingAbbreviation("Wyif", "With yarn in front", "Hold yarn to the front (purl side) of work"),
            KnittingAbbreviation("Wyib", "With yarn in back", "Hold yarn to the back (knit side) of work"),
            // Short rows
            KnittingAbbreviation("W&T", "Wrap and turn", "Wrap the next stitch and turn work for short row shaping"),
            // Neulontatyylit
            KnittingAbbreviation(
                "St st",
                "Stockinette stitch",
                "Knit on RS, purl on WS (or knit every round in circular)",
            ),
            KnittingAbbreviation("Rev St st", "Reverse stockinette", "Purl on RS, knit on WS"),
            KnittingAbbreviation("G st", "Garter stitch", "Knit every row (or knit/purl alternate rounds in circular)"),
            KnittingAbbreviation("Seed st", "Seed stitch", "Alternating K1, P1 with offset each row"),
            // Yleiset termit
            KnittingAbbreviation("Tog", "Together", "Work stitches together as one"),
            KnittingAbbreviation("Rem", "Remaining", "Stitches left on the needle"),
            KnittingAbbreviation("Beg", "Beginning", "Start of the row or round"),
            KnittingAbbreviation("Alt", "Alternate", "Every other row or stitch"),
            KnittingAbbreviation("Approx", "Approximately", "Close to but not exact"),
            KnittingAbbreviation("Cont", "Continue", "Keep working as established"),
            KnittingAbbreviation("Foll", "Following", "The next row, round, or instruction"),
            KnittingAbbreviation("Prev", "Previous", "The row, round, or stitch before the current one"),
            KnittingAbbreviation("Patt", "Pattern", "Continue in established pattern"),
            KnittingAbbreviation("Sk", "Skip", "Miss the next stitch"),
            KnittingAbbreviation(
                "Selvage",
                "Selvage/selvedge stitch",
                "Edge stitch worked differently for a neat border",
            ),
            KnittingAbbreviation("LH", "Left hand", "Left needle or left side"),
            KnittingAbbreviation("RH", "Right hand", "Right needle or right side"),
            // Värit
            KnittingAbbreviation("MC", "Main color", "The primary yarn color in a project"),
            KnittingAbbreviation("CC", "Contrast color", "The secondary yarn color"),
            // Mittaus
            KnittingAbbreviation("WPI", "Wraps per inch", "Yarn thickness measurement: wraps around a ruler per inch"),
            KnittingAbbreviation("Gauge", "Gauge/tension", "Number of stitches and rows per unit of measurement"),
            // Yhteisötermit
            KnittingAbbreviation("FO", "Finished object", "A completed knitting project"),
            KnittingAbbreviation("WIP", "Work in progress", "A project currently being knitted"),
            KnittingAbbreviation("UFO", "Unfinished object", "An abandoned or paused project"),
            KnittingAbbreviation("Frog", "Frog/frogging", "Rip out knitting (rip-it, rip-it — like a frog)"),
            KnittingAbbreviation("Tink", "Tink", "Unknit stitch by stitch (knit spelled backwards)"),
            KnittingAbbreviation("LYS", "Local yarn store", "A neighborhood yarn shop"),
        )

    fun search(query: String): List<KnittingAbbreviation> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return abbreviations
        return abbreviations.filter {
            it.abbreviation.lowercase().contains(trimmed) ||
                it.meaning.lowercase().contains(trimmed)
        }
    }
}
