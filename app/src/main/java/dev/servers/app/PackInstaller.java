package dev.servers.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Installs a Bedrock .mcpack into a server and enables it for the selected world. */
public final class PackInstaller {
    private PackInstaller() {}

    public static String install(File sourcePack, BedrockServer server) throws Exception {
        File manifestFile = findFile(sourcePack, "manifest.json", 5);
        if (manifestFile == null) throw new IOException("manifest.json was not found");
        JSONObject manifest = readJson(manifestFile);
        JSONObject header = manifest.getJSONObject("header");
        String uuid = header.getString("uuid");
        JSONArray version = header.optJSONArray("version");
        if (version == null) version = new JSONArray().put(1).put(0).put(0);

        String folderName = sourcePack.getName();
        boolean resource = isResourcePack(manifest.optJSONArray("modules"));
        String type = resource ? "resource" : "behavior";
        File destinationParent = new File(server.directory, resource ? "resource_packs" : "behavior_packs");
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Could not create " + type + "_packs directory");
        }
        String levelName = ServerProperties.load(server.propertiesFile()).get("level-name", "Bedrock level");
        File worldsRoot = new File(server.directory, "worlds").getCanonicalFile();
        File world = new File(worldsRoot, levelName).getCanonicalFile();
        String allowedPrefix = worldsRoot.getPath() + File.separator;
        if (!world.getPath().startsWith(allowedPrefix) || !world.isDirectory()) {
            throw new IOException("World folder was not found or has an invalid path: " + levelName);
        }

        File destination = new File(destinationParent, folderName);
        File temporary = new File(destinationParent, "." + folderName + ".installing");
        File backup = new File(destinationParent, "." + folderName + ".backup");
        ServerRepository.deleteRecursively(temporary);
        ServerRepository.deleteRecursively(backup);
        try {
            copyDirectory(sourcePack, temporary);
            if (destination.exists() && !destination.renameTo(backup)) {
                throw new IOException("Could not preserve the previous installed pack");
            }
            if (!temporary.renameTo(destination)) {
                if (backup.exists()) backup.renameTo(destination);
                throw new IOException("Could not activate the new pack folder");
            }
            ServerRepository.deleteRecursively(backup);
        } catch (IOException error) {
            ServerRepository.deleteRecursively(temporary);
            if (!destination.exists() && backup.exists()) backup.renameTo(destination);
            throw error;
        }
        String jsonName = resource ? "world_resource_packs.json" : "world_behavior_packs.json";
        File enabledFile = new File(world, jsonName);
        JSONArray enabled = readArray(enabledFile);
        JSONArray next = new JSONArray();
        boolean replaced = false;
        for (int i = 0; i < enabled.length(); i++) {
            JSONObject entry = enabled.optJSONObject(i);
            if (entry == null) continue;
            if (uuid.equalsIgnoreCase(entry.optString("pack_id"))) {
                if (!replaced) {
                    next.put(new JSONObject().put("pack_id", uuid).put("version", version));
                    replaced = true;
                }
            } else {
                next.put(entry);
            }
        }
        if (!replaced) next.put(new JSONObject().put("pack_id", uuid).put("version", version));
        writeJson(enabledFile, next);
        return (resource ? "Resource" : "Behavior") + " pack installed and enabled";
    }

    private static boolean isResourcePack(JSONArray modules) {
        if (modules == null) return false;
        for (int i = 0; i < modules.length(); i++) {
            JSONObject module = modules.optJSONObject(i);
            if (module != null && "resources".equalsIgnoreCase(module.optString("type"))) return true;
        }
        return false;
    }

    private static JSONObject readJson(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[16 * 1024];
            int count;
            int total = 0;
            while ((count = in.read(chunk)) != -1) {
                total += count;
                if (total > 512_000) throw new IOException("Pack manifest is larger than 512 KB");
                buffer.write(chunk, 0, count);
            }
            return new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    private static JSONArray readArray(File file) throws Exception {
        if (!file.isFile()) return new JSONArray();
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[16 * 1024];
            int count;
            int total = 0;
            while ((count = in.read(chunk)) != -1) {
                total += count;
                if (total > 512_000) throw new IOException("World pack list is larger than 512 KB");
                buffer.write(chunk, 0, count);
            }
            return new JSONArray(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    private static void writeJson(File file, JSONArray array) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create world folder");
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write((array.toString(2) + "\n").getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (Exception error) {
            // Keep the original world pack list intact if serialization fails.
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw new IOException("Could not write world pack list", error);
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
            throw new IOException("Could not safely replace world pack list", error);
        }
    }

    private static File findFile(File folder, String name, int depth) {
        if (folder == null || !folder.isDirectory() || depth < 0) return null;
        File direct = new File(folder, name);
        if (direct.isFile()) return direct;
        File[] children = folder.listFiles(File::isDirectory);
        if (children != null) for (File child : children) {
            File found = findFile(child, name, depth - 1);
            if (found != null) return found;
        }
        return null;
    }

    private static void copyDirectory(File source, File destination) throws IOException {
        if (!destination.exists() && !destination.mkdirs()) throw new IOException("Could not create pack folder");
        File[] files = source.listFiles();
        if (files == null) throw new IOException("Could not read pack folder");
        for (File file : files) {
            File target = new File(destination, file.getName());
            if (file.isDirectory()) copyDirectory(file, target);
            else {
                try (FileInputStream in = new FileInputStream(file); FileOutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[32 * 1024];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    out.getFD().sync();
                }
            }
        }
    }
}
