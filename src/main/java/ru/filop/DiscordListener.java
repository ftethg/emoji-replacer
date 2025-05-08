package ru.filop;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordListener {

    private final EmojiReplacer plugin;

    public DiscordListener(EmojiReplacer plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onDiscordMessage(DiscordGuildMessagePreProcessEvent event) {
        String originalMessage = event.getMessage().getContentDisplay();
        plugin.getLogger().info("Original Discord Message: " + originalMessage);

        String replacedMessage = replaceEmojis(originalMessage);
        plugin.getLogger().info("Replaced Discord Message: " + replacedMessage);
        String format = plugin.getConfig().getString("discord-format", "[Discord] %username% > %message%");
        String finalMessage = format
                .replace("%username%", event.getAuthor().getName())
                .replace("%message%", replacedMessage);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(finalMessage);
        }
        Bukkit.getConsoleSender().sendMessage(finalMessage);
        event.setCancelled(true);
    }

    private String replaceEmojis(String message) {
        Pattern pattern = Pattern.compile(":(\\w+):");
        Matcher matcher = pattern.matcher(message);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String emoji = matcher.group(1);
            String minecraftEmoji = plugin.getEmojiMap().get(emoji); 
            if (minecraftEmoji != null) {
                matcher.appendReplacement(result, minecraftEmoji);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }
}










