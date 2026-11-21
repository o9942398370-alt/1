package dev.servers.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Runtime capability gate.
 *
 * This first standalone source revision intentionally does not execute the Linux BDS binary.
 * Official BDS packages target x86_64 Linux and require a glibc userspace. An Android ARM64
 * APK needs a bundled Box64 + Linux userspace runtime before that binary can be launched.
 */
public final class BedrockRuntime {
    public static final int EM_X86_64 = 62;
    public static final int EM_AARCH64 = 183;

    public static final class Report {
        public final String architecture;
        public final String message;
        public final boolean ready;

        Report(String architecture, String message, boolean ready) {
            this.architecture = architecture;
            this.message = message;
            this.ready = ready;
        }
    }

    private BedrockRuntime() {}

    public static Report inspect(BedrockServer server) {
        File executable = server.executable();
        if (!executable.isFile()) {
            return new Report("Unknown", "bedrock_server was not found", false);
        }
        int machine;
        try {
            machine = readMachine(executable);
        } catch (IOException error) {
            return new Report("Unknown", "Could not inspect the server executable: " + error.getMessage(), false);
        }
        String architecture;
        if (machine == EM_X86_64) architecture = "Linux x86_64";
        else if (machine == EM_AARCH64) architecture = "Linux ARM64";
        else architecture = "ELF machine " + machine;

        return new Report(architecture,
                "This build does not yet contain the embedded Linux runtime " +
                "(Box64 + glibc userspace) required to start official Bedrock Dedicated Server builds on Android. " +
                "Your server files have not been changed.", false);
    }

    private static int readMachine(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] header = new byte[20];
            int count = 0;
            while (count < header.length) {
                int read = in.read(header, count, header.length - count);
                if (read < 0) break;
                count += read;
            }
            if (count < 20 || header[0] != 0x7f || header[1] != 'E' || header[2] != 'L' || header[3] != 'F') {
                throw new IOException("Not a valid ELF executable");
            }
            boolean littleEndian = header[5] == 1;
            if (!littleEndian) throw new IOException("Big-endian ELF is not supported");
            return (header[18] & 0xff) | ((header[19] & 0xff) << 8);
        }
    }
}
