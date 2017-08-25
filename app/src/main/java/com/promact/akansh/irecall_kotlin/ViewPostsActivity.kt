package com.promact.akansh.irecall_kotlin

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MenuItem
import com.crashlytics.android.Crashlytics
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.logentries.logger.AndroidLogger
import io.fabric.sdk.android.Fabric

class ViewPostsActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    val TAG: String = "viewPostActivity"
    lateinit var images: ArrayList<AlbumDetails>
    lateinit var logger: AndroidLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_posts)

        Fabric.with(this, Crashlytics())
        try {
            logger = com.logentries.logger.AndroidLogger.createInstance(applicationContext,
                    false, false, false, null, 0,
                    "b7c7c9d7-853b-483a-bb6d-375e727c2ec9", true)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Crashlytics.logException(ex)
        }

        val latLng: String
        val intent: Intent = intent

        latLng = intent.getStringExtra("lat")

        val toolbar: Toolbar = findViewById(R.id.toolbarViewPost) as Toolbar
        setSupportActionBar(toolbar)

        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        this.supportActionBar?.setHomeButtonEnabled(true)

        val recyclerView: RecyclerView = findViewById(R.id.card_recycler_view) as RecyclerView
        recyclerView.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager =
                GridLayoutManager (applicationContext,1)
        recyclerView.layoutManager = layoutManager

        images = intent.getSerializableExtra("listOfImages") as ArrayList<AlbumDetails>

        for (i in images.indices) {
            Log.d(TAG, "images: ${images[i].caption}")
            logger.log("images: ${images[i].caption}")
        }

        val adapter: ImageAdapter = ImageAdapter(this@ViewPostsActivity,
                images, latLng)
        recyclerView.adapter = adapter
    }

    override fun onStop() { super.onStop() }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                this.finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d(TAG, "Connection unsuccessful")
        logger.log("Connection unsuccessful")
    }
}