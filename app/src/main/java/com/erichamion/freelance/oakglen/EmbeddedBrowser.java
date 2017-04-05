/**
 * Originally from the CIM Auto-Connect app,
 * package com.nerrdit.freelancer.autoconnector
 */

package com.erichamion.freelance.oakglen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

@SuppressWarnings("ALL")
public class EmbeddedBrowser extends Activity {
	public static final String URL_TO_OPEN = "URL_TO_OPEN";
	private final String SERVER_HOMEPAGE = "http://10.0.0.1/";

	private WebView mWebView;

	private LinearLayout mContentView;
	private FrameLayout mCustomViewContainer;
	private View mCustomView;
	private WebChromeClient.CustomViewCallback mCustomViewCallback;
	FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);

	private WebChromeClient mWebChromeClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.embedded_browser);

		mContentView = (LinearLayout) findViewById(R.id.linearlayout);
		mWebView = (WebView) findViewById(R.id.webView);
		mCustomViewContainer = (FrameLayout) findViewById(R.id.fullscreen_custom_content);

		mWebChromeClient = new WebChromeClient() {

			@Override
			public void onShowCustomView(View view,
					WebChromeClient.CustomViewCallback callback) {
				// if a view already exists then immediately terminate the new
				// one
				if (mCustomView != null) {
					callback.onCustomViewHidden();
					return;
				}

				// Add the custom view to its container.
				mCustomViewContainer.addView(view, COVER_SCREEN_GRAVITY_CENTER);
				mCustomView = view;
				mCustomViewCallback = callback;

				// hide main browser view
				mContentView.setVisibility(View.GONE);

				// Finally show the custom view container.
				mCustomViewContainer.setVisibility(View.VISIBLE);
				mCustomViewContainer.bringToFront();
			}

			@Override
			public void onHideCustomView() {
				if (mCustomView == null)
					return;

				// Hide the custom view.
				mCustomView.setVisibility(View.GONE);
				// Remove the custom view from its container.
				mCustomViewContainer.removeView(mCustomView);
				mCustomView = null;
				mCustomViewContainer.setVisibility(View.GONE);
				mCustomViewCallback.onCustomViewHidden();

				// Show the content view.
				mContentView.setVisibility(View.VISIBLE);
			}
		};

		// mWebView = (WebView) findViewById(R.id.webView);
		configureWebView();

		String url = SERVER_HOMEPAGE;// getIntent().getStringExtra(URL_TO_OPEN);

		if (url != null) {
			mWebView.loadUrl(url);
		}
	}

	/**
	 * Configures webview settings by enabling javascript, disabling password
	 * saving and rejecting cookies
	 */
	@SuppressLint("SetJavaScriptEnabled")
	private void configureWebView() {
		if (mWebView == null)
			return;

		WebSettings webSettings = mWebView.getSettings();
		webSettings.setPluginState(WebSettings.PluginState.ON);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setUseWideViewPort(true);
		webSettings.setLoadWithOverviewMode(true);

		mWebView.loadUrl("http://www.google.com");
		mWebView.setWebViewClient(new HelloWebViewClient());

		// mWebView.setWebViewClient(new HelloWebViewClient());

		// Enable Javascript
		// WebSettings webSettings = mWebView.getSettings();
		// webSettings.setJavaScriptEnabled(true);

		// Force links and redirects to open in the WebView instead of in a
		// browser
		// mWebView.setWebViewClient(new WebViewClient());
	}

	private class HelloWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView webview, String url) {
			webview.setWebChromeClient(mWebChromeClient);
			webview.loadUrl(url);

			return true;
		}
	}

	@Override
	protected void onStop() {

		super.onStop();
		if (mCustomView != null) {
			if (mCustomViewCallback != null)
				mCustomViewCallback.onCustomViewHidden();
			mCustomView = null;
		}

	}

	@Override
	public void onBackPressed() {

		super.onBackPressed();
		if (mCustomView != null) {
			mWebChromeClient.onHideCustomView();
		} else {
			finish();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
			mWebView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}
}
