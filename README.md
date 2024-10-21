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

## Setup

1. Clone the repository
2. Ensure you have Java Development Kit (JDK) installed

## Building the APK

To build the debug APK, follow these steps:

1. Open a terminal in the project root directory
2. Run the following commands:

   ```
   ./gradlew wrapper
   ./gradlew assembleDebug
   ```

3. The debug APK will be generated in the `app/build/outputs/apk/debug/` directory

## Usage

1. Install the generated APK on your Android device
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

## Project Structure

- `MainActivity`: The main screen of the app, displaying current settings and monitoring status
- `SettingsActivity`: Allows users to configure monitoring parameters
- `APIMonitorService`: Background service that performs API checks
- `SecureStorage`: Handles encrypted storage of sensitive data

## Dependencies

- OkHttp: For making API requests
- Kotlin Coroutines: For asynchronous programming
- AndroidX Security: For encrypted shared preferences
- MediaPlayer: For playing alarm sounds

## Permissions

- `INTERNET`: Required for making API requests
- `ACCESS_NETWORK_STATE`: For checking network connectivity

## Contributing

Contributions to APIGuardian are welcome! Please feel free to submit a Pull Request.

## License

[Insert your chosen license here]

## Contact

[Your contact information or project maintainer's contact]
