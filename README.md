# 📡 Deadzone Messenger

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Vite](https://img.shields.io/badge/Landing%20Page-Vite%20%2B%20React-646CFF.svg)](https://vitejs.dev/)

> **Stay Connected When Networks Die.**  
> A fully decentralized, off-grid communication platform that turns everyday smartphones into an encrypted, self-healing peer-to-peer (P2P) mesh network. **Zero cell towers. Zero Wi-Fi routers. 100% free and private.**

---

## 🌟 Overview & Unique Value Proposition (UVP)

Modern smartphones are tethered to fragile centralized telecom infrastructure. When network towers fail—due to remote wilderness, thick concrete walls in campus basements, or congested crowds at stadiums—communication halts immediately.

**Deadzone Messenger** eliminates this vulnerability. By harnessing the **Bluetooth Low Energy (BLE)** and **Wi-Fi Direct** hardware already inside Android devices, it dynamically discovers nearby peers and chains them together into a resilient, multi-hop mesh network. Messages, voice notes, and high-resolution photos seamlessly hop from phone to phone until they reach their intended recipient—without ever touching the Internet.

---

## 🚀 Key Features

* **🔀 Self-Healing P2P Mesh Routing:** Dynamic multi-hop packet routing. If an intermediate phone moves away or goes offline, the network instantly self-heals to discover an alternative route.
* **🔐 End-to-End Cryptography (AES/GCM):** Every transferred packet is cryptographically encrypted. Intermediate relay nodes can never read, modify, or tamper with the payloads.
* **📸 Rich Offline Media:** Full-resolution image transfer and crystal-clear voice notes over high-bandwidth peer-to-peer connections.
* **💾 Local-First Data Sovereignty:** Powered by Android's **Room ORM** and **Scoped Storage** APIs. Zero server logs, zero cloud tracking, and complete user ownership of their chat history.
* **🗑️ Granular Message Control:** Offline permanent message saving, plus instant `Delete for Me` and `Delete for Everyone` synchronization.

---

## 🎯 Target Use Cases

| Scenario | The Challenge | Deadzone Messenger Solution |
| :--- | :--- | :--- |
| **🏔️ Hikers & Trekkers** | Zero cellular coverage in remote trails & valleys. | Free local P2P mesh keeping groups connected across miles. |
| **🎓 Universities & Campus** | Basements, labs, and thick concrete walls block signals. | Mesh packets hop through devices to bypass signal-blocking walls. |
| **🏟️ Stadiums & Festivals** | 50,000+ people overload local cell towers (congestion). | Local communications bypass cellular infrastructure entirely. |
| **🛡️ Privacy Advocates** | ISP tracking, data brokers, and government metadata collection. | 100% off-grid, encrypted, decentralized, and serverless. |

---

## 🛠️ Technology Stack

### Android Client
* **Language & Architecture:** Kotlin, Clean Architecture, MVVM + MVI
* **UI Framework:** Jetpack Compose (Modern declarative UI)
* **P2P Networking:** Google Nearby Connections API (BLE + Wi-Fi Direct)
* **Local Persistence:** Room Database, AndroidX DataStore, Scoped Storage
* **Security & Cryptography:** Android Keystore, AES-256 GCM

### Interactive Landing Page & Simulator
* **Framework:** React 19 + Vite
* **Animations:** Framer Motion (interactive multi-device simulations)
* **Icons:** Lucide React
* **Bundler:** Vite SingleFile (self-contained, ultra-portable distribution)
* **Deployment:** Automated GitHub Actions workflow to GitHub Pages

---

## 💻 Running the Landing Page Locally

```bash
# 1. Clone the repository
git clone https://github.com/Subhash-Chowdary-Chandra/Deadzone-Messenger.git
cd Deadzone-Messenger

# 2. Install dependencies
npm install

# 3. Start local development server
npm run dev

# 4. Build single-file production release
npm run build
```

---

## 🌐 Deploying to GitHub Pages (Automatic)

This repository includes a pre-configured GitHub Actions workflow (`.github/workflows/deploy.yml`):

1. Go to your repository on GitHub: **Settings** → **Pages**.
2. Under **Build and deployment** → **Source**, select **GitHub Actions**.
3. Every push to the `main` branch will automatically build and publish your landing page at:
   `https://<your-username>.github.io/<your-repo-name>/`

---

## 👤 Venture & Leadership

### **Synapse Logic** — *Connecting Intelligence*
A frontier technology venture engineering resilient decentralized systems, edge intelligence, and sovereign peer-to-peer communication platforms.

**Subhash Chowdary Chandra**  
* Founder & Lead Architect, **Synapse Logic**
* **LinkedIn:** [Subhash Chowdary Chandra](https://www.linkedin.com/in/subhash-chowdary-chandra)
* **Contact:** [Click to Email Founder](mailto:subhash.25bca7607@vitapstudent.ac.in)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
