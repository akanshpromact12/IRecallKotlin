package com.promact.akansh.irecall_kotlin

import android.app.ProgressDialog
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.firebase.client.Firebase
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus

class HomeActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private val REQUEST_IMAGE_CAPTURE: Int = 1
    private val REQUEST_VIDEO_CAPTURE: Int = 2
    private lateinit var name: String
    private lateinit var email: String
    private lateinit var photoUri: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var fileName: String
    private lateinit var thumbnailName: String
    private val REQUEST_CODE_RESOLUTION: Int = 1
    private val SELECT_PICTURE: Int = 100
    private val SELECT_VIDEO: Int = 500
    private lateinit var pathVideoGallery: String
    private lateinit var pathImgGallery: String
    private val MEDIA_TYPE_VIDEO: Int = 200
    private val VIDEO_DIR_NAME: String = "videoDir"
    private lateinit var floatingActionMenu: FloatingActionMenu
    private lateinit var firebase: Firebase
    private lateinit var albumId: String
    private lateinit var latitudeAlbum: String
    private lateinit var longitudeAlbum: String
    private lateinit var strCaption: String
    private lateinit var auth: FirebaseAuth
    private val TAG: String = "HomeActivity"
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var storageRef: StorageReference
    private lateinit var downloadUri: Uri
    lateinit var newStr: Array<String>
    val mapSize: Int = 0
    lateinit var progressDialog: ProgressDialog
    val j: Int = 0
    val cap: String = ""
    val count: Int = 0
    val trueCount: Int = 0
    lateinit var date: Date
    lateinit var albumDetails: AlbumDetails
    val arrayList: List<AlbumDetails> = ArrayList(initialCapacity = 0)
    val revList: List<AlbumDetails> = ArrayList(initialCapacity = 0)
    val revMap: Map<String, ArrayList<AlbumDetails>> = HashMap(initialCapacity = 0)
    lateinit var marker: Marker
    val childrenCount: Int = 0
    val showLock: Boolean = false
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var gps: GPSTracker
    lateinit var folderNameFirebase: String
    lateinit var coverPic: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val mapFragment: SupportMapFragment = supportFragmentManager
                .findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        auth = FirebaseAuth.getInstance()

        storageRef = FirebaseStorage.getInstance().reference
        val options: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //.requestIdToken()
                .requestEmail()
                .build()
        googleApiClient = GoogleApiClient.Builder(this).enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build()
        googleApiClient.connect()

        val floatingActionCamera: FloatingActionButton = findViewById(R.id.menu_camera_option) as FloatingActionButton
        val floatingActionGallery: FloatingActionButton = findViewById(R.id.menu_gallery_option) as FloatingActionButton
        val floatingAtionMenu: FloatingActionMenu = findViewById(R.id.floating_action_menu) as FloatingActionMenu
        val relativeLayout: RelativeLayout = findViewById(R.id.content_main) as RelativeLayout

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val txt: TextView = findViewById(R.id.textView) as TextView
        getLocation();
    }

    fun getLocation() {
        gps = GPSTracker(this@HomeActivity)

        if (gps.canGetLocation()) {
            latitude = gps.getLatitude()
            longitude = gps.getLongitude()

            Log.d(TAG, "Your current location is: " +
                    "\nLatitude: " + latitude + "" +
                    "\nLongitude: " + longitude)
        } else {
            gps.showSettingsAlert()
        }
    }

    override fun onRestart() {
        super.onRestart()

        getLocation()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        checkFolderExists()
        loadData(googleMap)
    }

    fun checkFolderExists() {
        val folder: File = File(Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_PICTURES).path +
                File.separator + "/IRecall_Images")
        var success: Boolean = true;
        if (!folder.exists()) {
            success = folder.mkdir()
        }

        if (success) {
            Log.d(TAG, "Folder Property: Folder created successfully")
        } else {
            Log.d(TAG, "Folder Property: Folder creation unsuccessful")
        }
    }

    fun loadData(googleMap: GoogleMap?) {

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(this@HomeActivity, "Connection unsuccessful", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Connection unsuccessful")
    }
}
