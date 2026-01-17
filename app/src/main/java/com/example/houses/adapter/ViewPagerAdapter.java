package com.example.houses.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.houses.ui.ChatMessageFragment;
import com.example.houses.ui.SettingsFragment;
import com.example.houses.ui.TaskFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new TaskFragment();
        if (position == 1) return new ChatMessageFragment();
        return new SettingsFragment();
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
