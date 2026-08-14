package ir.nemooneshop.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ir.nemooneshop.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        fileUploadCallback?.onReceiveValue(uris.toTypedArray())
        fileUploadCallback = null
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Camera permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }

        if (!granted) {
            Toast.makeText(
                this,
                "Location permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*
         * Android 15 / targetSdk 35 uses edge-to-edge.
         * Apply system bar insets so the website does not
         * appear underneath the status/navigation bars.
         */
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(binding.root)

        setupWebView()
        setupSwipeRefresh()

        binding.webView.loadUrl("https://nemooneshop.ir/")
    }

    private fun setupWebView() {

        binding.webView.apply {

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true

                setSupportZoom(true)
                builtInZoomControls = false

                loadWithOverviewMode = true
                useWideViewPort = true

                javaScriptCanOpenWindowsAutomatically = true
                mediaPlaybackRequiresUserGesture = false

                allowFileAccess = true
                allowContentAccess = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }

            webViewClient = object : WebViewClient() {

                override fun onPageStarted(
                    view: WebView?,
                    url: String?,
                    favicon: Bitmap?
                ) {
                    super.onPageStarted(view, url, favicon)

                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    super.onPageFinished(view, url)

                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)

                    if (request?.isForMainFrame == true) {
                        view?.loadData(
                            getOfflineHtml(),
                            "text/html",
                            "UTF-8"
                        )
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {

                override fun onProgressChanged(
                    view: WebView?,
                    newProgress: Int
                ) {
                    binding.progressBar.progress = newProgress
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {

                    fileUploadCallback = filePathCallback

                    fileChooserLauncher.launch("*/*")

                    return true
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {

                    if (hasLocationPermission()) {

                        callback?.invoke(
                            origin,
                            true,
                            false
                        )

                    } else {

                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                override fun onPermissionRequest(
                    request: PermissionRequest?
                ) {

                    request?.resources?.forEach { resource ->

                        when (resource) {

                            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {

                                if (hasCameraPermission()) {

                                    request.grant(
                                        request.resources
                                    )

                                } else {

                                    cameraPermissionLauncher.launch(
                                        Manifest.permission.CAMERA
                                    )
                                }
                            }

                            else -> {
                                request.grant(request.resources)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupSwipeRefresh() {

        binding.swipeRefresh.apply {

            setOnRefreshListener {
                binding.webView.reload()
            }

            setColorSchemeColors(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.primary
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCameraPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOfflineHtml(): String {

        val message =
            "فروشگاه اینترنتی نمونه | محصولات سالم، طبیعی و باکیفیت"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport"
                      content="width=device-width, initial-scale=1.0">

                <style>

                    body {
                        font-family:
                            -apple-system,
                            BlinkMacSystemFont,
                            "Segoe UI",
                            Roboto,
                            sans-serif;

                        display: flex;
                        justify-content: center;
                        align-items: center;

                        height: 100vh;
                        margin: 0;

                        background: #D90000;
                        color: #333;

                        text-align: center;
                        padding: 20px;
                    }

                    .container {
                        max-width: 400px;
                    }

                    h1 {
                        color: #F70101;
                        font-size: 24px;
                        margin-bottom: 16px;
                    }

                    p {
                        font-size: 16px;
                        line-height: 1.5;
                        color: #666;
                    }

                </style>

            </head>

            <body>

                <div class="container">

                    <h1>No Connection</h1>

                    <p>$message</p>

                </div>

            </body>
            </html>
        """.trimIndent()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        if (binding.webView.canGoBack()) {

            binding.webView.goBack()

        } else {

            super.onBackPressed()
        }
    }
}
