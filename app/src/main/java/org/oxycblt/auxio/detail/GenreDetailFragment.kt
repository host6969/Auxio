package org.oxycblt.auxio.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.oxycblt.auxio.R
import org.oxycblt.auxio.detail.adapters.GenreDetailAdapter
import org.oxycblt.auxio.logD
import org.oxycblt.auxio.music.Album
import org.oxycblt.auxio.music.Artist
import org.oxycblt.auxio.music.BaseModel
import org.oxycblt.auxio.music.MusicStore
import org.oxycblt.auxio.playback.state.PlaybackMode
import org.oxycblt.auxio.ui.setupGenreSongActions

/**
 * The [DetailFragment] for a genre.
 * @author OxygenCobalt
 */
class GenreDetailFragment : DetailFragment() {
    private val args: GenreDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // If DetailViewModel isn't already storing the genre, get it from MusicStore
        // using the ID given by the navigation arguments
        if (detailModel.currentGenre.value == null ||
            detailModel.currentGenre.value?.id != args.genreId
        ) {
            detailModel.updateGenre(
                MusicStore.getInstance().genres.find {
                    it.id == args.genreId
                }!!
            )
        }

        val detailAdapter = GenreDetailAdapter(
            detailModel, viewLifecycleOwner,
            doOnClick = {
                playbackModel.playSong(it, PlaybackMode.IN_GENRE)
            },
            doOnLongClick = { data, view ->
                PopupMenu(requireContext(), view).setupGenreSongActions(
                    requireContext(), data, playbackModel, detailModel
                )
            }
        )

        // --- UI SETUP ---

        binding.lifecycleOwner = this

        setupToolbar(R.menu.menu_genre_actions) {
            when (it) {
                R.id.action_shuffle -> {
                    playbackModel.playGenre(
                        detailModel.currentGenre.value!!,
                        true
                    )

                    true
                }

                else -> false
            }
        }

        setupRecycler(detailAdapter)

        // --- VIEWMODEL SETUP ---

        detailModel.genreSortMode.observe(viewLifecycleOwner) { mode ->
            logD("Updating sort mode to $mode")

            val data = mutableListOf<BaseModel>(detailModel.currentGenre.value!!).also {
                it.addAll(mode.getSortedSongList(detailModel.currentGenre.value!!.songs))
            }

            detailAdapter.submitList(data)
        }

        detailModel.navToChild.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it is Artist) {
                    findNavController().navigate(
                        GenreDetailFragmentDirections.actionGoArtist(it.id)
                    )
                } else if (it is Album) {
                    findNavController().navigate(
                        GenreDetailFragmentDirections.actionGoAlbum(it.id, false)
                    )
                }

                detailModel.doneWithNavToChild()
            }
        }

        logD("Fragment created.")

        return binding.root
    }
}
