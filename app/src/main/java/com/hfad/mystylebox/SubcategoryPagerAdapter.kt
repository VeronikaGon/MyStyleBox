package com.hfad.mystylebox

import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hfad.mystylebox.database.Subcategory

class SubcategoryPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val subcategories: List<Subcategory>,
    private val imageUri: Uri?
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 1

    override fun createFragment(position: Int): Fragment {
        // Создаем фрагмент, передавая id подкатегории и imageUri, если нужно
        return SearchResultsFragment.newInstance(subcategories, imageUri)
    }
}