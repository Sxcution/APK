package com.sxcution.app.dialogs

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.sxcution.app.R

class SettingsDialogFragment : DialogFragment() {
    
    companion object {
        const val TAG = "SettingsDialogFragment"
        private const val PREFS_NAME = "sxcution_settings"
        private const val KEY_MOVEMENT_SIMULATION = "movement_simulation_enabled"
        private const val KEY_MOVEMENT_SPEED = "movement_speed"
        private const val KEY_ZOOM_WITH_MARKER = "zoom_with_marker_enabled"
        private const val KEY_BEARING_ROTATION = "bearing_rotation_enabled"
        
        fun newInstance(): SettingsDialogFragment {
            return SettingsDialogFragment()
        }
    }
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var switchMovementSimulation: Switch
    private lateinit var seekbarMovementSpeed: SeekBar
    private lateinit var tvSpeedValue: TextView
    private lateinit var switchZoomWithMarker: Switch
    private lateinit var switchBearingRotation: Switch
    private lateinit var btnClose: Button
    
    // Callback interface
    interface OnSettingsChangedListener {
        fun onMovementSimulationChanged(enabled: Boolean)
        fun onMovementSpeedChanged(speed: Float)
        fun onZoomWithMarkerChanged(enabled: Boolean)
        fun onBearingRotationChanged(enabled: Boolean)
        fun onLanguageChanged(language: String)
    }
    
    private var settingsChangedListener: OnSettingsChangedListener? = null
    
    fun setOnSettingsChangedListener(listener: OnSettingsChangedListener) {
        this.settingsChangedListener = listener
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        setStyle(STYLE_NORMAL, R.style.CustomDialog)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        switchMovementSimulation = view.findViewById(R.id.switch_movement_simulation)
        seekbarMovementSpeed = view.findViewById(R.id.seekbar_movement_speed)
        tvSpeedValue = view.findViewById(R.id.tv_speed_value)
        switchZoomWithMarker = view.findViewById(R.id.switch_zoom_with_marker)
        switchBearingRotation = view.findViewById(R.id.switch_bearing_rotation)
        btnClose = view.findViewById(R.id.btn_close_settings)
        
        setupViews()
        loadSettings()
    }
    
    override fun onStart() {
        super.onStart()
        // Set dialog window dimensions
        dialog?.window?.let { window ->
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt() // 90% of screen width
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }
    
    private fun setupViews() {
        // Movement simulation switch
        switchMovementSimulation.setOnCheckedChangeListener { _, isChecked ->
            saveMovementSimulationSetting(isChecked)
            settingsChangedListener?.onMovementSimulationChanged(isChecked)
        }
        
        // Movement speed seekbar
        seekbarMovementSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = (progress + 1) / 10.0f // Convert to 0.1x to 2.1x
                tvSpeedValue.text = "${String.format("%.1f", speed)}x"
                if (fromUser) {
                    saveMovementSpeedSetting(speed)
                    settingsChangedListener?.onMovementSpeedChanged(speed)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Zoom with marker switch
        switchZoomWithMarker.setOnCheckedChangeListener { _, isChecked ->
            saveZoomWithMarkerSetting(isChecked)
            settingsChangedListener?.onZoomWithMarkerChanged(isChecked)
        }
        
        // Bearing rotation switch
        switchBearingRotation.setOnCheckedChangeListener { _, isChecked ->
            saveBearingRotationSetting(isChecked)
            settingsChangedListener?.onBearingRotationChanged(isChecked)
        }
        
        // Language toggle button - disabled
        
        // Close button
        btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun loadSettings() {
        // Load movement simulation setting
        val movementSimulationEnabled = sharedPreferences.getBoolean(KEY_MOVEMENT_SIMULATION, false)
        switchMovementSimulation.isChecked = movementSimulationEnabled
        
        // Load movement speed setting
        val movementSpeed = sharedPreferences.getFloat(KEY_MOVEMENT_SPEED, 1.0f)
        val progress = ((movementSpeed * 10) - 1).toInt().coerceIn(0, 20)
        seekbarMovementSpeed.progress = progress
        tvSpeedValue.text = "${String.format("%.1f", movementSpeed)}x"
        
        // Load zoom with marker setting
        val zoomWithMarkerEnabled = sharedPreferences.getBoolean(KEY_ZOOM_WITH_MARKER, false) // Default false
        switchZoomWithMarker.isChecked = zoomWithMarkerEnabled
        
        // Load bearing rotation setting
        val bearingRotationEnabled = sharedPreferences.getBoolean(KEY_BEARING_ROTATION, true) // Default true
        switchBearingRotation.isChecked = bearingRotationEnabled
        
        // Language setting - disabled
    }
    
    private fun saveMovementSimulationSetting(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_MOVEMENT_SIMULATION, enabled)
            .apply()
    }
    
    private fun saveMovementSpeedSetting(speed: Float) {
        sharedPreferences.edit()
            .putFloat(KEY_MOVEMENT_SPEED, speed)
            .apply()
    }
    
    private fun saveZoomWithMarkerSetting(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ZOOM_WITH_MARKER, enabled)
            .apply()
    }
    
    private fun saveBearingRotationSetting(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BEARING_ROTATION, enabled)
            .apply()
    }
    
    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // Clean up any resources if needed
    }
}
