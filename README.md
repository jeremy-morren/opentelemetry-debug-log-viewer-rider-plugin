# OpenTelemetry Debug Log Viewer For Rider

<!-- Plugin description -->
This plugin allow you to see, instantly view, opentelemetry logs, in a nice way, you can use below list for the backend

To use just start a debug session with a program.
The logs will appear in a new tab(OpenTelemetry tab) in the debugger session.
<!-- Plugin description end -->

You can enable opentelemetry debug logs with below dependency in your dotnet project

https://www.nuget.org/packages/Ozkanpakdil.OpenTelemetry.Exporter.Json.Console
```xml
<ItemGroup>
    ...
    <PackageReference Include="Ozkanpakdil.OpenTelemetry.Exporter.Json.Console" Version="1.0.13" />
    ...
</ItemGroup>
```

```csharp
using OpenTelemetry.Exporter;
using OpenTelemetry.Trace;
using Ozkanpakdil.OpenTelemetry.Exporter.Json.Console;

var tracerProvider = Sdk.CreateTracerProviderBuilder()
    .AddHttpClientInstrumentation()
    .AddJsonConsoleExporter(o => o.Targets = ConsoleExporterOutputTargets.Debug)
    .Build();

var client = new HttpClient();
await client.GetAsync("https://github.com/");
```

### Dev

To edit and test the plugin, just open this project with [InteliJ IDEA](https://www.jetbrains.com/idea/) and run the plugin with predefined run configuration

### Build

```
./gradlew :buildPlugin -PbuildType=stable
```

Then the plugins will be in `build/distributions`

### Screenshot

![Screenshot](screenshots/screenshot1.png)

### Kudos

Kudos to https://github.com/Socolin/ApplicationInsightsRiderPlugin and https://github.com/ozkanpakdil/opentelemetry-debug-log-viewer-rider-plugin

### Latest dev version

Latest version can be found [here](https://github.com/jeremy-morren/opentelemetry-debug-log-viewer-rider-plugin)