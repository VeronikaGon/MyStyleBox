package com.hfad.mystylebox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.hfad.mystylebox.R

class SecondFragment : Fragment() {

    private lateinit var rbGarderob: RadioButton
    private lateinit var rbPlan: RadioButton
    private lateinit var rbWishlist: RadioButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_second, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rbGarderob = view.findViewById(R.id.rbStatGarderob)
        rbPlan     = view.findViewById(R.id.rbStatPlan)
        rbWishlist = view.findViewById(R.id.rbStatWishlist)

        // По умолчанию Гардероб
        replaceStatsFragment(WardrobeStatsFragment())

        rbGarderob.setOnClickListener {
            setChecked(rbGarderob)
            replaceStatsFragment(WardrobeStatsFragment())
        }
        rbPlan.setOnClickListener {
            setChecked(rbPlan)
            replaceStatsFragment(PlanningStatsFragment())
        }
        rbWishlist.setOnClickListener {
            setChecked(rbWishlist)
            replaceStatsFragment(WishlistStatsFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        val current = childFragmentManager.findFragmentById(R.id.stats_container)
        if (current is WardrobeStatsFragment) {
            replaceStatsFragment(WardrobeStatsFragment())
        }
    }

    private fun setChecked(selected: RadioButton) {
        listOf(rbGarderob, rbPlan, rbWishlist).forEach { rb ->
            rb.isChecked = (rb == selected)
        }
    }

    private fun replaceStatsFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.stats_container, fragment)
            .commit()
    }
}