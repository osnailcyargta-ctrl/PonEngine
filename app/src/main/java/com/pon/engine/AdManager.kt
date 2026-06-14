package com.pon.engine

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class AdManager(private val context: Context) {

    private val API_KEY = "5802e3aa356488026782df315b04e04330be79ee6c0441ea04eab4fdd9c407bb"

    interface AdCallback {
        fun onRewarded()
        fun onFailed(reason: String)
    }

    fun showRewardedAd(callback: AdCallback) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val webView = WebView(context)
        webView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // JS bridge so ad can call back into Kotlin
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onReward(value: String, type: String) {
                dialog.dismiss()
                (context as? android.app.Activity)?.runOnUiThread {
                    callback.onRewarded()
                }
            }

            @JavascriptInterface
            fun onFail(reason: String) {
                dialog.dismiss()
                (context as? android.app.Activity)?.runOnUiThread {
                    callback.onFailed(reason)
                }
            }
        }, "PonBridge")

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        val html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { background:#0f0f1a; display:flex; flex-direction:column;
         align-items:center; justify-content:center; height:100vh;
         font-family:monospace; color:#fff; }
  .title { color:#ff6b9d; font-size:18px; margin-bottom:16px; }
  .sub { color:#888; font-size:13px; margin-bottom:24px; }
  #adContainer { width:100%; max-width:400px; }
</style>
</head>
<body>
<div class="title">🎯 Watch Ad to Unlock Slot</div>
<div class="sub">Please wait for the ad to finish...</div>
<div id="adContainer"></div>
<script src="http://cppotatoads.rocev.me/assets/js/potatoads.js"></script>
<script>
PotatoAds.show({
  apiKey: '$API_KEY',
  delay: 3000,
  onReward: function(r) {
    try { PonBridge.onReward(String(r.value), String(r.type)); } catch(e) {}
  },
  onFail: function(reason) {
    try { PonBridge.onFail(String(reason)); } catch(e) {}
  }
});
</script>
</body>
</html>
        """.trimIndent()

        webView.loadDataWithBaseURL("http://cppotatoads.rocev.me", html, "text/html", "UTF-8", null)

        dialog.setContentView(webView)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }
}
