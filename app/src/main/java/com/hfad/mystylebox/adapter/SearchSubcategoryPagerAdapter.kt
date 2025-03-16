package com.hfad.mystylebox.adapter

import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hfad.mystylebox.database.Subcategory
import com.hfad.mystylebox.fragment.SearchResultsFragment

class SearchSubcategoryPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val filteredCategories: List<String>,
    private val filteredSubcategoriesByCategory: Map<String, List<Subcategory>>,
    private val imageUri: Uri?
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = filteredCategories.size

    override fun createFragment(position: Int): Fragment {
        val category = filteredCategories[position]
        val subcategories = filteredSubcategoriesByCategory[category] ?: emptyList()
        return SearchResultsFragment.newInstance(subcategories, imageUri)
    }
}