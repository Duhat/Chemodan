package com.example.chemodan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class activity_registration extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private static final String TAG = "GoogleSignIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Расширенная конфигурация Google Sign-In с запросом доступа к Drive
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.file"))
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.appdata"))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleSignInResult(result.getData()));

        Button googleSignInButton = findViewById(R.id.googleSignInButton);
        Button skipButton = findViewById(R.id.SkipButton);

        googleSignInButton.setOnClickListener(v -> signIn());
        skipButton.setOnClickListener(v -> proceedToMainApp(false));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Проверяем обновленный статус авторизации
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            proceedToMainApp(true);
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Intent data) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkDriveAccess(user);
                        }
                    } else {
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkDriveAccess(FirebaseUser user) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && account.getGrantedScopes().containsAll(
                java.util.Arrays.asList(
                        new Scope("https://www.googleapis.com/auth/drive.file"),
                        new Scope("https://www.googleapis.com/auth/drive.appdata")))) {
            proceedToMainApp(true);
        } else {

            signIn();
        }
    }

    private void proceedToMainApp(boolean isSignedIn) {
        Intent intent = new Intent(this, ChemodanActivity.class);
        intent.putExtra("isSignedIn", isSignedIn);


        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            intent.putExtra("google_account", account.getId());
        }

        startActivity(intent);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleSignInClient = null;
        googleSignInLauncher = null;
    }
}