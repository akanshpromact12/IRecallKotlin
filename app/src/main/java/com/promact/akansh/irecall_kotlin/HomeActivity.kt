package com.promact.akansh.irecall_kotlin

import android.app.ProgressDialog
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.firebase.client.*
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus

class HomeActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, android.location.LocationListener {
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
    var count: Int = 0
    var trueCount: Int = 0
    lateinit var date: Date
    lateinit var albumDetails: AlbumDetails
    lateinit var arrayList: List<AlbumDetails>
    val revList: MutableList<AlbumDetails> = ArrayList(initialCapacity = 0)
    lateinit var revMap: MutableMap<String, ArrayList<AlbumDetails>>
    lateinit var marker: Marker
    var childrenCount: Long = 0
    val showLock: Boolean = false
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var gps: GPSTracker
    lateinit var folderNameFirebase: String
    lateinit var coverPic: String
    lateinit var sharedPrefs: saveSharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val mapFragment: SupportMapFragment = supportFragmentManager
                .findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        auth = FirebaseAuth.getInstance()
        arrayList = arrayListOf(albumDetails)

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
        loadData(googleMap!!)
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

    fun loadData(googleMap: GoogleMap) {
        googleMap.setOnMapClickListener {
            GoogleMap.OnMapClickListener {
                latLng: LatLng? ->
                    if (floatingActionMenu.isOpened) {
                        floatingActionMenu.close(true)
                    }
            }
        }

        firebase.orderByChild("Date").addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot?, s: String?) {
                val handler: Transaction.Handler = object: Transaction.Handler {
                    override fun doTransaction(mutableData: MutableData?): Transaction.Result {
                        childrenCount = mutableData?.childrenCount!!

                        return Transaction.success(mutableData)
                    }

                    override fun onComplete(firebaseError: FirebaseError?, b: Boolean, dataSnapshot: DataSnapshot?) {

                    }
                }

                firebase.runTransaction(handler)
                albumDetails = dataSnapshot?.getValue(AlbumDetails::class.java)!!
                Log.d(TAG, "strLength: " + childrenCount)

                arrayList = arrayListOf(albumDetails)
                Log.d(TAG, "arrlist size: " + arrayList.size)

                revMap.put(albumDetails.AlbumId, arrayListOf(AlbumDetails()))
                if (arrayList.size==childrenCount.toInt()) {
                    if (revList.isNotEmpty()) {
                        revList.clear()
                    }
                    for (i in 0..(revList.size-1) step 1) {
                        Log.d(TAG, "revList size before adding element: " + revList.size)
                        revList.add(arrayList.get(i))
                        Log.d(TAG, "revList size after adding element: " + revList.size)
                    }

                    Log.d(TAG, "revList size: "+revList.size+" values: "+revList.get(0).caption)

                    for (key: String in revMap.keys) {
                        for (i in 0..(revList.size-1) step 1) {
                            var album: AlbumDetails = revList.get(i)
                            if (key.equals(album.AlbumId)) {
                                var imageListofAlbum: ArrayList<AlbumDetails> = revMap.get(key)!!
                                imageListofAlbum.add(album)
                                revMap.put(key, imageListofAlbum)
                            }
                        }
                    }
                    var set: Set<String> = revMap.keys

                    Log.d(TAG, "Children size: " + childrenCount)
                    for (s2: String in set) {
                        Log.d(TAG, "Key-> " + s2)

                        var str: ArrayList<AlbumDetails> = revMap.get(s2)!!
                        for (i in 0..(str.size-1) step 1) {
                            Log.d(TAG, "Values->" + str.get(i).caption)
                        }
                        var str1: String = str.get(0).caption
                        Log.d(TAG, "Caption Map: " + str1)

                        //Map Start
                        var latLng: LatLng = LatLng(str.get(0).Latitude.toDouble(),
                                str.get(0).Longitude.toDouble())
                        Log.d(TAG, "LatLng: " + latLng)

                        if (str.get(0).MediaType.equals("IMAGE")) {
                            folderNameFirebase = "IRecall"
                            coverPic = str.get(0).Filename
                        } else {
                            folderNameFirebase = "Thumbnail"
                            coverPic = str.get(0).thumbnail
                        }

                        Log.d(TAG, "arrlist size: " + str.size)
                        Log.d(TAG, "str-caption: " + str.get(0).caption)

                        marker = googleMap.addMarker(MarkerOptions()
                                .position(latLng).title(str1 + "~" + s2 + "~" + str.get(0).MediaId)
                                .snippet("https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.firebaseio.com/o/"+ folderNameFirebase +"%2F" + coverPic + "?alt=media&token=1"))

                        var fireUrl: String = "https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.firebaseio.com/o/"+ folderNameFirebase +"%2F" + coverPic + "?alt=media&token=1"

                        googleMap.setInfoWindowAdapter(object: GoogleMap.InfoWindowAdapter {
                            override fun getInfoWindow(marker: Marker?): View {
                                throw Exception()
                            }

                            override fun getInfoContents(marker: Marker?): View {
                                var view: View = layoutInflater.inflate(
                                        R.layout.map_layout, null)

                                var imageView: ImageView = findViewById(R.id.imgPhotoMap) as ImageView
                                var play: ImageView = findViewById(R.id.playHomeAct) as ImageView
                                var title: TextView = findViewById(R.id.titleMarker) as TextView

                                if (str.get(0).MediaType.equals("IMAGE")) {
                                    play.visibility = View.GONE
                                } else {
                                    play.visibility = View.VISIBLE
                                    play.bringToFront()
                                }

                                try {
                                    Log.d(TAG, "title: " + marker?.title!!.split("~")[0])
                                    title.setText(str1)
                                    Log.d(TAG, "Resources: " + marker.snippet.split("~")[0])

                                    var s: String = String.toString()
                                    var gd: GlideDrawable
                                    Glide.with(applicationContext)
                                            .load(fireUrl)
                                            .listener(object: RequestListener<String, GlideDrawable> {
                                                override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                                                    if (!isFromMemoryCache) {
                                                        marker.showInfoWindow()
                                                    }

                                                    return false
                                                }

                                                override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                                                    e?.printStackTrace()

                                                    return false
                                                }
                                            })
                                            .into(imageView)
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }

                                return view
                            }
                        })

                        googleMap.setOnInfoWindowClickListener { object: GoogleMap.OnInfoWindowClickListener {
                            override fun onInfoWindowClick(marker: Marker?) {
                                var geoCoder: Geocoder = Geocoder(this@HomeActivity,
                                        Locale.getDefault())
                                var addr: String = ""
                                try {
                                    var addresses: List<Address> = geoCoder.getFromLocation(marker?.position!!.latitude,
                                            marker?.position!!.longitude, 1)
                                    var objAddr: Address = addresses.get(0)
                                    addr = objAddr.locality
                                } catch (ex: Exception) {
                                    ex.stackTrace
                                }

                                var key: String = marker?.title!!.split("~")[1]
                                var alist: ArrayList<AlbumDetails> = revMap.get(key) as ArrayList<AlbumDetails>

                                var intentAlbumsPage: Intent = Intent(applicationContext, ViewPostsActivity::class.java)
                                intentAlbumsPage.putExtra("listOfImages", alist)
                                intentAlbumsPage.putExtra("name", name)
                                intentAlbumsPage.putExtra("email", email)
                                intentAlbumsPage.putExtra("photoUri", photoUri)
                                intentAlbumsPage.putExtra("lat", addr)
                                marker.hideInfoWindow()

                                startActivity(intentAlbumsPage)
                            }
                        }}
                    }
                    Log.d(TAG, "revMap size: " + revMap.size)

