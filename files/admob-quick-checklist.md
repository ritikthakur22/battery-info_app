# Battery Gyan — AdMob & Monetization Quick Checklist
**Print this out or bookmark it!**

---

## 🚀 TODAY (30 minutes) - Create AdMob Account

### Step 1: Create AdMob Account
- [ ] Go to https://admob.google.com/
- [ ] Sign in with your Google account (same one as Play Developer)
- [ ] Click "Get Started"
- [ ] Accept terms
- [ ] Create app: Battery Gyan
- [ ] Category: Tools / Utilities
- [ ] Click "Create"

✅ **Result**: AdMob account created!

---

### Step 2: Create Banner Ad Unit
- [ ] In AdMob → Apps → Battery Gyan → Ad Units
- [ ] Click "Create New Ad Unit"
- [ ] Name: `battery_gyan_banner_home`
- [ ] Format: **Banner** (Adaptive)
- [ ] Click "Create"
- [ ] **COPY & SAVE THIS ID**: `ca-app-pub-XXXXXXXXXX/YYYYYYYYYY`

✅ **Result**: Banner ad unit created!

---

### Step 3: Create Native Ad Unit
- [ ] In AdMob → Apps → Battery Gyan → Ad Units
- [ ] Click "Create New Ad Unit"
- [ ] Name: `battery_gyan_native_analytics`
- [ ] Format: **Native Advanced**
- [ ] Click "Create"
- [ ] **COPY & SAVE THIS ID**: `ca-app-pub-XXXXXXXXXX/ZZZZZZZZZZ`

✅ **Result**: Native ad unit created!

---

### Step 4: Get Your AdMob App ID
- [ ] In AdMob → Settings → App information
- [ ] **COPY & SAVE APP ID**: `ca-app-pub-xxxxxxxxxxxxxxxx`

✅ **Result**: You have all ad IDs! ✅

---

## 💰 TODAY (15 minutes) - Create Play Billing Product

### Step 5: Create In-App Purchase
- [ ] Go to Google Play Console
- [ ] Your App (Battery Gyan) → Monetization
- [ ] Products → In-app products → Create Product
- [ ] Product ID: `remove_ads_lifetime`
- [ ] Type: **One-time purchase**
- [ ] Name: "Remove Ads (Lifetime)"
- [ ] Description: "Purchase to remove all ads from Battery Gyan forever"
- [ ] Price: **$1.50 USD**
- [ ] Availability: All countries
- [ ] Status: **Active**
- [ ] Click "Save"

✅ **Result**: Product created!

---

### Step 6: Set Regional Pricing
- [ ] Play Console → Battery Gyan → Monetization → Pricing and distribution
- [ ] Set prices for:
  - [ ] **USD**: $1.50 (default)
  - [ ] **INR**: ₹100 (for India)
  - [ ] **EUR**: €1.50 (for Europe)
  - [ ] Others: Auto-convert

✅ **Result**: Pricing configured!

---

## 📝 WEEK 1-3 (Development) - Add Code

### Step 7: Add Dependencies
Edit `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
}
```
- [ ] Add Google Mobile Ads dependency
- [ ] Add Play Billing dependency
- [ ] Sync Gradle
- [ ] No errors? ✅

---

### Step 8: Update Manifest
Edit `app/src/main/AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application>
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-xxxxxxxxxxxxxxxx"/>
</application>
```
- [ ] Add permissions
- [ ] Add meta-data (use YOUR AdMob App ID)
- [ ] Manifest updated? ✅

---

### Step 9: Initialize Mobile Ads in MainActivity
Edit `app/src/main/java/.../MainActivity.kt`:
```kotlin
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        MobileAds.initialize(this) // Add this line
        
        setContent {
            BatteryGyanTheme {
                MainApp()
            }
        }
    }
}
```
- [ ] Add import
- [ ] Add `MobileAds.initialize(this)`
- [ ] Compiles? ✅

---

### Step 10: Add Banner Ad to Home Screen
Follow code example from **admob-setup-guide.md** → Section 5.4
- [ ] Add `BannerAdView` composable
- [ ] Show banner only if NOT premium
- [ ] Use **TEST ID**: `ca-app-pub-3940256099942544/6300978111`
- [ ] Compiles? ✅

---

### Step 11: Add Native Ad to Analytics Screen
Follow code example from **admob-setup-guide.md** → Section 5.5
- [ ] Add `NativeAdView` composable
- [ ] Show native ad only if NOT premium
- [ ] Use **TEST ID**: `ca-app-pub-3940256099942544/2247696110`
- [ ] Compiles? ✅

---

### Step 12: Add Billing Manager
Follow code example from **admob-setup-guide.md** → Section 5.6
- [ ] Create `BillingManager.kt`
- [ ] Implement `queryPurchases()`
- [ ] Implement `buyRemoveAds()`
- [ ] Add to MainActivity
- [ ] Compiles? ✅

---

### Step 13: Add Premium UI Button
Follow code example from **admob-setup-guide.md** → Section 5.7
- [ ] Add premium card to Customize screen
- [ ] Show only if NOT premium
- [ ] "Remove Ads Forever - $1.50" button
- [ ] Button calls `billingManager.buyRemoveAds()`
- [ ] Renders? ✅

---

