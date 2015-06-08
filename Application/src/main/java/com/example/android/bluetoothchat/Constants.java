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

/**
 * Defines several constants used between {@link BluetoothChatService} and the UI.
 */
public interface Constants {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_OBDTIMER = 6;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // CVT Diag actions
    public static final int CVTDIAG_ACTION_READDTC = 1;
    public static final int CVTDIAG_ACTION_READDETERIORATION = 2;
    public static final int CVTDIAG_ACTION_READPARAMS = 3;
    // CVT Diag next ation IDs
    public static final int CVTDIAG_NEXT_DTC_STEP1_ATZ = 10;
    public static final int CVTDIAG_NEXT_DTC_STEP1A_ATE0 = 11;
    public static final int CVTDIAG_NEXT_DTC_STEP1B_ATE0 = 12;
    public static final int CVTDIAG_NEXT_DTC_STEP2_ATAL = 20;
    public static final int CVTDIAG_NEXT_DTC_STEP3_ATST32 = 30;
    public static final int CVTDIAG_NEXT_DTC_STEP4_ATSW00 = 40;
    public static final int CVTDIAG_NEXT_DTC_STEP5_ATSP6 = 50;
    public static final int CVTDIAG_NEXT_DTC_STEP6_ATSH7E1 = 60;
    public static final int CVTDIAG_NEXT_DTC_STEP7_10C0 = 70;
    public static final int CVTDIAG_NEXT_DTC_STEP8_17FF00 = 80;
    public static final int CVTDIAG_NEXT_DTC_STEP9_RESULT = 81;
    public static final int CVTDIAG_NEXT_DETERIORATION_STEP8_2103 = 90;
    public static final int CVTDIAG_NEXT_DETERIORATION_STEP9_RESULT = 91;
    public static final int CVTDIAG_NEXT_READPARAMS_STEP8_2101 = 100;
    public static final int CVTDIAG_NEXT_READPARAMS_STEP9_RESULT = 101;
    public static final int CVTDIAG_NEXT_READECU_STEP5_ATSP5 = 110;
    public static final int CVTDIAG_NEXT_READECU_STEP6_ATSH8110FC = 120;
    public static final int CVTDIAG_NEXT_READECU_STEP7_2211010401 = 130;
    public static final int CVTDIAG_NEXT_READECU_STEP8_2212060401 = 140;
    public static final int CVTDIAG_NEXT_READECU_STEP9_2212010401 = 150;
    public static final int CVTDIAG_NEXT_READECU_STEP10_2211020401 = 160;
    public static final int CVTDIAG_NEXT_READECU_STEP11_A330 = 170;
    public static final int CVTDIAG_NEXT_READECU_STEP12_DTCRESULT = 180;
    public static final int CVTDIAG_NEXT_READAWD_STEP5A_ATSP6 = 190;
    public static final int CVTDIAG_NEXT_READAWD_STEP5B_ATSP5 = 191;
    public static final int CVTDIAG_NEXT_READAWD_STEP6_ATSH8522FC = 200;
    public static final int CVTDIAG_NEXT_READAWD_STEP7_2211100401 = 210;
    public static final int CVTDIAG_NEXT_READAWD_STEP8_RESULT = 220;
    // CVT Diag Log File IDs
    public static final int CVTDIAG_LOGFILE_GENERAL = 1;
    public static final int CVTDIAG_LOGFILE_PARAMS = 2;


}
