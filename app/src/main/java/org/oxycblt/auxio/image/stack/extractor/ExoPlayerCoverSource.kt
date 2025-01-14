/*
 * Copyright (c) 2024 Auxio Project
 * ExoPlayerCoverSource.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.image.stack.extractor

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.metadata.flac.PictureFrame
import androidx.media3.extractor.metadata.id3.ApicFrame
import javax.inject.Inject
import kotlinx.coroutines.guava.asDeferred

class ExoPlayerCoverSource
@Inject
constructor(private val mediaSourceFactory: MediaSource.Factory) : CoverSource {
    override suspend fun extract(fileUri: Uri): ByteArray? {
        val tracks =
            MetadataRetriever.retrieveMetadata(mediaSourceFactory, MediaItem.fromUri(fileUri))
                .asDeferred()
                .await()

        // The metadata extraction process of ExoPlayer results in a dump of all metadata
        // it found, which must be iterated through.
        val metadata = tracks[0].getFormat(0).metadata

        if (metadata == null || metadata.length() == 0) {
            // No (parsable) metadata. This is also expected.
            return null
        }

        return findCoverDataInMetadata(metadata)
    }

    private fun findCoverDataInMetadata(metadata: Metadata): ByteArray? {
        var fallbackPic: ByteArray? = null

        for (i in 0 until metadata.length()) {
            // We can only extract pictures from two tags with this method, ID3v2's APIC or
            // Vorbis picture comments.
            val pic: ByteArray?
            val type: Int

            when (val entry = metadata.get(i)) {
                is ApicFrame -> {
                    pic = entry.pictureData
                    type = entry.pictureType
                }
                is PictureFrame -> {
                    pic = entry.pictureData
                    type = entry.pictureType
                }
                else -> continue
            }

            if (type == MediaMetadata.PICTURE_TYPE_FRONT_COVER) {
                return pic
            } else if (fallbackPic == null) {
                fallbackPic = pic
            }
        }

        return fallbackPic
    }
}
