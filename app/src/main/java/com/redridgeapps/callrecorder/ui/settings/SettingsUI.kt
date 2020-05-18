package com.redridgeapps.callrecorder.ui.settings

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.ui.core.Modifier
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.material.IconButton
import androidx.ui.material.ListItem
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.ArrowBack
import androidx.ui.unit.dp
import com.koduok.compose.navigation.BackStackAmbient
import com.redridgeapps.callrecorder.callutils.PcmChannels
import com.redridgeapps.callrecorder.callutils.PcmEncoding
import com.redridgeapps.callrecorder.callutils.PcmSampleRate
import com.redridgeapps.callrecorder.ui.compose_viewmodel.fetchViewModel
import com.redridgeapps.callrecorder.ui.routing.Destination
import com.redridgeapps.callrecorder.ui.utils.SingleSelectListPreference
import com.redridgeapps.callrecorder.ui.utils.SwitchPreference
import com.redridgeapps.callrecorder.ui.utils.TitlePreference

class SettingsState {

    var isSystemized by mutableStateOf<Boolean?>(null)

    var isRecordingOn by mutableStateOf<Boolean?>(null)

    var audioRecordSampleRate by mutableStateOf<PcmSampleRate?>(null)

    var audioRecordChannels by mutableStateOf<PcmChannels?>(null)

    var audioRecordEncoding by mutableStateOf<PcmEncoding?>(null)
}

object SettingsDestination : Destination {

    @Composable
    override fun initializeUI() {

        val viewModel = fetchViewModel<SettingsViewModel>()

        SettingsUI(viewModel)
    }
}

@Composable
private fun SettingsUI(viewModel: SettingsViewModel) {

    val navigationIcon = @Composable {

        val backStack = BackStackAmbient.current

        IconButton(onClick = { backStack.pop() }) {
            Icon(Icons.Default.ArrowBack)
        }
    }

    val topAppBar = @Composable {
        TopAppBar(
            title = { Text("Settings", modifier = Modifier.padding(bottom = 16.dp)) },
            navigationIcon = navigationIcon
        )
    }

    Scaffold(topAppBar = topAppBar) { modifier ->
        ContentMain(viewModel, modifier)
    }
}

@Composable
private fun ContentMain(viewModel: SettingsViewModel, modifier: Modifier) {

    Column(modifier) {

        SwitchPreference("Recording", viewModel.uiState.isRecordingOn) {
            viewModel.flipRecording()
        }

        SwitchPreference("Systemize", viewModel.uiState.isSystemized) {
            viewModel.flipSystemization()
        }

        ListItem(
            text = { Text("Update contact names") },
            onClick = { viewModel.updateContactNames() }
        )

        AudioRecordAPIPreference(viewModel)
    }
}

@Composable
private fun AudioRecordAPIPreference(viewModel: SettingsViewModel) {

    Column {

        TitlePreference(text = "AudioRecord API")

        SingleSelectListPreference(
            title = "Sample Rate",
            keys = PcmSampleRate.values().asList(),
            keyToTextMapper = { it.sampleRate.toString() },
            selectedItem = viewModel.uiState.audioRecordSampleRate,
            onSelectedChange = { viewModel.setAudioRecordSampleRate(it) }
        )

        SingleSelectListPreference(
            title = "Channels",
            keys = PcmChannels.values().asList(),
            keyToTextMapper = { it.toReadableString() },
            selectedItem = viewModel.uiState.audioRecordChannels,
            onSelectedChange = { viewModel.setAudioRecordChannels(it) }
        )

        SingleSelectListPreference(
            title = "Encoding",
            keys = PcmEncoding.values().asList(),
            keyToTextMapper = { it.toReadableString() },
            selectedItem = viewModel.uiState.audioRecordEncoding,
            onSelectedChange = { viewModel.setAudioRecordEncoding(it) }
        )
    }
}

private fun PcmChannels.toReadableString(): String = when (this) {
    PcmChannels.MONO -> "Mono"
    PcmChannels.STEREO -> "Stereo"
}

private fun PcmEncoding.toReadableString(): String = when (this) {
    PcmEncoding.PCM_8BIT -> "PCM_8BIT"
    PcmEncoding.PCM_16BIT -> "PCM_16BIT"
    PcmEncoding.PCM_FLOAT -> "PCM_FLOAT"
}
