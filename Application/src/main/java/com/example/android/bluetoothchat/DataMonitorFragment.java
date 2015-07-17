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
    ProgressBar m_progressBarSlipRev;
    ProgressBar m_progressBarAwdCurrent;
    ProgressBar m_progressBarAccelerator;
    ProgressBar m_progressBarSecPrsTarget;
    ProgressBar m_progressBarSecPrs;
    ProgressBar m_progressBarGearRatio;
    ProgressBar m_progressBarStepMotor;
    ProgressBar m_progressBarPriPrs;
    ProgressBar m_progressBarLinePrs;
    ProgressBar m_progressBarConsumptionL100Km;
    TextView m_textViewRpm;
    TextView m_textViewEngineTemp;
    TextView m_textViewEngineTempIndicator;
    TextView m_textViewCvtTemp;
    TextView m_textViewCvtTempCount;
    TextView m_textViewCvtTempIndicator;
    TextView m_textViewSpeed;
    TextView m_textViewGSpeed;
    TextView m_textViewLeverPosition;
    TextView m_textViewClutchLockup;
    TextView m_textViewClutchLockupIndicator;
    TextView m_textViewSlipRev;
    TextView m_textViewTorqueConverterRatio;
    TextView m_textViewAwdCurrent;
    TextView m_textViewAccelerator;
    TextView m_textViewSecPrsTarget;
    TextView m_textViewSecPrs;
    TextView m_textViewVirtualGear;
    TextView m_textViewGearRatio;
    TextView m_textViewStepMotor;
    TextView m_textViewFinalTorque;
    TextView m_textViewEngineTorque;
    TextView m_textViewEngineHorsePower;
    TextView m_textViewPriPrs;
    TextView m_textViewPriPrsTestTitle;
    TextView m_textViewPriPrsTestResult;
    TextView m_textViewLinePrs;
    TextView m_textViewLuPrs;
    TextView m_textViewAwdRatio;
    TextView m_textViewConsumptionLH;
    TextView m_textViewConsumptionL100Km;
    TextView m_textViewConsumptionL100KmAverage;
    TextView m_textViewConsumptionTotalLiters;
    TextView m_textViewConsumptionTotalDistance;
    TextView m_textViewDtc;
    TextView m_textViewDeterioration;
    double m_dDiagnosticPriPrsTestAverage = 0;
    double m_dDiagnosticPriPrsTestMinimum = 0;
    int m_dDiagnosticPriPrsTestPointsCount = 0;
    double m_dAverageConsumptionTotalDistance = 0;
    double m_dAverageConsumptionTotalLiters = 0;
    BluetoothChatFragment.CvtDataDump m_cvtDataDumpPrevious = null;

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
        m_progressBarConsumptionL100Km = (ProgressBar) view.findViewById(R.id.progressBarConsumptionL100Km);
        m_progressBarEngineTemp = (ProgressBar) view.findViewById(R.id.progressBarEngineTemp);
        m_progressBarCvtTemp = (ProgressBar) view.findViewById(R.id.progressBarCvtTemp);
        m_progressBarClutchLockup = (ProgressBar) view.findViewById(R.id.progressBarClutchLockup);
        m_progressBarSlipRev = (ProgressBar) view.findViewById(R.id.progressBarSlipRev);
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
        m_textViewGSpeed = (TextView) view.findViewById(R.id.textViewGSpeed);
        m_textViewAccelerator = (TextView) view.findViewById(R.id.textViewAccelerator);
        m_textViewConsumptionLH = (TextView) view.findViewById(R.id.textViewConsumptionLH);
        m_textViewConsumptionL100Km = (TextView) view.findViewById(R.id.textViewConsumptionL100Km);
        m_textViewConsumptionL100KmAverage = (TextView) view.findViewById(R.id.textViewConsumptionL100KmAverage);
        m_textViewConsumptionTotalLiters = (TextView) view.findViewById(R.id.textViewConsumptionTotalLiters);
        m_textViewConsumptionTotalDistance = (TextView) view.findViewById(R.id.textViewConsumptionTotalDistance);
        m_textViewEngineTemp =  (TextView) view.findViewById(R.id.textViewEngineTemp);
        m_textViewEngineTempIndicator =  (TextView) view.findViewById(R.id.textViewEngineTempIndicator);
        m_textViewCvtTemp = (TextView) view.findViewById(R.id.textViewCvtTemp);
        m_textViewCvtTempCount = (TextView) view.findViewById(R.id.textViewCvtTempCount);
        m_textViewCvtTempIndicator = (TextView) view.findViewById(R.id.textViewCvtTempIndicator);
        m_textViewClutchLockup = (TextView) view.findViewById(R.id.textViewClutchLockup);
        m_textViewClutchLockupIndicator = (TextView) view.findViewById(R.id.textViewClutchLockupIndicator);
        m_textViewSlipRev = (TextView) view.findViewById(R.id.textViewSlipRev);
        m_textViewTorqueConverterRatio = (TextView) view.findViewById(R.id.textViewTorqueConverterRatio);
        m_textViewAwdCurrent = (TextView) view.findViewById(R.id.textViewAwdCurrent);
        m_textViewAwdRatio = (TextView) view.findViewById(R.id.textViewAwdRatio);
        m_textViewVirtualGear = (TextView) view.findViewById(R.id.textViewVirtualGear);
        m_textViewGearRatio = (TextView) view.findViewById(R.id.textViewGearRatio);
        m_textViewStepMotor = (TextView) view.findViewById(R.id.textViewStepMotor);
        m_textViewFinalTorque = (TextView) view.findViewById(R.id.textViewFinalTorque);
        m_textViewEngineTorque = (TextView) view.findViewById(R.id.textViewEngineTorque);
        m_textViewEngineHorsePower = (TextView) view.findViewById(R.id.textViewEngineHorsePower);
        m_textViewSecPrsTarget = (TextView) view.findViewById(R.id.textViewSecPrsTarget);
        m_textViewSecPrs = (TextView) view.findViewById(R.id.textViewSecPrs);
        m_textViewPriPrs = (TextView) view.findViewById(R.id.textViewPriPrs);
        m_textViewPriPrsTestTitle = (TextView) view.findViewById(R.id.textViewPriPrsTestTitle);
        m_textViewPriPrsTestResult = (TextView) view.findViewById(R.id.textViewPriPrsTestResult);
        m_textViewLinePrs = (TextView) view.findViewById(R.id.textViewLinePrs);
        m_textViewLuPrs = (TextView) view.findViewById(R.id.textViewLuPrs);
        m_textViewDtc = (TextView) view.findViewById(R.id.textViewDtc);
        m_textViewDeterioration = (TextView) view.findViewById(R.id.textViewDeterioration);
        m_cvtDataDumpPrevious = null;
    }

    public void setData(BluetoothChatFragment.CvtDataDump cvtDataDump) {
        m_textViewRpm.setText(cvtDataDump.m_sDataEngSpeedSig);
        m_progressBarRpm.setSecondaryProgress(m_progressBarRpm.getProgress());
        m_progressBarRpm.setProgress((int) (100 * cvtDataDump.m_iDataEngSpeedSig / 7000));
        m_textViewLeverPosition.setText(cvtDataDump.m_sDataLeverPosition);
        if (cvtDataDump.m_bDataBrakeSw) { m_textViewLeverPosition.setText("(" + m_textViewLeverPosition.getText() + ")"); }
        m_textViewSpeed.setText(cvtDataDump.m_sDataVehicleSpeed);
        m_textViewGSpeed.setText(cvtDataDump.m_sDataGSpeed);
        m_progressBarAccelerator.setSecondaryProgress(m_progressBarAccelerator.getProgress());
        m_progressBarAccelerator.setProgress((int) (100 * cvtDataDump.m_dDataAccPedalOpen / 8));
        m_textViewAccelerator.setText(cvtDataDump.m_sDataAccPedalOpen);

        m_textViewConsumptionLH.setText(cvtDataDump.m_sEcuInstantConsumptionLiterPerHour);
        m_progressBarConsumptionL100Km.setSecondaryProgress(m_progressBarConsumptionL100Km.getProgress());
        m_progressBarConsumptionL100Km.setProgress((int) (100 * cvtDataDump.m_dEcuInstantConsumptionLiterPer100Km / 30));
        m_textViewConsumptionL100Km.setText(cvtDataDump.m_sEcuInstantConsumptionLiterPer100Km);
        if ( null != m_cvtDataDumpPrevious && (cvtDataDump.m_iEcuVehicleSpeed > 0 || cvtDataDump.m_dEcuInstantConsumptionLiterPerHour > 0.001) ) {
            double dPeriodHours = (double)(cvtDataDump.m_tTime.toMillis(true) - m_cvtDataDumpPrevious.m_tTime.toMillis(true)) / (1000.0 * 3600.0);
            m_dAverageConsumptionTotalDistance += dPeriodHours * (double)(cvtDataDump.m_iEcuVehicleSpeed + m_cvtDataDumpPrevious.m_iEcuVehicleSpeed) / 2.0;
            m_dAverageConsumptionTotalLiters += dPeriodHours * (cvtDataDump.m_dEcuInstantConsumptionLiterPerHour + m_cvtDataDumpPrevious.m_dEcuInstantConsumptionLiterPerHour) / 2.0;
        }
        if (m_dAverageConsumptionTotalDistance > 0.1 && (cvtDataDump.m_iEcuVehicleSpeed > 0 || cvtDataDump.m_dEcuInstantConsumptionLiterPerHour > 0.001) )
            m_textViewConsumptionL100KmAverage.setText(String.format("%.1f", m_dAverageConsumptionTotalLiters * 100.0 / m_dAverageConsumptionTotalDistance));
        else
            m_textViewConsumptionL100KmAverage.setText("∞");
        m_textViewConsumptionTotalLiters.setText(String.format("%.1f", m_dAverageConsumptionTotalLiters));
        m_textViewConsumptionTotalDistance.setText(String.format("%.1f", m_dAverageConsumptionTotalDistance));

        if (-50 != cvtDataDump.m_iEcuCoolanTemp && -40 != cvtDataDump.m_iEcuCoolanTemp) {
            m_textViewEngineTemp.setText(cvtDataDump.m_sEcuCoolanTemp + "°C");
            String sEngineTempIndicatorString;
            String sEngineTempIndicatorColor;
            if (cvtDataDump.m_iEcuCoolanTemp < 20) { sEngineTempIndicatorString = "COLD"; sEngineTempIndicatorColor = "#FF20A0E0"; }
            else if (cvtDataDump.m_iEcuCoolanTemp < 80) { sEngineTempIndicatorString = "WARM"; sEngineTempIndicatorColor = "#FF00C0C0"; }
            else if (cvtDataDump.m_iEcuCoolanTemp < 100) { sEngineTempIndicatorString = "OK"; sEngineTempIndicatorColor = "#FF00AA00"; }
            else if (cvtDataDump.m_iEcuCoolanTemp < 120) { sEngineTempIndicatorString = "HOT"; sEngineTempIndicatorColor = "#FFFF8000"; }
            else { sEngineTempIndicatorString = "HOTTER"; sEngineTempIndicatorColor = "#FFFF0000"; }
            m_textViewEngineTempIndicator.setText(sEngineTempIndicatorString);
            m_textViewEngineTempIndicator.setBackgroundColor(Color.parseColor(sEngineTempIndicatorColor));
            m_textViewEngineTempIndicator.setVisibility(View.VISIBLE);
        }
        else {
            m_textViewEngineTemp.setText("∞");
            m_textViewEngineTempIndicator.setVisibility(View.INVISIBLE);
        }
        m_progressBarEngineTemp.setSecondaryProgress(m_progressBarEngineTemp.getProgress());
        m_progressBarEngineTemp.setProgress((int) (100 * (cvtDataDump.m_iEcuCoolanTemp + 20) / (120 + 20)));
        m_progressBarCvtTemp.setSecondaryProgress(m_progressBarCvtTemp.getProgress());
        m_progressBarCvtTemp.setProgress((int) (100 * (cvtDataDump.m_iDataAtfTemp + 20) / (120 + 20)));
        if (-50 != cvtDataDump.m_iDataAtfTemp) {
            m_textViewCvtTemp.setText(cvtDataDump.m_sDataAtfTemp + "°C");
            m_textViewCvtTempIndicator.setVisibility(View.VISIBLE);
        } else {
            m_textViewCvtTemp.setText("∞");
            m_textViewCvtTempIndicator.setVisibility(View.INVISIBLE);
        }
        m_textViewCvtTempCount.setText(cvtDataDump.m_sDataAtfTempCount);

        String sCvtTempIndicatorString;
        String sCvtTempIndicatorColor;
        if (cvtDataDump.m_iDataAtfTemp < 20) { sCvtTempIndicatorString = "COLD"; sCvtTempIndicatorColor = "#FF20A0E0"; }
        else if (cvtDataDump.m_iDataAtfTemp < 50) { sCvtTempIndicatorString = "WARM"; sCvtTempIndicatorColor = "#FF00C0C0"; }
        else if (cvtDataDump.m_iDataAtfTemp < 90) { sCvtTempIndicatorString = "OK"; sCvtTempIndicatorColor = "#FF00AA00"; }
        else if (cvtDataDump.m_iDataAtfTemp < 110) { sCvtTempIndicatorString = "HOT"; sCvtTempIndicatorColor = "#FFFF8000"; }
        else { sCvtTempIndicatorString = "HOTTER"; sCvtTempIndicatorColor = "#FFFF0000"; }
        m_textViewCvtTempIndicator.setText(sCvtTempIndicatorString);
        m_textViewCvtTempIndicator.setBackgroundColor(Color.parseColor(sCvtTempIndicatorColor));

        m_textViewClutchLockup.setText(cvtDataDump.m_sDataIsolT1 + "A");
        if (cvtDataDump.m_dDataIsolT1 > 0.1 && Math.abs(cvtDataDump.m_iDataSlipRev) < 10 && cvtDataDump.m_dDataTrqRto < 1.01) m_textViewClutchLockupIndicator.setVisibility(View.VISIBLE);
        else m_textViewClutchLockupIndicator.setVisibility(View.INVISIBLE);
        m_progressBarClutchLockup.setSecondaryProgress(m_progressBarClutchLockup.getProgress());
        m_progressBarClutchLockup.setProgress((int) (100 * cvtDataDump.m_dDataIsolT1 / 0.7));
        m_progressBarSlipRev.setSecondaryProgress(m_progressBarSlipRev.getProgress());
        m_progressBarSlipRev.setProgress((int) (100 * Math.abs(cvtDataDump.m_iDataSlipRev) / 127));
        m_textViewSlipRev.setText(cvtDataDump.m_sDataSlipRev + ( (127 == cvtDataDump.m_iDataSlipRev || -127 == cvtDataDump.m_iDataSlipRev) ? "+" : "" ) );
        m_textViewTorqueConverterRatio.setText(cvtDataDump.m_sDataTrqRto);

        m_progressBarAwdCurrent.setSecondaryProgress(m_progressBarAwdCurrent.getProgress());
        m_progressBarAwdCurrent.setProgress((int)(100*cvtDataDump.m_dAwdDataEtsSolenoid/1.8));
        m_textViewAwdCurrent.setText(cvtDataDump.m_sAwdDataEtsSolenoid+"A");
        m_textViewAwdRatio.setText(cvtDataDump.m_sAwdDataEtsSolenoidRatio);

        String sVirtualGear;
        if (cvtDataDump.m_dDataGearRatio <= 0.44) sVirtualGear = "6";
        else if (cvtDataDump.m_dDataGearRatio <= 0.69) sVirtualGear = "5+";
        else if (cvtDataDump.m_dDataGearRatio <= 0.86) sVirtualGear = "4+";
        else if (cvtDataDump.m_dDataGearRatio <= 1.11) sVirtualGear = "3+";
        else if (cvtDataDump.m_dDataGearRatio <= 1.53) sVirtualGear = "2+";
        else sVirtualGear = "1+";
        m_textViewVirtualGear.setText(sVirtualGear);
        m_textViewGearRatio.setText(cvtDataDump.m_sDataGearRatio);
        m_progressBarGearRatio.setSecondaryProgress(m_progressBarGearRatio.getProgress());
        m_progressBarGearRatio.setProgress(100 - (int) (100 * (cvtDataDump.m_dDataGearRatio - 0.43) / (2.37 - 0.43)));
        m_progressBarStepMotor.setSecondaryProgress(m_progressBarStepMotor.getProgress());
        m_progressBarStepMotor.setProgress((int) (100 * (cvtDataDump.m_iDataStmStep + 20) / (190 + 20)));
        m_textViewStepMotor.setText(cvtDataDump.m_sDataStmStep);
        double dEffectiveGearRatio;
        if (cvtDataDump.m_sDataLeverPosition.contentEquals("D")) dEffectiveGearRatio = cvtDataDump.m_dDataGearRatio;
        else if (cvtDataDump.m_sDataLeverPosition.contentEquals("R")) dEffectiveGearRatio = 1.766;
        else dEffectiveGearRatio = 0;
        m_textViewFinalTorque.setText(String.format("%d", Math.round(cvtDataDump.m_dDataEngineTorque * cvtDataDump.m_dDataTrqRto * dEffectiveGearRatio * 5.173)));
        m_textViewEngineTorque.setText(cvtDataDump.m_sDataEngineTorque);
        m_textViewEngineHorsePower.setText(String.format("%d", Math.round(1.3596 * cvtDataDump.m_dDataEngineTorque * (double) cvtDataDump.m_iDataEngSpeedSig / 9549)));

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
        m_textViewLuPrs.setText(cvtDataDump.m_sDataLuPrs);

        m_textViewDeterioration.setText(cvtDataDump.m_sCvtfDeteriorationDate + " (+" + cvtDataDump.m_sCvtfDeteriorationDateDelta + ")");

        String sDtc;
        if (0 == cvtDataDump.m_iCvtDtcCount && 0 == cvtDataDump.m_iEcuDtcFound)
            sDtc = cvtDataDump.m_sTime;
        else {
            sDtc = "DTC:";
            if (0 != cvtDataDump.m_iEcuDtcFound) {
                sDtc += " ECU";
                m_textViewDtc.setBackgroundColor(Color.YELLOW);
            }
            if (0 != cvtDataDump.m_iCvtDtcCount) {
                sDtc += " CVT";
                m_textViewDtc.setBackgroundColor(Color.RED);
            }
        }
        m_textViewDtc.setText(sDtc);

        // Pri Prs test during full stop with stm_step=4
        if (cvtDataDump.m_iDataAtfTemp >= 50 && 0 == cvtDataDump.m_iDataVehicleSpeed && cvtDataDump.m_bDataBrakeSw && 4 == cvtDataDump.m_iDataStmStep && Math.abs(cvtDataDump.m_dDataTgtSecPrs - 0.7) < 0.1 && Math.abs(cvtDataDump.m_dDataSecPress - 0.7) < 0.1 && cvtDataDump.m_sDataLeverPosition.contentEquals("D")) {
            if (0 == m_dDiagnosticPriPrsTestPointsCount) {
                m_dDiagnosticPriPrsTestMinimum = cvtDataDump.m_dDataPriPress;
                m_textViewPriPrsTestTitle.setVisibility(View.VISIBLE);
                m_textViewPriPrsTestResult.setVisibility(View.VISIBLE);
            }
            if (cvtDataDump.m_dDataPriPress < m_dDiagnosticPriPrsTestMinimum) m_dDiagnosticPriPrsTestMinimum = cvtDataDump.m_dDataPriPress;
            m_dDiagnosticPriPrsTestAverage = ((m_dDiagnosticPriPrsTestAverage * m_dDiagnosticPriPrsTestPointsCount) + cvtDataDump.m_dDataPriPress) / ++m_dDiagnosticPriPrsTestPointsCount;
            m_textViewPriPrsTestResult.setText(String.format("%.2f", m_dDiagnosticPriPrsTestMinimum) + "/" + String.format("%.2f", m_dDiagnosticPriPrsTestAverage));
            if (m_dDiagnosticPriPrsTestMinimum < 0.3) m_textViewPriPrsTestResult.setBackgroundColor(Color.parseColor("#FFFF0000"));
            else if (m_dDiagnosticPriPrsTestMinimum < 0.5) m_textViewPriPrsTestResult.setBackgroundColor(Color.parseColor("#FFFF8000"));
        }

        m_cvtDataDumpPrevious = cvtDataDump;
    }
}
