# 🤖💬 RealTalkAI

**RealTalkAI** is a voice-first AI chat application for Android that breaks away from traditional polite assistants. It delivers raw, real, and emotionally expressive conversations — like talking to an edgy, brutally honest best friend who actually keeps up with the world.



## ✨ Core Features

### 🎙️ Voice-Only Interface  
A sleek and immersive UI centered around voice interaction. Includes a fluid, animated orb that reacts to listening states for an engaging experience.

### 👤 Custom AI Persona  
This isn't your grandma's chatbot. The AI personality is sarcastic, emotional, unapologetically real, and uses modern slang — even cursing when the vibe demands it.

### 🧠 Advanced Voice Synthesis  
Powered by **Google Cloud Text-to-Speech** (TTS) with premium **Studio voices** and **SSML**. Produces human-like speech with natural pauses, hesitations, and tone.

### 🌍 Real-World Context Awareness  
- **Time & Date**: The AI knows when it is and refers to it naturally.  
- **Live News Integration**: Fetches real-time headlines from **NewsAPI.org** for current events awareness.

### 💬 Multi-Conversation Management  
- **Chat History Drawer**: Slide-out panel showing all previous conversations.  
- **Session Switching**: Start new chats, revisit old ones, or delete as needed.  
- **Local Persistence**: All data stored locally using **Room DB** — no internet needed to retrieve history.

---

## 🛠️ Tech Stack & Architecture

| Layer          | Tech Used                             |
|----------------|----------------------------------------|
| **UI**         | Jetpack Compose                        |
| **Architecture** | MVVM                                 |
| **Async Ops**  | Kotlin Coroutines                      |
| **Database**   | Room Persistence Library               |
| **Networking** | OkHttp                                 |
| **JSON Parsing** | Gson                                 |

### External Integrations
- **AI Chat**: [OpenRouter](https://openrouter.ai) (GPT-3.5-Turbo via API)
- **Speech Recognition**: Android’s native `SpeechRecognizer`
- **Text-to-Speech (TTS)**: Google Cloud TTS (Premium voices with SSML)
- **News**: [NewsAPI.org](https://newsapi.org)

---

## 🚀 Setup & Installation

### 1. Clone the Repository

```bash
git clone https://github.com/AR-47/RealTalkAI.git
```

### 2. Get Your API Keys

Create developer accounts and obtain keys for:

- **OpenRouter** → [openrouter.ai](https://openrouter.ai)
- **Google Cloud TTS** → [cloud.google.com/text-to-speech](https://cloud.google.com/text-to-speech)  
  *Ensure billing is enabled for Studio voices.*
- **NewsAPI** → [newsapi.org](https://newsapi.org)

### 3. Add Your Keys to the Project

> **🔒 Pro Tip:** You should eventually move these to `local.properties` for better security.

#### In `GPTHelper.kt`:
```kotlin
private const val OPENROUTER_API_KEY = "YOUR_OPENROUTER_KEY_HERE"
private const val NEWS_API_KEY = "YOUR_NEWS_API_KEY_HERE"
```

#### In `GoogleTTSHelper.kt`:
```kotlin
private const val apiKey = "YOUR_GOOGLE_TTS_KEY_HERE"
```

### 4. Build & Run

- Open the project in **Android Studio**
- Let Gradle sync
- Build and deploy to an emulator or physical Android device

---

## 🔮 Future Improvements

- **Auto-Listen Loop**: Restart listening immediately after AI finishes speaking (hands-free loop)
- **UI Enhancements**:
  - Add typing indicator
  - Animate chat bubble entry
- **AI-Generated Titles**: Summarize conversations into titles using a secondary GPT call
- **API Key Security**: Use `BuildConfig` with `local.properties` for environment-specific config

---

## 💬 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## ❤️ Contributions

Pull requests are welcome! Whether it's bug fixes, UI polish, or new features — feel free to open a PR or start a discussion.

---

## 📧 Contact

Created by [R J Adithya Yadav] — reach out via [adithyayadav641@gmail.com] or open an issue!
