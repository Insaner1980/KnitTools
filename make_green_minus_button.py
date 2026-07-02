"""
make_green_minus_button.py

Muuntaa oranssin plus-napin (orange_plus_button.png) vihreäksi miinus-napiksi
PIKSELITASOLLA — ei generointia, ei uudelleenpiirtoa.

Vain kaksi muutosta alkuperäiseen oranssiin nappiin:
  1. Värimaailma vaihdetaan vihreäksi (color transfer Lab-avaruudessa,
     luminanssirakenne säilyttäen) current_green_minus_button.png:n paletista.
  2. Kaiverretun plus-merkin pystyveto poistetaan ja pinta korjataan
     content-aware inpaintingilla -> jäljelle jää kaiverrettu miinus-merkki.

Kaikki napin geometria, reunus, sisäkehä, varjot, korostukset, 3D-syvyys,
kanvaskoko ja alfakanava periytyvät sellaisinaan oranssista lähteestä.
"""

import cv2
import numpy as np
from PIL import Image

# ============================================================================
# SÄÄDETTÄVÄT ARVOT  (kaikki tunable-vakiot tässä yläosassa)
# ============================================================================

# --- Polut ---
ORANGE_PLUS_PATH = "orange_plus_button.png"          # rakenteen lähde
GREEN_REF_PATH = "current_green_minus_button.png"    # tarkka värireferenssi
OUTPUT_PATH = "final_green_minus_button.png"

# --- Pystyvedon maski (kaiverretun plus-merkin pystyveto) ---
# Mitattu lähteestä: pystyveto x 233..263, koko pystysuunnan ulottuvuus y 156..331.
# Vaakaveto (= tuleva miinus) y 220..251 SÄILYTETÄÄN koskemattomana.
ARM_X_MIN = 225          # pystyvedon vasen reuna - padding kattaa viisteen/AA-pikselit
ARM_X_MAX = 271          # pystyvedon oikea reuna + padding
# Kaiverrettu pystyveto alkaa todellisuudessa vasta ~y=143 (mitattu: keskisarake
# x=248 on puhdasta lattiaa y<=142, ura alkaa y=144). Aloitetaan maski/silta
# hieman ylempää (y=138) jotta kärjen viiste/AA varmasti mukaan, mutta EI enää
# y=126:sta — muuten silta ylikirjoittaa puhdasta kaarevaa lattiaa ja jättää
# tumman painauman (näkyvä "naarmu").
ARM_Y_TOP = 138          # maskin/siltauksen yläraja - juuri kaiverruksen kärjen yllä
ARM_Y_BOTTOM = 337       # pystyvedon alapää (veto loppuu ~y332)
BAR_TOP = 220            # vaakavedon yläreuna -> säilytetään tästä alkaen
BAR_BOTTOM = 251         # vaakavedon alareuna -> säilytetään tähän asti

# --- Maskin pehmennys ja inpaint ---
MASK_DILATE = 4          # maskin laajennus (px) jotta AA-reunapikselit varmasti mukaan
MASK_FEATHER = 2.5       # gaussian-sumennuksen sigma reunan pehmeään sulautukseen
INPAINT_RADIUS = 15      # cv2.inpaint säde

# --- Rakennetietoinen siltakorjaus (poistaa cv2.inpaintin keskisauman) ---
# cv2.inpaint jättää korkeaan kapeaan alueeseen heikon keskisauman ja kuhmuja
# vaakavedon reunoihin. Korjataan siltaamalla pystyvedon alue vaakasuunnassa:
# rivikohtainen lineaari-interpolaatio uran molemmin puolin olevien puhtaiden
# sarakkeiden välillä.
#
# TÄRKEÄÄ: sisäkehän pohja on KAAREVA kuppi, ei vaakagradientti — keskikohta on
# kirkkaampi kuin suora viiva kaukaisten reunasarakkeiden välillä. Siksi lähde-
# sarakkeiden on oltava LÄHELLÄ uraa (pieni BRIDGE_PAD): mitä leveämpi silta,
# sitä suurempi kaarevuusvirhe (leveä silta x=209..287 tuotti jopa -26 yksikön
# tumman painauman = näkyvä naarmu). Lähellä uraa (x~222/274) virhe on ~0..8 ja
# sarakkeet ovat silti puhtaita (~213) myös kaiverretuilla riveillä.
REPAIR_BAR_EDGES = True
# Siltauksen lähdesarakkeet otetaan tämän verran ulompaa kuin pystyvedon maski.
# Pieni arvo pitää kaarevuusvirheen minimissä; mittausten mukaan x~222/274 ovat
# puhtaita eikä AA-varjo tartu.
BRIDGE_PAD = 2
# Siltauksen ylä-/alareunan pystysuora pehmennys (riviä), poistaa vaakasauman.
BRIDGE_FEATHER = 9

