package com.hfad.mystylebox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.hfad.mystylebox.R

class SecondFragment : Fragment() {

    private lateinit var hsv: HorizontalScrollView
    private lateinit var rbGarderob: RadioButton
    private lateinit var rbPlan: RadioButton
    private lateinit var rbWishlist: RadioButton

    interface StatsUpdatable {
        fun updateStats()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_second, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hsv         = view.findViewById(R.id.hsv)
        rbGarderob  = view.findViewById(R.id.rbStatGarderob)
        rbPlan      = view.findViewById(R.id.rbStatPlan)
        rbWishlist  = view.findViewById(R.id.rbStatWishlist)

        setChecked(rbGarderob)
        replaceStatsFragment(WardrobeStatsFragment())

        rbGarderob.setOnClickListener {
            setChecked(rbGarderob)
            scrollToCenter(rbGarderob)
            replaceStatsFragment(WardrobeStatsFragment())
        }
        rbPlan.setOnClickListener {
            setChecked(rbPlan)
            scrollToCenter(rbPlan)
            replaceStatsFragment(PlanningStatsFragment())
        }
        rbWishlist.setOnClickListener {
            setChecked(rbWishlist)
            scrollToCenter(rbWishlist)
            replaceStatsFragment(WishlistStatsFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentFragment()
    }

    private fun setChecked(selected: RadioButton) {
        listOf(rbGarderob, rbPlan, rbWishlist).forEach { rb ->
            rb.isChecked = (rb == selected)
        }
    }

    private fun scrollToCenter(selected: RadioButton) {
        hsv.post {
            val scrollX = selected.left + selected.width / 2 - hsv.width / 2
            hsv.smoothScrollTo(scrollX, 0)
        }
    }

    private fun replaceStatsFragment(fragment: Fragment) {
        val tag = fragment.javaClass.simpleName
        childFragmentManager.beginTransaction()
            .replace(R.id.stats_container, fragment, tag)
            .commit()

        if (fragment is StatsUpdatable) {
            childFragmentManager.executePendingTransactions()
            fragment.updateStats()
        }
    }

    private fun refreshCurrentFragment() {
        val current = childFragmentManager.findFragmentById(R.id.stats_container)
        if (current is StatsUpdatable) {
            current.updateStats()
        }
    }
}