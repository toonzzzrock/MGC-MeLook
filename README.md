# Mental Melook — App

Android keyboard IME + on-device dashboard. See the [umbrella README](../README.md)
for the full project layout.

## 4. What is actually recorded

Everything below comes straight from the code that writes to the local
Room database (`stats-core/.../HourlyStat.kt`, `StatsRepository.kt`). If a
metric isn't listed here, this app does not collect it.

### 4.1 Per-hour behavioral stats (`HourlyStat`, one row per hour)

| Field | Source | Meaning |
|---|---|---|
| `totalKeyPresses` | IME (`KeyboardStatsSink`) | key presses on the MGC keyboard that hour |
| `backspacePresses` | IME (`BackspaceTracker`) | backspace presses that hour |
| `wordsScored` / `sentimentSum` | IME (`SentimentScorer` + on-device `SentimentModel`) | count of words run through the on-device sentiment model, and the sum of their scores (0..1 each). `sentimentSum / wordsScored` = average sentiment for the hour. The model runs entirely on-device; no text is ever stored, only the numeric score. |
| `appSwitchCount` | `usage-monitor` (`ForegroundSwitchWatcher`, Android `UsageEvents`) | number of foreground-app switches |
| `screenTimeMillis` | `usage-monitor` (Android `UsageStatsManager`) | screen-on time that hour |
| `distinctAppCount` | `usage-monitor` (Android `UsageStatsManager`) | distinct app package count that hour |

Never recorded: raw keystrokes, typed text, message/call content, contact
names, or which specific apps were used (only counts).

### 4.2 Derived baseline (`BehavioralBaseline`, one row, recomputed periodically)

Rolling averages of the above (`avgBackspaceRate`, `avgSentiment`,
`avgAppSwitchesPerHour`, `avgDistinctAppsPerDay`, `avgScreenTimeMillisPerDay`,
`avgLongestInactiveStretchHours`) over `daysOfDataUsed` days. This is what
"usual"/"baseline" comparisons on the dashboard are measured against.

### 4.3 Storage, retention, and network

- Stored in a local Room/SQLite database (`mental_melook_stats.db`) inside
  the app's private storage. **Not currently encrypted at rest** — this is a
  known gap, not yet SQLCipher as originally planned.
- Retention: rows accumulate until `StatsRepository.clearAllData()` is
  called (Settings > reset). There is no automatic expiry yet.
- Network: the app declares the `INTERNET` permission because of the
  optional Clinical Bridge feature (`dashboard/.../bridge/`). With Clinical
  Bridge off (the default), no network call is made anywhere in the app. If
  a user turns it on in Settings, the metrics in §4.1/§4.2 (never raw text)
  are sent to the server URL that user configures — see the in-app
  "Data sharing" screen for the live, exact list.
- `AuditLogEntry` records app-level events (e.g. data reset, PIN changes)
  for the user's own reference; also local-only.

### 4.4 Demo data

`StatsRepository.seedDemoData()`, wired to a "Load demo data" button in
Settings, backfills synthetic stats so the dashboard can be previewed
before real usage accumulates. It is explicit and user-triggered — it never
runs automatically, and never mixes into a chart alongside real data.

### 4.5 Research references

Every chart on the "Everything we track" and "Trends" screens has an "ⓘ"
button (see `charts/ChartInfo.kt`, `ChartCitations`) that explains what the
metric means and cites the passive-sensing literature behind tracking it.
Full list:

- Saeb et al., "Mobile Phone Sensor Correlates of Depressive Symptom
  Severity in Daily-Life Behavior" (JMIR, 2015) — app-usage diversity,
  phone-usage duration, and typing/activity pace vs. PHQ-9 scores.
- Place et al., "Behavioral Indicators on a Mobile Sensing Platform Predict
  Clinically Validated Psychiatric Symptoms of Mood, Anxiety, and
  Psychosis" (JMIR, 2017) — screen-on time / rest-activity rhythm vs. mood
  and anxiety symptoms.
- Liu, Vesel, Rashidisabet, Zulueta, et al., "Digital phenotypes of mobile
  keyboard backspace rates and their associations with symptoms of mood
  disorder" (JMIR, vol. 26, e51269, 2024); Cao et al., "DeepMood: Modeling
  Mobile Phone Typing Dynamics for Mood Detection" (KDD, 2017) — backspace
  and keystroke-edit metrics vs. PHQ-8/PHQ-9 severity.
- Rozgonjuk et al., "Associations between symptoms of anxiety and
  depression, and smartphone app-switching behavior" (2021); Jacobson,
  Summers, and Wilhelm, "Automated screening for social anxiety,
  generalized anxiety, and depression from objective smartphone collected
  data: Cross sectional study" (JMIR, vol. 23, no. 8, e28918, 2021) —
  app-switching frequency vs. GAD-7/PHQ scores.
- Chikersal et al., "Differential temporal utility of passively sensed
  smartphone features for depression and anxiety symptom prediction: A
  longitudinal cohort study" (npj Mental Health Research, 2024) — passive
  smartphone features linked to both PHQ and GAD symptom trajectories
  over time.
- Eichstaedt et al., "Facebook language predicts depression in medical
  records" (PNAS, 2018) — the language/affect association the on-device
  sentiment score is inspired by.
- Ross et al., "Associations Between Smartphone Keystroke Metadata and
  Mental Health Symptoms in Adolescents: Findings From the Future
  Proofing Study" (JMIR Mental Health, 2023) — keystroke-timing patterns
  linked to depression, anxiety, distress, and insomnia symptoms.

Every reference above backs a specific chart's citation dialog — nothing
here is a general/foundational reference without a chart attached to it.
These are correlational findings from research literature, not diagnostic
thresholds — the app cites them to explain *why* a signal is tracked, never
to claim it can screen, diagnose, or replace PHQ-9/GAD-7/MoCA assessment.
