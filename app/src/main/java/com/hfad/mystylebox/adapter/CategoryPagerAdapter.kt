package com.hfad.mystylebox.adapter

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hfad.mystylebox.fragment.SubcategoryFragment
import com.hfad.mystylebox.database.entity.Category

class CategoryPagerAdapter(
    activity: AppCompatActivity,
    private val categories: List<Category>,
    private val imageUri: Uri?
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        return SubcategoryFragment.newInstance(categories[position].id, imageUri)
    }
}