package pl.krzysztofwojciechowski.yamaharadio.model

object RadioPresetMapper {
    val regex = "(.*) \\(.*/(.*)\\)".toRegex()

    fun mapPresets(responseDto: GetPresetInfoResponseDto): List<RadioPreset> =
        responseDto.presetInfo.mapIndexed { i, presetInfo ->
            if (presetInfo.input == "net_radio") mapPreset(i + 1, presetInfo) else null
        }
            .filterNotNull()
            .toList()

    private fun mapPreset(id: Int, presetInfoDto: PresetInfoDto): RadioPreset {
        val match = regex.matchEntire(presetInfoDto.text)
        return if (match == null) RadioPreset(id, presetInfoDto.text, null)
        else RadioPreset(id, match.groupValues[1], match.groupValues[2])
    }
}