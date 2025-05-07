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

        // Печатаем исходное сообщение в консоль для отладки
        plugin.getLogger().info("Original Discord Message: " + originalMessage);

        // Заменяем все эмодзи с помощью метода replaceEmojis
        String replacedMessage = replaceEmojis(originalMessage);

        // Печатаем сообщение после замены эмодзи
        plugin.getLogger().info("Replaced Discord Message: " + replacedMessage);

        // Формируем финальное сообщение с заменой
        String format = plugin.getConfig().getString("discord-format", "[Discord] %username% > %message%");
        String finalMessage = format
                .replace("%username%", event.getAuthor().getName())
                .replace("%message%", replacedMessage);

        // Отправляем сообщение всем игрокам в Minecraft
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(finalMessage);
        }

        // Также выводим в консоль
        Bukkit.getConsoleSender().sendMessage(finalMessage);

        // Останавливаем обработку сообщения Discord, так как оно уже отправлено в Minecraft
        event.setCancelled(true);
    }

    // Метод для замены эмодзи в сообщении
    private String replaceEmojis(String message) {
        // Используем шаблон для поиска всех эмодзи (например, :smile:)
        Pattern pattern = Pattern.compile(":(\\w+):");  // Например, :smile:
        Matcher matcher = pattern.matcher(message);

        StringBuffer result = new StringBuffer();

        // Ищем и заменяем эмодзи
        while (matcher.find()) {
            String emoji = matcher.group(1);
            String minecraftEmoji = plugin.getEmojiMap().get(emoji); // Получаем Minecraft-эквивалент эмодзи
            if (minecraftEmoji != null) {
                matcher.appendReplacement(result, minecraftEmoji);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }
}










