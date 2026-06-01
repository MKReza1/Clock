# GitHub Actions APK Build Guide 🚀

This repository is equipped with a professional, robust **GitHub Actions CI/CD pipeline** located in `.github/workflows/android.yml`. It automatically builds your application, compiles the correct dependencies, runs optimizations, and packs them into installable Android Package files (APKs) on every commit.

---

## 📦 What the Pipeline Does

Whenever you **push** code to the `main` or `master` branch, open a **Pull Request**, or **manually run** the workflow via GitHub UI:
1. **Resolves Environment dependencies & cache systems** using high-speed JDK 17 caching.
2. **Decodes client-side keystores** so the build works securely "out of the box" without needing external keys.
3. **Compiles & builds the Debug APK**.
4. **Validates production signing tokens** and securely compiles a signed **Release APK** if configured.
5. **Registers downloadable artifacts** in the action's summary dashboard.

---

## 🚀 How to Trigger a Build

### 1. Automatic Triggers
Simply push code or merge a pull request to your main integration branch:
```bash
git add .
git commit -m "feat: updated minimalism design layout"
git push origin main
```

### 2. Manual Trigger (Universal)
1. Navigate to your repository on **GitHub**.
2. Click the **Actions** tab along the top navigation bar.
3. In the left panel, select **Android CI - Build APK**.
4. Click the dropdown menu **Run workflow** -> select your branch -> click **Run workflow**.

---

## 📥 Where to Find & Download your APK

Once a pipeline run finishes successfully (indicated by a green checkmark):
1. Select the specific workflow run from the laundry list under the **Actions** tab.
2. Scroll to the bottom of the page to find the **Artifacts** section.
3. You will see:
   * 🍏 `alarm-clock-debug-apk` (This is the instantly installable testing build, perfect for testing on emulator/real devices).
   * 🔐 `alarm-clock-release-apk` (This will appear only after you set up the signature keys below!).
4. Click on the package name to download the zipped `.apk` file, unzip it, and copy it onto your phone!

---

## 🔐 How to configure Production Release Signing

To build a professional production-grade signed **Release APK** that you can submit to the Google Play Store or install with maximum device trust, configure the following secrets inside your GitHub repository:

### Step 1: Create a Release Keystore (If you don't have one)
In your terminal, run the following command to generate a key file (`my-upload-key.jks`):
```bash
keytool -genkey -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000 -storepass YOUR_STORE_SECRET -keypass YOUR_KEY_SECRET
```
*Note: Keep this key file safe. Do not lose your passwords.*

### Step 2: Convert the Keystore to Base64 String
Since GitHub requires secrets as strings, convert the binary file to Base64:
* **macOS / Linux:**
  ```bash
  base64 my-upload-key.jks > key_base64.txt
  ```
* **Windows (PowerShell):**
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) > key_base64.txt
  ```
Copy the long string content of `key_base64.txt`.

### Step 3: Register SECRETS in GitHub settings
1. In your GitHub repository, click on ⚙️ **Settings** tab.
2. On the left sidebar, expand **Secrets and variables** and click **Actions**.
3. Create three new secret parameters:
   * **`KEYSTORE_BASE64`**: Paste the entire copied content of your `key_base64.txt`.
   * **`STORE_PASSWORD`**: Your chosen store password (e.g., `YOUR_STORE_SECRET`).
   * **`KEY_PASSWORD`**: Your chosen key password (e.g., `YOUR_KEY_SECRET`).
4. Re-run or push code again. The workflow will detect the keys, compile, sign, and publish your secure production `alarm-clock-release-apk`!

---

## 🛠️ Troubleshooting

* **Build times are taking too long:** The action uses high-performance caching for both JDK packages and Gradle dependencies, meaning subsequence runs will be 3-4x faster than the initial cold build!
* **Permission Denied error during build:** If you receive a permission error with `./gradlew`, verify that the executable permissions were successfully set in Git (`git update-index --chmod=+x gradlew` or standard shell `chmod +x gradlew`). The pipeline handles this step automatically!
