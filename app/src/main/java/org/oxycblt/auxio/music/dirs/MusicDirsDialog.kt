/*
 * Copyright (c) 2021 Auxio Project
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
 
package org.oxycblt.auxio.music.dirs

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import org.oxycblt.auxio.BuildConfig
import org.oxycblt.auxio.R
import org.oxycblt.auxio.databinding.DialogMusicDirsBinding
import org.oxycblt.auxio.music.Dir
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.settings.SettingsManager
import org.oxycblt.auxio.ui.ViewBindingDialogFragment
import org.oxycblt.auxio.util.hardRestart
import org.oxycblt.auxio.util.logD
import org.oxycblt.auxio.util.showToast

/**
 * Dialog that manages the music dirs setting.
 * @author OxygenCobalt
 */
class MusicDirsDialog :
    ViewBindingDialogFragment<DialogMusicDirsBinding>(), MusicDirAdapter.Listener {
    private val settingsManager = SettingsManager.getInstance()

    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val dirAdapter = MusicDirAdapter(this)

    override fun onCreateBinding(inflater: LayoutInflater) =
        DialogMusicDirsBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        // Don't set the click listener here, we do some custom magic in onCreateView instead.
        builder
            .setTitle(R.string.set_dirs)
            .setNeutralButton(R.string.lbl_add, null)
            .setPositiveButton(R.string.lbl_save, null)
            .setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onBindingCreated(binding: DialogMusicDirsBinding, savedInstanceState: Bundle?) {
        val launcher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree(), ::addDocTreePath)

        // Now that the dialog exists, we get the view manually when the dialog is shown
        // and override its click listener so that the dialog does not auto-dismiss when we
        // click the "Add"/"Save" buttons. This prevents the dialog from disappearing in the former
        // and the app from crashing in the latter.
        requireDialog().setOnShowListener {
            val dialog = it as AlertDialog

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                logD("Opening launcher")
                launcher.launch(null)
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val dirs = settingsManager.musicDirs

                if (dirs.dirs != dirAdapter.data.currentList ||
                    dirs.shouldInclude != isInclude(requireBinding())) {
                    logD("Committing changes")
                    saveAndRestart()
                } else {
                    logD("Dropping changes")
                    dismiss()
                }
            }
        }

        binding.dirsRecycler.apply {
            adapter = dirAdapter
            itemAnimator = null
        }

        var dirs = settingsManager.musicDirs

        if (savedInstanceState != null) {
            val pendingDirs = savedInstanceState.getStringArrayList(KEY_PENDING_DIRS)

            if (pendingDirs != null) {
                dirs =
                    MusicDirs(
                        pendingDirs.mapNotNull(MusicDirs::parseDir),
                        savedInstanceState.getBoolean(KEY_PENDING_MODE))
            }
        }

        dirAdapter.data.addAll(dirs.dirs)
        requireBinding().dirsEmpty.isVisible = dirs.dirs.isEmpty()

        binding.folderModeGroup.apply {
            check(
                if (dirs.shouldInclude) {
                    R.id.dirs_mode_include
                } else {
                    R.id.dirs_mode_exclude
                })

            updateMode()
            addOnButtonCheckedListener { _, _, _ -> updateMode() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(
            KEY_PENDING_DIRS, ArrayList(dirAdapter.data.currentList.map { it.toString() }))
        outState.putBoolean(KEY_PENDING_MODE, isInclude(requireBinding()))
    }

    override fun onDestroyBinding(binding: DialogMusicDirsBinding) {
        super.onDestroyBinding(binding)
        binding.dirsRecycler.adapter = null
    }

    override fun onRemoveDirectory(dir: Dir.Relative) {
        dirAdapter.data.remove(dir)
        requireBinding().dirsEmpty.isVisible = dirAdapter.data.currentList.isEmpty()
    }

    private fun addDocTreePath(uri: Uri?) {
        if (uri == null) {
            // A null URI means that the user left the file picker without picking a directory
            logD("No URI given (user closed the dialog)")
            return
        }

        val dir = parseExcludedUri(uri)
        if (dir != null) {
            dirAdapter.data.add(dir)
            requireBinding().dirsEmpty.isVisible = false
        } else {
            requireContext().showToast(R.string.err_bad_dir)
        }
    }

    private fun parseExcludedUri(uri: Uri): Dir.Relative? {
        // Turn the raw URI into a document tree URI
        val docUri =
            DocumentsContract.buildDocumentUriUsingTree(
                uri, DocumentsContract.getTreeDocumentId(uri))

        // Turn it into a semi-usable path
        val treeUri = DocumentsContract.getTreeDocumentId(docUri)

        // Parsing handles the rest
        return MusicDirs.parseDir(treeUri)
    }

    private fun updateMode() {
        val binding = requireBinding()
        if (isInclude(binding)) {
            binding.dirsModeDesc.setText(R.string.set_dirs_mode_include_desc)
        } else {
            binding.dirsModeDesc.setText(R.string.set_dirs_mode_exclude_desc)
        }
    }

    private fun isInclude(binding: DialogMusicDirsBinding) =
        binding.folderModeGroup.checkedButtonId == R.id.dirs_mode_include

    private fun saveAndRestart() {
        settingsManager.musicDirs =
            MusicDirs(dirAdapter.data.currentList, isInclude(requireBinding()))

        playbackModel.savePlaybackState(requireContext()) { requireContext().hardRestart() }
    }

    companion object {
        const val TAG = BuildConfig.APPLICATION_ID + ".tag.EXCLUDED"
        const val KEY_PENDING_DIRS = BuildConfig.APPLICATION_ID + ".key.PENDING_DIRS"
        const val KEY_PENDING_MODE = BuildConfig.APPLICATION_ID + ".key.SHOULD_INCLUDE"
    }
}