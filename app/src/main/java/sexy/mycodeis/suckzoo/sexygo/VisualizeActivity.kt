package sexy.mycodeis.suckzoo.sexygo

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.jetbrains.anko.*
import java.io.FileOutputStream

class VisualizeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val contentType = intent.getStringExtra("contentType")
        val payload = intent.getByteArrayExtra("payload")
        when (contentType) {
            "text/plain" -> {
                verticalLayout {
                    padding = dip(30)
                    textView {
                        text = String(payload)
                    }
                }
            }
            "image/png" -> {
                verticalLayout {
                    imageView {
                        imageBitmap = BitmapFactory.decodeByteArray(payload, 0, payload.size)
                    }.lparams(width=matchParent, height=matchParent)
                }
            }
            "video/mp4" -> {
                val filePath = intent.getStringExtra("filePath")
                verticalLayout {
                    videoView {
                        setVideoPath(filePath)
                        start()
                    }.lparams(width=matchParent, height=matchParent)
                }
            }
        }
    }
}
