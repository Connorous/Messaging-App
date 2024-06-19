package com.example.messagingapp.adapters;

import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messagingapp.databinding.ItemContainerUserBinding;
import com.example.messagingapp.listerners.UserListener;
import com.example.messagingapp.models.User;

import java.util.List;

//users adapter class, takes a list of users to display in a view holder
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder>{

    //list of users to be displayed
    private final List<User> users;
    //on click listener on user displayed
    private UserListener userListener;

    //constructor, takes a list of users and a user listener class
    public UsersAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }


    //creates a user view holder with the binding references to the views of a ItemContainerUser xml
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(itemContainerUserBinding);
    }

    //when the view holder is added, add user object to it containing the user's details
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    //gets the size of the users list
    @Override
    public int getItemCount() {
        return users.size();
    }

    //user view holder class, defines the view holder for listed users
    class UserViewHolder extends RecyclerView.ViewHolder {

        //binding for ItemContainerUser xml
        ItemContainerUserBinding binding;

        //constructor for user view holder
        public UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            //pass the item container to the recycle view super class
            super(itemContainerUserBinding.getRoot());
            //bind ItemContainerUser xml views to easily get references of them
            binding = itemContainerUserBinding;
        }

        //sets the user details of the ItemContainerUser xml to that of the user provided
        private void setUserData(User user) {
            binding.textName.setText(user.getName());
            //binding.textEmail.setText(user.getEmail());
            binding.imageProfile.setImageBitmap(getUserImage(user.getImage()));
            //add a onclick listener to the ItemContainerUser view to detect if it has been clicked
            //provide a user object used of the user been used for the view holder
            binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(user));
        }
    }

    //decodes a string of an image into a bitmap
    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
