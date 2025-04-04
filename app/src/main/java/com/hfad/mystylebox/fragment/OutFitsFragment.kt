package com.hfad.mystylebox.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.hfad.mystylebox.ClothingSelectionActivity
import com.hfad.mystylebox.R

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class OutFitsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_out_fits, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val addoutfit = view.findViewById<ImageButton>(R.id.addoutfit)
        addoutfit.setOnClickListener { startOutfitActivity() }
    }
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            OutFitsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun startOutfitActivity() {
        val intent = Intent(requireContext(), ClothingSelectionActivity::class.java)
        startActivity(intent)
    }
}