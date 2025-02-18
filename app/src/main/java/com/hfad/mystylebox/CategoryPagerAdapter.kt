package com.hfad.mystylebox

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hfad.mystylebox.database.Category

class CategoryPagerAdapter(
    activity: AppCompatActivity,
    private val categories: List<Category>,
    private val imageUri: Uri?
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        // Передаём id категории во фрагмент для загрузки подкатегорий
        return SubcategoryFragment.newInstance(categories[position].id, imageUri)
    }
}