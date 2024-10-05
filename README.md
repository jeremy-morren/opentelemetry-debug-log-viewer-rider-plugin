# Opentelemetry Debug Log Viewer

<!-- Plugin description -->
This plugin allow you to see, instantly, in a nice way [Honeycomb.io](https://www.honeycomb.io/)

To use just start a debug session with a program using Honeycomb.io.
The logs will appear in a new tab in the debugger session.
<!-- Plugin description end -->

### Dev

To edit and test the plugin, just open this project with [InteliJ IDEA](https://www.jetbrains.com/idea/) and run the plugin with predefined run configuration

### Build

```
./gradlew :buildPlugin -PbuildType=stable
```

Then the plugins will be in `build/distributions`

### Screenshot

![Screenshot](screenshots/screenshot1.png)
