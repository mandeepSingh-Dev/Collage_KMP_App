# 🔥 Collage Studio - Firebase Integration & Console Guide

## 📋 Overview
This document contains complete documentation for all Firebase services integrated into **Collage Studio Multiplatform**:
1. **Firebase Cloud Messaging (FCM)** — Bulk Push Notifications (`all_users` topic & `CollageMessagingService`)
2. **Firebase Remote Config** — Mandatory Force Update management (`RemoteConfigManager`)
3. **Firebase Cloud Firestore** — User Device Details Tracking (`users_devices` collection & `DeviceTracker`)
4. **Firebase Analytics** — Custom Event Logging & Feature Usage Tracking (`FirebaseAnalyticsManager`)

---

## 🛠️ Integrated Firebase Components in Codebase

### 1. 🔔 Firebase Cloud Messaging (FCM Push Notifications)
- **Service File:** `androidApp/src/main/kotlin/com/oem/collagekmpapp/firebase/CollageMessagingService.kt`
- **Permission:** `android.permission.POST_NOTIFICATIONS` in `AndroidManifest.xml`
- **Behavior:** Automatically subscribes every device to the `"all_users"` FCM topic on launch. Receives FCM push messages and displays native Android notification banners.

### 2. 🚀 Firebase Remote Config (Force Update System)
- **Common Interface:** `shared/src/commonMain/kotlin/com/oem/collagekmpapp/firebase/RemoteConfigManager.kt`
- **Android Implementation:** `shared/src/androidMain/kotlin/com/oem/collagekmpapp/firebase/RemoteConfigManager.android.kt`
- **Keys Monitored:**
  - `min_required_version_code` (Long) — Default: `1`
  - `latest_version_name` (String) — Default: `"1.0"`
  - `force_update_message` (String) — Custom popup text
  - `update_url` (String) — Play Store link

### 3. 📱 User Device Tracking (Cloud Firestore)
- **Tracker File:** `shared/src/androidMain/kotlin/com/oem/collagekmpapp/firebase/DeviceTracker.android.kt`
- **Collection:** `users_devices`
- **Tracked Attributes:**
  - `device_model` & `device_manufacturer`
  - `android_version` & `sdk_int`
  - `app_version_name` & `app_version_code`
  - `screen_width_px`, `screen_height_px`, `screen_density_dpi`
  - `locale`
  - `fcm_token`
  - `last_updated_timestamp`

### 4. 📊 Firebase Custom Analytics Logging
- **Manager File:** `shared/src/commonMain/kotlin/com/oem/collagekmpapp/firebase/FirebaseAnalytics.kt`
- **Logged Events:**
  - `tool_used`: Screen name, tool name (Brightness, Lens Blur, Selective, Perspective, Filter, Adjust, Draw, etc.), extra info.
  - `template_selected`: Template ID & Template Name (Christmas, New Year, Birthday, Polaroid, etc.).
  - `export_completed`: Export Resolution (720p, 1080p, 4K) & Format (JPG, PNG, WebP).

---

## ⚙️ Step-by-Step Firebase Console Setup Guide

### Project Info
- **Firebase Project ID:** `collage-studio-afc33`
- **Android Package Name:** `com.oem.collagekmpapp`
- **Config File:** `androidApp/google-services.json`

---

### Step 1: Bulk Push Notifications Setup (FCM)
1. Open [Firebase Console](https://console.firebase.google.com/) -> Select project **`collage-studio-afc33`**.
2. Go to **Engage** -> **Messaging** (Cloud Messaging).
3. Click **Create your first campaign** -> Select **Firebase Notification messages**.
4. Fill in notification fields:
   - **Title:** e.g., `New Christmas & New Year Templates!`
   - **Text:** e.g., `Create stunning festive collages now.`
5. Click **Next** -> Under **Targeting**, select **Topic** -> Choose **`all_users`**.
6. Click **Review** -> **Publish**. Notification will be delivered to all app users simultaneously.

---

### Step 2: Remote Config Force-Update Setup
1. In Firebase Console, go to **Engage** -> **Remote Config**.
2. Click **Add parameter** and configure the following parameters:

| Parameter Key | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `min_required_version_code` | Number | `1` | Increase to `2` to force older builds to update |
| `latest_version_name` | String | `1.1` | New version display string |
| `force_update_message` | String | `A new update is available. Please update Collage Studio to continue.` | Popup message |
| `update_url` | String | `https://play.google.com/store/apps/details?id=com.oem.collagekmpapp` | Store link |

3. Click **Save** -> **Publish changes**.

---

### Step 3: Cloud Firestore Device Tracking Setup
1. In Firebase Console, go to **Build** -> **Firestore Database**.
2. Click **Create database** -> Choose **Production mode** (or Test mode) -> Select server region.
3. Click **Enable**.
4. Open the **`users_devices`** collection to inspect live device stats, FCM tokens, and hardware specs for all installed users.

---

### Step 4: Custom Analytics Reports Setup
1. In Firebase Console, go to **Analytics** -> **Events**.
2. View real-time event counts for:
   - `tool_used` (Breakdown by screen and tool name)
   - `template_selected` (Breakdown by template category)
   - `export_completed` (Breakdown by 720p / 1080p / 4K and JPG / PNG / WebP)
