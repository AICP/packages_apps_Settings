package com.android.settings.aicp.stats;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.android.settings.R;

public class ViewStats extends Activity {
	Intent intent;
	Context context;
	WebView wv;

	@SuppressLint("SetJavaScriptEnabled")
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewstats_webview);

		WebView wv = (WebView) findViewById(R.id.webStats);

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
			@Override
			public boolean onCreateOptionsMenu(Menu menu) {
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.main, menu);
				return super.onCreateOptionsMenu(menu);
			}

			@Override
			public boolean onPrepareOptionsMenu(Menu menu) {

				return super.onPrepareOptionsMenu(menu);
			}

			@Override
			public boolean onOptionsItemSelected(MenuItem item) {
				switch (item.getItemId()) {

				case R.id.exit:
					super.finish();
					break;

				default:

				};

				return super.onOptionsItemSelected(item);
			}
}
