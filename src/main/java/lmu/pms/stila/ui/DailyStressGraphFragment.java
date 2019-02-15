package lmu.pms.stila.ui;


import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

import lmu.pms.stila.Database.HRVDatabase;
import lmu.pms.stila.Database.HRVDatabaseHandler;
import lmu.pms.stila.R;
import lmu.pms.stila.Utils.TimeConversionUtil;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.provider.FixedBoundsStressedIndicator;
import lmu.pms.stila.provider.StressedIndicatorAlgorithm;
import lmu.pms.stila.provider.ThreePartsStressedIndicator;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DailyStressGraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DailyStressGraphFragment extends Fragment {

    private LineChart mChart;


    private Drawable mGradientDrawable;

    private float mRelaxedBound;
    private float mStressedBound;

    public DailyStressGraphFragment() {

    }


    public static DailyStressGraphFragment newInstance(String param1, String param2) {
        DailyStressGraphFragment fragment = new DailyStressGraphFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mGradientDrawable = ContextCompat.getDrawable(getContext(),R.drawable.gradient);



        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());
        View view = inflater.inflate(R.layout.fragment_daily_stress_graph, container, false);

        mChart =(LineChart) view.findViewById(R.id.lineChartToday);
        mChart.setDescription("");
        mChart.setNoDataText("No Stress Data available yet. Please stay connected to your Phone");
        Paint p = mChart.getPaint(Chart.PAINT_INFO);
        p.setTextSize(20);
       // p.setColor(ContextCompat.getColor(mContext, R.color.fitbitGreen));
        //method deprecated
        //p.setColor(getResources().getColor(R.color.fitbitGreen));
        //mChart.setOnChartGestureListener(this);
        mChart.setDrawGridBackground(false);
        mChart.setDrawGridBackground(false);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleXEnabled(true);
        mChart.setScaleYEnabled(false);
        mChart.setDoubleTapToZoomEnabled(false);
        mChart.setMaxVisibleValueCount(144);
        mChart.setPinchZoom(true);

        // The legend of the Chart is enabled here
        Legend l = mChart.getLegend();
        l.setEnabled(false);

        YAxis yl = mChart.getAxisLeft();
        yl.setDrawGridLines(false);
        yl.setDrawAxisLine(false);
        yl.setDrawLabels(false);
        yl.setAxisMaxValue(100);
        yl.setSpaceTop(30f);
        yl.setSpaceBottom(30f);
        yl.setDrawZeroLine(false);


        mChart.getAxisRight().setEnabled(false);

        XAxis xl = mChart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setDrawAxisLine(false);
        xl.setDrawGridLines(false);
        xl.setTextColor(getResources().getColor(R.color.white,null));
        xl.setAvoidFirstLastClipping(false);
        //xl.setXOffset(60);
        xl.setTextSize(10);
       // xl.setTypeface(tf);
        mChart.setExtraLeftOffset(20);
        mChart.setExtraRightOffset(30);
        xl.setXOffset(20);
        xl.setLabelRotationAngle(-90);
        mChart.invalidate();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        StressedIndicatorAlgorithm algo =  new FixedBoundsStressedIndicator(getContext());
        algo.updateData();
        mRelaxedBound = algo.getRelaxedBound();
        mStressedBound = algo.getStressedBound();
        addEntries();

        YAxis yl = mChart.getAxisLeft();
        LimitLine stressedLimitLine = new LimitLine(mStressedBound);
        yl.addLimitLine(stressedLimitLine);
        mChart.invalidate();
    }

    private void addEntries(){
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        HRVDatabaseHandler dbHandler = new HRVDatabaseHandler(getContext());
        long fromTimestamp = TimeConversionUtil.calculateMidnightTimestamp();
        long toTimestamp = fromTimestamp+86400;
        ArrayList<HRVDatabase> hrvs = dbHandler.getEntriesInTimeRange(fromTimestamp,toTimestamp,null);

        /** Variant: Only add the labels of recorded datapoints
        for(int i = 0; i< hrvs.size();i++){
            Entry nextPoint = new Entry((float)hrvs.get(i).getComputedStress(),i);
            entries.add(nextPoint);
            String formattedDate = TimeConversionUtil.utcTimestampToLongDaytimeStr(hrvs.get(i).getTimestamp());
            labels.add(formattedDate);
        }
         **/
        int firstIndexWithData = -1;

        int index = 0;
        int lastIndexWithData = -1;
        for(long time = fromTimestamp; time<toTimestamp; time+=600){
            // Date date = new Date(time * 1000);
            String formattedDate = TimeConversionUtil.utcTimestampToLongDaytimeStr(time);
            // dateFormat.format(date);
            labels.add(formattedDate);
            //HRVDatabase current = dbHandler.getExactEntry(time);
            HRVDatabase current = getEntryForExactTimeFromList(time,hrvs);
            if(current != null){
                float computedStress = (float)current.getComputedStress();
                entries.add(new Entry(computedStress,index));
                if(firstIndexWithData ==-1){
                    firstIndexWithData = index;
                }
                lastIndexWithData=index;
            }
            index ++;
        }

        LineDataSet dataSet = new LineDataSet(entries,"");
        dataSet.setDrawValues(false);   //dont show values
        dataSet.setDrawCircles(false);  //dont show value points on the line
        dataSet.setLineWidth(0.2f);
        //dataSet.setColor(R.color.colorBackground,255); //set the line color
        dataSet.setFillDrawable(mGradientDrawable); // fill the background with the gradient drawable
        dataSet.setDrawFilled(true);    // draw it filled
        dataSet.setDrawCubic(true); //draw the lines smoothly

        LineData lineData = new LineData(labels,dataSet);
        mChart.setData(lineData);
        // let the chart know it's data has changed
        mChart.notifyDataSetChanged();
        // limit the number of visible entries
        mChart.setVisibleXRangeMaximum(144);

        mChart.fitScreen(); //resets previous zooming

        mChart.setScaleYEnabled(false);
        int offset = 0;
        if(lastIndexWithData>15){
            offset = 15;
        }
        mChart.moveViewToX(lastIndexWithData-offset);
        mChart.zoom(4,0,lastIndexWithData-offset,0);


    }


    private HRVDatabase getEntryForExactTimeFromList(long timestamp, ArrayList<HRVDatabase> list) {

        for(HRVDatabase entry: list){
            if(entry.getTimestamp() ==timestamp){
                return entry;
            }
        }
        return null;
    }

}
