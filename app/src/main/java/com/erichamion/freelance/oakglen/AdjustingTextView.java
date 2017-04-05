package com.erichamion.freelance.oakglen;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.Locale;

/**
 * TODO: document your custom view class.
 */
public class AdjustingTextView extends TextView {
    @SuppressWarnings("FieldCanBeLocal")
    private static final String TAG = "AdjustingTextView";

    private float mTargetWidthFraction;
    private TextSizeAdjuster mAdjuster;

    public AdjustingTextView(Context context) {
        super(context);
        if (!isInEditMode()) {
            init(null, 0);
        }
    }

    public AdjustingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            init(attrs, 0);
        }
    }

    public AdjustingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode()) {
            init(attrs, defStyle);
        }
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.AdjustingTextView, defStyle, 0);

        mTargetWidthFraction = a.getFloat(R.styleable.AdjustingTextView_targetWidthPercent, -1.0f) / 100;
        if (mTargetWidthFraction <= 0) {
            throw new IllegalArgumentException("AdjustingTextView must have attribute targetWidthPercent that is strictly greater than 0");
        }

        boolean useFastCalculation = a.getBoolean(R.styleable.AdjustingTextView_useFastCalculation, false);
        if (useFastCalculation) {
            mAdjuster = new FastTextAdjuster(this);
        } else {
            mAdjuster = new SlowTextAdjuster();
        }

        a.recycle();

    }

    private void adjustTextSize(boolean isOnlyTextAppearanceChanging) {
        adjustTextSize(getWidth() * mTargetWidthFraction, isOnlyTextAppearanceChanging);
    }

    private void adjustTextSize(float targetWidth, boolean isOnlyTextAppearanceChanging) {
        if (mAdjuster == null) return;
        mAdjuster.adjustTextSize(this, targetWidth, isOnlyTextAppearanceChanging);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            adjustTextSize(MeasureSpec.getSize(widthMeasureSpec) * mTargetWidthFraction, false);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        adjustTextSize(false);
    }

    @Override
    public void setAllCaps(boolean allCaps) {
        super.setAllCaps(allCaps);

        adjustTextSize(true);
    }

    @Override
    public void setFontFeatureSettings(String fontFeatureSettings) {
        super.setFontFeatureSettings(fontFeatureSettings);

        adjustTextSize(true);
    }

    @Override
    public void setLetterSpacing(float letterSpacing) {
        super.setLetterSpacing(letterSpacing);

        adjustTextSize(true);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);

        adjustTextSize(true);
    }

    @Override
    public void setTextAppearance(int resId) {
        super.setTextAppearance(resId);

        adjustTextSize(true);
    }

    @Override
    public void setTextLocale(@NonNull Locale locale) {
        super.setTextLocale(locale);

        adjustTextSize(true);
    }

    @Override
    public void setTextScaleX(float size) {
        super.setTextScaleX(size);

        adjustTextSize(true);
    }

    @Override
    public void setTypeface(Typeface tf) {
        super.setTypeface(tf);

        adjustTextSize(false);
    }

    @Override
    public void setTypeface(Typeface tf, int style) {
        super.setTypeface(tf, style);

        adjustTextSize(false);
    }

    @Override
    public void setEms(int ems) {
        // If the text size is calculated from the view size, then
        // the view size can't be calculated from the text size.
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMaxEms(int maxems) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMinEms(int minems) {
        throw new UnsupportedOperationException();
    }

    private interface TextSizeAdjuster {
        void adjustTextSize(@NonNull TextView textView, float targetWidth, boolean isOnlyTextAppearanceChanging);
    }

    private static class FastTextAdjuster implements TextSizeAdjuster {
        private final float mReferenceTextSize;
        private final double mAverageCharReferenceSize;

        public FastTextAdjuster(@NonNull TextView textView) {
            mReferenceTextSize = textView.getTextSize();

            String testText = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,./<>?;':[]{}`1234567890-=\\~!@#$%^&*()_+| ";
            TextPaint paint = textView.getPaint();
            mAverageCharReferenceSize = (double) paint.measureText(testText) / testText.length();
        }

        @Override
        public void adjustTextSize(@NonNull TextView textView, float targetWidth, boolean isOnlyTextAppearanceChanging) {
            // This adjuster has no way of dealing with appearance changes
            if (isOnlyTextAppearanceChanging) {
                Log.w(TAG, "Text appearance changed on AdjustingTextView with fast adjustment. Size may not be calculated correctly.");
                return;
            }

            if (targetWidth < 0.0001f) return;

            int textLength = textView.length();
            if (textLength == 0) return;

            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (targetWidth * mReferenceTextSize / textLength / mAverageCharReferenceSize));
        }
    }

    private static class SlowTextAdjuster implements TextSizeAdjuster{
        @Override
        public void adjustTextSize(@NonNull TextView textView, float targetWidth, boolean isOnlyTextAppearanceChanging) {
            // Ignore isOnlyTextAppearanceChanging. It doesn't matter.

            if (targetWidth < 0.0001f) return;

            String text = textView.getText().toString();
            if (text.length() == 0) return;

            TextPaint paint = textView.getPaint();
            float currentTextSize = textView.getTextSize();
            float currentWidth = paint.measureText(text);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentTextSize * targetWidth / currentWidth);

        }
    }
}
