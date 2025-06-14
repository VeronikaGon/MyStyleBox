package com.hfad.mystylebox.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hfad.mystylebox.MainActivity
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.ViewPagerAdapter
import com.hfad.mystylebox.ui.activity.AboutActivity

private const val PREFS_NAME = "home_prefs"
private const val KEY_LAST_TAB = "last_tab"

class HomeFragment : Fragment() {

    private lateinit var btnImageHelp: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val tabTitles = listOf("Планирование", "Статистика", "Список желаний")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        btnImageHelp = view.findViewById(R.id.imageHelp)
        btnImageHelp.setOnClickListener {
            val intent = Intent(requireContext(), AboutActivity::class.java)
            startActivity(intent)
        }

        val btnBDsetings: ImageButton = view.findViewById(R.id.imageBDsetings)
        btnBDsetings.setOnClickListener {
            (activity as? MainActivity)?.drawerLayout?.openDrawer(GravityCompat.START)
        }

        viewPager = view.findViewById(R.id.viewPager)
        viewPager.adapter = ViewPagerAdapter(requireActivity())
        viewPager.isUserInputEnabled = false

        tabLayout = view.findViewById(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTab = prefs.getInt(KEY_LAST_TAB, 1)
        viewPager.currentItem = lastTab

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                prefs.edit().putInt(KEY_LAST_TAB, position).apply()
            }
        })
        return view
    }
}