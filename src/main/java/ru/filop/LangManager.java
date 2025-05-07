package ru.filop;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LangManager {
    private final JavaPlugin plugin;
    private FileConfiguration langConfig;

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.setupLanguageFile();
    }

    private void setupLanguageFile() {
        String[] defaultLangs = new String[]{"en_us", "ru_ru"};

        for (String lang : defaultLangs) {
            File file = new File(plugin.getDataFolder(), "language/" + lang + ".yml");
            if (!file.exists()) {
                plugin.saveResource("language/" + lang + ".yml", false);
            }
        }

        String langCode = plugin.getConfig().getString("language", "ru_ru").toLowerCase();
        File langFile = new File(plugin.getDataFolder(), "language/" + langCode + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + langCode + ".yml' not found, falling back to 'en_us.yml'");
            langFile = new File(plugin.getDataFolder(), "language/en_us.yml");
        }

        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String get(String key) {
        return langConfig.getString(key, "§cMissing translation: " + key);
    }

    public void reload() {
        this.setupLanguageFile();
    }
}