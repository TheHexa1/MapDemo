package com.erichamion.freelance.oakglen;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

public class FeedbackActivity extends MenuHandlerActivity {private static final int[] DEPENDENT_QUESTION_IDS = {R.id.visitRating, R.id.relevance};
    private static final String[] EMAIL_ADDRESSES = {"JeffMBaxter@gmail.com"};
    private static final String EMAIL_SUBJECT = "Oak Glen app feedback";
    private final View[] mDependentQuestionViews = new View[DEPENDENT_QUESTION_IDS.length];
    private LinearLayout mQuestionsHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View rootContentView = findViewById(R.id.rootContentView);
        setMainBackground(rootContentView, getIntent().getExtras());

        RatingBar ratingBar = (RatingBar) findViewById(R.id.answerOverallRating);
        assert ratingBar != null;
        Util.setRatingBarToAccentColor(ratingBar, getTheme());

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
        } else if (answerView instanceof RatingBar) {
            RatingBar bar = (RatingBar) answerView;
            if (bar.getRating() < 0.1f) {
                return "";
            } else {
                return String.format(Locale.US, "%.1f out of %d", bar.getRating(), bar.getNumStars());
            }
        } else {
            return "";
        }
    }

    private String getQuestionString(RelativeLayout view) {
        TextView questionView = (TextView) view.findViewById(R.id.question);
        return questionView.getText().toString();
    }



}
