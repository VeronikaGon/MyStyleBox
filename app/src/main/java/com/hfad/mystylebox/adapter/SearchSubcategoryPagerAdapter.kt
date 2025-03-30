package com.hfad.mystylebox.adapter

import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hfad.mystylebox.database.Subcategory
import com.hfad.mystylebox.fragment.SearchResultsFragment

class SearchSubcategoryPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val tabLabels: List<String>,
    private val subcategoriesByTab: Map<String, List<Subcategory>>,
    private val imageUri: Uri?
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = tabLabels.size

    override fun createFragment(position: Int): Fragment {
        val label = tabLabels[position]
        val subcategories = subcategoriesByTab[label] ?: emptyList()
        return SearchResultsFragment.newInstance(subcategories, imageUri)
    }
}