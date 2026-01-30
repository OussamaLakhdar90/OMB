# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BNC CIAM Test Automation Framework - A Java 21 library for API and Web UI testing using TestNG, Selenium WebDriver, and RestAssured.

## Build Commands

```bash
# Compile the project
mvn clean compile

# Run unit tests
mvn test -Dgroups=unit

# Run tests with a specific suite file
mvn test -DsuiteXmlFile=suites/unit.xml

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Generate coverage report (after running tests)
mvn jacoco:report
# Reports output to target/site/jacoco/
```

## Architecture

### Class Hierarchy

```
AbstractDataDrivenTest          # Base: ThreadLocal context management
    ├── AbstractSeleniumTest    # Web UI: WebDriver lifecycle + testData field
    │       └── PageObject      # Page Object Model base with element interactions
    └── (API test classes)      # Extend directly for API testing
```

### ThreadLocal Context Pattern

The framework uses ThreadLocal storage for safe parallel test execution:
- `worldLocal` - Per-thread data sharing between test steps (use `WorldKey` enum)
- `testDataLocal` - Per-thread test data map
- `sharedContext` - Cross-thread shared state (ConcurrentHashMap)

```java
// Store/retrieve data between test steps
pushToTheWorld(WorldKey.AUTH_TOKEN, token);
String token = pullFromTheWorld(WorldKey.AUTH_TOKEN, String.class);
```

### Test Method Naming Convention

Methods must be named `t000`, `t001`, `t002`, etc. for lexicographic ordering by `TestngListener`.

### Dependency Annotation

```java
@DependentStep                      // Skip if ANY previous test in class failed
@DependentStep("t001_Login")        // Skip if specific method failed
public void t002_Dashboard() { }
```

### Test Data Handling

Test data loaded from JSON files. Sensitive values use `$sensitive:` prefix for environment variable substitution:

```json
{ "password": "$sensitive:DB_PASSWORD" }
```

Access via fluent API:
```java
testData.from("users.json").forIndex(1).getForKey("username");
testData.getForKey("email");  // Direct access from current data
```

### URL Resolution Priority (Web Tests)

1. System property `bnc.web.app.url` (pipeline)
2. Test data `_app_url` field
3. System property `web.url` or `webUrl` (local config)
4. `getBaseUrl()` override in subclass

### Key Modules

| Module | Purpose |
|--------|---------|
| `api/` | RestClient (fluent builder), ApiResponse wrapper |
| `web/page/` | PageObject base class, element locators prefer `data-testid` |
| `web/elements/` | Element wrappers: TextField, Button, CheckBox (null-safe chaining) |
| `web/builder/` | Browser-specific driver builders (Chrome, Firefox, Edge, Safari, IE) |
| `visual/` | Screenshot capture (AShot), image comparison (OpenCV), baseline management |
| `listener/` | TestngListener - ordering, dependency checking, logging |
| `xray/` | Jira/Xray integration for test reporting |
| `metrics/` | Test metrics collection and export (HTML, JSON, CSV, XML) |

### Annotations

| Annotation | Purpose |
|------------|---------|
| `@DependentStep` | Auto-skip if dependency failed |
| `@VisualCheckpoint` | Trigger visual validation |
| `@SkipVisualCheck` | Skip visual tests |
| `@Xray` | Link test to Jira/Xray ticket |

### Web Test Pattern

```java
public class LoginTest extends AbstractSeleniumTest {

    @Test
    public void t000_Start_Application() {
        runApplication();  // Inits driver, navigates to URL, refreshes testData
    }

    @Test
    @DependentStep
    public void t001_Login() {
        type("username", testData.getForKey("user"));
        type("password", testData.getForKey("pass"));
        click("submit-btn");
    }
}
```

### API Test Pattern

```java
public class ApiTest extends AbstractDataDrivenTest {

    @Test
    public void t001_CreateUser() {
        Response response = RestClient.builder()
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + token)
            .body(requestBody)
            .post("/users");

        pushToTheWorld(WorldKey.RAW_RESPONSE, response);
    }
}
```

## Configuration

### Basic Properties

- **Browser**: System property `browser` (CHROME, FIREFOX, EDGE, SAFARI, IE)
- **Execution mode**: System property `execution.mode` (LOCAL, SAUCELABS)
- **Headless**: System property `headless` (true/false)
- **Language**: System property `bnc.web.gui.lang` or testData `_lang` field

