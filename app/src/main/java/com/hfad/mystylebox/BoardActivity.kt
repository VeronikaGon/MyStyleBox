package com.hfad.mystylebox

import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide

data class ImageState(
    var alpha: Float = 1f,
    var rotation: Float = 0f,
    var scale: Float = 1f
)

class BoardActivity : AppCompatActivity() {

    private lateinit var boardContainer: FrameLayout
    private lateinit var llDialogAdjustment: LinearLayout
    private lateinit var llButtons: LinearLayout
    private lateinit var llClothes: LinearLayout
    private lateinit var textViewAdjustment: TextView
    private lateinit var seekBar: androidx.appcompat.widget.AppCompatSeekBar

    private lateinit var btnForeground: ImageButton
    private lateinit var btnBackground: ImageButton
    private lateinit var btnBlur: ImageButton
    private lateinit var btnTurn: ImageButton
    private lateinit var btnScaling: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var addClothesButton: ImageButton

    private val boardItems = mutableListOf<ImageView>()
    private val thumbnailMapping = mutableMapOf<ImageView, ImageView>()
    private val imageStates = mutableMapOf<ImageView, ImageState>()
    private var selectedView: ImageView? = null

    private var currentRotation: Float = 0f
    private var currentAlpha: Float = 1f
    private var currentScale: Float = 1f

    private var adjustmentType: String? = null
    private var currentAdjustmentButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        boardContainer = findViewById(R.id.boardContainer)
        llDialogAdjustment = findViewById(R.id.lldialog_adjustment)
        llButtons = findViewById(R.id.ll)
        llClothes = findViewById(R.id.llclothes)
        textViewAdjustment = findViewById(R.id.textViewAdjustment)
        seekBar = findViewById(R.id.seekBarAdjustment)

        btnForeground = findViewById(R.id.btnforeground)
        btnBackground = findViewById(R.id.btnbackground)
        btnBlur = findViewById(R.id.btnblur)
        btnTurn = findViewById(R.id.btnturn)
        btnScaling = findViewById(R.id.btnscaling)
        btnDelete = findViewById(R.id.btnDelete)
        addClothesButton = findViewById(R.id.addclothes)

        setAdjustmentContainerVisibility(View.GONE)

        val selectedPaths = intent.getStringArrayListExtra("selected_items") ?: arrayListOf()
        selectedPaths.forEach { path ->
            addBoardItem(path)
        }

