package dev.servers.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small properties editor that retains the standard Bedrock key=value format. */
public final class ServerProperties {
    private final LinkedHashMap<String, String> values = new LinkedHashMap<>();

    public static ServerProperties load(File file) throws IOException {
        ServerProperties result = new ServerProperties();
        if (!file.isFile()) return result;
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[16 * 1024];
            int count;
            int total = 0;
            while ((count = in.read(chunk)) != -1) {
                total += count;
                if (total > 2_000_000) throw new IOException("server.properties is larger than the 2 MB safety limit");
                buffer.write(chunk, 0, count);
            }
            if (total > 0) {
                String source = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                for (String line : source.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) continue;
                    int split = trimmed.indexOf('=');
                    if (split <= 0) continue;
                    result.values.put(trimmed.substring(0, split).trim(), trimmed.substring(split + 1).trim());
                }
            }
        }
        return result;
    }

    public String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    public void set(String key, String value) {
        values.put(key, value == null ? "" : value);
    }

    public Map<String, String> asMap() {
        return new LinkedHashMap<>(values);
    }

    public void save(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create server folder");
        }
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            output.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        File temp = new File(parent == null ? file.getAbsoluteFile().getParentFile() : parent,
                file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(output.toString().getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
        try {
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw new IOException("Could not safely replace server.properties", error);
        }
    }
}
