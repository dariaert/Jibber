package com.example.jibber.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.example.jibber.R;
import com.example.jibber.activities.AccountActivity;
import com.example.jibber.utilities.Constans;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class NewUsername extends Fragment {

    AppCompatImageView imageBack, imageOk;
    EditText inputUsername;
    FirebaseFirestore db;
    private String userID;
    TextView mesactual;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_username, container, false);

        imageBack = view.findViewById(R.id.imageBack);
        imageOk = view.findViewById(R.id.imageOk);
        inputUsername = view.findViewById(R.id.Username);
        mesactual = view.findViewById(R.id.mesactual);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            userID = getArguments().getString("userID");
        }
        setSTATUS();
        setListeners();
        return view;
    }

    private void closeFragment() {
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

    private  void setListeners(){
        imageBack.setOnClickListener(v -> closeFragment());
        imageOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUsername = inputUsername.getText().toString().trim();
                saveNewName(newUsername);
            }
        });
    }

    private void setSTATUS(){
        inputUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Не требуется
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // При изменении текста в EditText выполняется проверка уникальности имени пользователя
                String enteredUsername = s.toString();
                //checkUsernameAvailability(enteredUsername);

                db.collection(Constans.KEY_COLLECTION_USERS)
                        .whereEqualTo(Constans.KEY_USERNAME, enteredUsername)
                        .get()
                        .addOnCompleteListener(dar -> {
                            if (dar.isSuccessful()) {
                                boolean isUsernameAvailable = dar.getResult().isEmpty();
                                if (isUsernameAvailable) {
                                    // Имя пользователя доступно
                                    mesactual.setText("Имя пользователя доступно");
                                    imageOk.setEnabled(true); // Делаем кнопку доступной

                                } else {
                                    // Имя пользователя уже существует в базе данных
                                    mesactual.setText("Имя пользователя уже занято");
                                    imageOk.setEnabled(false); // Делаем кнопку недоступной
                                    //imageOk.setBackgroundColor(Color.LTGRAY); // Устанавливаем серый цвет фона кнопки
                                }
                            } else {
                                // Обработка ошибки запроса к базе данных
                                mesactual.setText("Ошибка при проверке имени пользователя");
                                imageOk.setEnabled(false); // Делаем кнопку недоступной
                                imageOk.setBackgroundColor(Color.LTGRAY); // Устанавливаем серый цвет фона кнопки
                            }
                        });

            }

            @Override
            public void afterTextChanged(Editable s) {
                // Не требуется
            }
        });

    }

    private void saveNewName(String newUsername) {
        db.collection("users").document(userID)
                .update(
                        "username", newUsername)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Успешно обновлено имя в Firestore
                            getParentFragmentManager().popBackStack();
                            AccountActivity accountActivity = (AccountActivity) getActivity();
                            if (accountActivity != null) {
                                accountActivity.updateUsername(newUsername);
                            }
                        } else {
                            // Обработка ошибки при обновлении имени
                        }
                    }
                });
    }
}