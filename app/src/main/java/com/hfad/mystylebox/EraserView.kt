package com.hfad.mystylebox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class EraserView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // Класс для хранения пути и его параметров (например, толщины кисти)
    data class ErasePath(val path: Path, val strokeWidth: Float)

    private var baseBitmap: Bitmap? = null
    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null


    private val erasePaths = mutableListOf<ErasePath>()
    private val redoPaths = mutableListOf<ErasePath>()
    private var currentPath: Path? = null
    private var currentStrokeWidth: Float = 50f

    private val eraserPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = currentStrokeWidth
    }

    private val drawMatrix = Matrix()
    private val inverseMatrix = Matrix()

    private var currentScale = 1.0f
    private val minScale = 1.0f
    private val maxScale = 5.0f

    // Детектор зума
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = (currentScale * scaleFactor).coerceIn(minScale, maxScale)
            val factor = newScale / currentScale
            currentScale = newScale
            drawMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
            invalidate()
            return true
        }
    })

    // Детектор панорамирования
    private val panDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            drawMatrix.postTranslate(-distanceX, -distanceY)
            invalidate()
            return true
        }
    })

    // Флаг режима ластика
    var isEraserEnabled = false


    //Устанавливает изображение для редактирования с сохранением пропорций и альфа-канала.
    fun setBitmap(bitmap: Bitmap) {
        // Создаем копию с конфигурацией ARGB_8888
        baseBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (width > 0 && height > 0) {
            drawingBitmap = createScaledBitmapPreservingRatio(baseBitmap!!, width, height)
            drawingBitmap?.setHasAlpha(true)
            drawingCanvas = Canvas(drawingBitmap!!)
        }
        erasePaths.clear()
        redoPaths.clear()
        resetMatrix()
        invalidate()
    }

    //Создает масштабированное изображение с сохранением пропорций.
    private fun createScaledBitmapPreservingRatio(bitmap: Bitmap, viewWidth: Int, viewHeight: Int): Bitmap {
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
        val scaledWidth: Int
        val scaledHeight: Int

        if (bitmapRatio > viewRatio) {
            scaledWidth = viewWidth
            scaledHeight = (viewWidth / bitmapRatio).toInt()
        } else {
            scaledHeight = viewHeight
            scaledWidth = (viewHeight * bitmapRatio).toInt()
        }
        // Создаем новый Bitmap, наследующий конфигурацию исходного (ARGB_8888)
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            .copy(Bitmap.Config.ARGB_8888, true)
    }

    /**
     * Возвращает текущее редактируемое изображение.
     */
    fun getBitmap(): Bitmap? {
        return drawingBitmap
    }

    /**
     * Устанавливает размер ластика (в пикселях).
     */
    fun setEraserSize(size: Float) {
        currentStrokeWidth = size
        eraserPaint.strokeWidth = size
    }

    /**
     * Undo: удаляем последний штрих.
     */
    fun undo() {
        if (erasePaths.isNotEmpty()) {
            val removed = erasePaths.removeAt(erasePaths.lastIndex)
            redoPaths.add(removed)
            redrawBitmap()
        }
    }

    /**
     * Redo: возвращаем отмененный штрих.
     */
    fun redo() {
        if (redoPaths.isNotEmpty()) {
            val path = redoPaths.removeAt(redoPaths.lastIndex)
            erasePaths.add(path)
            redrawBitmap()
        }
    }

    /**
     * Создаем локальную копию Paint с нужной толщиной для каждого штриха.
     */
    private fun getPaintForStroke(strokeWidth: Float): Paint {
        return Paint(eraserPaint).apply {
            this.strokeWidth = strokeWidth
        }
    }

    /**
     * Перерисовывает drawingBitmap: базовое изображение + все сохраненные штрихи.
     */
    private fun redrawBitmap() {
        baseBitmap?.let { base ->
            drawingBitmap = createScaledBitmapPreservingRatio(base, width, height)
            drawingBitmap?.setHasAlpha(true)
            drawingCanvas = Canvas(drawingBitmap!!)
            for (erasePath in erasePaths) {
                drawingCanvas?.drawPath(erasePath.path, getPaintForStroke(erasePath.strokeWidth))
            }
            invalidate()
        }
    }

    // При изменении размеров View масштабируем изображение и центрируем его.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        baseBitmap?.let { base ->
            drawingBitmap = createScaledBitmapPreservingRatio(base, w, h)
            drawingBitmap?.setHasAlpha(true)
            drawingCanvas = Canvas(drawingBitmap!!)
            for (erasePath in erasePaths) {
                drawingCanvas?.drawPath(erasePath.path, getPaintForStroke(erasePath.strokeWidth))
            }
            resetMatrix()
            invalidate()
        }
    }

    /**
     * Сбрасывает матрицу, центрируя изображение.
     */
    private fun resetMatrix() {
        drawingBitmap?.let { bmp ->
            drawMatrix.reset()
            val dx = (width - bmp.width) / 2f
            val dy = (height - bmp.height) / 2f
            drawMatrix.postTranslate(dx, dy)
            currentScale = 1.0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawingBitmap?.let { bitmap ->
            canvas.save()
            canvas.concat(drawMatrix)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            currentPath?.let {
                canvas.drawPath(it, getPaintForStroke(currentStrokeWidth))
            }
            canvas.restore()
        }
    }
    var multiTouchListener: (() -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            scaleDetector.onTouchEvent(event)
            panDetector.onTouchEvent(event)

            multiTouchListener?.invoke()
            return true
        } else {
            if (isEraserEnabled) {
                inverseMatrix.reset()
                drawMatrix.invert(inverseMatrix)
                val pts = floatArrayOf(event.x, event.y)
                inverseMatrix.mapPoints(pts)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        currentPath = Path().apply { moveTo(pts[0], pts[1]) }
                        redoPaths.clear()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        currentPath?.lineTo(pts[0], pts[1])
                        invalidate()
                    }
                    MotionEvent.ACTION_UP -> {
                        currentPath?.let { path ->
                            erasePaths.add(ErasePath(Path(path), currentStrokeWidth))
                            drawingCanvas?.drawPath(path, getPaintForStroke(currentStrokeWidth))
                        }
                        currentPath = null
                        invalidate()
                    }
                }
            } else {
                panDetector.onTouchEvent(event)
            }
            return true
        }
    }
}