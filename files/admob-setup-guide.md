# Battery Gyan — AdMob & Play Billing Setup Guide
**For Complete Beginners (No AdMob Experience)**

---

## 📋 Your Monetization Model (Confirmed)

```
FREE APP (all devices)
├─ Shows: Banner ads (Home) + Native ads (Analytics)
├─ Can buy: $1.50 lifetime to remove ads
└─ All features: Available to all users

PREMIUM ($1.50 one-time purchase)
└─ No ads + lifetime ad-free
```

**Revenue Sources**:
1. **AdMob ads** (banner + native) → CPM from global advertisers
2. **Play Billing** ($1.50 purchase) → Your revenue directly

---

## 🚀 Step 1: Create Google AdMob Account (15 minutes)

### 1.1 Go to AdMob
```
https://admob.google.com/
```

### 1.2 Sign In with Your Google Account
- Use your personal Google account (Gmail)
- If you have multiple accounts, use the same one as your Play Developer account

### 1.3 Create AdMob Account
1. Click "Get Started"
2. Accept AdMob Terms & Conditions
3. Add your website or app info
4. Select "Android App"
5. App name: "Battery Gyan"
6. Category: Tools / Utilities
7. Click "Create"

✅ **Result**: You now have an AdMob account!

---

## 📱 Step 2: Create Ad Units in AdMob (10 minutes)

### What is an "Ad Unit"?
An **Ad Unit** = a container in your app where ads show. Each ad unit has a unique ID (like `ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy`).

You'll create **2 ad units**: Banner + Native

### 2.1 Create Banner Ad Unit

```
AdMob Dashboard → Apps → Battery Gyan → Ad Units → Create New Ad Unit
```

**Settings**:
- Name: `battery_gyan_banner_home`
- Ad Format: **Banner** (fixed size)
- Banner Size: **Adaptive banner** (recommended)
- Ad Type: Both (Text + Image ads)
- Click "Create"

✅ **You get**: Banner Ad Unit ID  
Example: `ca-app-pub-3940256099942544/6300978111`

⚠️ **Save this ID!** You'll need it in code.

### 2.2 Create Native Ad Unit

```
AdMob Dashboard → Apps → Battery Gyan → Ad Units → Create New Ad Unit
```

**Settings**:
- Name: `battery_gyan_native_analytics`
- Ad Format: **Native Advanced**
- Ad Type: Both (Text + Image ads)
- Click "Create"

✅ **You get**: Native Ad Unit ID  
Example: `ca-app-pub-3940256099942544/2247696110`

⚠️ **Save this ID!**

---

## 🧪 Step 3: Test Ad IDs (Development)

**IMPORTANT**: Do NOT use production ad IDs during development!

Google provides **test ad IDs** for development:

```
Test Banner Ad Unit ID:
ca-app-pub-3940256099942544/6300978111

Test Native Ad Unit ID:
ca-app-pub-3940256099942544/2247696110

Test App ID:
ca-app-pub-xxxxxxxxxxxxxxxx (your AdMob account ID)
```

**Use these test IDs in development** → Your app won't click-fraud on your own ads.

---

## 💰 Step 4: Setup Play Billing ($1.50 Purchase)

### 4.1 Go to Google Play Console

```
https://play.google.com/console/
```

### 4.2 Create Product (In-App Purchase)

```
Your App (Battery Gyan) → Monetization → Products → In-app products → Create Product
```

**Settings**:
- Product ID: `remove_ads_lifetime`
- Product type: **One-time purchase** (not subscription)
- Name: "Remove Ads (Lifetime)"
- Description: "Purchase to remove all ads from Battery Gyan forever"
- Price: $1.50 USD
- Availability: All countries/regions
- Status: Active
- Click "Save"

✅ **You now have**: In-app purchase product ID = `remove_ads_lifetime`

### 4.3 Configure Pricing (By Region)

```
Play Console → Battery Gyan → Monetization → Pricing and distribution
```

**Set price for major regions**:
- **USD**: $1.50 (default)
- **INR**: ₹100 (approximately equivalent)
- **EUR**: €1.50
- Other regions: Auto-convert

