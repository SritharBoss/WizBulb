# WizBulb 💡

WizBulb is a modern Android application built with Jetpack Compose designed to control WiZ smart lights over a local area network (LAN). It leverages UDP communication to discover bulbs and manage their state without requiring cloud connectivity.

## ✨ Features

- **Network Discovery**: Robust UDP broadcast mechanism that scans all available network interfaces to find active WiZ bulbs.
- **Real-time Status**: Fetches current bulb state including power status, brightness, RGB values, color temperature, and active scenes.
- **Full Control**:
    - Toggle Power (On/Off).
    - Manual RGB color selection.
    - Extensive Scene library (Ocean, Romance, Party, Fireplace, etc.).
- **Visual Feedback**:
    - **Independent Loading States**: Separated "Discovery" loading (for the Fetch button) from "Status" loading (using shimmer skeletons).
    - **Bulb Simulation**: A dynamic UI element that reflects the bulb's current color, brightness, and animated effects (e.g., pulsing for active states, hue cycling for Party mode).
- **Material 3 Design**: Clean, modern interface using the latest Android design standards.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Asynchrony**: Kotlin Coroutines & Flow
- **Networking**: Java DatagramSocket (UDP)
- **Animations**: Compose Animation API (InfiniteTransitions, Shimmer effects)

## 📡 Architecture & Logic

### `WizBulbController`
The core engine of the app. It handles all low-level UDP communication:
- **Port**: Uses port `38899` (standard for WiZ bulbs).
- **Discovery**: Sends `getPilot` UDP broadcasts to all local network interfaces and the universal broadcast address (`255.255.255.255`).
- **Commands**: Encapsulates JSON-based UDP payloads for `setPilot` and `getPilot` methods.

### State Management
The UI state is decoupled to ensure a smooth user experience:
- `isDiscovering`: Tracks the UDP broadcast task for the "Fetch" button.
- `isLoading`: Tracks specific bulb status updates to trigger shimmer skeleton screens.
- `bulbState`: A data class representing the physical state of the light.

## 🚀 Getting Started

### Permissions
The app requires the following permissions in `AndroidManifest.xml` for network operations:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
```

### Usage
1. Ensure your Android device and WiZ bulbs are on the same Wi-Fi network.
2. Open the app and tap **Fetch** to automatically discover bulbs.
3. Select an IP from the discovered list.
4. Use the control buttons to change colors, scenes, or power.

## 📂 Project Structure

```text
com.srithar.wizbulb
├── MainActivity.kt         # UI Layer (Compose Screens, Animations)
├── WizBulbController.kt    # Data/Network Layer (UDP Logic, Discovery)
└── WizState.kt             # Data Model
```

---
*Created By SritharBoss*