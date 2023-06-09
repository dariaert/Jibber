package com.example.jibber.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.contentcapture.DataRemovalRequest;

import com.example.jibber.R;
import com.example.jibber.adapters.ChatAdapter;
import com.example.jibber.databinding.ActivityChatBinding;
import com.example.jibber.models.ChatMessage;
import com.example.jibber.models.User;
import com.example.jibber.utilities.Constans;
import com.example.jibber.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.checkerframework.checker.units.qual.C;


import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.prefs.PreferenceChangeEvent;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receivedUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private String conversionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessage();

    }

    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                //getBitmapFromEncodedString(receivedUser.image),
                null,
                preferenceManager.getString(Constans.KEY_USER_ID)
        );
        binding.chatRecycleView.setAdapter(chatAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private void sendMessage(){ // отправить сообщение
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constans.KEY_SENDER_ID, preferenceManager.getString(Constans.KEY_USER_ID));
        message.put(Constans.KEY_RECEIVER_ID, receivedUser.id);
        message.put(Constans.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constans.KEY_TIMESTAMP, new Date());
        db.collection(Constans.KEY_COLLECTION_CHAT).add(message);
        if(conversionId != null){
            updateConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constans.KEY_SENDER_ID, preferenceManager.getString(Constans.KEY_USER_ID));
            conversion.put(Constans.KEY_SENDER_NAME, preferenceManager.getString(Constans.KEY_NAME));
            conversion.put(Constans.KEY_SENDER_SURNAME, preferenceManager.getString(Constans.KEY_SURNAME));
            conversion.put(Constans.KEY_SENDER_IMAGE, preferenceManager.getString(Constans.KEY_IMAGE));
            conversion.put(Constans.KEY_RECEIVER_ID, receivedUser.id);
            conversion.put(Constans.KEY_RECEIVER_NAME, receivedUser.name);
            conversion.put(Constans.KEY_RECEIVER_SURNAME, receivedUser.surname);
            conversion.put(Constans.KEY_IMAGE, receivedUser.image);
            conversion.put(Constans.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Constans.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        binding.inputMessage.setText(null);
    }

    private void listenMessage(){
        db.collection(Constans.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constans.KEY_SENDER_ID, preferenceManager.getString(Constans.KEY_USER_ID))
                .whereEqualTo(Constans.KEY_RECEIVER_ID, receivedUser.id)
                .addSnapshotListener(eventListener);
        db.collection(Constans.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constans.KEY_SENDER_ID, receivedUser.id)
                .whereEqualTo(Constans.KEY_RECEIVER_ID, preferenceManager.getString(Constans.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final com.google.firebase.firestore.EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null){
            return ;
        }
        if (value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constans.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constans.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constans.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constans.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constans.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }

            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0){
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecycleView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecycleView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionId == null){
            checkForConversion();
        }
    };



    private Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadReceiverDetails(){
        receivedUser = (User) getIntent().getSerializableExtra(Constans.KEY_USER);
        binding.textName.setText(receivedUser.name);
        binding.textSurname.setText(receivedUser.surname);
        // Отобразить фото собеседника в imageProfile
        if (receivedUser.image != null) {
            Bitmap bitmap = getBitmapFromEncodedString(receivedUser.image);
            binding.imageProfile.setImageBitmap(bitmap);
        }
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
    }

    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String, Object> conversion){
        db.collection(Constans.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message){
        DocumentReference documentReference =
                db.collection(Constans.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constans.KEY_LAST_MESSAGE, message,
                Constans.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversion(){
        if(chatMessages.size() != 0){
            checkForConversionRemotely(
                    preferenceManager.getString(Constans.KEY_USER_ID),
                    receivedUser.id
            );
            checkForConversionRemotely(
                    receivedUser.id,
                    preferenceManager.getString(Constans.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        db.collection(Constans.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constans.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constans.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };
}