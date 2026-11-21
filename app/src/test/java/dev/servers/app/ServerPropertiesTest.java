package dev.servers.app;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class ServerPropertiesTest {
    @Test
    public void loadsAndSavesBedrockKeyValueProperties() throws Exception {
        File folder = Files.createTempDirectory("bedrock-properties").toFile();
        File file = new File(folder, "server.properties");
        Files.write(file.toPath(), (
                "# Bedrock server settings\n" +
                "server-name=Test World\n" +
                "level-name=world_one\n" +
                "max-players=6\n"
        ).getBytes(StandardCharsets.UTF_8));

        ServerProperties properties = ServerProperties.load(file);
        assertEquals("Test World", properties.get("server-name", ""));
        assertEquals("world_one", properties.get("level-name", ""));
        assertEquals("6", properties.get("max-players", "0"));

        properties.set("max-players", "10");
        properties.save(file);
        assertEquals("10", ServerProperties.load(file).get("max-players", "0"));
        ServerRepository.deleteRecursively(folder);
    }
}
