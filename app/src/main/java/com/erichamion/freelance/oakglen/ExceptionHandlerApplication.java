package com.erichamion.freelance.oakglen;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * Email stack trace when any uncaught exception occurs, for testing during development.
 *
 * Created by Eric Ray on 6/8/16.
 */
public class ExceptionHandlerApplication extends Application {

    @Override
    public void onCreate() {

        final Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"erichamion@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "'Discover Oak Glen' exception report");
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final StringBuilder stringBuilder = new StringBuilder(4096);

        ComponentName componentName = emailIntent.resolveActivity(getPackageManager());
        if (componentName == null || componentName.getPackageName().equals("com.android.fallback")) {
            Toast.makeText(this, "No email app/account. Will not be able to send crash reports.", Toast.LENGTH_LONG)
                    .show();
        } else {
            final Thread.UncaughtExceptionHandler baseHandler = Thread.getDefaultUncaughtExceptionHandler();

            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    try {
                        if (ex.getMessage() != null) {
                            safeAppend(stringBuilder, ex.getMessage(), 100);
                            safeAppend(stringBuilder, "\n", 2);
                        }
                        safeAppend(stringBuilder, Log.getStackTraceString(ex), 2000);
                        Throwable cause = (ex.getCause() == ex) ? null : ex.getCause();
                        while (cause != null) {
                            safeAppend(stringBuilder, "\nCaused by:\n", 20);
                            if (cause.getMessage() != null) {
                                safeAppend(stringBuilder, cause.getMessage(), 100);
                                safeAppend(stringBuilder, Log.getStackTraceString(cause), 2000);
                            }
                            cause = (cause.getCause() == ex) ? null : cause.getCause();
                        }
                        emailIntent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());
                        startActivity(emailIntent);
                    } catch (Throwable ignored) {
                    } finally {
                        baseHandler.uncaughtException(thread, ex);
                    }
                }
            });
        }


        super.onCreate();
    }


    void safeAppend(StringBuilder builder, String toAppend, int maxToAppend) {

        if (toAppend.length() <= builder.capacity() - builder.length() && toAppend.length() <= maxToAppend) {
            builder.append(toAppend);
        } else {
            builder.append(toAppend, 0, Math.min(builder.capacity() - builder.length(), maxToAppend));
        }
        
    }
}
