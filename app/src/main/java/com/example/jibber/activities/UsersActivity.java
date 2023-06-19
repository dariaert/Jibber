package com.example.jibber.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.example.jibber.R;
import com.example.jibber.adapters.UsersAdapter;
import com.example.jibber.databinding.ActivityUsersBinding;
import com.example.jibber.listeners.UserListener;
import com.example.jibber.models.User;
import com.example.jibber.utilities.Constans;
import com.example.jibber.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
    }

    private  void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constans.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task ->{
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constans.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null){
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if (currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constans.KEY_NAME);
                            user.surname = queryDocumentSnapshot.getString(Constans.KEY_SURNAME);
                            user.username = queryDocumentSnapshot.getString(Constans.KEY_USERNAME);
                            user.image = queryDocumentSnapshot.getString(Constans.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constans.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        if (users.size() > 0){
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            binding.usersRecycleView.setAdapter(usersAdapter);
                            binding.usersRecycleView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
        /*inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Не требуется
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String enteredUsername = s.toString();
                FirebaseFirestore database = FirebaseFirestore.getInstance();
                database.collection(Constans.KEY_COLLECTION_USERS)
                        .whereEqualTo(Constans.KEY_USERNAME, enteredUsername)
                        .get()
                        .addOnCompleteListener(task ->{
                            String currentUserId = preferenceManager.getString(Constans.KEY_USER_ID);
                            if (task.isSuccessful() && task.getResult() != null){
                                List<User> users = new ArrayList<>();
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                                    if (currentUserId.equals(queryDocumentSnapshot.getId())){
                                        continue;
                                    }
                                    User user = new User();
                                    user.name = queryDocumentSnapshot.getString(Constans.KEY_NAME);
                                    user.surname = queryDocumentSnapshot.getString(Constans.KEY_SURNAME);
                                    user.username = queryDocumentSnapshot.getString(Constans.KEY_USERNAME);
                                    user.image = queryDocumentSnapshot.getString(Constans.KEY_IMAGE);
                                    user.token = queryDocumentSnapshot.getString(Constans.KEY_FCM_TOKEN);
                                    users.add(user);
                                }
                                if (users.size() > 0){
                                    UsersAdapter usersAdapter = new UsersAdapter(users);
                                    binding.usersRecycleView.setAdapter(usersAdapter);
                                    binding.usersRecycleView.setVisibility(View.VISIBLE);
                                } else {
                                    showErrorMessage();
                                }
                            } else {
                                showErrorMessage();
                            }
                        });
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Не требуется
            }
        }); */

    }

    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s", "No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading){
        if (isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constans.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}