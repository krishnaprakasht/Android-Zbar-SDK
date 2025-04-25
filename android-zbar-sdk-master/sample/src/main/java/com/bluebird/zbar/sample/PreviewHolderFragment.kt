package com.bluebird.zbar.sample

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluebird.zbar.camera.CameraPreview
import com.bluebird.zbar.camera.ScanCallback
import com.bluebird.zbar.sample.databinding.FragmentPreviewHolderBinding
import com.bluebird.zbar.sample.roomdb.Barcode
import com.bluebird.zbar.sample.roomdb.BarcodeDatabase
import com.bluebird.zbar.sample.roomdb.BarcodeRepository
import com.bluebird.zbar.sample.roomdb.BarcodeViewModel
import com.bluebird.zbar.sample.roomdb.ContinuousScan
import com.bluebird.zbar.sample.roomdb.ViewModelFactory
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min


class PreviewHolderFragment() : Fragment() {
    private val TAG = "PreviewHolderFragment"
    private var _binding: FragmentPreviewHolderBinding? = null
    private val binding get() = _binding!!
    private lateinit var mContext: Context
    private var mPreviewView: CameraPreview? = null
    private var resultView: ConstraintLayout? = null
    private lateinit var tabLayout: TabLayout
    private lateinit var restart: ToggleButton
    private lateinit var totalTimeTextView: TextView
    private lateinit var startStopToggle: ToggleButton
    private var scanStatus: Boolean = false
    private var mode = 0
    private var tempCount = 0
    private var timeElapsed: Long = 0
    private lateinit var viewModel: BarcodeViewModel
    private val lst = listOf("Single Scan Result", "Continuous Scan Result")
    private val continuousScans = mutableListOf<ContinuousScan>()
    private lateinit var switchFlashlightButton: ImageButton
    private lateinit var switchTargetButton: ImageButton
    private var isFlashOn: Boolean = false
    private var isTargetOn: Boolean = false
    private var barcodeList: MutableList<Barcode> = mutableListOf()
    private lateinit var resultList:LinearLayout
    private lateinit var adapter: BarcodeListAdapterForPreview

