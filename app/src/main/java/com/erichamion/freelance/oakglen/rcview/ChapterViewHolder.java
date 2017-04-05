package com.erichamion.freelance.oakglen.rcview;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.erichamion.freelance.oakglen.R;
import com.erichamion.freelance.oakglen.Util;

import java.util.Locale;

/**
 * RecyclerView.ViewHolder for holding Chapter summary views within a table of contents.
 *
 * Created by Eric Ray on 7/14/2016.
 */
public class ChapterViewHolder extends BaseViewHolder {
    // textView1 is Chapter Number
    // textView2 is Chapter Title
    private final TextView chapterNumberView;
    private final TextView chapterTitleView;

    public ChapterViewHolder(View itemView) {
        super(itemView);
        chapterNumberView = textView1;
        chapterTitleView = textView2;
    }

    @Override
    protected ImageView findImageView(View parent) {
        return (ImageView) itemView.findViewById(R.id.thumbnailImage);
    }

    @Override
    protected TextView findFirstTextView(View parent) {
        return (TextView) itemView.findViewById(R.id.chapterNumText);
    }

    @Override
    protected TextView findSecondTextView(View parent) {
        return (TextView) itemView.findViewById(R.id.chapterTitleText);
    }

    @Override
    protected int getImageViewType() {
        return Util.IMAGE_TYPE_THUMBNAIL;
    }

    public void onClear() {
        // Do nothing
    }

    public void setFormattedChapterIndex(int chapterIndex) {
        chapterNumberView.setText(String.format(Locale.US, "Chapter %d", chapterIndex + 1));
    }

    public void setChapterTitle(String title) {
        chapterTitleView.setText(title);
    }

}
