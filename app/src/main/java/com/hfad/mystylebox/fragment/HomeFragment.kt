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
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
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

class HomeViewModel : ViewModel() {
    private val _selectedTab = MutableLiveData<Int>(1)
    val selectedTab: LiveData<Int> = _selectedTab

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }
}

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var btnHelp: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val tabTitles = listOf("Планирование", "Статистика", "Список желаний")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        btnHelp = view.findViewById(R.id.imageHelp)
        btnHelp.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

        view.findViewById<ImageButton>(R.id.imageBDsetings)
            .setOnClickListener {
                (activity as? MainActivity)
                    ?.drawerLayout
                    ?.openDrawer(GravityCompat.START)
            }

        viewPager = view.findViewById(R.id.viewPager)
        viewPager.adapter = ViewPagerAdapter(requireActivity())
        viewPager.isUserInputEnabled = false

        tabLayout = view.findViewById(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = tabTitles[pos]
        }.attach()

        viewModel.selectedTab.observe(viewLifecycleOwner, Observer { index ->
            viewPager.currentItem = index
        })

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setSelectedTab(position)
            }
        })

        return view
    }
}