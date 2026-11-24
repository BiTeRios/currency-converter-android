# Lab 4 Report — Mobile Currency Converter

## 1. Topic and rationale

Option B brings the Lab 3 currency converter to Android and adds the required weekly and monthly comparison screens. The metric on both screens is the exchange rate for the selected currency pair.

The National Bank of Moldova was already the Lab 3 data source. Its dated feed supplies historical data, so the app can draw a monthly chart on the first online run.

## 2. Language change

| Lab | Language | Stack |
| --- | --- | --- |
| 3 | Python 3 | Tkinter, `urllib`, `xml.etree`, `decimal` |
| 4 | Kotlin | Jetpack Compose, Material 3, coroutines, `HttpURLConnection`, DOM parsing, `BigDecimal` |

The Kotlin app was implemented independently using the Lab 3 specification. It retains the same amount validation messages, limit of 1,000,000,000,000, two decimal places for amounts, ten-day lookback, round-trip tolerance, and half-up rounding with four decimal places for converter rates.

The Android rate model and conversion code were committed before the first application code in the Python repository. The Lab 3 specification was already available.

| Lab and commit | Local time | Change |
| --- | --- | --- |
| Lab 4 `98eea15` | 18 Nov 2026, 23:31 | Kotlin rate models and BNM XML parser |
| Lab 4 `327200d` | 18 Nov 2026, 23:33 | Conversion, amount validation, and formatting |
| Lab 4 `9df311c` | 18 Nov 2026, 23:38 | Period statistics |
| Lab 3 `8695705` | 19 Nov 2026, 01:36 | First Python source for `bnm_source`, `converter`, and `gui` |

| Concern | Lab 3 Python desktop implementation | Lab 4 Kotlin implementation |
| --- | --- | --- |
| Cache | Latest rate table in JSON | Raw XML for each saved date |
| Source unavailable | The user chooses whether to use a saved rate | The app automatically displays available saved data and a notice |
| Conversion | Button and optional rate date | Changes recalculate immediately using today's rate |
| Concurrency | Tk event loop and a worker thread with a queue | Coroutines with up to four requests per history load |
| XML | `xml.etree` | DOM parser from `javax.xml` |

## 3. Historical data and offline behavior

The repository loads the dates of a seven-day or thirty-day period. It reads historical dates from the device when available, requests missing dates, and attempts a network refresh for the latest day. Each successful response is stored as an XML file for the corresponding snapshot date. [History and cache flow](README.md#history-and-cache)

- A first monthly load needs connectivity and can use up to 30 requests, with four running at once. All 30 days were visible in an emulator screenshot taken 15 seconds after opening the tab.
- When the app has only some of the dates, it shows how many days are available and the date of the latest data.
- Weekend and holiday dates may repeat the most recent working day's rate. Each returned calendar day has equal weight when the app calculates the average.
- The app uses today's date in `Europe/Chisinau`. The BNM can publish the following day's rate around 13:00, and the converter continues to show the rate for today.
- The feed publishes the official exchange rate. Commercial buy and sell rates are outside the scope of the app.

## 4. IDE, AI tools, and model

| Work | Tool |
| --- | --- |
| Generation of code, tests, README, and original report | Claude Code for Windows with **Claude Opus 5.5** |
| Independent review | Claude Code reviewer subagent with a separate context and read-only access to the code, plus test execution |
| Command-line build | Gradle 9.3.1, Android Gradle Plugin 9.1.1, Kotlin 2.3.21 |
| Runtime checks and screenshots | Pixel 8 Android emulator, API 37, controlled through `adb` |
| IDE and SDK | Android Studio 2026.2.1 supplied the SDK, emulator, and JDK. The original submission was built and checked through the command line. |

## 5. Generated code and corrections

The original implementation correctly parsed XML denominations, converted through MDL with `BigDecimal`, calculated the period statistics, drew a line chart with Canvas, and applied the three-screen navigation. An emulator check matched a manual calculation of 100 USD = 1,833.83 MDL at a rate of 18.3383.

| Finding | How it was found | Correction |
| --- | --- | --- |
| `compileSdk` was set to 36 while the Compose dependencies required 37 | `checkDebugAarMetadata` failed | Raised `compileSdk` to 37 |
| The Gradle launcher picked up Java 8 from `PATH` | Gradle reported that it required JVM 17 | Selected the Android Studio JDK and documented the setting |
| A statistics test expected `1.09900590` | The test failed for a correct result | Corrected the expected value to `1.09900045` |
| A complete cache hid the fact that the device was offline | Airplane-mode check and review | Attempt a fresh request for the latest day and display a notice when it fails |
| The offline message exposed a raw Java exception | Emulator screenshot | Map network errors to clear Russian messages |
| The displayed trend could say flat while showing a nonzero percentage for a small rate | Independent review | Derive the trend from the displayed percentage and use six places for small rates in analytics |
| An unexpected `RuntimeException` could interrupt loading | Independent review | Show a load error for unexpected exceptions |
| An index-based chart hid missing calendar days | Independent review | Place points by calendar date |
| Concurrent writes could share a temporary cache file, and one XML parser feature was unsupported on Android | Independent review | Use unique temporary cache files and disable expansion of entity references |
| The converter requested dates that were already saved during the lookback | Follow-up review | Prefer the cache for earlier dates and identify that history may contain both live and saved data |

The amount and pair reset if Android terminates the process. A converter left open across midnight updates its rate when the user presses refresh. Trend labels use the percentage rounded to two places, so a very small change can show a flat trend alongside a nonzero absolute difference.

## 6. Student's reflection

*Reserved for the student's personal reflection.*

Points the student can draw on include the extra Android build and emulator setup, the role of runtime checks in finding offline and trend issues, and how understanding coroutines, `StateFlow`, and Compose helps explain and verify generated Kotlin code.
