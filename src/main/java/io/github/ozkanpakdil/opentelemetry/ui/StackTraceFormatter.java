package io.github.ozkanpakdil.opentelemetry.ui;

import io.github.ozkanpakdil.opentelemetry.metricdata.ExceptionData;

public class StackTraceFormatter {
    public static String formatStackTrace(ExceptionData.ExceptionDetailData exceptionDetailData) {
        StringBuilder sb = new StringBuilder();
        sb.append(exceptionDetailData.typeName).append(": ").append(exceptionDetailData.message).append("\n");
        if (exceptionDetailData.parsedStack != null) {
            for (ExceptionData.ExceptionDetailData.Stack stack : exceptionDetailData.parsedStack) {
                sb.append("  at ").append(stack.method).append("\n");
                if (stack.fileName != null)
                    sb.append("    ").append(stack.fileName).append(':').append(stack.line).append("\n");
            }
        }
        return sb.toString();
    }
}
