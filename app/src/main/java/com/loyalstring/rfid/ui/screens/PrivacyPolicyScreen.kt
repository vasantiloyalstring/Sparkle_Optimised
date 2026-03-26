package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.loyalstring.rfid.navigation.GradientTopBar

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    var hasError by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        Log.d("PRIVACY_POLICY", "PrivacyPolicyScreen opened")
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Privacy Policy",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                selectedCount = 0,
                titleTextSize = 20.sp
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.allowFileAccess = true
                        settings.mixedContentMode =
                            android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                Log.d("PRIVACY_POLICY", "Page started: $url")
                                hasError = false
                                errorText = "Loading..."
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                Log.d("PRIVACY_POLICY", "Page finished: $url")
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                Log.e("PRIVACY_POLICY", "WebView error: ${error?.description}")
                                hasError = true
                                errorText = "Failed to load Privacy Policy"
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: android.webkit.WebResourceResponse?
                            ) {
                                Log.e(
                                    "PRIVACY_POLICY",
                                    "HTTP error: ${errorResponse?.statusCode}"
                                )
                            }
                        }

                        loadUrl("https://rrgold.loyalstring.co.in/AndroidDoc/privacypolicy.html")
                    }
                }
            )

            if (hasError) {
                Text(
                    text = errorText,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}