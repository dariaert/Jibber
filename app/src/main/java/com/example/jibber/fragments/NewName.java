package com.example.jibber.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.jibber.R;
import com.example.jibber.activities.AccountActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class NewName extends Fragment {

    AppCompatImageView imageBack, imageOk;
    EditText inputName, inputSurname;
    FirebaseFirestore db;
    private String userID;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_name, container, false);

        imageBack = view.findViewById(R.id.imageBack);
        imageOk = view.findViewById(R.id.imageOk);
        inputName = view.findViewById(R.id.inputName);
        inputSurname = view.findViewById(R.id.inputSurname);
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
                String newName = inputName.getText().toString().trim();
                String newSurname = inputSurname.getText().toString().trim();
                saveNewName(newName, newSurname);
            }
        });
    }

    private void saveNewName(String newName, String newSurname) {
        db.collection("users").document(userID)
                .update(
                        "name", newName,
                        "surname", newSurname)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Успешно обновлено имя в Firestore
                            getParentFragmentManager().popBackStack();
                            AccountActivity accountActivity = (AccountActivity) getActivity();
                            if (accountActivity != null) {
                                accountActivity.updateName(newName, newSurname);
                            }
                        } else {
                            // Обработка ошибки при обновлении имени
                        }
                    }
                });
    }
}