package com.googlespacecleaner.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

/**
 * Affiche la miniature d'un ScannedItem (Drive/Photos). Repose sur l'ImageLoader
 * Coil global configuré dans GsApplication avec le client OkHttp authentifié
 * partagé (voir NetworkAuthModule) : les URLs Drive (thumbnailLink) et Photos
 * Picker (mediaFile.baseUrl) exigent un en-tête OAuth valide pour se charger,
 * sans quoi elles renvoient une erreur 403 — Coil transmet cet en-tête
 * automatiquement puisque le client OkHttp partagé porte déjà l'intercepteur.
 *
 * Si `thumbnailUrl` est null (cas des imports Takeout, sans URL distante) ou
 * si le chargement échoue, une icône de repli est affichée plutôt qu'une
 * image cassée.
 */
@Composable
fun ItemThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUrl == null) {
            FallbackIcon()
        } else {
            SubcomposeAsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                loading = { CircularProgressIndicator(modifier = Modifier.size(size / 3)) },
                error = { FallbackIcon() },
                success = { SubcomposeAsyncImageContent() }
            )
        }
    }
}

@Composable
private fun FallbackIcon() {
    Icon(
        imageVector = Icons.Filled.Image,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
