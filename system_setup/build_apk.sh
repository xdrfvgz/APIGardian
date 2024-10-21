echo ""
echo "Build APK with gradle:"
echo ""
cd ..
gradle wrapper
./gradlew assembleDebug
