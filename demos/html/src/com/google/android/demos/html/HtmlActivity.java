/*-
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.demos.html;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Window;

import com.google.android.htmlwidget.HtmlChromeClient;
import com.google.android.htmlwidget.HtmlView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Demo for {@link HtmlView}.
 */
public class HtmlActivity extends Activity {

    private HtmlView mHtmlView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        mHtmlView = (HtmlView) findViewById(R.id.html);
        mHtmlView.setHtmlChromeClient(new ProgressChromeClient());
        mHtmlView.setMovementMethod(LinkMovementMethod.getInstance());

        // Load HTML asynchronously:
        //
        // TODO: Move this functionality into HtmlView
        new HtmlTask().execute("http://slashdot.org/palm");
    }

    private class ProgressChromeClient extends HtmlChromeClient {
        @Override
        public void onProgressChanged(HtmlView view, int newProgress) {
            Window window = getWindow();
            window.setFeatureInt(Window.FEATURE_PROGRESS, newProgress * 100);
        }
    }

    private class HtmlTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... args) {
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(args[0]);
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            } catch (IOException e) {
                return String.valueOf(e);
            }
        }

        @Override
        protected void onPostExecute(String source) {
            if (!isFinishing()) {
                // Resolve relative URLs:
                //
                // TODO: Move this functionality into HtmlView
                source = source.replace("<img src=\"//", "<img src=\"http://");
                source = source.replace("<a href=\"/", "<a href=\"http://slashdot.org/");
                
                mHtmlView.setHtml(source);
            }
        }
    }
}
