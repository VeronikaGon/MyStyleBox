package com.hfad.mystylebox

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.flexbox.FlexboxLayout
import com.hfad.mystylebox.database.AppDatabase

class SubcategoryFragment : Fragment() {

    private var categoryId: Int = 0
    private var imageUri: Uri? = null

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_IMAGE_URI = "image_uri"

        fun newInstance(categoryId: Int, imageUri: Uri?): SubcategoryFragment {
            val fragment = SubcategoryFragment()
            val args = Bundle()
            args.putInt(ARG_CATEGORY_ID, categoryId)
            args.putString(ARG_IMAGE_URI, imageUri?.toString())
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getInt(ARG_CATEGORY_ID)
            val uriString = it.getString(ARG_IMAGE_URI)
            if (!uriString.isNullOrEmpty()) {
                imageUri = Uri.parse(uriString)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_subcategory, container, false)
        val containerLayout = view.findViewById<FlexboxLayout>(R.id.containerSubcategories)
        val db = AppDatabase.getInstance(requireContext())
        val subcategories = db.subcategoryDao().getSubcategoriesForCategory(categoryId)
        for (subcategory in subcategories) {
            val button = Button(requireContext())
            button.text = subcategory.name
            button.setBackgroundResource(R.drawable.checkbox_background)

            button.setPadding(15, 2, 15, 2)
            val layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(8, 8, 8, 8)
            button.layoutParams = layoutParams
            button.setOnClickListener {
                (activity as? CategorySelectionActivity)?.onSubcategorySelected(subcategory.name,subcategory.id)
            }
            containerLayout.addView(button)
        }
        return view
    }
}