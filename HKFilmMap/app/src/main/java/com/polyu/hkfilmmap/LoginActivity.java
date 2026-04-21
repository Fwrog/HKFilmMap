package com.polyu.hkfilmmap;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth auth;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextView tvError;
    private MaterialButton btnLogin;
    private MaterialButton btnEmailLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SystemBarInsetsHelper.applyContentInsets(this, R.id.contentRoot);

        auth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        btnEmailLink = findViewById(R.id.btnEmailLink);
        TextView tvGoRegister = findViewById(R.id.tvGoRegister);

        btnLogin.setOnClickListener(this);
        btnEmailLink.setOnClickListener(this);
        tvGoRegister.setOnClickListener(this);

        handleIncomingLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingLink(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            openMainAndFinish();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnLogin) {
            attemptLogin();
        } else if (id == R.id.btnEmailLink) {
            attemptSendEmailLink();
        } else if (id == R.id.tvGoRegister) {
            startActivity(new Intent(this, RegisterActivity.class));
        }
    }

    private void attemptSendEmailLink() {
        clearErrors();
        String email = text(etEmail);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.auth_error_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.auth_error_email_invalid));
            return;
        }

        ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                .setUrl(getString(R.string.firebase_auth_continue_url))
                .setHandleCodeInApp(true)
                .setAndroidPackageName(getPackageName(), true, "1")
                .build();

        setLoading(true);
        setEmailLinkLoading(true);
        AuthEmailLinkStore.saveEmail(this, email);
        auth.sendSignInLinkToEmail(email, settings)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        setLoading(false);
                        setEmailLinkLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(
                                            LoginActivity.this,
                                            R.string.auth_email_link_sent,
                                            Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            AuthEmailLinkStore.clear(LoginActivity.this);
                            Exception e = task.getException();
                            String msg = getString(R.string.auth_error_email_link_failed);
                            if (e != null && e.getMessage() != null) {
                                msg = e.getMessage();
                            }
                            tvError.setText(msg);
                            tvError.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void handleIncomingLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        String emailLink = intent.getData().toString();
        if (!auth.isSignInWithEmailLink(emailLink)) {
            return;
        }

        String email = AuthEmailLinkStore.getEmail(this);
        if (TextUtils.isEmpty(email)) {
            email = text(etEmail);
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.auth_error_email_missing_for_link));
            tvError.setText(getString(R.string.auth_error_email_missing_for_link));
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        setLoading(true);
        setEmailLinkLoading(true);
        auth.signInWithEmailLink(email, emailLink)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoading(false);
                        setEmailLinkLoading(false);
                        if (task.isSuccessful()) {
                            AuthEmailLinkStore.clear(LoginActivity.this);
                            openMainAndFinish();
                        } else {
                            Exception e = task.getException();
                            String msg = getString(R.string.auth_error_email_link_complete);
                            if (e != null && e.getMessage() != null) {
                                msg = e.getMessage();
                            }
                            tvError.setText(msg);
                            tvError.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void attemptLogin() {
        clearErrors();
        String email = text(etEmail);
        String password = text(etPassword);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.auth_error_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.auth_error_email_invalid));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.auth_error_password_required));
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.auth_error_password_short));
            return;
        }

        setLoading(true);
        setEmailLinkLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoading(false);
                        setEmailLinkLoading(false);
                        if (task.isSuccessful()) {
                            openMainAndFinish();
                        } else {
                            showError(task.getException());
                        }
                    }
                });
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tvError.setVisibility(View.GONE);
    }

    private void showError(Exception e) {
        String msg = getString(R.string.auth_error_sign_in_failed);
        if (e instanceof FirebaseAuthInvalidUserException) {
            msg = "No account for this email. Register first.";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            msg = "Wrong password or invalid email.";
        } else if (e != null && e.getMessage() != null) {
            msg = e.getMessage();
        }
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setAlpha(loading ? 0.6f : 1f);
    }

    private void setEmailLinkLoading(boolean loading) {
        btnEmailLink.setEnabled(!loading);
        btnEmailLink.setAlpha(loading ? 0.6f : 1f);
    }

    private static String text(TextInputEditText et) {
        if (et.getText() == null) {
            return "";
        }
        return et.getText().toString().trim();
    }

    private void openMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
