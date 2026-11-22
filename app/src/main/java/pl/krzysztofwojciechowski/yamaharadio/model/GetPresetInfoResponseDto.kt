package pl.krzysztofwojciechowski.yamaharadio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetPresetInfoResponseDto(
    @SerialName("response_code")
    val responseCode: Int,
    @SerialName("preset_info")
    val presetInfo: List<PresetInfoDto>,
    @SerialName("func_list")
    val funcList: List<String>
)