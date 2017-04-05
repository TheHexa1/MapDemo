package com.erichamion.freelance.oakglen;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * The full contents of a picture-book, divided into global/cover information, chapters,
 * and pages (with each page holding an image resource ID and a latitude/longitude location)
 *
 * Created by Eric Ray on 5/31/16.
 */
public class BookContents {

    private static BookContents staticInstance;

    private static final int RESOURCE_ID = R.xml.contents;
    private static final int DEFAULT_GRAVITY = Gravity.CENTER;
    private static final ImageView.ScaleType DEFAULT_SCALETYPE = ImageView.ScaleType.CENTER_CROP;

    private final List<Chapter> mChapters = new ArrayList<>();
    private double mCoverLatitude;
    private double mCoverLongitude;
    private String mTitle;
    private int mBackgroundResId = 0;
    private int mBackgroundGravity = DEFAULT_GRAVITY;

    public static void requestContents(Context context, final OnContentsAvailableListener listener) {
        if (staticInstance == null) {
            ContentsReader reader = new ContentsReader(context.getResources(), new ContentsReader.OnContentsReadListener() {
                @Override
                public void onContentsRead() {
                    listener.onContentsAvailable(staticInstance);
                }
            });
            reader.execute();
        } else {
            listener.onContentsAvailable(staticInstance);
        }
    }

    public static void requestChapter(final int chapterIndex, Context context, final OnContentsChapterAvailableListener listener) {
        if (staticInstance == null) {
            ContentsReader reader = new ContentsReader(context.getResources(), new ContentsReader.OnContentsReadListener() {
                @Override
                public void onContentsRead() {
                    listener.onContentsChapterAvailable(staticInstance.getChapter(chapterIndex));
                }
            });
            reader.execute();
        } else {
            listener.onContentsChapterAvailable(staticInstance.getChapter(chapterIndex));
        }
    }

    @Nullable
    private static BookContents instantiate(Resources res) {
        XmlResourceParser parser = res.getXml(RESOURCE_ID);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            parser.next();
            parser.require(XmlPullParser.START_DOCUMENT, null, null);
            parser.nextTag();
            staticInstance = readBook(parser, res);
            parser.require(XmlPullParser.END_DOCUMENT, null, null);
        } catch (XmlPullParserException e) {
            Log.e(Util.TAG, "Error reading Table of Contents XML resource, may be invalid XML format.");
            staticInstance = null;
        } catch (IOException e) {
            String msg = "Could not process Table of Contents XML resource:\n";
            if (e.getMessage() != null) {
                msg += e.getMessage();
            }
            msg += "\n" + Log.getStackTraceString(e);
            Log.e(Util.TAG, msg);
            staticInstance = null;
        } finally {
            parser.close();
        }

