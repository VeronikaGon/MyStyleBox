package com.hfad.mystylebox.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.slider.Slider
import com.hfad.mystylebox.MainActivity
import com.hfad.mystylebox.R
import java.io.File
import java.io.FileOutputStream

data class ImageState(
    var alpha: Float = 1f,
    var rotation: Float = 0f,
    var scale: Float = 1f
)

class BoardViewModel : androidx.lifecycle.ViewModel() {
    private val _imageStates = androidx.lifecycle.MutableLiveData<MutableMap<String, ImageState>>(mutableMapOf())
    val imageStates: androidx.lifecycle.LiveData<MutableMap<String, ImageState>> = _imageStates

    fun updateImageState(imageId: String, newState: ImageState) {
        val currentStates = _imageStates.value ?: mutableMapOf()
        currentStates[imageId] = newState
        _imageStates.value = currentStates
    }

    fun getImageState(imageId: String): ImageState {
        return _imageStates.value?.get(imageId) ?: ImageState()
    }
}

class BoardActivity : AppCompatActivity() {

    private lateinit var boardContainer: FrameLayout
    private lateinit var llDialogAdjustment: LinearLayout
    private lateinit var llButtons: LinearLayout
    private lateinit var llClothes: LinearLayout
    private lateinit var textViewAdjustment: TextView
    private lateinit var slider: Slider

    private lateinit var btnForeground: ImageButton
    private lateinit var btnBackground: ImageButton
    private lateinit var btnBlur: ImageButton
    private lateinit var btnTurn: ImageButton
    private lateinit var btnScaling: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var addClothesButton: ImageButton

    private val boardItems = mutableListOf<ImageView>()
    private val thumbnailMapping = mutableMapOf<ImageView, ImageView>()
    private var selectedView: ImageView? = null

    private lateinit var viewModel: BoardViewModel

    private var adjustmentType: String? = null
    private var currentAdjustmentButton: ImageButton? = null
    private val selectedIds = mutableListOf<Int>()
    private val itemIdMapping = mutableMapOf<ImageView, Int>()

    companion object {
        private const val REQUEST_CODE_ADD_ITEMS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        // Инициализация View
        boardContainer = findViewById(R.id.boardContainer)
        llDialogAdjustment = findViewById(R.id.lldialog_adjustment)
        llButtons = findViewById(R.id.ll)
        llClothes = findViewById(R.id.llclothes)
        textViewAdjustment = findViewById(R.id.textViewAdjustment)
        slider = findViewById(R.id.sliderAdjustment)

        btnForeground = findViewById(R.id.btnforeground)
        btnBackground = findViewById(R.id.btnbackground)
        btnBlur = findViewById(R.id.btnblur)
        btnTurn = findViewById(R.id.btnturn)
        btnScaling = findViewById(R.id.btnscaling)
        btnDelete = findViewById(R.id.btnDelete)
        addClothesButton = findViewById(R.id.addclothes)
        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Подтверждение сохранения")
                .setMessage("Вы уверены, что хотите сохранить этот комплект?")
                .setPositiveButton("Да") { dialog, which ->
                    saveBoardImage()
                }
                .setNegativeButton("Нет", null)
                .show()
        }

        setAdjustmentContainerVisibility(View.GONE)

        viewModel = ViewModelProvider(this).get(BoardViewModel::class.java)
        val passedImagePaths =
            intent.getStringArrayListExtra("selected_image_paths") ?: arrayListOf()
        val passedIds = intent.getIntegerArrayListExtra("selected_item_ids") ?: arrayListOf()
        Log.d("BoardActivity", "Полученные пути изображений: $passedImagePaths")
        Log.d("BoardActivity", "Полученные идентификаторы: $passedIds")
        selectedIds.addAll(passedIds)
        for (i in passedImagePaths.indices) {
            addBoardItem(passedImagePaths[i], passedIds[i])
        }

        btnForeground.setOnClickListener {
            if (ensureItemSelected()) {
                bringToFront()
            }
        }
        btnBackground.setOnClickListener {
            if (ensureItemSelected()) {
                sendToBack()
            }
        }
        btnDelete.setOnClickListener {
            if (ensureItemSelected()) {
                deleteItem()
            }
        }
        addClothesButton.setOnClickListener {
            llClothes.removeView(addClothesButton)
            llClothes.addView(addClothesButton)
            val lockedPaths = boardItems.map { it.tag.toString() }
            val intent = Intent(this, ClothingSelectionActivity::class.java)
            intent.putExtra("fromBoard", true)
            intent.putStringArrayListExtra("locked_items", ArrayList(lockedPaths))
            startActivityForResult(intent, REQUEST_CODE_ADD_ITEMS)
        }

        btnBlur.setOnClickListener {
            if (ensureItemSelected()) {
                toggleAdjustment("alpha", btnBlur)
            }
        }
        btnTurn.setOnClickListener {
            if (ensureItemSelected()) {
                toggleAdjustment("rotation", btnTurn)
            }
        }
        btnScaling.setOnClickListener {
            if (ensureItemSelected()) {
                toggleAdjustment("scale", btnScaling)
            }
        }

