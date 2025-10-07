<p align="center">
  <img src= "https://github.com/ST10268917/WordleApp/blob/main/WordRush_Logo.png" width="220" alt="WordRush Logo"/>
</p>

#  WordRush

WordRush is a next-generation Android word challenge application that blends fun, learning, and competition.  
Built in **Kotlin**, powered by **Firebase**, and connected via a **Render-hosted REST API**, it brings daily puzzles, speed modes, and AI-driven multiplayer into one sleek experience.

---

## 📖 Table of Contents

1. [Introduction](#introduction)  
2. [Purpose](#purpose)  
3. [Objectives](#objectives)  
4. [Design Considerations](#design-considerations)  
5. [System Architecture](#system-architecture)  
6. [REST API Documentation](#rest-api-documentation)  
7. [Features](#features)  
8. [Screens](#screens)  
9. [Tech Stack](#tech-stack)  
10. [Functional Requirements](#functional-requirements)  
11. [Non-Functional Requirements](#non-functional-requirements)  
12. [GitHub and CI/CD](#github-and-cicd)  
13. [Demo Video](#demo-video)  
14. [Repository Links](#repository-links)  
15. [References](#references)  
16. [AI Usage Disclosure](#ai-usage-disclosure)

---

## 🟢 Introduction

In an era where word games like *Wordle* captivate millions, **WordRush** redefines the genre with modern mechanics, cloud connectivity, and visual flair.  
Developed in **Kotlin (Android Studio)**, it integrates a **Node.js + Express REST API** hosted on **Render**, backed by **Firebase Firestore** and **RapidAPI’s WordsAPI**.

This ensures:
- A global daily puzzle  
- Reliable validation and scalable sessions  
- Cloud-backed progress tracking and leaderboards  

<p align="center">
  <img src="https://media1.giphy.com/media/v1.Y2lkPWFkZWE2ZTUyOHlhOWRwd204N2poZ2NjMjhnNGwwbTZqbnJ6Mmt3aWg2OGNoY21oaSZlcD12MV9naWZzX3NlYXJjaCZjdD1n/e34lA8V77WQS0Ut1sG/giphy.gif" width="400"/>
</p>

---

## 🟠 Purpose

To create an interactive, educational, and competitive platform that strengthens vocabulary and mental agility through quick, rewarding gameplay.  
Unlike traditional word games, WordRush fuses **gamification**, **streak rewards**, and **timed challenges** to keep players engaged.

---

## 🎯 Objectives

- Develop a scalable, secure mobile application that enhances linguistic ability.  
- Integrate **Firebase Authentication** (Email + Google SSO).  
- Implement a **REST API layer** for core gameplay and word validation.  
- Introduce **multiple game modes** (Daily, Speedle, Multiplayer AI).  
- Automate builds and deployments using **GitHub Actions** and **Render**.  

---

## 💡 Design Considerations

### 🖋 User Experience (UX)
- **Dark/Light Themes:** Modern gradients reduce eye strain.  
- **Central Dashboard:** Card-based navigation for intuitive access.  
- **Instant Feedback:** Green = correct, Yellow = misplaced, Grey = incorrect.  
- **Progress Tracking:** Streaks, achievements, and badge animations.  
- **Accessibility:** Consistent layouts across all device sizes.

### 🎨 Visual Design
- **Color Palette:** Navy, Cyan, Neon Green, Red, and Gold.  
  - 🟦 Blue → Calm logic (Daily Mode)  
  - 🟩 Green → Action (Speedle Mode)  
  - 🟥 Red → Competition (Multiplayer)  
  - 🟨 Gold → Achievement (Streaks)  
- **Typography:** Rounded sans-serif for readability.  
- **Layout:** Rounded cards, shadows, and vibrant glow accents.

---

## ⚙️ System Architecture

WordRush follows a three-layer cloud architecture:

| Layer | Description |
|-------|--------------|
| **Frontend (Android)** | Kotlin + Material Components. Manages UI, logic, and Firebase login. |
| **Backend (Node.js + Express)** | Hosted on Render. Handles validation, word data, and gameplay APIs. |
| **Cloud (Firebase + WordsAPI)** | Firestore stores user data; WordsAPI provides definitions/synonyms. |

**Data Flow Overview**
1. User logs in via Firebase (Email or Google SSO).  
2. App requests a word or session from API.  
3. API validates via WordsAPI and Firestore.  
4. Data returned → stored and displayed dynamically.  

---

## 🔗 REST API Documentation

**Base URL:** `https://wordleappapi.onrender.com/api/v1`

| Endpoint | Method | Description |
|-----------|---------|-------------|
| `/word/today` | GET | Retrieves or seeds the daily 5-letter word |
| `/word/validate` | POST | Validates player’s guess |
| `/speedle/start` | POST | Starts timed Speedle mode |
| `/speedle/validate` | POST | Checks guesses & updates timer |
| `/speedle/finish` | POST | Ends session, saves score |
| `/speedle/leaderboard` | GET | Displays top players |

**Security:** Authenticated with Firebase ID Token.  

---

## 🧩 Features

### Core Gameplay
- **Daily Puzzle** shared globally  
- **Real-time feedback** with colored indicators  
- **Definition reveal** after each game  

### Highlighted Modes
- **Speedle:** Timed sprint (60/90/120 seconds)  
- **Multiplayer vs AI:** Compete against intelligent opponents (Easy–Hard)  
- **Hints & Power-ups:** Get clues at a cost of time  
- **Badges & Streaks:** Unlock glowing achievements  
- **Themes:** Switch between dark and light styles  

<p align="center">
  <img src="https://media0.giphy.com/media/v1.Y2lkPWFkZWE2ZTUyOHlhOWRwd204N2poZ2NjMjhnNGwwbTZqbnJ6Mmt3aWg2OGNoY21oaSZlcD12MV9naWZzX3NlYXJjaCZjdD1n/62HRHz7zZZYThhTwEI/giphy.gif" width="400"/>
</p>

---

## 📱 Screens

| Screen | Description |
|--------|--------------|
| **Welcome** | Eye-catching logo introduction |
| **Login/Register** | Email + Google SSO |
| **Dashboard** | Central navigation hub |
| **Speedle** | Timer-based challenge interface |
| **Multiplayer AI** | Play versus smart AI |
| **Badges/Profile** | View progress and stats |
| **Settings** | Toggle themes and haptics |

---

## 🧰 Tech Stack

| Layer | Technology |
|--------|-------------|
| **Frontend** | Kotlin, Android Jetpack, Material Components |
| **Backend** | Node.js, Express, CORS, Axios |
| **Hosting** | Render |
| **Database** | Firebase Firestore |
| **Authentication** | Firebase Auth (Email & Google) |
| **External API** | RapidAPI WordsAPI |
| **CI/CD** | GitHub Actions |

---

## ⚡ Functional Requirements

- Daily WordRush (shared puzzle)  
- Speedle Mode (timed)  
- Multiplayer vs AI (Easy–Hard)  
- Hints, Streaks & Badges  
- Profile & Settings  
- Firebase Authentication  

---

## 🛡 Non-Functional Requirements

- **Security:** Firebase ID tokens, Firestore rules, secrets via Render  
- **Performance:** Cached daily puzzles, timeouts, and error handling  
- **Accessibility:** Screen reader labels, high contrast themes  
- **Maintainability:** Modular helpers, semantic commits, CI checks  

---

## 🔁 GitHub and CI/CD

- **Frontend Repo:** [WordRush Android (Kotlin)](https://github.com/Atiyyahm/Wordleandroidclient.git)  
- **Backend Repo:** [WordRush API (Node.js)](https://github.com/ST10268917/WordleApp.git)

Both use **GitHub Actions** for build automation.  
Render auto-deploys backend changes on merge to `main`.

---

## 🎥 Demo Video

[![Watch the Demo](https://img.youtube.com/vi/sbE3L70ndtg/0.jpg)](https://youtu.be/sbE3L70ndtg)

---

## 🧭 Repository Links

- Frontend → [Atiyyahm/Wordleandroidclient](https://github.com/Atiyyahm/Wordleandroidclient.git)  
- Backend → [ST10268917/WordleApp](https://github.com/ST10268917/WordleApp.git)

---

<p align="center">
  <img src="https://media3.giphy.com/media/v1.Y2lkPWFkZWE2ZTUyOHlhOWRwd204N2poZ2NjMjhnNGwwbTZqbnJ6Mmt3aWg2OGNoY21oaSZlcD12MV9naWZzX3NlYXJjaCZjdD1n/NdieEAYwEZJot8ZA92/giphy.gif" width="380"/>
</p>

---

## 🧠 References

- Fowler, M. (2022). *Continuous Integration and Delivery: Modern DevOps Practices.* ThoughtWorks.  
- GitHub Docs. (2024). *Understanding GitHub Actions Workflows.*  
- GitHub Marketplace. (2024). *Automated Build Android App Workflow.*  
- Martin, R.C. (2023). *Clean DevOps: Building and Testing Automation in Practice.*  
- OWASP. (2023). *Secrets Management Cheat Sheet.*  
- Render. (2024). *Continuous Deployment and Environment Variables.*

---

## 🤖 AI Usage Disclosure

During development, **AI** was used responsibly to:
- Generate the **WordRush logo concept**  
- Assist with **debugging RetrofitClient and Google SSO issues**  

All implementation and creative decisions were completed manually.  
AI served only as a supportive tool for learning and troubleshooting.

<p align="center">
  <img src="https://media4.giphy.com/media/v1.Y2lkPWFkZWE2ZTUyOHlhOWRwd204N2poZ2NjMjhnNGwwbTZqbnJ6Mmt3aWg2OGNoY21oaSZlcD12MV9naWZzX3NlYXJjaCZjdD1n/NFgM484nJxhYpvDz9C/giphy.gif" width="420"/>
</p>

---
