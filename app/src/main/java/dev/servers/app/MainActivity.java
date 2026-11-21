package dev.servers.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    // Palette recovered from the supplied APK. Keep the original monochrome dark style.
    private static final int BLACK = Color.BLACK;
    private static final int WHITE = Color.WHITE;
    private static final int CARD = 0xff121212;
    private static final int BORDER = 0xff2c2c2c;
    private static final int MUTED = 0xff9a9a9a;
    private static final int GREEN = 0xff2e7d32;
    private static final int ORANGE = 0xffb26a00;
    private static final int GRAY = 0xff616161;

    private static final int PICK_SERVER_FOLDER = 101;
    private static final int PICK_BUILD_ZIP = 102;
    private static final int PICK_PACK = 103;

    private LinearLayout root;
    private LinearLayout content;
    private ScrollView scroll;
    private TextView appTitle;
    private TextView statusChip;
    private int selectedTab = 0;
    private ServerRepository repository;
    private SharedPreferences preferences;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);
        repository = new ServerRepository(this);
        preferences = getSharedPreferences("server_preferences", MODE_PRIVATE);
        buildFrame();
        render();
    }

    @Override
    protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }

    private void buildFrame() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BLACK);
        root.setPadding(dp(16), dp(14), dp(16), dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        appTitle = text("servers", 22, WHITE, true);
        top.addView(appTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        statusChip = chip("Bedrock", BORDER);
        top.addView(statusChip);
        root.addView(top);

        TextView subtitle = text("Bedrock server manager", 12, MUTED, false);
        subtitle.setPadding(0, dp(4), 0, dp(14));
        root.addView(subtitle);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"Servers", "Builds", "Packs", "Settings"};
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            Button button = button(labels[i], true, false, v -> {
                selectedTab = tab;
                render();
            });
            button.setTextSize(12);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
            params.setMargins(0, 0, dp(5), 0);
            tabs.addView(button, params);
        }
        root.addView(tabs);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(16), 0, dp(8));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void render() {
        content.removeAllViews();
        switch (selectedTab) {
            case 1: renderBuilds(); break;
            case 2: renderPacks(); break;
            case 3: renderSettings(); break;
            default: renderServers(); break;
        }
    }

    private void renderServers() {
        section("Servers");
        body("Every server imported here is stored inside this app. Only Minecraft Bedrock Linux server folders are accepted.");
        addGap(8);
        addButton("Import Bedrock server folder", true, v -> pickServerFolder());
        addGap(12);
        List<BedrockServer> servers = repository.scanServers();
        if (servers.isEmpty()) {
            cardStart();
            addText("No Bedrock servers yet", 16, WHITE, true);
            addText("Import a Bedrock server folder or a Linux Bedrock build ZIP. Java server files are not scanned.", 13, MUTED, false);
            cardEnd();
            return;
        }
        for (BedrockServer server : servers) serverCard(server);
    }

    private void serverCard(BedrockServer server) {
        cardStart();
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(server.name, 17, WHITE, true);
        line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(chip("Stopped", GRAY));
        currentParent().addView(line);

        BedrockRuntime.Report report = BedrockRuntime.inspect(server);
        addText(report.architecture, 12, MUTED, false);
        addText("World: " + ServerPropertiesSafe.worldName(server), 12, MUTED, false);
        addGap(8);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button start = button("Start", true, false, v -> showRuntimeBlock(server));
        Button properties = button("Properties", true, false, v -> editProperties(server));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(42), 1f);
        half.setMargins(0, 0, dp(6), 0);
        actions.addView(start, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, dp(42), 1f);
        actions.addView(properties, half2);
        currentParent().addView(actions);
        cardEnd();
    }

    private void renderBuilds() {
        section("Bedrock Builds");
        body("Import official Bedrock Linux server ZIP files. The archive is extracted into this app and checked for bedrock_server and server.properties.");
        addGap(8);
        addButton("Import Bedrock build ZIP", true, v -> pickBuildZip());
        addGap(10);
        addText("Imported builds are added to Servers automatically.", 12, MUTED, false);
        addGap(8);
        List<BedrockServer> servers = repository.scanServers();
        for (BedrockServer server : servers) {
            cardStart();
            addText(server.name, 15, WHITE, true);
            addText(BedrockRuntime.inspect(server).architecture, 12, MUTED, false);
            cardEnd();
        }
    }

    private void renderPacks() {
        section("Packs");
        body("Import Bedrock .mcpack archives. Install a pack into a server to copy it into behavior_packs or resource_packs and enable it for the selected world.");
        addGap(8);
        addButton("Import Bedrock .mcpack", true, v -> pickPack());
        addGap(10);
        List<File> packs = repository.listPacks();
        if (packs.isEmpty()) {
            cardStart();
            addText("No packs imported", 15, WHITE, true);
            addText("Only Bedrock packs with a valid manifest.json are accepted.", 12, MUTED, false);
            cardEnd();
            return;
        }
        for (File pack : packs) {
            cardStart();
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = text(pack.getName(), 15, WHITE, true);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button install = button("Install", true, false, v -> chooseServerForPack(pack));
            row.addView(install, new LinearLayout.LayoutParams(dp(96), dp(40)));
            currentParent().addView(row);
            cardEnd();
        }
    }

    private void renderSettings() {
        section("Settings");
        cardStart();
        addText("Storage", 14, MUTED, true);
        addText("App-private Bedrock library", 16, WHITE, true);
        addText(repository.getServersDirectory().getAbsolutePath(), 11, MUTED, false);
        addGap(8);
        addText("Imported worlds, server settings and packs stay inside this app's private storage. No terminal app or external command runner is used.", 12, MUTED, false);
        cardEnd();

        addGap(8);
        cardStart();
        addText("Behavior", 14, MUTED, true);
        Switch autoStart = toggle("Start all servers when the app opens", "auto_start_all", false);
        currentParent().addView(autoStart);
        Switch autoRestart = toggle("Restart a server after a crash", "auto_restart", false);
        currentParent().addView(autoRestart);
        addText("These preferences are saved. They become active when the embedded Bedrock runtime is added.", 12, MUTED, false);
        cardEnd();

        addGap(8);
        cardStart();
        addText("Built-in runtime", 14, MUTED, true);
        addText("Not included in this source revision", 16, WHITE, true);
        addText("Official Bedrock Linux servers are x86_64 Linux programs. Android ARM64 needs an embedded Box64 and glibc userspace runtime. The app has no external terminal dependency, but the runtime still needs to be integrated before local server execution can work.", 12, MUTED, false);
        cardEnd();

        addGap(8);
        cardStart();
        addText("About", 14, MUTED, true);
        addText("servers", 18, WHITE, true);
        addText("Version 0.0.0 (1)", 12, MUTED, false);
        addText("Bedrock Edition only", 12, MUTED, false);
        cardEnd();
    }

    private Switch toggle(String label, String key, boolean fallback) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setTextColor(WHITE);
        control.setTextSize(14);
        control.setPadding(0, dp(8), 0, dp(8));
        control.setChecked(preferences.getBoolean(key, fallback));
        control.setOnCheckedChangeListener((button, checked) -> preferences.edit().putBoolean(key, checked).apply());
        return control;
    }

    private void showRuntimeBlock(BedrockServer server) {
        BedrockRuntime.Report report = BedrockRuntime.inspect(server);
        new AlertDialog.Builder(this)
                .setTitle("Runtime unavailable")
                .setMessage(report.message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void editProperties(BedrockServer server) {
        ServerProperties properties;
        try {
            properties = ServerProperties.load(server.propertiesFile());
        } catch (IOException error) {
            toast("Could not read server.properties: " + error.getMessage());
            return;
        }
        String[] keys = {"server-name", "level-name", "gamemode", "difficulty", "max-players", "server-port", "transport"};
        String[] labels = {"Server name", "World name", "Game mode", "Difficulty", "Max players", "Server port", "Transport (BDS 26.60+)"};
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(4));
        EditText[] inputs = new EditText[keys.length];
        for (int i = 0; i < keys.length; i++) {
            TextView label = text(labels[i], 12, MUTED, false);
            label.setPadding(0, dp(7), 0, dp(3));
            form.addView(label);
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setTextColor(WHITE);
            input.setHintTextColor(MUTED);
            input.setTextSize(14);
            input.setHint(keys[i]);
            input.setText(properties.get(keys[i], defaultValue(keys[i])));
            form.addView(input);
            inputs[i] = input;
        }
        Switch allowList = new Switch(this);
        allowList.setText("Allow list");
        allowList.setTextColor(WHITE);
        allowList.setChecked(Boolean.parseBoolean(properties.get("allow-list", "true")));
        form.addView(allowList);
        Switch online = new Switch(this);
        online.setText("Online mode");
        online.setTextColor(WHITE);
        online.setChecked(Boolean.parseBoolean(properties.get("online-mode", "true")));
        form.addView(online);
        ScrollView wrapper = new ScrollView(this);
        wrapper.addView(form);
        new AlertDialog.Builder(this)
                .setTitle(server.name)
                .setView(wrapper)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        for (int i = 0; i < keys.length; i++) {
                            String value = inputs[i].getText().toString().trim();
                            if (!"transport".equals(keys[i]) || !value.isEmpty() || properties.asMap().containsKey(keys[i])) {
                                properties.set(keys[i], value);
                            }
                        }
                        properties.set("allow-list", Boolean.toString(allowList.isChecked()));
                        properties.set("online-mode", Boolean.toString(online.isChecked()));
                        properties.save(server.propertiesFile());
                        toast("Server properties saved");
                    } catch (IOException error) {
                        toast("Could not save server.properties: " + error.getMessage());
                    }
                }).show();
    }

    private String defaultValue(String key) {
        switch (key) {
            case "server-name": return "Dedicated Server";
            case "level-name": return "Bedrock level";
            case "gamemode": return "survival";
            case "difficulty": return "easy";
            case "max-players": return "10";
            case "server-port": return "19132";
            case "transport": return "";
            default: return "";
        }
    }

    private void chooseServerForPack(File pack) {
        List<BedrockServer> servers = repository.scanServers();
        if (servers.isEmpty()) {
            toast("Import a Bedrock server first");
            return;
        }
        String[] names = new String[servers.size()];
        for (int i = 0; i < servers.size(); i++) names[i] = servers.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle("Install into which server?")
                .setItems(names, (dialog, index) -> {
                    BedrockServer server = servers.get(index);
                    io.execute(() -> {
                        try {
                            String result = PackInstaller.install(pack, server);
                            runOnUiThread(() -> toast(result));
                        } catch (Exception error) {
                            runOnUiThread(() -> toast("Install failed: " + error.getMessage()));
                        }
                    });
                }).show();
    }

    private void pickServerFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_SERVER_FOLDER);
    }

    private void pickBuildZip() { pickArchive(PICK_BUILD_ZIP); }
    private void pickPack() { pickArchive(PICK_PACK); }

    private void pickArchive(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, data.getFlags() &
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (SecurityException ignored) {
            // Some document providers grant a transient read permission only.
        }

        if (requestCode == PICK_SERVER_FOLDER) {
            importServerFolder(uri);
        } else if (requestCode == PICK_BUILD_ZIP) {
            String name = documentName(uri, "BedrockBuild");
            if (name.toLowerCase().endsWith(".zip")) name = name.substring(0, name.length() - 4);
            importBuild(uri, name);
        } else if (requestCode == PICK_PACK) {
            String name = documentName(uri, "BedrockPack");
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
            importPack(uri, name);
        }
    }

    private void importServerFolder(Uri uri) {
        showBusy("Importing Bedrock server...");
        io.execute(() -> {
            try {
                BedrockServer server = repository.importServerFolder(uri);
                runOnUiThread(() -> {
                    hideBusy();
                    toast("Imported " + server.name);
                    selectedTab = 0;
                    render();
                });
            } catch (Exception error) {
                runOnUiThread(() -> { hideBusy(); toast("Import failed: " + error.getMessage()); });
            }
        });
    }

    private void importBuild(Uri uri, String name) {
        showBusy("Extracting Bedrock build...");
        io.execute(() -> {
            try {
                BedrockServer server = repository.importBuildZip(uri, name);
                runOnUiThread(() -> {
                    hideBusy();
                    toast("Imported " + server.name);
                    selectedTab = 0;
                    render();
                });
            } catch (Exception error) {
                runOnUiThread(() -> { hideBusy(); toast("Import failed: " + error.getMessage()); });
            }
        });
    }

    private void importPack(Uri uri, String name) {
        showBusy("Importing Bedrock pack...");
        io.execute(() -> {
            try {
                File pack = repository.importPack(uri, name);
                runOnUiThread(() -> {
                    hideBusy();
                    toast("Imported " + pack.getName());
                    selectedTab = 2;
                    render();
                });
            } catch (Exception error) {
                runOnUiThread(() -> { hideBusy(); toast("Import failed: " + error.getMessage()); });
            }
        });
    }

    private String documentName(Uri uri, String fallback) {
        DocumentFile document = DocumentFile.fromSingleUri(this, uri);
        return document == null || document.getName() == null ? fallback : document.getName();
    }

    private void showBusy(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void hideBusy() { }

    private void section(String title) {
        TextView heading = text(title, 15, MUTED, true);
        heading.setLetterSpacing(0.07f);
        heading.setPadding(0, dp(2), 0, dp(10));
        content.addView(heading);
    }

    private void body(String value) {
        TextView view = text(value, 13, MUTED, false);
        view.setLineSpacing(dp(3), 1f);
        content.addView(view);
    }

    private void cardStart() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(background(CARD, 18, BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        content.addView(card, params);
        cardStack.add(card);
    }

    private final ArrayList<LinearLayout> cardStack = new ArrayList<>();

    private void cardEnd() {
        if (!cardStack.isEmpty()) cardStack.remove(cardStack.size() - 1);
    }

    private LinearLayout currentParent() {
        return cardStack.isEmpty() ? content : cardStack.get(cardStack.size() - 1);
    }

    private void addText(String value, int size, int color, boolean bold) {
        LinearLayout parent = currentParent();
        TextView view = text(value, size, color, bold);
        view.setPadding(0, dp(2), 0, dp(3));
        parent.addView(view);
    }

    private void addGap(int height) {
        View spacer = new View(this);
        currentParent().addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void addButton(String title, boolean enabled, View.OnClickListener click) {
        Button button = button(title, enabled, false, click);
        content.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
    }

    private Button button(String title, boolean enabled, boolean outlined, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(WHITE);
        button.setMinHeight(dp(40));
        button.setMinimumHeight(dp(40));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(background(outlined ? BLACK : CARD, 12, BORDER));
        button.setEnabled(enabled);
        button.setOnClickListener(click);
        return button;
    }

    private TextView chip(String title, int color) {
        TextView view = text(title, 11, WHITE, true);
        view.setPadding(dp(10), dp(5), dp(10), dp(5));
        view.setBackground(background(color, 30, color));
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable background(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    private static final class ServerPropertiesSafe {
        static String worldName(BedrockServer server) {
            try {
                return ServerProperties.load(server.propertiesFile()).get("level-name", "Bedrock level");
            } catch (IOException ignored) {
                return "Unknown";
            }
        }
    }
}
