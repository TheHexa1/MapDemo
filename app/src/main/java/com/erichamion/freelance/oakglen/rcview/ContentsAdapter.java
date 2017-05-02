package com.erichamion.freelance.oakglen.rcview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erichamion.freelance.oakglen.BookContents;
import com.erichamion.freelance.oakglen.ChapterActivity;
import com.erichamion.freelance.oakglen.MainActivity;
import com.erichamion.freelance.oakglen.R;

import java.lang.ref.WeakReference;

/**
 * RecyclerView.Adapter for showing table of contents, with a link to each chapter.
 * Optionally includes a title card as the first item.
 *
 * Created by Eric Ray on 7/15/2016.
 */
public class ContentsAdapter extends RecyclerView.Adapter<BaseViewHolder> {

    private BookContents mContents;
    private final boolean mHasTitle;
    private final int mThumbWidth;
    private final int mThumbHeight;
    private int mNumLocations;
    private int mVisitedLocations;
    private boolean mCanClick = false;
    private final WeakReference<MainActivity> activityRef;

    public ContentsAdapter(MainActivity activity, boolean includeTitle, int thumbWidth, int thumbHeight) {
        activityRef = new WeakReference<>(activity);
        mHasTitle = includeTitle;
        mThumbWidth = thumbWidth;
        mThumbHeight = thumbHeight;
    }

    public void setContents(@Nullable BookContents contents) {
        int startIndex = mHasTitle ? 1 : 0;
        if (mHasTitle) {
            MainActivity activity = getActivity();
            if (contents != null && activity != null) {
//                Pair<Integer, Integer> totalAndVisitedLocations = getActivity().countVisitedLocations(contents);
//                mNumLocations = totalAndVisitedLocations.first;
//                mVisitedLocations = totalAndVisitedLocations.second;
            }
            notifyItemChanged(0);
        }
        if (mContents != null) {
            int oldCount = mContents.getNumChapters();
            mContents = null;
            notifyItemRangeRemoved(startIndex, oldCount);
        }
        mContents = contents;
        if (contents != null && contents.getNumChapters() > 0) {
            notifyItemRangeInserted(startIndex, mContents.getNumChapters());
        }


    }

    public void enableClicks() {
        mCanClick = true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canClick() {
        return mCanClick;
    }

    @Nullable
    private MainActivity getActivity() {
        return activityRef.get();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        recyclerView.addItemDecoration(new DividerDecoration(recyclerView.getContext()));
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case R.id.viewTypeTitle:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_title, parent, false);
                TitleViewHolder titleViewHolder = new TitleViewHolder(itemView);
                titleViewHolder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!canClick() || mContents == null || getActivity() == null) {
                            return;
                        }
                        getActivity().launchMap();
                    }
                });
                return titleViewHolder;

            case R.id.viewTypeChapter:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_chapter, parent, false);
                final ChapterViewHolder chapterViewHolder = new ChapterViewHolder(itemView);
                chapterViewHolder.setOnClickListener(new View.OnClickListener() {
                    private final ChapterViewHolder viewHolder = chapterViewHolder;

                    @Override
                    public void onClick(View v) {
                        if (!canClick() || getActivity() == null) return;

                        Intent intent = new Intent(v.getContext(), ChapterActivity.class);
                        intent.putExtra(ChapterActivity.CHAPTERINDEX_KEY, getRealPosition(viewHolder.getLayoutPosition()));
                        intent.putExtra(ChapterActivity.CHAPTERCOUNT_KEY, mContents.getNumChapters());
                        getActivity().addBackgroundExtras(intent);

                        getActivity().startActivity(intent);
                    }
                });
                return chapterViewHolder;

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getItemCount() {
        int realCount = (mContents == null) ? 0 : mContents.getNumChapters();
        return realCount + (mHasTitle ? 1 : 0);
    }

    @Override
    public long getItemId(int position) {
        return getRealPosition(position);
    }

    @Override
    public int getItemViewType(int position) {
        return (getRealPosition(position) < 0) ? R.id.viewTypeTitle : R.id.viewTypeChapter;
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case R.id.viewTypeTitle:
                final TitleViewHolder titleHolder = (TitleViewHolder) holder;
                if (mContents != null) {
                    titleHolder.setCoords(mContents.getLatitude(), mContents.getLongitude());
//                    titleHolder.setNumLocations(mNumLocations); //////////////////////*****
//                    titleHolder.setNumVisitedLocations(mVisitedLocations);
                }
                break;

            case R.id.viewTypeChapter:
                ChapterViewHolder chapterHolder = (ChapterViewHolder) holder;
                final int realPosition = getRealPosition(position);
                BookContents.Chapter chapter = mContents.getChapter(realPosition);
                BookContents.Page firstPage = chapter.getPage(0);
                chapterHolder.setImage(firstPage.imageId, firstPage.backgroundId, firstPage.scaleType, mThumbWidth, mThumbHeight);
//                chapterHolder.setFormattedChapterIndex(realPosition);
                chapterHolder.setChapterTitle(chapter.getTitle());
                break;

            default:
                throw new IllegalArgumentException();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        holder.clear();

        super.onViewRecycled(holder);
    }

    private int getRealPosition(int position) {
        return position - (mHasTitle ? 1 : 0);
    }

    private static class DividerDecoration extends RecyclerView.ItemDecoration {
        private final GradientDrawable mDrawable;
        private final int mHeight;

        public DividerDecoration(Context context) {
            mDrawable = (GradientDrawable) ResourcesCompat.getDrawable(context.getResources(),
                    R.drawable.divider_gradient, context.getTheme());
            mHeight = context.getResources().getDimensionPixelSize(R.dimen.divider_height);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
                // Don't draw below the bottom item
                return;
            }

            outRect.bottom = mHeight;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int leftBound = parent.getPaddingLeft();
            int rightBound = parent.getWidth() - parent.getPaddingRight();

            int numChildren = parent.getChildCount();
            for (int i = 0; i < numChildren - 1; i++) {
                View child = parent.getChildAt(i);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                int topBound = child.getBottom() + params.bottomMargin;
                int bottomBound = topBound + mHeight;
                mDrawable.setBounds(leftBound, topBound, rightBound, bottomBound);
                mDrawable.draw(c);
            }
        }
    }
}