                    var lat1: Double = latitude
                    var long1: Double = longitude
                    var currLatLng: LatLng = LatLng(lat1, long1)
                    Log.d(TAG, "Filename: " + albumDetails.Filename)

                    trueCount = 0
                    count = 0

                    var albumId: String = albumDetails.AlbumId
                    var mediaId: String = albumDetails.MediaId
                    var filename: String = albumDetails.Filename
                    var lat_load: Double = albumDetails.Latitude.toDouble()
                    var long_load: Double = albumDetails.Longitude.toDouble()

                    Log.d(TAG, "\nValues fetched are: " +
                            "\nAlbumId: " + albumId +
                            "\nMediaId: " + mediaId +
                            "\nFilename: " + filename +
                            "\nlatitude: " + lat_load +
                            "\nlongitude: " + long_load)
                    Log.d(TAG, "Map Ready")

                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(currLatLng))
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot?) {

            }

            override fun onCancelled(dataSnapshot: FirebaseError?) {

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot?, s: String?) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot?, s: String?) {

            }
        })
    }

    override fun onStart() {
        super.onStart()

        var viewSnackbar: View = findViewById(android.R.id.content)
        var nameNew: String
        if (sharedPrefs.getToken(this@HomeActivity).isEmpty()) {
            var intent: Intent = intent

            nameNew = intent.getStringExtra("name")
            Snackbar.make(viewSnackbar, "Logged in as: " + nameNew, Snackbar.LENGTH_LONG)
                    .show()
        } else {
            nameNew = sharedPrefs.getUsername(applicationContext)
            Snackbar.make(viewSnackbar, "Welcome back " + nameNew, Snackbar.LENGTH_LONG)
                    .show()
        }

        storageRef = FirebaseStorage.getInstance().reference
    }

    fun openImageChooser() {
        var intent: Intent = Intent()
        intent.setType("image/*")
        var imageSizeLimit: Long = 12 * 1024 * 1024
        intent.setAction(Intent.ACTION_GET_CONTENT)
        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, imageSizeLimit)

        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    fun openViewChooser() {
        var intent: Intent = Intent()
        intent.setType("video/*")
        var maxVideoSize: Long = 12 * 1024 * 1024
        intent.setAction(Intent.ACTION_GET_CONTENT)
        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, maxVideoSize)

        startActivityForResult(Intent.createChooser(intent, "Select Video"), SELECT_VIDEO)
    }

    override fun onLocationChanged(location: Location?) {
        latitude = location!!.latitude
        longitude = location!!.longitude

        Log.d(TAG, "Location: \nLatitude: " + latitude +
                "\nLongitude: " + longitude)
    }

    override fun onProviderDisabled(provider: String?) {
        Log.d(TAG, "disabled")
    }

    override fun onProviderEnabled(provider: String?) {
        Log.d(TAG, "enabled")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d(TAG, "status" + status)
    }

    fun takingPicture() {
        var pictureIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (pictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    fun makeVideo() {
        var videoIntent: Intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        var fileUri: Uri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO)
        videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)

        if (videoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE)
        }
    }



    fun getOutputMediaFileUri(type: Int): Uri {
        var getOutputFunction: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getOutputFunction = FileProvider.getUriForFile(
                    this@HomeActivity,
                    applicationContext.packageName+".provider",
                    getOutputMediaFile(type))
        } else {
            getOutputFunction = Uri.fromFile(getOutputMediaFile(type))
        }

        return getOutputFunction
    }

    fun getOutputMediaFile(type: Int): File {
        var sdcard: File = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                VIDEO_DIR_NAME)

        if (!sdcard.exists()) {
            if (!sdcard.mkdirs()) {
                Log.d(TAG, VIDEO_DIR_NAME + "something went wrong while creating the file")
            }
        }

        var mediaFile: File? = null
        if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = File(sdcard.path + File.separator +
                    "VID_" + System.currentTimeMillis() + ".mp4")
            fileName = sdcard.path + File.separator +
                    "VID_" + System.currentTimeMillis() + ".mp4"
        }

        return mediaFile!!
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(this@HomeActivity, "Connection unsuccessful", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Connection unsuccessful")
    }
}
