package com.space.darkmatter.jeoparty;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private TextView mGreetings;
    private Button mNewGame;
    private Button mSignOut;
    private String mJsonUrl;
    private StorageReference mStorageRef;
    private String mGameNumber;
    private String mFilePath;
    private ArrayList<String> mShowList;


    private void kickUserOut() {
        mAuth.signOut();
        finish();
        startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
    }

    private void readShowData() {

        AssetManager manager;
        String line = null;
        mShowList = new ArrayList<String>();


        InputStream is;
        InputStreamReader isr;
        BufferedReader br;
        try {
            manager = getAssets();
            is = manager.open("show_numbers.txt");
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                mShowList.add(line.trim());
            }

            br.close();
            isr.close();
            is.close();
        } catch (IOException e1) {
            Toast.makeText(getBaseContext(), "Problem!", Toast.LENGTH_SHORT).show();
        }
    }

    private void getShowJsonUrl() {

        String relativePath = "shows/" + mGameNumber + ".json";
        StorageReference showRef = mStorageRef.child(relativePath);


        showRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                mJsonUrl = uri.toString();
                Intent gameIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                gameIntent.putExtra("mGameNumber", mGameNumber);
                gameIntent.putExtra("mJsonUrl", mJsonUrl);
                finish();
                startActivity(gameIntent);

                // Got the download URL for 'users/me/profile.png'
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Toast.makeText(WelcomeActivity.this, "Unable to load game number " + mGameNumber + " json file", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLastGamePlayed() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String gameKey = getString(R.string.last_played_game);
        editor.putString(gameKey, mGameNumber);
        editor.commit();
    }

    private String getNewGameName() {
        String newGame;
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String defaultGameName = "1";
        String gameKey = getString(R.string.last_played_game);
        if ( mShowList == null ) { return defaultGameName; }

        if ( !sharedPref.contains(gameKey) ) {
            editor.putString(gameKey, mShowList.get(0));
            editor.commit();
            newGame = mShowList.get(0);
        } else {
            String oldGame = sharedPref.getString(gameKey, "DEFAULT");
            if ( oldGame == "DEFAULT" ) {
                newGame = mShowList.get(0);
            } else {
                Integer old_index = mShowList.indexOf(oldGame);
                if ( old_index == -1 ) { return defaultGameName; }
                Integer new_index = old_index+1;
                newGame = mShowList.get(new_index);
            }
        }
        return newGame;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        mAuth = FirebaseAuth.getInstance();
        mNewGame = (Button) findViewById(R.id.newgame_button);
        mGreetings = (TextView) findViewById(R.id.greetings_tv);
        mUser = mAuth.getCurrentUser();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        if ( mUser == null ) {
            finish();
            startActivity( new Intent(this, LoginActivity.class));
        } else {

            String mUserName = mUser.getEmail().toString().trim();
            int index = mUserName.indexOf("@");
            String mUserID = mUserName.substring(0, index);
            mGreetings.setText("Welcome " + mUserID);
            Toast.makeText(this, "Bonjour! " + mUserID, Toast.LENGTH_SHORT).show();
            readShowData();

            final MediaPlayer theme_player = MediaPlayer.create(this, R.raw.intro);
            theme_player.start();
            theme_player.setLooping(false);

            mNewGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    mGameNumber = getNewGameName();
                    Toast.makeText(WelcomeActivity.this, "Starting new game # " + mGameNumber, Toast.LENGTH_SHORT).show();
                    setLastGamePlayed();
                    theme_player.stop();
                    getShowJsonUrl();

                    //gameIntent.putExtra("game_path", newGamePath);
                }

             });

            mSignOut = (Button) findViewById(R.id.welcome_signout_button);

            mSignOut.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    theme_player.stop();
                    kickUserOut();
                }
            });

        }
    }
}


