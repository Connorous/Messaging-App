package com.example.messagingapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messagingapp.databinding.ActivitySignInBinding;
import com.example.messagingapp.utilities.Constants;
import com.example.messagingapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

//Sign in activity class
public class SignInActivity extends AppCompatActivity {

    //binding for views and preference manager which contains saved preferences
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    //creates activity, view binding and preference manager
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //binding uses views from Sign In xml
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        //root of binding is the view
        setContentView(binding.getRoot());
        //create preference manager using app context to store data across activities
        preferenceManager = new PreferenceManager(getApplicationContext());
        //check if user is signed in, if they are load main activity
        if (preferenceManager.getBoolean(Constants.KEY_ID_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        //set the onclick listeners of the activity
        setListeners();
    }

    //sets the onclick listeners of the activity
    private void setListeners() {
        //use binding reference to Create new account text to set on click listener for Create new account text to start signup activity
        binding.CreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        //use binding reference to sign in button to set on click listener for sign in button to check user details are valid and then sign in
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                signIn();
            }
        });
        //display help when help button is pressed
        binding.helpButton.setOnClickListener(v -> {
            help();
        });
    }

    //signs in the user, getting their information from the database
    private void signIn() {
        //set loading to true to show progress bar
        loading(true);
        //set database
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        //use the users collection from the Firebase database JSON, check for a user that matches the email and password entered
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    //if successful and nothing is null or empty, get a snapshot or current version of the JSON document for the user matched
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        //get current information from document containing user information
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        //add the information from the document to the preference manager, so that it can be used in multiple activities
                        preferenceManager.putBoolean(Constants.KEY_ID_SIGNED_IN, true);
                        //use document id as the user id
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                        //then go to the main activity
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    //otherwise inform the user that they could not be signed in
                    else {
                        loading(false);
                        showToast("Unable to sign in, check your login details");
                    }
                })
                //if there's an issue show an exception
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });

    }

    //shows an inputted message
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    //checks the user's details are correct, informs them if they are not
    private Boolean isValidSignInDetails(){
        //check email is not empty
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter Email");
            return false;
        }
        //check email is valid
        else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Email is invalid");
            return false;
        }
        //check password is not empty
        else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter Password");
            return false;
        }
        else {
            return true;
        }
    }

    //shows the progress bar and hides the sign in button when signing in
    //reshows the sign in button and hides the progress bar if details are incorrect
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else {
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    //displays help messages, informing the user what they can do
    private void help() {
        showToast("Enter your details in the fields above to login");
        showToast("Then click the sign in button when you are done to sign in.");
        showToast("If you wish to return to the sign up page, click sign up at the bottom of the page.");
    }
}