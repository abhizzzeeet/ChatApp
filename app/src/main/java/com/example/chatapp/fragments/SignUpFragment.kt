package com.example.chatapp.fragments

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
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit


class SignUpFragment : Fragment() {

    lateinit var etName: EditText
    lateinit var etEmail: EditText
    lateinit var etMobileNo: EditText
    private lateinit var etPass: EditText
    lateinit var btnSendCode: Button
    lateinit var etCode: EditText
    private lateinit var btnSignUp: Button
    private lateinit var verificationId: String


    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        etName = view.findViewById<EditText>(R.id.nameEditText)
        etEmail = view.findViewById<EditText>(R.id.emailEditText)
        etPass = view.findViewById<EditText>(R.id.passwordEditText)
        etMobileNo = view.findViewById<EditText>(R.id.mobileNoEditText)
        btnSignUp = view.findViewById<Button>(R.id.signUpButton)
//        btnSendCode = view.findViewById<Button>(R.id.sendCodeButton)
//        etCode = view.findViewById<EditText>(R.id.codeEditText)

        // Initialising auth object
        auth = Firebase.auth





        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPass.text.toString()


            if (email.isNotEmpty() && password.isNotEmpty()) {
                signUpWithEmail(email, password)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }


    private fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    val userMap = hashMapOf(
                        "name" to etName.text.toString(),
                        "email" to email,
                        "phone" to etMobileNo.text.toString()
                    )

                    userId?.let {
                        FirebaseDatabase.getInstance().getReference("users").child(it).setValue(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Signup successful", Toast.LENGTH_SHORT).show()
                                // Redirect to main activity
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to save user data", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }



}