package lmu.pms.stila.onboarding;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import lmu.pms.stila.R;

public class OnboardingGridViewPager  extends FragmentGridPagerAdapter {

    private static final int TRANSITION_DURATION_MILLIS = 100;

    private final Context mContext;
    private List<Row> mRows;
    private ColorDrawable mDefaultBg;

    private ColorDrawable mClearBg;

    public OnboardingGridViewPager(Context ctx, FragmentManager fm) {
        super(fm);
        mContext = ctx;

        mRows = new ArrayList<OnboardingGridViewPager.Row>();

        mRows.add(new Row(cardFragment(R.string.tutorial_title_1, R.string.tutorial_description_1)));
        mRows.add(new Row(cardFragment(R.string.tutorial_title_2, R.string.tutorial_description_2)));
        mRows.add(new Row(cardFragment(R.string.tutorial_title_3, R.string.tutorial_description_3)));
        mRows.add(new Row(cardFragment(R.string.tutorial_title_4, R.string.tutorial_description_4)));
        mRows.add(new Row(cardFragment(R.string.tutorial_title_5, R.string.tutorial_description_5)));
        mRows.add(new Row(cardFragment(R.string.tutorial_title_6, R.string.tutorial_description_6)));
        mRows.add(new Row(cardFragment(R.string.tutorial_title_7, R.string.tutorial_description_7)));
        mRows.add(new Row(cardFragment(R.string.tutorial_title_8, R.string.tutorial_description_8)));
        mRows.add(new Row(cardFragment(R.string.tutorial_title_9, R.string.tutorial_description_9)));
        mRows.add(new Row(new PickWatchfaceOnboardingFragment()));
        mDefaultBg = new ColorDrawable(ctx.getResources().getColor(R.color.primaryColor));
        mClearBg = new ColorDrawable(ctx.getResources().getColor(android.R.color.transparent));
    }



    private Fragment cardFragment(int titleRes, int textRes) {
        Resources res = mContext.getResources();
        CardFragment fragment =
                CardFragment.create(res.getText(titleRes), res.getText(textRes));
        // Add some extra bottom margin to leave room for the page indicator
        fragment.setCardMarginBottom(
                res.getDimensionPixelSize(R.dimen.card_margin_bottom));
        return fragment;
    }


    /** A convenient container for a row of fragments. */
    private class Row {
        final List<Fragment> columns = new ArrayList<Fragment>();

        public Row(Fragment... fragments) {
            for (Fragment f : fragments) {
                add(f);
            }
        }

        public void add(Fragment f) {
            columns.add(f);
        }

        Fragment getColumn(int i) {
            return columns.get(i);
        }

        public int getColumnCount() {
            return columns.size();
        }
    }

    @Override
    public Fragment getFragment(int row, int col) {
        Row adapterRow = mRows.get(row);
        return adapterRow.getColumn(col);
    }

    @Override
    public Drawable getBackgroundForRow(final int row) {
        return mContext.getResources().getDrawable(R.drawable.tutorial_background,null);

    }

    @Override
    public Drawable getBackgroundForPage(final int row, final int column) {
        return mContext.getResources().getDrawable(R.drawable.tutorial_background,null);
    }

    @Override
    public int getRowCount() {
        return mRows.size();
    }

    @Override
    public int getColumnCount(int rowNum) {
        return mRows.get(rowNum).getColumnCount();
    }


}