### Step 14: Test Everything (Week 3)
- [ ] Open app on phone
- [ ] Banner ad appears on Home ✅
- [ ] Native ad appears on Analytics ✅
- [ ] Tap "Buy Now" button → Purchase flow opens
- [ ] (Don't actually buy with test IDs)
- [ ] Close purchase flow
- [ ] Ads still show (test IDs, not purchased)

✅ **All working!**

---

## 📦 WEEK 4 (Before Release) - Production IDs

### Step 15: Get Production Ad IDs
- [ ] Go to AdMob → Apps → Battery Gyan → Ad Units
- [ ] Click your **Banner Ad Unit**
- [ ] Copy **Production** Ad Unit ID (NOT test ID)
- [ ] Paste here: `BANNER_PROD_ID = ________________________`
- [ ] Click your **Native Ad Unit**
- [ ] Copy **Production** Ad Unit ID
- [ ] Paste here: `NATIVE_PROD_ID = ________________________`

---

### Step 16: Replace Test IDs with Production IDs
Edit `app/src/main/java/.../ui/screens/HomeScreen.kt`:
```kotlin
// BEFORE (Week 1-3, development)
BannerAdView(adUnitId = "ca-app-pub-3940256099942544/6300978111")

// AFTER (Week 4, before release)
BannerAdView(adUnitId = "ca-app-pub-YOUR_REAL_ID/YOUR_BANNER_ID")
```
- [ ] Update HomeScreen banner ID
- [ ] Update AnalyticsScreen native ID
- [ ] Compiles? ✅

⚠️ **WARNING**: Only update after internal testing works!

---

### Step 17: Test on Internal Track
- [ ] Build release APK
- [ ] Upload to Play Console → Internal testing track
- [ ] Install on test device
- [ ] Ads appear (with real ad impressions now!)
- [ ] Tap "Buy Now" → Real purchase dialog opens
- [ ] ❌ DO NOT ACTUALLY BUY (testing only)
- [ ] Close dialog
- [ ] Ads still show (not purchased)

✅ **Everything works!**

---

### Step 18: Release to Beta
- [ ] Upload same APK to Beta track
- [ ] Invite beta testers
- [ ] They can test ads + purchase
- [ ] Monitor impressions in AdMob (should see real ads now)
- [ ] No fraud clicks!

---

### Step 19: Go Live!
- [ ] Promote to Production
- [ ] App is live! 🎉
- [ ] Monitor AdSense account daily
- [ ] Watch for fraud (unusual click patterns)
- [ ] Celebrate first ad revenue! 🚀

---

## 📋 Save Your IDs Here

Print this section and fill it in:

```
========================================
YOUR ADMOB & BILLING IDS (SAVE THIS!)
========================================

AdMob App ID:
ca-app-pub-xxxxxxxxxxxxxxxx

DEVELOPMENT (Test IDs - Weeks 1-3):
Banner Test ID:  ca-app-pub-3940256099942544/6300978111
Native Test ID:  ca-app-pub-3940256099942544/2247696110

PRODUCTION (Your Real IDs - Week 4):
Banner Prod ID:  ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
Native Prod ID:  ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ

PLAY BILLING:
Product ID: remove_ads_lifetime
Price: $1.50 USD

Google Play Console:
App ID: com.crdy.batterygyan
Account: [Your email]
========================================
```

---

## 🚨 Critical Reminders

### ❌ DO NOT:
- [ ] Click your own ads in production (FRAUD!)
- [ ] Use production IDs during development
- [ ] Forget `MobileAds.initialize()`
- [ ] Skip the manifest meta-data
- [ ] Show ads to premium users
- [ ] Load same ad multiple times

### ✅ DO:
- [ ] Use test IDs first (weeks 1-3)
- [ ] Test everything on internal track before release
- [ ] Monitor AdSense daily
- [ ] Ask friends to test purchase
- [ ] Check for fraud clicks
- [ ] Celebrate when money comes! 🎉

---

## 🎯 Timeline at a Glance

```
TODAY (30 min)
├─ Create AdMob account
├─ Create 2 ad units
├─ Create billing product
└─ Save all IDs ✅

WEEK 1-3 (Development)
├─ Add dependencies
├─ Update manifest
├─ Initialize Mobile Ads
├─ Add banner to Home
├─ Add native to Analytics
├─ Add premium button
└─ Test with TEST IDs ✅

WEEK 4 (Release Prep)
├─ Get production IDs
├─ Replace test IDs
├─ Test on internal track
├─ Test on beta track
└─ Go live! ✅

AFTER RELEASE
└─ Monitor AdSense daily
```

---

## 📞 Quick Answers

**Q: When do I use test IDs?**  
A: Weeks 1-3 (during development). Never in production!

**Q: When do I use production IDs?**  
A: Week 4, before releasing to beta/production on Play Store.

**Q: Will test ads show?**  
A: Yes, with "Google test ads" label. That's normal.

**Q: Can I click test ads?**  
A: Yes, test clicks don't count. But never click production ads!

**Q: How much money will I make?**  
A: Depends on users. Conservative estimate: $2-5/month for 1000 users.

**Q: How do users know they can buy?**  
A: Show premium button on Customize screen. "Remove Ads - $1.50"

**Q: What if someone buys but ads still show?**  
A: Check `isPremium` state. Might need to restart app.

**Q: How do I handle refunds?**  
A: Google Play handles it automatically. You don't need to do anything.

---

## ✅ Completion Checklist

- [ ] Completed TODAY section (create accounts)
- [ ] Completed WEEK 1-3 section (add code)
- [ ] Completed WEEK 4 section (production IDs)
- [ ] Saved all IDs in "Save Your IDs Here" section
- [ ] Read "Critical Reminders" section
- [ ] Ready to start development!

---

**You're ready! Start with TODAY section right now.** 🚀

Questions? Read **admob-setup-guide.md** for detailed explanations.

**Good luck! First ad payment is coming! 💰**
