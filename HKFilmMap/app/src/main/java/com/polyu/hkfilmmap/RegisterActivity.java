package com.polyu.hkfilmmap;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth auth;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilPasswordConfirm;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etPasswordConfirm;
    private TextView tvError;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        SystemBarInsetsHelper.applyContentInsets(this, R.id.contentRoot);

        auth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilPasswordConfirm = findViewById(R.id.tilPasswordConfirm);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        tvError = findViewById(R.id.tvError);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoLogin = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(this);
        tvGoLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnRegister) {
            attemptRegister();
        } else if (id == R.id.tvGoLogin) {
            finish();
        }
    }

    private void attemptRegister() {
        clearErrors();
        String email = text(etEmail);
        String password = text(etPassword);
        String confirm = text(etPasswordConfirm);

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
        if (!password.equals(confirm)) {
            tilPasswordConfirm.setError(getString(R.string.auth_error_password_mismatch));
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoading(false);
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
        tilPasswordConfirm.setError(null);
        tvError.setVisibility(View.GONE);
    }

    private void showError(Exception e) {
        String msg = getString(R.string.auth_error_register_failed);
        if (e instanceof FirebaseAuthUserCollisionException) {
            msg = "This email is already registered. Sign in instead.";
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            msg = getString(R.string.auth_error_password_short);
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            msg = "Invalid email format.";
        } else if (e != null && e.getMessage() != null) {
            msg = e.getMessage();
        }
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setAlpha(loading ? 0.6f : 1f);
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
