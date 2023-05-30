package com.example.jibber.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jibber.R;
import com.example.jibber.databinding.ActivitySignUpBinding;
import com.example.jibber.utilities.Constans;
import com.example.jibber.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    EditText inputName, inputSurname, inputUserName, inputPhone;
    Button buttonSignUp;
    TextView mesactual;
    FirebaseFirestore database = FirebaseFirestore.getInstance();

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private String PhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        inputUserName = findViewById(R.id.inputUserName);
        mesactual = findViewById(R.id.mesactual);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        setSTATUS();
        setListeners();
        PhoneNumber = getIntent().getStringExtra("NUMBER");

    }

    private void setSTATUS(){
        inputUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Не требуется
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // При изменении текста в EditText выполняется проверка уникальности имени пользователя
                String enteredUsername = s.toString();
                //checkUsernameAvailability(enteredUsername);

                database.collection(Constans.KEY_COLLECTION_USERS)
                        .whereEqualTo(Constans.KEY_USERNAME, enteredUsername)
                        .get()
                        .addOnCompleteListener(dar -> {
                            if (dar.isSuccessful()) {
                                boolean isUsernameAvailable = dar.getResult().isEmpty();
                                if (isUsernameAvailable) {
                                    // Имя пользователя доступно
                                    mesactual.setText("Имя пользователя доступно");
                                    buttonSignUp.setEnabled(true); // Делаем кнопку доступной
                                } else {
                                    // Имя пользователя уже существует в базе данных
                                    mesactual.setText("Имя пользователя уже занято");
                                    buttonSignUp.setEnabled(false); // Делаем кнопку недоступной
                                }
                            } else {
                                // Обработка ошибки запроса к базе данных
                                mesactual.setText("Ошибка при проверке имени пользователя");
                                buttonSignUp.setEnabled(false); // Делаем кнопку недоступной
                            }
                        });

            }

            @Override
            public void afterTextChanged(Editable s) {
                // Не требуется
            }
        });

    }

    private void setListeners(){
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()){
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp(){
        loading(true);
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constans.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constans.KEY_SURNAME, binding.inputSurname.getText().toString());
        user.put(Constans.KEY_USERNAME, binding.inputUserName.getText().toString());
        user.put(Constans.KEY_PHONE_NUMBER, PhoneNumber);
        user.put(Constans.KEY_BIO, null);
        user.put(Constans.KEY_IMAGE, encodedImage);
        database.collection(Constans.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constans.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constans.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constans.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constans.KEY_SURNAME, binding.inputSurname.getText().toString());
                    preferenceManager.putString(Constans.KEY_USERNAME, binding.inputUserName.getText().toString());
                    preferenceManager.putString(Constans.KEY_PHONE_NUMBER, PhoneNumber);
                    preferenceManager.putString(Constans.KEY_IMAGE, encodedImage);

                    // Сохраняем данные о пользователе в SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constans.KEY_NAME, binding.inputName.getText().toString());
                    editor.putString(Constans.KEY_SURNAME, binding.inputSurname.getText().toString());
                    editor.putString(Constans.KEY_USERNAME, binding.inputUserName.getText().toString());
                    editor.putString(Constans.KEY_PHONE_NUMBER, PhoneNumber);
                    editor.putString(Constans.KEY_IMAGE, encodedImage);
                    editor.apply();

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
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
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean isValidSignUpDetails(){
        if(encodedImage == null){
            showToast("Select profile image");
            return false;
        }else if (binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Enter first name");
            return  false;
        } else if (binding.inputSurname.getText().toString().trim().isEmpty()){
            showToast("Enter last name");
            return false;
        } else if (binding.inputUserName.getText().toString().trim().isEmpty()) {
            showToast("Enter username");
            return false;
        } else {
            return true;
        }
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }

}