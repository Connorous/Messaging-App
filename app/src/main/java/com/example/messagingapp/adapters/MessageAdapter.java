package com.example.messagingapp.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.databinding.ItemContainerReceivedMessageBinding;
import com.example.messagingapp.databinding.ItemContainerSentMessageBinding;
import com.example.messagingapp.models.MessageContents;

import java.util.List;

//message adapter class, takes a list of message to display in a view holder
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    //list of messages to be displayed
    private final List<MessageContents> conversationMessages;
    //bitmap for the receiver's profile image
    private  final Bitmap receiverProfileImage;
    //id of the sender
    private final String userId;
    //simple types used for determining message type, one for sent and 2 for received
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    //constructor, takes a list of messages, a bitmap for the receiver users and the id of the sender
    public MessageAdapter(List<MessageContents> messages, Bitmap receiverProfileImage, String userId) {
        this.conversationMessages = messages;
        this.receiverProfileImage = receiverProfileImage;
        this.userId = userId;
    }

    //creates a user view holder with the binding references to the views of a ItemContainer Sent or Received Message xml
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //if view type is sent, creates a sent message item container and view holder
        if(viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        //else creates a received message item container and view holder
        else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }

    //when the view holder is added, add message object to it containing a message's details
    //checks type of view holder before providing message object
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(conversationMessages.get(position));
        }
        else {
            //the receiver view holder is also passed
            ((ReceivedMessageViewHolder) holder).setData(conversationMessages.get(position), receiverProfileImage);
        }
    }

    //gets the size of list of message objects
    @Override
    public int getItemCount() {
        return conversationMessages.size();
    }


    //provides the view type based on senderId
    public int getItemViewType(int position) {
        //if the sender id matches the message sender id then the message was sent by the user
        if(conversationMessages.get(position).getSenderId().equals(userId)) {
            return VIEW_TYPE_SENT;
        }
        //otherwise the message was not sent by the user, so it is a received message
        else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    //sent message view holder class, defines the view holder for sent messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        //binding for ItemContainerSentMessage xml
        private final ItemContainerSentMessageBinding binding;

        //constructor for sent message view holder
        public SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            //pass the item container to the recycle view super class
            super(itemContainerSentMessageBinding.getRoot());
            //bind itemContainerSentMessage xml views to easily get references of them
            binding = itemContainerSentMessageBinding;
        }

        //sets the message details of the itemContainerSentMessage xml to that of the message object provided
        void setData(MessageContents messageContents) {
            binding.textMessage.setText(messageContents.getMessage());
            binding.textDateTime.setText(messageContents.getDateTime());
        }
    }

    //received message view holder class, defines the view holder for received messages
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        //binding for ItemContainerReceivedMessage xml
        private final ItemContainerReceivedMessageBinding binding;

        //constructor for sent message view holder
        public ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            //pass the item container to the recycle view super class
            super(itemContainerReceivedMessageBinding.getRoot());
            //bind itemContainerReceivedMessage xml views to easily get references of them
            binding = itemContainerReceivedMessageBinding;
        }

        //sets the message details of the itemContainerSentMessage xml to that of the message object provided
        void setData(MessageContents messageContents, Bitmap receiverProfileImage) {
            binding.textMessage.setText(messageContents.getMessage());
            binding.textDateTime.setText(messageContents.getDateTime());
            //
            binding.imageProfile.setImageBitmap(receiverProfileImage);
        }
    }
}