    companion object {
        private const val ARG_MODE = "mode"
        fun newInstance(mode: Int): PreviewHolderFragment {
            val fragment = PreviewHolderFragment()
            val args = Bundle().apply {
                putInt(ARG_MODE, mode)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mode = it.getInt(ARG_MODE, 0)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d("TAG", "onCreateView: Created fragment")
        _binding = FragmentPreviewHolderBinding.inflate(inflater, container, false)
        mContext = requireContext()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getViews()
        mPreviewView?.setScanCallback(resultCallback)
        mPreviewView?.setOverlayView(binding.overlayView, binding.overlayViewBounded, mContext)
        registerRequiredListeners()
        setupDragToExpand()
        setupListLayoutOpenOnMultIconClick()
        val database = BarcodeDatabase.getDatabase(mContext)
        val repository = BarcodeRepository(database.barcodeDao())
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(repository)
        )[BarcodeViewModel::class.java]
    }

    fun replaceFragment(
        fragmentManager: FragmentManager?,
        containerId: Int,
        fragment: Fragment?,
        addToBackStack: Boolean
    ) {
        requireNotNull(fragmentManager) { "FragmentManager cannot be null." }
        requireNotNull(fragment) { "Fragment cannot be null." }

        val transaction = fragmentManager.beginTransaction()
        transaction.replace(containerId, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }


    override fun onResume() {
        super.onResume()
        hideMainActviityLayouts()
        startProcessBasedOnTheMode()
        mPreviewView?.setTargetMode(if (isTargetOn) 1 else 0)
        if (!hasFlash()) {
            switchFlashlightButton.visibility = View.GONE
        }
        mPreviewView?.setMode(1);
    }

    override fun onPause() {
        super.onPause()
        endProcess()
        (activity as MainActivity).ViewNavAndScanSpeedLayouts()
    }


    private fun getViews() {
        mPreviewView = binding.capturePreview
        resultView = binding.resultView
        resultList = binding.resultList
        restart = binding.captureRestartScan
        startStopToggle = binding.startStopToggle
        tabLayout = binding.bottomNavigationView
        switchFlashlightButton = binding.switchFlashlight
        switchTargetButton = binding.imageViewTarget
    }

    private fun hideMainActviityLayouts() {
        (activity as MainActivity).hideNavAndScanSpeedLayouts()
    }

    private fun registerRequiredListeners() {
        restart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startScan()
            } else {
                stopScan()
            }
        }
        switchFlashlightButton.setOnClickListener {
            val ret = mPreviewView?.toggleFlash()
            if (ret == true) {
                switchFlashlightButton.setImageResource(R.drawable.ic_flash_on_white_36dp)
            } else {
                switchFlashlightButton.setImageResource(R.drawable.ic_flash_off_white_36dp)
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                mode = tab?.position ?: 0;
                mPreviewView?.setMode(mode);
//                binding.mode.text = lst[mode]
                if (mode == 1) {
                    registerStartStopToggle();
                } else {
                    if (startStopToggle.isChecked) {
                        startStopToggle.setChecked(false);
                        Log.d("TAG", "onTabSelected: ")
                        if (scanStatus) {
                            stopScan()
                        }
                    }
                    unregisterStartStopToggle();
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

//        binding.textViewHistory.setOnClickListener {
//            replaceFragment(fragmentManager, R.id.fragment_container, HistoryFragment(), true)
//        }
//
//        binding.imageViewHistory.setOnClickListener {
//            replaceFragment(fragmentManager, R.id.fragment_container, HistoryFragment(), true)
//        }

        switchTargetButton.setOnClickListener {
            Log.d("TAG", "registerRequiredListeners: ")
            if (!isTargetOn) {
                switchTargetButton.setImageResource(R.drawable.target_blue)
                isTargetOn = true
            } else {
                switchTargetButton.setImageResource(R.drawable.target)
                isTargetOn = false
            }
            mPreviewView?.setTargetMode(if (isTargetOn) 1 else 0)
            Log.d("TAG", "registerRequiredListeners: ${if (isTargetOn) 1 else 0}")
        }
    }

    private fun startProcessBasedOnTheMode() {
        if (mode == 0 && !scanStatus) {
            startScan()
        } else {
            tabLayout.selectTab(tabLayout.getTabAt(mode))
            startStopToggle.isChecked = true
            startScan()
        }
//        binding.mode.text = lst[mode]
    }


    private fun endProcess() {
        if (scanStatus) {
            stopScan()
        }
        startStopToggle.setOnClickListener(null)
    }

    private fun hasFlash(): Boolean {
        return (mContext).packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }


    private fun startScan() {
        val ret = mPreviewView?.start()
        tempCount = 0
        scanStatus = true
        ret?.let {
            if (!it) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.camera_failure)
                    .setMessage(R.string.camera_hint)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { _, _ -> requireActivity().finish() }
                    .show()

            } else {
                timeElapsed = System.currentTimeMillis()
                resultList.visibility = View.GONE
                restart.isChecked =true
            }
        }

    }

    private fun stopScan() {
        mPreviewView?.stop()
        Log.d("TAG", "stopScan: ")
        scanStatus = false
        restart.isChecked = false
    }


    private fun registerStartStopToggle() {
        stopScan()
        startStopToggle.setOnClickListener { v ->
            if (startStopToggle.isChecked) {
                startScan()

            } else {
                addResultToDBContinuousScan()
                stopScan()
            }
        }
        startStopToggle.isVisible = true
        restart.visibility = View.GONE
    }

    private fun unregisterStartStopToggle() {
        startStopToggle.visibility = View.INVISIBLE
        restart.visibility = View.VISIBLE
        startStopToggle.setOnClickListener(null)
        mPreviewView?.setMode(0);
    }

    private val resultCallback = object : ScanCallback {
        override fun onScanResult(result: String) {
            // Existing focus handling
            if (result == "focus") {
                mPreviewView?.getFocus()
                return
            }

            // Parse result and validate
            val parsedData = parseResult(result)
            var tagId = ""
            var symbol = ""
            var decoderTime = 0f
            var focusTime = 0L
            if (parsedData != null) {
                tagId = parsedData[0]
                symbol = parsedData[1]
                decoderTime = parsedData[2].toFloat()
                focusTime = parsedData[3].toLong()
            } else {
                println("Invalid format")
                return
            }

//            addTimeProfile(focusTime, decoderTime, System.currentTimeMillis() - timeElapsed)

            // Check if scanning is active
            if ((!startStopToggle.isChecked && mode == 1) || (mode == 0 && !restart.isChecked)) {
                stopScan()
                return
            }

            // Find existing barcode
            val existingBarcode = barcodeList.find { it.barcode == tagId && it.symbol == symbol }

            if (existingBarcode != null) {
                // Update count and move to top
                existingBarcode.count++
                existingBarcode.timeStamp = ((System.currentTimeMillis() - timeElapsed)/ 1000f).toString()
                val index = barcodeList.indexOf(existingBarcode)
                if (index != -1) {
                    barcodeList.removeAt(index)
                    barcodeList.add(0, existingBarcode)
                }
            } else {
                // Add new entry at the top
                barcodeList.add(
                    0, // Insert at index 0 (top)
                    Barcode(
                        mode = mode,
                        barcode = tagId,
                        symbol = symbol,
                        count = 1,
                        timeStamp = ((System.currentTimeMillis() - timeElapsed)/ 1000f).toString()
                    )
                )
            }

            updateResultsUI() // Refresh UI with new order
            resultList.isVisible = true

            // Handle mode-specific logic
            if (mode == 0) {
                stopScan()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                addResultToDBSingleScan(tagId, tempCount, symbol, dateFormat.format(Date()))
            } else {
                addResultToMap(tagId, tempCount, symbol)
            }
        }
    }
    private fun setupListLayoutOpenOnMultIconClick() {
        val button = binding.multipleScansIcon

        val displayMetrics = resources.displayMetrics
        val maxHeight = (displayMetrics.heightPixels * 0.55).toInt()

        button.setOnClickListener {
            if (resultList.visibility == View.GONE) {
                resultList.visibility = View.VISIBLE
                val layoutParams = resultList.layoutParams
                layoutParams.height = maxHeight
                resultList.layoutParams = layoutParams
            } else {
                resultList.visibility = View.GONE
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragToExpand() {
        val displayMetrics = resources.displayMetrics
        val maxHeight = (displayMetrics.heightPixels * 0.55).toInt()

        var initialY = 0f
        var initialHeight = resultList.height

        // Get single row height dynamically
        val minHeightThreshold = binding.itemContainer.getChildAt(0)?.height ?: 185

        resultList.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    initialHeight = resultList.height
                }

                MotionEvent.ACTION_MOVE -> {
                    val diffY = (event.rawY - initialY).toInt()
                    val newHeight = initialHeight - diffY

                    if (newHeight in minHeightThreshold..maxHeight) {
                        resultList.layoutParams.height = newHeight
                        resultList.requestLayout()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    val diffY = (event.rawY - initialY).toInt()
                    val newHeight = initialHeight - diffY

                    when {
                        newHeight >= (maxHeight-400) -> {
                            animateHeightChange(resultList, newHeight, maxHeight)
                        }
                        newHeight <= minHeightThreshold -> {
                            animateHeightChange(resultList, newHeight, 0) {
                                resultList.visibility = View.GONE
                            }
                        }
                        else -> {
                            animateHeightChange(resultList, newHeight, minHeightThreshold + 200)
                        }
                    }
                }
            }
            true
        }
    }

    private fun animateHeightChange(view: View, fromHeight: Int, toHeight: Int, onEnd: (() -> Unit)? = null) {
        ValueAnimator.ofInt(fromHeight, toHeight).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                view.layoutParams.height = animator.animatedValue as Int
                view.requestLayout()
            }
            doOnEnd { onEnd?.invoke() }
            start()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateResultsUI() {
        // Initialize adapter if not already done
        if (!::adapter.isInitialized) {
            adapter = BarcodeListAdapterForPreview(mutableListOf())
            binding.itemContainer.layoutManager = LinearLayoutManager(mContext)
            binding.itemContainer.adapter = adapter
        }

        // Update visibility and scan count badge
        binding.multipleScansCount.visibility = if (barcodeList.isEmpty()) View.GONE else View.VISIBLE
        binding.multipleScansCount.text = barcodeList.size.toString()

        // Update RecyclerView data
        adapter.updateData(barcodeList)

        // Dynamically adjust RecyclerView height based on the number of items
        binding.itemContainer.post {
            if (adapter.itemCount > 0) {
                val singleItemHeight = binding.itemContainer.getChildAt(0).height + 160

                binding.resultList.layoutParams.height = min(singleItemHeight, 600)
                binding.resultList.requestLayout()
                binding.resultList.visibility = View.VISIBLE
            }
        }
    }

    private fun addResultToDBSingleScan(
        result: String,
        cnt: Int,
        symbology: String,
        currentDataTime: String
    ) {
        val barcode = Barcode(
            mode = mode,
            barcode = result,
            symbol = symbology,
            count = 1,
            timeStamp = currentDataTime
        )
        viewModel.insertBarcode(barcode)

    }

    private fun addResultToDBContinuousScan() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.format(Date())
        for (scan in continuousScans) {
            val barcode = Barcode(
                mode = mode,
                barcode = scan.barcode,
                symbol = scan.symbol,
                count = scan.count,
                timeStamp = date
            )
            viewModel.insertBarcode(barcode)
        }
    }

    private fun addResultToMap(result: String, cnt: Int, symbology: String) {
        val existingEntry = continuousScans.find { it.barcode == result && it.symbol == symbology }
        if (existingEntry != null) {
            existingEntry.count = existingEntry.count + 1
        } else {
            val newScan = ContinuousScan(
                barcode = result,
                symbol = symbology,
                count = 1
            )
            continuousScans.add(newScan)
        }

    }


    private fun parseResult(result: String): List<String>?{
        val parts = result.split(";")
        return if (parts.size == 4) {
            listOf(parts[0], parts[1],parts[2],parts[3])
        } else {
            null
        }
    }

//    @SuppressLint("SetTextI18n")
//    private fun addTimeProfile(focusTimeMillis: Long, decoderTimeMillis: Float, totalTimeMillis: Long) {
//        // Convert milliseconds to seconds
//        val focusTime = focusTimeMillis / 1000f
//        val decoderTimeMicro = decoderTimeMillis * 1000 // Convert milliseconds to microseconds
//        val totalTime = totalTimeMillis / 1000f
//
//        // Calculate the running average for focus time
//        TimeProfile.focusTime = if (TimeProfile.focusTime == 0f) {
//            focusTime
//        } else {
//            (TimeProfile.focusTime + focusTime) / 2
//        }
//
//        // Calculate the running average for decoder time (in microseconds)
//        TimeProfile.decoderTime = if (TimeProfile.decoderTime == 0f) {
//            decoderTimeMicro
//        } else {
//            (TimeProfile.decoderTime + decoderTimeMicro) / 2
//        }
//
//        if (mode!=1) {
//            // Calculate the running average for total time
//            TimeProfile.totalTime = if (TimeProfile.totalTime == 0f) {
//                totalTime
//            } else {
//                (TimeProfile.totalTime + totalTime) / 2
//            }
//        }
//
//        (mContext as MainActivity).findViewById<TextView>(R.id.textViewScanSpeed).text =
//            "Avg Decoder: ${TimeProfile.decoderTime}μs Total : ${TimeProfile.totalTime}s"
//    }


}