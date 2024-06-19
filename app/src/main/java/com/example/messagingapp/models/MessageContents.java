package com.example.messagingapp.models;

import java.util.Date;

//message contents class, used for storing the contents and details of a message
public class MessageContents {

    private String senderId, receiverId, message, dateTime;
    private Date dateObject;
    private String conversationId, conversationName, conversationImage;

    //getters for variables

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessage() {
        return message;
    }

    public String getDateTime() {
        return dateTime;
    }

    public Date getDateObject() {
        return dateObject;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getConversationName() {
        return conversationName;
    }

    public String getConversationImage() {
        return conversationImage;
    }

    //setters for variables

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setDateObject(Date dateObject) {
        this.dateObject = dateObject;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public void setConversationImage(String conversationImage) {
        this.conversationImage = conversationImage;
    }
}
