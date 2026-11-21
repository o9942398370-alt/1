package dev.servers.app;

import java.io.File;

public final class BedrockServer {
    public final File directory;
    public final String name;

    public BedrockServer(File directory) {
        this.directory = directory;
        this.name = directory.getName();
    }

    public File executable() {
        return new File(directory, "bedrock_server");
    }

    public File propertiesFile() {
        return new File(directory, "server.properties");
    }

    public boolean isValidBedrockServer() {
        return directory.isDirectory() && executable().isFile() && propertiesFile().isFile();
    }
}
