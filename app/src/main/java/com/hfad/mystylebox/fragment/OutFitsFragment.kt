package com.hfad.mystylebox.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.hfad.mystylebox.ClothingSelectionActivity
import com.hfad.mystylebox.EditoutfitActivity
import com.hfad.mystylebox.FilterActivity
import com.hfad.mystylebox.OutfitActionsBottomSheet
import com.hfad.mystylebox.R
import com.hfad.mystylebox.SearchClothingActivity
import com.hfad.mystylebox.SearchOutfitActivity
import com.hfad.mystylebox.adapter.OutfitAdapter
import com.hfad.mystylebox.database.AppDatabase
import com.hfad.mystylebox.database.Outfit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutFitsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter
    private lateinit var imageFilter: ImageButton
    private var isFilterActive: Boolean = false
    private var currentFilters: Bundle? = null

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
            val intent = Intent(requireContext(), EditoutfitActivity::class.java).apply {
                putExtra("outfit", outfit)
                putExtra("image_uri", outfit.imagePath)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent) }

        outfitAdapter.onItemLongClick = { outfit ->
            val bottomSheet = OutfitActionsBottomSheet.newInstance(outfit.name, outfit.imagePath)
            bottomSheet.show(childFragmentManager, bottomSheet.tag)
            bottomSheet.onEditClicked = {
                val intent = Intent(requireContext(), EditoutfitActivity::class.java).apply {
                    putExtra("outfit", outfit)
                    putExtra("image_uri", outfit.imagePath)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
            bottomSheet.onDeleteClicked = {
                deleteOutfit(outfit)
            }
        }

        loadOutfits()

        val imageSearch = view.findViewById<ImageButton>(R.id.imageSearch)
        imageSearch.setOnClickListener { startSearchClothingActivity() }
        imageFilter = view.findViewById(R.id.imageFilter)
//        imageFilter.setOnClickListener { startFilterActivity() }
    }
    private fun startSearchClothingActivity() {
        val intent = Intent(requireContext(), SearchOutfitActivity::class.java)
        startActivity(intent)
    }
//    private fun startFilterActivity() {
//        val intent = Intent(requireContext(), FilterOutfitActivity::class.java).apply {
//
//        }
//        filterActivityLauncher.launch(intent)
//    }
    private val filterActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val selectedGender = data.getStringArrayListExtra("selectedGender") ?: arrayListOf()
            val selectedSeasons = data.getStringArrayListExtra("selectedSeasons") ?: arrayListOf()
            val selectedSizes = data.getStringArrayListExtra("selectedSizes") ?: arrayListOf()
            val selectedStatuses = data.getStringArrayListExtra("selectedStatuses") ?: arrayListOf()
            val selectedTags = data.getStringArrayListExtra("selectedTags") ?: arrayListOf()

            currentFilters = Bundle().apply {
                putStringArrayList("genders", selectedGender)
                putStringArrayList("seasons", selectedSeasons)
                putStringArrayList("sizes", selectedSizes)
                putStringArrayList("statuses", selectedStatuses)
                putStringArrayList("tags", selectedTags)
            }

            val hasFilters = listOf(selectedGender, selectedSeasons, selectedSizes, selectedStatuses, selectedTags)
                .any { it.isNotEmpty() }
            imageFilter.setColorFilter(
                if (hasFilters) Color.parseColor("#FFB5A7")
                else Color.parseColor("#000000")
            )
            isFilterActive = true
        }
    }
    private fun startOutfitActivity() {
        val intent = Intent(requireContext(), ClothingSelectionActivity::class.java)
        startActivity(intent)
    }

    private fun loadOutfits() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            val outfits = db.outfitDao().getAllOutfits()
            withContext(Dispatchers.Main) {
                outfitAdapter.updateData(outfits)
            }
        }
    }
    // Метод для вывода диалога с подтверждением удаления
    private fun showDeleteConfirmation(outfit: Outfit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Подтверждение удаления")
            .setMessage("Вы уверены, что хотите удалить комплект \"${outfit.name}\"?")
            .setPositiveButton("Удалить") { dialog, _ ->
                deleteOutfit(outfit)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val editedImageUriString = result.data?.getStringExtra("result_image_uri")
            if (!editedImageUriString.isNullOrEmpty()) {
                val editedImageUri = Uri.parse(editedImageUriString)
                val imageView = requireView().findViewById<ImageView>(R.id.image)
                Glide.with(this).load(editedImageUri).into(imageView)
            }
        }
    }
    private fun startEditActivity(imageUri: Uri?) {
        if (imageUri == null) return
        val intent = Intent(requireContext(), EditoutfitActivity::class.java)
        intent.putExtra("imageUri", imageUri.toString())
        editLauncher.launch(intent)
    }
    private fun deleteOutfit(outfit: Outfit) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                requireContext(), AppDatabase::class.java, "wardrobe_db"
            ).build()
            db.outfitClothingItemDao().deleteForOutfit(outfit.id)

            db.outfitTagDao().deleteTagsForOutfit(outfit.id)

            db.outfitDao().delete(outfit)

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Комплект удалён", Toast.LENGTH_SHORT).show()
                loadOutfits()
            }
        }
    }
}