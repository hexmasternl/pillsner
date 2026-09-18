## 1. Quantity caching

- [x] 1.1 In `Quantity.kt`, replace the `normalizedValue` `get()` with a `by lazy { value.stripTrailingZeros() }` property so `stripTrailingZeros()` runs at most once per instance.
- [x] 1.2 Confirm `hashCode()` and `toString()` still read the cached `normalizedValue` and that `equals()` is untouched (it already compares via `value.compareTo(other.value)`).
- [x] 1.3 Add/confirm a unit test that two `Quantity` instances with different scales but equal numeric value (e.g. `1` and `1.0` tablet) still produce equal `hashCode()` and `equals()` results, and that repeated `hashCode()`/`toString()` calls return stable results.

## 2. Wear module minSdk

- [x] 2.1 In `wear/build.gradle.kts`, change `minSdk = libs.versions.minSdk.get()` to `minSdk = libs.versions.wearMinSdk.get()`.
- [x] 2.2 Confirm the README's Toolchain table already states `minSdk` as "26 (phone), 30 (Wear OS)" — no README edit needed (verify, don't just assume).
- [x] 2.3 Run a wear module assemble (e.g. `gradlew :wear:assembleDebug` from `src/`) to confirm it still builds cleanly against API 30.

## 3. Verification

- [x] 3.1 Run the unit test task from `src/` (Gradle wrapper) and confirm it passes, including the new/updated `Quantity` test.
- [x] 3.2 Run the lint task from `src/` and confirm no new warnings from either change.
