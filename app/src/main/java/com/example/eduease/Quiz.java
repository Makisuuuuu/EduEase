package com.example.eduease;

import com.google.firebase.Timestamp;
import java.util.List;

public class Quiz {
    private String id;
    private String creatorId;
    private Timestamp timestamp;
    private String title;
    private String description;
    private List<Question> questions;

    // Default constructor (required for Firestore to deserialize data)
    public Quiz() {}

    // Constructor with parameters
    public Quiz(String id, String creatorId, Timestamp timestamp, String title, String description, List<Question> questions) {
        this.id = id;
        this.creatorId = creatorId;
        this.timestamp = timestamp;
        this.title = title;
        this.description = description;
        this.questions = questions;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}
