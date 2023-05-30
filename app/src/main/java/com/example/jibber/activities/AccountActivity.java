package com.example.jibber.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jibber.R;
import com.example.jibber.databinding.ActivityMainBinding;
import com.example.jibber.databinding.ActivityUsersBinding;
import com.example.jibber.utilities.Constans;
import com.example.jibber.utilities.PreferenceManager;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountActivity extends AppCompatActivity {

    TextView titleName, titleSurname;
    TextView profileFullName, profilePhoneNumber, profileLogin, settings_bio;
    CircleImageView settings_user_photo;
    AppCompatImageView imageBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        titleName = findViewById(R.id.titleName);
        titleSurname = findViewById(R.id.titleSurname);
        profileFullName = findViewById(R.id.profileFullName);
        profilePhoneNumber = findViewById(R.id.profilePhoneNumber);
        profileLogin = findViewById(R.id.profileLogin);
        settings_bio = findViewById(R.id.settings_bio);
        imageBack = findViewById(R.id.imageBack);
        settings_user_photo = findViewById(R.id.settings_user_photo);
        loadUserDetails();
        setListeners();
    }

    private  void setListeners(){
        imageBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadUserDetails(){
        titleName.setText(getIntent().getStringExtra("name"));
        titleSurname.setText(getIntent().getStringExtra("surname"));
        profilePhoneNumber.setText(getIntent().getStringExtra("phoneNumber"));
        profileLogin.setText(getIntent().getStringExtra("username"));
        profileFullName.setText(titleName.getText().toString() + " " + titleSurname.getText().toString());
        //byte[] bytes = Base64.decode(getIntent().getStringExtra("image"), Base64.DEFAULT);
        //Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        //settings_user_photo.setImageBitmap(bitmap);
    }
}