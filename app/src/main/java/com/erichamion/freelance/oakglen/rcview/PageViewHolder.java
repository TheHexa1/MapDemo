package com.erichamion.freelance.oakglen.rcview;

import android.support.annotation.DrawableRes;
import android.support.v4.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.erichamion.freelance.oakglen.R;
import com.erichamion.freelance.oakglen.Util;

/**
 * RecyclerView.ViewHolder to hold a screen-width image and latitude/longitude location.
 *
 * Created by Eric Ray on 7/14/2016.
 */
public class PageViewHolder extends BaseViewHolder {
    // Inherits:
    //    protected final ImageView imageView;
    //    protected final TextView textView1;
    //    protected final TextView textView2;
    //    protected ImageLoaderHandle mLoaderHandle;

    //textView1 is Latitude
    //textView2 is Longitude

    private ImageView visitedIndicator;
    private boolean mLoadingCancelled = false;
    private int mResId = 0;
    private int mBackgroundId = 0;
    private double mLatitude;
    private double mLongitude;
    private String mPrefKey;

    public PageViewHolder(View itemView) {
        super(itemView);
        visitedIndicator = (ImageView) itemView.findViewById(R.id.visitedIndicator);
        Util.setRatingBarToColoredImage(visitedIndicator);
    }

    @Override
    protected TextView findFirstTextView(View parent) {
        return (TextView) parent.findViewById(R.id.latText);
    }

    @Override
    protected TextView findSecondTextView(View parent) {
        return (TextView) parent.findViewById(R.id.longText);
    }

    @Override
    protected ImageView findImageView(View parent) {
        return (ImageView) parent.findViewById(R.id.imageView);
    }

    @Override
    protected int getImageViewType() {
        return Util.IMAGE_TYPE_FULL_WIDTH;
    }

    @Override
    public void setImageInternal(final int resId, final int backgroundResId, final ImageView.ScaleType scaleType, final int width, final int height) {
        mResId = resId;
        mBackgroundId = backgroundResId;
        mLoadingCancelled = false;
        imageView.setScaleType(scaleType);

        int effectiveWidth = imageView.getWidth();
        int effectiveHeight = imageView.getHeight();
        if (imageView.getWidth() <= 10 && width != 0) {
            imageView.getLayoutParams().width = width;
            effectiveWidth = width;
        }
        if (imageView.getHeight() <= 10 && height > 10) {
            imageView.getLayoutParams().height = height;
            effectiveHeight = height;
        }
        if (effectiveWidth != 0 && effectiveHeight <= 10) {
            effectiveHeight = effectiveWidth * 2 / 3;
            imageView.getLayoutParams().height = effectiveHeight;
        }

        if (effectiveWidth <= 10) {
            Util.setSingleUseLayoutChangeListener(imageView, new Util.LayoutChangeListener() {
                // Time is not guaranteed to be unique (and is not guaranteed
                // to have better than millisecond resolution). A random number
                // is likely, but not guaranteed, to be unique. So use both to
                // reduce the chances of collision.
                @Override
                public void onLayoutChange(View v) {
                    int newHeight = v.getWidth() * 2 / 3;
                    v.getLayoutParams().height = newHeight;
                    if (!mLoadingCancelled && resId == mResId && backgroundResId == mBackgroundId) {
                        PageViewHolder.super.setImageInternal(resId, backgroundResId, scaleType, v.getWidth(), newHeight);
                    }
                }
            });
        } else {
            super.setImageInternal(resId, backgroundResId, scaleType, effectiveWidth, effectiveHeight);
        }
    }

    public void setCoords(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        Pair<String, String> latLong = Util.getCoordinateStrings(latitude, longitude);
        setFirstText(latLong.first);
        setSecondText(latLong.second);
    }

    public void setPrefkey(String prefkey) {
        mPrefKey = prefkey;
    }

    public String getPrefkey() {
        return mPrefKey;
    }

    public void setIsVisited(boolean isVisited) {
//        visitedIndicator.setRating(isVisited ? 1.0f : 0.0f);
        if(isVisited){
            visitedIndicator.setImageResource(R.drawable.colored_apple);
        }
        else{
            visitedIndicator.setImageResource(R.drawable.bw_apple);
        }
    }

//    public boolean isVisited() {
//        return visitedIndicator.getRating() > 0.5;
//    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    @DrawableRes
    public int getImageResId() {
        return mResId;
    }

    public void onClear() {
        mLoadingCancelled = true;
    }


}
