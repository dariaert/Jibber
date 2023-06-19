package com.example.jibber.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.jibber.R;
import com.example.jibber.adapters.RecentConversationsAdapter;
import com.example.jibber.databinding.ActivityMainBinding;
import com.example.jibber.listeners.ConversionListener;
import com.example.jibber.models.ChatMessage;
import com.example.jibber.models.User;
import com.example.jibber.utilities.Constans;
import com.example.jibber.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConversionListener {

    ActivityMainBinding binding;
    PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        //loadUserDetails();
        Dif();
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.home:
                    // Проигнорировать действие, так как это уже текущая активность
                    return true;

                case R.id.new_group:
                    Intent newGroupIntent = new Intent(this, NewGroupActivity.class);
                    startActivity(newGroupIntent);
                    finish();
                    break;


                case R.id.contacts:
                    Intent contactsIntent = new Intent(this, ContactsActivity.class);
                    startActivity(contactsIntent);
                    finish();
                    break;

                case R.id.exit:
                    signOut();
                    //break;
            }
            return  true;
        });
        getToken();
        setListeners();
        listenConversations();
    }

    private void init(){
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private void setListeners(){
        binding.fabNewChat.setOnClickListener( v ->
                startActivity(new Intent(MainActivity.this, UsersActivity.class)));
        binding.imageProfile.setOnClickListener(v -> {
            String NOMER = preferenceManager.getString(Constans.KEY_PHONE_NUMBER);
            Intent intent = new Intent(MainActivity.this, AccountActivity.class);
            intent.putExtra("PHONENUM", NOMER);
            startActivity(intent);
        });
    }

    private void Dif(){
        String PHONENUM = preferenceManager.getString(Constans.KEY_PHONE_NUMBER);
        db.collection(Constans.KEY_COLLECTION_USERS).whereEqualTo("phone", PHONENUM)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            // Документ существует, получаем данные
                            String name = document.getString("name");
                            String surname = document.getString("surname");
                            String image = document.getString("image");
                            loadUserDetailsFromDatabase(name, surname, image);
                        } else {
                            // Документ не существует
                            Toast.makeText(MainActivity.this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
                            //loadUserDetails();
                        }
                    } else {
                        // Обработка ошибки
                        Log.d("Firestore", "Ошибка при получении данных из Firestore", task.getException());
                        Toast.makeText(MainActivity.this, "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void loadUserDetailsFromDatabase(String name, String surname, String image){
        String FULLNAME    = name + " " + surname;
        binding.textName.setText(FULLNAME);
        byte[] bytes = Base64.decode(image, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations(){
        db.collection(Constans.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constans.KEY_SENDER_ID, preferenceManager.getString(Constans.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        db.collection(Constans.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constans.KEY_RECEIVER_ID, preferenceManager.getString(Constans.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constans.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constans.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;
                    if (preferenceManager.getString(Constans.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constans.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constans.KEY_RECEIVER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constans.KEY_RECEIVER_ID);
                        chatMessage.conversionSurname = documentChange.getDocument().getString(Constans.KEY_RECEIVER_SURNAME);
                    } else {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constans.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constans.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constans.KEY_SENDER_ID);
                        chatMessage.conversionSurname = documentChange.getDocument().getString(Constans.KEY_SENDER_SURNAME);
                    }
                    chatMessage.message = documentChange.getDocument().getString(Constans.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constans.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for (int i = 0; i < conversations.size(); i++){
                        String senderId = documentChange.getDocument().getString(Constans.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constans.KEY_RECEIVER_ID);
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message = documentChange.getDocument().getString(Constans.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constans.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token){ // обновление токена в бд
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constans.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constans.KEY_USER_ID)
                );
        documentReference.update(Constans.KEY_FCM_TOKEN, token)
                .addOnFailureListener( e -> showToast("Unable to update token"));
    }

    private void signOut(){ // выход из сессии
        showToast ("Signing out...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                db.collection(Constans.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constans.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constans.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();

                    startActivity(new Intent(MainActivity.this, SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    public void onConversionClicked(User user) { //переход в сообщения с пользователем
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constans.KEY_USER, user);
        startActivity(intent);
    }
}