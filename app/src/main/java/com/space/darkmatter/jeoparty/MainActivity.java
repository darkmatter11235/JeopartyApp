package com.space.darkmatter.jeoparty;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements JpdReader.AsyncResponse {

    JpdReader reader;


    private TextView playerHighScore;

    private  int numColumns = 6;
    private int numRows = 6;
    private int round_number = 1;
    private int player_score = 0;
    private String mGameNumber;
    private String mFilePath;

    private String mJsonUrl;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private StorageReference mStorageRef;

    private ProgressDialog mProgressReadJson;

    GridLayout layout;

    static final int GET_ANSWER_INTENT = 1;

    int r = numRows;
    int c = numColumns;
    private boolean status[][];

    MediaPlayer theme_player;

    public void initStatus() {
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {

               status[i][j] = false;

            }
        }
    }

    public void setRound() {
        boolean done = true;

        for (int i = 1; i < r; i++) {
            for (int j = 0; j < c; j++) {
               if ( status[i][j] == false ) {
                   done = false;
                   return;
               }
            }
        }

        if ( done == true ) {
            round_number++;
            initStatus();
            if ( round_number == 3 ) {
                setHighScore();
            }
        }

    }



    private void registerUser() {
        Intent registerIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivityForResult(registerIntent, 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = new boolean[numRows][numColumns];

        playerHighScore = (TextView) findViewById(R.id.player_high_score);
        mAuth = FirebaseAuth.getInstance();

        mProgressReadJson = new ProgressDialog(this);


       if ( mAuth.getCurrentUser() == null ) {
            finish();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            return;
        }

        if ( getIntent().getExtras() == null ) {
            finish();
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            return;
        }

        mGameNumber = getIntent().getExtras().getString("mGameNumber");

        mJsonUrl = getIntent().getExtras().getString("mJsonUrl");

        if ( mGameNumber == "" || mJsonUrl == "" ) {

            Toast.makeText(this, "Got empty game number !! ", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        }

        mStorageRef = FirebaseStorage.getInstance().getReference();

        String displayName = mAuth.getCurrentUser().getEmail().toString().trim();

        //Toast.makeText(this, "Bonjour! " + displayName, Toast.LENGTH_SHORT).show();

        String maxScore = getHighScore();

        playerHighScore.setText("High Score: $"+ maxScore);
        playerHighScore.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        playerHighScore.setTextColor(Color.BLACK);

        TextView playerScore = (TextView) findViewById(R.id.player_score_tv);
        playerScore.setText("Score: $"+ player_score);
        playerScore.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        playerScore.setTextColor(Color.BLACK);


        initStatus();

        setRound();

        mProgressReadJson.setMessage("Mr.Trebek Hard at Work!");

        mProgressReadJson.show();
        reader = (JpdReader) new JpdReader(this, mJsonUrl).execute();

        //fillview(layout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        /*
        if ( mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }*/
    }

    public void fillview(GridLayout gl) {

       //Stretch buttons
        int idealChildWidth = (int) ((gl.getWidth())/gl.getColumnCount());
        int idealChildHeight = (int) ((gl.getHeight())/gl.getRowCount());

        //int numColumns = gl.getColumnCount();
        String[] jpd_categories = new String[numColumns];
        String[] dbl_jpd_categories = new String[numColumns];
        String final_category = reader.final_category;

        int i = 0;


        for(String c : reader.jpd_categories) {
            jpd_categories[i] = c;
            i++;
        }
        i = 0;
        for(String c : reader.dbl_jpd_categories) {
            dbl_jpd_categories[i] = c;
            i++;
        }


        for( i=0; i< gl.getChildCount();i++) {
            LinearLayout v = (LinearLayout) gl.getChildAt(i);
            final String category_name;

            final int rowNum = i / numColumns;
            final int colNum = i % numColumns;

            int value = rowNum * 200 * round_number;


            if (round_number == 1) {
                category_name = jpd_categories[colNum];
                if (reader.mJpdValuesArray.length >= rowNum) {
                    if (rowNum != 0) {
                        value = reader.mJpdValuesArray[rowNum - 1];
                    } else {
                        value = 0;
                    }
                } else {
                    continue;
                }
            } else if (round_number == 2) {
                category_name = dbl_jpd_categories[colNum];
                if (reader.mJpdValuesArray.length >= rowNum) {
                    if (rowNum != 0) {
                        value = reader.mDblJpdValuesArray[rowNum - 1];
                    } else {
                        value = 0;
                    }
                } else {
                    continue;
                }
            } else {
                category_name = final_category;
            }

            JpdAnswer ans = getAnswer(round_number, category_name, value);

            final TextView tv = (TextView) v.getChildAt(0);
            tv.setWidth(idealChildWidth);
            tv.setHeight(idealChildHeight);
            int padv = 5;
            tv.setPadding(padv, padv, padv, padv);

            tv.setBackgroundColor(Color.BLUE);
            if ( rowNum == 0) {
                tv.setTextColor(Color.WHITE);
                status[rowNum][colNum] = true;
            } else if ( status[rowNum][colNum] == true  || ans == null ) {
                tv.setTextColor(Color.BLACK);
                status[rowNum][colNum] = true;
            } else {
                tv.setTextColor(Color.YELLOW);
            }
            tv.setGravity(Gravity.CENTER);
            tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));



            //Drawable bkgrnd = getResources().getDrawable(R.drawable.jpd_border);
            tv.setBackgroundResource(R.drawable.jpd_border);


            if ( rowNum == 0 ) {
                tv.setText(category_name);
                tv.setTextSize((float) 10.0);
            } else {
                //value = reader.jpd.get(Pair.create(category_name, Integer.toString(value)));
                tv.setText("$"+Integer.toString(value));
                tv.setTextSize((float) 16.0);
            }

            final Integer passedValue = value;

            if ( rowNum != 0 ) {
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       // Toast.makeText(getApplicationContext(), "Row " + Integer.toString(rowNum) + " Col " + Integer.toString(colNum), Toast.LENGTH_SHORT).show();
                        tv.setTextColor(Color.BLACK);
                        //theme_player.stop();
                        showAnswer(rowNum, colNum, category_name, passedValue);


                    }
                });
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( requestCode == GET_ANSWER_INTENT ) {

            if ( resultCode == RESULT_OK) {
                String newScore = data.getStringExtra("newScore");
                player_score = Integer.parseInt(newScore);
                TextView tv = (TextView) findViewById(R.id.player_score_tv);
                tv.setTextColor(Color.BLACK);
                tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                tv.setText("Score: $"+ newScore);
                Toast.makeText(this, "New Score: $" + newScore, Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
       // setRound();
       // fillview(layout);
    }


    private void setHighScore() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String scoreKey = getString(R.string.high_score);
        String currentHighScore = getHighScore();
        Integer chs = Integer.parseInt(currentHighScore);
        Integer mScore = player_score;
        if ( chs < mScore ) {
            chs = mScore;
            Toast.makeText(MainActivity.this, "Yapoo! New High Score! : "+chs.toString(), Toast.LENGTH_SHORT).show();
        }
        editor.putString(scoreKey, chs.toString());
        editor.commit();
    }

    private String getHighScore() {

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String scoreKey = getString(R.string.high_score);
        if (!sharedPref.contains(scoreKey)) {
            return "0";
        } else {
            return sharedPref.getString(scoreKey, "0");
        }

    }

    JpdAnswer getAnswer(int round_number, String category_name, Integer value) {
        JpdAnswer ans = null;

        String round_name;

        if ( round_number == 1 ) {
            round_name = "Jeopardy!";

            //ans = reader.jpd.get(Pair.create(category_name, value));
        } else if ( round_number == 2 ) {
            round_name = "Double Jeopardy!";
            //ans = reader.dbl_jpd.get(Pair.create(category_name, value));
        } else {
            return reader.finalJpd;
        }

        if ( reader.jpdData.containsKey(round_name)) {
            HashMap<String, HashMap<Integer, JpdAnswer> > level = reader.jpdData.get(round_name);
            if ( level.containsKey(category_name)) {
                HashMap<Integer, JpdAnswer> valueMap = level.get(category_name);
                if ( valueMap.containsKey(value) ) {

                    return valueMap.get(value);

                }  else {
                    return ans;
                }
            } else {
                return ans;
            }

        } else {
            return ans;
        }

    }
    //http://skeeto.s3.amazonaws.com/share/JEOPARDY_QUESTIONS1.json.gz

    public void showAnswer(int rnum, int cnum, String category_name, Integer value) {

        Intent intent = new Intent(MainActivity.this, DisplayAnswer.class);
        JpdAnswer ans;

        ans = getAnswer(round_number, category_name, value);

        if ( round_number == 1 || round_number == 2 )
            status[rnum][cnum] = true;

        if ( ans == null ) {
            Toast.makeText(this, "Sorry, this question was not answered in this show :( \n Please continue!....Bitte fahre fort! ", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.putExtra("question", ans.question);
        intent.putExtra("answer", ans.answer);
        intent.putExtra("value", value);
        intent.putExtra("total_score", Integer.toString(player_score));

        setRound();
        fillview(layout);
        startActivityForResult(intent, GET_ANSWER_INTENT);


    }

    @Override
    public void processFinish(String output) {

        mProgressReadJson.dismiss();

        //theme_player = MediaPlayer.create(this, R.raw.theme);
        //theme_player.start();

        layout = (GridLayout) findViewById(R.id.gridview);

        final ViewTreeObserver observer = layout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        GridLayout layout = findViewById(R.id.gridview);
                        fillview(layout);
                    }

                } );

        LayoutInflater inflater = getLayoutInflater();

        for(int r = 0; r < numRows; r++) {
            for(int c=0; c < numColumns; c++) {
                getLayoutInflater().inflate(R.layout.jpd_item, layout);
            }
        }

    }
}
