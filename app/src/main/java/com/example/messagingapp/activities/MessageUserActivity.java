package com.example.messagingapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.example.messagingapp.adapters.MessageAdapter;
import com.example.messagingapp.databinding.ActivityMessageUserBinding;
import com.example.messagingapp.models.MessageContents;
import com.example.messagingapp.models.User;
import com.example.messagingapp.utilities.Constants;
import com.example.messagingapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

//message user activity class
public class MessageUserActivity extends AppCompatActivity {

    //binding for views
    private ActivityMessageUserBinding binding;
    //user object for the other users details
    private User receiveUser;
    //list of messageContents objects
    private List<MessageContents> conversationMessages;
    //message adapter used to display messages in view holders in recycler view
    private MessageAdapter messageAdapter;
    // preference manager which contains saved preferences
    private PreferenceManager preferenceManager;
    //database
    private FirebaseFirestore database;
    //reference to firebase document json for the current user
    private DocumentReference documentReference;
    //reference to firebase document for chat
    private DocumentReference chatDocumentReference;
    //id used for a given conversation, used to get reference a certain json document for a chat
    private String conversationId = null;
    //used for checking database listener connection is working
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //binding uses views from message user xml
        binding = ActivityMessageUserBinding.inflate(getLayoutInflater());
        //set on click listeners for activity
        setListeners();
        //root of binding is the view
        setContentView(binding.getRoot());
        //get the details of the user the current user wants to contact
        loadReceiverDetails();
        //initialise everything
        init();
        //set database listener to check for changes in messages
        listenMessages();
    }

    //initialises everything
    private void init() {
        //create preference manager using app context to store data across activities
        preferenceManager = new PreferenceManager(getApplicationContext());
        //array list of the messages
        conversationMessages = new ArrayList<>();
        //adapter used by recycleview, uses list of messages to display message, other users image to display their image and the current users id
        messageAdapter = new MessageAdapter(conversationMessages, getBitmapFromEncodedString(receiveUser.getImage()), preferenceManager.getString(Constants.KEY_USER_ID));
        //attach adapter to recycleview
        binding.messageRecycleView.setAdapter(messageAdapter);
        //initialise database
        database = FirebaseFirestore.getInstance();
        //set document reference to the database users json belonging to the user
        documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
    }

    //adds a message to the firebase database based on user input
    private void sendMessage() {
        //if the current user has not typed a message, inform them to type a message and do not proceed further
        if (binding.inputMessage.getText().toString().equals("")) {
            showToast("Please type a message");
            return;
        }
        //hashmap for storing message information
        HashMap<String, Object> message = new HashMap<>();
        //put user id as sender id
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        //put receiver id as the receiver user's id
        message.put(Constants.KEY_RECEIVER_ID, receiveUser.getId());
        //put the message in the text input field as the message
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        //put a new date object for the date of the message
        message.put(Constants.KEY_TIMESTAMP, new Date());
        //add the message hashmap to the database collection of message called conversation contents
        database.collection(Constants.KEY_COLLECTION_CONVERSATION_CONTENTS).add(message);
        //if the id for the conversation is assigned, make sure to call update to add new message to database
        if (conversationId != null) {
            updateConversation(binding.inputMessage.getText().toString());
        }
        //otherwise this is a new conversation, so a conversation json will need to be added to the database for tracking this conversation
        else {
            //create a hashmap for the conversation
            HashMap<String, Object> conversation = new HashMap<>();
            //add sender user details to hashmap, current user is sender
            conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            //add receiver user details to hashmap, other user is receiver
            conversation.put(Constants.KEY_RECEIVER_ID, receiveUser.getId());
            conversation.put(Constants.KEY_RECEIVER_NAME, receiveUser.getName());
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receiveUser.getImage());
            //add message and date to hashmap
            conversation.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            //add hashmap to the conversation json in the database
            addConversation(conversation);
        }
        //clear input field after sending message
        binding.inputMessage.setText("");
    }

    //listens for new messages added to the database conversation contents (messages) json
    private void listenMessages() {
        //add listener to listen for changes in the conversations contents (messages) json
        //where sender is the current user and receiver is the other user
        database.collection(Constants.KEY_COLLECTION_CONVERSATION_CONTENTS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiveUser.getId())
                .addSnapshotListener(eventListener);
        //add listener to listen for changes in the conversations contents (messages) json
        //where sender is the receiver user and receiver is the current user
        database.collection(Constants.KEY_COLLECTION_CONVERSATION_CONTENTS)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiveUser.getId())
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    //event listener used for the snapshot listeners above
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        //do nothing if there is an error
        if (error != null) {
            return;
        }
        //otherwise continue
        if (value != null) {
            //initialise count
            int count = 0;
            //for change in the conversation contents (messages) json that includes messages sent or received by the current user
            //get the message contents of each set of values in the json
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                //check that the type of document change is added only messages added by the app should be listed
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    //create a messageContents object to store the the json field's message information
                    MessageContents messageContents = new MessageContents();
                    //add details from document change to messageContents object
                    messageContents.setSenderId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                    messageContents.setReceiverId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                    messageContents.setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));
                    messageContents.setDateTime(getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP)));
                    messageContents.setDateObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    //add messageContents to list of conversation messages
                    conversationMessages.add(messageContents);
                }
            }
            //count the number of messages
            count = conversationMessages.size();
            //sort list of conversation messages by the date of the date objects in messageContents objects
            conversationMessages.sort(Comparator.comparing(obj -> obj.getDateObject()));
            //if there are no message this is a new count, notify adapter that the dataset has changed so previous message don't appear
            if (count == 0) {
                messageAdapter.notifyDataSetChanged();
            }
            //otherwise notify the adapter of the a certain number of messages have been added
            else {
                //the number of messages is the size of the conversation message list, starting at position 0
                messageAdapter.notifyItemRangeInserted(count, count);
                //scroll the recycleview down to the last message in the list of conversation messages
                binding.messageRecycleView.smoothScrollToPosition(conversationMessages.size() - 1);
            }
            //set the reycleview to be visible
            binding.messageRecycleView.setVisibility(View.VISIBLE);
        }
        //make the loading bar disappear as the messages have been loaded
        binding.progressBar.setVisibility(View.GONE);
        //if conversation id is null check if it exists
        if (conversationId == null) {
            checkForConversation();
        }
    };

    //checks if the other user been messaged is available / online
    private void listenAvailabilityListener() {
        //add snapshot or change listener to users for the json field of the other user been messaged
        database.collection(Constants.KEY_COLLECTION_USERS).document(receiveUser.getId())
                .addSnapshotListener(MessageUserActivity.this, (value, error) -> {
                    //don't do anything if there is an error
                    if (error != null) {
                        return;
                    }
                    //otherwise get the availability of the other user been messaged
                    if (value != null) {
                        //also check that the availability field is not null just in case
                        if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                            //get the number for the availability
                            int availability = value.getLong(Constants.KEY_AVAILABILITY).intValue();
                            //if the other user's availability is 1 then they are online
                            if (availability == 1) {
                                isReceiverAvailable = true;
                            }
                            //otherwise they're not online
                            else {
                                isReceiverAvailable = false;
                            }
                        }
                    }
                    //if the other user is online, show the online textview
                    if (isReceiverAvailable) {
                        binding.textAvailability.setVisibility(View.VISIBLE);
                    }
                    //otherwise hide it
                    else {
                        binding.textAvailability.setVisibility(View.GONE);
                    }
                });
    }

    //use the encoded string of the other's users profile image to create a bitmap
    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    //gets the other user to be message via an the user object passed from the intent
    private void loadReceiverDetails() {
        receiveUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiveUser.getName());
    }

    //sets on click listeners for views
    private void setListeners() {
        //sets the back button to go back to the main activity
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        //sets the send button to send the message typed
        binding.layoutSend.setOnClickListener(v -> sendMessage());
    }

    //returns a readable string of the date from a date object
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh : mm a", Locale.getDefault()).format(date);
    }

    //adds a conversation to the conversations json in the firebase database
    private void addConversation(HashMap<String, Object> conversation) {
        //adds the conversation hashmap object passed in to the conversation collection json in the database
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(chatDocumentReference -> conversationId = chatDocumentReference.getId());
    }

    //updates the conversations json in the firebase database for the current conversation with the latest message
    private void updateConversation(String message) {
        //get the conversations collection json document for the current conversation using the conversation id
        chatDocumentReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        //update the conversation document json with the latest message
        chatDocumentReference.update(Constants.KEY_LAST_MESSAGE, message, Constants.KEY_TIMESTAMP, new Date());
    }

    //
    private void checkForConversation() {
        if (conversationMessages.size() != 0) {
            //checks for conversation using the current user's id and the other receiver user's id
            checkForConversationRemotely(preferenceManager.getString(Constants.KEY_USER_ID), receiveUser.getId());
            //double checks this by swapping the ids around in case the receiver user is the sender
            checkForConversationRemotely(receiveUser.getId(), preferenceManager.getString(Constants.KEY_USER_ID));
        }
    }

    //adds a one time listener for the conversations collection json in the database to look for the current conversation
    private void checkForConversationRemotely(String senderId, String receiverId) {
        //gets a conversation from the database conversations collection
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                //where the sender id and receiver id matches
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    //event listener used for checking conversations above
    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        //check the listener was successful
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
          //get a snapshot of the json document for this conversation
          DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
          //set conversation id to the id of the document
          conversationId = documentSnapshot.getId();
        }
    };

    //on reopening the app updates the availability to 1
    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityListener();
        documentReference.update(Constants.KEY_AVAILABILITY, 1);
    }

    //on closing the app updates the availability to 0
    @Override
    protected void onPause() {
        super.onPause();
        documentReference.update(Constants.KEY_AVAILABILITY, 0);
    }

    //shows an inputted message
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}