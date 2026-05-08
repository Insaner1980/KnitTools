# KnitTools Ravelry Integration

Delta spec for adding Ravelry pattern search, OAuth authentication, and pattern-to-project linking.

---

## Overview

Ravelry integration as a new tool in the Tools tab. Users can search for knitting patterns via the Ravelry API, save patterns locally, and create KnitTools projects from pattern data. OAuth sign-in is optional — pattern search works without it, but sign-in unlocks access to the user's personal favorites, queue, and projects on Ravelry.

---

## Navigation

### New Routes

| Screen | Route | Tab |
|--------|-------|-----|
| Ravelry Search | `ravelry` | Tools |
| Ravelry Pattern Detail | `ravelry_detail/{patternId}` | Tools |
| Saved Patterns | `ravelry_saved` | Tools |

### Tools List Entry
New item in Tools List screen:
- **Title:** "Ravelry"
- **Subtitle:** "Search patterns and save favorites"
- **Accent color:** Muted teal `#5F8A8B` (or plum `#8B4A6B` — final decision pending)

---

## Authentication

### OAuth 1.0a Flow
Ravelry uses OAuth 1.0a. Implementation:

1. Register KnitTools as a Ravelry API app at `https://www.ravelry.com/pro/developer`.
2. OAuth flow uses **Custom Chrome Tab** (not WebView) — current Android best practice.
3. User taps "Sign in with Ravelry" → Custom Chrome Tab opens Ravelry login → user authorizes → redirect back to app via deep link / redirect URI.
4. App stores **access token + token secret** securely (EncryptedSharedPreferences or DataStore with encryption).
5. Refresh/re-auth only needed if user revokes access or signs out manually.
6. **One-time sign-in** — user does not need to sign in again after initial authorization.

### Sign-in UI
- **No blocking sign-in gate.** When user opens Ravelry tool for the first time, search screen opens directly.
- **Dismissible banner** at top of search screen: "Sign in to access your favorites and queue" with "Sign in" button.
- Once signed in, banner disappears permanently.
- If user taps a feature requiring auth (e.g. "Save to Favorites" on Ravelry), contextual prompt appears: "Sign in to save favorites."

### Settings Integration
- Settings screen shows: "Ravelry: Connected as [username]" + "Sign out" button (only when connected).
- Sign out clears stored tokens.

---

## Screens

### Ravelry Search Screen (`ravelry`)

**Top section:**
- Sign-in banner (if not authenticated, dismissible)
- Search text field (SearchTextField component)

**Filters** (collapsible section below search):
- Craft type: Knitting / Crochet
- Availability: Free / Paid / All
- Category: Hat, Sweater, Socks, Scarf, etc.
- Weight: Lace, Fingering, Sport, DK, Worsted, Aran, Bulky, etc.
- Difficulty: 1–5 range

**Results:**
- Scrollable list of pattern cards
- Each card: thumbnail image, pattern name, designer name, difficulty rating, free/paid badge
- Tap → navigates to Ravelry Pattern Detail

**Tabs** (if authenticated):
- Search (default)
- My Favorites
- My Queue
- Saved Patterns (local)

If not authenticated, only Search and Saved Patterns tabs are visible.

### Ravelry Pattern Detail Screen (`ravelry_detail/{patternId}`)

**Content:**
- Pattern photo (large, top)
- Pattern name (headline)
- Designer name
- Difficulty rating
- Gauge info (stitches × rows per unit)
- Needle size recommendation
- Yarn weight
- Yardage required
- Available sizes
- Pattern description / notes (truncated, expandable)

**Actions:**
- **"Start Project"** button (Primary, prominent) — creates a new KnitTools project pre-filled with:
  - Project name = pattern name
  - Linked pattern metadata (gauge, needle size stored in project)
  - Navigates to Counter screen for new project
- **"Save Pattern"** button (Secondary) — saves pattern data locally to Room
- **"Open in Ravelry"** button (text/link style) — opens pattern URL in browser

