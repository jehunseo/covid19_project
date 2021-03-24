package com.example.covid19_project

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class MyPagerAdapter(fm :FragmentManager) : FragmentStatePagerAdapter(fm) {
    var mData : ArrayList<Fragment> = arrayListOf(QRFragment(),AlertFragment())
    private val tabTitles = arrayOf("Main", "Alert")

    override fun getPageTitle(position: Int): CharSequence? {
        return tabTitles[position]
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> QRFragment()
            else -> AlertFragment()
        }
    }

    override fun getCount(): Int {
        return mData.size
    }
}