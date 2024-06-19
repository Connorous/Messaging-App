package com.example.messagingapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.databinding.ItemContainerRecentConversationBinding;
import com.example.messagingapp.listerners.ConversationListener;
import com.example.messagingapp.models.MessageContents;
import com.example.messagingapp.models.User;

import java.util.List;

//recent conversations adapter class, takes a list of recent messages to display in a view holder
public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversationViewHolder>{

    //list of messages to be displayed
    private final List<MessageContents> conversationMessages;
    //on click listener on message displayed
    private final ConversationListener conversationListener;

    //constructor, takes a list of recent messages and a listener class
    public RecentConversationsAdapter(List<MessageContents> conversationMessages, ConversationListener conversationListener) {
        this.conversationMessages = conversationMessages;
        this.conversationListener = conversationListener;
    }

    //creates a conversation view holder with the binding references to the views of a ItemContainerUser xml
    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(ItemContainerRecentConversationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    //when the view holder is added, add conversation message object to it containing the user's details
    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.setData(conversationMessages.get(position));
    }

    //gets the size of the recent conversations messages list
    @Override
    public int getItemCount() {
        return conversationMessages.size();
    }

    //conversation message view holder class, defines the view holder for listed conversation messages
    class ConversationViewHolder extends RecyclerView.ViewHolder {
        //binding for ItemContainerRecentConversation xml
        ItemContainerRecentConversationBinding binding;

        //constructor for recent conversation message view holder
        ConversationViewHolder(ItemContainerRecentConversationBinding itemContainerRecentConversationBinding) {
            //pass the item container to the recycle view super class
            super(itemContainerRecentConversationBinding.getRoot());
            //bind ItemContainerRecentConversation xml views to easily get references of them
            binding = itemContainerRecentConversationBinding;
        }

        //sets the message details of the ItemContainerRecentConversation xml to that of the recent message object provided
        void setData(MessageContents messageContents) {
            binding.imageProfile.setImageBitmap(getConversationImage(messageContents.getConversationImage()));
            binding.textName.setText(messageContents.getConversationName());
            binding.textRecentMessage.setText(messageContents.getMessage());
            //add a onclick listener to the ItemContainerRecentConversation view to detect if it has been clicked
            binding.getRoot().setOnClickListener(v -> {
                //if is clicked provide a user object with the details of the receiver user of the recent conversation
                User user = new User();
                user.setId(messageContents.getConversationId());
                user.setName(messageContents.getConversationName());
                user.setImage(messageContents.getConversationImage());
                conversationListener.onConversationClicked(user);
            });
        }
    }

    //decodes a string of an image into a bitmap
    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