This ensures users pay fairly in their local currency.

---

## 🔧 Step 5: Code Implementation (Week 4)

### 5.1 Add Dependencies to `build.gradle.kts`

```kotlin
dependencies {
    // Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    
    // Google Play Billing Library
    implementation("com.android.billingclient:billing-ktx:7.1.1")
}
```

### 5.2 Initialize Mobile Ads SDK

```kotlin
// MainActivity.kt
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ads (BEFORE showing any ads)
        MobileAds.initialize(this)
        
        setContent {
            BatteryGyanTheme {
                MainApp()
            }
        }
    }
}
```

### 5.3 Update AndroidManifest.xml

```xml
<!-- AndroidManifest.xml -->
<manifest>
    <!-- Add these permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application>
        <!-- Add your AdMob App ID (get from AdMob dashboard) -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-xxxxxxxxxxxxxxxx"/>
    </application>
</manifest>
```

**How to find your AdMob App ID**:
```
AdMob Dashboard → Settings → App information
```

### 5.4 Banner Ad Implementation

```kotlin
// HomeScreen.kt (Compose)
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.admanager.GoogleAdView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.admanager.GoogleAdView as BannerAdView

@Composable
fun BannerAdView(adUnitId: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            com.google.android.gms.ads.AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId) // Test or production ID
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}

// Use in HomeScreen
@Composable
fun HomeScreen(viewModel: HomeViewModel, isPremium: Boolean) {
    Column {
        // ... battery content ...
        
        // Show ads only if NOT premium user
        if (!isPremium) {
            BannerAdView(
                adUnitId = "ca-app-pub-3940256099942544/6300978111", // Test ID for dev
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}
```

### 5.5 Native Ad Implementation

```kotlin
// AnalyticsScreen.kt
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.GoogleNativeAd
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun NativeAdView(adUnitId: String, modifier: Modifier = Modifier) {
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    
    LaunchedEffect(adUnitId) {
        val adLoader = GoogleNativeAd.Builder(
            LocalContext.current,
            adUnitId
        )
            .forNativeAd { ad ->
                nativeAd = ad
            }
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        
        adLoader.loadAd(AdRequest.Builder().build())
    }
    
    nativeAd?.let { ad ->
        AndroidView(
            factory = { context ->
                TemplateView(context).apply {
                    setNativeAd(ad)
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(8.dp)
        )
    }
}

// Use in Analytics
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel, isPremium: Boolean) {
    Column {
        // ... analytics content ...
        
        if (!isPremium) {
            NativeAdView(
                adUnitId = "ca-app-pub-3940256099942544/2247696110", // Test ID for dev
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}
```

### 5.6 Play Billing Implementation

```kotlin
// BillingManager.kt
import com.android.billingclient.api.*

class BillingManager(private val context: Context) {
    private lateinit var billingClient: BillingClient
    private val productId = "remove_ads_lifetime"
    
    var isPremium = mutableStateOf(false)
    
    fun setup() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        if (purchase.products.contains(productId)) {
                            // User purchased remove_ads
                            isPremium.value = true
                            savePremiumStatus(true)
                        }
                    }
                }
            }
            .enablePendingPurchases()
            .build()
        
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {}
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }
        })
    }
    
    fun queryPurchases() {
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                isPremium.value = purchases.any { it.products.contains(productId) }
            }
        }
    }
    
    fun buyRemoveAds() {
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                ))
                .build()
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: ""
                
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    ))
                    .build()
                
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }
    
    private fun savePremiumStatus(isPremium: Boolean) {
        // Save to DataStore
        // dataStore.savePremiumStatus(isPremium)
    }
}
```

### 5.7 Show Premium Button in UI

