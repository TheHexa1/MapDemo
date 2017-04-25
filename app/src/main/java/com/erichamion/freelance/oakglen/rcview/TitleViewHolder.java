package com.erichamion.freelance.oakglen.rcview;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.erichamion.freelance.oakglen.AdjustingTextView;
import com.erichamion.freelance.oakglen.R;
import com.erichamion.freelance.oakglen.Util;

/**
 * Displays the title card within a RecyclerView.
 *
 * Created by Eric Ray on 7/14/2016.
 */
public class TitleViewHolder extends BaseViewHolder {
    private final AdjustingTextView subtitleView;
    private final AdjustingTextView titleView;
//    private final ImageView visitedLocationsIndicator;
    View itemView;


    //textView1 is latitude
    //textView2 is longitude

    public TitleViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        subtitleView = (AdjustingTextView) itemView.findViewById(R.id.subtitleText);
        titleView = (AdjustingTextView) itemView.findViewById(R.id.titleText);
//        visitedLocationsIndicator = (ImageView) itemView.findViewById(R.id.visitedLocationsIndicator);
//        Util.setRatingBarToColoredImage(visitedLocationsIndicator);
    }

    @Override
    @Nullable
    protected ImageView findImageView(View parent) {
        return null;
    }

    @Override
    @NonNull
    protected TextView findFirstTextView(View parent) {
        return (TextView) itemView.findViewById(R.id.latText);
    }

    @Override
    @NonNull
    protected TextView findSecondTextView(View parent) {
        return (TextView) itemView.findViewById(R.id.longText);
    }

    @Override
    protected int getImageViewType() {
        return 0;
    }

    @Override
    protected void onSetOnClickListener(@Nullable View.OnClickListener listener) {
        super.onSetOnClickListener(listener);

        subtitleView.setOnClickListener(listener);
        titleView.setOnClickListener(listener);

        itemView.setOnClickListener(listener);

    }

    public void setCoords(double latitude, double longitude) {
        Pair<String, String> latLong = Util.getCoordinateStrings(latitude, longitude);
        setFirstText(latLong.first);
        setSecondText(latLong.second);
    }

    public void setNumLocations(int numLocations) {
//        visitedLocationsIndicator.setNumStars(numLocations);
    }

    public void setNumVisitedLocations(int visitedCount) {
//        visitedLocationsIndicator.setRating(visitedLocations);
        int indicator_apples[] = {
                R.id.indicator_apple_1,R.id.indicator_apple_2,R.id.indicator_apple_3,
                R.id.indicator_apple_4,R.id.indicator_apple_5,R.id.indicator_apple_6,
                R.id.indicator_apple_7,R.id.indicator_apple_8,R.id.indicator_apple_9,
                R.id.indicator_apple_10,R.id.indicator_apple_11,R.id.indicator_apple_12,
                R.id.indicator_apple_13,R.id.indicator_apple_14,R.id.indicator_apple_15,
        };

        for(int i=0; i<visitedCount; i++){
            ((ImageView)itemView.findViewById(indicator_apples[i])).setImageResource(R.drawable.colored_apple);
        }
    }

    public void onClear() {
        // Do nothing
    }

    @SuppressWarnings("unused")
    public void setImage(@DrawableRes int resId, int width) {
        throw new UnsupportedOperationException();

    }


}
