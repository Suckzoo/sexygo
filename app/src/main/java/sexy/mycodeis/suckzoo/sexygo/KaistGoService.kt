package sexy.mycodeis.suckzoo.sexygo

import io.reactivex.Observable
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface KaistGoService {
    @GET("go?{latitude},{longitude}")
    fun search(@Path("latitude") lat : Double, @Path("longitude") lng: Double) : Observable<Response>

    @GET("{object}")
    fun fetch(@Path("object") uri : String) : Observable<ResponseBody>
}

class KaistGoNotFoundException: Exception()