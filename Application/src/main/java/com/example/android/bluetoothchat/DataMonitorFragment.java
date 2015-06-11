package com.example.android.bluetoothchat;

import android.graphics.Color;
import android.os.Bundle;
//import android.app.Fragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DataMonitor.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DataMonitor#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DataMonitorFragment extends Fragment {
    ProgressBar m_progressBarRpm;
    ProgressBar m_progressBarEngineTemp;
    ProgressBar m_progressBarCvtTemp;
    ProgressBar m_progressBarClutchLockup;
    ProgressBar m_progressBarAwdCurrent;
    ProgressBar m_progressBarAccelerator;
    ProgressBar m_progressBarSecPrsTarget;
    ProgressBar m_progressBarSecPrs;
    ProgressBar m_progressBarGearRatio;
    ProgressBar m_progressBarStepMotor;
    ProgressBar m_progressBarPriPrs;
    ProgressBar m_progressBarLinePrs;
    ProgressBar m_progressBarConsumptionLH;
    ProgressBar m_progressBarConsumptionL100Km;
    TextView m_textViewRpm;
    TextView m_textViewEngineTemp;
    TextView m_textViewCvtTemp;
    TextView m_textViewCvtTempIndicator;
    TextView m_textViewSpeed;
    TextView m_textViewLeverPosition;
    TextView m_textViewClutchLockup;
    TextView m_textViewAwdCurrent;
    TextView m_textViewAccelerator;
    TextView m_textViewSecPrsTarget;
    TextView m_textViewSecPrs;
    TextView m_textViewVirtualGear;
    TextView m_textViewGearRatio;
    TextView m_textViewStepMotor;
    TextView m_textViewPriPrs;
    TextView m_textViewLinePrs;
    TextView m_textViewAwdRatio;
    TextView m_textViewConsumptionLH;
    TextView m_textViewConsumptionL100Km;
    TextView m_textViewDtc;
    TextView m_textViewDeterioration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_monitor, container, false);
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        m_progressBarRpm = (ProgressBar) view.findViewById(R.id.progressBarRpm);
        m_progressBarAccelerator = (ProgressBar) view.findViewById(R.id.progressBarAccelerator);
        m_progressBarConsumptionLH = (ProgressBar) view.findViewById(R.id.progressBarConsumptionLH);
        m_progressBarConsumptionL100Km = (ProgressBar) view.findViewById(R.id.progressBarConsumptionL100Km);
        m_progressBarEngineTemp = (ProgressBar) view.findViewById(R.id.progressBarEngineTemp);
        m_progressBarCvtTemp = (ProgressBar) view.findViewById(R.id.progressBarCvtTemp);
        m_progressBarClutchLockup = (ProgressBar) view.findViewById(R.id.progressBarClutchLockup);
        m_progressBarAwdCurrent = (ProgressBar) view.findViewById(R.id.progressBarAwdCurrent);
        m_progressBarGearRatio = (ProgressBar) view.findViewById(R.id.progressBarGearRatio);
        m_progressBarStepMotor = (ProgressBar) view.findViewById(R.id.progressBarStepMotor);
        m_progressBarSecPrsTarget = (ProgressBar) view.findViewById(R.id.progressBarSecPrsTarget);
        m_progressBarSecPrs = (ProgressBar) view.findViewById(R.id.progressBarSecPrs);
        m_progressBarPriPrs = (ProgressBar) view.findViewById(R.id.progressBarPriPrs);
        m_progressBarLinePrs = (ProgressBar) view.findViewById(R.id.progressBarLinePrs);
        m_textViewRpm = (TextView) view.findViewById(R.id.textViewRpm);
        m_textViewLeverPosition = (TextView) view.findViewById(R.id.textViewLeverPosition);
        m_textViewSpeed = (TextView) view.findViewById(R.id.textViewSpeed);
        m_textViewAccelerator = (TextView) view.findViewById(R.id.textViewAccelerator);
        m_textViewConsumptionLH = (TextView) view.findViewById(R.id.textViewConsumptionLH);
        m_textViewConsumptionL100Km = (TextView) view.findViewById(R.id.textViewConsumptionL100Km);
        m_textViewEngineTemp =  (TextView) view.findViewById(R.id.textViewEngineTemp);
        m_textViewCvtTemp = (TextView) view.findViewById(R.id.textViewCvtTemp);
        m_textViewCvtTempIndicator = (TextView) view.findViewById(R.id.textViewCvtTempIndicator);
        m_textViewClutchLockup = (TextView) view.findViewById(R.id.textViewClutchLockup);
        m_textViewAwdCurrent = (TextView) view.findViewById(R.id.textViewAwdCurrent);
        m_textViewAwdRatio = (TextView) view.findViewById(R.id.textViewAwdRatio);
        m_textViewVirtualGear = (TextView) view.findViewById(R.id.textViewVirtualGear);
        m_textViewGearRatio = (TextView) view.findViewById(R.id.textViewGearRatio);
        m_textViewStepMotor = (TextView) view.findViewById(R.id.textViewStepMotor);
        m_textViewSecPrsTarget = (TextView) view.findViewById(R.id.textViewSecPrsTarget);
        m_textViewSecPrs = (TextView) view.findViewById(R.id.textViewSecPrs);
        m_textViewPriPrs = (TextView) view.findViewById(R.id.textViewPriPrs);
        m_textViewLinePrs = (TextView) view.findViewById(R.id.textViewLinePrs);
        m_textViewDtc = (TextView) view.findViewById(R.id.textViewDtc);
        m_textViewDeterioration = (TextView) view.findViewById(R.id.textViewDeterioration);
    }

    public void setData(BluetoothChatFragment.CvtDataDump cvtDataDump) {
        m_textViewRpm.setText(cvtDataDump.m_sDataEngSpeedSig);
        m_progressBarRpm.setSecondaryProgress(m_progressBarRpm.getProgress());
        m_progressBarRpm.setProgress((int) (100 * cvtDataDump.m_iDataEngSpeedSig / 7000));
        m_textViewLeverPosition.setText(cvtDataDump.m_sDataLeverPosition);
        m_textViewSpeed.setText(cvtDataDump.m_sDataVehicleSpeed);
        m_progressBarAccelerator.setSecondaryProgress(m_progressBarAccelerator.getProgress());
        m_progressBarAccelerator.setProgress((int) (100 * cvtDataDump.m_dDataAccPedalOpen / 8));
        m_textViewAccelerator.setText(cvtDataDump.m_sDataAccPedalOpen);

        m_textViewConsumptionLH.setText(cvtDataDump.m_sEcuInstantConsumptionLiterPerHour);
        m_progressBarConsumptionLH.setSecondaryProgress(m_progressBarConsumptionLH.getProgress());
        m_progressBarConsumptionLH.setProgress((int) (100 * cvtDataDump.m_dEcuInstantConsumptionLiterPerHour / 30));
        m_progressBarConsumptionL100Km.setSecondaryProgress(m_progressBarConsumptionL100Km.getProgress());
        m_progressBarConsumptionL100Km.setProgress((int) (100 * cvtDataDump.m_dEcuInstantConsumptionLiterPer100Km / 30));
        m_textViewConsumptionL100Km.setText(cvtDataDump.m_sEcuInstantConsumptionLiterPer100Km);

        if (-50 != cvtDataDump.m_iEcuCoolanTemp) m_textViewEngineTemp.setText(cvtDataDump.m_sEcuCoolanTemp + "°C");
        else m_textViewEngineTemp.setText("∞");
        m_progressBarEngineTemp.setSecondaryProgress(m_progressBarEngineTemp.getProgress());
        m_progressBarEngineTemp.setProgress((int) (100 * (cvtDataDump.m_iEcuCoolanTemp + 20) / (120 + 20)));
        m_progressBarCvtTemp.setSecondaryProgress(m_progressBarCvtTemp.getProgress());
        m_progressBarCvtTemp.setProgress((int) (100 * (cvtDataDump.m_iDataAtfTemp + 20) / (120 + 20)));
        m_textViewCvtTemp.setText(cvtDataDump.m_sDataAtfTemp + "°C");

        String sCvtTempIndicatorString;
        String sCvtTempIndicatorColor;
        if (cvtDataDump.m_iDataAtfTemp < 20) { sCvtTempIndicatorString = "COLD"; sCvtTempIndicatorColor = "#FF20A0E0"; }
        else if (cvtDataDump.m_iDataAtfTemp < 50) { sCvtTempIndicatorString = "WARM"; sCvtTempIndicatorColor = "#FF00C0C0"; }
        else if (cvtDataDump.m_iDataAtfTemp < 90) { sCvtTempIndicatorString = "OK"; sCvtTempIndicatorColor = "#FF00AA00"; }
        else if (cvtDataDump.m_iDataAtfTemp < 100) { sCvtTempIndicatorString = "HOT"; sCvtTempIndicatorColor = "#FFFF8000"; }
        else { sCvtTempIndicatorString = "HOTTER"; sCvtTempIndicatorColor = "#FFFF0000"; }
        m_textViewCvtTempIndicator.setText(sCvtTempIndicatorString);
        m_textViewCvtTempIndicator.setBackgroundColor(Color.parseColor(sCvtTempIndicatorColor));

        m_textViewClutchLockup.setText(cvtDataDump.m_sDataIsolT1+"A");
        m_progressBarClutchLockup.setSecondaryProgress(m_progressBarClutchLockup.getProgress());
        m_progressBarClutchLockup.setProgress((int)(100*cvtDataDump.m_dDataIsolT1/0.7));
        m_progressBarAwdCurrent.setSecondaryProgress(m_progressBarAwdCurrent.getProgress());
        m_progressBarAwdCurrent.setProgress((int)(100*cvtDataDump.m_dAwdDataEtsSolenoid/1.8));
        m_textViewAwdCurrent.setText(cvtDataDump.m_sAwdDataEtsSolenoid+"A");
        m_textViewAwdRatio.setText(cvtDataDump.m_sAwdDataEtsSolenoidRatio);

        double dGr = cvtDataDump.m_dDataGearRatio;
        String sVirtualGear;
        if (dGr <= 0.44) sVirtualGear = "6";
        else if (dGr <= 0.69) sVirtualGear = "5+";
        else if (dGr <= 0.86) sVirtualGear = "4+";
        else if (dGr <= 1.11) sVirtualGear = "3+";
        else if (dGr <= 1.53) sVirtualGear = "2+";
        else sVirtualGear = "1+";
        m_textViewVirtualGear.setText(sVirtualGear);
        m_textViewGearRatio.setText(cvtDataDump.m_sDataGearRatio);
        m_progressBarGearRatio.setSecondaryProgress(m_progressBarGearRatio.getProgress());
        m_progressBarGearRatio.setProgress(100 - (int) (100 * (cvtDataDump.m_dDataGearRatio - 0.43) / (2.37 - 0.43)));
        m_progressBarStepMotor.setSecondaryProgress(m_progressBarStepMotor.getProgress());
        m_progressBarStepMotor.setProgress((int) (100 * (cvtDataDump.m_iDataStmStep + 20) / (190 + 20)));
        m_textViewStepMotor.setText(cvtDataDump.m_sDataStmStep);

        m_textViewSecPrsTarget.setText(cvtDataDump.m_sDataTgtSecPrs);
        m_progressBarSecPrsTarget.setSecondaryProgress(m_progressBarSecPrsTarget.getProgress());
        m_progressBarSecPrsTarget.setProgress((int) (100 * cvtDataDump.m_dDataTgtSecPrs / 6.375));
        m_progressBarSecPrs.setSecondaryProgress(m_progressBarSecPrs.getProgress());
        m_progressBarSecPrs.setProgress((int) (100 * cvtDataDump.m_dDataSecPress / 6.375));
        m_textViewSecPrs.setText(cvtDataDump.m_sDataSecPress);

        m_textViewPriPrs.setText(cvtDataDump.m_sDataPriPress);
        m_progressBarPriPrs.setSecondaryProgress(m_progressBarPriPrs.getProgress());
        m_progressBarPriPrs.setProgress((int) (100 * cvtDataDump.m_dDataPriPress / 6.375));
        m_progressBarLinePrs.setSecondaryProgress(m_progressBarLinePrs.getProgress());
        m_progressBarLinePrs.setProgress((int) (100 * cvtDataDump.m_dDataLinePrs / 6.375));
        m_textViewLinePrs.setText(cvtDataDump.m_sDataLinePrs);

        m_textViewDeterioration.setText(cvtDataDump.m_sCvtfDeteriorationDate + " (+" + cvtDataDump.m_sCvtfDeteriorationDateDelta + ")");

        String sDtc;
        if (0 == cvtDataDump.m_iCvtDtcCount && 0 == cvtDataDump.m_iEcuDtcFound)
            sDtc = cvtDataDump.m_sTime;
        else {
            sDtc = "DTC:";
            if (0 != cvtDataDump.m_iCvtDtcCount)
                sDtc += " CVT";
            if (0 != cvtDataDump.m_iEcuDtcFound)
                sDtc += " ECU";
        }
        m_textViewDtc.setText(sDtc);
    }
}
