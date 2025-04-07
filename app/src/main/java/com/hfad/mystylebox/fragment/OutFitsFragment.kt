package com.hfad.mystylebox.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.hfad.mystylebox.ClothingSelectionActivity
import com.hfad.mystylebox.OutfitActionsBottomSheet
import com.hfad.mystylebox.R
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Outfit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class OutFitsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_out_fits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val addOutfitButton = view.findViewById<ImageButton>(R.id.addoutfit)
        addOutfitButton.setOnClickListener { startOutfitActivity() }

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        outfitAdapter = OutfitAdapter(emptyList(), R.layout.item_clothing)
        recyclerView.adapter = outfitAdapter

        outfitAdapter.onItemClick = { outfit ->
            // Например, открыть подробное окно или экран редактирования комплекта
            Toast.makeText(requireContext(), "Выбран комплект: ${outfit.name}", Toast.LENGTH_SHORT).show()
        }

        outfitAdapter.onItemLongClick = { outfit ->
            // Показываем BottomSheet для дополнительных действий с комплектом
            val bottomSheet = OutfitActionsBottomSheet.newInstance(outfit.name, outfit.imagePath)
            bottomSheet.show(childFragmentManager, bottomSheet.tag)
            bottomSheet.onDeleteClicked = {
                // Реализуйте удаление комплекта из базы
                deleteOutfit(outfit)
            }
        }

        loadOutfits()
    }

    private fun startOutfitActivity() {
        // Здесь можно запустить активность для создания нового комплекта
        val intent = Intent(requireContext(), ClothingSelectionActivity::class.java)
        startActivity(intent)
    }

    private fun loadOutfits() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            // Предполагается, что в outfitDao есть метод для получения всех комплектов
            val outfits = db.outfitDao().getAllOutfits()
            withContext(Dispatchers.Main) {
                outfitAdapter.updateData(outfits)
            }
        }
    }

    private fun deleteOutfit(outfit: Outfit) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            db.outfitDao().delete(outfit)
            // Если есть связи в outfit_clothing_item, возможно, их тоже надо удалить
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Комплект удалён", Toast.LENGTH_SHORT).show()
                loadOutfits()
            }
        }
    }
}