# --- Värin siirto ---
COLOR_TRANSFER_STRENGTH = 1.0   # 0.0 = pysyy oranssina, 1.0 = täysi vihreä siirto

# --- Debug-tiedostot ---
DEBUG_MASK = "debug_vertical_stroke_mask.png"
DEBUG_INPAINTED = "debug_inpainted_orange_minus.png"
DEBUG_PALETTE = "debug_green_palette_preview.png"

# Läpinäkyvyysraja: tätä läpinäkyvämmät pikselit ovat "nappia"
OPAQUE_THRESHOLD = 200


# ============================================================================
# Apufunktiot
# ============================================================================

def load_rgba(path):
    """Lataa kuva RGBA-numpy-taulukkona (Pillow, alfa säilyy)."""
    return np.array(Image.open(path).convert("RGBA"))


def build_vertical_stroke_mask(shape):
    """Rakentaa binäärimaskin plus-merkin pystyvedolle.

    Maski kattaa pystyvedon vaakavedon YLÄ- ja ALAPUOLELLA, mutta jättää
    vaakavedon (tuleva miinus) sekä keskiristeyksen koskematta.
    """
    h, w = shape[:2]
    mask = np.zeros((h, w), dtype=np.uint8)
    # Yläsegmentti: pystyvedon yläpäästä vaakavedon yläreunaan (ei sen yli)
    mask[ARM_Y_TOP:BAR_TOP, ARM_X_MIN:ARM_X_MAX] = 255
    # Alasegmentti: vaakavedon alareunasta pystyvedon alapäähän
    mask[BAR_BOTTOM + 1:ARM_Y_BOTTOM, ARM_X_MIN:ARM_X_MAX] = 255

    if MASK_DILATE > 0:
        k = cv2.getStructuringElement(
            cv2.MORPH_ELLIPSE, (MASK_DILATE * 2 + 1, MASK_DILATE * 2 + 1)
        )
        mask = cv2.dilate(mask, k)
    # Varmista ettei dilataatio syö vaakavetoa: nollaa säilytettävä kaista
    mask[BAR_TOP:BAR_BOTTOM + 1, :] = 0
    return mask


def repair_bar_edges(rgb):
    """Siltaa koko pystyvedon alue vaakasuunnassa saumattoman tuloksen takaamiseksi.

    Käyttää alueen molemmin puolin olevia puhtaita sarakkeita reuna-arvoina ja
    interpoloi rivikohtaisesti niiden välillä -> sileä pinta vaakavedon ylä- ja
    alapuolelle sekä suorat reunat vaakavedolle (tuleva miinus).
    """
    src = rgb.astype(np.float32)
    out = src.copy()
    y0, y1 = ARM_Y_TOP, ARM_Y_BOTTOM
    xl, xr = ARM_X_MIN - 1 - BRIDGE_PAD, ARM_X_MAX + 1 + BRIDGE_PAD
    left = src[y0:y1, xl]                            # (rows,3)
    right = src[y0:y1, xr]                           # (rows,3)
    width = xr - xl

    # Rakenna siltattu alue: rivikohtainen vaakainterpolaatio xl..xr.
    bridged = src[y0:y1].copy()
    for i, x in enumerate(range(xl, xr + 1)):
        t = i / width
        bridged[:, x] = (1 - t) * left + t * right

    # Pystysuora feather-paino: silta sulautuu alkuperäiseen ylä-/alareunalla,
    # jolloin yläraja (juuri kaiverruksen kärjessä) ei jätä vaakasaumaa.
    rows = y1 - y0
    weight = np.ones(rows, dtype=np.float32)
    f = BRIDGE_FEATHER
    if f > 0 and rows > 2 * f:
        ramp = np.linspace(0.0, 1.0, f, endpoint=False)
        weight[:f] = ramp
        weight[-f:] = ramp[::-1]
    wcol = weight[:, None, None]

    region = slice(xl, xr + 1)
    out[y0:y1, region] = wcol * bridged[:, region] + (1 - wcol) * src[y0:y1, region]
    return np.clip(out, 0, 255).astype(np.uint8)


