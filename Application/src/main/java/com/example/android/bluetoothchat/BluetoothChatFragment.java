/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import android.text.format.Time;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private Button mCVTDiagButton;
    private Button mCVTDeteriorationButton;
    private Button mCVTParamsButton;
    private String m_sLastTriedDeviceMAC;
    private boolean m_bConnectedToReconnectOnLost = false;
    private boolean m_bConnectionLost = false;
    private int m_iReconnectionDivider = 18;
    private int m_iAutoConnectAction = 0;
    private DataMonitorAutostartTimer m_DataMonitorAutostartTimer = null;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    private CVTDiag mCVTDiag = null;
    private boolean m_bDisableChatLogging = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
        if (null != mCVTDiag)
            mCVTDiag.stop();
        if (null != m_DataMonitorAutostartTimer)
            m_DataMonitorAutostartTimer.stop();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
        mCVTDiagButton = (Button) view.findViewById(R.id.button_cvtdiag);
        mCVTDeteriorationButton = (Button) view.findViewById(R.id.button_cvtdeterioration);
        mCVTParamsButton = (Button) view.findViewById(R.id.button_cvtparams);
    }

    public interface UpdateDataMonitor {
        public void UpdateDataMonitorToActivity(BluetoothChatFragment.CvtDataDump cvtDataDump);
        public void ShowDataMonitorToActivity(boolean bShowDataMonitor);
    }
    UpdateDataMonitor m_callbackActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            m_callbackActivity = (UpdateDataMonitor) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnHeadlineSelectedListener");
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mOutEditText.setFocusableInTouchMode(false);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_DataMonitorAutostartTimer.stop();
                // Send a message using content of the edit text widget
                mOutEditText.setFocusableInTouchMode(true);
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    message += "\r";
                    sendMessage(message);
                }
            }
        });

        // Initialize the CVT Diag button with a listener that for click events
        mCVTDiagButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_DataMonitorAutostartTimer.stop();
                if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                    Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                    m_iAutoConnectAction = Constants.CVTDIAG_ACTION_READDTC;
                    return;
                }
                // Start CVT Diag thread
                if (null != mCVTDiag) {
                    mCVTDiag.stop();
                    mCVTDiag = null;
                }
                mCVTDiag = new CVTDiag(mHandler, Constants.CVTDIAG_ACTION_READDTC);
                mCVTDiag.start();
                m_bConnectedToReconnectOnLost = false;
            }
        });

        // Initialize the CVT Diag button with a listener that for click events
        mCVTDeteriorationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_DataMonitorAutostartTimer.stop();
                if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                    Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                    m_iAutoConnectAction = Constants.CVTDIAG_ACTION_READDETERIORATION;
                    return;
                }
                // Start CVT Diag thread
                if (null != mCVTDiag) {
                    mCVTDiag.stop();
                    mCVTDiag = null;
                }
                mCVTDiag = new CVTDiag(mHandler, Constants.CVTDIAG_ACTION_READDETERIORATION);
                mCVTDiag.start();
                m_bConnectedToReconnectOnLost = false;
            }
        });

        // Initialize the CVT Diag button with a listener that for click events
        mCVTParamsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_DataMonitorAutostartTimer.stop();
                if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
                    Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                    m_iAutoConnectAction = Constants.CVTDIAG_ACTION_READPARAMS;
                    return;
                }
                // Start CVT Diag thread
                if (null != mCVTDiag) {
                    mCVTDiag.stop();
                    mCVTDiag = null;
                }
                mCVTDiag = new CVTDiag(mHandler, Constants.CVTDIAG_ACTION_READPARAMS);
                mCVTDiag.start();
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        m_DataMonitorAutostartTimer = new DataMonitorAutostartTimer(mHandler);
        m_DataMonitorAutostartTimer.start();

        mConversationArrayAdapter.add("Data Monitor will be started in 10 seconds. Press any button to cancel.");
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            //Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show(); // Commenting to avoid crash
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            m_bConnectionLost = false;
                            m_iReconnectionDivider = 18;
                            mConversationArrayAdapter.clear();
                            if (m_bConnectedToReconnectOnLost) {
                                mConversationArrayAdapter.add("Bluetooth connection restored, continue data collection");
                                mCVTParamsButton.performClick();
                            }
                            if (0 != m_iAutoConnectAction) {
                                int iAction = m_iAutoConnectAction;
                                m_iAutoConnectAction = 0;
                                if (Constants.CVTDIAG_ACTION_READDTC == iAction) mCVTDiagButton.performClick();
                                else if (Constants.CVTDIAG_ACTION_READDETERIORATION == iAction) mCVTDeteriorationButton.performClick();
                                else if (Constants.CVTDIAG_ACTION_READPARAMS == iAction) mCVTParamsButton.performClick();
                            }
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    if (!m_bDisableChatLogging) {
                        WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "> " + writeMessage);
                        mConversationArrayAdapter.add("CVTz50:  " + writeMessage);
                    }
                    break;
                case Constants.MESSAGE_READ:
                    String readMessage = (String) msg.obj;
                    if (!m_bDisableChatLogging) {
                        mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                        WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "< " + readMessage);
                    }
                    if (null != mCVTDiag) {
                        mCVTDiag.OBD_Message(readMessage);
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                        if (msg.getData().getString(Constants.TOAST).equals("Device connection was lost")) {
                            mConversationArrayAdapter.add("Bluetooth connection lost");
                            m_bConnectionLost = true;
                        }
                    }
                    break;
                case Constants.MESSAGE_OBDTIMER:
                    if (null != mCVTDiag) {
                        mCVTDiag.ProcessOBDTimerEvent();
                    }
                    if (m_bConnectionLost && m_bConnectedToReconnectOnLost) {
                        m_iReconnectionDivider++;
                        if (20 == m_iReconnectionDivider) {
                            mConversationArrayAdapter.add("Reconnecting to " + m_sLastTriedDeviceMAC);
                            connectDevice(null, true);
                            m_iReconnectionDivider = 0;
                        }
                    }
                    break;
                case Constants.MESSAGE_DATAMONITORAUTOSTARTTIMER:
                    mCVTParamsButton.performClick();
                    break;
            }
        }
    };

    public static void WriteCvtLog(int iLogFileId, String sLogString) {
        Time t = new Time();
        t.setToNow();
        String sDateTime = t.format("%Y-%m-%d\t%H:%M:%S\t");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            try {
                String sFileName = "";
                boolean bNewFile = false;
                if (Constants.CVTDIAG_LOGFILE_GENERAL == iLogFileId)
                    sFileName = "CVTz50.txt";
                else if (Constants.CVTDIAG_LOGFILE_PARAMS == iLogFileId)
                    sFileName = "CVTz50_params.txt";
                File file = new File(Environment.getExternalStorageDirectory().getPath()+"/"+sFileName);
                if( !file.exists() ){
                    file.createNewFile();
                    bNewFile = true;
                }
                FileWriter fileWritter = new FileWriter(file.getAbsolutePath(),true);
                BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                if (bNewFile && Constants.CVTDIAG_LOGFILE_PARAMS == iLogFileId) {
                    bufferWritter.write("DATE\tTIME\tLEVER\tVEHICLE_SPEED\tENG_SPEED_SIG\tACC_PEDAL_OPEN\tATF_TEMP\tGEAR_RATIO\tSTM_STEP\tTGT_SEC_PRESS\tSEC_PRESS\tPRI_PRESS\tLINE_PRS\tLU_PRS\tISOLT1\tETS_SOLENOID\tCVTF_DETERIORATION\r\n");
                }
                bufferWritter.write(sDateTime+sLogString+"\r\n");
                bufferWritter.close();
            }
            catch (Exception e) {
                //Toast.makeText(getActivity(), e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra. Or null if reconnecting
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address;
        if (null != data) {
            address = data.getExtras()
                    .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            m_sLastTriedDeviceMAC = address;
        }
        else {
            address = m_sLastTriedDeviceMAC;
        }
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
/*            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }*/
            case R.id.start_data_monitor_cvt_only: {
                if (null != mCVTDiag) {
                    mCVTDiag.m_bReadCvtOnly = true;
                }
                return true;
            }
        }
        return false;
    }

    private final class DataMonitorAutostartTimer extends TimerTask {
        Timer m_tTimer;
        Handler m_hMessageHandler;
        public DataMonitorAutostartTimer(Handler h) {
            m_tTimer = new Timer();
            m_hMessageHandler = h;
        }
        public void start() { if (null != m_tTimer) m_tTimer.schedule(this, 10000L); }
        public void stop() {
            if (null != m_tTimer) {
                m_tTimer.cancel();
                m_tTimer = null;
            }
        }
        @Override public void run() {
            m_hMessageHandler.sendMessage(m_hMessageHandler.obtainMessage(Constants.MESSAGE_DATAMONITORAUTOSTARTTIMER));
            stop();
        }
    }

    private final class CVTDiagTimerTask extends TimerTask {
        Timer m_tTimerIncomingBufferCheck = null;
        Handler m_hMessageHandler;

        public CVTDiagTimerTask(Handler h) {
            m_tTimerIncomingBufferCheck = new Timer();
            m_hMessageHandler = h;
        }

        public void start() {
            m_tTimerIncomingBufferCheck.scheduleAtFixedRate(this, 0, 1000);
        }

        public void stop() {
            if (null != m_tTimerIncomingBufferCheck)
                m_tTimerIncomingBufferCheck.cancel();
        }

        @Override public void run() {
            m_hMessageHandler.sendMessage(m_hMessageHandler.obtainMessage(Constants.MESSAGE_OBDTIMER));
        }
    }

    private final class CVTDiag {
        int m_iGlobalAction;
        int m_iNextAction;
        String m_sWaitingForString;
        String m_sIncomingBuffer;
        boolean m_bIncomingBufferLock;
        CVTDiagTimerTask m_CVTDiagTimerTask = null;
        String m_sAwdDataFromIncomingBuffer;
        String m_sEcuDataFromIncomingBuffer_CoolanTemp;
        String m_sEcuDataFromIncomingBuffer_InjPulse;
        String m_sEcuDataFromIncomingBuffer_EngSpeed;
        String m_sEcuDataFromIncomingBuffer_VehicleSpeed;
        String m_sEcuDataFromIncomingBuffer_DTC;
        boolean m_bCvtDtcDataReceived;
        boolean m_bCvtDeteriorationDataReceived;
        int m_iCvtDtcCount;
        int m_iCvtfDeteriorationDate;
        int m_iCvtfDeteriorationDateFirst;
        public boolean m_bReadCvtOnly;

        public CVTDiag(Handler hParentMessageHandler, int iGlobalAction) {
            m_iGlobalAction = iGlobalAction;
            m_iNextAction = 0;
            m_sWaitingForString = "";
            m_sIncomingBuffer = "";
            m_bIncomingBufferLock = false;
            m_CVTDiagTimerTask = new CVTDiagTimerTask(hParentMessageHandler);
            m_bDisableChatLogging = false; // from parent class
            m_sAwdDataFromIncomingBuffer = "";
            m_sEcuDataFromIncomingBuffer_CoolanTemp = "";
            m_sEcuDataFromIncomingBuffer_InjPulse = "";
            m_sEcuDataFromIncomingBuffer_EngSpeed = "";
            m_sEcuDataFromIncomingBuffer_VehicleSpeed = "";
            m_sEcuDataFromIncomingBuffer_DTC = "";
            m_bCvtDtcDataReceived = false; // flag used to get CVT DTC data read once per data monitor start
            m_bCvtDeteriorationDataReceived = false;
            m_iCvtDtcCount = 0;
            m_iCvtfDeteriorationDate = 0;
            m_iCvtfDeteriorationDateFirst = -1;
            m_bReadCvtOnly = false;
        }

        public void start() {
            m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP1_ATZ;
            NextAction();
            m_CVTDiagTimerTask.start();
        }

        public void stop() {
            if (null != m_CVTDiagTimerTask)
                m_CVTDiagTimerTask.stop();
        }

        public void OBD_Message(String sObdMessage) {
            if (m_sWaitingForString.length() > 0) {
                m_bIncomingBufferLock = true;
                m_sIncomingBuffer += sObdMessage;
                m_bIncomingBufferLock = false;
            }
            ProcessOBDTimerEvent();
        }

        public void ProcessOBDTimerEvent() {
            CheckIncomingBufferForExpectedString();
        }

        public void CheckIncomingBufferForExpectedString() {
            if (0 != m_iNextAction && !m_bIncomingBufferLock && m_sWaitingForString.length() > 0 && -1 != m_sIncomingBuffer.lastIndexOf(m_sWaitingForString) ) {
                NextAction();
                m_sIncomingBuffer = "";
            }
        }

        private void NextAction() {
            switch (m_iNextAction) {
                case Constants.CVTDIAG_NEXT_DTC_STEP1_ATZ: {
                    mConversationArrayAdapter.clear();
                    m_bCvtDtcDataReceived = false;
                    sendMessage(" ATZ\r");
                    m_sWaitingForString = "ELM327";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP1A_ATE0;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP1A_ATE0: {
                    sendMessage(" ATE0\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP1B_ATE0;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP1B_ATE0: {
                    sendMessage(" ATE0\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP2_ATAL;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP2_ATAL: {
                    sendMessage(" ATAL\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP3_ATST32;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP3_ATST32: {
                    sendMessage(" ATST32\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP4_ATSW00;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP4_ATSW00: {
                    sendMessage(" ATSW00\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP5_ATSP6;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP5_ATSP6: {
                    sendMessage(" ATSP6\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP6_ATSH7E1;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP6_ATSH7E1: {
                    sendMessage(" ATSH7E1\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP7_10C0;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP7_10C0: {
                    sendMessage("10C0\r");
                    //m_sWaitingForString = "50 C0";
                    //m_sWaitingForString = "50 C0 \r\r>";
                    m_sWaitingForString = ">";
                    if ( Constants.CVTDIAG_ACTION_READDTC ==  m_iGlobalAction || (Constants.CVTDIAG_ACTION_READPARAMS==m_iGlobalAction && !m_bCvtDtcDataReceived) ) { //Read CVT DTC if requested and on every start of data monitor
                        m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP8_17FF00;
                    }
                    else if (Constants.CVTDIAG_ACTION_READDETERIORATION == m_iGlobalAction || (Constants.CVTDIAG_ACTION_READPARAMS==m_iGlobalAction && !m_bCvtDeteriorationDataReceived) ) {
                        m_iNextAction = Constants.CVTDIAG_NEXT_DETERIORATION_STEP8_2103;
                    }
                    else if (Constants.CVTDIAG_ACTION_READPARAMS == m_iGlobalAction) {
                        m_iNextAction = Constants.CVTDIAG_NEXT_READPARAMS_STEP8_2101;
                    }
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP8_17FF00: {
                    sendMessage(" 17FF00\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP9_RESULT;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DTC_STEP9_RESULT: {
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    m_sWaitingForString = "";
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "DTC RESULT: " + sIncomingBufferLocalCopy);
                    }
                    m_bCvtDtcDataReceived = true; // If comment this string, then CVT DTC will be checked on each cycle (not exactly correct, additional changes will be needed)

                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replaceAll("[^0-9A-F:]", ""); // remove all non HEX number symbols so now we will have reply with no any spaces
                    if ( sIncomingBufferLocalCopy.length()==4 && sIncomingBufferLocalCopy.substring(0,4).equals("5700") ) {
                        if (!m_bDisableChatLogging) {
//                        mConversationArrayAdapter.clear();
                            mConversationArrayAdapter.add(" ");
                            mConversationArrayAdapter.add("OK: NO CVT DTC");
                        }
                    }
                    else {
                        if (sIncomingBufferLocalCopy.length()>=5 && sIncomingBufferLocalCopy.substring(4,5).contains(":")) { // Reply format: 00B    0: 57 03 07 25 40 08    1: 26 40 17 01 40 00 00
                            sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.substring(3);
                            sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replaceAll(".:", ""); // only first ~32 errors will be processed correctly
                        }
                        // now we should have bytestream starting with 57
                        if (sIncomingBufferLocalCopy.length()>=4 && sIncomingBufferLocalCopy.substring(0,2).equals("57")) {
                            sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.substring(2);
                            m_iCvtDtcCount = Integer.parseInt(sIncomingBufferLocalCopy.substring(0, 2), 16);
                            sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.substring(2);
                            String sDtcList;
                            sDtcList = "";
                            while (sIncomingBufferLocalCopy.length() >= 4) {
                                sDtcList += sIncomingBufferLocalCopy.substring(0, 4) + " ";
                                sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.substring(4);
                                if (sIncomingBufferLocalCopy.length() >= 2)
                                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.substring(2); //remove delimiter (delimiter is 40 but doesn't matter)
                            }
                            sDtcList = sDtcList.replaceAll("0000", "");
                            sDtcList = sDtcList.replaceAll("D000", "U1000");
                            sIncomingBufferLocalCopy = String.format("%d CVT DTC DETECTED: ", m_iCvtDtcCount);
                            sIncomingBufferLocalCopy += sDtcList;
                            if (!m_bDisableChatLogging) {
//                            mConversationArrayAdapter.clear();
                                mConversationArrayAdapter.add(" ");
                                mConversationArrayAdapter.add(sIncomingBufferLocalCopy);
                                mConversationArrayAdapter.add("To clear (reset) CVT DTC: \r\n1. click [Send] button \r\n2. enter 14FF00 into the field below \r\n3. click [Read CVT DTC] button again \r\n4. click [Send] button immediately after this message shown again");
                            }
                        }
                    }

                    if (Constants.CVTDIAG_ACTION_READPARAMS == m_iGlobalAction) {
                        m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP7_10C0;
                        NextAction();
                    }
                    else { // Only reading DTC
                        m_CVTDiagTimerTask.stop();
                        m_iNextAction = 0;
                    }
                    break;
                }
                case Constants.CVTDIAG_NEXT_DETERIORATION_STEP8_2103: {
                    sendMessage(" 2103\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_DETERIORATION_STEP9_RESULT;
                    break;
                }
                case Constants.CVTDIAG_NEXT_DETERIORATION_STEP9_RESULT: {
                    m_CVTDiagTimerTask.stop();
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    m_sWaitingForString = "";
                    m_iNextAction = 0;
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "DETERIORATION RESULT: " + sIncomingBufferLocalCopy);
                    }
                    m_bCvtDeteriorationDataReceived = true;
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replaceAll("[^0-9A-F]", ""); // remove all non HEX number symbols so now we will have reply with no any spaces
                    if ( sIncomingBufferLocalCopy.length()>=12 && sIncomingBufferLocalCopy.substring(0,4).equals("6103") ) { // Format is 610300001234
                        m_iCvtfDeteriorationDate = Integer.parseInt(sIncomingBufferLocalCopy.substring(4, 12), 16);
                        if (-1 == m_iCvtfDeteriorationDateFirst) m_iCvtfDeteriorationDateFirst = m_iCvtfDeteriorationDate;
                        if (!m_bDisableChatLogging) {
                        sIncomingBufferLocalCopy = String.format("CVTF DETERIORATION DATE: %d", m_iCvtfDeteriorationDate);
                        if (m_iCvtfDeteriorationDate>210000) sIncomingBufferLocalCopy += "  CHANGE CVT FLUID!";
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, sIncomingBufferLocalCopy);
//                        mConversationArrayAdapter.clear();
                            mConversationArrayAdapter.add(" ");
                            mConversationArrayAdapter.add(sIncomingBufferLocalCopy);
                            mConversationArrayAdapter.add("To clear (reset): \r\n1. click [Send] button \r\n2. enter 3B0200000000 into the field below \r\n3. click [Read CVTF DETERIORATION] button again \r\n4. click [Send] button immediately after this message shown again");
                        }
                    }

                    if (Constants.CVTDIAG_ACTION_READPARAMS == m_iGlobalAction) {
                        m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP7_10C0;
                        NextAction();
                    }
                    else { // Only reading DETERIORATION
                        m_CVTDiagTimerTask.stop();
                        m_iNextAction = 0;
                    }
                    break;
                }
                case Constants.CVTDIAG_NEXT_READPARAMS_STEP8_2101: {
                    sendMessage(" 2101\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READPARAMS_STEP9_RESULT;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READPARAMS_STEP9_RESULT: {
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    //m_CVTDiagTimerTask.stop();
                    m_sWaitingForString = "";
                    //m_iNextAction = 0;
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "READPARAMS RESULT: " + sIncomingBufferLocalCopy);
                    }
                    CvtDataDump data = new CvtDataDump(sIncomingBufferLocalCopy, m_sAwdDataFromIncomingBuffer, m_sEcuDataFromIncomingBuffer_CoolanTemp, m_sEcuDataFromIncomingBuffer_InjPulse, m_sEcuDataFromIncomingBuffer_EngSpeed, m_sEcuDataFromIncomingBuffer_VehicleSpeed, m_sEcuDataFromIncomingBuffer_DTC, m_iCvtDtcCount, m_iCvtfDeteriorationDate, m_iCvtfDeteriorationDateFirst);
                    m_sAwdDataFromIncomingBuffer = "";
                    m_sEcuDataFromIncomingBuffer_CoolanTemp = "";
                    m_sEcuDataFromIncomingBuffer_InjPulse = "";
                    m_sEcuDataFromIncomingBuffer_EngSpeed = "";
                    m_sEcuDataFromIncomingBuffer_VehicleSpeed = "";
                    if (data.IsDumpValid()) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_PARAMS, data.GetTextStringForFile());
                        m_bDisableChatLogging = true;
                        m_bConnectedToReconnectOnLost = true; //Enable reconnection if we got at least 1 good parsable dump
                        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        mConversationArrayAdapter.clear();
                        mConversationArrayAdapter.add(data.GetTextStringForDisplay());
                        m_callbackActivity.ShowDataMonitorToActivity(true);
                        m_callbackActivity.UpdateDataMonitorToActivity(data);
                    }
                    else {
                        m_bDisableChatLogging = false;
                        m_callbackActivity.ShowDataMonitorToActivity(false);
                    }
                    if (m_bReadCvtOnly)
                        m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP7_10C0;
                    else {
                        m_iNextAction = Constants.CVTDIAG_NEXT_READECU_STEP5_ATSP5; // m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP7_10C0; - old NextAction when we had only repeating CVT param reading. Now we're moving to reading of ECU after reading CVT
                        m_bCvtDtcDataReceived = false;
                        m_bCvtDeteriorationDataReceived = false;
                    }
                    NextAction();
                    break;
                }
                case Constants.CVTDIAG_NEXT_READECU_STEP5_ATSP5: {
                    //Assuming that following commands already sent: atz ate0 atal atst32 atsw00
                    sendMessage(" ATSP5\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READECU_STEP6_ATSH8110FC;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READECU_STEP6_ATSH8110FC: {
                    sendMessage(" ATSH8110FC\r"); // Setting header for accessing ECU
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READECU_STEP7_2211010401;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READECU_STEP7_2211010401: {
                    sendMessage(" 2211010401\r"); //read engine coolant temperature
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READECU_STEP8_2212060401;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READECU_STEP8_2212060401: {
                    //process engine coolant temperature
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    m_sWaitingForString = "";
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "CoolanTemp RESULT: " + sIncomingBufferLocalCopy);
                    }
                    m_sEcuDataFromIncomingBuffer_CoolanTemp = sIncomingBufferLocalCopy;
                    sendMessage(" 2212060401\r"); //read injection time
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READECU_STEP9_2212010401;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READECU_STEP9_2212010401: {
                    //process injection time
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    m_sWaitingForString = "";
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "InjPulse RESULT: " + sIncomingBufferLocalCopy);
                    }
                    m_sEcuDataFromIncomingBuffer_InjPulse = sIncomingBufferLocalCopy;
                    sendMessage(" 2212010401\r"); //read rpm
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READECU_STEP10_2211020401;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READECU_STEP10_2211020401: {
                    //process rpm
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    m_sWaitingForString = "";
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "EngSpeed RESULT: " + sIncomingBufferLocalCopy);
                    }
                    m_sEcuDataFromIncomingBuffer_EngSpeed = sIncomingBufferLocalCopy;
                    sendMessage(" 2211020401\r"); //read speed
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READECU_STEP11_A330;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READECU_STEP11_A330: {
                    //process speed
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    m_sWaitingForString = "";
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "VehicleSpeed RESULT: " + sIncomingBufferLocalCopy);
                    }
                    m_sEcuDataFromIncomingBuffer_VehicleSpeed = sIncomingBufferLocalCopy;
                    sendMessage(" A330\r"); //read DTC
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READECU_STEP12_DTCRESULT;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READECU_STEP12_DTCRESULT: {
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    m_sWaitingForString = "";
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "ECU DTC RESULT: " + sIncomingBufferLocalCopy);
                    }
                    m_sEcuDataFromIncomingBuffer_DTC = sIncomingBufferLocalCopy;
                    m_iNextAction = Constants.CVTDIAG_NEXT_READAWD_STEP5A_ATSP6; //Continue with reading of AWD data
                    NextAction();
                    break;
                }
                case Constants.CVTDIAG_NEXT_READAWD_STEP5A_ATSP6: { // need to switch from atsp5 to atsp6 and back to avoid problem of switching between different systems
                    //Assuming that following commands already sent: atz ate0 atal atst32 atsw00 atsp5
                    sendMessage(" ATSP6\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READAWD_STEP5B_ATSP5;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READAWD_STEP5B_ATSP5: {
                    sendMessage(" ATSP5\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READAWD_STEP6_ATSH8522FC;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READAWD_STEP6_ATSH8522FC: {
                    //Assuming that following commands already sent: atz ate0 atal atst32 atsw00 atsp5
                    sendMessage(" ATSH8522FC\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READAWD_STEP7_2211100401;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READAWD_STEP7_2211100401: {
                    sendMessage(" 2211100401\r");
                    m_sWaitingForString = ">";
                    m_iNextAction = Constants.CVTDIAG_NEXT_READAWD_STEP8_RESULT;
                    break;
                }
                case Constants.CVTDIAG_NEXT_READAWD_STEP8_RESULT: {
                    String sIncomingBufferLocalCopy = m_sIncomingBuffer;
                    m_sWaitingForString = "";
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\r", " ");
                    sIncomingBufferLocalCopy = sIncomingBufferLocalCopy.replace("\n", " ");
                    if (!m_bDisableChatLogging) {
                        BluetoothChatFragment.WriteCvtLog(Constants.CVTDIAG_LOGFILE_GENERAL, "READAWD RESULT: " + sIncomingBufferLocalCopy);
                    }
                    m_sAwdDataFromIncomingBuffer = sIncomingBufferLocalCopy;
                    m_iNextAction = Constants.CVTDIAG_NEXT_DTC_STEP5_ATSP6; //Return to reading on CVT data
                    NextAction();
                    break;
                }

            }
        }

    }

    public final class CvtDataDump {
        String m_sCvtDataDump;
        boolean m_bDumpValid;
        int[] m_DataArrayInt;
        public String m_sTime;
        public String m_sDataLeverPosition;
        public String m_sDataVehicleSpeed;
        public String m_sDataGSpeed;
        public String m_sDataEngSpeedSig;
        public String m_sDataAtfTemp;
        public String m_sDataAtfTempCount;
        public String m_sDataGearRatio;
        public String m_sDataAccPedalOpen;
        public String m_sDataStmStep;
        public String m_sDataEngineTorque;
        public String m_sDataPriPress;
        public String m_sDataSecPress;
        public String m_sDataTgtSecPrs;
        public String m_sDataLuPrs;
        public String m_sDataLinePrs;
        public String m_sDataIsolT1;
        public String m_sDataTrqRto;
        public String m_sDataSlipRev;
        public String m_sAwdDataDump;
        int[] m_AwdDataArrayInt;
        public String m_sAwdDataEtsSolenoid;
        public String m_sAwdDataEtsSolenoidRatio;
        public String m_sEcuDataDump_CoolanTemp;
        public String m_sEcuDataDump_InjPulse;
        public String m_sEcuDataDump_EngSpeed;
        public String m_sEcuDataDump_VehicleSpeed;
        int[] m_EcuDataArrayInt;
        public String m_sEcuCoolanTemp;
        public String m_sEcuInjPulse;
        public String m_sEcuEngSpeed;
        public String m_sEcuVehicleSpeed;
        public String m_sEcuInstantConsumptionLiterPerHour;
        public String m_sEcuInstantConsumptionLiterPer100Km;
        public String m_sEcuDataDump_DTC;
        public String m_sCvtfDeteriorationDate;
        public String m_sCvtfDeteriorationDateDelta;
        public int m_iEcuDtcFound;
        public int m_iCvtDtcCount;
        public int m_iDataVehicleSpeed;
        public double m_dDataGSpeed;
        public int m_iDataEngSpeedSig;
        public int m_iDataAtfTemp;
        public int m_iDataAtfTempCount;
        public double m_dDataGearRatio;
        public double m_dDataAccPedalOpen;
        public int m_iDataStmStep;
        public double m_dDataEngineTorque;
        public double m_dDataPriPress;
        public double m_dDataSecPress;
        public double m_dDataTgtSecPrs;
        public double m_dDataLuPrs;
        public double m_dDataLinePrs;
        public double m_dDataIsolT1;
        public double m_dDataTrqRto;
        public int m_iDataSlipRev;
        public boolean m_bDataBrakeSw;
        public double m_dAwdDataEtsSolenoid;
        public int m_iEcuCoolanTemp;
        public double m_dEcuInstantConsumptionLiterPerHour;
        public double m_dEcuInstantConsumptionLiterPer100Km;
        public int m_iCvtfDeteriorationDate;
        public int m_iCvtfDeteriorationDateFirst;


        public CvtDataDump(String sCvtDataDump, String sAwdDataDump, String sEcuDataDump_CoolanTemp, String sEcuDataDump_InjPulse, String sEcuDataDump_EngSpeed, String sEcuDataDump_VehicleSpeed, String sEcuDataDump_DTC, int iCvtDtcCount, int iCvtfDeteriorationDate, int iCvtfDeteriorationDateFirst) {
            m_bDumpValid = false;
            m_sCvtDataDump = sCvtDataDump;
            m_sAwdDataDump = sAwdDataDump;
            m_DataArrayInt = new int[76];
            m_AwdDataArrayInt = new int[1]; m_AwdDataArrayInt[0] = 0;
            m_sEcuDataDump_CoolanTemp = sEcuDataDump_CoolanTemp;
            m_sEcuDataDump_InjPulse = sEcuDataDump_InjPulse;
            m_sEcuDataDump_EngSpeed = sEcuDataDump_EngSpeed;
            m_sEcuDataDump_VehicleSpeed = sEcuDataDump_VehicleSpeed;
            m_EcuDataArrayInt = new int[4]; m_EcuDataArrayInt[0] = 0; m_EcuDataArrayInt[1] = 0; m_EcuDataArrayInt[2] = 0; m_EcuDataArrayInt[3] = 0;
            m_sEcuDataDump_DTC = sEcuDataDump_DTC;
            m_iEcuDtcFound = 0;
            m_iCvtDtcCount = iCvtDtcCount;
            m_iCvtfDeteriorationDate = iCvtfDeteriorationDate;
            m_iCvtfDeteriorationDateFirst = iCvtfDeteriorationDateFirst;
            ParseDumpToIntArray();
            if (!m_bDumpValid) { return; }
            ParseArrayToParams();
        }

        public boolean IsDumpValid() {
            return m_bDumpValid;
        }

        // convert cvt reply to array of integer values
        private void ParseDumpToIntArray() {
            m_sCvtDataDump = m_sCvtDataDump.replaceAll("[^0-9A-F:]", ""); // remove all non HEX number symbols except ":" so now we will have reply with no any spaces
            if (m_sCvtDataDump.length()<3) {
                return;
            }
            if ( !m_sCvtDataDump.substring(0,3).equalsIgnoreCase("046") ) { // dump shall start with 046 - length of answer that we can parse
                return;
            }
            m_sCvtDataDump = m_sCvtDataDump.substring(3); // remove header 046
            m_sCvtDataDump = m_sCvtDataDump.replaceAll("[0-9A-F]:", ""); // remove markers in beginning of each line 0: 1: ... A:
            // now we have pure bytestream of CVT parameters
            if (76*2 != m_sCvtDataDump.length()) { // length of reply of known format is 76 hex values 2 chars each
                return;
            }
            for (int i = 0; i <= 75; i++) {
                m_DataArrayInt[i] = Integer.parseInt(m_sCvtDataDump.substring(i*2, i*2+2), 16);
            }
            m_bDumpValid = true;
            //Parse ECU data
            m_sEcuDataDump_CoolanTemp = m_sEcuDataDump_CoolanTemp.replaceAll("BUS", ""); // Since this is first param after connecting to ECU, it will contain "BUS INIT: OK". "B" is valid hex char, so remove the word
            m_sEcuDataDump_CoolanTemp = m_sEcuDataDump_CoolanTemp.replaceAll("[^0-9A-F]", ""); // remove all non HEX number symbols so now we will have reply with no any spaces
            if (m_sEcuDataDump_CoolanTemp.length()>=8 && m_sEcuDataDump_CoolanTemp.substring(0,6).equalsIgnoreCase("621101")) { // dump of coolant temp shall start with 621101
                m_sEcuDataDump_CoolanTemp = m_sEcuDataDump_CoolanTemp.substring(6); // remove header 621101; now string has only one hex value
                m_EcuDataArrayInt[0] = Integer.parseInt(m_sEcuDataDump_CoolanTemp.substring(0, 2), 16);
            }
            //Parse ECU data
            m_sEcuDataDump_InjPulse = m_sEcuDataDump_InjPulse.replaceAll("[^0-9A-F]", ""); // remove all non HEX number symbols except ":" so now we will have reply with no any spaces
            if (m_sEcuDataDump_InjPulse.length()>=10 && m_sEcuDataDump_InjPulse.substring(0,6).equalsIgnoreCase("621206")) { // dump of inj pulse shall start with 621206
                m_sEcuDataDump_InjPulse = m_sEcuDataDump_InjPulse.substring(6); // remove header 621101; now string has only one hex value
                m_EcuDataArrayInt[1] = Integer.parseInt(m_sEcuDataDump_InjPulse.substring(0, 2), 16) * 256 + Integer.parseInt(m_sEcuDataDump_InjPulse.substring(2, 4), 16);
            }
            //Parse ECU data
            m_sEcuDataDump_EngSpeed = m_sEcuDataDump_EngSpeed.replaceAll("[^0-9A-F]", ""); // remove all non HEX number symbols except ":" so now we will have reply with no any spaces
            if (m_sEcuDataDump_EngSpeed.length()>=10 && m_sEcuDataDump_EngSpeed.substring(0,6).equalsIgnoreCase("621201")) { // dump of eng speed shall start with 621201
                m_sEcuDataDump_EngSpeed = m_sEcuDataDump_EngSpeed.substring(6); // remove header 621201; now string has only one hex value
                m_EcuDataArrayInt[2] = Integer.parseInt(m_sEcuDataDump_EngSpeed.substring(0, 2), 16) * 256 + Integer.parseInt(m_sEcuDataDump_EngSpeed.substring(2, 4), 16);
            }
            //Parse ECU data
            m_sEcuDataDump_VehicleSpeed = m_sEcuDataDump_VehicleSpeed.replaceAll("[^0-9A-F]", ""); // remove all non HEX number symbols except ":" so now we will have reply with no any spaces
            if (m_sEcuDataDump_VehicleSpeed.length()>=8 && m_sEcuDataDump_VehicleSpeed.substring(0,6).equalsIgnoreCase("621102")) { // dump of vehicle speed shall start with 621101
                m_sEcuDataDump_VehicleSpeed = m_sEcuDataDump_VehicleSpeed.substring(6); // remove header 621102; now string has only one hex value
                m_EcuDataArrayInt[3] = Integer.parseInt(m_sEcuDataDump_VehicleSpeed.substring(0, 2), 16);
            }
            //Parse ECU DTC
            m_sEcuDataDump_DTC = m_sEcuDataDump_DTC.replaceAll("[^0-9A-F]", ""); // remove all non HEX number symbols so now we will have reply with no any spaces
            if (m_sEcuDataDump_DTC.length()>=4 && m_sEcuDataDump_DTC.substring(0,2).equalsIgnoreCase("E3") && !m_sEcuDataDump_DTC.substring(2,4).equalsIgnoreCase("00")) {
                m_iEcuDtcFound = 1;
            }

            //now parse AWD data, remove "BUS INIT: OK" first
            m_sAwdDataDump = m_sAwdDataDump.replaceAll("BUS", "");
            m_sAwdDataDump = m_sAwdDataDump.replaceAll("[^0-9A-F]", ""); // remove all non HEX number symbols except ":" so now we will have reply with no any spaces
            if (m_sAwdDataDump.length()>=6 && m_sAwdDataDump.substring(0,6).equalsIgnoreCase("621110")) {// dump shall start with 046 - length of answer that we can parse
                m_sAwdDataDump = m_sAwdDataDump.substring(6); // remove header 621110; now AWD string has only one hex value
                if (1*2 != m_sAwdDataDump.length()) { // length of reply of known format is 76 hex values 2 chars each
                    return;
                }
                for (int i = 0; i <= 0; i++) {
                    m_AwdDataArrayInt[i] = Integer.parseInt(m_sAwdDataDump.substring(i*2, i*2+2), 16);
                }
            }

        }
        private void ParseArrayToParams() {
            // Time
            Time t = new Time();
            t.setToNow();
            m_sTime = t.format("%H:%M:%S");

            // Lever Position from 7_6 [54]
            if (0 == m_DataArrayInt[54])
                m_sDataLeverPosition = "P";
            else if (9 == m_DataArrayInt[54])
                m_sDataLeverPosition = "R";
            else if (3 == m_DataArrayInt[54])
                m_sDataLeverPosition = "N";
            else if (31 == m_DataArrayInt[54])
                m_sDataLeverPosition = "D";
            else
                m_sDataLeverPosition = "?";

            // VEHICLE SPEED (km/h) in 1_6 [12]
            m_iDataVehicleSpeed = m_DataArrayInt[12];
            m_sDataVehicleSpeed = String.format("%d", m_DataArrayInt[12]);

            // G SPEED (G) in 2_6 [19]
            m_dDataGSpeed = ( (double)((m_DataArrayInt[19] > 127) ? (m_DataArrayInt[19] - 256) : m_DataArrayInt[19]) )/32;
            m_sDataGSpeed = String.format("%.2f", m_dDataGSpeed);

            // ENG SPEED SIG (rpm) in 1_1 [7]
            m_iDataEngSpeedSig = m_DataArrayInt[7] * 32;
            m_sDataEngSpeedSig = String.format("%d", m_iDataEngSpeedSig);

            // ATF TEMPerature in 4_4 [31]
            m_iDataAtfTemp = AtfTempCountToCelcius(m_DataArrayInt[31]);
            m_sDataAtfTemp = String.format("%d", m_iDataAtfTemp);

            // ATF TEMP COUNT - raw value in 4_4 [31]
            m_iDataAtfTempCount = m_DataArrayInt[31];
            m_sDataAtfTempCount = String.format("%d", m_iDataAtfTempCount);

            // GEAR RATIO in 2_5 [18]
            m_dDataGearRatio = (double)(m_DataArrayInt[18]) / 100;
            m_sDataGearRatio = String.format("%.2f", m_dDataGearRatio);

            // ACC PEDAL OPEN (0.0/8) in 3_0 [20]
            m_dDataAccPedalOpen = ( (double) m_DataArrayInt[20]) / (double)32;
            m_sDataAccPedalOpen = String.format("%.1f", m_dDataAccPedalOpen);

            // STM STEP (step) in 5_1 [35]
            m_iDataStmStep = m_DataArrayInt[35] - 30;
            m_sDataStmStep = String.format("%d", m_iDataStmStep);

            // Engine Torque (N*m) in 3_6 [26]
            m_dDataEngineTorque = (double) ((m_DataArrayInt[26] > 127) ? (m_DataArrayInt[26] - 256) : m_DataArrayInt[26]) * 6.4;
            m_sDataEngineTorque = String.format("%.1f", m_dDataEngineTorque);

            // PRI PRESS (MPa) in 4_3 [30]
            m_dDataPriPress = ( (double) (m_DataArrayInt[30]) ) * 0.025;
            m_sDataPriPress = String.format("%.2f", m_dDataPriPress);

            // SEC PRESS (MPa) in 4_2 [29]
            m_dDataSecPress = ( (double) (m_DataArrayInt[29]) ) * 0.025;
            m_sDataSecPress = String.format("%.2f", m_dDataSecPress);

            // TGT SEC PRESS (MPa) in 5_4 [38]
            m_dDataTgtSecPrs = ( (double) (m_DataArrayInt[38]) ) * 0.025;
            m_sDataTgtSecPrs = String.format("%.2f", m_dDataTgtSecPrs);

            // LU PRS (MPa) in 5_2 [36]
            m_dDataLuPrs = ( (double) (m_DataArrayInt[36]) ) * 0.025;
            m_sDataLuPrs = String.format("%.2f", m_dDataLuPrs);

            // LINE PRS (MPa) in 5_3 [37]
            m_dDataLinePrs = ( (double) (m_DataArrayInt[37]) ) * 0.025;
            m_sDataLinePrs = String.format("%.2f",m_dDataLinePrs);

            // ISOLT1 (A) in 6_1,6_2 [42],[43]
            m_dDataIsolT1 = ( ( (double) (m_DataArrayInt[42]) ) * ( (double) 255 ) + ( (double) (m_DataArrayInt[43]) ) ) / 1000;
            m_sDataIsolT1 = String.format("%.2f",m_dDataIsolT1);

            // TRQ RTO in 4_1 [28]
            m_dDataTrqRto = ( (double) m_DataArrayInt[28]) / (double)64;
            m_sDataTrqRto = String.format("%.2f", m_dDataTrqRto);

            // SLIP REV in 2_4 [17]
            m_iDataSlipRev = (m_DataArrayInt[17] > 127) ? (m_DataArrayInt[17] - 256) : m_DataArrayInt[17];
            m_sDataSlipRev = String.format("%d", m_iDataSlipRev);

            // BRAKE SW in 8_0 [55]
            m_bDataBrakeSw = (8 == (8 & m_DataArrayInt[55]));

            // ETS SOLENOID (A) in AWD [0]
            m_dAwdDataEtsSolenoid = ( (double) (m_AwdDataArrayInt[0]) ) * 0.02;
            m_sAwdDataEtsSolenoid = String.format("%.2f",m_dAwdDataEtsSolenoid);
            int iAwdDataEtsSolenoidPercentage = (int) ( (m_dAwdDataEtsSolenoid / 1.8 ) * 50 );
            if (iAwdDataEtsSolenoidPercentage > 50) iAwdDataEtsSolenoidPercentage = 50;
            m_sAwdDataEtsSolenoidRatio = String.format("%d%%:%d%%",100-iAwdDataEtsSolenoidPercentage, iAwdDataEtsSolenoidPercentage);

            // ECU
            m_iEcuCoolanTemp = m_EcuDataArrayInt[0] - 50;
            m_sEcuCoolanTemp = String.format("%d", m_iEcuCoolanTemp);
            double dEcuInjPulse = m_EcuDataArrayInt[1] * 0.01;
            m_sEcuInjPulse = String.format("%.2f",dEcuInjPulse);
            int iEcuEngSpeed = (int) (m_EcuDataArrayInt[2] * 12.5);
            m_sEcuEngSpeed = String.format("%d", iEcuEngSpeed);
            int iEcuVehicleSpeed = m_EcuDataArrayInt[3] * 2;
            m_sEcuVehicleSpeed = String.format("%d", iEcuVehicleSpeed);
            m_dEcuInstantConsumptionLiterPerHour = (dEcuInjPulse*0.001) * (iEcuEngSpeed/60)/2 * 6 * (0.296/60) * 3600;
            m_sEcuInstantConsumptionLiterPerHour = String.format("%.1f",m_dEcuInstantConsumptionLiterPerHour);
            if (0 != iEcuVehicleSpeed) {
                m_dEcuInstantConsumptionLiterPer100Km = m_dEcuInstantConsumptionLiterPerHour * 100 / iEcuVehicleSpeed;
                m_sEcuInstantConsumptionLiterPer100Km = String.format("%.1f", m_dEcuInstantConsumptionLiterPer100Km);
            }
            else {
                m_dEcuInstantConsumptionLiterPer100Km = 0;
                m_sEcuInstantConsumptionLiterPer100Km = "∞";
            }

            // CVTF DETERIORATION
            m_sCvtfDeteriorationDate = String.format("%d", m_iCvtfDeteriorationDate);
            m_sCvtfDeteriorationDateDelta = String.format("%d", m_iCvtfDeteriorationDate-m_iCvtfDeteriorationDateFirst);

        }

        public String GetTextStringForDisplay() {
            String sDisplayString;
            sDisplayString =
                    m_sTime + "  " + m_sDataLeverPosition + "  " + m_sDataVehicleSpeed + " km/h" + "  " + m_sDataEngSpeedSig + " rpm"
                    + "\r\nAccel: " + m_sDataAccPedalOpen + "/8" + "  CVTF Temp: " + m_sDataAtfTemp + "°C"
                    + "\r\nGear Ratio: " + m_sDataGearRatio + "  Stm Step: " + m_sDataStmStep
                    + "\r\nTarget Sec Prs: " + m_sDataTgtSecPrs + "MPa"
                    + "\r\nSec Prs: " + m_sDataSecPress + "MPa"
                    + "\r\nPri Prs: " + m_sDataPriPress + "MPa" + "  Line Prs: " + m_sDataLinePrs + "MPa"
                    + "\r\nLu Prs: " + m_sDataLuPrs + "MPa" + "  IsolT1: " + m_sDataIsolT1 + "A"
                    + "\r\nAwd Ets Solenoid: " + m_sAwdDataEtsSolenoid + "A" + "  (" + m_sAwdDataEtsSolenoidRatio + ")"
                    + "\r\nEngine: " + m_sEcuCoolanTemp + "°C  " + m_sEcuInstantConsumptionLiterPerHour + "L/h  " + m_sEcuInstantConsumptionLiterPer100Km + "L/100km"
//                    + "\r\n" + m_sEcuEngSpeed + "rpm  " + m_sEcuVehicleSpeed + "km/h  " + m_sEcuInjPulse + "ms" // Information from ECU, duplicates what we already printed from CVT dump
            ;
            if (0 != m_iEcuDtcFound || 0 != m_iCvtDtcCount) {
                sDisplayString += "\r\nDTC DETECTED:";
                if (0 != m_iEcuDtcFound)
                    sDisplayString += " ECU";
                if (0 != m_iCvtDtcCount)
                    sDisplayString += " CVT";
            }
            return sDisplayString;
        }

        public String GetTextStringForFile() {
            String sLogString;

            sLogString =
                    m_sDataLeverPosition + "\t" + m_sDataVehicleSpeed + "\t" + m_sDataEngSpeedSig
                    + "\t" + m_sDataAccPedalOpen + "\t" + m_sDataAtfTemp + "\t" + m_sDataGearRatio
                    + "\t" + m_sDataStmStep + "\t" + m_sDataTgtSecPrs  + "\t" + m_sDataSecPress
                    + "\t" + m_sDataPriPress + "\t" + m_sDataLinePrs + "\t" + m_sDataLuPrs + "\t"
                    + m_sDataIsolT1 + "\t" + m_sAwdDataEtsSolenoid + "\t" + m_sCvtfDeteriorationDate
            ;
            if (0 != m_iEcuDtcFound || 0 != m_iCvtDtcCount) {
                sLogString += "\t";
                if (0 != m_iEcuDtcFound)
                    sLogString += "ECU";
                if (0 != m_iCvtDtcCount)
                    sLogString += "CVT";
            }
            return sLogString;
        }

        private int AtfTempCountToCelcius(int iAtfTempCount) {
            if (iAtfTempCount > 243) return 500;
            int ReferenceTempCounts[] =  {  4,   8,  13, 17, 21, 27, 32, 39, 47, 55, 64, 73, 83, 93, 104, 114, 124, 134, 143, 152, 161, 169, 177, 183, 190, 196, 201, 206, 210, 214, 218, 221, 224, 227, 229, 231, 233, 235, 236, 238, 239, 241, 243};
            int ReferenceTempCelcius[] = {-30, -20, -10, -5,  0,  5, 10, 15, 20, 25, 30, 35, 40, 45,  50,  55,  60,  65,  70,  75,  80,  85,  90,  95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, 165, 170, 175, 180, 190, 200};
            for (int i=0; i<= 41; i++) {
                if (iAtfTempCount >= ReferenceTempCounts[i] && iAtfTempCount < ReferenceTempCounts[i+1]) {
                    return ReferenceTempCelcius[i] + (ReferenceTempCelcius[i+1]-ReferenceTempCelcius[i]) * (iAtfTempCount-ReferenceTempCounts[i]) / (ReferenceTempCounts[i+1]-ReferenceTempCounts[i]) ;
                }
            }
            return -50;
        }
    }
}
