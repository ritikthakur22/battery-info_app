# Google Mobile Ads references newer optional platform audio classes.
# They are absent on older supported devices and are guarded by the SDK.
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
