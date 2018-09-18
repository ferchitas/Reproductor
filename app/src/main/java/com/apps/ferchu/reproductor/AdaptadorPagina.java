package com.apps.ferchu.reproductor;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.apps.ferchu.reproductor.PlayListsFragment;
import com.apps.ferchu.reproductor.ReproductorFragment;

import java.nio.channels.ClosedByInterruptException;

public class AdaptadorPagina extends FragmentStatePagerAdapter {

    int mNoOfTabs;
    Context context;

    public AdaptadorPagina(FragmentManager fm, int numberOfTabs, Context context){

        super(fm);
        this.mNoOfTabs = numberOfTabs;
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {

        Fragment fragment = null;
        switch (position){

            case 0:
                fragment = new PlayListsFragment();
                break;
            case 1:
                fragment = new ReproductorFragment();
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return this.mNoOfTabs;
    }
}
