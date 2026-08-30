package com.crdy.powergyan.monetization

import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun TestNativeAdCard(modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = { context ->
        val adView = NativeAdView(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.TRANSPARENT)
        }
        val headline = TextView(context).apply { textSize = 16f; setTextColor(Color.WHITE) }
        val body = TextView(context).apply { textSize = 13f; setTextColor(Color.LTGRAY) }
        val action = Button(context).apply { text = "Learn more" }
        layout.addView(headline)
        layout.addView(body)
        layout.addView(action)
        adView.addView(layout)
        adView.headlineView = headline
        adView.bodyView = body
        adView.callToActionView = action
        AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { ad ->
                headline.text = ad.headline
                body.text = ad.body ?: ""
                action.text = ad.callToAction ?: "Learn more"
                adView.setNativeAd(ad)
            }
            .build()
            .loadAd(AdRequest.Builder().build())
        adView
    })
}
