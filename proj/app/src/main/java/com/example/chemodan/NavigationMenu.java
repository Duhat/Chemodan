package com.example.chemodan;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class NavigationMenu {
    private final AppCompatActivity activity;
    private final DrawerLayout drawerLayout;
    private GoogleSignInClient googleSignInClient;
    private ImageView profileImage;
    private TextView profileName, profileEmail;
    private TextView signInOutText;

    public NavigationMenu(AppCompatActivity activity, DrawerLayout drawerLayout) {
        this.activity = activity;
        this.drawerLayout = drawerLayout;

        initializeComponents();
        setupAuth();
        setupClickListeners();
        updateAuthUI();
    }

    private void initializeComponents() {
        // Убедитесь, что в layout активности есть View с id drawer_view_weather
        View drawerView = activity.findViewById(R.id.drawer_view_weather);
        if (drawerView == null) {
            throw new IllegalStateException("drawer_view_weather not found in activity layout");
        }

        profileImage = drawerView.findViewById(R.id.profile_image);
        profileName = drawerView.findViewById(R.id.profile_name);
        profileEmail = drawerView.findViewById(R.id.profile_email);
        signInOutText = drawerView.findViewById(R.id.sign_out_text);

        // Проверка на null для всех элементов
        if (profileImage == null || profileName == null || profileEmail == null || signInOutText == null) {
            throw new IllegalStateException("One or more navigation menu views not found");
        }
    }

    private void setupAuth() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    private void setupClickListeners() {
        View drawerView = activity.findViewById(R.id.drawer_view_weather);

        drawerView.findViewById(R.id.weather_btn).setOnClickListener(v -> {
            closeDrawer();

            Toast.makeText(activity, "Weather screen is active", Toast.LENGTH_SHORT).show();
        });

        drawerView.findViewById(R.id.money_btn).setOnClickListener(v -> {
            closeDrawer();
            Intent intent = new Intent(activity, currency_converter.class);
            activity.startActivity(intent);
        });

        drawerView.findViewById(R.id.sign_out_btn).setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                signOut();
            } else {
                signIn();
            }
        });
    }

    public void updateAuthUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Пользователь вошел - показываем профиль и Sign out
            profileName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            profileEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            signInOutText.setText("Sign out");

            if (user.getPhotoUrl() != null) {
                Glide.with(activity)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            // Пользователь не вошел - показываем Sign in
            profileName.setText("Guest");
            profileEmail.setText("Sign in to access all features");
            signInOutText.setText("Sign in");
            profileImage.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    private void signIn() {
        closeDrawer();
        Intent intent = new Intent(activity, activity_registration.class);
        activity.startActivity(intent);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        googleSignInClient.signOut().addOnCompleteListener(activity, task -> {
            updateAuthUI();
            Toast.makeText(activity, "Signed out successfully", Toast.LENGTH_SHORT).show();
        });
    }

    public void openDrawer() {
        drawerLayout.openDrawer(Gravity.START);
        updateAuthUI(); // Обновляем UI при каждом открытии меню
    }

    public void closeDrawer() {
        drawerLayout.closeDrawer(Gravity.START);
    }
}