### Pipeline Execution (SauceLabs Tunnel)

Pipeline mode is enabled when `bnc.test.hub.use=true`. This connects to SauceLabs via a tunnel.

| Property | Description |
|----------|-------------|
| `bnc.test.hub.use` | Enable pipeline mode (true=SauceLabs, false=local agent) |
| `bnc.test.hub.url` | SauceLabs tunnel URL |
| `bnc.test.hub.name` | Tunnel name (tunnelIdentifier) |
| `bnc.test.hub.owner` | Tunnel owner (parentTunnel) for shared tunnels |
| `bnc.web.browsers.config` | Path to browser config JSON file |
| `bnc.web.app.url` | Application URL to test |
| `bnc.execution.environment` | Environment name (staging-ta, prod, etc.) |

**Browser Config File Structure (bnc.web.browsers.config):**

```json
{
  "browsers": [{
    "browserName": "chrome",
    "platformName": "WIN10",
    "browserVersion": "latest",
    "sauce:options": {
      "extendedDebugging": true,
      "screenResolution": "1920x1080",
      "parentTunnel": "TestAdmin",
      "tunnelIdentifier": "SauceConnect",
      "idleTimeout": 300
    },
    "goog:chromeOptions": {
      "args": [
        "--ignore-certificate-errors",
        "--disable-notifications",
        "--disable-popup-blocking"
      ]
    }
  }]
}
```

**How pipeline mode works:**
1. Framework checks `bnc.test.hub.use` property
2. If true, loads browser config from `bnc.web.browsers.config`
3. Extracts browserName, platformName, sauce:options, and goog:chromeOptions
4. Creates RemoteWebDriver connecting to `bnc.test.hub.url`
5. Tunnel configuration (tunnelIdentifier, parentTunnel) passed in sauce:options

**context.json integration:**

Pipeline properties come from context.json via TestNG parameters:
```json
{
  "staging-ta": {
    "chrome-en": {
      "bnc.web.app.url": "https://app.example.com",
      "bnc.test.hub.url": "https://ondemand.saucelabs.com/wd/hub",
      "bnc.test.hub.use": true,
      "bnc.web.browsers.config": "configuration/config_chrome_win10.json",
      "bnc.web.gui.lang": "en"
    }
  }
}
```

### Local Execution (debug_config.json)

For local development, Chrome options can be configured in `src/test/resources/debug_config.json`:

```json
{
  "data": "./data/staging-ta/data-manager.json",
  "web_url": "https://the-internet.herokuapp.com/login",
  "browser": "chrome",
  "lang": "en",
  "record": false,
  "cap": {
    "headless": false,
    "window_size": "1920x1080",
    "disable_notifications": true,
    "disable_popup_blocking": true,
    "ignore_certificate_errors": true,
    "disable_password_manager": true
  }
}
```

**Capability mapping:**
| debug_config.json | Chrome option |
|-------------------|---------------|
| `headless` | `--headless=new` |
| `disable_notifications` | `--disable-notifications` |
| `disable_popup_blocking` | `--disable-popup-blocking` |
| `ignore_certificate_errors` | `--ignore-certificate-errors` |
| `disable_password_manager` | Chrome prefs: `credentials_enable_service=false` |

### Local SauceLabs Execution (debug_config.json)

To run tests on SauceLabs from your local machine, add hub configuration to `debug_config.json`.
Property names match system property names exactly:

```json
{
  "data": "./data/staging-ta/data-manager.json",
  "web_url": "https://app.example.com",
  "browser": "chrome",
  "lang": "en",
  "bnc.test.hub.use": true,
  "bnc.test.hub.url": "https://ondemand.saucelabs.com/wd/hub",
  "bnc.web.browsers.config": "configuration/config_chrome_win10.json",
  "bnc.test.hub.tunnelIdentifier": "SauceConnect",
  "bnc.test.hub.parentTunnel": "TestAdmin"
}
```

**Hub Properties:**
| Property | Description |
|----------|-------------|
| `bnc.test.hub.use` | Enable SauceLabs mode (true/false) |
| `bnc.test.hub.url` | SauceLabs hub URL |
| `bnc.web.browsers.config` | Path to browser config file (overrides local `browser` setting) |
| `bnc.test.hub.tunnelIdentifier` | Tunnel identifier for SauceLabs |
| `bnc.test.hub.parentTunnel` | Parent tunnel owner for shared tunnels |

