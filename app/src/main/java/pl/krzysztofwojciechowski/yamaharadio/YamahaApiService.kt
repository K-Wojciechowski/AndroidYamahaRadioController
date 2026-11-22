package pl.krzysztofwojciechowski.yamaharadio

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import pl.krzysztofwojciechowski.yamaharadio.model.ActionResponseDto
import pl.krzysztofwojciechowski.yamaharadio.model.GetPresetInfoResponseDto
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "http://${BuildConfig.RADIO_IP}/"

/**
 * Use the Retrofit builder to build a retrofit object using a kotlinx.serialization converter
 */
private val retrofit = Retrofit.Builder()
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

/**
 * Retrofit service object for creating api calls
 */
interface YamahaApiService {
    @GET("YamahaExtendedControl/v1/netusb/getPresetInfo")
    suspend fun getPresetInfo(): GetPresetInfoResponseDto

    @GET("YamahaExtendedControl/v1/main/setPower?power=toggle")
    suspend fun togglePower(): ActionResponseDto

    @GET("YamahaExtendedControl/v1/main/setVolume")
    suspend fun setVolume(@Query("volume") volume: String): ActionResponseDto

    @GET("YamahaExtendedControl/v1/netusb/recallPreset?zone=main")
    suspend fun recallPreset(@Query("num") num: Int): ActionResponseDto
}

/**
 * A public Api object that exposes the lazy-initialized Retrofit service
 */
object YamahaApi {
    val retrofitService: YamahaApiService by lazy {
        retrofit.create(YamahaApiService::class.java)
    }
}
