package com.erichamion.freelance.oakglen;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

public class FeedbackActivity extends MenuHandlerActivity implements View.OnClickListener {

    private static final int[] DEPENDENT_QUESTION_IDS = {R.id.visitRating, R.id.relevance};
    private static final String[] EMAIL_ADDRESSES = {"JeffMBaxter@gmail.com"};
    private static final String EMAIL_SUBJECT = "Oak Glen app feedback";
    private final View[] mDependentQuestionViews = new View[DEPENDENT_QUESTION_IDS.length];
    private LinearLayout mQuestionsHolder;
    ImageView apple_1,apple_2,apple_3,apple_4,apple_5;
    SharedPreferences myPref;

    ImageView appleIVs [] = {
            apple_1,apple_2,apple_3,apple_4,apple_5
    };
    int feedbackAppleIds[] = {
            R.id.apple_1,
            R.id.apple_2,
            R.id.apple_3,
            R.id.apple_4,
            R.id.apple_5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View rootContentView = findViewById(R.id.rootContentView);
        setMainBackground(rootContentView, getIntent().getExtras());

        myPref = getSharedPreferences("Discover_Oak_Glen", MODE_PRIVATE);

//        RatingBar ratingBar = (RatingBar) findViewById(R.id.answerOverallRating);
//        assert ratingBar != null;
//        Util.setRatingBarToColoredImage(ratingBar);

        //take ids of all feedback apple
//        apple_1 = (ImageView) findViewById(R.id.apple_1);
//        apple_1.setOnClickListener(this);
//        apple_2 = (ImageView) findViewById(R.id.apple_2);
//        apple_2.setOnClickListener(this);
//        apple_3 = (ImageView) findViewById(R.id.apple_3);
//        apple_3.setOnClickListener(this);
//        apple_4 = (ImageView) findViewById(R.id.apple_4);
//        apple_4.setOnClickListener(this);
//        apple_5 = (ImageView) findViewById(R.id.apple_5);
//        apple_5.setOnClickListener(this);

        for(int i=0; i<appleIVs.length; i++){
            appleIVs[i] = (ImageView) findViewById(feedbackAppleIds[i]);
            appleIVs[i].setOnClickListener(this);
        }


        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        assert toolbar != null;
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_up_white_24dp);

        Spinner recentVisitAnswerView = (Spinner) findViewById(R.id.answerRecentVisit);
        assert recentVisitAnswerView != null;
        recentVisitAnswerView.setOnItemSelectedListener(recentVisitListener);

        mQuestionsHolder = (LinearLayout) findViewById(R.id.questionsHolder);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendEmail(createFeedbackReport());
            }
        });


    }

    private void fillDependentQuestionViewArray() {
        if (mDependentQuestionViews[0] == null) {
            for (int i = 0; i < DEPENDENT_QUESTION_IDS.length; i++) {
                mDependentQuestionViews[i] = findViewById(DEPENDENT_QUESTION_IDS[i]);
            }
        }
    }

    private final AdapterView.OnItemSelectedListener recentVisitListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            fillDependentQuestionViewArray();
            String[] options = getResources().getStringArray(R.array.visitedOptions);
            int visibility = (options[(int) id].startsWith("No")) ? View.GONE : View.VISIBLE;
            for (View dependentView : mDependentQuestionViews) {
                dependentView.setVisibility(visibility);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing
        }
    };

    private String createFeedbackReport() {
        StringBuilder resultBuilder = new StringBuilder("***Auto-generated text from 'Discover Oak Glen' app feedback***\n");

        for (int i = 0; i < mQuestionsHolder.getChildCount(); i++) {
            View child = mQuestionsHolder.getChildAt(i);
            int id = child.getId();
            if (id == View.NO_ID || child.getVisibility() != View.VISIBLE || !(child instanceof RelativeLayout)) continue;

            RelativeLayout questionAnswerView = (RelativeLayout) child;
            String questionIdString = getResources().getResourceEntryName(id);
            String answerString = getResultString(questionAnswerView, questionIdString);
            if (answerString.length() > 0) {
                resultBuilder.append("\n\n[<")
                        .append(questionIdString)
                        .append(">]\n\n[Question Text]\n")
                        .append(getQuestionString(questionAnswerView))
                        .append("\n\n[User's Answer]\n")
                        .append(answerString)
                        .append('\n');
            }

        }

        return resultBuilder.toString();
    }

    private void sendEmail(String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, EMAIL_ADDRESSES);
        intent.putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        ComponentName componentName = intent.resolveActivity(getPackageManager());
        if (componentName != null && !componentName.getPackageName().equals("com.android.fallback")) {
            startActivity(intent);
        } else {
            Snackbar.make(mQuestionsHolder, "Unable to send: Either no email app is installed, or no account is set up.", Snackbar.LENGTH_LONG)
                    .show();
        }

    }

    private String getResultString(RelativeLayout holderView, String mainIdString) {
        String answerIdString = "answer" + Character.toUpperCase(mainIdString.charAt(0)) + mainIdString.substring(1);
        View answerView = holderView.findViewById(getResources().getIdentifier(answerIdString, "id", getPackageName()));


        if (answerView instanceof EditText) {
            return ((EditText) answerView).getText().toString();
        } else if (answerView instanceof Spinner) {
            return ((Spinner) answerView).getSelectedItem().toString();
        } else if (answerView instanceof LinearLayout) {
//            LinearLayout linearLayout = (LinearLayout) answerView;
            if (myPref.getInt("feedback_apple",0) == 0) {
                return "";
            } else {
                return String.format(Locale.US, "%d out of %d", myPref.getInt("feedback_apple",0), 5);
            }
        } else {
            return "";
        }
    }

    private String getQuestionString(RelativeLayout view) {
        TextView questionView = (TextView) view.findViewById(R.id.question);
        return questionView.getText().toString();
    }

    @Override
    public void onClick(View v) {
        int clickCount = 0;

//        for(int i=0; i<5; i++){
//            appleIVs[i].setImageResource(R.drawable.bw_apple);
//        }

        switch (v.getId()){
            case R.id.apple_1:
                clickCount += 1;
                myPref.edit().putInt("feedback_apple",1).apply();
                break;
            case R.id.apple_2:
                clickCount += 2;
                myPref.edit().putInt("feedback_apple",2).apply();
                break;
            case R.id.apple_3:
                clickCount += 3;
                myPref.edit().putInt("feedback_apple",3).apply();
                break;
            case R.id.apple_4:
                clickCount += 4;
                myPref.edit().putInt("feedback_apple",4).apply();
                break;
            case R.id.apple_5:
                clickCount += 5;
                myPref.edit().putInt("feedback_apple",5).apply();
                break;
        }

        for(int j=clickCount; j<5; j++){
            appleIVs[j].setImageResource(R.drawable.bw_apple);
        }

        for(int i=0; i<clickCount; i++){
            appleIVs[i].setImageResource(R.drawable.colored_apple);
        }

    }
}
