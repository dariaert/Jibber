package com.example.jibber.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.jibber.R;
import com.example.jibber.activities.AccountActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class NewBio extends Fragment {

    AppCompatImageView imageBack, imageOk;
    EditText inputBio;
    FirebaseFirestore db;
    private String userID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_bio, container, false);

        imageBack = view.findViewById(R.id.imageBack);
        imageOk = view.findViewById(R.id.imageOk);
        inputBio = view.findViewById(R.id.inputBio);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            userID = getArguments().getString("userID");
        }
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
                String newBio = inputBio.getText().toString().trim();
                saveNewName(newBio);
            }
        });
    }

    private void saveNewName(String newBio) {
        db.collection("users").document(userID)
                .update(
                        "bio", newBio)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Успешно обновлено имя в Firestore
                            getParentFragmentManager().popBackStack();
                            AccountActivity accountActivity = (AccountActivity) getActivity();
                            if (accountActivity != null) {
                                accountActivity.updateBio(newBio);
                            }
                        } else {
                            // Обработка ошибки при обновлении имени
                        }
                    }
                });
    }
}