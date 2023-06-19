package com.example.jibber.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jibber.R;
import com.example.jibber.fragments.NewBio;
import com.example.jibber.fragments.NewName;
import com.example.jibber.fragments.NewUsername;
import com.example.jibber.utilities.Constans;
import com.example.jibber.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountActivity extends AppCompatActivity {

    TextView titleName, titleSurname;
    private String encodedImage;
    TextView profileName, profilePhoneNumber, profileLogin, settings_bio, profileSurName, settings_label_fullname,
            settings_label_login, settings_label_bio;
    CircleImageView settings_user_photo, settings_change_photo;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration nameListener, usernameListener;

    AppCompatImageView imageBack;
    private ProgressBar progressBar;
    private ConstraintLayout container;
    private CollectionReference usersCollection;
    private String userID; // Поле для хранения ID пользователя

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        progressBar = findViewById(R.id.progressBar);
        container = findViewById(R.id.container);
        titleName = findViewById(R.id.titleName);
        titleSurname = findViewById(R.id.titleSurname);
        profilePhoneNumber = findViewById(R.id.profilePhoneNumber);
        profileLogin = findViewById(R.id.profileLogin);
        settings_bio = findViewById(R.id.settings_bio);
        imageBack = findViewById(R.id.imageBack);
        profileName = findViewById(R.id.profileName);
        profileSurName = findViewById(R.id.profileSurName);
        settings_user_photo = findViewById(R.id.settings_user_photo);
        settings_change_photo = findViewById(R.id.settings_change_photo);
        settings_label_fullname = findViewById(R.id.settings_label_fullname);
        settings_label_login = findViewById(R.id.settings_label_login);
        settings_label_bio = findViewById(R.id.settings_label_bio);
        usersCollection = db.collection("users");
        String PHONENUM = getIntent().getStringExtra("PHONENUM");
        getUserID(PHONENUM);
        Dif();
        setListeners();
    }

    private void Dif(){
        String PHONENUM = getIntent().getStringExtra("PHONENUM");
        progressBar.setVisibility(View.VISIBLE);
        container.setVisibility(View.GONE);
        db.collection(Constans.KEY_COLLECTION_USERS).whereEqualTo("phone", PHONENUM)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    container.setVisibility(View.VISIBLE);
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            // Документ существует, получаем данные
                            String name = document.getString("name");
                            String surname = document.getString("surname");
                            String username = document.getString("username");
                            String image = document.getString("image");
                            String bio = document.getString("bio");
                            loadUserDetailsFromDatabase(name, surname, image, username, bio, PHONENUM);
                        } else {
                            // Документ не существует
                            Toast.makeText(AccountActivity.this, "Пользователь не найден", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Обработка ошибки
                        Log.d("Firestore", "Ошибка при получении данных из Firestore", task.getException());
                        Toast.makeText(AccountActivity.this, "Ошибка при загрузке данных", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void loadUserDetailsFromDatabase(String name, String surname, String image, String username, String bio, String PHONENUM){
        titleName.setText(name);
        titleSurname.setText(surname);
        profileLogin.setText(username);
        profilePhoneNumber.setText(PHONENUM);
        profileSurName.setText(name);
        profileName.setText(surname);
        if (bio == null){
            settings_bio.setText(R.string.about_me);
        } else settings_bio.setText(bio);
        byte[] bytes = Base64.decode(image, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        settings_user_photo.setImageBitmap(bitmap);
    }

    private  void setListeners(){
        imageBack.setOnClickListener(v -> onBackPressed());
        settings_label_fullname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNewNameFragment();
            }
        });
        settings_label_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUsernameFragment();
            }
        });
        settings_label_bio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNewBioFragment();
            }
        });
        settings_change_photo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private String encodeImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK){
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try{
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            settings_user_photo.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                            updateImageInFirestore(encodedImage); // Обновление изображения в Firestore
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void openNewNameFragment() {
        Bundle bundle = new Bundle();
        bundle.putString("userID", userID);
        NewName newNameFragment = new NewName();
        newNameFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, newNameFragment)
                .addToBackStack(null)
                .commit();
    }
    private void openUsernameFragment() {
        Bundle bundle = new Bundle();
        bundle.putString("userID", userID);
        NewUsername newNameFragment = new NewUsername();
        newNameFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, newNameFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openNewBioFragment() {
        Bundle bundle = new Bundle();
        bundle.putString("userID", userID);
        NewBio newBioFragment = new NewBio();
        newBioFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, newBioFragment)
                .addToBackStack(null)
                .commit();
    }

    private void getUserID(String PHONENUM) {
        usersCollection.whereEqualTo("phone", PHONENUM)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                userID = document.getId();
                                // В этом месте вы можете использовать userID в своей логике

                                listenForNameChanges(userID);
                                listenForUsernameChanges(userID);
                                listenForBioChanges(userID);
                            }
                        } else {
                            Log.d("Firestore", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void listenForNameChanges(String userID) {
        nameListener = db.collection("users").document(userID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            // Обработка ошибки чтения из Firestore
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            String name = snapshot.getString("name");
                            String surname = snapshot.getString("surname");
                            if (name != null) {
                                if (surname != null){
                                    updateName(name, surname);
                                }
                            }
                        }
                    }
                });
    }

    private void listenForUsernameChanges(String userID) {
        usernameListener = db.collection("users").document(userID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            // Обработка ошибки чтения из Firestore
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            String username = snapshot.getString("username");
                            if (username != null) {
                                updateUsername(username);
                            }
                        }
                    }
                });
    }

    private void updateImageInFirestore(String encodedImage) {
        db.collection("users").document(userID)
                .update("image", encodedImage)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Успешно обновлено фото в Firestore
                        } else {
                            // Обработка ошибки при обновлении имени
                        }
                    }
                });
    }

    private void listenForBioChanges(String userID) {
        usernameListener = db.collection("users").document(userID)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            // Обработка ошибки чтения из Firestore
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            String bio = snapshot.getString("bio");
                            if (bio != null) {
                                updateBio(bio);
                            }
                        }
                    }
                });
    }

    // Метод для обновления имени в TextView
    public void updateName(String newName, String newSurName) {
        profileName.setText(newName);
        profileSurName.setText(newSurName);
    }

    public void updateUsername(String newUsername) {
        profileLogin.setText(newUsername);
    }
    public void updateBio(String newBio) {settings_bio.setText(newBio); }
}