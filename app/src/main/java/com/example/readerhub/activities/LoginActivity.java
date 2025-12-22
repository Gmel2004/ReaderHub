package com.example.readerhub.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.readerhub.R;
import com.example.readerhub.api.RetrofitClient;
import com.example.readerhub.api.models.AuthResponse;
import com.example.readerhub.api.models.LoginRequest;
import com.example.readerhub.utils.PreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private ProgressBar progressBar;
    private PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefsManager = new PreferencesManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        setLoading(true);

        LoginRequest request = new LoginRequest(email, password);

        RetrofitClient.getApiService().login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    if (authResponse.isSuccess()) {
                        // Save user session
                        prefsManager.saveUserSession(
                                authResponse.getUser().getId(),
                                authResponse.getToken(),
                                authResponse.getUser().getUsername(),
                                authResponse.getUser().getEmail()
                        );

                        // Navigate to main activity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                authResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Login failed. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
    }
}