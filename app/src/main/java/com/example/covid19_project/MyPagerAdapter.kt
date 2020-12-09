package com.example.covid19_project

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class MyPagerAdapter(fm :FragmentManager) : FragmentStatePagerAdapter(fm) {
    var mData : ArrayList<Fragment> = arrayListOf(MapFragment(),AlertFragment())
    private val tabTitles = arrayOf("Maps", "Alert")

    override fun getPageTitle(position: Int): CharSequence? {
        return tabTitles[position]
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> MapFragment()
            else -> AlertFragment()
        }
    }

    override fun getCount(): Int {
        return mData.size
    }
}