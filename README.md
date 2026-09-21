# 🪰 Feed My Fly (Virtual Connectome Pet)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![FlyWire Dataset](https://img.shields.io/badge/Data-FlyWire%20Connectome-purple.svg)](https://flywire.ai)
[![License](https://img.shields.io/badge/License-MIT%20%2F%20CC--BY--4.0-yellow.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-Passing%20(10%2F10)-success.svg)]()

> **A scientifically authentic virtual pet simulation driven by real biological connectome data.**  
> Powered by the landmark **FlyWire whole-brain dataset** (*Drosophila melanogaster*, Nature 2024), **Feed My Fly** couples a real 127-neuron anatomical neural circuit with interactive behavioral mechanics, procedural canvas rendering, real-time auditory synthesis, and a live neural graph visualizer.

---

## 🌟 Key Features

### 1. 🧬 Biologically Grounded Neural Engine
- **127 Unaltered Biological Neurons**: Direct mapping of real FlyWire connectome IDs and synaptic weights across sensory, interneuron, descending motor, and dopaminergic modulatory clusters.
- **Double-Buffered Leaky Integrator**: 
  $$\Delta x_j(t+1) = \text{clamp}\Big(\text{LEAK} \cdot x_j + (1 - \text{LEAK}) \cdot \text{clamp}\big(\text{GAIN} \sum_i w_{ij} x_i + I_j, 0, 1\big), 0, 1\Big)$$
  where normalized synaptic weight $w_{ij} = \frac{\text{sign} \cdot \text{count}}{\sum \text{excitatory counts}}$, $\text{LEAK} = 0.6$, and $\text{GAIN} = 0.95$.
- **Strictly Deterministic**: No pseudo-random number generators in the brain; identical sensory stimuli yield identical firing trajectories.

### 2. 🎮 Interactive Fly Behaviors & Game Loops
- 🍯 **Gustatory Feeding Circuit**: Stimulate 20 sugar-sensing neurons (`SENSORY_SUGAR`); watch descending proboscis motor neurons (`MN9`) fire ($\ge 0.5$) to trigger anatomical proboscis extension and sate hunger.
- 📱 **BugReels & Dopamine Addiction**: Swipe through an infinite feed of colorful bug reels; visual relay inputs stimulate PAM dopamine neurons, driving the fly into a mesmerizing **Dopamine Stare** with animated spiral eyes.
- ⚡ **Looming Threat & Escape Reflex**: Tap left or right to trigger directional threat visual sensors (`LPLC2` / `LC4`); Giant Fiber descending neurons (`DNp01`) activate an emergency escape leap synchronized with real-time procedural 220 Hz wingbeat audio.
- 🛁 **Johnston's Organ Bath Mode**: Rub the fly’s head to stimulate antennal mechanosensory neurons (`SENSORY_JON`), initiating rhythmic grooming kinematics with the front legs.
- 🔬 **Real-Time 4-Band Brain Visualizer**: Pan and zoom across 4 functional neural bands (`FEED`, `ESCAPE`, `REWARD`, `GROOM`) color-coded by sign and activation intensity. Tap any neuron for instant FlyWire lineage metadata.
- 🌍 **Bilingual Support (EN / IT)**: Full localization in English and Italian with in-app language switching.

---

## 🏗️ Architecture & Technology Stack

- **UI Framework**: Modern Jetpack Compose with Material Design 3 (M3).
- **Architecture**: Unidirectional Data Flow (UDF) / MVVM with Kotlin Coroutines & `StateFlow`.
- **Graphics**: Hardware-accelerated custom Compose `Canvas` rendering with vector trigonometry (jointed legs, chitin gradient body, translucent venated wings, compound eye ommatidia).
- **Audio Synthesis**: Low-latency procedural sine-wave frequency synthesis using Android `AudioTrack` (220 Hz Drosophila flight wingbeat tone).
- **Persistence & Catalog**: Local Room database & bundled `connectome.json` parsed via KotlinX Serialization.
- **Testing**: Robolectric JVM unit tests & Roborazzi screenshot verification.

---

## 🚀 Getting Started

### Direct APK Installation
You can download and install the pre-compiled APK directly:
1. In the **Google AI Studio** interface, click the **Install** button in the top-right corner of the preview pane (or navigate to **Settings ⚙️ > Download APK**).
2. Transfer `app-debug.apk` to your Android device (Android 8.0+ / API 26+).
3. Open the file and follow the on-screen prompt to install.

### Building from Source (Android Studio / Gradle)
```bash
# Clone the repository
git clone https://github.com/your-username/feed-my-fly.git
cd feed-my-fly

# Run unit tests (10 passing tests)
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Install directly to a connected device or emulator via ADB
./gradlew installDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔬 Scientific Citations

The connectome wiring, cell-type classifications, and neurotransmitter predictions are derived from:

1. **Dorkenwald, S., et al.** (2024). *Neuronal wiring diagram of an adult brain.* **Nature**, 634, 124–138. [doi:10.1038/s41586-024-07558-y](https://doi.org/10.1038/s41586-024-07558-y)
2. **Shiu, P. K., et al.** (2024). *A Drosophila computational brain model reveals how neural circuits generate behavior.* **Nature**, 634, 210–219. [doi:10.1038/s41586-024-07763-9](https://doi.org/10.1038/s41586-024-07763-9)
3. **Schlegel, P., et al.** (2024). *Whole-brain annotation and multi-connectome marker catalog.* **Nature**, 634, 139–152. [doi:10.1038/s41586-024-07686-5](https://doi.org/10.1038/s41586-024-07686-5)

Data provided by the **FlyWire Consortium** under [Creative Commons Attribution 4.0 International (CC-BY 4.0)](https://creativecommons.org/licenses/by/4.0/).

---

## 📄 License
This application code is open source under the [MIT License](LICENSE).
Connectome graph data and annotations are licensed under [CC-BY 4.0](https://creativecommons.org/licenses/by/4.0/).
