package com.erichamion.freelance.oakglen.bm;

import android.support.annotation.NonNull;

/**
 * Base class for ReusableBitmap and for any placeholders that can be
 * compared to ReusableBitmaps.
 *
 * Created by Eric Ray on 7/14/2016.
 */
abstract class ReusableBitmapAbstract {
    @Override
    public final boolean equals(Object o) {
        if (o == null || !(o instanceof ReusableBitmapAbstract)) return false;

        ReusableBitmapAbstract other = (ReusableBitmapAbstract) o;
        return this.getSrc().equals(other.getSrc()) && this.getDisplayType() == other.getDisplayType();
    }

    @Override
    public final int hashCode() {
        int hash = 11;
        hash = hash * 41 + getSrc().hashCode();
        hash = hash * 41 + getDisplayType();
        return hash;
    }

    @NonNull
    abstract BitmapSource getSrc();

    @SuppressWarnings("WeakerAccess")
    public abstract int getDisplayType();

    @SuppressWarnings("unused")
    public abstract int getByteSize();
}
