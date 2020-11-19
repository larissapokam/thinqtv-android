package com.thinqtv.thinqtv_android;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.thinqtv.thinqtv_android.data.UserRepository;

import java.util.HashMap;

public class AccountSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        HashMap<String, String> userInfo = UserRepository.getInstance().getLoggedInUser().getUserInfo();
        EditText email = findViewById(R.id.email);
        email.setText(userInfo.get("email"));
        EditText newPassword = findViewById(R.id.new_password);
        EditText newPasswordConfirm = findViewById(R.id.new_password_confirm);
        EditText permalink = findViewById(R.id.permalink);
        permalink.setText(userInfo.get("permalink"));
        Button sendButton = findViewById(R.id.send);
        Context context = this;
        sendButton.setOnClickListener(view -> {
            UserRepository.getInstance().updateAccount(context, email.getText().toString(), newPassword.getText().toString(),
                newPasswordConfirm.getText().toString(), permalink.getText().toString());
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
