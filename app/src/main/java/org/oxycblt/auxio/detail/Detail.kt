package org.oxycblt.auxio.detail

import androidx.annotation.StringRes
import org.oxycblt.auxio.list.Item
import org.oxycblt.auxio.music.Song
import org.oxycblt.auxio.music.storage.MimeType

/**
 * A header variation that displays a button to open a sort menu.
 * @param titleRes The string resource to use as the header title
 */
data class SortHeader(@StringRes val titleRes: Int) : Item

/**
 * A header variation that delimits between disc groups.
 * @param disc The disc number to be displayed on the header.
 */
data class DiscHeader(val disc: Int) : Item

/**
 * A [Song] extension that adds information about it's file properties.
 * @param song The internal song
 * @param properties The properties of the song file. Null if parsing is ongoing.
 */
data class DetailSong(val song: Song, val properties: Properties?) {
    /**
     * The properties of a [Song]'s file.
     * @param bitrateKbps The bit rate, in kilobytes-per-second. Null if it could not be parsed.
     * @param sampleRateHz The sample rate, in hertz.
     * @param resolvedMimeType The known mime type of the [Song] after it's file format was
     * determined.
     */
    data class Properties(
        val bitrateKbps: Int?,
        val sampleRateHz: Int?,
        val resolvedMimeType: MimeType
    )
}