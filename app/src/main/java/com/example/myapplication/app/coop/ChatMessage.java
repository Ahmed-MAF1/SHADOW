package com.example.myapplication.app.coop;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/** Model class for chat messages */
public class ChatMessage {

    @Nullable public String id;
    @Nullable public String sender;
    @Nullable public String text;
    @Nullable public Date timestamp;

    public ChatMessage() { }

    public ChatMessage(String sender, String text) {
        this.sender = sender;
        this.text = text;
    }

    public static ChatMessage fromSnapshot(DocumentSnapshot doc) {
        ChatMessage m = new ChatMessage();
        m.id = doc.getId();
        m.sender = doc.getString("sender");
        m.text = doc.getString("text");

        Timestamp ts = doc.getTimestamp("timestamp");
        if (ts != null) m.timestamp = ts.toDate();

        return m;
    }
}
