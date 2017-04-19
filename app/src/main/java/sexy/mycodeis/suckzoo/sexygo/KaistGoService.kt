package sexy.mycodeis.suckzoo.sexygo

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_response
import rx.Observable
import javax.net.ssl.HttpsURLConnection

class KaistGoService(base_url: String) {
    val BASE_URL: String = base_url
    init {
        FuelManager.instance.removeAllResponseInterceptors()
        FuelManager.instance.addResponseInterceptor({
            next: (Request, Response) -> Response ->
            {
                request: Request, response: Response ->
                if (response.httpStatusCode == HttpsURLConnection.HTTP_MOVED_PERM ||
                        response.httpStatusCode == HttpsURLConnection.HTTP_MOVED_TEMP) {
                    response
                }
                else
                    next(request, response)
            }
        })
    }

    fun search(lat : Double, lng: Double): Observable<Pair<Response, ByteArray>> {
        return "$BASE_URL/go?$lat,$lng".httpGet().rx_response()
    }

    fun fetch(uri : String): Observable<Pair<Response, ByteArray>> {
        return uri.httpGet().rx_response().switchMap {
            Observable.just(it)
        }
    }
}

