package com.space.darkmatter.jeoparty;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText mEmail;
    private EditText mPassword;
    private Button mRegister;
    private TextView mSignInText;
    private TextView mDisplayNameText;
    private String mDisplayName;
    private ProgressDialog mProgressDialog;

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mRegister = (Button) findViewById(R.id.buttonRegister);
        mEmail = (EditText) findViewById(R.id.editTextEmail);
        mPassword = (EditText) findViewById(R.id.editTextPassword);
        mSignInText = (TextView) findViewById(R.id.textViewSignIn);
        mDisplayNameText = (TextView) findViewById(R.id.playerName);
        mDisplayName = mDisplayNameText.getText().toString();

        mProgressDialog = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();

        if ( mAuth.getCurrentUser() != null ) {
            finish();
            startActivity(new Intent(this, WelcomeActivity.class));
        }

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        mSignInText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });

    }

    private void registerUser() {

        String email = mEmail.getText().toString().trim();
        String password = mPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        if ( TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        //so far so good show progress bar
        mProgressDialog.setMessage("Registering User...");

        mProgressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(
                this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {

                            FirebaseUser user = mAuth.getCurrentUser();
                            if(user!=null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(mDisplayName).build();
                                user.updateProfile(profileUpdates);
                            }

                            Toast.makeText(RegisterActivity.this, "Successfully registered user with e-mail", Toast.LENGTH_SHORT).show();
                            finish();
                            Intent gameIntent = new Intent(RegisterActivity.this, WelcomeActivity.class);
                            startActivity(gameIntent);
                        } else {
                            //Log.w(TAG, "createUserWithEmailPassword failure ", task.getException());
                            Toast.makeText(RegisterActivity.this, "Unable to register User" + task.getException().toString(), Toast.LENGTH_SHORT).show();
                        }
                        mProgressDialog.dismiss();
                    }
                }
        );


    }


}