```kotlin
// CustomizeScreen.kt
@Composable
fun CustomizeScreen(viewModel: CustomizeViewModel, isPremium: Boolean, billingManager: BillingManager) {
    Column {
        // ... customize content ...
        
        if (!isPremium) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎁 Remove Ads Forever", style = MaterialTheme.typography.titleMedium)
                    Text("$1.50 lifetime — no more ads", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { billingManager.buyRemoveAds() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Buy Now")
                    }
                }
            }
        } else {
            Text("✨ Thanks for removing ads!", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

### 5.8 Initialize Billing Manager in MainActivity

```kotlin
// MainActivity.kt
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private lateinit var billingManager: BillingManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Mobile Ads
        MobileAds.initialize(this)
        
        // Initialize Billing
        billingManager = BillingManager(this)
        billingManager.setup()
        
        setContent {
            BatteryGyanTheme {
                MainApp(
                    isPremium = billingManager.isPremium.value,
                    billingManager = billingManager
                )
            }
        }
    }
}
```

---

## 🔄 Workflow: Development → Release

### Phase 1: Development (Week 1-3)

**Use TEST Ad IDs:**
```kotlin
// During development
val BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111" // TEST
val NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110" // TEST
```

✅ Benefits:
- No clicks count as fraud
- App won't be suspended for clicking own ads
- Fast testing

### Phase 2: Before Release (Week 4)

**Replace with PRODUCTION Ad IDs:**

Go to AdMob Dashboard:
```
Apps → Battery Gyan → Ad Units → [Your Banner Ad Unit]
```

Copy your **Production Banner Ad Unit ID**:
```
ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy (your real ID, NOT test)
```

Replace in code:
```kotlin
// After release
val BANNER_AD_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy" // YOUR REAL ID
val NATIVE_AD_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz" // YOUR REAL ID
```

⚠️ **CRITICAL**: 
- Never click your own ads in production (fraud)
- Only use production IDs after publishing
- Test ads → Production IDs = ONE-WAY transition

---

## 📊 What Happens at Each Stage

### Development (Test IDs)
```
User opens app → Sees test ads (Google branding)
                → Clicks do nothing (test safe)
                → Play Billing works
                → Premium = no ads (works)
```

### Internal Testing (Play Store Internal Track)
```
Your beta testers → See test ads
                  → Can test purchase
                  → Still safe (test IDs)
```

### Beta Release (Play Store Beta Track)
```
Beta users → See PRODUCTION ads (real money)
           → Real clicks = real revenue (careful!)
           → Monitor for fraud
```

### Production (Play Store Live)
```
All users → See production ads
          → Real ad revenue flows
          → Monitor daily
          → Watch for fraud clicks
```

---

## 💡 Revenue Expectations

### CPM (Cost Per Mille = per 1000 ads)
- **India**: $0.50-1.50 CPM (lower)
- **USA**: $5-15 CPM (higher)
- **Global average**: $2-3 CPM

**Example**:
```
1000 ad impressions @ $2 CPM = $2 revenue
100,000 impressions = $200
1,000,000 impressions = $2,000
```

### Play Billing Revenue
```
$1.50 purchase × 100 users = $150 (minus 30% Google cut)
                            = ~$105 your revenue
```

**Strategy**: Combine both:
- 90% from ads (volume)
- 10% from premium (high-value users)

---

## 📋 Checklist Before Release

### AdMob Setup ✅
- [ ] AdMob account created
- [ ] Banner ad unit created (get prod ID)
- [ ] Native ad unit created (get prod ID)
- [ ] AdMob App ID in manifest
- [ ] Test ads working (use test IDs)

### Play Billing Setup ✅
- [ ] `remove_ads_lifetime` product created
- [ ] Price set ($1.50 USD)
- [ ] Pricing for INR + other regions
- [ ] Play Billing library added
- [ ] Billing manager implemented
- [ ] Purchase tested (internal track)

### Code Readiness ✅
- [ ] Banner ads show in Home (if not premium)
- [ ] Native ads show in Analytics (if not premium)
- [ ] Premium button in Customize tab
- [ ] isPremium state persists after purchase
- [ ] Test IDs working correctly

### Release Prep ✅
- [ ] Replace test IDs with production IDs
- [ ] Test billing on beta track
- [ ] Monitor for clicks on own ads (NEVER do this)
- [ ] Publish to production
- [ ] Monitor AdSense account daily first week

---

## 🚨 Common Mistakes (AVOID!)

### ❌ Mistake 1: Clicking Your Own Ads
**NEVER** click your own ads in production!
- AdSense will **disable your account** permanently
- Loss of all revenue
- Might need to restart with new account

**Solution**: Use test IDs during dev, ask friends to test

---

### ❌ Mistake 2: Forgetting to Update Manifest
**Without this**: Ads won't load
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxxxxxxxxxx"/>
```

