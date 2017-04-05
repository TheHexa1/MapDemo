package com.erichamion.freelance.oakglen;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
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
//                contentTextView.setText(R.string.about_app_text);

//                String temp_container =
                contentTextView.setText(getString(R.string.about_app_text_0));

                contentTextView.append(getSpannableString(getString(R.string.about_app_text_1), R.drawable.map));
                contentTextView.append(" ");
                contentTextView.append(getSpannableString(getString(R.string.about_app_text_2), R.drawable.gallery));
                contentTextView.append(getSpannableString(getString(R.string.about_app_text_3), R.drawable.map));
                contentTextView.append(" ");
                contentTextView.append(getString(R.string.about_app_text_4));
                contentTextView.append(getSpannableString(getString(R.string.about_app_text_5), R.drawable.map));
                contentTextView.append(" ");
                contentTextView.append(getString(R.string.about_app_text_6)+getString(R.string.about_app_text_7));

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

    public String getBoldString(String string, int start, int end){

//        return getString(R.string.about_app_text_0)
//                + getSpannableString(getString(R.string.about_app_text_1), R.drawable.map)
//                + getSpannableString(getString(R.string.about_app_text_2), R.drawable.gallery)
//                + getSpannableString(getString(R.string.about_app_text_3), R.drawable.map)
//                + getString(R.string.about_app_text_4)
//                + getSpannableString(getString(R.string.about_app_text_5), R.drawable.map)
//                + getString(R.string.about_app_text_6)
//                + getString(R.string.about_app_text_7);

        final SpannableStringBuilder str = new SpannableStringBuilder(string);
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
                , start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return str.toString();
    }

    public SpannableString getSpannableString(String val, int drawable){
        SpannableString spannableString = new SpannableString(val);
        Drawable d = getResources().getDrawable(drawable);
        d.setBounds(0, 0, 50, 50);
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
//        ImageSpan span = new ImageSpan(this,R.drawable.map);
        spannableString.setSpan(span, val.indexOf("@"), val.indexOf("@")+3, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

//    SpannableString spannableString = new SpannableString("Lorem");
//        Drawable d = getResources().getDrawable(R.drawable.map);
//        d.setBounds(0, 0, 50, 50);
//        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
////        ImageSpan span = new ImageSpan(this,R.drawable.map);
//        spannableString.setSpan(span, 5,  5+10, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
//        ((TextView)findViewById(R.id.tv_test)).setText(spannableString);
}
