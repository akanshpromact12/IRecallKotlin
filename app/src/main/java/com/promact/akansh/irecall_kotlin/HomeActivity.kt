package com.promact.akansh.irecall_kotlin

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.Crashlytics
import com.firebase.client.*
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallbacks
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.logentries.logger.AndroidLogger
import de.hdodenhof.circleimageview.CircleImageView
import io.fabric.sdk.android.Fabric
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class HomeActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, android.location.LocationListener, NavigationView.OnNavigationItemSelectedListener {
    private val REQUEST_IMAGE_CAPTURE: Int = 1
    private val REQUEST_VIDEO_CAPTURE: Int = 2
    private lateinit var name: String
    private lateinit var email: String
    private lateinit var photoUri: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var fileName: String
    private lateinit var thumbnailName: String
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
    var mapSize: Int = 0
    var count: Int = 0
    var trueCount: Int = 0
    lateinit var albumDetails: AlbumDetails
    lateinit var arrayList: List<AlbumDetails>
    val revList: MutableList<AlbumDetails> = ArrayList(initialCapacity = 0)
    var revMap: MutableMap<String, ArrayList<AlbumDetails>> = HashMap()
    lateinit var marker: Marker
    var childrenCount: Long = 0
    var showLock: Boolean = false
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var gps: GPSTracker
    lateinit var folderNameFirebase: String
    lateinit var coverPic: String
    lateinit var sharedPrefs: saveSharedPrefs
    lateinit var logger: AndroidLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        Fabric.with(this, Crashlytics())
        try {
            logger = com.logentries.logger.AndroidLogger.createInstance(applicationContext,
                    false, false, false, null, 0,
                    "b7c7c9d7-853b-483a-bb6d-375e727c2ec9", true)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Crashlytics.logException(ex)
        }

        val mapFragment: SupportMapFragment = supportFragmentManager
                .findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        auth = FirebaseAuth.getInstance()
        sharedPrefs = saveSharedPrefs()

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
        floatingActionMenu = findViewById(R.id.floating_action_menu) as FloatingActionMenu
        val relativeLayout: RelativeLayout = findViewById(R.id.content_main) as RelativeLayout

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val txt: TextView = findViewById(R.id.txtView1) as TextView
        getLocation()
        val userId: String

        if (sharedPrefs.getToken(this@HomeActivity).isEmpty()) {
            val intent: Intent = intent
            name = intent.getStringExtra("name")
            email = intent.getStringExtra("email")
            photoUri = intent.getStringExtra("photoUri")
            userId = intent.getStringExtra("userId")
        } else {
            name = sharedPrefs.getUsername(applicationContext)
            email = sharedPrefs.getEmail(applicationContext)
            photoUri = sharedPrefs.getPhotoUri(applicationContext)
            userId = sharedPrefs.getUserId(applicationContext)
        }

        Log.d(TAG, "userid: $userId")
        logger.log("UserId: $userId")
        Toast.makeText(applicationContext, "userId: $userId", Toast.LENGTH_SHORT).show()

        Firebase.setAndroidContext(applicationContext)
        firebase = Firebase("https://irecall-kotlin.firebaseio.com/$userId")
        Log.d(TAG, "firebase db: https://irecall-kotlin.firebaseio.com/$userId")
        logger.log("firebase db: https://irecall-kotlin.firebaseio.com/$userId")

        loadLatLong()

        Log.d(TAG, "id:: $albumId")
        logger.log("id:: $albumId")

        val nav: NavigationView = findViewById(R.id.nav_view) as NavigationView
        nav.itemIconTintList = null
        val navView: View = nav.getHeaderView(0)

        val txtUname: TextView = navView.findViewById(R.id.usernm) as TextView
        val txtEmail: TextView = navView.findViewById(R.id.emailNav) as TextView
        val profilePic: CircleImageView = navView.findViewById(R.id.imgProfile) as CircleImageView

        txtUname.text = name
        txtEmail.text = email
        Glide.with(applicationContext)
                .load(photoUri)
                .centerCrop()
                .into(profilePic)
        txt.text = " "

        val toolbar: Toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        floatingActionCamera.setOnClickListener {
            showAlertDialogBox()

            floatingActionMenu.close(true)
        }

        floatingActionGallery.setOnClickListener { View.OnClickListener {
            showGalleryDialog()

            floatingActionMenu.close(true)
        }}

        val drawer: DrawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle: ActionBarDrawerToggle = ActionBarDrawerToggle(this,
                drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        relativeLayout.setOnTouchListener { _, _ ->
            if (floatingActionMenu.isOpened) {
                floatingActionMenu.close(true)
            }

            true
        }
        nav.setNavigationItemSelectedListener(this)
        FirebaseCrash.log("My First Kotlin App")
    }

    fun getLocation() {
        gps = GPSTracker(this@HomeActivity)

        if (gps.canGetLocation()) {
            latitude = gps.getLatitude()
            longitude = gps.getLongitude()

            Log.d(TAG, "Your current location is: " +
                    "\nLatitude: $latitude" +
                    "\nLongitude: $longitude")
            logger.log("Your current location is: " +
                    "\nLatitude: $latitude" +
                    "\nLongitude: $longitude")
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
        var success: Boolean = true
        if (!folder.exists()) {
            success = folder.mkdir()
        }

        if (success) {
            Log.d(TAG, "Folder Property: Folder created successfully")
            logger.log("Folder Property: Folder created successfully")
        } else {
            Log.d(TAG, "Folder Property: Folder creation unsuccessful")
            logger.log("Folder Property: Folder creation unsuccessful")
        }
    }

    fun loadData(googleMap: GoogleMap) {
        googleMap.setOnMapClickListener {
            GoogleMap.OnMapClickListener {
                _: LatLng? ->
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
                logger.log("strLength: " + childrenCount)

                arrayList = arrayListOf(albumDetails)
                Log.d(TAG, "arrlist size: ${arrayList.size}, " +
                        "arr: ${arrayList[0]}")
                logger.log("arrlist size: ${arrayList.size}")

                revMap.put(albumDetails.AlbumId, java.util.ArrayList<AlbumDetails>())
                if (arrayList.size==childrenCount.toInt()) {
                    if (revList.isNotEmpty()) {
                        revList.clear()
                    }
                    for (i in arrayList.indices.reversed()) {
                        Log.d(TAG, "revList size before addition" + revList.size)
                        revList.add(arrayList[i])
                        Log.d(TAG, "revList size after addition " + revList.size + " i:" + i)
                    }

                    Log.d(TAG, "revList size: "+revList.size+" values: "+ revList[0].caption)
                    logger.log("revList size: "+revList.size+" values: "+ revList[0].caption)

                    for (key: String in revMap.keys) {
                        for (i in revList.indices) {
                            val album = revList[i]
                            if (key == album.AlbumId) {
                                val imagesListOfAlbum = revMap[key]
                                Log.d(TAG, "listOfImages size: ${imagesListOfAlbum?.size}")
                                imagesListOfAlbum?.add(album)
                                Log.d(TAG, "listOfImages size-after: ${imagesListOfAlbum?.size}")
                                revMap.put(key, imagesListOfAlbum!!)
                                Log.d(TAG, "caption - key: " + revMap[key]?.get(0)?.caption)
                            }
                        }
                    }
                    val set: Set<String> = revMap.keys

                    Log.d(TAG, "Children size: " + childrenCount)
                    logger.log("Children size: " + childrenCount)
                    for (s2: String in set) {
                        Log.d(TAG, "Key-> " + s2)
                        logger.log("Key-> " + s2)

                        val str: ArrayList<AlbumDetails> = revMap[s2]!!
                        for (i in str.indices) {
                            Log.d(TAG, "Values ----- " + str[i].caption)
                            logger.log("Values ----- " + str[i].caption)
                        }
                        val str1: String = str[0].caption
                        Log.d(TAG, "Caption Map: " + str1)
                        logger.log("Caption Map: " + str1)

                        //Map Start
                        val latLng: LatLng = LatLng(str[0].Latitude.toDouble(),
                                str[0].Longitude.toDouble())
                        Log.d(TAG, "LatLng: " + latLng)
                        logger.log("LatLng: " + latLng)

                        if (str[0].MediaType == "IMAGE") {
                            folderNameFirebase = "IRecall"
                            coverPic = str[0].Filename
                        } else {
                            folderNameFirebase = "Thumbnail"
                            coverPic = str[0].thumbnail
                        }

                        Log.d(TAG, "arrlist size: " + str.size)
                        logger.log("str-caption: " + str[0].caption)

                        marker = googleMap.addMarker(MarkerOptions()
                                .position(latLng).title(str1 + "~" + s2 + "~" + str[0].MediaId)
                                .snippet("https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.appspot.com/o/$folderNameFirebase%2F$coverPic?alt=media&token=1"))

                        val fireUrl: String = "https://firebasestorage.googleapis.com/v0/b/irecall-kotlin.appspot.com/o/$folderNameFirebase%2F$coverPic?alt=media&token=1"

                        googleMap.setInfoWindowAdapter(object: GoogleMap.InfoWindowAdapter {
                            override fun getInfoWindow(marker: Marker?): View? {
                                return null
                            }

                            override fun getInfoContents(marker: Marker?): View {
                                val view: View = layoutInflater.inflate(
                                        R.layout.map_layout, null)

                                val imageView: ImageView = view.findViewById(R.id.imgPhotoMap) as ImageView
                                val play: ImageView = view.findViewById(R.id.playHomeAct) as ImageView
                                val title: TextView = view.findViewById(R.id.titleMarker) as TextView

                                if (str[0].MediaType == "IMAGE") {
                                    play.visibility = View.GONE
                                } else {
                                    play.visibility = View.VISIBLE
                                    play.bringToFront()
                                }

                                try {
                                    Log.d(TAG, "title: " + marker?.title!!.split("~")[0])
                                    logger.log("title: " + marker.title!!.split("~")[0])
                                    title.text = str1
                                    Log.d(TAG, "Resources: " + marker.snippet.split("~")[0])
                                    logger.log("Resources: " + marker.snippet.split("~")[0])

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
                                    FirebaseCrash.report(ex)
                                }

                                return view
                            }
                        })

                        googleMap.setOnInfoWindowClickListener { marker ->
                            val geocoder = Geocoder(this@HomeActivity,
                                    Locale.getDefault())
                            var addr = ""
                            try {
                                val addresses = geocoder.getFromLocation(marker.position.latitude,
                                        marker.position.longitude, 1)
                                val objAddr = addresses[0]
                                addr = objAddr.locality
                            } catch (e: Exception) {
                                e.printStackTrace()
                                FirebaseCrash.log(e.toString())
                            }

                            val key = marker.title.split("~".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                            val alist = revMap[key]

                            val intentAlbumsPage = Intent(applicationContext, ViewPostsActivity::class.java)
                            intentAlbumsPage.putExtra("listOfImages", alist)
                            intentAlbumsPage.putExtra("name", name)
                            intentAlbumsPage.putExtra("email", email)
                            intentAlbumsPage.putExtra("photoUri", photoUri)
                            intentAlbumsPage.putExtra("lat", addr)
                            marker.hideInfoWindow()

                            startActivity(intentAlbumsPage)
                        }
                    }
                    Log.d(TAG, "revMap size: " + revMap.size)
                    logger.log("revMap size: " + revMap.size)

                    val lat1: Double = latitude
                    val long1: Double = longitude
                    val currLatLng: LatLng = LatLng(lat1, long1)
                    Log.d(TAG, "Filename: " + albumDetails.Filename)
                    logger.log("Filename: " + albumDetails.Filename)

                    trueCount = 0
                    count = 0

                    val albumId: String = albumDetails.AlbumId
                    val mediaId: String = albumDetails.MediaId
                    val filename: String = albumDetails.Filename
                    val lat_load: Double = albumDetails.Latitude.toDouble()
                    val long_load: Double = albumDetails.Longitude.toDouble()

                    Log.d(TAG, "\nValues fetched are: " +
                            "\nAlbumId: " + albumId +
                            "\nMediaId: " + mediaId +
                            "\nFilename: " + filename +
                            "\nlatitude: " + lat_load +
                            "\nlongitude: " + long_load)
                    Log.d(TAG, "Map Ready")
                    logger.log("\nValues fetched are: " +
                            "\nAlbumId: " + albumId +
                            "\nMediaId: " + mediaId +
                            "\nFilename: " + filename +
                            "\nlatitude: " + lat_load +
                            "\nlongitude: " + long_load)
                    logger.log("Map Ready")

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

        val viewSnackbar: View = findViewById(android.R.id.content)
        val nameNew: String
        if (sharedPrefs.getToken(this@HomeActivity).isEmpty()) {
            val intent: Intent = intent

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
        val intent: Intent = Intent()
        intent.type = "image/*"
        val imageSizeLimit: Long = 12 * 1024 * 1024
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, imageSizeLimit)

        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    fun openVideoChooser() {
        val intent: Intent = Intent()
        intent.type = "video/*"
        val maxVideoSize: Long = 12 * 1024 * 1024
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, maxVideoSize)

        startActivityForResult(Intent.createChooser(intent, "Select Video"), SELECT_VIDEO)
    }

    override fun onLocationChanged(location: Location?) {
        latitude = location!!.latitude
        longitude = location.longitude

        Log.d(TAG, "Location: \nLatitude: " + latitude +
                "\nLongitude: " + longitude)
        logger.log("Location: \nLatitude: " + latitude +
                "\nLongitude: " + longitude)
    }

    override fun onProviderDisabled(provider: String?) {
        Log.d(TAG, "disabled")
        logger.log("disabled")
    }

    override fun onProviderEnabled(provider: String?) {
        Log.d(TAG, "enabled")
        logger.log("enabled")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d(TAG, "status" + status)
        logger.log("status" + status)
    }

    fun takingPicture() {
        val pictureIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (pictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    fun makeVideo() {
        val videoIntent: Intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        val fileUri: Uri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO)
        videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)

        if (videoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE)
        }
    }

    fun showGalleryDialog() {
        val options: Array<String> = arrayOf("Select a Photo", "Select a Video")
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)

        dialogBuilder.setTitle("Select from the options given below")
        dialogBuilder.setItems(options, { _, item ->
                    if (options[item] == "Select a Photo") {
                        openImageChooser()
                    } else if (options[item] == "Select a Video") {
                        openVideoChooser()
                    }
                })
        dialogBuilder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener { dialog, _ ->
            dialog.cancel()
        }))

        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    fun showAlertDialogBox() {
        val options = arrayOf<CharSequence>("Take a Photo", "Make a Video")
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)

        alertDialogBuilder.setTitle("Select from the options given below")
        alertDialogBuilder.setItems(options, { dialog, item ->
            if (options[item] == "Take a Photo") {
                takingPicture()

                dialog.cancel()
            } else if (options[item] == "Take a Video") {
                makeVideo()

                dialog.cancel()
            }
        })

        alertDialogBuilder.setNegativeButton(android.R.string.cancel, { dialog, _ ->
            dialog.cancel()
        })

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    fun getFileSize (file: File): Long { return file.length() }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val fileSize: Long

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE ->
                    if (resultCode == Activity.RESULT_OK) {
                        showLock = true
                        val date: String = SimpleDateFormat("yyyyMddHHMMSS", Locale.getDefault())
                                .format(Date())
                        val bundle: Bundle = data!!.extras
                        val bitmap: Bitmap = bundle.get("data") as Bitmap

                        val imageSavedFile: File = saveFileToStorage("IMG_$date.png", bitmap)
                        Log.d(TAG, "filepath: " + imageSavedFile)
                        logger.log("filepath: " + imageSavedFile)
                        val uri: Uri

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            uri = FileProvider.getUriForFile(
                                    this@HomeActivity,
                                    applicationContext.packageName + ".provider",
                                    imageSavedFile)
                        } else {
                            uri = Uri.fromFile(imageSavedFile)
                        }

                        showPhotoDialog(uri)
                    }
            REQUEST_VIDEO_CAPTURE ->
                    if (resultCode == Activity.RESULT_OK) {
                        val file: File = File(fileName)
                        fileSize = getFileSize(file) / 1024
                        showLock = true

                        if ((fileSize /1024) > 5) {
                            showVideoDialog()
                        } else {
                            Toast.makeText(this@HomeActivity,
                                    "Video size is greater than 5 MB",
                                    Toast.LENGTH_SHORT).show()
                        }
                    }
            SELECT_PICTURE ->
                    if (data != null && resultCode == Activity.RESULT_OK) {
                        val selectedImageUri: Uri = data.data
                        showLock = true

                        try {
                            pathImgGallery = getPath(this@HomeActivity, selectedImageUri)!!
                        } catch (e: Exception) {
                            e.printStackTrace()
                            FirebaseCrash.report(e)
                        }

                        showSelectedImageDialog(selectedImageUri)
                    }
            SELECT_VIDEO ->
                if (data != null && resultCode == Activity.RESULT_OK) {
                    val selectedVideoUri: Uri = data.data
                    showLock = true

                    try {
                        pathVideoGallery = getPath(this@HomeActivity, selectedVideoUri)!!
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        FirebaseCrash.report(ex)
                    }

                    showSelectedVideoDialog()
                }
        }
    }

    fun showPhotoDialog(imageUri: Uri) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this, R.style.AppCompatAlertDialog)
        val layoutInflater: LayoutInflater = this.layoutInflater
        val viewGroup: ViewGroup = RelativeLayout(this@HomeActivity)
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_layout, viewGroup, false)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setTitle("Upload Image")

        val txtCaption: EditText = dialogView.findViewById(R.id.txtBoxCaption) as EditText
        val photoImg: ImageView = dialogView.findViewById(R.id.imgPhoto) as ImageView

        Glide.with(this)
                .load(imageUri)
                .into(photoImg)

        val positive: String = getString(android.R.string.ok)
        dialogBuilder.setPositiveButton(positive, { dialog, _ ->
            strCaption = txtCaption.text.toString()
            Log.d(TAG, "caption: " + strCaption)
            logger.log("caption: " + strCaption)

            uploadImageToFirebase(photoImg, strCaption)
            dialog.cancel()
        })

        dialogBuilder.setNegativeButton(android.R.string.cancel, { dialog, _ ->
            dialog.cancel()
        })

        val alertDialog: AlertDialog = dialogBuilder.create()
        try {
            alertDialog.window.setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_ADJUST_RESIZE)
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrash.report(e)
        }

        alertDialog.show()
    }

    fun getPath(context: Context, uri: Uri): String? {
        val isKitkat: Boolean = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)

        if (isKitkat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDoc(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split: List<String> = docId.split(":")
                val type: String = split[0]

                if ("primary" == type) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadDoc(uri)) {
                val id: String = DocumentsContract.getDocumentId(uri)
                val contentUri: Uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        id.toLong())

                return getDataColumns(context, contentUri, null, null)
            } else if (isMediaDoc(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split: List<String> = docId.split(":")
                val type: String = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection: String = "_id=?"
                val selectionArgs: Array<String> = arrayOf(split[1])

                return getDataColumns(context, contentUri!!, selection, selectionArgs)
            }
        } else if ("content" == uri.scheme) {
            return getDataColumns(context, uri, null, null)
        } else if ("file" == uri.path) {
            return uri.path
        }

        return null
    }

    fun getDataColumns(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null

        val col: String = "_data"
        val projection: Array<String> = arrayOf(col)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)

            if (cursor != null && cursor.moveToNext()) {
                val colIndex: Int = cursor.getColumnIndexOrThrow(col)

                return cursor.getString(colIndex)
            }
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }

        return null
    }

    fun isExternalStorageDoc(uri: Uri): Boolean { return "com.android.externalstorage.documents" == uri.authority }

    fun isDownloadDoc(uri: Uri): Boolean { return "com.android.providers.downloads.documents" == uri.authority }

    fun isMediaDoc(uri: Uri): Boolean { return "com.android.providers.media.documents" == uri.authority }

    override fun onResume() { super.onResume() }

    override fun onStop() {
        super.onStop()

        if (true) {
            googleApiClient.disconnect()
        }

        super.onPause()
    }

    fun showVideoDialog() {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this@HomeActivity, R.style.AppCompatAlertDialog)
        val layoutInflater: LayoutInflater = this.layoutInflater
        val viewGroup: ViewGroup = RelativeLayout(this@HomeActivity)
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_layout, viewGroup, false)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setTitle("Upload Video")

        val txtCaption: EditText = dialogView.findViewById(R.id.txtBoxCaption) as EditText
        val videoView: ImageView = dialogView.findViewById(R.id.imgPhoto) as ImageView

        Log.d(TAG, "File name: $fileName")
        logger.log("File name: $fileName")

        val thumbnail: Bitmap = ThumbnailUtils.createVideoThumbnail(fileName, MediaStore.Images.Thumbnails.MINI_KIND)
        videoView.setImageBitmap(thumbnail)

        dialogBuilder.setPositiveButton(android.R.string.ok, { _, _ ->
            strCaption = txtCaption.text.toString()
            Log.d(TAG, "Caption: $strCaption")
            logger.log("Caption: $strCaption")

            val sdcard: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    VIDEO_DIR_NAME)
            Log.d(TAG, "Files: $fileName")
            logger.log("Files: $fileName")

            if (sdcard.exists()) {
                val fileList: Array<File> = sdcard.listFiles()

                for (file: File in fileList) {
                    if (file.toString() == fileName) {
                        Log.d(TAG, "files: " + file.toString())
                        logger.log("files: " + file.toString())

                        uploadVideoToFirebase(file, strCaption)
                    }
                }
            }
        })

        dialogBuilder.setNegativeButton(android.R.string.cancel, { dialog, _ ->
            dialog.cancel()
        })

        val alertDialog: AlertDialog = dialogBuilder.create()
        try {
            alertDialog.window.setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_ADJUST_RESIZE)
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrash.report(e)
        }

        alertDialog.show()
    }

    fun uploadVideoToFirebase(file: File, caption: String) {
        val strCaption: String = caption
        val fileVideo: Uri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileVideo = FileProvider.getUriForFile(this@HomeActivity,
                    applicationContext.packageName + ".provider",
                    file)
        } else {
            fileVideo = Uri.fromFile(file)
        }

        val sdcard: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                VIDEO_DIR_NAME)
        thumbnailName = sdcard.path + File.separator +
                "VID_"+System.currentTimeMillis() + ".mp4"
        val thumbnail: Bitmap = ThumbnailUtils.createVideoThumbnail(fileName,
                MediaStore.Images.Thumbnails.MINI_KIND)
        val fileThumb: File = saveThumbnail("VID_" + System.currentTimeMillis()+
                ".png", sdcard, thumbnail)

        val thumbnailUri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            thumbnailUri = FileProvider.getUriForFile(this@HomeActivity,
                    applicationContext.packageName + ".provider",
                    fileThumb)
        } else {
            thumbnailUri = Uri.fromFile(fileThumb)
        }

        val videoRef: StorageReference = storageRef.child("IRecall")
                .child(fileVideo.lastPathSegment)
        val thumbRef: StorageReference = storageRef.child("Thumbnails")
                .child(thumbnailUri.lastPathSegment)
        val videoUpload: UploadTask = videoRef.putFile(fileVideo)

        videoUpload.addOnFailureListener { OnFailureListener {
            Log.d(TAG, "Something went wrong while uploading the video")
            logger.log("Something went wrong while uploading the video")
        }}.addOnSuccessListener { OnSuccessListener<UploadTask.TaskSnapshot> {
            val date: String = SimpleDateFormat("yyyyMddHHMMSS", Locale.getDefault())
                    .format(Date())
            val thumbUpload: UploadTask = thumbRef.putFile(thumbnailUri)
            thumbUpload.addOnSuccessListener { OnSuccessListener<UploadTask.TaskSnapshot> {
                Log.d(TAG, "Name of thumbnail: $thumbnailName")
                logger.log("Name of thumbnail: $thumbnailName")
                addDbValues(fileVideo.lastPathSegment, strCaption,
                        latitudeAlbum.toDouble(),
                        longitudeAlbum.toDouble(),
                        "V", date, thumbnailName, "VIDEO")
                Toast.makeText(this@HomeActivity, "Video Uploaded successfully",
                        Toast.LENGTH_SHORT).show()
            }}.addOnFailureListener { OnFailureListener {
                Log.d(TAG, "Error while uploading the video")
                logger.log("Error while uploading the video")
            }}
        }}
    }

    fun saveThumbnail(filename: String, sdcard: File, bitmap: Bitmap): File {
        val stream: OutputStream

        var file: File = File(filename)
        if (file.exists()) {
            file.delete()
            file = File(sdcard, filename)
            Log.d(TAG, "File exists: $file, Bitmap= $filename")
            logger.log("File exists: $file, Bitmap= $filename")
        }

        file = File(sdcard, filename)
        try {
            stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrash.report(e)
        }

        Log.d(TAG, "File: $file")
        logger.log("File: $file")
        thumbnailName = filename

        return file
    }

    fun showSelectedImageDialog(bitmap: Uri) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this@HomeActivity,
                R.style.AppCompatAlertDialog)
        val layoutInflater: LayoutInflater = this.layoutInflater
        val viewGroup: ViewGroup = RelativeLayout(this@HomeActivity)
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_layout, viewGroup, false)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setTitle("Upload an Image")

        val txtCaption: EditText = dialogView.findViewById(R.id.txtBoxCaption) as EditText
        val photoImg: ImageView = dialogView.findViewById(R.id.imgPhoto) as ImageView

        photoImg.setImageURI(bitmap)
        dialogBuilder.setPositiveButton(android.R.string.ok, { _, _ ->
            strCaption = txtCaption.text.toString()
            Log.d(TAG, "Caption: $strCaption")
            logger.log("Caption: $strCaption")

            uploadGalleryImageToFirebase(pathImgGallery, strCaption)

            val alert: AlertDialog = dialogBuilder.create()
            alert.cancel()
        })

        dialogBuilder.setNegativeButton(android.R.string.cancel, { dialog, _ ->
            dialog.cancel()
        })

        val alertDialog: AlertDialog = dialogBuilder.create()
        try {
            alertDialog.window.setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_ADJUST_RESIZE)
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrash.report(e)
        }

        alertDialog.show()
    }

    fun uploadGalleryImageToFirebase(galleryImage: String, caption: String) {
        val file: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            file = FileProvider.getUriForFile(this@HomeActivity,
                    applicationContext.packageName + ".provider",
                    File(galleryImage))
        } else {
            file = Uri.fromFile(File(galleryImage))
        }

        val filenm = file.lastPathSegment
        val date: String = SimpleDateFormat("yyyyMddHHMMSS", Locale.getDefault())
                .format(Date())
        val galleryRef: StorageReference = storageRef.child("IRecall")
                .child(file.lastPathSegment)
        val galleryImgUpload: UploadTask = galleryRef.putFile(file)

        galleryImgUpload.addOnFailureListener { OnFailureListener {
            Log.d(TAG, "Something went wrong while uploading the image")
            logger.log("Something went wrong while uploading the image")
        }}.addOnSuccessListener { OnSuccessListener<UploadTask.TaskSnapshot> {
            addDbValues(filenm, caption,
                    latitudeAlbum.toDouble(),
                    longitudeAlbum.toDouble(),
                    "I", date, "", "IMAGE")
            Toast.makeText(this@HomeActivity, "Gallery image uploaded successfully",
                    Toast.LENGTH_SHORT).show()
            revMap.clear()
        }}
    }

    fun showSelectedVideoDialog() {
        val dialogBuidler: AlertDialog.Builder = AlertDialog.Builder(this@HomeActivity,
                R.style.AppCompatAlertDialog)
        val layoutInflater: LayoutInflater = this.layoutInflater
        val viewGroup: ViewGroup = RelativeLayout(this@HomeActivity)
        val dialogView: View = layoutInflater.inflate(R.layout.dialog_layout, viewGroup, false)
        dialogBuidler.setView(dialogView)
        dialogBuidler.setTitle("Upload Video")

        val txtCaption: EditText = dialogView.findViewById(R.id.txtBoxCaption) as EditText
        val thumbnail = ThumbnailUtils.createVideoThumbnail(pathVideoGallery, MediaStore.Images.Thumbnails.MINI_KIND)
        val videoImg: ImageView = dialogView.findViewById(R.id.imgPhoto) as ImageView

        videoImg.setImageBitmap(thumbnail)

        dialogBuidler.setPositiveButton(android.R.string.ok, { _, _ ->
            strCaption = txtCaption.text.toString()
            Log.d(TAG, "Caption: $strCaption")
            logger.log("Caption: $strCaption")

            uploadGalleryVideoToFirebase(pathVideoGallery, strCaption)

            val alert: AlertDialog = dialogBuidler.create()
            alert.cancel()
        })

        dialogBuidler.setNegativeButton(android.R.string.cancel, { dialog, _ ->
            dialog.cancel()
        })

        val alertDialog: AlertDialog = dialogBuidler.create()
        try {
            alertDialog.window.setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_ADJUST_RESIZE)
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrash.report(e)
        }

        alertDialog.show()
    }

    fun uploadGalleryVideoToFirebase (galleryVideo: String, caption: String) {
        val file: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            file = FileProvider.getUriForFile(
                    this@HomeActivity,
                    applicationContext.packageName + ".provider",
                    File(galleryVideo))
        } else {
            file = Uri.fromFile(File(galleryVideo))
        }

        val filenm: String = file.lastPathSegment
        val sdcard: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                VIDEO_DIR_NAME)
        thumbnailName = sdcard.path + File.separator + filenm
        val thumbnail: Bitmap = ThumbnailUtils.createVideoThumbnail(file.path,
                MediaStore.Images.Thumbnails.MINI_KIND)
        val fileThumb: File = saveThumbnail(filenm.replace(".mp4", ".png"),
                sdcard, thumbnail)
        val thumbnailUri: Uri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            thumbnailUri = FileProvider.getUriForFile(this@HomeActivity,
                    applicationContext.packageName + ".provider",
                    fileThumb)
        } else {
            thumbnailUri = Uri.fromFile(fileThumb)
        }

        val galleryRef: StorageReference = storageRef.child("IRecall")
                .child(file.lastPathSegment)
        val thumbRef: StorageReference = storageRef.child("Thumbnails")
                .child(thumbnailUri.lastPathSegment)

        val galleryImgUpload: UploadTask = galleryRef.putFile(file)
        val date: String = SimpleDateFormat("yyyyMddHHMMSS", Locale.getDefault())
                .format(Date())
        galleryImgUpload.addOnFailureListener { OnFailureListener {
            Log.d(TAG, "Error while uploading the image from gallery")
            logger.log("Error while uploading the image from gallery")
        }}.addOnSuccessListener { OnSuccessListener<UploadTask.TaskSnapshot> {
            val thumbUpload: UploadTask = thumbRef.putFile(thumbnailUri)
            thumbUpload.addOnSuccessListener { OnSuccessListener<UploadTask.TaskSnapshot> {
                addDbValues(filenm, caption,
                        latitudeAlbum.toDouble(),
                        longitudeAlbum.toDouble(),
                        "V", date, thumbnailName,
                        "VIDEO")
                Toast.makeText(this@HomeActivity,
                        "Gallery Video Uploaded sucessfully",
                        Toast.LENGTH_SHORT).show()
                revMap.clear()
            }}.addOnFailureListener { OnFailureListener {
                Log.d(TAG, "Error while uploading thumbnail")
                logger.log("Error while uploading thumbnail")
            }}
        }}
    }

    fun uploadImageToFirebase(photoImg: ImageView, caption: String) {
        val timestamp: String = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        .format(Date())
        val strCapt: String = caption

        val imageStorage: StorageReference = storageRef.child("IRecall")
                .child("IMG_$timestamp.png")
        photoImg.isDrawingCacheEnabled = true
        photoImg.buildDrawingCache()

        val bitmap: Bitmap = photoImg.drawingCache
        val outputStream: ByteArrayOutputStream =ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val data: ByteArray = outputStream.toByteArray()

        val uploadTask: UploadTask = imageStorage.putBytes(data)
        uploadTask.addOnFailureListener(this@HomeActivity, {
            Toast.makeText(this@HomeActivity,
                    "Upload was unsuccessful",
                    Toast.LENGTH_SHORT).show()
        }).addOnSuccessListener(this@HomeActivity, { taskSnapshot ->
            Toast.makeText(this@HomeActivity,
                    "Upload was successful",
                    Toast.LENGTH_SHORT).show()
            val date: String = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                    .format(Date())
            addDbValues("IMG_$timestamp.png", strCapt,
                    latitudeAlbum.toDouble(),
                    longitudeAlbum.toDouble(), "I", date, "", "IMAGE")
            downloadUri = taskSnapshot?.downloadUrl!!
            Log.d(TAG, "Uri: " + downloadUri)
            logger.log("Uri: " + downloadUri)
        })
    }

    fun addDbValues(filename: String, caption: String, latitude: Double,
                    longitude: Double, mediaIdentify: String, date: String,
                    thumbnailName: String, mediaType: String) {
        val random: Random = Random()
        Log.d(TAG, "AlbumId: $albumId")
        Log.d(TAG, "lat: $latitude, long: $longitude")
        logger.log("AlbumId: $albumId")
        logger.log("lat: $latitude, long: $longitude")

        val map: MutableMap<String, String> = HashMap()
        if (albumId == "") {
            map.put("AlbumId", (random.nextInt(1081) + 20).toString())
        } else {
            map.put("AlbumId", albumId)
        }

        map.put("MediaId", mediaIdentify+"_"+(random.nextInt(1081) + 20).toString())
        map.put("Filename", filename)
        map.put("caption", caption)
        map.put("Latitude", latitude.toString())
        map.put("Longitude", longitude.toString())
        map.put("Date", date)
        map.put("Thumbnail", thumbnailName)
        map.put("MediaType", mediaType)

        firebase.push().setValue(map)
    }

    fun loadLatLong() {
        albumId = ""
        latitudeAlbum = latitude.toString()
        longitudeAlbum = longitude.toString()

        firebase.addChildEventListener( object: ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot?, s: String?) {
                val map: Map<*,*>? = dataSnapshot?.getValue(Map::class.java)
                newStr = Array(size = map?.size!!) { "" }
                mapSize = map.size

                val lat: Double = map["Latitude"].toString().toDouble()
                val longi: Double = map["Longitude"].toString().toDouble()

                Log.d(TAG, "Values: Latitude is $lat, longitude is $longi")
                logger.log("Values: Latitude is $lat, longitude is $longi")
                val album: String = calcDistance(latitudeAlbum.toDouble(),
                        longitudeAlbum.toDouble(), lat, longi)
                val random: Random = Random()
                newStr = arrayOf("lat: $lat, long: $longi")

                if (map.isNotEmpty()) {
                    if (album == "same") {
                        albumId = map["AlbumId"].toString()
                        Log.d(TAG, "Albumid: $albumId")
                        logger.log("Albumid: $albumId")
                        latitudeAlbum = map["Latitude"].toString()
                        longitudeAlbum = map["Longitude"].toString()
                    } else {
                        albumId = (random.nextInt(1081) + 20).toString()
                        latitudeAlbum = latitude.toString()
                        longitudeAlbum = longitude.toString()
                    }
                } else {
                    albumId = (random.nextInt(1081) + 20).toString()
                    latitudeAlbum = latitude.toString()
                    longitudeAlbum = longitude.toString()
                }

                Log.d(TAG, "lat: $latitudeAlbum, longi: $longitudeAlbum")
                logger.log("lat: $latitudeAlbum, longi: $longitudeAlbum")
            }

            override fun onChildMoved(p0: DataSnapshot?, p1: String?) {

            }

            override fun onCancelled(p0: FirebaseError?) {

            }

            override fun onChildChanged(p0: DataSnapshot?, p1: String?) {

            }

            override fun onChildRemoved(p0: DataSnapshot?) {

            }
        })

        Log.d(TAG, "albumid: $albumId")
        logger.log("albumid: $albumId")
    }

    fun calcDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val theta: Double = lon1 - lon2
        var dist: Double = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) +
                  Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                  Math.cos(deg2rad(theta))

        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist *= 60 * 1.1515
        dist *= 1.609344

        Log.d(TAG, "Distance: $dist")
        logger.log("Distance: $dist")

        val albumid: String
        if (dist < 1) {
            albumid = "same"
        } else {
            albumid = "different"
        }

        return albumid
    }

    fun deg2rad(deg: Double): Double { return (deg * Math.PI / 180.0) }

    fun rad2deg(rad: Double): Double { return (rad * 180 / Math.PI) }

    fun saveFileToStorage(fileName: String, bitmap: Bitmap): File {
        val sdcard: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() +
                File.separator + "IRecall_Images"

        val filePath: File = File(sdcard, fileName)
        val stream: FileOutputStream

        try {
            stream = FileOutputStream(filePath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            MediaStore.Images.Media.insertImage(contentResolver,
                    bitmap, filePath.path, fileName)
            Log.d(TAG, "File successfully saved")
            logger.log("File successfully saved")
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrash.report(e)
        }

        return filePath
    }

    fun getOutputMediaFileUri(type: Int): Uri {
        val getOutputFunction: Uri
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
        val sdcard: File = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                VIDEO_DIR_NAME)

        if (!sdcard.exists()) {
            if (!sdcard.mkdirs()) {
                Log.d(TAG, VIDEO_DIR_NAME + "something went wrong while creating the file")
                logger.log(VIDEO_DIR_NAME + "something went wrong while creating the file")
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId

        if (id == R.id.nav_logout) {
            if (googleApiClient.isConnected) {
                signOut()
            }
        }

        val drawer: DrawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)

        return true
    }

    fun signOut() {
        auth.signOut()

        Auth.GoogleSignInApi.signOut(googleApiClient)
                .setResultCallback { object: ResultCallbacks<Status>() {
                    override fun onSuccess(status: Status) {
                        val intent: Intent = Intent(applicationContext,
                                MainActivity::class.java)
                        val editor: SharedPreferences.Editor = sharedPrefs
                                .getSharedPreferences(applicationContext).edit()
                        editor.clear()
                        editor.apply()
                        finish()

                        startActivity(intent)
                    }

                    override fun onFailure(status: Status) {
                        Log.d(TAG, "Logout Unsuccessful")
                        logger.log("Logout Unsuccessful")
                    }
                }}
    }

    override fun onConnected(p0: Bundle?) {
        Log.d(TAG, "Connected")
        logger.log("Connected")
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d(TAG, "Connection Suspended")
        logger.log("Connection Suspended")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(this@HomeActivity, "Connection unsuccessful", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Connection unsuccessful")
        logger.log("Connection unsuccessful")
    }
}