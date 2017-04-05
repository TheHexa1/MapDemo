package com.erichamion.freelance.oakglen;

import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;

public class AboutActivity extends MenuHandlerActivity {
    public static final String ABOUT_TYPE_KEY = "aboutType";
    public static final int ABOUT_APP = 1;
    public static final int ABOUT_TOWN = 2;

    private ScrollView mContentScroll;
    private WebView mSkobblerAttrib;

    private final View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mContentScroll.setVisibility(mContentScroll.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            mSkobblerAttrib.setVisibility(mSkobblerAttrib.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.about_short);
        assert toolbar != null;
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_up_white_24dp);

        int aboutType = ABOUT_APP;
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            aboutType = extras.getInt(ABOUT_TYPE_KEY, aboutType);

            // Maybe the background should be set on mContentScroll instead?
            View rootContentView = findViewById(R.id.rootContentView);
            setMainBackground(rootContentView, extras);
        }

        TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
        TextView contentTextView = (TextView) findViewById(R.id.contentTextView);
        assert contentTextView != null;
        mContentScroll = (ScrollView) findViewById(R.id.contentScroll);
        switch (aboutType) {
            case ABOUT_APP:
                assert titleTextView != null;
                titleTextView.setText(R.string.about_app);
                contentTextView.setText(R.string.about_app_text);

                CardView legalCard = (CardView) findViewById(R.id.legalCard);
                assert legalCard != null;
                legalCard.setOnClickListener(mListener);
                legalCard.setVisibility(View.VISIBLE);

                mSkobblerAttrib = (WebView) findViewById(R.id.skobblerAttrib);
                mSkobblerAttrib.loadUrl(getResources().getString(R.string.skobbler_attrib_url));

                break;

            case ABOUT_TOWN:
                assert titleTextView != null;
                titleTextView.setText(R.string.about_town);
                contentTextView.setText(R.string.about_town_text);
                break;

            default:
                finish();
        }



    }
}