def reinhard_lab_transfer(src_rgb, src_opaque, ref_rgb, ref_opaque, strength):
    """Luminanssirakenteen säilyttävä värinsiirto Lab-avaruudessa (Reinhard).

    Skaalaa lähteen L,a,b -kanavat lineaarisesti niin että opaaki keskiarvo ja
    hajonta vastaavat referenssin opaakkeja pikseleitä. Lineaarisuus säilyttää
    paikalliskontrastin (reunus, kaiverrus, varjot, korostukset).
    """
    # Muunnos uint8-Lab:iin (L,a,b 0..255), tilastot lasketaan floattina.
    src_lab = cv2.cvtColor(src_rgb.astype(np.uint8), cv2.COLOR_RGB2LAB).astype(np.float32)
    ref_lab = cv2.cvtColor(ref_rgb.astype(np.uint8), cv2.COLOR_RGB2LAB).astype(np.float32)

    s_pix = src_lab[src_opaque]
    r_pix = ref_lab[ref_opaque]
    s_mean, s_std = s_pix.mean(axis=0), s_pix.std(axis=0)
    r_mean, r_std = r_pix.mean(axis=0), r_pix.std(axis=0)
    s_std = np.where(s_std < 1e-5, 1e-5, s_std)

    out_lab = (src_lab - s_mean) / s_std * r_std + r_mean
    # Sekoita siirtovoiman mukaan (Lab-avaruudessa)
    out_lab = (1.0 - strength) * src_lab + strength * out_lab

    out_lab = np.clip(out_lab, 0, 255).astype(np.uint8)
    out_rgb = cv2.cvtColor(out_lab, cv2.COLOR_LAB2RGB)
    return out_rgb


def save_palette_preview(ref_rgb, ref_opaque, path):
    """Tallentaa esikatselun referenssin vihreästä paletista (L-järjestyksessä)."""
    pix = ref_rgb[ref_opaque].astype(np.float32)
    lab = cv2.cvtColor(pix.reshape(-1, 1, 3), cv2.COLOR_RGB2LAB).reshape(-1, 3)
    order = np.argsort(lab[:, 0])               # tummasta vaaleaan
    pix_sorted = pix[order].astype(np.uint8)
    w = 512
    idx = np.linspace(0, len(pix_sorted) - 1, w).astype(int)
    strip = pix_sorted[idx]                     # (w,3) gradientti
    preview = np.repeat(strip[None, :, :], 96, axis=0)
    Image.fromarray(preview).save(path)


# ============================================================================
# Pääohjelma
# ============================================================================

def main():
    orange = load_rgba(ORANGE_PLUS_PATH)
    green_ref = load_rgba(GREEN_REF_PATH)

    if orange.shape[:2] != (500, 500):
        print(f"VAROITUS: odotettu 500x500, saatu {orange.shape[:2]}")

    orange_rgb = orange[..., :3].copy()
    orange_alpha = orange[..., 3].copy()

    # --- 1. Pystyvedon maski ---
    mask = build_vertical_stroke_mask(orange.shape)
    Image.fromarray(mask).save(DEBUG_MASK)

    # --- 2. Inpaint: poista pystyveto, korjaa pinta sisällöstä ---
    inpainted_rgb = cv2.inpaint(orange_rgb, mask, INPAINT_RADIUS, cv2.INPAINT_TELEA)

    # Pehmeä sulautus alkuperäiseen vain maskin alueella (feather-reuna)
    if MASK_FEATHER > 0:
        feather = cv2.GaussianBlur(mask.astype(np.float32) / 255.0, (0, 0),
                                   MASK_FEATHER)[..., None]
        inpainted_rgb = (feather * inpainted_rgb +
                         (1 - feather) * orange_rgb).astype(np.uint8)

    # Korjaa vaakavedon reunat risteyksen yli (poistaa kuhmut)
    if REPAIR_BAR_EDGES:
        inpainted_rgb = repair_bar_edges(inpainted_rgb)

    # Tallenna oranssi miinus -debug (alfa mukaan)
    inpainted_rgba = np.dstack([inpainted_rgb, orange_alpha])
    Image.fromarray(inpainted_rgba).save(DEBUG_INPAINTED)

    # --- 3. Värinsiirto vihreäksi (vain opaakit napin pikselit) ---
    orange_opaque = orange_alpha > OPAQUE_THRESHOLD
    green_opaque = green_ref[..., 3] > OPAQUE_THRESHOLD
    save_palette_preview(green_ref[..., :3], green_opaque, DEBUG_PALETTE)

    green_rgb = reinhard_lab_transfer(
        inpainted_rgb, orange_opaque,
        green_ref[..., :3], green_opaque,
        COLOR_TRANSFER_STRENGTH,
    )

    # --- 4. Säilytä alkuperäinen alfa, älä väritä taustaa ---
    final = np.dstack([green_rgb, orange_alpha])
    # Täysin läpinäkyvät pikselit: nollaa RGB sidistin tuloksen vuoksi (valinnainen)
    final[orange_alpha == 0, :3] = 0
    Image.fromarray(final).save(OUTPUT_PATH)

    print("Valmis:")
    for p in (DEBUG_MASK, DEBUG_INPAINTED, DEBUG_PALETTE, OUTPUT_PATH):
        print("  ", p)


if __name__ == "__main__":
    main()
