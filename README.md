# ♟️ Java MultiChess

A minimalist, high-performance Java-based chess application featuring cloud-synchronized multiplayer and a custom AI engine.

## 🚀 Core Features
* **Online Multiplayer:** Real-time synchronization via **Firebase REST API** using session codes. No port forwarding required.
* **AI Engine:** Integrated local opponent powered by the **Minimax algorithm with Alpha-Beta pruning**.
* **Dynamic UI:** Automatic **180° board rotation** for the Black player and a real-time evaluation bar.
* **Full FIDE Rules:** Complete implementation of Castling, En Passant, Pawn Promotion, and Draw detection (3-fold repetition).

## 🛠️ Tech Stack
* **Language:** Java 25+
* **Cloud Infrastructure:** Firebase Realtime Database
* **GUI Framework:** Java Swing / AWT (Custom rendering)

## 🔧 Installation & Usage
1. Clone the repository.
2. Ensure the `art/` folder (containing assets) remains in the same directory as the executable.
3. Run `Main.java` or the compiled `.jar` artifact.
