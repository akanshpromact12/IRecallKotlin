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
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        if (!sharedPrefs.getToken(this@MainActivity).isEmpty()) {
            val options: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    //.requestIdToken(getString(R.string))
                    .requestEmail()
                    .build()

            val googleApiClient: GoogleApiClient = GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, options)
                    .build()
            val signInButton: com.google.android.gms.common.SignInButton = findViewById(R.id.btnGoogleSignIn) as SignInButton
            val listPermissions: Array<String> = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE)

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
                            "Kindly grant all the permissions",
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            View.OnClickListener {
                                ActivityCompat.requestPermissions(this@MainActivity,
                                        listPermissions, REQUEST_PERMISSIONS)
                            }).show()
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity,
                            listPermissions, REQUEST_PERMISSIONS)
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
        } else {
            user = auth.currentUser!!

            val intent: Intent = Intent(applicationContext, HomeActivity::class.java)
            startActivity(intent)
        }

        if (intent.getBooleanExtra("EXIT", false)) {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result: GoogleSignInResult = Auth.GoogleSignInApi
                    .getSignInResultFromIntent(data)
            Log.d(TAG, "In google sign in method")
            handleSignInResult(result)
        }
    }

    fun handleSignInResult(result: GoogleSignInResult) {
        Log.d(TAG, "handleSignInResult" + result.isSuccess)
        Log.d(TAG, "status: " + result.status)

        if (result.isSuccess) {
            val account: GoogleSignInAccount = result.signInAccount!!

            firebaseAuthWithGoogle(account)
        } else {
            Toast.makeText(applicationContext, "Login unsuccessful", Toast.LENGTH_SHORT).show()
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "<------------Login details------------>")
        showProgressDialog()
        val credential: AuthCredential = GoogleAuthProvider
                .getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener {
                    task: Task<AuthResult> ->
                        if (task.isSuccessful) {
                            user = auth.currentUser!!

                            idToken = account.idToken!!
                            name = account.displayName!!
                            email = account.email!!
                            photoUri = account.photoUrl!!

                            Log.d(TAG, "id of: " + user.uid)

                            val intent: Intent = Intent(applicationContext, HomeActivity::class.java)
                            intent.putExtra("idToken", idToken)
                            intent.putExtra("name", name)
                            intent.putExtra("email", email)
                            intent.putExtra("photoUri", photoUri)
                            intent.putExtra("userId", user.uid)
                            sharedPrefs.setPrefs(applicationContext, idToken, name, email,
                                    photoUri.toString(), user.uid)

                            startActivity(intent)
                        } else {
                            Toast.makeText(this@MainActivity, "Authentication Unsuccessful",
                                    Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Authentication Unsuccessful")
                            task.exception
                        }

                    hideProgressDialog()
                }
    }

    fun showProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.loading))
        progressDialog.setIndeterminate(true)

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
    }

    override fun onBackPressed() {
        super.onBackPressed()

        val intent: Intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        finish()
    }
}