        return staticInstance;
    }

    public double getLatitude() {
        return mCoverLatitude;
    }

    public double getLongitude() {
        return mCoverLongitude;
    }

    public int getNumChapters() {
        return mChapters.size();
    }

    public Chapter getChapter(int index) {
        return mChapters.get(index);
    }

    public String getTitle() {
        return mTitle;
    }

    public int getBackgroundResId() {
        return mBackgroundResId;
    }

    public int getBackgroundGravity() {
        return mBackgroundGravity;
    }

    private static BookContents readBook(XmlResourceParser parser, Resources res)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "book");

        BookContents result = new BookContents();

        result.mTitle = parser.getAttributeValue(null, "title");
        if (result.mTitle.charAt(0) == '@') {
            int titleResId = parser.getAttributeResourceValue(null, "title", 0);
            if (titleResId != 0) {
                result.mTitle = res.getString(titleResId);
            }
        }
        result.mCoverLatitude = Double.parseDouble(parser.getAttributeValue(null, "latitude"));
        result.mCoverLongitude = Double.parseDouble(parser.getAttributeValue(null, "longitude"));
        result.mBackgroundResId = parser.getAttributeResourceValue(null, "background", 0);
        result.mBackgroundGravity = readGravity(parser.getAttributeValue(null, "backgroundGravity"));

        parser.next();
        while (parser.getEventType() != XmlPullParser.END_TAG) {
            result.addChapter(Chapter.readXml(parser, res));
        }

        parser.require(XmlPullParser.END_TAG, null, "book");
        parser.next();

        return result;

    }

    private void addChapter(Chapter chapter) {
        mChapters.add(chapter);
    }

    private BookContents() {}


    public interface OnContentsAvailableListener {
        void onContentsAvailable(BookContents contents);
    }

    public interface OnContentsChapterAvailableListener {
        void onContentsChapterAvailable(Chapter chapter);
    }


    private static class ContentsReader extends AsyncTask<Void, Void, BookContents> {
        private final WeakReference<OnContentsReadListener> mListenerRef;
        private final Resources mResources;


        public ContentsReader(Resources res, OnContentsReadListener listener) {
            mResources = res;
            mListenerRef = new WeakReference<>(listener);
        }

        @Override
        protected BookContents doInBackground(Void... params) {
            return BookContents.instantiate(mResources);
        }

        @Override
        protected void onPostExecute(BookContents result) {
            OnContentsReadListener listener = mListenerRef.get();
            if (listener != null) {
                listener.onContentsRead();
            }
        }

        public interface OnContentsReadListener {
            void onContentsRead();
        }
    }


    public static class Chapter {
        private final String mTitle;

        private final List<Page> mPages = new ArrayList<>();

        public static Chapter readXml(XmlResourceParser parser, Resources res)
                throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, null, "chapter");

            String title = parser.getAttributeValue(null, "title");
            if (title.charAt(0) == '@') {
                int titleResId = parser.getAttributeResourceValue(null, "title", 0);
                if (titleResId != 0) {
                    title = res.getString(titleResId);
                }
            }
            Chapter result = new Chapter(title);
            parser.next();

            while (parser.getEventType() != XmlPullParser.END_TAG) {
                result.addPage(Page.readXml(parser));
            }

            parser.require(XmlPullParser.END_TAG, null, "chapter");
            parser.next();
            return result;
        }

        public String getTitle() {
            return mTitle;
        }

        public Page getPage(int index) {
            return mPages.get(index);
        }

        public int getNumPages() {
            return mPages.size();
        }

        private Chapter(String title) {
            this.mTitle = title;
        }

        private void addPage(Page page) {
            mPages.add(page);
        }
    }

    public static class Page {
        public final int imageId;
        public final double latitude;
        public final double longitude;
        public final ImageView.ScaleType scaleType;
        public final int backgroundId;


        public static Page readXml(XmlResourceParser parser)
                throws IOException, XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, null, "page");

            int imageId = 0;
            double latitude = 0.0;
            double longitude = 0.0;
            ImageView.ScaleType scaleType = ImageView.ScaleType.CENTER_CROP;
            int background = 0;

            boolean foundLatitude = false;
            boolean foundLongitude = false;

            for (int i = 0; i < parser.getAttributeCount(); i++) {
                switch (parser.getAttributeName(i)) {

                    case "img":
                        imageId = parser.getAttributeResourceValue(i, 0);
                        break;

                    case "latitude":
                        latitude = Double.parseDouble(parser.getAttributeValue(i));
                        foundLatitude = true;
                        break;

                    case "longitude":
                        longitude = Double.parseDouble(parser.getAttributeValue(i));
                        foundLongitude = true;
                        break;

                    case "gravity":
                        scaleType = readScaleType(parser.getAttributeValue(i));

                        break;

                    case "background":
                        background = parser.getAttributeResourceValue(i, 0);
                        if (background == 0) {
                            throw new IOException("Background resource not found: " + parser.getAttributeValue(i));
                        }
                        break;

                    default:
                        throw new IOException("Unrecognized XML attribute: " + parser.getAttributeName(i));

                }
            }


            // background can be unspecified (at default of 0)
            // gravity can be unspecified, left at default
            // Everything else must be specified
            if (!(imageId != 0 && foundLatitude && foundLongitude)) {
                throw new IOException("img, latitude, and longitude must be specified for every page");
            }


            parser.next();
            parser.require(XmlPullParser.END_TAG, null, "page");
            parser.next();

            return new Page(imageId, latitude, longitude, scaleType, background);
        }

        private Page(int resId, double lat, double lon, ImageView.ScaleType scaleType, int backgroundId) {
            this.latitude = lat;
            this.longitude = lon;
            this.imageId = resId;
            this.scaleType = scaleType;
            this.backgroundId = backgroundId;
        }

    }

    @NonNull
    private static ImageView.ScaleType readScaleType(@Nullable String attributeValue) throws IOException {
        if (attributeValue == null || attributeValue.equals("")) return DEFAULT_SCALETYPE;

        ImageView.ScaleType scaleType;
        switch (attributeValue) {
            case "center":
                scaleType = ImageView.ScaleType.CENTER;
                break;

            case "centerCrop":
                scaleType = ImageView.ScaleType.CENTER_CROP;
                break;

            case "centerInside":
                scaleType = ImageView.ScaleType.CENTER_INSIDE;
                break;

            case "fitCenter":
                scaleType = ImageView.ScaleType.FIT_CENTER;
                break;

            case "fitEnd":
                scaleType = ImageView.ScaleType.FIT_END;
                break;

            case "fitStart":
                scaleType = ImageView.ScaleType.FIT_START;
                break;

            case "fitXY":
                scaleType = ImageView.ScaleType.FIT_XY;
                break;

            case "matrix":
                scaleType = ImageView.ScaleType.MATRIX;
                break;

            default:
                throw new IOException("Unrecognized gravity: " + attributeValue);
        }
        return scaleType;
    }

    private static int readGravity(@Nullable String gravString) throws IOException {
        if (gravString == null || gravString.equals("")) return DEFAULT_GRAVITY;

        int result = 0;
        String[] gravValues = gravString.split("\\|");
        for (String gravValue : gravValues) {
            switch (gravValue) {
                case "top":
                    result |= Gravity.TOP;
                    break;

                case "bottom":
                    result |= Gravity.BOTTOM;
                    break;

                case "left":
                    result |= Gravity.LEFT;
                    break;

                case "right":
                    result |= Gravity.RIGHT;
                    break;

                case "center_vertical":
                    result |= Gravity.CENTER_VERTICAL;
                    break;

                case "fill_vertical":
                    result |= Gravity.FILL_VERTICAL;
                    break;

                case "center_horizontal":
                    result |= Gravity.CENTER_HORIZONTAL;
                    break;

                case "fill_horizontal":
                    result |= Gravity.FILL_HORIZONTAL;
                    break;

                case "center":
                    result |= Gravity.CENTER;
                    break;

                case "fill":
                    result |= Gravity.FILL;
                    break;

                case "clip_vertical":
                    result |= Gravity.CLIP_VERTICAL;
                    break;

                case "clip_horizontal":
                    result |= Gravity.CLIP_HORIZONTAL;
                    break;

                case "start":
                    result |= Gravity.START;
                    break;

                case "end":
                    result |= Gravity.END;
                    break;

                default:
                    throw new IOException("Unrecognized gravity attribute: " + gravValue);

            }
        }

        return result;

    }
}
