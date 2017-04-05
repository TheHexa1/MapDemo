package com.erichamion.freelance.oakglen.bm;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.erichamion.freelance.oakglen.R;

/**
 * Holds the source for a single bitmap, regardless of whether the source is an
 * integer resource ID or a string filesystem path.
 *
 * Created by Eric Ray on 7/14/2016.
 */
class BitmapSource implements Comparable<BitmapSource> {
    public final int sourceType;
    public final int resId;
    public final String path;

    public BitmapSource(@DrawableRes int resId) {
        sourceType = R.id.bitmapSourceResId;
        this.resId = resId;
        path = null;
    }

    public BitmapSource(@NonNull String path) {
        sourceType = R.id.bitmapSourceFile;
        resId = 0;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BitmapSource)) return false;
        BitmapSource other = (BitmapSource) o;
        return this.sourceType == other.sourceType &&
                this.resId == other.resId &&
                (this.path == null ? other.path == null : this.path.equals(other.path));
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 27 + sourceType;
        hash = hash * 27 + resId;
        hash = hash * 27 + (path == null ? 0 : path.hashCode());
        return hash;
    }

    @Override
    public int compareTo(@NonNull BitmapSource another) {
        // Ordering is arbitrary, but must be consistent.
        // Sort by different source types first, resource ids before file paths.
        // Break ties by whichever field is meaningful, lowest resource id or
        // lexicographically lowest path string first.
        // If the source types are the same, then the non-meaningful field will always
        // be the same (0 or null).

        if (this.sourceType == another.sourceType) {
            if (this.sourceType == R.id.bitmapSourceResId) {
                return this.resId - another.resId;
            } else {
                assert this.path != null && another.path != null;
                return this.path.compareTo(another.path);
            }
        } else if (this.sourceType == R.id.bitmapSourceResId) {
            return -1;
        } else {
            return 1;
        }
    }
}
