package com.erichamion.freelance.oakglen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.erichamion.freelance.oakglen.rcview.PageListAdapter;

import java.util.List;
import java.util.Locale;

public class ChapterActivity extends MenuHandlerActivity implements BookContents.OnContentsChapterAvailableListener {
    public static final String CHAPTERINDEX_KEY = "chapterIndex";
    public static final String CHAPTERCOUNT_KEY = "chapterCount";

    private int mChapterIndex;
    private int mNumChapters;

    private PageListAdapter mPageHolderViewAdapter;
    private ImageView mNextIcon;
    private ImageView mPrevIcon;

    private Intent mMapIntent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        assert toolbar != null;
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_up_white_24dp);

        Intent startIntent = getIntent();
        mChapterIndex = startIntent.getIntExtra(CHAPTERINDEX_KEY, -1);
        mNumChapters = startIntent.getIntExtra(CHAPTERCOUNT_KEY, 0);
        View rootView = findViewById(R.id.rootContentLayout);
        setMainBackground(rootView, startIntent.getExtras());

        RecyclerView pageHolderView = (RecyclerView) findViewById(R.id.pageHolder);
        assert pageHolderView != null;
        pageHolderView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return super.getExtraLayoutSpace(state) / 3;
            }
        });
        mPageHolderViewAdapter = new PageListAdapter(this);
        pageHolderView.setAdapter(mPageHolderViewAdapter);


        mPrevIcon = (ImageView) findViewById(R.id.previousIcon);
        mNextIcon = (ImageView) findViewById(R.id.nextIcon);

        mPrevIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trySwitchChapter(-1);
            }
        });
        mNextIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trySwitchChapter(1);
            }
        });


        initSwipeListener(pageHolderView);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        mChapterIndex = intent.getIntExtra(CHAPTERINDEX_KEY, -1);
        mNumChapters = intent.getIntExtra(CHAPTERCOUNT_KEY, 0);

        startSetup();
    }

    @Override
    protected void onStart() {
        super.onStart();

        startSetup();
    }

    @Override
    protected void onStop() {
        mPageHolderViewAdapter.setChapter(null, null, null);
        super.onStop();
    }

    private void initSwipeListener(RecyclerView recyclerView) {

        final GestureDetectorCompat flingDetector =
                new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (Math.abs(velocityX) / 3 > Math.abs(velocityY) * 2) {
                            int increment = (velocityX < 0) ? 1 : -1;
                            trySwitchChapter(increment);
                            return true;
                        }

                        return super.onFling(e1, e2, velocityX, velocityY);
                    }
                });

        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return flingDetector.onTouchEvent(e);
            }
        });
    }

    private void startSetup() {
        setIconVisibility();
        BookContents.requestChapter(mChapterIndex, this, this);
    }

    private void trySwitchChapter(int increment) {
        if (increment == 0) return;

        int targetChapter = mChapterIndex + increment;
        if (targetChapter >= 0 && targetChapter < mNumChapters) {
            Intent intent = new Intent(this, ChapterActivity.class);
            intent.putExtra(CHAPTERINDEX_KEY, targetChapter);
            intent.putExtra(CHAPTERCOUNT_KEY, mNumChapters);
            startActivity(intent);
            overridePendingTransition(increment > 0 ? R.anim.next_chapter_in : R.anim.prev_chapter_in,
                    increment > 0 ? R.anim.next_chapter_out : R.anim.prev_chapter_out);
        }
    }

    @Override
    public void onContentsChapterAvailable(final BookContents.Chapter chapter) {
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(String.format(Locale.US, "Chapter %d", mChapterIndex + 1));

        TextView chapterTitleTextView = (TextView) findViewById(R.id.chapterTitleText);
        String chapterTitle = chapter.getTitle();
        assert chapterTitleTextView != null;
        chapterTitleTextView.setText(chapterTitle);

        int numPages = chapter.getNumPages();
        String[] visitedPrefkeys = new String[numPages];
        boolean[] visitedPages = new boolean[numPages];
        SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, MODE_PRIVATE);
        for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
            String prefkey = Util.getVisitedPrefkey(mChapterIndex, pageIndex);
            visitedPrefkeys[pageIndex] = prefkey;
            visitedPages[pageIndex] = prefs.getBoolean(prefkey, false);

        }

        mPageHolderViewAdapter.setChapter(chapter, visitedPrefkeys, visitedPages);
    }

    private void setIconVisibility() {
        mPrevIcon.setVisibility((mChapterIndex > 0) ? View.VISIBLE : View.INVISIBLE);
        mPrevIcon.setClickable(mChapterIndex > 0);
        mNextIcon.setVisibility((mChapterIndex < mNumChapters - 1) ? View.VISIBLE : View.INVISIBLE);
        mNextIcon.setClickable(mChapterIndex < mNumChapters - 1);

    }

    @Override
    public boolean onRequestPermissionsResultEx(int requestCode, @NonNull List<String> deniedPermissions) {
        if (requestCode == MapActivity.MAP_PERMS_REQUEST && mMapIntent != null) {
            if (deniedPermissions.isEmpty()) {
                startActivity(mMapIntent);
                mMapIntent = null;
            }

            return true;
        }

        return false;
    }

    public void launchMap(Double latitude, Double longitude, String visitedPrefkey, @DrawableRes int thumbResId, String title) {
        // If permissions are already granted, map will launch.
        // Otherwise, use the stored mMapIntent to launch from the onRequestPermissionsResult
        // callback.
        mMapIntent = MapActivity.launchMap(this, latitude, longitude, visitedPrefkey, thumbResId, title);
    }


}