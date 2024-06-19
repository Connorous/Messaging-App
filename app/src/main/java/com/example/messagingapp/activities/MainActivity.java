package com.example.messagingapp.activities;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;


import com.example.messagingapp.R;
import com.example.messagingapp.adapters.RecentConversationsAdapter;
import com.example.messagingapp.databinding.ActivityMainBinding;
import com.example.messagingapp.listerners.ConversationListener;
import com.example.messagingapp.models.MessageContents;
import com.example.messagingapp.models.User;
import com.example.messagingapp.utilities.Constants;
import com.example.messagingapp.utilities.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConversationListener {

    //binding for views
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    //list of messageContents objects
    private List<MessageContents> conversations;
    //conversations adapter used to display conversations in view holders in recycler view
    private RecentConversationsAdapter conversationsAdapter;
    //database
    private FirebaseFirestore database;
    //reference to firebase document json for the current user
    private DocumentReference documentReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //initialises everything
        init();
        //loads the current users details into the xml headings
        loadUserDetails();
        //set on click listeners for activity
        setListeners();
        //lists the conversations the user has had
        listenConversations();
        //shows a dialog asking for permission to send notifications if they haven't already been granted
        showPermissionDialog();
        //listens for any new messages and notifies the user
        listenNewMessages();

        //check if the layout for the list of unContacted users is open
        if (binding.unContactedUsersLayout.getVisibility() == View.VISIBLE) {
            //if it is set other stuff to be gone, so that it doesn't accidentally get clicked on
            binding.conversationRecyclerView.setVisibility(View.GONE);
            binding.addNewChat.setVisibility(View.GONE);
            binding.helpButton.setVisibility(View.GONE);
        }
    }

    //initialises everything
    private void init() {
        //create preference manager using app context to store data across activities
        preferenceManager = new PreferenceManager(getApplicationContext());
        //array list of the conversations
        conversations = new ArrayList<>();
        //adapter used by recycleview, uses list of conversations to display them
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        //attach adapter to recycleview
        binding.conversationRecyclerView.setAdapter(conversationsAdapter);
        //initialise database
        database = FirebaseFirestore.getInstance();
        //set document reference to the database users json belonging to the user
        documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
    }

    //sets on click listeners for views
    private void setListeners() {
        //when additional options button is pressed display additional options
        binding.imageAdditionalOptions.setOnClickListener(v -> toggleAdditionalOptions());
        //when close options button is pressed close additional options
        binding.imageCloseOptions.setOnClickListener(v -> toggleAdditionalOptions());
        //when + button is pressed display fragment containing a list of all users
        binding.addNewChat.setOnClickListener(v -> toggleUnContactedUsers());
        //when account info button is pressed display account info
        binding.accountInfoButton.setOnClickListener(v -> toggleAccountInfo());
        //when close account info button is pressed close account info
        binding.imageCloseAccountInfo.setOnClickListener(v -> toggleAccountInfo());
        //when map service button is pressed display map fragment
        binding.viewMapButton.setOnClickListener(v -> toggleMapView());
        //when sign out button is pressed sign the user out
        binding.signOutButton.setOnClickListener(v -> signOut());
        //when close map service button is pressed close map fragment
        binding.imageCloseMap.setOnClickListener(v -> toggleMapView());
        //when close contact list button is pressed close fragment containing a list of all users
        binding.imageCloseContactsList.setOnClickListener(v -> toggleUnContactedUsers());
        //display help when help button is pressed
        binding.helpButton.setOnClickListener(v -> {
            help();
        });
    }

    //loads the users details
    private void loadUserDetails() {
        //set the name field to the user's username
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        //also set the name field in the account info section to the user's username
        binding.textUserName.setText("Username: " + preferenceManager.getString(Constants.KEY_NAME));
        //set the email field in the account info section to the user's email
        binding.textEmail.setText("Email: " + preferenceManager.getString(Constants.KEY_EMAIL));
        //create a bitmap out of the user's profile image string
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        //assign the bitmap to the image view for the user's profile
        binding.imageProfile.setImageBitmap(bitmap);
    }

    //shows an inputted message
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    //asks for permissions to send notifications
    private void showPermissionDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("This permission is needed to receive Notifications for chat messages")
                            .setPositiveButton("OK", (dialog, listenerInterface) ->
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                    }, 101))
                            .setNegativeButton("Cancel", (dialog, listenerInterface) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.POST_NOTIFICATIONS
                    }, 101);
                }
            }
        }
    }


    //shows a notification using the sender name to identify who sent it
    private void showNotification(String senderName, String message) {
        //make a channel for Chateract
        String chanelID = "Chateract Messaging";
        //use a notification manager for the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //check build version, if it is older a notification channel object is needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(chanelID);
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel(chanelID, "Messages", NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.setDescription("This is messaging notification channel");
                notificationChannel.enableVibration(true);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        //build the notification with a builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), chanelID);
        //set title to the sender's name and set the message to the message sent
        builder.setContentTitle(senderName);
        builder.setContentText(message);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        //set notification image to the message icon
        builder.setSmallIcon(R.drawable.ic_message);
        //set category to social
        builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
        //notify the use with the notification
        notificationManager.notify(0, builder.build());

    }

    //listens for conversations that the current has had with other users
    private void listenConversations() {
        //create a snapshot listener for the conversation collection json in the database where the sender id is the user's id
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        //create a snapshot listener for the conversation collection json in the database where the receiver id is the user's id
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    //event listener for the conversations json listener above
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        //if there is an error don't do anything more
        if (error != null) {
            return;
        }
        //otherwise go through the conversations collection json document
        if (value != null) {
            //for a given change in the json document
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                //if the type is added it is a new conversation added to database
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    //set senderId to the sender id in the document
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    //set receiverId to the receiver id in the document
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    //make a message contents object for the message
                    MessageContents messageContents = new MessageContents();
                    //set the ids in the message contents object
                    messageContents.setSenderId(senderId);
                    messageContents.setReceiverId(receiverId);
                    //if the user id matches the sender id, use the receiver info from the document for the conversation name, conversationId and contact image
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                        messageContents.setConversationImage(documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE));
                        messageContents.setConversationName(documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME));
                        messageContents.setConversationId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                    }
                    //otherwise use the send info
                    else {
                        messageContents.setConversationImage(documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE));
                        messageContents.setConversationName(documentChange.getDocument().getString(Constants.KEY_SENDER_NAME));
                        messageContents.setConversationId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                    }
                    //use the message from the document for the conversation message
                    messageContents.setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                    //get the dateb object from the document for the date of the last message
                    messageContents.setDateObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    //add the message contents object to the conversations list
                    conversations.add(messageContents);
                }
                //if the document type instead is modified then just update the message object in the conversations list
                else if (documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for (int i = 0; i < conversations.size(); i++) {
                        //set senderId to the sender id in the document
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        //set receiverId to the receiver id in the document
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        //if a conversation in the conversations collection json has a matching sender and receiver id
                        //re get the message and the date object from the document
                        if (conversations.get(i).getSenderId().equals(senderId) && conversations.get(i).getReceiverId().equals(receiverId)) {
                            conversations.get(i).setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                            conversations.get(i).setDateObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                            //show a notification when the message to inform the user a message has been received
                            //currently broken as it also notifies the user when you send a message, didn't have time to work pn this further
                            //showNotification(conversations.get(i).getConversationName(), conversations.get(i).getMessage());
                            break;
                        }
                    }
                }
            }
            //sort the conversations list by the date objects, sorting it by date
            Collections.sort(conversations, (obj1, obj2) -> obj2.getDateObject().compareTo(obj1.getDateObject()));
            //notify the adapters data has changed so it updates the recycleview
            conversationsAdapter.notifyDataSetChanged();
            //scroll the list of conversations to the first position which is the latest message
            binding.conversationRecyclerView.smoothScrollToPosition(0);
            //make the recycle view visible
            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
            //make the loading bar invisible
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    //signs the user out
    private void signOut() {
        //tell the user they're are signing out
        showToast("Signing out...");
        //clear the preference manager to prevent them from been
        //able to auto log back in as the app no longer will have their details
        preferenceManager.clear();
        //go back to sign in screen
        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
    }


    //toggles the view of the options menu
    private void toggleAdditionalOptions() {
        //if the layout with a list of users is visible make it invisible
        if (binding.unContactedUsersLayout.getVisibility() == View.VISIBLE) {
            toggleUnContactedUsers();
        }

        //if the account info layout is visible make it invisible
        if (binding.AccountInfoLayout.getVisibility() == View.VISIBLE) {
            toggleAccountInfo();
        }
        //if the map fragment layout is visible make it invisible
        else if (binding.mapServiceFragmentLayout.getVisibility() == View.VISIBLE) {
            toggleMapView();
        }
        //if the options layout is visible make it invisible
        else if (binding.additionalOptionsLayout.getVisibility() == View.VISIBLE) {
            binding.additionalOptionsLayout.setVisibility(View.GONE);
            //make the new chat and help buttons reappear
            binding.addNewChat.setVisibility(View.VISIBLE);
            binding.helpButton.setVisibility(View.VISIBLE);
        }
        //otherwise make the new chat and help button invisible
        //and make the options menu visible
        else {
            binding.addNewChat.setVisibility(View.GONE);
            binding.helpButton.setVisibility(View.GONE);
            binding.additionalOptionsLayout.setVisibility(View.VISIBLE);
        }
    }

    //toggles account info layout
    private void toggleAccountInfo() {
        //if the account info is visible, set it to invisible and redisplay options menu
        if (binding.AccountInfoLayout.getVisibility() == View.VISIBLE) {
            binding.AccountInfoLayout.setVisibility(View.GONE);
            toggleAdditionalOptions();
        }
        //otherwise make the account info visible
        else {
            //if the options menu is visible set it to invisible
            if (binding.additionalOptionsLayout.getVisibility() == View.VISIBLE) {
                binding.additionalOptionsLayout.setVisibility(View.GONE);
            }
            binding.AccountInfoLayout.setVisibility(View.VISIBLE);
        }
    }

    //toggles the map service fragment
    private void toggleMapView() {
        //if the map service fragment is visible make it invisible
        if (binding.mapServiceFragmentLayout.getVisibility() == View.VISIBLE) {
            //also check if recycle view is invisible, as it was set to
            //invisible when the map view was set to visible
            if (binding.conversationRecyclerView.getVisibility() == View.GONE) {
                binding.conversationRecyclerView.setVisibility(View.VISIBLE);
            }
            binding.mapServiceFragmentLayout.setVisibility(View.GONE);
            //reshow the options menu
            toggleAdditionalOptions();
        }
        //otherwise make the map service fragment visible
        else {
            //close the options menu
            toggleAdditionalOptions();
            //check if the add chat button is visible and set it and the help button to invisible
            if (binding.addNewChat.getVisibility() == View.VISIBLE) {
                binding.addNewChat.setVisibility(View.GONE);
                binding.helpButton.setVisibility(View.GONE);
            }
            //check if the reycleview is visible and set it to invisible
            if (binding.conversationRecyclerView.getVisibility() == View.VISIBLE) {
                binding.conversationRecyclerView.setVisibility(View.GONE);
            }
            binding.mapServiceFragmentLayout.setVisibility(View.VISIBLE);
        }
    }

    //toggles the list of contactable users layout
    public void toggleUnContactedUsers() {
        //if the list of contactable users layout is invisible and the add new chat button is visible
        //set them to invisible and set the list of contactable users layout to visible
        if (binding.unContactedUsersLayout.getVisibility() == View.GONE && binding.addNewChat.getVisibility() == View.VISIBLE) {
            binding.conversationRecyclerView.setVisibility(View.GONE);
            binding.addNewChat.setVisibility(View.GONE);
            binding.helpButton.setVisibility(View.GONE);
            binding.unContactedUsersLayout.setVisibility(View.VISIBLE);
        }
        //otherwise make the list of contactable users layout invisible
        //and make the recycle view visible
        else {
            binding.unContactedUsersLayout.setVisibility(View.GONE);
            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
            //if the options layout is invisible also make the new chat and help buttons visible
            if (binding.additionalOptionsLayout.getVisibility() == View.GONE) {
                binding.addNewChat.setVisibility(View.VISIBLE);
                binding.helpButton.setVisibility(View.VISIBLE);
            }

        }


    }

    //on click method for the on click listeners in the recycleview viewholders
    //takes the user to the message user activity to message the user in the list of
    //conversations clicked on
    @Override
    public void onConversationClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), MessageUserActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }

    //update the document with the user's details, setting their availability to 0 or offline when exiting the app
    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(Constants.KEY_AVAILABILITY, 0);
    }

    //update the document with the user's details, setting their availability to 1 or online when opening the app
    @Override
    protected void onResume() {
        super.onResume();
        documentReference.update(Constants.KEY_AVAILABILITY, 1);
    }

    //provides help messages, informing the user as to what they can do
    private void help() {
        showToast("Click on the + button to see a list of contacts");
        showToast("Click on a contact to message them.");
        showToast("To see account info, sign out or use the map service");
        showToast("Click on the triple line options button in the top right.");
    }

    //listens for new messages added to the database conversation contents (messages) json
    private void listenNewMessages() {
        //add listener to listen for changes in the conversations contents (messages) json
        //where the receiver id is equal to the current user's id
        database.collection(Constants.KEY_COLLECTION_CONVERSATION_CONTENTS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(messageEventListener);
    }

    //message event listener used for the snapshot listener above
    private final EventListener<QuerySnapshot> messageEventListener = (value, error) -> {
        //do nothing if there is an error
        if (error != null) {
            return;
        }
        //otherwise continue
        if (value != null) {
            //for change in the conversation contents (messages) json that includes messages sent or received by the current user
            //get the message contents of each set of values in the json
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                //check that the type of document change is added only messages added by the app should be listed
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    MessageContents messageContents = new MessageContents();
                    messageContents.setSenderId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                    messageContents.setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));
                    database.collection(Constants.KEY_COLLECTION_USERS)
                            .get()
                            .addOnCompleteListener(getUserTask -> {
                                //if successful and nothing is null or empty, get a snapshot or current version of the JSON document for the user matched
                                if (getUserTask.isSuccessful() && getUserTask.getResult() != null && getUserTask.getResult().getDocuments().size() > 0) {
                                    //loop through the json document to get a user with an id that matches sender id, the user id is the document id
                                    for (int i = 0; i < getUserTask.getResult().getDocuments().size(); i++) {
                                        if (getUserTask.getResult().getDocuments().get(i).getId().equals(messageContents.getSenderId())) {
                                            //set the conversation name to the name of the user
                                            messageContents.setConversationName(getUserTask.getResult().getDocuments().get(i).getString(Constants.KEY_NAME));
                                            //display a notification using the user's name as the title and the message they send
                                            //note on startup can just show a random message as I hadn't worked out how to get it to to only listen for new messages
                                            showNotification(messageContents.getConversationName(), messageContents.getMessage());
                                            break;
                                        }
                                    }
                                }
                            })
                            //if there's an issue show an exception
                            .addOnFailureListener(exception -> {
                                showToast(exception.getMessage());
                            });
                }
            }
        }
    };


}