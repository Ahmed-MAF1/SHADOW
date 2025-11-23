package com.example.myapplication.app.shop;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.app.coop.ChatMessage;
import com.example.myapplication.app.core.AppUser;
import com.example.myapplication.app.core.BaseActivity;
import com.example.myapplication.app.core.CoopTimerManager;
import com.example.myapplication.app.core.FirestoreHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class InviteActivity extends BaseActivity {

    private LinearLayout layoutLobby;
    private MaterialButton btnCreateRoom, btnJoinRoom;
    private EditText etRoomCode;

    private ConstraintLayout layoutRoom;
    private TextView tvRoomCodeDisplay;
    private MaterialButton btnLeaveRoom, btnCoopTimer;
    private View btnCopyCode;
    private LinearLayout participantsContainer;

    private RecyclerView recyclerChat;
    private EditText etChatMessage;
    private View btnSendChat;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> chatMessages = new ArrayList<>();

    private FirebaseFirestore db;
    private SharedPreferences prefs;

    private String currentUsername = "Unknown";
    private String currentRoomId   = null;

    private ListenerRegistration roomListener;
    private ListenerRegistration chatListener;

    private CoopTimerManager coopTimerManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        db    = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("coop_prefs", MODE_PRIVATE);

        if (FirestoreHelper.getCurrentUser() == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        initBaseNavigation();

        setupCoopListeners();
        setupChatRecycler();
        fetchCurrentUserData();

        // Timer manager (Co-op)
        coopTimerManager = new CoopTimerManager(
                this,
                prefs,
                btnCoopTimer,
                new CoopTimerManager.Callback() {
                    @Override
                    public void updateStatus(String newStatus) {
                        InviteActivity.this.updateMyStatus(newStatus);
                    }

                    @Override
                    public void onSessionFinished(long initialTimeInMillis) {
                        InviteActivity.this.onCoopSessionFinished(initialTimeInMillis);
                    }
                }
        );
        coopTimerManager.attachButtonListener();

        checkExistingRoom();
        coopTimerManager.checkTimerState();
    }

    @Override
    protected int getCurrentNavItemId() {
        return R.id.btnInvite;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null) roomListener.remove();
        if (chatListener != null) chatListener.remove();
        if (coopTimerManager != null) coopTimerManager.cleanup();
    }

    private void bindViews() {
        layoutLobby   = findViewById(R.id.layoutLobby);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        btnJoinRoom   = findViewById(R.id.btnJoinRoom);
        etRoomCode    = findViewById(R.id.etRoomCode);

        layoutRoom      = findViewById(R.id.layoutRoom);
        tvRoomCodeDisplay = findViewById(R.id.tvRoomCodeDisplay);
        btnCopyCode       = findViewById(R.id.btnCopyCode);
        btnLeaveRoom      = findViewById(R.id.btnLeaveRoom);
        participantsContainer = findViewById(R.id.participantsContainer);

        recyclerChat  = findViewById(R.id.recyclerChat);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendChat   = findViewById(R.id.btnSendChat);
        btnCoopTimer  = findViewById(R.id.btnCoopTimer);
    }

    // -------------------- Co-op Room --------------------

    private void setupCoopListeners() {
        btnCreateRoom.setOnClickListener(v -> createRoom());
        btnJoinRoom.setOnClickListener(v -> joinRoom());
        btnLeaveRoom.setOnClickListener(v -> leaveRoom());
        btnCopyCode.setOnClickListener(v -> copyRoomCode());
        btnSendChat.setOnClickListener(v -> sendMessage());
    }

    private void createRoom() {
        String code = generateRoomCode();
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("rooms").document(code)
                .set(roomData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Room created: " + code, Toast.LENGTH_SHORT).show();
                    addMeAsParticipant(code);
                    saveRoomId(code);
                    enterRoomUI(code);
                });
    }

    private void joinRoom() {
        String code = etRoomCode.getText().toString().trim();
        if (code.length() != 6) {
            etRoomCode.setError("Enter 6 digits");
            return;
        }

        db.collection("rooms").document(code)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        addMeAsParticipant(code);
                        saveRoomId(code);
                        enterRoomUI(code);
                    } else {
                        Toast.makeText(this, "Room not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addMeAsParticipant(String roomId) {
        String uid = FirestoreHelper.getCurrentUid();
        if (uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("name", currentUsername);
        data.put("status", "Active");

        db.collection("rooms").document(roomId)
                .collection("participants")
                .document(uid)
                .set(data);
    }

    private void enterRoomUI(String roomId) {
        currentRoomId = roomId;
        tvRoomCodeDisplay.setText(roomId);
        layoutLobby.setVisibility(View.GONE);
        layoutRoom.setVisibility(View.VISIBLE);

        listenToRoomParticipants();
        listenToChat();
    }

    private void leaveRoom() {
        String uid = FirestoreHelper.getCurrentUid();

        if (currentRoomId != null && uid != null) {
            db.collection("rooms").document(currentRoomId)
                    .collection("participants")
                    .document(uid)
                    .delete();
        }

        if (roomListener != null) roomListener.remove();
        if (chatListener != null) chatListener.remove();

        if (coopTimerManager != null) coopTimerManager.onLeaveRoom();
        updateMyStatus("Active");

        clearRoomId();
        currentRoomId = null;

        layoutRoom.setVisibility(View.GONE);
        layoutLobby.setVisibility(View.VISIBLE);
    }

    private void copyRoomCode() {
        String code = tvRoomCodeDisplay.getText().toString();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("room_code", code));
        Toast.makeText(this, "Copied: " + code, Toast.LENGTH_SHORT).show();
    }

    private void listenToRoomParticipants() {
        if (currentRoomId == null) return;
        if (roomListener != null) roomListener.remove();

        roomListener = db.collection("rooms")
                .document(currentRoomId)
                .collection("participants")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    participantsContainer.removeAllViews();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String name   = doc.getString("name");
                        String status = doc.getString("status");
                        addParticipantView(name, status);
                    }
                });
    }

    private void addParticipantView(String name, String status) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(android.view.Gravity.CENTER);
        item.setPadding(16, 0, 16, 0);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("👤");
        tvIcon.setTextSize(24);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(getResources().getColor(R.color.white));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(status);
        tvStatus.setTextSize(10);

        if (status != null && status.contains("Focusing")) {
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        item.addView(tvIcon);
        item.addView(tvName);
        item.addView(tvStatus);
        participantsContainer.addView(item);
    }

    private void updateMyStatus(String newStatus) {
        String uid = FirestoreHelper.getCurrentUid();
        if (currentRoomId != null && uid != null) {
            db.collection("rooms").document(currentRoomId)
                    .collection("participants").document(uid)
                    .update("status", newStatus);
        }
    }

    private void checkExistingRoom() {
        String savedRoom = prefs.getString("current_room_id", null);
        if (savedRoom != null) {
            enterRoomUI(savedRoom);
        } else {
            layoutLobby.setVisibility(View.VISIBLE);
            layoutRoom.setVisibility(View.GONE);
        }
    }

    private void saveRoomId(String roomId) {
        prefs.edit().putString("current_room_id", roomId).apply();
    }

    private void clearRoomId() {
        prefs.edit().remove("current_room_id").apply();
    }

    // -------------------- Co-op Timer نهاية المنطق في Callback --------------------

    private void onCoopSessionFinished(long initialTimeInMillis) {
        if (initialTimeInMillis <= 0) return;

        long minutes = initialTimeInMillis / 60_000;
        long coinsEarned = minutes / 5;
        if (coinsEarned < 1) coinsEarned = 1;

        FirestoreHelper.incrementUserField("coins", coinsEarned, new FirestoreHelper.SimpleCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { }
        });

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        FirestoreHelper.updateUserField("daily_date", todayDate, new FirestoreHelper.SimpleCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { }
        });

        FirestoreHelper.incrementUserField("daily_time", initialTimeInMillis, new FirestoreHelper.SimpleCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { }
        });

        updateMyStatus("Active");
    }

    // -------------------- Chat --------------------

    private void setupChatRecycler() {
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(chatAdapter);
    }

    private void sendMessage() {
        String msgText = etChatMessage.getText().toString().trim();
        if (msgText.isEmpty() || currentRoomId == null) return;

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("text", msgText);
        msgData.put("sender", currentUsername);
        msgData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("rooms")
                .document(currentRoomId)
                .collection("messages")
                .add(msgData);

        etChatMessage.setText("");
    }

    private void listenToChat() {
        if (currentRoomId == null) return;
        if (chatListener != null) chatListener.remove();

        chatListener = db.collection("rooms")
                .document(currentRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        chatMessages.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            chatMessages.add(ChatMessage.fromSnapshot(doc));
                        }
                        chatAdapter.notifyDataSetChanged();
                        if (!chatMessages.isEmpty())
                            recyclerChat.smoothScrollToPosition(chatMessages.size() - 1);
                    }
                });
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        List<ChatMessage> messages;
        ChatAdapter(List<ChatMessage> m) { messages = m; }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat, parent, false);
            return new ChatViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            holder.tvSender.setText(msg.sender);
            holder.tvMessage.setText(msg.text);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView tvSender, tvMessage;
            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSender  = itemView.findViewById(R.id.tvSender);
                tvMessage = itemView.findViewById(R.id.tvMessage);
            }
        }
    }

    // -------------------- User info --------------------

    private void fetchCurrentUserData() {
        FirestoreHelper.loadCurrentUser(new FirestoreHelper.UserDocCallback() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                AppUser user = AppUser.fromSnapshot(snapshot);
                if (user.username != null) {
                    currentUsername = user.username;
                }
            }

            @Override
            public void onError(String message) { }
        });
    }

    private String generateRoomCode() {
        Random r = new Random();
        int num = 100000 + r.nextInt(900000);
        return String.valueOf(num);
    }
}
