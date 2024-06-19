package com.example.messagingapp.listerners;

import com.example.messagingapp.models.User;

public interface ConversationListener {
    void onConversationClicked(User user);
}
