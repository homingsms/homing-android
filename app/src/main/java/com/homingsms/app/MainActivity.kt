package com.homingsms.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.homingsms.app.databinding.ActivityMainBinding
import com.homingsms.app.helpers.LocaleHelper
import com.homingsms.app.modules.cloud.CloudController
import com.homingsms.app.ui.HolderFragment
import com.homingsms.app.ui.HomeFragment
import com.homingsms.app.ui.SettingsFragment
import com.homingsms.core.model.CloudPairingState
import com.homingsms.core.model.PairingLink
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val cloud: CloudController by inject()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        binding.viewPager.adapter = FragmentsAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                TAB_INDEX_HOME -> tab.apply {
                    text = getString(R.string.tab_text_home)
                    setIcon(R.drawable.ic_home)
                }

                TAB_INDEX_MESSAGES -> tab.apply {
                    text = getString(R.string.tab_text_messages)
                    setIcon(R.drawable.ic_sms)
                }

                else -> tab.apply {
                    text = getString(R.string.tab_text_settings)
                    setIcon(R.drawable.ic_advanced)
                }
            }
        }.attach()

        processIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
            )
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun processIntent(intent: Intent) {
        val data: Uri? = intent.data
        if (intent.action == Intent.ACTION_VIEW && data?.scheme == PairingLink.SCHEME) {
            confirmPairing(data.toString())
            return
        }
        binding.viewPager.currentItem = intent.getIntExtra(EXTRA_TAB_INDEX, TAB_INDEX_HOME)
    }

    private fun confirmPairing(link: String) {
        val parsed = PairingLink.parse(link, cloud.allowCleartext)
        if (parsed == null) {
            Toast.makeText(this, getString(R.string.pairing_failed, link), Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.pair_device)
            .setMessage(getString(R.string.confirm_pair_link, parsed.serverUrl.toUri().host ?: parsed.serverUrl))
            .setPositiveButton(R.string.confirm) { _, _ ->
                lifecycleScope.launch {
                    val result = cloud.pairWithLink(link)
                    if (result is CloudPairingState.Failed) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.pairing_failed, result.message ?: result.reason.name),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    class FragmentsAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = TAB_COUNT

        override fun createFragment(position: Int): Fragment = when (position) {
            TAB_INDEX_HOME -> HomeFragment.newInstance()
            TAB_INDEX_MESSAGES -> HolderFragment.newInstance()
            else -> SettingsFragment.newInstance()
        }
    }

    companion object {
        const val TAB_INDEX_HOME = 0
        const val TAB_INDEX_MESSAGES = 1
        const val TAB_INDEX_SETTINGS = 2
        private const val TAB_COUNT = 3

        private const val EXTRA_TAB_INDEX = "tabIndex"

        fun starter(context: Context, tabIndex: Int): Intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_TAB_INDEX, tabIndex)
        }
    }
}
