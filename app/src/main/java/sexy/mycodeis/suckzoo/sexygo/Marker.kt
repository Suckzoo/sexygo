package sexy.mycodeis.suckzoo.sexygo

import android.util.Log
import android.util.LruCache
import rx.Observable
import rx.subjects.AsyncSubject
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Suckzoo on 2017. 4. 19..
 */
data class Marker(val latitude: Double, val longitude: Double, val uri: String)

class MarkerMap (val activity: MapsActivity) {
    private val cacheSize = 4 * 1024 * 1024
    private var markerMap: ConcurrentHashMap<String, Marker> = ConcurrentHashMap<String, Marker>()
    private var cache: LruCache<String, Pair<String, ByteArray>> = LruCache(cacheSize)

    fun addMarker(lat: Double, long: Double, uri: String) {
        if (markerMap[uri] == null) {
            val marker = Marker(lat, long, uri)
            markerMap[uri] = marker
            activity.addMarker(lat, long, uri)
        }
    }

    fun fetchPayloadFromMarker(uri: String): Observable<Pair<String, ByteArray>> {
        Log.wtf("fetch", "fetching...")
        if (cache[uri] != null) {
            Log.wtf("fetch", "cache hit!")
            return Observable.just(cache[uri])
        } else {
            Log.wtf("fetch", "cache miss!")
            return activity.mService.fetch(uri)
                    .switchMap {
                        Log.wtf("fetch", "fetched from server!")
                        val contentType = it.first.httpResponseHeaders["Content-Type"]!![0]
                        val payload = it.second
                        var cachePayload: Pair<String, ByteArray> = Pair(contentType, payload)
                        cache.put(uri, cachePayload)
                        Observable.just(cachePayload)
                    }
        }
    }

}
