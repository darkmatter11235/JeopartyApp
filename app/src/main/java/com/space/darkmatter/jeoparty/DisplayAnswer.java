package com.space.darkmatter.jeoparty;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by prasanth on 10/21/17.
 */

public class DisplayAnswer extends Activity {

    private ProgressBar barTimer;

    private TextView answerView;
    private TextView questionView;
    private String answerString;
    private String questionString;
    private MediaPlayer audio_timer;
    private Button update_score_button;

    private RadioGroup radioGroup;
    private MediaPlayer times_up;

    private TextView btnPass;

    private int currentScore;
    private int totalScore;


    private Button show_answer_button;

    private int timer_limit;


    private CountDownTimer countDownTimer;


    private void showAnswer() {
        barTimer.setProgress(timer_limit);
        questionView.setText(answerString);
        questionView.setTextSize( (float) 24);
        audio_timer.stop();
        //audio_timer.stop();
    }

    private void updateScore() {

        int selectedId = radioGroup.getCheckedRadioButtonId();
        RadioButton selected_button = (RadioButton) findViewById(selectedId);

        if ( selected_button == null ) {
            Toast.makeText(this, "Please select Correct or Wrong to update score", Toast.LENGTH_SHORT).show();
            return;
        }
        String selected_text = (String) selected_button.getText();

        if ( selected_text.equals("Correct")) {
            totalScore += currentScore;
        } else if ( selected_text.equals("Wrong")) {
            totalScore -= currentScore;
        } else if ( selected_text.equals("Pass")) {
            //do nothing
        }

        Intent returnIntent = new Intent();
        returnIntent.putExtra("newScore",Integer.toString(totalScore));
        setResult(Activity.RESULT_OK,returnIntent);
        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(audio_timer != null && audio_timer.isPlaying() ) {
            audio_timer.pause();
        }

        if (times_up != null && times_up.isPlaying()) {
            times_up.pause();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        if(audio_timer != null && audio_timer.isPlaying() ) {
            audio_timer.pause();
        }

        if (times_up != null && times_up.isPlaying()) {
            times_up.pause();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_answer);

        Intent iin = getIntent();
        Bundle b = iin.getExtras();

        timer_limit = 10;

        answerView = (TextView) findViewById(R.id.answer_text);
        questionView = (TextView) findViewById(R.id.question_text);
        barTimer = (ProgressBar) findViewById(R.id.progressBar2);
        btnPass = (TextView) findViewById(R.id.rg_pass);

        barTimer.setProgress(0);

        show_answer_button = (Button) findViewById(R.id.show_answer);

        update_score_button = (Button) findViewById(R.id.update_score);

        radioGroup = (RadioGroup) findViewById(R.id.yes_no_group);

        update_score_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateScore();
            }
        });

        show_answer_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAnswer();
            }
        });


        btnPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAnswer();
            }
        });

        if (b != null) {

            questionString = (String) b.get("question");
            answerString = (String) b.get("answer");

            currentScore = (Integer) b.get("value");

            String player_score = (String) b.get("total_score");
            totalScore = Integer.parseInt(player_score);

            answerView.setText(questionString);
            answerView.setTextSize((float) 24);
            barTimer.setMax(timer_limit);
            questionView.setText("");
            audio_timer = MediaPlayer.create(this, R.raw.timer);
            audio_timer.start();
            startTimer(timer_limit);

            //Toast.makeText(getApplicationContext(), "Question "+ question + " \n Answer " + answer, Toast.LENGTH_SHORT).show();
        }



    }


    private void startTimer(final int minuti) {


        //ProgressBar barTimer = (ProgressBar) findViewById(R.id.progressBar2);
        countDownTimer = new CountDownTimer(minuti * 1000, 500) {
            // 500 means, onTick function will be called at every 500 milliseconds

            @Override
            public void onTick(long leftTimeInMilliseconds) {

                long seconds = leftTimeInMilliseconds / 1000;
                barTimer.setProgress((int)seconds);
               // textTimer.setText(String.format("%02d", seconds/60) + ":" + String.format("%02d", seconds%60));
                // format the textview to show the easily readable format

            }
            @Override
            public void onFinish() {
                showAnswer();
                RadioButton wrongButton = (RadioButton) findViewById(R.id.rg_no);
                times_up = MediaPlayer.create(DisplayAnswer.this, R.raw.times_up);
                if ( audio_timer.isPlaying() ) {
                    audio_timer.stop();
                    times_up.setLooping(false);
                    times_up.start();
                    wrongButton.setChecked(true);
                    radioGroup.setEnabled(false);
                }
                //updateScore();
            }
        }.start();

    }
}
