package pl.krzysztofwojciechowski.yamaharadio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.krzysztofwojciechowski.yamaharadio.model.ActionResponseDto
import pl.krzysztofwojciechowski.yamaharadio.model.RadioPreset
import pl.krzysztofwojciechowski.yamaharadio.model.RadioPresetMapper

sealed interface RadioUiState {
    val filterEnabled: Boolean

    data class Success(
        val shownPresets: List<RadioPreset>,
        override val filterEnabled: Boolean,
        val allPresets: List<RadioPreset>
    ) : RadioUiState

    data class NoWifi(override val filterEnabled: Boolean) : RadioUiState
    data class Error(val detail: String?, override val filterEnabled: Boolean) : RadioUiState
    data class Loading(override val filterEnabled: Boolean) : RadioUiState
}

class YamahaRadioViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<RadioUiState>(RadioUiState.Loading(true))

    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    fun setError(detail: String? = null) {
        _uiState.update { s -> RadioUiState.Error(detail, s.filterEnabled) }
    }

    fun setNoWifi() {
        _uiState.update { s -> RadioUiState.NoWifi(s.filterEnabled) }
    }

    fun toggleIsFiltered() {
        _uiState.update { s ->
            when (s) {
                is RadioUiState.Success -> listState(s.allPresets, !s.filterEnabled)
                is RadioUiState.NoWifi -> RadioUiState.NoWifi(!s.filterEnabled)
                is RadioUiState.Error -> RadioUiState.Error(s.detail, !s.filterEnabled)
                is RadioUiState.Loading -> RadioUiState.Loading(!s.filterEnabled)
            }
        }
    }

    private fun listState(presets: List<RadioPreset>, filterEnabled: Boolean): RadioUiState.Success {
        return RadioUiState.Success(
            if (filterEnabled) filterPresets(presets) else presets,
            filterEnabled,
            presets
        )
    }

    fun filterPresets(presets: List<RadioPreset>): List<RadioPreset> =
        presets.filter { p -> p.language == "Polish" || p.language == null }.toList()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { s -> RadioUiState.Loading(s.filterEnabled) }
            _uiState.update { s ->
                try {
                    val response = YamahaApi.retrofitService.getPresetInfo()
                    if (response.responseCode == 0) {
                        val presets = RadioPresetMapper.mapPresets(response)
                        listState(presets, s.filterEnabled)
                    } else RadioUiState.Error("Response code is ${response.responseCode}", s.filterEnabled)
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        ensureActive()
                    }
                    RadioUiState.Error(e.localizedMessage, s.filterEnabled)
                }
            }
        }
    }

    fun setPreset(preset: RadioPreset) = invokeAction { YamahaApi.retrofitService.recallPreset(preset.id) }

    fun togglePower() = invokeAction { YamahaApi.retrofitService.togglePower() }

    fun volumeUp() = invokeAction { YamahaApi.retrofitService.setVolume("up") }

    fun volumeDown() = invokeAction { YamahaApi.retrofitService.setVolume("down") }

    private fun invokeAction(act: (suspend () -> ActionResponseDto)) {
        viewModelScope.launch {
            _uiState.update { s ->
                try {
                    val response = act()
                    if (response.responseCode == 0) s else RadioUiState.Error(
                        "Response code is ${response.responseCode}",
                        s.filterEnabled
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        ensureActive()
                    }
                    RadioUiState.Error(e.localizedMessage, s.filterEnabled)
                }
            }
        }
    }
}