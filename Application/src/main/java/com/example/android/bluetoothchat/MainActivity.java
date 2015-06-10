/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.bluetoothchat;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.android.common.activities.SampleActivityBase;
import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;

import java.util.HashMap;
import java.util.Locale;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase
        implements BluetoothChatFragment.UpdateDataMonitor {

    public static final String TAG = "MainActivity";
    private static final long REPEAT_INTERVAL = 300 * 1000;

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;
    private TextToSpeech tts;
    private HashMap<Integer, Long> repeatNotify = new HashMap<>();

    private static final int temp[] = { -20, -10, 0, 10, 20, 30, 40, 50, 60, 70, 80, 85, 90, 95, 100, 105, 110 };

    BluetoothChatFragment m_fragment1;
    DataMonitorFragment m_fragment2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {

                    int result = tts.setLanguage(new Locale("en"));

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "language is not supported");
                    }
                    tts.setSpeechRate(1.3f);
                    tts.speak("start", TextToSpeech.QUEUE_FLUSH, null);
                } else {
                    Log.e("TTS", "Error");
                }

            }
        });

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            m_fragment1 = new BluetoothChatFragment();
            transaction.replace(R.id.sample_content_fragment, m_fragment1);
            transaction.commit();
            FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
            m_fragment2 = new DataMonitorFragment();
            transaction2.replace(R.id.sample_content_fragment2, m_fragment2);
            transaction2.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
/*        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.sample_output) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);
*/
        return super.onPrepareOptionsMenu(menu);
    }

    public void onDestroy(){
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
/*            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = (ViewAnimator) findViewById(R.id.sample_output);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                supportInvalidateOptionsMenu();
                return true;
*/        }
        return super.onOptionsItemSelected(item);
    }

    /** Create a chain of targets that will receive log data */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());

        Log.i(TAG, "Ready");
    }

    public void UpdateDataMonitorToActivity(BluetoothChatFragment.CvtDataDump cvtDataDump) {
        m_fragment2.setData(cvtDataDump);

        for(int t : temp){
            if(cvtDataDump.m_iDataAtfTemp == t){
                if(!repeatNotify.containsKey(t) || (repeatNotify.containsKey(t) && System.currentTimeMillis() - repeatNotify.get(t) > REPEAT_INTERVAL)) {
                    Toast.makeText(getApplicationContext(), "AtfTemp current: " + cvtDataDump.m_iDataAtfTemp, Toast.LENGTH_SHORT).show();
                    tts.speak("temperature: " + cvtDataDump.m_iDataAtfTemp, TextToSpeech.QUEUE_FLUSH, null);
                    repeatNotify.put(t, System.currentTimeMillis());
                }
            }
        }


    }

    public void ShowDataMonitorToActivity (boolean bShowDataMonitor) {
        if (bShowDataMonitor) {
            findViewById(R.id.relativeLayout1).setVisibility(View.GONE);
            findViewById(R.id.relativeLayout2).setVisibility(View.VISIBLE);
            //findViewById(R.id.relativeLayout1).getLayoutParams().height = 1; // Works but not needed since View.GONE works ok
        }
        else {
            findViewById(R.id.relativeLayout2).setVisibility(View.GONE);
            findViewById(R.id.relativeLayout1).setVisibility(View.VISIBLE);
        }
    }
}
