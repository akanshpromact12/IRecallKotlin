package com.promact.akansh.irecall_kotlin

import android.app.ProgressDialog
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import io.fabric.sdk.android.Fabric

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private val RC_SIGN_IN: Int = 1
    private lateinit var idToken: String
    private lateinit var email: String
    private lateinit var name: String
    private lateinit var photoUri: Uri
    private val TAG: String = "MainActivity"
    private val REQUEST_PERMISSIONS: Int = 20
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var progressDialog: ProgressDialog
    private val sharedPrefs: saveSharedPrefs = saveSharedPrefs()
    private lateinit var logger: com.logentries.logger.AndroidLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()

        Fabric.with(this, Crashlytics())
        try {
            logger = com.logentries.logger.AndroidLogger.createInstance(applicationContext,
                    false, false, false, null, 0,
                    "b7c7c9d7-853b-483a-bb6d-375e727c2ec9", true)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Crashlytics.logException(ex)
        }

        if (sharedPrefs.getToken(this@MainActivity).isEmpty()){
            Log.d(TAG, "clientId: "+getString(R.string.default_web_client_id))
            logger.log("clientId: "+getString(R.string.default_web_client_id))
            Toast.makeText(this,
                    "clientId: "+getString(R.string.default_web_client_id),
                    Toast.LENGTH_LONG).show()
            val options: GoogleSignInOptions = GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()

            //The next step is to build a GoogleApiClient
            val googleApiClient: GoogleApiClient = GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, options)
                    .build()
            val signInButton: com.google.android.gms.common.SignInButton = findViewById(R.id.btnGoogleSignIn) as SignInButton

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    + ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    + ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                    + ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale
                        (this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                        ActivityCompat.shouldShowRequestPermissionRationale
                                (this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        ActivityCompat.shouldShowRequestPermissionRationale
                                (this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                        ActivityCompat.shouldShowRequestPermissionRationale
                                (this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Snackbar.make(findViewById(android.R.id.content),
                            "Please Grant all permissions",
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            {
                                ActivityCompat.requestPermissions(this@MainActivity,
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION),
                                        REQUEST_PERMISSIONS)
                            }).show()
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION),
                                    REQUEST_PERMISSIONS)
                }
            } else {
                signInButton.setOnClickListener {
                    val signInIntent: Intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
                    startActivityForResult(signInIntent, RC_SIGN_IN)
                }
            }
            signInButton.setOnClickListener {
                val signInIntent: Intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
        else{
            val intent: Intent = Intent(applicationContext, HomeActivity::class.java)
            startActivity(intent)
        }

        if (intent.getBooleanExtra("EXIT", false)) {
            finish()
        }

        /*user = auth.currentUser!!
        Log.i("IRecall user", "user: " + user)*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result: GoogleSignInResult = Auth.GoogleSignInApi
                    .getSignInResultFromIntent(data)
            Log.d(TAG, "In google sign in method")
            logger.log("In google sign in method")
            handleSignInResult(result)
        }
    }

    fun handleSignInResult(result: GoogleSignInResult) {
        Log.d(TAG, "handleSignInResult" + result.isSuccess)
        Log.d(TAG, "status: " + result.status)
        logger.log("handleSignInResult" + result.isSuccess)
        logger.log("status: " + result.status)

        if (result.isSuccess) {
            val account: GoogleSignInAccount = result.signInAccount!!

            firebaseAuthWithGoogle(account)
        } else {
            Toast.makeText(applicationContext, "Login unsuccessful", Toast.LENGTH_SHORT).show()
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "<------------Login details------------>")
        logger.log("<------------Login details------------>")
        showProgressDialog()
        Log.d(TAG, "idToken: ${account.idToken}")
        logger.log("idToken: ${account.idToken}")
        val credential: AuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener {
                    task: Task<AuthResult> ->
                        if (task.isSuccessful) {
                            user = auth.currentUser!!
                            Log.d(TAG, "UserId is: $user")
                            Toast.makeText(applicationContext,
                                    "UserId is: $user", Toast.LENGTH_SHORT)
                                    .show()

                            idToken = account.idToken!!
                            name = account.displayName!!
                            email = account.email!!
                            photoUri = account.photoUrl!!

                            Log.d(TAG, "id of: " + user.uid)
                            logger.log("id of: " + user.uid)

                            val intent: Intent = Intent(applicationContext, HomeActivity::class.java)
                            intent.putExtra("idToken", idToken)
                            intent.putExtra("name", name)
                            intent.putExtra("email", email)
                            intent.putExtra("photoUri", photoUri)
                            intent.putExtra("userId", user.uid)
                            intent.putExtra("logger", "true")
                            sharedPrefs.setPrefs(applicationContext, idToken, name, email,
                                    photoUri.toString(), user.uid)

                            startActivity(intent)
                        } else {
                            Toast.makeText(this@MainActivity, "Authentication Unsuccessful",
                                    Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Authentication Unsuccessful")
                            logger.log("Authentication Unsuccessful")
                            task.exception
                        }

                    hideProgressDialog()
                }
    }

    fun showProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.loading))
        progressDialog.isIndeterminate = true

        progressDialog.show()
    }

    fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "Connection Failed: " + connectionResult)
        logger.log("Connection Failed: " + connectionResult)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        val intent: Intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)

        finish()
    }
}
