
# APIGuardian

APIGuardian is an Android application that allows users to monitor specified API endpoints and trigger an alarm when a target value is detected in the API response.

## Features

- Monitor any API endpoint at regular intervals
- Support for both JSON and plain text responses
- Customizable JSON path for value extraction
- User-defined target value for triggering the alarm
- Custom alarm sound selection
- Background monitoring service
- Secure storage for sensitive data

## Setup and Build Instructions

### 1. Clone the Repository

First, clone the APIGuardian repository to your local machine:

```
git clone https://github.com/xdrfvgz/APIGuardian
cd APIGuardian
```

### 2. Prepare Ubuntu System for APK Compilation

This project includes an install script that prepares your Ubuntu system for APK compilation. To use it:

1. Navigate to the system_setup directory:
   ```
   cd system_setup
   ```

2. Make the install script executable:
   ```
   chmod +x install.sh
   ```

3. Run the script with sudo privileges:
   ```
   sudo ./install.sh
   ```

This script will:
- Update system packages
- Install required dependencies (e.g., OpenJDK, Gradle, Android SDK)
- Set up environment variables for Android SDK
- Install specific Android SDK components needed for compilation

Note: This script is designed for Ubuntu systems. It may need modifications for other Linux distributions.

### 3. Building the APK

After preparing your system, you can build the debug APK:

1. Return to the project root directory:
   ```
   cd ..
   ```

2. Run the following commands:
   ```
   ./gradlew wrapper
   ./gradlew assembleDebug
   ```

3. The debug APK will be generated in the `app/build/outputs/apk/debug/` directory

## Downloading APK from GitHub Actions

If you prefer not to build the APK yourself, you can download the latest version built by GitHub Actions:

1. Go to the [APIGuardian GitHub repository](https://github.com/xdrfvgz/APIGuardian)
2. Click on the "Actions" tab at the top of the repository page
3. In the left sidebar, click on the workflow that builds the APK (e.g., "Android CI")
4. From the list of workflow runs, click on the most recent successful run (marked with a green checkmark)
5. Scroll down to the "Artifacts" section
6. Click on the artifact name (e.g., "app-debug") to download the APK
7. Once downloaded, transfer the APK to your Android device and install it

Note: You may need to enable "Install from unknown sources" in your Android settings to install the APK.

## Usage

1. Install the generated or downloaded APK on your Android device
2. Launch APIGuardian
3. Go to Settings and configure:
   - API URL
   - Is JSON Response (toggle)
   - JSON Path (if JSON response)
   - Target Value
   - Alarm Sound (optional)
4. Return to the main screen
5. Press "START MONITORING" to begin API monitoring
6. APIGuardian will check the API every minute and trigger an alarm if the target value is found
7. Press "STOP MONITORING" to stop the monitoring service

## Contributing

Contributions to APIGuardian are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact

For questions, support or bug reports, please open an issue on the [GitHub repository](https://github.com/xdrfvgz/APIGuardian/issues).
