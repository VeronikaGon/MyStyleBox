package com.hfad.mystylebox

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.flexbox.FlexboxLayout
import com.hfad.mystylebox.database.Subcategory

class SearchResultsFragment : Fragment() {

    private var subcategories: List<Subcategory> = emptyList()
    private var imageUri: Uri? = null

    companion object {
        fun newInstance(subcategories: List<Subcategory>, imageUri: Uri?): SearchResultsFragment {
            val fragment = SearchResultsFragment()
            fragment.subcategories = subcategories
            fragment.imageUri = imageUri
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_subcategory, container, false)
        val containerLayout = view.findViewById<FlexboxLayout>(R.id.containerSubcategories)
        for (subcategory in subcategories) {
            val button = createSubcategoryButton(subcategory)
            containerLayout.addView(button)
        }
        return view
    }
    private fun createSubcategoryButton(subcategory: Subcategory): Button {
        val button = Button(requireContext())
        button.text = subcategory.name
        button.setBackgroundResource(R.drawable.checkbox_background)
        button.setPadding(2, 2, 2, 2)
        val layoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 8, 8, 8)
        button.layoutParams = layoutParams
        button.setOnClickListener {
            (activity as? CategorySelectionActivity)?.onSubcategorySelected(subcategory.name)
        }
        return button
    }
}