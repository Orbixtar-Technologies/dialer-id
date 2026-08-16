package com.example.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Contact

/**
 * Contact photo with an initials fallback.
 *
 * The initials stay behind the image so they also act as the placeholder while
 * the thumbnail is decoded from the content resolver.
 */
@Composable
fun ContactAvatar(
    name: String,
    photoUri: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(name) { Contact.initialsOf(name) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        if (!photoUri.isNullOrEmpty()) {
            AsyncImage(
                model = photoUri,
                contentDescription = stringResource(R.string.contacts_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        }
    }
}