        slider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                selectedView?.let { view ->
                    val imageId = view.tag.toString()
                    val currentState = viewModel.getImageState(imageId)
                    val newState = currentState.copy().apply {
                        when (adjustmentType) {
                            "alpha" -> alpha = 0.2f + value / slider.valueTo
                            "rotation" -> rotation = value
                            "scale" -> scale = 0.5f + (value / slider.valueTo) * 4.5f
                        }
                    }
                    viewModel.updateImageState(imageId, newState)
                    applyNewState(newState)
                }
            }
        }

        boardContainer.setOnClickListener {
            deselectAll()
        }
    }

    private fun saveBoardImage() {
        val boardView = findViewById<View>(R.id.boardContainer)
        var originalBackground: Drawable? = null
        selectedView?.let { view ->
            originalBackground = view.background
            view.setBackgroundResource(R.drawable.no_border)
        }

        val bitmap = Bitmap.createBitmap(boardView.width, boardView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        boardView.draw(canvas)

        val uniqueFileName = "boardImage_${System.currentTimeMillis()}.png"
        val file = File(externalCacheDir, uniqueFileName)
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            selectedView?.background = originalBackground

            val intent = Intent(this, OutfitActivity::class.java)
            intent.putExtra("imagePath", file.path)
            intent.putIntegerArrayListExtra(
                "selected_clothing_ids",
                ArrayList(itemIdMapping.values.distinct())
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Вы уверены?")
            .setMessage("Вы точно хотите уйти? Комплект не сохранится!")
            .setPositiveButton("Да") { dialog, which ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("openFragment", "outfits")
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    // Проверка, выбран ли объект
    private fun ensureItemSelected(): Boolean {
        return if (selectedView == null) {
            Toast.makeText(this, "Выберите вещь для выполнения операции", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    // Добавление изображения на доску
    private fun addBoardItem(path: String, clothingItemId: Int) {
        val imageView = ImageView(this)
        Glide.with(this).load(path).into(imageView)
        val layoutParams = FrameLayout.LayoutParams(300, 300)
        imageView.layoutParams = layoutParams
        imageView.setBackgroundResource(0)
        imageView.setOnClickListener { view -> selectBoardItem(view as ImageView) }
        imageView.setOnTouchListener(MultiTouchListener())
        boardContainer.addView(imageView)
        boardItems.add(imageView)
        addThumbnail(imageView, path)
        imageView.tag = path
        viewModel.updateImageState(path, ImageState())
        imageView.rotation = 0f
        imageView.alpha = 1f
        imageView.scaleX = 1f
        imageView.scaleY = 1f
        val offset = boardItems.size * 20
        imageView.x = offset.toFloat()
        imageView.y = offset.toFloat()
        selectedIds.add(clothingItemId)
        itemIdMapping[imageView] = clothingItemId
        selectBoardItem(imageView)
    }

    // Обработка результата из ClothingSelectionActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_ITEMS && resultCode == RESULT_OK) {
            val newPaths = data?.getStringArrayListExtra("selected_image_paths")
            val newIds = data?.getIntegerArrayListExtra("selected_item_ids")
            if (newPaths != null && newIds != null && newPaths.size == newIds.size) {
                for (i in newPaths.indices) {
                    if (boardItems.none { it.tag == newPaths[i] }) {
                        addBoardItem(newPaths[i], newIds[i])
                    }
                }
            } else {
                Toast.makeText(this, "Данные о новых вещах не получены", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Добавление миниатюры для изображения
    private fun addThumbnail(boardItem: ImageView, path: String) {
        val thumbnail = ImageView(this)
        val thumbParams = LinearLayout.LayoutParams(100, 100)
        thumbParams.marginEnd = 8
        thumbnail.layoutParams = thumbParams
        Glide.with(this)
            .load(path)
            .into(thumbnail)
        thumbnail.setBackgroundResource(R.drawable.thumbnail_border)
        thumbnail.setOnClickListener { selectBoardItem(boardItem) }
        val addIndex = llClothes.indexOfChild(addClothesButton)
        llClothes.addView(thumbnail, addIndex)
        thumbnailMapping[boardItem] = thumbnail
    }

    // Выбор изображения с доски
    private fun selectBoardItem(view: ImageView) {
        deselectAll()
        selectedView = view
        view.setBackgroundResource(R.drawable.black_border)
        thumbnailMapping[view]?.setBackgroundResource(R.drawable.black_border)
        val imageId = view.tag.toString()
        val state = viewModel.getImageState(imageId)
        updateAdjustmentUI(state)
    }

    // Снятие выделения
    private fun deselectAll() {
        selectedView?.let { view ->
            view.setBackgroundResource(R.drawable.no_border)
            thumbnailMapping[view]?.setBackgroundResource(R.drawable.thumbnail_border)
        }
        selectedView = null
        currentAdjustmentButton?.setColorFilter(null)
        currentAdjustmentButton = null
        adjustmentType = null
        setAdjustmentContainerVisibility(View.GONE)
    }

    // Управление видимостью контейнера настроек
    private fun setAdjustmentContainerVisibility(visibility: Int) {
        llDialogAdjustment.visibility = visibility
        val marginTop = if (visibility == View.VISIBLE) 0 else dpToPx(16)
        llButtons.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = marginTop
        }
    }

    // Преобразование dp в пиксели
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    // Переключение режима настройки: обновление типа и UI
    private fun toggleAdjustment(type: String, button: ImageButton) {
        if (adjustmentType == type) {
            adjustmentType = null
            currentAdjustmentButton?.setColorFilter(null)
            currentAdjustmentButton = null
            setAdjustmentContainerVisibility(View.GONE)
        } else {
            adjustmentType = type
            currentAdjustmentButton?.setColorFilter(null)
            button.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
            currentAdjustmentButton = button
            setAdjustmentContainerVisibility(View.VISIBLE)
            selectedView?.let {
                val imageId = it.tag.toString()
                val state = viewModel.getImageState(imageId)
                updateAdjustmentUI(state)
            }
        }
    }

    // Обновление UI контейнера настроек в зависимости от выбранного типа
    private fun updateAdjustmentUI(state: ImageState) {
        when (adjustmentType ?: "alpha") {
            "alpha" -> {
                textViewAdjustment.text = "Прозрачность"
                // Для Slider задаём диапазон значений от 0 до 100
                slider.valueFrom = 0f
                slider.valueTo = 100f
                val progress = ((state.alpha - 0.2f) * 100).toInt().coerceAtLeast(0)
                slider.value = progress.toFloat()
            }

            "rotation" -> {
                textViewAdjustment.text = "Поворот"
                slider.valueFrom = 0f
                slider.valueTo = 360f
                val normalizedRotation = (((state.rotation % 360) + 360) % 360).toFloat()
                slider.value = normalizedRotation
            }

            "scale" -> {
                textViewAdjustment.text = "Масштабирование"
                slider.valueFrom = 0f
                slider.valueTo = 100f
                val progress = (((state.scale - 0.5f) / 4.5f) * 100).toInt().coerceIn(0, 100)
                slider.value = progress.toFloat()
            }
        }
    }

    // Применение нового состояния к выбранному ImageView
    private fun applyNewState(state: ImageState) {
        selectedView?.apply {
            alpha = state.alpha
            rotation = state.rotation
            scaleX = state.scale
            scaleY = state.scale
        }
    }

    // Перемещение изображения на передний план
    private fun bringToFront() {
        selectedView?.let { view ->
            val currentIndex = boardContainer.indexOfChild(view)
            if (currentIndex < boardContainer.childCount - 1) {
                boardContainer.removeView(view)
                boardContainer.addView(view, currentIndex + 1)
            }
            boardContainer.invalidate()
            updateThumbnailOrder()
        }
    }

    // Отправка изображения на задний план
    private fun sendToBack() {
        selectedView?.let { view ->
            val currentIndex = boardContainer.indexOfChild(view)
            if (currentIndex > 0) {
                boardContainer.removeView(view)
                boardContainer.addView(view, currentIndex - 1)
            }
            updateThumbnailOrder()
        }
    }

    // Обновление порядка миниатюр
    private fun updateThumbnailOrder() {
        val currentThumbs = mutableListOf<View>()
        for (i in 0 until llClothes.childCount) {
            val child = llClothes.getChildAt(i)
            if (child != addClothesButton) {
                currentThumbs.add(child)
            }
        }
        currentThumbs.forEach { llClothes.removeView(it) }
        llClothes.removeAllViews()
        for (i in 0 until boardContainer.childCount) {
            val child = boardContainer.getChildAt(i)
            if (child is ImageView && thumbnailMapping.containsKey(child)) {
                thumbnailMapping[child]?.let { thumb ->
                    llClothes.addView(thumb)
                }
            }
        }
        llClothes.addView(addClothesButton)
    }

    // Удаление изображения с доски
    private fun deleteItem() {
        selectedView?.let { view ->
            AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Вы уверены, что хотите удалить выбранную вещь?")
                .setPositiveButton("Да") { _, _ ->
                    boardContainer.removeView(view)
                    boardItems.remove(view)
                    thumbnailMapping[view]?.let { thumb ->
                        llClothes.removeView(thumb)
                    }
                    thumbnailMapping.remove(view)
                    itemIdMapping[view]?.let { realId ->
                        selectedIds.remove(realId)
                        itemIdMapping.remove(view)
                    }
                    selectedView = null
                    setAdjustmentContainerVisibility(View.GONE)
                    Toast.makeText(this, "Вещь удалена", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}
// Класс MultiTouchListener для перетаскивания изображения на доске.
class MultiTouchListener : View.OnTouchListener {
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                isDragging = false
                view.performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastX
                val dy = event.rawY - lastY
                if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                    view.x += dx
                    view.y += dy
                    lastX = event.rawX
                    lastY = event.rawY
                    isDragging = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    view.performClick()
                }
            }
        }
        return true
    }
}