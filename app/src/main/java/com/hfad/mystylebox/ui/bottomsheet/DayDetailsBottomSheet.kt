package com.hfad.mystylebox.ui.bottomsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hfad.mystylebox.ui.widget.DataProvider
import com.hfad.mystylebox.ui.activity.OutfitSelectionActivity
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.PlannedOutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.DailyPlan
import com.hfad.mystylebox.database.DailyPlanDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class DayDetailsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_DATE = "arg_date"
        const val RESULT_KEY = "planUpdated"
        fun newInstance(date: LocalDate) = DayDetailsBottomSheet().apply {
            arguments = Bundle().apply {putString(ARG_DATE, date.toString())
            }
        }
    }

    private lateinit var dao: DailyPlanDao
    private lateinit var date: LocalDate
    private lateinit var tvCount: TextView
    private lateinit var btnAction: Button
    private lateinit var rvOutfits: RecyclerView

    private val outfitSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val idStrings = result.data
                ?.getStringArrayListExtra("EXTRA_SELECTED_IDS")
                ?: arrayListOf()
            val selectedIds = idStrings.mapNotNull(String::toLongOrNull)
            lifecycleScope.launch(Dispatchers.IO) {
                selectedIds.forEach { dao.insert(DailyPlan(date.toString(), it.toInt())) }
                withContext(Dispatchers.Main) {
                    loadOutfits()
                    parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
                    DataProvider.notifyWidgetDataChanged(requireContext())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = AppDatabase.getInstance(requireContext()).dailyPlanDao()
        date = LocalDate.parse(requireArguments()!!.getString(ARG_DATE)!!)
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        inflater.inflate(R.layout.sheet_day_details, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvCount   = view.findViewById(R.id.tvCount)
        btnAction = view.findViewById(R.id.btnAction)
        rvOutfits = view.findViewById<RecyclerView>(R.id.rvOutfits).apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        }
        view.findViewById<TextView>(R.id.tvDate).text =
            date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("ru")))

        loadOutfits()
    }
    private fun loadOutfits() {
        lifecycleScope.launch(Dispatchers.IO) {
            val outfits = dao.getOutfitsByDate(date.toString())
            withContext(Dispatchers.Main) {
                if (outfits.isEmpty()) {
                    tvCount.text = "Запланированных комплектов нет"
                    btnAction.text = "Запланировать комплект"
                } else {
                    tvCount.text = "Запланирован${if (outfits.size>1) "о" else ""} ${outfits.size} " +
                            "комплект${when {
                                outfits.size%10 in 2..4 && outfits.size !in 12..14 -> "а"
                                else -> "ов"
                            }}"
                    btnAction.text = "Запланировать ещё"
                }
                btnAction.setOnClickListener {
                    val intent = Intent(requireContext(), OutfitSelectionActivity::class.java).apply {
                        if (outfits.isNotEmpty()) {
                            putStringArrayListExtra(
                                "EXTRA_SELECTED_IDS",
                                ArrayList(outfits.map { it.id.toString() })
                            )
                        }
                    }
                    outfitSelectionLauncher.launch(intent)
                }

                rvOutfits.adapter = PlannedOutfitAdapter(outfits) { outfit ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Удалить комплект")
                        .setMessage("Точно убрать этот комплект из планирования?")
                        .setNegativeButton("Нет", null)
                        .setPositiveButton("Да") { _, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                dao.deleteByDateAndOutfitId(
                                    date.toString(),
                                    outfit.id.toLong()
                                )
                                withContext(Dispatchers.Main) {
                                    loadOutfits()
                                    parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
                                    DataProvider.notifyWidgetDataChanged(requireContext())
                                }
                            }
                        }
                        .show()
                }
            }
        }
    }
}