**How it works:**
1. When `bnc.test.hub.use=true`, the framework switches to SauceLabs execution
2. If `bnc.web.browsers.config` is set, browser configuration is loaded from that file
3. The browser config file overrides the local `browser` setting (e.g., you can have `browser=chrome` locally but run IE via SauceLabs config)
4. Tunnel settings are applied to `sauce:options` for the remote connection

**Example - Running IE on SauceLabs while local browser is Chrome:**
```json
{
  "browser": "chrome",
  "bnc.test.hub.use": true,
  "bnc.test.hub.url": "https://ondemand.saucelabs.com/wd/hub",
  "bnc.web.browsers.config": "configuration/config_ie_win10.json"
}
```
The `config_ie_win10.json` defines IE browser settings, which override the local `browser=chrome`.

### BrowserConfigLoader

The `BrowserConfigLoader` class handles loading browser configuration from JSON files:

```java
// Pipeline mode - loads from bnc.web.browsers.config
BrowserConfig config = BrowserConfigLoader.getInstance()
    .loadPipelineConfig("configuration/config_chrome_win10.json");

// Local mode - loads from debug_config.json
BrowserConfig config = BrowserConfigLoader.getInstance()
    .loadLocalConfig();

// Access loaded values
config.getBrowserName();      // "chrome"
config.getPlatformName();     // "WIN10"
config.getChromeArgs();       // ["--ignore-certificate-errors", ...]
config.getSauceOptions();     // {extendedDebugging: true, ...}
config.getTunnelIdentifier(); // "SauceConnect"
config.getParentTunnel();     // "TestAdmin"
```

## Test Suites

Suite files in `suites/` directory:
- `unit.xml` - Unit tests (parallel=classes, thread-count=4, groups=unit)

## Visual Testing

### VisualCapture Utility

Visual regression testing with dynamic scrolling support and browser-specific baselines:

```java
// In test method
boolean passed = VisualCapture.captureStep(driver, getClass().getSimpleName(), "step_name");
assertThat(passed).as("Visual validation failed").isTrue();
```

**Ignoring Dynamic Elements:**

For pages with dynamic content (timestamps, counters, live data), you can exclude specific elements from comparison. Both `WebElement` and `IElement` wrappers are supported:

```java
// Using WebElement directly
WebElement timestamp = driver.findElement(By.id("timestamp"));
boolean passed = VisualCapture.captureStepIgnoring(driver, getClass().getSimpleName(), "dashboard", timestamp);

// Using IElement wrappers (Element, TextField, Button, CheckBox, Image)
IElement clock = pageObject.getElement("clock");
IElement counter = pageObject.getElement("visitor-counter");
boolean passed = VisualCapture.captureStepIgnoring(driver, getClass().getSimpleName(), "dashboard", clock, counter);

// Ignore multiple WebElements (varargs)
WebElement clock = driver.findElement(By.id("clock"));
WebElement counter = driver.findElement(By.id("visitor-counter"));
WebElement livePrice = driver.findElement(By.className("live-price"));
boolean passed = VisualCapture.captureStepIgnoring(driver, getClass().getSimpleName(), "trading_page",
    clock, counter, livePrice);

// With custom tolerance (5% = 0.05)
boolean passed = VisualCapture.captureStepIgnoring(driver, getClass().getSimpleName(), "step_name",
    0.05, timestamp, counter);
```

**Alternative: Coordinate-based regions:**

If you don't have access to WebElements, you can specify regions as coordinate arrays `[x, y, width, height]`:

```java
List<int[]> ignoreRegions = Arrays.asList(
    new int[]{100, 50, 200, 30},   // Region at (100,50) with 200x30 size
    new int[]{500, 100, 150, 40}   // Another region to ignore
);
boolean passed = VisualCapture.captureStep(driver, getClass().getSimpleName(), "step_name",
    0.02, ignoreRegions);
```

**How ignore regions work:**
1. Element bounds are extracted using `element.getLocation()` and `element.getSize()`
2. Regions are painted over with a neutral color in both baseline and actual images
3. Pixel/AI comparison then runs on the masked images
4. Null or stale elements are safely skipped with a warning

