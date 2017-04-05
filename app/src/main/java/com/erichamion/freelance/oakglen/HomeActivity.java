package com.erichamion.freelance.oakglen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;


public class HomeActivity extends MenuHandlerActivity {

    private RelativeLayout mRootContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Glide.with(this).load(R.drawable.home_screen_bg).into((ImageView) findViewById(R.id.iv_home_screen_bg));

        mRootContentView = (RelativeLayout) findViewById(R.id.rootContentLayout);

        (findViewById(R.id.iv_gallery)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this,MainActivity.class);
                startActivity(i);
            }
        });

        (findViewById(R.id.iv_help)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuStartAboutActivity(AboutActivity.ABOUT_APP);
            }
        });

        final SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, MODE_PRIVATE);

        if (!prefs.contains(Util.PREFKEY_EULA)) {
            enableMenu(false);
            final View eulaView = getLayoutInflater().inflate(R.layout.eula, mRootContentView, false);
            TextView tv_eula = (TextView) eulaView.findViewById(R.id.tv_eula);
            tv_eula.setMovementMethod(LinkMovementMethod.getInstance());
            View cancelView = eulaView.findViewById(R.id.cancel);
            cancelView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            View okView = eulaView.findViewById(R.id.ok);
            okView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mRootContentView.removeView(eulaView);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(Util.PREFKEY_EULA, true);
                    editor.apply();
                    enableMenu(true);

                    if (prefs.getBoolean(Util.PREFKEY_AUTOCONNECT, true)) {
                        Util.startWiFiService(HomeActivity.this);
                    }
                }
            });

            RelativeLayout.LayoutParams eulaParams =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootContentView.addView(eulaView, eulaParams);

        } else if (prefs.getBoolean(Util.PREFKEY_AUTOCONNECT, true)) {
            Util.startWiFiService(this);
        }

//        SpannableString spannableString = new SpannableString("Lorem     ");
//        Drawable d = getResources().getDrawable(R.drawable.map);
//        d.setBounds(0, 0, 50, 50);
//        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
////        ImageSpan span = new ImageSpan(this,R.drawable.map);
//        spannableString.setSpan(span, 5,  5+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
//        ((TextView)findViewById(R.id.tv_test)).setText(spannableString);

    }
}
