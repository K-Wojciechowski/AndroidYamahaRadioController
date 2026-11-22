package pl.krzysztofwojciechowski.yamaharadio

import android.Manifest
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import pl.krzysztofwojciechowski.yamaharadio.model.RadioPreset


class MainActivity : AppCompatActivity() {
    private val viewModel: YamahaRadioViewModel by viewModels()
    private var pendingLocalNetworkAction: (() -> Unit)? = null
    private val localNetworkPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val action = pendingLocalNetworkAction
            pendingLocalNetworkAction = null
            if (granted) {
                (action ?: ::updateWifiState).invoke()
            } else {
                viewModel.setError(getString(R.string.local_network_permission_required))
            }
        }
    private var connManager: ConnectivityManager? = null
    private var wifiCallbackRegistered = false

    private val wifiNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateWifiState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            updateWifiState()
        }

        override fun onLost(network: Network) {
            updateWifiState()
        }

        override fun onUnavailable() {
            viewModel.setNoWifi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.myToolbar))
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBarContainer)) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBar.top, v.paddingRight, v.paddingBottom)
            insets
        }

        val adapter = RadioListAdapter()
        adapter.clickListener = { preset -> withLocalNetworkPermission { viewModel.setPreset(preset) } }

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            withLocalNetworkPermission { viewModel.refresh() }
        }
        findViewById<Button>(R.id.enableWifiButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(
            MaterialColors.getColor(swipeRefresh, com.google.android.material.R.attr.colorPrimaryInverse)
        )
        swipeRefresh.setOnRefreshListener { withLocalNetworkPermission { viewModel.refresh() } }
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.getContext(),
            layoutManager.getOrientation()
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        connManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
        val manager = connManager
        if (manager == null) {
            viewModel.setError("Unable to retrieve connectivity manager")
        } else {
            val wifiRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            manager.registerNetworkCallback(wifiRequest, wifiNetworkCallback)
            wifiCallbackRegistered = true
            withLocalNetworkPermission { updateWifiState() }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    invalidateOptionsMenu()
                    val noWifiLayout = findViewById<ConstraintLayout>(R.id.noWifiLayout)
                    val errorLayout = findViewById<ConstraintLayout>(R.id.errorLayout)
                    val errorText = findViewById<TextView>(R.id.errorText)
                    when (state) {
                        is RadioUiState.Error -> {
                            swipeRefresh.isRefreshing = false
                            adapter.update(emptyList())
                            if (state.detail == null) {
                                errorText.setText(R.string.error_text)
                            } else {
                                errorText.text = getString(R.string.error_text_detail, state.detail)
                            }

                            noWifiLayout.visibility = View.GONE
                            errorLayout.visibility = View.VISIBLE
                        }

                        is RadioUiState.Loading -> {
                            swipeRefresh.isRefreshing = true
                            adapter.update(emptyList())
                            noWifiLayout.visibility = View.GONE
                            errorLayout.visibility = View.GONE
                        }

                        is RadioUiState.NoWifi -> {
                            swipeRefresh.isRefreshing = false
                            adapter.update(emptyList())
                            noWifiLayout.visibility = View.VISIBLE
                            errorLayout.visibility = View.GONE
                        }

                        is RadioUiState.Success -> {
                            swipeRefresh.isRefreshing = false
                            noWifiLayout.visibility = View.GONE
                            errorLayout.visibility = View.GONE
                            adapter.update(state.shownPresets)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateWifiState()
    }

    private fun updateWifiState() {
        if (!hasLocalNetworkPermission()) return

        val manager = connManager
        val activeNetwork = manager?.activeNetwork
        val capabilities = activeNetwork?.let(manager::getNetworkCapabilities)
        val hasWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (hasWifi) {
            if (viewModel.uiState.value !is RadioUiState.Success) {
                viewModel.refresh()
            }
        } else {
            viewModel.setNoWifi()
        }
    }

    private fun hasLocalNetworkPermission(): Boolean =
        Build.VERSION.SDK_INT < 37 ||
            checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun withLocalNetworkPermission(action: () -> Unit) {
        if (hasLocalNetworkPermission()) {
            action()
        } else {
            pendingLocalNetworkAction = action
            localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }

    override fun onDestroy() {
        if (wifiCallbackRegistered) {
            connManager?.unregisterNetworkCallback(wifiNetworkCallback)
            wifiCallbackRegistered = false
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val viewModel: YamahaRadioViewModel by viewModels()
        menuInflater.inflate(R.menu.topbar, menu)

        val filterItem = menu?.findItem(R.id.menu_filter)
        filterItem?.setIcon(
            if (viewModel.uiState.value.filterEnabled) R.drawable.globe else R.drawable.globe_off
        )
        filterItem?.setTitle(
            if (viewModel.uiState.value.filterEnabled) R.string.menu_filter_disable else R.string.menu_filter_enable
        )

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val viewModel: YamahaRadioViewModel by viewModels()

        return when (item.itemId) {
            R.id.menu_filter -> {
                viewModel.toggleIsFiltered()
                true
            }

            R.id.menu_volume_down -> {
                withLocalNetworkPermission { viewModel.volumeDown() }
                true
            }

            R.id.menu_volume_up -> {
                withLocalNetworkPermission { viewModel.volumeUp() }
                true
            }

            R.id.menu_power -> {
                withLocalNetworkPermission { viewModel.togglePower() }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
