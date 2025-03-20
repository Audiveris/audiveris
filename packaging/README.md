# How to Use the Audiveris macOS Installer

This guide explains how to install and run the Audiveris application on macOS using the provided DMG installer. Since the installer is not signed with an Apple Developer certificate, you'll need to adjust your macOS privacy settings to allow it to run.

## Installation Steps

1. **Obtain the DMG File**
    - Download or receive the `Audiveris-<version>.dmg` file (e.g., `Audiveris-1.0.dmg`) from the source (e.g., a contributor or repository release).

2. **Open the DMG**
    - Double-click the `Audiveris-<version>.dmg` file in your `Downloads` folder (or wherever it’s saved). This mounts the installer as a virtual disk on your desktop or in Finder.

3. **Install the Application**
    - Inside the mounted DMG, you’ll see `Audiveris.app`. Drag this file to your **Applications** folder to install it.
    - Once copied, you can eject the DMG by clicking the eject icon next to it in Finder or dragging it to the trash.

## Running Audiveris

Since the app is not signed, macOS will block it by default. Follow these steps to allow it to run:

1. **Attempt to Open the App**
    - Go to your **Applications** folder and double-click `Audiveris.app`.
    - You’ll likely see a warning: *"“Audiveris” cannot be opened because it is from an unidentified developer."*

2. **Adjust Privacy Settings**
    - Open **System Preferences** (or **System Settings** on macOS Ventura and later):
        - Click the Apple menu () > **System Preferences** > **Security & Privacy** > **General** tab.
    - At the bottom, you’ll see a message: *“Audiveris” was blocked from use because it is not from an identified developer.*
    - Click **"Open Anyway"** to allow the app to run.

3. **Launch the App**
    - Double-click `Audiveris.app` again. You may see one final prompt asking for confirmation—click **"Open"**.
    - The app should now launch successfully.

## Notes

- **Unsigned App**: The lack of a signature is due to the installer not being created with an Apple Developer account. This is a one-time adjustment; once approved, macOS will remember your choice.
- **Troubleshooting**: If the app still won’t open, ensure you’ve completed the privacy settings step. For persistent issues, contact the provider or check the Audiveris documentation.