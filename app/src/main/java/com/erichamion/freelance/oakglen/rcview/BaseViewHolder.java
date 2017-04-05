package com.erichamion.freelance.oakglen.rcview;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.erichamion.freelance.oakglen.Util;
import com.erichamion.freelance.oakglen.bm.BitmapManager;

/**
 * Base functionality for a RecyclerView.ViewHolder. Implementing classes will typically,
 * but not necessarily, support views that contain two TextViews and an ImageView, plus
 * optionally other views.
 *
 * Created by Eric Ray on 7/14/2016.
 */
public abstract class BaseViewHolder extends RecyclerView.ViewHolder implements BitmapManager.OnImageLoadCompleteListener {
    protected final ImageView imageView;
    protected final TextView textView1;
    protected final TextView textView2;

    protected final int imageViewType;

    private BitmapManager.ImageLoaderHandle mLoaderHandle;

    public BaseViewHolder(View itemView) {
        super(itemView);
        imageView = findImageView(itemView);
        textView1 = findFirstTextView(itemView);
        textView2 = findSecondTextView(itemView);
        imageViewType = getImageViewType();
    }

    private static void resizeGradientOrShape(Drawable drawable, int width, int height) {
        drawable.mutate();
        if (drawable instanceof ShapeDrawable) {
            ((ShapeDrawable) drawable).setIntrinsicWidth(width);
            ((ShapeDrawable) drawable).setIntrinsicHeight(height);
        } else if (drawable instanceof GradientDrawable) {
            ((GradientDrawable) drawable).setSize(width, height);
        } else {
            // Throw a ClassCastException
            //noinspection ConstantConditions,UnusedAssignment
            @SuppressWarnings("unused")
            ShapeDrawable myVariable = (ShapeDrawable) drawable;
        }
    }

    public final void setFirstText(String text) {
        if (textView1 == null) throw new AssertionError();
        textView1.setText(text);
    }

    public final void setSecondText(String text) {
        if (textView2 == null) throw new AssertionError();
        textView2.setText(text);
    }

    public final void clear() {
        if (mLoaderHandle != null) {
            mLoaderHandle.cancel();
        }
        if (imageView != null) {
            BitmapManager.clearImageViewForegroundAndBackground(imageView);
            Util.setViewBackgroundCompat(imageView, null);
        }
        if (textView1 != null) textView1.setText("");
        if (textView2 != null) textView2.setText("");

        onClear();
    }

    public final void setImage(@DrawableRes int resId, @DrawableRes int backgroundResId, ImageView.ScaleType scaleType, int width, int height) {
        if (imageView == null) throw new AssertionError();

        if (mLoaderHandle != null) {
            mLoaderHandle.cancel();
            mLoaderHandle = null;
        }

        setImageInternal(resId, backgroundResId, scaleType, width, height);


    }

    protected void setImageInternal(@SuppressWarnings("unused") @DrawableRes int resId, @DrawableRes int backgroundResId, ImageView.ScaleType scaleType, int width, int height) {
        if (imageView == null) throw new AssertionError();

        imageView.setScaleType(scaleType);

        if (backgroundResId != 0) {
            Drawable backgroundDrawable = ResourcesCompat.getDrawable(imageView.getResources(), backgroundResId, null);
            resizeGradientOrShape(backgroundDrawable, width, height);
            Util.setViewBackgroundCompat(imageView, backgroundDrawable);
        } else {
            Util.setViewBackgroundCompat(imageView, null);
        }

        if (resId == 0) {
            BitmapManager.clearImageViewForegroundAndBackground(imageView);
        } else {
            mLoaderHandle = BitmapManager.setImageViewBitmap(imageView, resId, imageViewType, width, height, this);
        }
    }

    public final void setOnClickListener(@Nullable View.OnClickListener listener) {
        onSetOnClickListener(listener);
    }

    protected void onSetOnClickListener(@Nullable View.OnClickListener listener) {
        if (imageView != null) imageView.setOnClickListener(listener);
        if (textView1 != null) textView1.setOnClickListener(listener);
        if (textView2 != null) textView2.setOnClickListener(listener);
    }

    @Override
    public final void onImageLoadComplete() {
        mLoaderHandle = null;
    }

    protected abstract ImageView findImageView(View parent);

    protected abstract TextView findFirstTextView(View parent);

    protected abstract TextView findSecondTextView(View parent);

    @IdRes
    protected abstract int getImageViewType();

    protected abstract void onClear();
}
