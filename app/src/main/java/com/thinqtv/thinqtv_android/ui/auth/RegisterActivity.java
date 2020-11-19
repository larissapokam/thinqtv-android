package com.thinqtv.thinqtv_android.ui.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.thinqtv.thinqtv_android.MainActivity;
import com.thinqtv.thinqtv_android.R;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import static java.security.AccessController.getContext;

public class RegisterActivity extends AppCompatActivity {
    private RegisterViewModel registerViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        registerViewModel = ViewModelProviders.of(this, new AuthViewModelFactory())
                .get(RegisterViewModel.class);

        final EditText emailEditText = findViewById(R.id.email);
        final EditText nameEditText = findViewById(R.id.name);
        final EditText permalinkEditText = findViewById(R.id.permalink);
        final EditText passwordEditText = findViewById(R.id.password);
        final EditText passwordConfirmationEditText = findViewById(R.id.password_confirmation);
        final Button registerButton = findViewById(R.id.register_button);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);
        final TextView errorTextView = findViewById(R.id.error);

        // Can't submit initial empty form- some text has to be typed.
        registerButton.setEnabled(false);

        // Check if the data in the form is valid each time the registerFormState changes.
        registerViewModel.getRegisterFormState().observe(this, registerFormState -> {
            if (registerFormState == null) {
                return;
            }
            registerButton.setEnabled(registerFormState.isDataValid());
            if (registerFormState.getEmailError() != null) {
                emailEditText.setError(getString(registerFormState.getEmailError()));
            }
            if (registerFormState.getNameError() != null) {
                nameEditText.setError(getString(registerFormState.getNameError()));
            }
            if (registerFormState.getPermalinkError() != null) {
                permalinkEditText.setError(getString(registerFormState.getPermalinkError()));
            }
            if (registerFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(registerFormState.getPasswordError()));
            }
            if (registerFormState.getPasswordConfirmationError() != null) {
                passwordConfirmationEditText.setError(getString(registerFormState.getPasswordConfirmationError()));
            }
        });

        // Respond once the result is made available.
        registerViewModel.getResult().observe(this, result -> {
            if (result == null) {
                return;
            }
            loadingProgressBar.setVisibility(View.GONE);
            if (!result.isSuccess() && result.getData() != null) {
                List<?> errorList = (List<?>)result.getData();
                showRegisterFailed(errorList, errorTextView);
            }
            if (result.isSuccess()) {
                setResult(Activity.RESULT_OK);

                // Complete and destroy activity on a successful registration.
                //finish();
                registerButton.setOnClickListener(view -> goToMain());

            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                registerViewModel.registerDataChanged(emailEditText.getText().toString(),
                        nameEditText.getText().toString(), permalinkEditText.getText().toString(),
                        passwordEditText.getText().toString(), passwordConfirmationEditText.getText().toString());
            }
        };

        emailEditText.addTextChangedListener(afterTextChangedListener);
        nameEditText.addTextChangedListener(afterTextChangedListener);
        permalinkEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordConfirmationEditText.addTextChangedListener(afterTextChangedListener);

        Context context = this;
        registerButton.setOnClickListener(view -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            registerViewModel.register(emailEditText.getText().toString(),
                    nameEditText.getText().toString(), permalinkEditText.getText().toString(),
                    passwordEditText.getText().toString(), context);
        });
    }

    // Display errors at top of the screen.
    private void showRegisterFailed(List<?> errorList, TextView errorTextView) {
        if (errorList.size() == 0) {
            errorTextView.setText(R.string.register_failed);
        }
        else {
            // If multiple errors exist, show the first one.
            errorTextView.setText((Integer)errorList.get(0));
        }
    }

    private void goToMain() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}