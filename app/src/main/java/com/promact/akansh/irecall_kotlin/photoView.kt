package com.promact.akansh.irecall_kotlin

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.Crashlytics
import com.logentries.logger.AndroidLogger
import io.fabric.sdk.android.Fabric
import java.lang.Exception

class photoView : AppCompatActivity() {
    lateinit var mediaUrl: String
    lateinit var logger: AndroidLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)

        Fabric.with(this, Crashlytics())
        try {
            logger = com.logentries.logger.AndroidLogger.createInstance(applicationContext,
                    false, false, false, null, 0,
                    "b7c7c9d7-853b-483a-bb6d-375e727c2ec9", true)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Crashlytics.logException(ex)
        }

        val toolbar: Toolbar = findViewById(R.id.toolbarViewPost) as Toolbar
        setSupportActionBar(toolbar)

        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        this.supportActionBar?.setHomeButtonEnabled(true)

        val fullScreenImage: ImageView = findViewById(R.id.fullPageImage) as ImageView
        val videoView: VideoView = findViewById(R.id.video_view) as VideoView

        val intent: Intent = intent
        mediaUrl = intent.getStringExtra("media")

        if (mediaUrl.contains(".mp4")) {
            fullScreenImage.visibility = View.GONE
            videoView.visibility = View.VISIBLE

            val uri: Uri = Uri.parse("https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.appspot.com/o/IRecall%2F$mediaUrl?alt=media&token=1")
            videoView.setVideoURI(uri)
            videoView.setMediaController(MediaController(this))
            videoView.requestFocus()
            videoView.start()
        } else {
            videoView.visibility = View.GONE
            fullScreenImage.visibility = View.VISIBLE

            Glide.with(this@photoView)
                    .load("https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.appspot.com/o/IRecall%2F$mediaUrl?alt=media&token=1")
                    .fitCenter()
                    .listener(object: RequestListener<String, GlideDrawable> {
                        override fun onException(e: Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            e?.printStackTrace()

                            return false
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            if (!isFromMemoryCache) {
                                resource?.start()
                            }

                            return false
                        }
                    }).into(fullScreenImage)
        }
    }

    override fun onStop() { super.onStop() }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home ->
            {
                this.finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
