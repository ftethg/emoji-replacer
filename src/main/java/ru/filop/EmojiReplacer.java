package ru.filop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import github.scarsz.discordsrv.DiscordSRV;

public class EmojiReplacer extends JavaPlugin implements Listener {
    private Map<String, String> emojiMap = new HashMap<>();
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":(\\w+):");
    private File emojiFile;
    private FileConfiguration emojiConfig;
    private FileConfiguration langConfig;
    private LangManager langManager;

    public EmojiReplacer() {}

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.createEmojiFile();
        this.langManager = new LangManager(this);
        this.langManager.reload();
        this.loadEmojiMap();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getLogger().info("EmojiReplacer enabled.");
        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            DiscordSRV.api.subscribe(new DiscordListener(this));
        }
    }

    public void onDisable() {
        this.getLogger().info("EmojiReplacer disabled.");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        message = ColorUtil.color(message);
        Matcher matcher = EMOJI_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while(matcher.find()) {
            String key = matcher.group(1);
            String replacement = (String)this.emojiMap.get(key);
            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }

        matcher.appendTail(result);
        event.setMessage(result.toString());
    }

    public String replaceEmojis(String message) {
        Matcher matcher = EMOJI_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = emojiMap.get(key);
            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("EmojiReplacer")) {
            return false;
        } else if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.color(langManager.get("only-players")));
            return true;
        } else {
            Player player = (Player)sender;
            if (args.length == 0) {
                player.sendMessage(ColorUtil.color(langManager.get("usage")));
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("emojireplacer.reload")) {
                    player.sendMessage(ColorUtil.color(langManager.get("no-permission")));
                    return true;
                } else {
                    this.reloadConfig();
                    this.reloadEmojiConfig();
                    this.langManager.reload();
                    this.loadEmojiMap();
                    player.sendMessage(ColorUtil.color(langManager.get("reload-success")));
                    return true;
                }
            } else if (!args[0].equalsIgnoreCase("list")) {
                return false;
            } else if (!player.hasPermission("emojireplacer.list")) {
                player.sendMessage(ColorUtil.color(langManager.get("no-permission")));
                return true;
            } else if (!this.getConfig().getBoolean("list-enabled", true)) {
                player.sendMessage(ColorUtil.color(langManager.get("list-disabled")));
                return true;
            } else {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException var13) {
                    }
                }

                List<String> lines = new ArrayList();

                for(String emoji : this.emojiConfig.getKeys(false)) {
                    List<String> aliases = this.emojiConfig.getStringList(emoji);
                    lines.add(ColorUtil.color(langManager.get("list-line").replace("{emoji}", emoji).replace("{aliases}", String.join(":/:", aliases))));
                }

                int linesPerPage = this.getConfig().getInt("list-page-size", 5);
                int totalPages = (int)Math.ceil((double)lines.size() / (double)linesPerPage);
                page = Math.max(1, Math.min(page, totalPages));
                player.sendMessage(ColorUtil.color(langManager.get("list-header").replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(totalPages))));
                int start = (page - 1) * linesPerPage;
                int end = Math.min(start + linesPerPage, lines.size());

                for(int i = start; i < end; ++i) {
                    player.sendMessage((String)lines.get(i));
                }

                if (totalPages > 1) {
                    Component prevNext = Component.text(ColorUtil.color(langManager.get("prev")));
                    if (page > 1) {
                        prevNext = prevNext.append(((TextComponent)Component.text(ColorUtil.color(langManager.get("prev-page"))).clickEvent(ClickEvent.runCommand("/emojireplacer list " + (page - 1)))).hoverEvent(Component.text(ColorUtil.color(langManager.get("prev-page-hover")))));
                    } else {
                        prevNext = prevNext.append(Component.text(ColorUtil.color(langManager.get("prev-page-"))));
                    }

                    prevNext = prevNext.append(Component.text(ColorUtil.color(langManager.get("prev-next"))));
                    if (page < totalPages) {
                        prevNext = prevNext.append(((TextComponent)Component.text(ColorUtil.color(langManager.get("next-page"))).clickEvent(ClickEvent.runCommand("/emojireplacer list " + (page + 1)))).hoverEvent(Component.text(ColorUtil.color(langManager.get("next-page-hover")))));
                    } else {
                        prevNext = prevNext.append(Component.text(ColorUtil.color(langManager.get("next-page+"))));
                    }

                    prevNext = prevNext.append(Component.text(ColorUtil.color(langManager.get("next"))));
                    player.sendMessage(prevNext);
                }

                return true;
            }
        }
    }
    public Map<String, String> getEmojiMap() {
        return emojiMap;
    }
    private void loadEmojiMap() {
        this.emojiMap.clear();

        for(String emoji : this.emojiConfig.getKeys(false)) {
            for(String k : this.emojiConfig.getStringList(emoji)) {
                this.emojiMap.put(k, emoji);
            }
        }

    }

    private void createEmojiFile() {
        this.emojiFile = new File(this.getDataFolder(), "emojis.yml");
        if (!this.emojiFile.exists()) {
            this.emojiFile.getParentFile().mkdirs();
            this.saveResource("emojis.yml", false);
        }

        this.emojiConfig = YamlConfiguration.loadConfiguration(this.emojiFile);
    }

    private void reloadEmojiConfig() {
        this.emojiConfig = YamlConfiguration.loadConfiguration(this.emojiFile);
    }

}


