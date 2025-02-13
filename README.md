# OpenTelemetry Debug Log Viewer For Rider

<!-- Plugin description -->
This plugin allow you to see, instantly view, opentelemetry logs, in a nice way, you can use below list for the backend

To use just start a debug session with a program.
The logs will appear in a new tab(OpenTelemetry tab) in the debugger session.
<!-- Plugin description end -->

You can enable opentelemetry debug logs with below dependency in your dotnet project

https://www.nuget.org/packages/JeremyMorren.OpenTelemetry.Exporter.Console.Json

```shell
dotnet add package JeremyMorren.OpenTelemetry.Exporter.Console.Json
```

```csharp
using OpenTelemetry.Metrics;
using OpenTelemetry.Trace;
using OpenTelemetry.Exporter.Console.Json;

var tracer = Sdk.CreateTracerProviderBuilder()
    .AddJsonConsoleExporter(o => o.Targets = ConsoleExporterOutputTargets.Debug)
    // Add activity sources to the provider
    .Build();

var metrics = Sdk.CreateMeterProviderBuilder()
    .AddJsonConsoleExporter(o => o.Targets = ConsoleExporterOutputTargets.Debug)
    // Add instrumentation sources to the provider
    .Build();
```

### Kudos

Kudos to https://github.com/Socolin/ApplicationInsightsRiderPlugin and https://github.com/ozkanpakdil/opentelemetry-debug-log-viewer-rider-plugin

### Screenshot

![Screenshot1](screenshots/screenshot1.png)

### Dev

To edit and test the plugin, just open this project with [InteliJ IDEA](https://www.jetbrains.com/idea/) and run the plugin with predefined run configuration

### Latest dev version

Latest version can be found [here](https://github.com/jeremy-morren/opentelemetry-debug-log-viewer-rider-plugin)

### Build

```
./gradlew :buildPlugin -PbuildType=stable
```

Then the plugins will be in `build/distributions`