**Key Features:**
- **Fixed resolution**: Always uses 1920x1080 for consistency across machines
- **Dynamic scrolling**: Automatically captures multiple screenshots for long pages
- **Browser-specific baselines**: Separate baselines for Chrome, Firefox, Edge, etc.
- **Clear error messages**: Distinguishes structure changes from visual differences
- **Native Selenium**: Works on all browsers without CDP/DevTools dependency

**Modes:**
- **Record mode** (`bnc.record.mode=true`): Saves screenshot(s) as baseline, returns true
- **Compare mode** (`bnc.record.mode=false`): Compares against baseline using hybrid strategy

**How It Works:**

1. **Recording:**
   - Sets window to 1920x1080
   - Calculates how many viewports needed (based on page height)
   - Saves: `step_1.png`, `step_2.png`, etc.

2. **Comparison:**
   - Counts existing baseline files
   - Calculates current page needs
   - If counts differ → FAIL with "Page structure changed" message
   - If counts match → Compares each pair using hybrid strategy

**Error Messages:**
| Situation | Error Message |
|-----------|---------------|
| No baselines | "No baselines found. Run with record=true to create baselines." |
| Structure changed | "PAGE STRUCTURE CHANGED: Baseline has 2 screenshot(s), but current page needs 1" |
| Visual mismatch | "Visual mismatch on screenshot 2: 5.25% difference" |

**Hybrid Comparison Strategy:**
1. Fast pixel-based comparison (OpenCV) runs first
2. If result is in "gray zone" (5-20% pixel diff), AI fallback is used
3. AI uses DJL + ResNet18 for perceptual similarity (~45MB model, embedded in library)

| Diff Range | Strategy | Description |
|------------|----------|-------------|
| 0-0.3% | PIXEL_PASS | Clear match (within 0.3% tolerance) |
| 0.3%-5% | PIXEL_FAIL | Exceeds tolerance but below gray zone |
| 5-20% | AI_FALLBACK | Gray zone, AI decides final result |
| >20% | PIXEL_FAIL | Clear mismatch, no AI needed |

**Local vs Pipeline Execution:**

The framework automatically detects the execution context and adjusts comparison accordingly:

| Context | Resolution | Tolerance | Notes |
|---------|------------|-----------|-------|
| Pipeline (SauceLabs) | 1920x1080 (controlled) | 0.3% default | Very strict comparison |
| Local (laptop) | Dynamic (may differ) | 3% for scaled images | Higher tolerance when scaling needed |

**Why this matters:**
- Baselines are recorded at 1920x1080 (SauceLabs controlled environment)
- Local laptops may have smaller screens (e.g., 1366x768, 1536x864)
- When local resolution differs from baseline, the framework:
  1. Detects the resolution mismatch
  2. Scales actual image UP to baseline dimensions using high-quality BICUBIC interpolation
  3. Applies higher tolerance (8%) to account for scaling artifacts
  4. Extends gray zone to 25% for AI fallback opportunities
  5. Logs clear warnings about the scaled comparison

**Scaled Comparison Behavior:**
```
========================================
LOCAL SCALED COMPARISON MODE
Original tolerance: 0.30%, Effective tolerance: 3.00%
Scale factor: 1.13x
Gray zone extended to 25% to account for scaling artifacts
========================================
```

**Key point:** Big structural changes (deleted logos, missing elements) will still be detected even with scaled comparison. The higher tolerance only helps with minor scaling artifacts (anti-aliasing, font rendering differences).

**File locations:**
- Baselines: `src/test/resources/baselines/{browser}/{language}/{ClassName}/{stepName}_1.png`
- Multiple viewports: `{stepName}_1.png`, `{stepName}_2.png`, etc.
- Diff images: `target/metrics/visual/{ClassName}_{language}_{stepName}_1_diff.png`

**Baseline Structure (with language support):**
```
src/test/resources/baselines/
├── chrome/
│   ├── en/
│   │   └── LoginTest/
│   │       ├── login_page_1.png    # English, first viewport
│   │       └── login_page_2.png    # English, second viewport
│   └── fr/
│       └── LoginTest/
│           └── login_page_1.png    # French version
├── firefox/
│   ├── en/
│   │   └── LoginTest/
│   │       └── login_page_1.png
│   └── fr/
│       └── LoginTest/
│           └── login_page_1.png
└── edge/
    └── en/
        └── LoginTest/
            └── login_page_1.png
```

