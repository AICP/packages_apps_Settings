package com.android.settings.aicp.stats.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.android.settings.R;

public class ViewStats extends Activity {
	Context context;
	WebView wv;

	@SuppressLint("SetJavaScriptEnabled")
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewstats_webview);

		WebView wv = (WebView) findViewById(R.id.webStats);

		wv.getSettings().setUserAgentString(
				"Mozilla/5.0 " + "(Windows NT 6.2; "
						+ "WOW64) AppleWebKit/537.31 "
						+ "(KHTML, like Gecko) Chrome/20 " + "Safari/537.31");

		wv.loadUrl("http://stats.aicp-rom.com/");

		wv.getSettings().setJavaScriptEnabled(true);

		wv.clearCache(true);

		WebSettings webSettings = wv.getSettings();

		wv.getSettings().setPluginState(PluginState.ON);

		webSettings.setDomStorageEnabled(true);

		wv.setDownloadListener(new DownloadListener() {

			@Override
			public void onDownloadStart(String url, String userAgent,
					String contentDisposition, String mimetype,
					long contentLength) {

				Uri uri = Uri.parse(url);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);

			}
		});

		wv.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
			}
		});

		wv.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				{
					view.loadUrl(url);
					return true;
				}
			}
		});
	}
}
