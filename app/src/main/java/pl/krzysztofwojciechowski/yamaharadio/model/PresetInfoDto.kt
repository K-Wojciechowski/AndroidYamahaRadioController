package pl.krzysztofwojciechowski.yamaharadio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresetInfoDto(
    @SerialName("input")
    val input: String,
    @SerialName("text")
    val text: String,
    @SerialName("attribute")
    val attribute: Int? = null
)