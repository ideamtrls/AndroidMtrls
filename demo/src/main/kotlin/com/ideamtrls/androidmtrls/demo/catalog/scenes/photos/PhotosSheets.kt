package com.ideamtrls.androidmtrls.demo

import android.icu.text.ListFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMFlowLayout
import com.ideamtrls.androidmtrls.AMLabeledContent
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMSheet
import com.ideamtrls.androidmtrls.AMSheetHeight
import com.ideamtrls.androidmtrls.AMSheetSurface
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * The scene's two sheets, frosted like its cards over the art of the view they're opened on (so
 * the view on top composes them, inside its backdrop): "Add to album", as tall as its albums, and
 * the full-height "Info". What they hold sits on solid cards.
 */
@Composable
internal fun PhotosSheets(model: PhotosModel) {
    AMSheet(
        model.showsAlbumSheet,
        onDismissRequest = { model.showsAlbumSheet = false },
        title = "Add to album",
        height = AMSheetHeight.Content,
        surface = AMSheetSurface.Frosted,
    ) {
        PhotosAlbumSheet(model)
    }

    // The sheet keeps showing its photo while it slides away.
    val live = model.infoPhotoId?.let(model::photo)
    val last = remember { PhotosLastSeen(live) }
    SideEffect { if (live != null) last.value = live }
    AMSheet(
        live != null,
        onDismissRequest = { model.infoPhotoId = null },
        title = "Info",
        surface = AMSheetSurface.Frosted,
    ) {
        (live ?: last.value)?.let { PhotosInfoSheet(it) }
    }
}

/**
 * Every album as a row in one card: its cover, its name over its photo count. A tap adds the
 * sheet's photos to it; an album already holding all of them carries a check and reads as
 * selected.
 */
@Composable
private fun PhotosAlbumSheet(model: PhotosModel) {
    val spacing = AMTheme.tokens.spacing
    val targets = model.albumSheetTargets
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
    ) {
        Column(Modifier.fillMaxWidth().amCard()) {
            model.albums.forEach { album ->
                // An empty target list never marks an album full.
                val holdsAll = targets.isNotEmpty() && album.photoIds.containsAll(targets)
                AMListRow(
                    album.name,
                    modifier = Modifier.semantics { selected = holdsAll },
                    subtitle = photosCountLabel(album.photoIds.size),
                    leadingContent = { PhotosAlbumCover(model.cover(album), 44.dp, MaterialTheme.shapes.small) },
                    trailingContent = if (holdsAll) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                    onClick = { model.add(toAlbum = album.id) },
                )
            }
        }
    }
}

/**
 * A photo's Info: Details (date, place, dimensions, size) and Camera (camera, lens, exposure, ISO),
 * each a section of bare rows on a card, then its keywords as tags, read as one element.
 */
@Composable
private fun PhotosInfoSheet(photo: PhotosItem) {
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        AMSection("Details", style = AMSectionStyle.Rows, inset = false) {
            AMLabeledContent("Date", photo.fullDate)
            AMLabeledContent("Place", photo.place.detail)
            AMLabeledContent("Dimensions", photo.pixelSize)
            AMLabeledContent("Size", photo.fileSize)
        }
        AMSection("Camera", style = AMSectionStyle.Rows, inset = false) {
            AMLabeledContent("Camera", photo.camera)
            AMLabeledContent("Lens", photo.lens)
            AMLabeledContent("Exposure", photo.exposure)
            AMLabeledContent("ISO", photo.iso)
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            AMSectionHeader("Keywords")
            val words = ListFormatter.getInstance().format(photo.keywords)
            AMFlowLayout(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = "Keywords: $words" }) {
                photo.keywords.forEach { AMTag(it) }
            }
        }
    }
}