**Configuration:**
- `bnc.record.mode` - Enable record mode (true/false)
- `bnc.baselines.root` - Override baseline directory location
- `bnc.visual.ai.enabled` - Enable/disable AI fallback (default: true)
- `bnc.web.gui.lang` - Language for baselines (e.g., "en", "fr") - from context.json
- `lang` - Fallback language property - from debug_config.json

**Language Resolution Priority:**
1. `bnc.web.gui.lang` system property (pipeline/context.json)
2. `lang` system property (local/debug_config.json)
3. Default: "en"

**Usage in debug_config.json:**
```json
{
  "record": true,   // Sets bnc.record.mode system property (overrides existing baselines)
  ...
}
```

**Record Mode Behavior:**
- `record: true` - Creates new baselines or **overwrites** existing ones
- `record: false` - Compares against existing baselines
- The setting is loaded directly by `VisualCapture` from `debug_config.json`

**Diff Image Visualization:**
When visual comparison fails, a diff image is generated with:
- **Separate red ellipses/circles** around each distinct diff region
- Each region is numbered (#1, #2, etc.) for easy identification
- Summary label at top (e.g., "DIFF: 2 regions, 1234 pixels total")
- Regions within 50 pixels are automatically merged into one circle
- Noise is filtered out (regions with less than 10 pixels are ignored)

### AI Image Comparison (DJL + ResNet18)

The framework includes AI-based image comparison using Deep Java Library (DJL):

```java
// Direct usage of AI comparator
AIImageComparator aiComparator = new AIImageComparator(0.92); // 92% threshold
AIComparisonResult result = aiComparator.compare(baseline, actual);
if (result.isMatch()) {
    // Images are perceptually similar
}
```

**How it works:**
1. ResNet18 pre-trained model extracts 512-dimensional feature vectors
2. Cosine similarity compares the feature vectors
3. Threshold determines match/mismatch

**Model details:**
- Size: ~45MB (embedded in library JAR)
- Training: ImageNet (14 million images)
- Location: `src/main/resources/models/resnet18/traced_resnet18.pt`

**Model loading priority:**
1. Embedded model from classpath (bundled with library) - **default, works offline**
2. Local model path (if `bnc.visual.ai.model.path` is set)
3. Direct URL download (if `bnc.visual.ai.model.url` is set)
4. DJL model zoo (requires internet access)

**Override with custom model (optional):**

To use a different model version, set the system property:
```bash
# Via Maven
mvn test -Dbnc.visual.ai.model.path=C:/models/resnet18

# Or in debug_config.json
# Add: "bnc.visual.ai.model.path": "C:/models/resnet18"
```

**AI configuration properties:**
| Property | Description |
|----------|-------------|
| `bnc.visual.ai.model.path` | Override: Local path to custom ResNet18 model |
| `bnc.visual.ai.model.url` | Override: Direct URL to download model |
| `bnc.visual.ai.enabled` | Enable/disable AI fallback (default: true) |
| `DJL_CACHE_DIR` | Override DJL cache location (default: system temp) |

**Corporate Environment Support:**
The AI comparator automatically handles corporate environments with SSL interception:
- SSL certificate verification is disabled for DJL downloads (MITM proxy compatible)
- DJL cache directory defaults to system temp (`java.io.tmpdir/djl-cache`)
- Embedded JNI library (`djl_torch.dll`) is auto-extracted to avoid runtime downloads
- Works fully offline after first initialization

## Test Reports

### Automatic Report Generation

Test reports are automatically generated at suite completion in multiple formats:

**Output location:** `target/metrics/`
- `test-report-latest.html` - Human-readable HTML report
- `test-report-latest.json` - Machine-readable JSON
- `test-report-latest.csv` - Spreadsheet-compatible CSV
- `test-report-latest.xml` - XML format for CI/CD integration
- Timestamped versions are also saved (e.g., `test-report_SuiteName_20240115_143022.html`)

**Report contents:**
- Suite summary (passed/failed/skipped counts, success rate)
- Individual test results with timing
- Error messages and stack traces for failures
- Visual comparison metrics with **actual image** and **diff image** thumbnails (clickable)
- API call metrics (if API testing used)

**Configuration:**
- `bnc.metrics.enabled` - Enable/disable report generation (default: true)

**Disable reports:**
```bash
mvn test -Dbnc.metrics.enabled=false
```
