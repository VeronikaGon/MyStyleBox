package com.hfad.mystylebox

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
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
        fun newInstance(date: LocalDate) = DayDetailsBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_DATE, date.toString())
            }
        }
    }

    private lateinit var dao: DailyPlanDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = AppDatabase.getInstance(requireContext()).dailyPlanDao()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.sheet_day_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val date = LocalDate.parse(requireArguments().getString(ARG_DATE)!!)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvCount = view.findViewById<TextView>(R.id.tvCount)
        val rvOutfits = view.findViewById<RecyclerView>(R.id.rvOutfits)
        val btnAction = view.findViewById<Button>(R.id.btnAction)

       val outfitLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // после возвращения обновляем calendar (например, через callback или перезагрузку adapter-а)
        }
        tvDate.text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("ru")))

        rvOutfits.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

        lifecycleScope.launch(Dispatchers.IO) {
            val outfits = dao.getOutfitsByDate(date.toString())
            withContext(Dispatchers.Main) {
                if (outfits.isEmpty()) {
                    tvCount.text = "Запланированных комплектов нет"
                    btnAction.text = "Запланировать комплект"
                    btnAction.setOnClickListener {
                        val intent = Intent(requireContext(), OutfitSelectionActivity::class.java)
                        outfitLauncher.launch(intent)
                    }
                } else {
                    tvCount.text = "Запланирован${if (outfits.size>1) "о" else ""} ${outfits.size} комплект${if (outfits.size>5) "ов" else if (outfits.size>2) "а" else ""}"
                    btnAction.text = "Запланировать ещё"
                    btnAction.setOnClickListener {
                        val intent = Intent(requireContext(), OutfitSelectionActivity::class.java).apply {
                            putStringArrayListExtra("EXTRA_SELECTED_IDS", ArrayList(outfits.map { it.id.toString() }))
                        }
                        outfitLauncher.launch(intent)
                    }
                }
                rvOutfits.adapter = OutfitAdapter(outfits, R.layout.item_clothing)
            }
        }

        btnAction.setOnClickListener {
        }
    }
}