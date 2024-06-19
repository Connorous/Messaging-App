package com.example.messagingapp.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.messagingapp.R;
import com.example.messagingapp.activities.MessageUserActivity;
import com.example.messagingapp.adapters.UsersAdapter;
import com.example.messagingapp.listerners.UserListener;
import com.example.messagingapp.models.User;
import com.example.messagingapp.utilities.Constants;
import com.example.messagingapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ContactableUsersFragment extends Fragment implements UserListener {

    //binding for views and preference manager which contains saved preferences


    private TextView errorMessageTextView;
    private RecyclerView contactsRecyclerView;
    private ProgressBar loadingBar;
    private PreferenceManager preferenceManager;
    //reference to a certain document in the database's json
    private DocumentReference documentReference;
    //reference to a firebase database
    private FirebaseFirestore database;

    public ContactableUsersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_contactable_users, container, false);
        preferenceManager = new PreferenceManager(getContext());
        errorMessageTextView = fragmentView.findViewById(R.id.textErrorMessage);
        contactsRecyclerView = fragmentView.findViewById(R.id.usersRecycleView);
        loadingBar = fragmentView.findViewById(R.id.progressBar);
        database = FirebaseFirestore.getInstance();
        init();
        getUsers();
        return fragmentView;
    }

    private void init() {
        //get database firestore instance
        database = FirebaseFirestore.getInstance();
    }

    //display an error message in a textview if there is an issue
    private void showErrorMessage() {
        errorMessageTextView.setText(String.format("&s", "No users available"));
        errorMessageTextView.setVisibility(View.VISIBLE);
    }

    //gets the users from the database
    private void getUsers() {
        //set loading to true to show progress bar
        loading(true);
        //use the users collection from the Firebase database JSON, get all user documents
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    //stop loading when done
                    loading(false);
                    //store the current user id for comparison
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    //if result is successful and not empty, get a document snapshot of the current documents in the database and loop through it
                    if (task.isSuccessful() && task.getResult() != null) {
                        //create a list of users
                        List<User> users = new ArrayList<>();
                        //for each document in the result, create a user
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            //make sure user details in the document does not match the current user
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            //create a new user, filling in their details with the details from the document
                            User user = new User();
                            user.setName(queryDocumentSnapshot.getString(Constants.KEY_NAME));
                            user.setEmail(queryDocumentSnapshot.getString(Constants.KEY_EMAIL));
                            user.setImage(queryDocumentSnapshot.getString(Constants.KEY_IMAGE));
                            //use document id as user id
                            user.setId(queryDocumentSnapshot.getId());
                            //add user to list
                            users.add(user);
                        }
                        //check if the list contains at least one user
                        if (users.size() > 0) {
                            //create an adaptor, putting the list of users into it
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            //put this adaptor into the recycle view to see the list of users
                            contactsRecyclerView.setAdapter(usersAdapter);
                            //make this recycle view visible
                            contactsRecyclerView.setVisibility(View.VISIBLE);
                        }
                        //if not show an error
                        else {
                            showErrorMessage();
                        }
                    }
                    //if task is not successful show an error
                    else {
                        showErrorMessage();
                    }
                });
    }

    //shows the progress bar and hides the sign in button when signing in
    //reshows the sign in button and hides the progress bar if details are incorrect
    private void loading(Boolean isLoading) {
        if (isLoading) {
            loadingBar.setVisibility(View.VISIBLE);
        }
        else {
            loadingBar.setVisibility(View.INVISIBLE);
        }
    }

    //when the a listed user is clicked on, opens an activity to view and send messages to the user
    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getContext(), MessageUserActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }

}