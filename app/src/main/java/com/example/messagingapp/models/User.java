package com.example.messagingapp.models;

import java.io.Serializable;

//user class, used to store user details
public class User implements Serializable {

    private String name, image, email, id;

    //getters for variables

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    //setters for variables

    public void setName(String name) {
        this.name = name;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(String id) {
        this.id = id;
    }
}
