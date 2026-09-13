package dev.doorevisuals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class BootDebugLog {
    private static final Path LOG = Path.of("C:/Users/zxcho/Projects/DooreVisuals/debug-370a55.log");

    private BootDebugLog() {
    }

    static void write(String hypothesisId, String location, String message, String dataJson) {
        try {
            long i = System.currentTimeMillis();
            String s = "{\"sessionId\":\"370a55\",\"runId\":\"boot\",\"hypothesisId\":\""
                + hypothesisId
                + "\",\"location\":\""
                + location
                + "\",\"message\":\""
                + esc(message)
                + "\",\"data\":"
                + (dataJson == null ? "{}" : dataJson)
                + ",\"timestamp\":"
                + i
                + "}\n";
            Files.writeString(LOG, s, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception exception) {
        }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
