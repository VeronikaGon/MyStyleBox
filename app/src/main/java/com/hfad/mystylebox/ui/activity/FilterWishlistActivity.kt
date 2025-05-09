    package com.hfad.mystylebox.ui.activity

    import android.app.Activity
    import android.content.Intent
    import android.os.Bundle
    import android.view.Gravity
    import android.view.View
    import android.widget.CheckBox
    import android.widget.ImageView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.children
    import androidx.room.Room
    import com.hfad.mystylebox.R
    import java.util.ArrayList
    import com.hfad.mystylebox.database.AppDatabase
    import com.hfad.mystylebox.databinding.ActivityFilterWishlistBinding

    class FilterWishlistActivity : AppCompatActivity() {
        private lateinit var binding: ActivityFilterWishlistBinding
        private lateinit var db: AppDatabase

        // Диапазоны цен
        private val priceRanges: Map<String, IntRange> = mapOf(
            "до 1000" to 0..1000,
            "от 1000 до 3000" to 1000..3000,
            "от 3000 до 5000" to 3000..5000,
            "от 5000 до 10000" to 5000..10000,
            "от 10000 до 40000" to 10000..40000,
            "больше 40000" to 40001..Int.MAX_VALUE
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityFilterWishlistBinding.inflate(layoutInflater)
            setContentView(binding.root)

            db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "wardrobe_db"
            ).allowMainThreadQueries().build()

            restoreSelections()
            setupToggle()
            setupCheckboxListeners()
            binding.btnReset.setOnClickListener { onReset() }
            binding.btnApply.setOnClickListener { onApply() }

            updateApplyButtonState()
        }

        private fun restoreSelections() {
            val prevGenders = intent.getStringArrayListExtra("selectedGender") ?: arrayListOf()
            val prevSizes = intent.getStringArrayListExtra("selectedSize") ?: arrayListOf()
            val prevCounts = intent.getStringArrayListExtra("selectedCount") ?: arrayListOf()

            prevGenders.forEach {
                when (it) {
                    "Женский" -> binding.cbWoman.isChecked = true
                    "Мужской" -> binding.cbMan.isChecked = true
                    "Универсально" -> binding.cbUniversal.isChecked = true
                }
            }
            binding.cbNotSize.isChecked = prevSizes.contains("Без размера")
            binding.sizeContainer.children.filterIsInstance<CheckBox>().forEach { cb ->
                cb.isChecked = prevSizes.contains(cb.text.toString())
            }
            binding.countContainer.children.filterIsInstance<CheckBox>().forEach { cb ->
                cb.isChecked = prevCounts.contains(cb.text.toString())
            }
        }

        private fun setupToggle() {
            listOf(
                binding.blockGender to (binding.genderContainer to binding.ivGenderArrow),
                binding.blockSize to (binding.sizeContainer to binding.ivSizeArrow),
                binding.blockCount to (binding.countContainer to binding.ivCountArrow)
            ).forEach { (block, pair) ->
                val (container, arrow) = pair
                container.visibility = View.GONE
                arrow.setImageResource(R.drawable.ic_arrow_drop_down)
                block.setOnClickListener { toggle(container, arrow) }
            }
        }

        private fun setupCheckboxListeners() {
            val allBoxes = mutableListOf<CheckBox>().apply {
                add(binding.cbWoman); add(binding.cbMan); add(binding.cbUniversal)
                add(binding.cbNotSize)
                addAll(binding.sizeContainer.children.filterIsInstance<CheckBox>())
                addAll(binding.countContainer.children.filterIsInstance<CheckBox>())
            }
            allBoxes.forEach { cb ->
                cb.setOnCheckedChangeListener { _, _ -> updateApplyButtonState() }
            }
        }

        private fun updateApplyButtonState() {
            val anyChecked = listOf(binding.cbWoman, binding.cbMan, binding.cbUniversal).any { it.isChecked }
                    || binding.cbNotSize.isChecked
                    || binding.sizeContainer.children.filterIsInstance<CheckBox>().any { it.isChecked }
                    || binding.countContainer.children.filterIsInstance<CheckBox>().any { it.isChecked }
            binding.btnApply.isEnabled = anyChecked
        }

        private fun onReset() {
            listOf(binding.cbWoman, binding.cbMan, binding.cbUniversal, binding.cbNotSize).forEach { it.isChecked = false }
            binding.sizeContainer.children.filterIsInstance<CheckBox>().forEach { it.isChecked = false }
            binding.countContainer.children.filterIsInstance<CheckBox>().forEach { it.isChecked = false }
            updateApplyButtonState()

            setResult(Activity.RESULT_OK, Intent().apply {
                putStringArrayListExtra("selectedGender", arrayListOf())
                putStringArrayListExtra("selectedSize", arrayListOf())
                putStringArrayListExtra("selectedCount", arrayListOf())
            })
            finish()
        }

        private fun onApply() {
            val genders = listOf(
                "Женский" to binding.cbWoman,
                "Мужской" to binding.cbMan,
                "Универсально" to binding.cbUniversal
            ).filter { it.second.isChecked }.map { it.first }

            val sizes = mutableListOf<String>().apply {
                if (binding.cbNotSize.isChecked) add("Без размера")
                addAll(
                    binding.sizeContainer.children
                        .filterIsInstance<CheckBox>()
                        .filter { it.id != R.id.cbNotSize && it.isChecked }
                        .map { it.text.toString() }
                )
            }

            val counts = binding.countContainer
                .children
                .filterIsInstance<CheckBox>()
                .filter { it.isChecked }
                .map { it.text.toString() }
                .toList()

            val gendersList = ArrayList(genders)
            val sizesList = ArrayList(sizes)
            val countsList = ArrayList(counts)

            val all = db.wishListItemDao().getAll()
            val filtered = all.filter { item ->
                val okGender = genders.isEmpty() || genders.contains(item.gender)
                val okSize = sizes.isEmpty() || sizes.any { sizeText ->
                    if (sizeText == "Без размера") {
                        // 1) item.size == null
                        // 2) item.size == "" (пустая строка)
                        // 3) item.size == "null" — если БД вернула буквальный "null"
                        item.size.isNullOrBlank() || item.size.equals("null", ignoreCase = true)
                    } else {
                        sizeText.equals(item.size, ignoreCase = true)
                    }
                }
                val okCount = counts.isEmpty() || counts.any { rangeText ->
                    priceRanges[rangeText]?.contains(item.price.toInt()) == true
                }
                okGender && okSize && okCount
            }

            if (filtered.isEmpty()) {
                Toast.makeText(this, "Ничего не найдено по выбранным фильтрам", Toast.LENGTH_SHORT).show()
                return
            }

            setResult(Activity.RESULT_OK, Intent().apply {
                putStringArrayListExtra("selectedGender", gendersList)
                putStringArrayListExtra("selectedSize", sizesList)
                putStringArrayListExtra("selectedCount", countsList)
            })
            finish()
        }

        private fun toggle(container: View, arrow: ImageView) {
            if (container.visibility == View.VISIBLE) {
                container.visibility = View.GONE
                arrow.setImageResource(R.drawable.ic_arrow_drop_down)
            } else {
                container.visibility = View.VISIBLE
                arrow.setImageResource(R.drawable.ic_arrow_drop_up)
            }
        }
    }