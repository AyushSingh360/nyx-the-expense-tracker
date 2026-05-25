# 🌌 Nyx — Elegant Dark Obsidian Financial Ledger

Nyx is a highly sophisticated, offline-first personal ledger application for Android, built with **Jetpack Compose** and **Room Database**. Embracing a premium, low-contrast dark aesthetic inspired by obsidian stone, gold metallic accents, and fluid typography, Nyx is designed for high-precision financial logging and cognitive-load-free visual budgeting.

<iframe src="https://github.com/sponsors/AyushSingh360/button" title="Sponsor AyushSingh360" height="32" width="114" style="border: 0; border-radius: 6px;"></iframe>

```
       .---.            
      /     \           🌌 NYX LEDGER SYSTEM
      \.---./           Premium Offline Financial Core
      _||_||__          Obsidian & Gold Aesthetics
     [________]         
```

---

## 🎨 Visual Identity & Color Palette

The user interface of Nyx takes cues from modern high-end obsidian hardware and physical premium metal credit cards. Hardcoded color declarations are eschewed in favor of cohesive dark surface elevations paired with emerald growth indicators and high-luster gold warnings.

| Token | Value | Visual Purpose |
|:---|:---|:---|
| **DarkBackground** | `#050505` | Deepest canvas background for absolute contrast control |
| **DarkSurface** | `#0E0E0E` | Elevating card interfaces and secondary structural layers |
| **DarkSurfaceElevated** | `#161616` | Sheet alerts, prompt cards, and bottom modal wrappers |
| **GoldAccent** | `#E5C158` | Strategic highlights, warnings, targets, and active tags |
| **EmeraldAccent** | `#4EBA87` | Financial growth, positive cash flow, and healthy budgets |
| **OffWhitePrimary** | `#F5F5F7` | Display typography, core call-to-actions, and main FAB |

---

## 🏛️ Application & Database Architecture

Nyx adheres strictly to modern **MVVM (Model-View-ViewModel)** architecture principles. Data synchronization flows unidirectionally from the local SQLite engine back to the stateless UI rendering pipeline using asynchronous Kotlin Coroutines and Flows.

```
  ┌────────────────────────────────────────────────────────┐
  │                   Jetpack Compose UI                   │
  │   - Dynamic Canvas Charts  - Bottom Sheets Sheet Overlay│
  │   - Stateful TextFields    - Navigation Viewports       │
  └──────────────────────────┬─────────────────────────────┘
                             │
                  Exposes StateFlow Updates
                             │
  ┌──────────────────────────▼─────────────────────────────┐
  │                 ExpenseViewModel (MVVM)                │
  │   - Stateful Filters: Selected Categories, Sub-Types    │
  │   - live Aggregate Calculation Reducers                │
  └──────────────────────────┬─────────────────────────────┘
                             │
                     Queries / Dispatches
                             │
  ┌──────────────────────────▼─────────────────────────────┐
  │                     Room Database                      │
  │   - SQLite Local Off-disk Ledger Entities (Expense)     │
  │   - Reactive State Tracking & Flow Streaming           │
  └────────────────────────────────────────────────────────┘
```

### 🗄️ Database Schema & Data Models
The system operates securely on a single-table SQLite dynamic ledger managed by the `@Database` descriptor `ExpenseDatabase`.

#### Entity: `Expense`
- `id` (Long, Auto-increment, Primary Key)
- `title` (String, Non-Null transaction summary)
- `amount` (Double, Absolute value representation)
- `category` (String, Category mapping index e.g., "Salary", "Food", "Leisure")
- `type` (String, Income/Expense sub-type tracking e.g., `"INCOME"` or `"EXPENSE"`)
- `note` (String, Multi-line developer log or contextual note)
- `timestamp` (Long, Millisecond Epoch logging index)

---

## 📈 Visual Layouts & Dynamic Compose Canvas Charts

Nyx replaces standard bloated UI widgets with lightweight, highly fluid Canvas calculations engineered to redraw dynamically based on Room flows.

### ⭕ 1. Spending Distribution Ring Chart
The **Spending Distribution** is a dynamic Canvas ring layout. It computes raw percentages from total transaction amounts and maps individual categories to distinct custom-graded colors:

```
           .-""""-.         
         .'   __   '.       [■] Housing (% of Total)
        /   .'  '.   \      [■] Food & Cafes
       ;   ;      ;   ;     [■] Leisure & Hobbies
       |   | SPENT|   |     [■] Travel & Gadgets
       ;   ;      ;   ;     
        \   '.__.'   /      Calculated dynamically matching:
         '.        .'       SweepAngle = (CategorySum / TotalSpent) * 360f
           '-....-'         
```

### 📊 2. Quick Bonus Seeds & State Machine
Nyx contains built-in quick-action keys (located on the **Home** dashboard) to immediately trigger test flows:
- **`Add` (`＋`)**: Instantly pulls up the custom Obsidian modular bottom-sheet for custom transaction registrations.
- **`In-Bonus` (`↗`)**: Fires a `$950.0` premium client bonus directly into the Room database, instantly updating the global balance, net ledger, and income charts with smooth vector transitions.
- **`Insights` (`◕`)**: Toggles the visibility of the primary Canvas-based analytics charts instantly to focus on list ledger entries.
- **`Limits` (`⚙`)**: Pulls up live, inline educational guides directly beneath the App Bar.

---

## 📁 System Folder Structure

```
app/src/main/
├── AndroidManifest.xml                        # Hardware manifests and launcher configs
├── java/com/example/
│   ├── MainActivity.kt                        # Primary Activity, Dialog overlays, & Composables
│   ├── data/
│   │   ├── Expense.kt                         # Kotlin Data Entity Model
│   │   ├── ExpenseDao.kt                      # Complex Room SQL Dao query operations
│   │   └── ExpenseDatabase.kt                 # Thread-safe database builder & instances
│   └── ui/
│       ├── CategoryHelper.kt                  # Aesthetic Category color mappings and Vector icons
│       ├── ExpenseViewModel.kt                # MVVM state container & live flow calculators
│       └── theme/
│           ├── Color.kt                       # Obsidian theme Hex values
│           └── Theme.kt                       # Material 3 typography and dark schema overlays
└── res/
    ├── drawable/
    │   ├── ic_launcher_background.xml         # Obsidian-tinted vector background
    │   └── ic_launcher_foreground.xml         # Modernist minimal vector system logo (matching prompt)
    └── values/
        └── strings.xml                        # Localized App name ("Nyx") and accessibility values
```

---

## 💾 Project Compilation & Development Lifecycle

### Prerequisites
- Android Gradle Plugin: `8.5+`
- Kotlin Support: Embedded Compiler with standard Jetpack Compose Runtime
- Room Database Dependencies: `androidx.room:room-runtime`, `@ksp` Compiler Engine

### Verification Command Sequences
To verify compile builds and execute internal JUnit/Robolectric test suites cleanly:

```bash
# Verify absolute compile stability
compile_applet

# Execute intensive JUnit or Robolectric tests
gradle :app:testDebugUnitTest
```

---

*Designed and engineered with absolute visual discipline on the Google AI Studio platform.*