**If authenticated, additional actions:**
- "Add to Favorites" (syncs to Ravelry)
- "Add to Queue" (syncs to Ravelry)

### Saved Patterns Screen (`ravelry_saved`)
- List of locally saved patterns (stored in Room)
- Same card style as search results
- Tap → Pattern Detail
- Swipe-to-delete or long-press context menu to remove
- Empty state with appropriate illustration

---

## Data Model

### New Room Entity: `SavedPatternEntity`

| Field | Type | Description |
|-------|------|-------------|
| id | Long (PK) | Local ID |
| ravelryId | Int | Ravelry pattern ID |
| name | String | Pattern name |
| designerName | String | Designer |
| thumbnailUrl | String? | Photo URL (cached locally) |
| difficulty | Float? | Difficulty rating 1–5 |
| gaugeStitches | Float? | Stitches per gauge unit |
| gaugeRows | Float? | Rows per gauge unit |
| needleSize | String? | Recommended needle size |
| yarnWeight | String? | Yarn weight category |
| yardage | Int? | Required yardage |
| isFree | Boolean | Free or paid |
| patternUrl | String | Ravelry URL |
| savedAt | Long | Timestamp |

### New DAO: `SavedPatternDao`
- `getAll()`: Flow<List<SavedPatternEntity>>
- `getById(id)`: SavedPatternEntity?
- `getByRavelryId(ravelryId)`: SavedPatternEntity?
- `insert(pattern)`: Long
- `delete(pattern)`
- `deleteById(id)`

### Room Migration
- Migration N→N+1: Add `saved_patterns` table.

### CounterProjectEntity Extension
Add optional field:
- `linkedPatternId: Long?` — FK to SavedPatternEntity (nullable, no cascade — pattern can be deleted independently)

---

## Project Linking

Patterns can be linked to projects in two ways:

1. **From Pattern Detail:** "Start Project" creates new project with `linkedPatternId` set.
2. **From Counter Screen:** Project card could show "Link Pattern" option (future enhancement, not required for v1).

When a project has a linked pattern:
- Project card in Project List shows pattern name as subtitle
- Counter screen project card shows pattern name
- Pattern gauge/needle info available for reference within the project

---

## API Integration

### Ravelry API Endpoints Used

| Endpoint | Auth Required | Purpose |
|----------|--------------|---------|
| `GET /patterns/search.json` | App-level (basic) | Pattern search |
| `GET /patterns/{id}.json` | App-level (basic) | Pattern detail |
| `GET /current_user.json` | OAuth | Get authenticated user info |
| `GET /people/{username}/favorites/list.json` | OAuth | User's favorites |
| `GET /people/{username}/queue/list.json` | OAuth | User's queue |
| `POST /people/{username}/favorites/create.json` | OAuth | Add to favorites |
| `POST /people/{username}/queue/create.json` | OAuth | Add to queue |

### Networking
- Use Ktor or Retrofit for HTTP client.
- App-level basic auth for public endpoints (API key stored in BuildConfig, not in source).
- OAuth tokens stored in EncryptedSharedPreferences.
- **Offline behavior:** Search requires internet. Saved patterns available offline. Show appropriate empty state / error message when offline.

---

## Monetization
- Ravelry search: **Free** — accessible to all users.
- Save patterns locally: **Pro** — limited to Pro users (or limit free to 3 saved patterns).
- OAuth sign-in (favorites, queue): **Pro** feature.
- "Start Project" from pattern: Follows existing project limit (1 active for free, unlimited for Pro).

---

## Privacy
- No Ravelry data sent to Finnvek servers (there are none).
- OAuth tokens stored locally with encryption.
- Saved pattern data stored locally in Room.
- Ravelry API calls go directly from device to Ravelry servers.
- Data Safety: declare network access for Ravelry API. No data collected by developer.
