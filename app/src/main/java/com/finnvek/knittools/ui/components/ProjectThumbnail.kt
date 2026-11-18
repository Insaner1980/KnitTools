package com.finnvek.knittools.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.ui.theme.ProjectListDimens
import com.finnvek.knittools.ui.theme.knitToolsColors
import com.finnvek.knittools.ui.theme.yarnColorForId

/**
 * Projektin pikkukuva: uusin edistymiskuva tai neulepintatilkku projektin lankavärissä.
 * Tilkun pinta kertoo käsityötyypin (neule tai virkkaus), joka ei näy kortilla tekstinä.
 */
@Composable
fun ProjectThumbnail(
    projectId: Long,
    craftType: CraftType,
    photoUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    ThumbnailTile(
        photoUri = photoUri,
        size = size,
        fabric =
            when (craftType) {
                CraftType.KNITTING -> FabricKind.KNIT
                CraftType.CROCHET -> FabricKind.CROCHET
            },
        fabricColor = yarnColorForId(projectId, MaterialTheme.knitToolsColors.yarnPalette),
        modifier = modifier,
    )
}

/**
 * Langan valokuva. Kuvan alla on neutraali kerrattu lanka, joka näkyy latauksen ajan ja
 * latausvirheessä: id:stä laskettu väri antaisi ymmärtää langan väriksi jotain muuta kuin sen
 * oikea väri. Ilman kuvaa kutsuja jättää pikkukuvan kokonaan pois.
 */
@Composable
fun YarnThumbnail(
    photoUri: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    ThumbnailTile(
        photoUri = photoUri,
        size = size,
        fabric = FabricKind.YARN,
        fabricColor = MaterialTheme.knitToolsColors.yarnSwatchNeutral,
        modifier = modifier,
    )
}

@Composable
private fun ThumbnailTile(
    photoUri: String?,
    size: Dp,
    fabric: FabricKind,
    fabricColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(RoundedCornerShape(ProjectListDimens.ThumbnailCornerRadius)),
    ) {
        // Tilkku jää kuvan alle: se näkyy latauksen aikana ja jos kuvan lataus epäonnistuu.
        FabricSwatch(kind = fabric, color = fabricColor, modifier = Modifier.matchParentSize())
        if (photoUri != null) {
            AsyncImage(
                model = photoUri.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
