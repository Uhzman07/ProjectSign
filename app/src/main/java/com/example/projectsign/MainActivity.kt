package com.example.projectsign

import android.content.Intent
import android.net.Uri

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.projectsign.ui.theme.ProjectSignTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {

    companion object {
        const val RC_SIGN_IN = 100
    }

    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase auth instance
        mAuth = FirebaseAuth.getInstance()

        // configure Google SignIn
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            ProjectSignTheme {
                if(mAuth.currentUser == null){
                    GoogleSignInButton {
                        signIn()
                    }
                } else {
                    val user : FirebaseUser = mAuth.currentUser!! // This is to show that the the user is inheriting from the Firebase use class and then it is used to represent the current user
                    ProfileScreen(
                        // Also note that the photo Url, displayName and the email are associated with the firebase data
                        profileImage = user.photoUrl!!,
                        name = user.displayName!!,
                        email =user.email!!,
                        SignOutClicked = {
                            signOut()
                        }
                    )
                }

            }
        }
    }

    private fun signIn(){
        val signInIntent = googleSignInClient.signInIntent // This is used to send the intent
        startActivityForResult(signInIntent, RC_SIGN_IN) // Then we need to start an activity for the intent that was sent and note that the second parameter is also the code that is used for validation
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // result returned from launching the intent from GoogleSignInApi.getSignInIntent()
        if(requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data) // The data here is from the intent method that we had used
            val exception = task.exception // "Task" here from the "GoogleSignIn" method that is representing the account that is to be signed into
            if (task.isSuccessful){
                try{
                    // Google SignIn was successful, authenticate with firebase
                    val account = task.getResult(ApiException::class.java)!! // this is then used to authenticate the account with firebase and then remember that this is so important
                    firebaseAuthWithGoogle(account.idToken!!) // This is compulsory
                } catch (e :Exception){
                    // Google SignIn Failed
                    Log.d("SignIn", "Google SignIn Failed")
                }
            } else{
                Log.d("SignIn", exception.toString())
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken : String){
        val credential = GoogleAuthProvider.getCredential(idToken,null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this){ task ->
                if(task.isSuccessful){
                    // SignIn Successful
                    Toast.makeText(this, "SignIn Successful", Toast.LENGTH_SHORT).show()
                    setContent {
                        ProjectSignTheme {
                            val user: FirebaseUser = mAuth.currentUser!!
                            ProfileScreen(
                                profileImage = user.photoUrl!! ,
                                name =  user.displayName!!,
                                email = user.email!!,
                                SignOutClicked = {
                                    signOut()
                                }
                            )

                        }
                    }
                }else{
                    // SignIn Failed
                    Toast.makeText(this, "SignIn Failed", Toast.LENGTH_SHORT).show()


                }

            }
    }
    private fun signOut(){
        // get the google account
        val googleSignInClient : GoogleSignInClient

        // configure Google SignIn
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Sign Out of all accounts
        mAuth.signOut()
        googleSignInClient.signOut()
            .addOnSuccessListener {  // This is to check if it has signed out successfully
                Toast.makeText(this, "Sign Out Successful", Toast.LENGTH_SHORT).show()
                // then if it is successful we want to add a new view
                setContent {
                    ProjectSignTheme {
                        // Now we can call the function that we want our button we created to perform
                        GoogleSignInButton {
                            signIn()
                        }

                    }
                }
            }
            .addOnFailureListener {  // This is used to check any form of failure just in case
            Toast.makeText(this,"Sign Out Failed",Toast.LENGTH_SHORT).show()

        }


    }
}

@Composable
fun GoogleSignInButton(
    signInClicked : () -> Unit
){
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Card(
            modifier = Modifier
                .padding(start = 30.dp, end = 30.dp)
                .height(55.dp)
                .fillMaxWidth()
                .clickable {
                    signInClicked()
                },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(width = 1.5.dp, color = Color.Black),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 5.dp // Note that since we are using material3 so it is quite different, also note that we use the elevation to control the size of the border


            )
        ){
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    modifier = Modifier
                        .padding(start = 15.dp)
                        .size(32.dp)
                        .padding(0.dp)
                        .align(Alignment.CenterVertically),
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "google_logo"
                )
                Text(modifier = Modifier
                    .padding(start = 20.dp)
                    .align(Alignment.CenterVertically),
                    text = "Sign In With Google",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                
                
            }
            
            
        }

    }



}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileImage : Uri,
    name: String,
    email: String,
    SignOutClicked :() -> Unit

){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ){
        Card(

            modifier = Modifier
                .size(150.dp)
                .fillMaxHeight(0.4f),
            shape = RoundedCornerShape(125.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 10.dp
            )

        ){
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = profileImage,
                contentDescription = "profile_photo",
                contentScale = ContentScale.FillBounds

            )

        }
        Column(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .padding(top = 60.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange ={},
                readOnly = true,
                label ={
                    Text(text = "Name")
                },
            )
            OutlinedTextField(
                modifier = Modifier.padding(top = 20.dp),
                value = email,
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(text = "Email")
                }
            )
            Button(
                modifier = Modifier
                    .align(CenterHorizontally)
                    .padding(top = 100.dp),
                onClick = {SignOutClicked()}
            ) {
                Text(text = "LogOut")

            }

        }

    }

}

/*
private fun ColumnScope.Card(modifier: Modifier, shape: RoundedCornerShape, border: BorderStroke, elevation: Dp, content: ColumnScope.() -> Unit) {

}

 */



/*
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProjectSignTheme {
        Greeting("Android")
    }
}

 */
