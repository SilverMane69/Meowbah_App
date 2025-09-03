package com.kawaii.meowbah.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.kawaii.meowbah.R
import com.kawaii.meowbah.ui.theme.AvailableTheme
import com.kawaii.meowbah.ui.theme.allThemes

class VideoWidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var themeRadioGroup: RadioGroup

    companion object {
        private const val WIDGET_PREFS_NAME = "MeowbahWidgetPrefs"
        private const val PREF_THEME_KEY_PREFIX = "widget_theme_"

        fun saveThemePref(context: Context, appWidgetId: Int, themeName: String) {
            val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit()
            prefs.putString("$PREF_THEME_KEY_PREFIX$appWidgetId", themeName)
            prefs.apply()
            Log.d("WidgetConfig", "Saved theme '$themeName' for widget ID $appWidgetId")
        }

        fun loadThemePref(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val themeName = prefs.getString("$PREF_THEME_KEY_PREFIX$appWidgetId", null)
            Log.d("WidgetConfig", "Loaded theme '$themeName' for widget ID $appWidgetId")
            return themeName ?: AvailableTheme.Pink.displayName // Default to Pink if not set
        }

        fun deleteThemePref(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit()
            prefs.remove("$PREF_THEME_KEY_PREFIX$appWidgetId")
            prefs.apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED) // Default result is CANCELED

        setContentView(R.layout.video_widget_configure_activity)

        themeRadioGroup = findViewById(R.id.theme_radio_group)
        val saveButton: Button = findViewById(R.id.save_widget_config_button)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e("WidgetConfig", "Invalid widget ID, finishing activity")
            finish()
            return
        }

        populateThemeRadioButtons()
        loadAndSetCurrentThemeSelection()

        saveButton.setOnClickListener {
            val selectedRadioButtonId = themeRadioGroup.checkedRadioButtonId
            if (selectedRadioButtonId != -1) {
                val selectedRadioButton: RadioButton = findViewById(selectedRadioButtonId)
                val selectedThemeName = selectedRadioButton.tag as String // We stored theme name in tag
                saveThemePref(this, appWidgetId, selectedThemeName)

                // It is the responsibility of the configuration activity to update the app widget
                val appWidgetManager = AppWidgetManager.getInstance(this)
                VideoWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(Activity.RESULT_OK, resultValue)
                Log.d("WidgetConfig", "Configuration complete for widget ID $appWidgetId")
            } else {
                Log.w("WidgetConfig", "No theme selected for widget ID $appWidgetId")
                // Optionally show a message to the user
            }
            finish()
        }
    }

    private fun populateThemeRadioButtons() {
        allThemes.forEachIndexed { index, theme ->
            val radioButton = RadioButton(this).apply {
                text = theme.displayName
                tag = theme.displayName // Store the theme's display name for easy retrieval
                id = index + 1 // Ensure unique ID for RadioButton
            }
            themeRadioGroup.addView(radioButton)
        }
    }

    private fun loadAndSetCurrentThemeSelection() {
        val currentThemeName = loadThemePref(this, appWidgetId)
        for (i in 0 until themeRadioGroup.childCount) {
            // Get child as RadioButton? and use safe call with let
            (themeRadioGroup.getChildAt(i) as? RadioButton)?.let { rb ->
                // Inside this 'let' block, 'rb' is non-null (smart-cast)
                if (rb.tag == currentThemeName) {
                    rb.isChecked = true
                    return // Exit the loop and function once the theme is found and set
                }
            }
        }
    }
}
