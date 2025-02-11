/*
 * Copyright (c) 2024 Auxio Project
 * MetadataExtractor.kt is part of Auxio.
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
 
package org.oxycblt.musikr.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.MediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import org.oxycblt.musikr.fs.query.DeviceFile

interface MetadataExtractor {
    suspend fun extract(file: DeviceFile): AudioMetadata
}

class MetadataExtractorImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val mediaSourceFactory: MediaSource.Factory
) : MetadataExtractor {
    override suspend fun extract(file: DeviceFile): AudioMetadata {
        val exoPlayerMetadataFuture =
            MetadataRetriever.retrieveMetadata(mediaSourceFactory, MediaItem.fromUri(file.uri))
        val mediaMetadataRetriever =
            MediaMetadataRetriever().apply {
                withContext(Dispatchers.IO) { setDataSource(context, file.uri) }
            }
        val trackGroupArray = exoPlayerMetadataFuture.await()
        if (trackGroupArray.isEmpty) {
            return AudioMetadata(null, mediaMetadataRetriever)
        }
        val trackGroup = trackGroupArray.get(0)
        if (trackGroup.length == 0) {
            return AudioMetadata(null, mediaMetadataRetriever)
        }
        val format = trackGroup.getFormat(0)
        return AudioMetadata(format, mediaMetadataRetriever)
    }
}
