package com.crdy.powergyan.monetization

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun TestAdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                // Google's official test banner unit. Replace only with a real unit for release.
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                setAdSize(AdSize.BANNER)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
