package com.example.chatapp.fragments

import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth


class LoginFragment : Fragment() {


    lateinit var etName: EditText
    lateinit var etEmail: EditText
    lateinit var etMobileNo: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvDirectSignUp: TextView

    lateinit var auth: FirebaseAuth

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

        btnLogin.setOnClickListener{
            login()
        }


        return view
    }

    private fun login() {
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()
        val mobileNo = etMobileNo.text.toString()
        // calling signInWithEmailAndPassword(email, pass)
        // function using Firebase auth object
        // On successful response Display a Toast
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(requireActivity()) {
            if (it.isSuccessful) {
                Toast.makeText(requireContext(), "Successfully LoggedIn", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(),ChatActivity::class.java)
                startActivity(intent)
            } else
                Toast.makeText(requireContext(), "Log In failed ", Toast.LENGTH_SHORT).show()
        }
    }




}