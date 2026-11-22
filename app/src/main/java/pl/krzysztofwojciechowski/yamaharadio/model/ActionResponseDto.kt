package pl.krzysztofwojciechowski.yamaharadio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActionResponseDto(
    @SerialName("response_code")
    val responseCode: Int
)