---

### ❌ Mistake 3: Not Initializing MobileAds
**Without this**: Ads crash
```kotlin
MobileAds.initialize(context) // Call FIRST in MainActivity
```

---

### ❌ Mistake 4: Showing Ads to Premium Users
**Mistake**:
```kotlin
// WRONG - always shows ads
BannerAdView(adUnitId)

// CORRECT - check premium first
if (!isPremium) {
    BannerAdView(adUnitId)
}
```

---

### ❌ Mistake 5: Loading Ads Too Often
**Mistake**:
```kotlin
// WRONG - loads new ad every render
@Composable
fun HomeScreen() {
    BannerAdView(adUnitId) // Creates new ad view every recomposition
}

// CORRECT - use AndroidView with remember
@Composable
fun HomeScreen() {
    AndroidView(
        factory = { context ->
            com.google.android.gms.ads.AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build()) // Load once
            }
        }
    )
}
```

---

## 📊 Monitor Your AdSense Account

### Daily Checks (First Month)
```
https://adsense.google.com/
```

**Monitor**:
- Impressions (ad views)
- Clicks (user taps)
- CTR (Click-Through Rate, should be 1-3%)
- Earnings (revenue)

**Red flags**:
- CTR > 5% → suspicious clicks
- Sudden drops → ads blocked or premium users surge
- Unusual patterns → click fraud?

---

## 🎯 Step-by-Step Timeline

### Week 1-3 (Development)
- ✅ Create AdMob account
- ✅ Create ad units
- ✅ Add test IDs to code
- ✅ Implement banner + native ads
- ✅ Implement Play Billing
- ✅ Test everything locally

### Week 4 (Release Prep)
- ✅ Replace test IDs with production IDs
- ✅ Release to internal testing
- ✅ Test purchase on beta track
- ✅ Monitor ad impressions
- ✅ Publish to production

### After Release
- ✅ Monitor AdSense daily
- ✅ Celebrate first ad revenue! 🎉
- ✅ Optimize ad placements (if CTR too low)
- ✅ Watch for fraud patterns
- ✅ Plan next features based on revenue

---

## 📞 Quick Reference: Your Ad IDs

**Bookmark this section and update after creating ad units:**

```
==== DEV (TEST IDS - Use Until Week 4) ====
Banner Test ID:     ca-app-pub-3940256099942544/6300978111
Native Test ID:     ca-app-pub-3940256099942544/2247696110
App ID (test):      ca-app-pub-3940256099942544

==== PRODUCTION (Your Real IDs - Use After Week 4) ====
Banner Prod ID:     [CREATE AND PASTE HERE]
Native Prod ID:     [CREATE AND PASTE HERE]
AdMob App ID:       [GET FROM ADMOB SETTINGS]

==== PLAY BILLING ====
Product ID:         remove_ads_lifetime
Price:              $1.50 USD
```

---

## 🚀 Next Action

### Today
1. Go to https://admob.google.com/
2. Sign in with your Google account
3. Create AdMob account
4. Create 2 ad units (Banner + Native)
5. **Save your production Ad Unit IDs** (you'll need these in Week 4)

### This Week
1. Add ad dependencies to Gradle
2. Implement banner ads in HomeScreen
3. Implement native ads in AnalyticsScreen
4. Test with test IDs
5. Add premium button to UI

### Week 4
1. Replace test IDs with production IDs
2. Release to beta track
3. Monitor first ads
4. Go live!

---

**Questions? Review this guide again or ask me for clarification!**

**You've got this! AdMob is simpler than it looks. 🚀**
