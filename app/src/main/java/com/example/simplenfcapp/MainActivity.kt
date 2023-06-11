package com.example.simplenfcapp

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.example.simplenfcapp.databinding.ActivityMainBinding
import com.example.simplenfcapp.model.MainViewModel
import com.github.skjolber.ndef.Message
import com.github.skjolber.ndef.Record
import com.github.skjolber.ndef.externaltype.AndroidApplicationRecord
import com.github.skjolber.ndef.wellknown.TextRecord
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONException
import org.json.JSONObject
import splitties.toast.toast

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val TAG = "MainActivity"

    private val nfcAdapter: NfcAdapter by lazy { NfcAdapter.getDefaultAdapter(applicationContext) }
    private val viewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (nfcAdapter == null) {
            toast(R.string.nfc_not_available)
            finish()
        }

        val intent = intent
        Log.i(TAG, "onCreate: ${intent.action}")

        processNfcIntent(intent)

    }

    //Auswertung der im Intent enthaltenen Informationen
    private fun processNfcIntent(intent: Intent) {
        if(!intent.hasExtra(NfcAdapter.EXTRA_TAG)) return

        // Message vom NFC TAG lesen
        val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if(messages == null || messages.size == 0) return
        try {
            val records : List<Record> = Message(messages[0] as NdefMessage)
            // Jeden Record der Message verarbeiten
            for(record in records) {
                when (record) {
                    // Wir reagieren nur auf TextRecords
                    is TextRecord -> parseJsonData(record.text)
                    // Ausgabe des AAR Records nur zu Info im Log
                    is AndroidApplicationRecord -> Log.i(TAG, "AAR is ${record.packageName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Problem parsing message : ${e.localizedMessage}")
        }
    }

    private fun parseJsonData(jsonString : String) {
        try {
            val obj = JSONObject(jsonString)
            val id = obj.getInt("ID")
            val command = obj.getString("command").toString()

            val data = "ID: $id , command: $command"

            viewModel.setTagData(data)
            Log.i(TAG,"Daten: $data")

            // Aktion hier nur Ausgabe als Toast

            toast("NFC Card: ID $id, Anweisung $command")
        } catch (e : JSONException) {
            e.printStackTrace()
            Log.e(TAG, getString(R.string.error_json_parsing))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i(TAG, "onNewIntent : ${intent.action}")

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            processNfcIntent(intent)
        }
    }


    override fun onResume() {
        super.onResume()
        if (!nfcAdapter.isEnabled) {
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            this?.let {
                MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.nfc_activate_title)
                    .setMessage(R.string.nfc_not_activated)
                    .setNeutralButton(R.string.dialog_cancel){dialog, which ->}
                    .setPositiveButton(R.string.activate){dialog, which -> startActivity(intent)}
                    .show()
            }
        }
        enableNfcForegroundDispatch()
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (ex: IllegalStateException) {
            Log.e(TAG, "Error enabling NFC foreground dispatch", ex)
        }
    }

    override fun onPause() {
        disableNfcForegroundDispatch()
        super.onPause()
    }

    private fun disableNfcForegroundDispatch() {
        try {
            nfcAdapter.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            Log.e(TAG, "Error disabling NFC foreground dispatch", ex)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}