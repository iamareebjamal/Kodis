package com.kodis.ui.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.kodis.ui.fragment.EditorFragment;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentPagerAdapter {
    private List<Fragment> mFragmentList = new ArrayList<>();

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    public void addFragment(Fragment fragment) {
        mFragmentList.add(fragment);
    }

    @Override
    public int getItemPosition(Object object) {
        // refresh all fragments when data set changed
        return ViewPagerAdapter.POSITION_NONE;
    }

    public void removeTabPage(int position) {
        mFragmentList.remove(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return ((EditorFragment)mFragmentList.get(position)).getFileName();
    }

    public List<Fragment> getFragmentList() {
        return mFragmentList;
    }
}
