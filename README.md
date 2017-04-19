# CS442 Project 2

- Title: `sexygo`
- Author: Seokju Hong
- ID: 20164484

Codes are also available on https://github.com/Suckzoo/sexygo/.

This report is also available on https://github.com/Suckzoo/sexygo/blob/master/README.md.

## Implementation

### Overall stack and how to build
- `Kotlin` 1.1.1
- `RxKotlin` 1.0.0
- `RxAndroid` 1.2.1
- `Fuel` 1.5.0
- `Anko` 0.9

Building this app is simple. Just simply import provided project files
with Android Studio, and then build. ¯\\_(ツ)_/¯

### Implemented features
- Rendered map fragment with Google maps API
- `Reactive` location based marker fetching
- Marker data fetching and visualizing
- LRU Caching when fetching payloads

### Not implemented features
- Progress bar UI
- Walking distance measurement

### Location sensing service with Google Play Location Service
It is easy to sense where the user is with Google's Play Location Service.
I fetched geological information from those Google's APIs.

### Reactive HTTP communication
I communicated with the webserver with the library
[`RxKotlin`](https://github.com/reactivex/rxkotlin) and
[`Fuel`](https://github.com/kittinunf/Fuel). As fuel supports Rx manner
programming, I could write code as simple as possible, without callback
hells.
First, I created `Observable.interval` which emits an integer every
second. And then, I applied `Fuel` library to fetch markers. And then with
fetched markers, I saved it to another data structure.

### Visualizing marker data
First, when I got 302 response from web server, I created a new marker
with response's redirected location and let the marker's title be the
redirected location. When user touches a marker, `OnMarkerClickListener`
listens the touch and fetch payloads.(Fetching manner is to be discussed)
When the payload fetched, create a new intent and open new activity
`VisualizeActivity`. `Content-type` and payloads are passed with intent.
In case of `video/mp4`, it first stores the payload to the internal
storage and don't pass payload on the created intent.
In the `VisualizeActivity`, I created the view programatically with
`Anko` DSL. If `Content-Type` is `text/plain`, I created `TextView`.
And I created `ImageView` and `VideoView`, if the `Content-Type` is
`image/png` and `video/mp4` respectively.

### Caching payloads fetched from web server
Cache hits and misses are decided with the resources URI. I used URI
as a key, and stored the payload on the `LruCache`.

```Kotlin
private val cacheSize = 4 * 1024 * 1024
private var cache: LruCache<String, Pair<String, ByteArray>> = LruCache(cacheSize)

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
```

Before fetching resources, look cache with given URI first and if possible,
use payloads stored in the LruCache because it is a cache hit. If no
cache element is found in LruCache, then I fetched the resource from
the webserver and put it on LruCache.
