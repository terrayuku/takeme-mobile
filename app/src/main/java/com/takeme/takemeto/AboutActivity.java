package com.takeme.takemeto;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;



public class AboutActivity extends AppCompatActivity {
    private WebView webview;
    ProgressBar simpleProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        webview =(WebView) findViewById(R.id.webView);
        simpleProgressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);

        webview.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                    simpleProgressBar.setVisibility(View.VISIBLE);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                view.loadUrl(url);
                return true;
            }
            @Override
            public void onPageFinished(final WebView view, String url) {

                simpleProgressBar.setVisibility(View.GONE);
            }
        });
//        webview.setWebViewClient(new MyClient());
//        client.setFirsttime();
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
//        webview.getSettings().setUseWideViewPort(true);
//        webview.getSettings().setLoadWithOverviewMode(true);
        webview.loadUrl("https://terrayuku.github.io/takeme-web/#/about");


    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public class MyClient extends WebViewClient {
        private boolean oneTime=false;
        public void setFirsttime(){
            oneTime=true;
        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if(oneTime){
                simpleProgressBar.setVisibility(View.VISIBLE);
                oneTime=false;
            }

        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            view.loadUrl(url);
            return true;
        }
        @Override
        public void onPageFinished(final WebView view, String url) {

            super.onPageFinished(view, url);
            simpleProgressBar.setVisibility(View.GONE);
        }
    }
}