        // Обработчики для кнопок нижней панели
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
            startActivity(Intent(this, ClothingSelectionActivity::class.java))
        }

        btnBlur.setOnClickListener {
            if (ensureItemSelected()) {
                toggleAdjustment("blur", btnBlur)
            }
        }
        btnTurn.setOnClickListener {
            if (ensureItemSelected()) {
                toggleAdjustment("turn", btnTurn)
            }
        }
        btnScaling.setOnClickListener {
            if (ensureItemSelected()) {
                toggleAdjustment("scale", btnScaling)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textViewAdjustment.text = getAdjustmentText(progress)
                applyAdjustment(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                adjustmentType?.let { updateAdjustmentUI(it) }
            }
        })

        boardContainer.setOnClickListener {
            deselectAll()
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
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    // Метод, проверяющий, выбран ли объект. Если нет - уведомляем пользователя.
    private fun ensureItemSelected(): Boolean {
        return if (selectedView == null) {
            Toast.makeText(this, "Выберите вещь для выполнения операции", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    // Метод для добавления нового объекта на доску
    private fun addBoardItem(path: String) {
        val imageView = ImageView(this)
        Glide.with(this)
            .load(path)
            .into(imageView)
        val layoutParams = FrameLayout.LayoutParams(300, 300)
        imageView.layoutParams = layoutParams
        imageView.setBackgroundResource(0)
        imageView.setOnTouchListener(MultiTouchListener(this))
        imageView.setOnClickListener { view ->
            selectBoardItem(view as ImageView)
        }
        boardContainer.addView(imageView)
        boardItems.add(imageView)
        addThumbnail(imageView, path)
        imageStates[imageView] = ImageState()

        // Устанавливаем начальные параметры изображения
        imageView.rotation = 0f
        imageView.alpha = 1f
        imageView.scaleX = 1f
        imageView.scaleY = 1f
    }

    // Добавление миниатюры в нижний LinearLayout (llclothes)
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

    // Выбор объекта: снимаем выделение с предыдущего и устанавливаем обводку для выбранного
    private fun selectBoardItem(view: ImageView) {
        deselectAll()
        selectedView = view
        view.setBackgroundResource(R.drawable.black_border)
        thumbnailMapping[view]?.setBackgroundResource(R.drawable.black_border)
        val state = imageStates.getOrPut(view) { ImageState() }
        updateAdjustmentUI(adjustmentType ?: "blur")
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

    // Установка видимости контейнера настроек и изменение отступа нижней панели
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

    // Переключение режима настройки
    private fun toggleAdjustment(type: String, button: ImageButton) {
        if (adjustmentType == type) {
            adjustmentType = null
            currentAdjustmentButton?.setColorFilter(null)
            currentAdjustmentButton = null
            setAdjustmentContainerVisibility(View.GONE)
        } else {
            adjustmentType = type
            currentAdjustmentButton?.setColorFilter(null)
            button.setColorFilter(resources.getColor(android.R.color.white))
            currentAdjustmentButton = button
            setAdjustmentContainerVisibility(View.VISIBLE)
            updateAdjustmentUI(type)
        }
    }

    // Обновление UI контейнера настроек в зависимости от режима и текущего состояния выбранного объекта
    private fun updateAdjustmentUI(type: String) {
        selectedView?.let { view ->
            val state = imageStates[view] ?: ImageState()

            when (type) {
                "blur" -> {
                    textViewAdjustment.text = "Прозрачность"
                    seekBar.max = 100
                    // Переводим alpha в диапазон progress (учтите, что минимальное значение – 0.2f)
                    val progress = ((state.alpha - 0.2f) * 100).toInt().coerceAtLeast(0)
                    seekBar.progress = progress
                    seekBar.thumb = resources.getDrawable(R.drawable.ic_blur, theme)
                }
                "turn" -> {
                    textViewAdjustment.text = "Поворот"
                    seekBar.max = 360
                    // Нормализуем угол в диапазон 0-360
                    val normalizedRotation = ((state.rotation % 360) + 360) % 360
                    seekBar.progress = normalizedRotation.toInt()
                    seekBar.thumb = resources.getDrawable(R.drawable.ic_turn, theme)
                }
                "scale" -> {
                    textViewAdjustment.text = "Масштабирование"
                    seekBar.max = 100
                    // Преобразование текущего масштаба в progress
                    val progress = (((state.scale - 0.5f) / 4.5f) * 100).toInt()
                    seekBar.progress = progress.coerceIn(0, 100)
                    seekBar.thumb = resources.getDrawable(R.drawable.ic_scaling, theme)
                }
            }
        }
    }

    // Формирование строки с текущим значением настройки
    private fun getAdjustmentText(progress: Int): String {
        return when (adjustmentType) {
            "blur" -> "Прозрачность"
            "turn" -> "Поворот"
            "scale" -> "Масштабирование"
            else -> ""
        }
    }

    // Применение выбранного эффекта к выбранному объекту без сброса других настроек
    private fun applyAdjustment(progress: Int) {
        selectedView?.let { view ->
            val state = imageStates.getOrPut(view) { ImageState() }
            when (adjustmentType) {
                "blur" -> {
                    val newAlpha = 0.2f + progress / 100f
                    view.alpha = newAlpha
                    state.alpha = newAlpha
                }
                "turn" -> {
                    val newRotation = progress.toFloat()
                    view.rotation = newRotation
                    state.rotation = newRotation
                }
                "scale" -> {
                    val newScale = 0.5f + (progress / 100f) * 4.5f
                    view.scaleX = newScale
                    view.scaleY = newScale
                    state.scale = newScale
                }
            }
            // Обновляем состояние в мапе
            imageStates[view] = state
        }
    }

    // Перемещение выбранного объекта на передний план и обновление порядка миниатюр
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

    // Отправка выбранного объекта на задний план и обновление порядка миниатюр
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

    // Метод обновления порядка миниатюр в llclothes согласно порядку объектов на доске.
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

    // Удаление выбранного объекта с доски (при этом миниатюра остаётся)
    private fun deleteItem() {
        selectedView?.let { view ->
            boardContainer.removeView(view)
            boardItems.remove(view)
            thumbnailMapping[view]?.let { thumb ->
                llClothes.removeView(thumb)
            }
            thumbnailMapping.remove(view)
            selectedView = null
        }
    }

    // Класс для обработки жестов масштабирования, поворота и перемещения
    inner class MultiTouchListener(context: Context) : View.OnTouchListener {
        private val scaleGestureDetector: ScaleGestureDetector
        private val rotationGestureDetector: RotationGestureDetector
        private var lastPoint = PointF()
        private var baseScale = currentScale
        private var isScaling = false

        init {
            scaleGestureDetector = ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                        baseScale = currentScale
                        isScaling = true
                        return true
                    }

                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        selectedView?.let { view ->
                            val newScale = baseScale * detector.scaleFactor
                            val clampedScale = newScale.coerceIn(0.2f, 5f)
                            view.scaleX = clampedScale
                            view.scaleY = clampedScale
                            currentScale = clampedScale
                            imageStates[view]?.scale = clampedScale // Сохранение состояния
                        }
                        return true
                    }

                    override fun onScaleEnd(detector: ScaleGestureDetector) {
                        isScaling = false
                    }
                })

            rotationGestureDetector =
                RotationGestureDetector(object : RotationGestureDetector.OnRotationGestureListener {
                    override fun onRotation(angle: Float) {
                        // Если идет масштабирование, то игнорируем поворот
                        if (isScaling) return

                        selectedView?.let { view ->
                            view.rotation += angle
                            currentRotation = view.rotation
                            imageStates[view]?.rotation = view.rotation // Сохранение состояния
                        }
                    }
                })
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                lastPoint.set(event.rawX, event.rawY)
                if (view is ImageView && selectedView != view) {
                    selectBoardItem(view)
                }
            }
            scaleGestureDetector.onTouchEvent(event)
            rotationGestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastPoint.x
                    val dy = event.rawY - lastPoint.y
                    view.x += dx
                    view.y += dy
                    lastPoint.set(event.rawX, event.rawY)
                }
            }
            return true
        }
    }

    // Класс для распознавания жеста поворота с использованием двух пальцев
    class RotationGestureDetector(private val mListener: OnRotationGestureListener) {
        interface OnRotationGestureListener {
            fun onRotation(angle: Float)
        }

        private var fX = 0f
        private var fY = 0f
        private var sX = 0f
        private var sY = 0f

        fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount >= 2) {
                        fX = event.getX(0)
                        fY = event.getY(0)
                        sX = event.getX(1)
                        sY = event.getY(1)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount >= 2) {
                        val nfX = event.getX(0)
                        val nfY = event.getY(0)
                        val nsX = event.getX(1)
                        val nsY = event.getY(1)
                        val angle1 = Math.atan2((sY - fY).toDouble(), (sX - fX).toDouble())
                        val angle2 = Math.atan2((nsY - nfY).toDouble(), (nsX - nfX).toDouble())
                        val angle = Math.toDegrees(angle2 - angle1).toFloat()
                        mListener.onRotation(angle)
                        fX = nfX
                        fY = nfY
                        sX = nsX
                        sY = nsY
                    }
                }
            }
            return true
        }
    }
}
// Дополнительное расширение для форматирования чисел (опционально)
fun Float.format(digits: Int) = "%.${digits}f".format(this)