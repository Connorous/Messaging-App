package com.example.messagingapp.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.messagingapp.databinding.ActivitySignUpBinding;
import com.example.messagingapp.utilities.Constants;
import com.example.messagingapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

// sign up activity class
public class SignUpActivity extends AppCompatActivity {
    //binding for views and preference manager which contains saved preferences
    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    //String to represent the user's image
    private String encodedImage;


    //creates activity, view binding and preference manager
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //binding uses views from Sign Up xml
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        //root of binding is the view
        setContentView(binding.getRoot());
        //create preference manager using app context to store data across activities
        preferenceManager = new PreferenceManager(getApplicationContext());
        //set the onclick listeners of the activity
        setListeners();
    }

    //sets the onclick listeners of the activity
    private void setListeners() {
        //use binding reference to sign in text to set on click listener for sign in text to send user back to sign up page
        binding.SignIn.setOnClickListener(v -> onBackPressed());
        //use binding reference to sign button button to set on click listener for sign up text to validate user details and sign them up
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        //use binding reference to image field to set on click listener to open the users images and let them pick an image for their profile
        binding.layoutImage.setOnClickListener(v -> {
            //pick action is created and passed in to the a ActivityResultLauncher called pickImage
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        //display help when help button is pressed
        binding.helpButton.setOnClickListener(v -> {
            help();
        });
    }

    //shows an inputted message
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    //signs up the user, getting adding their information to the database
    private void signUp() {
        //set loading to true to show progress bar
        loading(true);
        //set database
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        //use hashmap to store user information
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        //use the users collection from the Firebase database JSON, add the user details hashmap
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    //once done no longer loading
                    loading(false);
                    //add the new user's details to the preference manager using a reference to the new document added to the database
                    preferenceManager.putBoolean(Constants.KEY_ID_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    //start the main activity
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                //display an error if there is an issue
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    //creates an encoded string of an image from a bitmap
    private String getEncodedImage(Bitmap bitmap) {
        //set height and width
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        //create a bitmap based on the dimensions above
        Bitmap previewBitMap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        //create byte array output stream of image bitmap
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        //compress bitmap using  byte array output stream
        previewBitMap.compress(Bitmap.CompressFormat.JPEG, 50, byteOutputStream);
        //get actual array from  byte array output stream
        byte[] bytes = byteOutputStream.toByteArray();
        //return byte array convert to a string
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    //ActivityResultLauncher that gets the user to pick an image
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //check result code
                if (result.getResultCode() == RESULT_OK) {
                    //check picked image result is not null
                    if (result.getData() != null) {
                        //get the image result data and turn it into a Uri
                        Uri imageUri = result.getData().getData();
                        try {
                            //try to read the information of the image Uri data with an input stream
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            //decode the input stream with a bitmap
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            //set the user's profile image to the bitmap created
                            binding.imageProfile.setImageBitmap(bitmap);
                            //get rid of the prompt to add an image
                            binding.textAddImage.setVisibility(View.GONE);
                            //set the image string to a string of the bitmap
                            encodedImage = getEncodedImage(bitmap);
                        }
                        catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    //checks the user's details are correct, informs them if they are not
    private Boolean isValidSignUpDetails() {
        //check the encoded string of the image is not null
        if (encodedImage == null) {
            showToast("Select Profile Image");
            return false;
        }
        //check name input is not empty
        else if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Enter your name");
            return false;
        }
        //check email input is not empty
        else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter your email");
            return false;
        }
        //check email input is valid
        else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Email is invalid");
            return false;
        }
        //check password is not empty
        else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter a password");
            return false;
        }
        //check confirm password is not empty
        else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Confirm your password");
            return false;
        }
        //check password and confirm password matches
        else if (!binding.inputConfirmPassword.getText().toString().equals(binding.inputPassword.getText().toString())) {
            showToast("Password and Confirm Password do not match");
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
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else {
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    //displays help messages, informing the user what they can do
    private void help() {
        showToast("Enter your details in the fields above");
        showToast("Then click the circle at the top of the page to add an image.");
        showToast("Click the sign up button when you are done to create an account.");
        showToast("If you wish to return to the sign in page, click sign at the bottom of the page.");
    }
}