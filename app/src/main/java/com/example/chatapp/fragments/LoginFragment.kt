package com.example.chatapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.chatapp.R
import com.example.chatapp.activities.ChatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase


class LoginFragment : Fragment() {


    lateinit var etName: EditText
    lateinit var etEmail: EditText
    lateinit var etMobileNo: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: Button
    private lateinit var tvDirectSignUp: TextView

    lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        etEmail = view.findViewById<EditText>(R.id.emailEditText)
        etPass = view.findViewById<EditText>(R.id.passwordEditText)
        etMobileNo = view.findViewById<EditText>(R.id.mobileNoEditText)
        btnLogin = view.findViewById<Button>(R.id.loginButton)
        btnGoogleSignIn = view.findViewById<Button>(R.id.googleSignInButton)
        tvDirectSignUp = view.findViewById<TextView>(R.id.signupTextView)

        auth = FirebaseAuth.getInstance()

        tvDirectSignUp.setOnClickListener(){
            if (savedInstanceState == null) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SignUpFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        btnLogin.setOnClickListener {
            login()
        }

//        btnGoogleSignIn.setOnClickListener {
//            signIn()
//        }

        return view
    }

    private fun login() {
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()
        val mobileNo = etMobileNo.text.toString()

        
        if(email.isNotEmpty() || pass.isNotEmpty() || mobileNo.isNotEmpty())
        {
            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(requireActivity()) {
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), "Successfully LoggedIn", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(),ChatActivity::class.java)
                    startActivity(intent)
                } else
                    Toast.makeText(requireContext(), "Log In failed ", Toast.LENGTH_SHORT).show()
            }
        }
        else
        {
            Toast.makeText(requireContext(),"Please fill all fields",Toast.LENGTH_SHORT).show()
        }

    }


    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(requireContext(), "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), ChatActivity::class.java))

                } else {
                    Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
    companion object {
        private const val RC_SIGN_IN = 9001
    }
}


