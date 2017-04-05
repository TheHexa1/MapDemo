package com.erichamion.freelance.oakglen.rcview;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erichamion.freelance.oakglen.BookContents;
import com.erichamion.freelance.oakglen.ChapterActivity;
import com.erichamion.freelance.oakglen.R;

import java.lang.ref.WeakReference;

/**
 * RecyclerView.Adapter for showing the (image/latitude/longitude) pages
 * within a chapter, with a link to launch a map for each page.
 *
 * Created by Eric Ray on 7/15/2016.
 */
public class PageListAdapter extends RecyclerView.Adapter<PageViewHolder> {

    private BookContents.Chapter mChapter;
    private String[] mVisitedPrefkeys = {};
    private boolean[] mVisitedPages = {};
    //private int imageWidth = 0;
    //private int imageHeight = 0;
    private final WeakReference<ChapterActivity> mActivityRef;

    public PageListAdapter(ChapterActivity activity) {
        setHasStableIds(true);
        mActivityRef = new WeakReference<>(activity);
    }

    /**
     *
     * @param chapter Can be null to empty out the adapter
     * @param visitedPrefkeys Must have length equal to chapter.getNumPages(). Must not be null. If
     *                        chapter is null, visitedPrefkeys should be a 0-length array.
     * @param visitedPages Must have length equal to chapter.getNumPages(). Must not be null. If
     *                        chapter is null, visitedPages should be a 0-length array.
     */
    public void setChapter(@Nullable BookContents.Chapter chapter, @NonNull String[] visitedPrefkeys, @NonNull boolean[] visitedPages) {
        mVisitedPrefkeys = visitedPrefkeys;
        mVisitedPages = visitedPages;

        if (mChapter != null) {
            int oldCount = mChapter.getNumPages();
            mChapter = null;
            notifyItemRangeRemoved(0, oldCount);
        }
        mChapter = chapter;
        if (chapter != null) {
            notifyItemRangeInserted(0, mChapter.getNumPages());
        }
    }

    @Override
    public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_page, parent, false);
        final PageViewHolder viewHolder = new PageViewHolder(itemView);

        viewHolder.setOnClickListener(new View.OnClickListener() {
            private final PageViewHolder pageViewHolder = viewHolder;

            @Override
            public void onClick(View v) {
                if (getActivity() == null) return;
                getActivity().launchMap(pageViewHolder.getLatitude(), pageViewHolder.getLongitude(), pageViewHolder.getPrefkey(), pageViewHolder.getImageResId(), mChapter.getTitle());
            }
        });

        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return (mChapter == null) ? 0 : mChapter.getNumPages();
    }

    @Override
    public long getItemId(int position) {
        return mChapter.getPage(position).imageId;
    }

    @Override
    public void onBindViewHolder(PageViewHolder holder, int position) {
        BookContents.Page page = mChapter.getPage(position);
        holder.setImage(page.imageId, page.backgroundId, page.scaleType, 0, 0);
        holder.setCoords(page.latitude, page.longitude);
        holder.setPrefkey(mVisitedPrefkeys[position]);
        holder.setIsVisited(mVisitedPages[position]);
    }

    @Override
    public void onViewRecycled(PageViewHolder holder) {
        holder.clear();

        super.onViewRecycled(holder);
    }


    @Nullable
    ChapterActivity getActivity() {
        return mActivityRef.get();
    }


}
