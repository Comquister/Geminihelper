package com.conquister.geminiHelper.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import com.conquister.geminiHelper.GeminiHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFormatter {
    private final GeminiHelper plugin;
    private final Pattern minimessagePattern = Pattern.compile("<([^>]+)>");
    private final Pattern emojiPattern = Pattern.compile(":([a-zA-Z0-9_]+):");

    // Mapa de emojis comuns
    private final Map<String, String> emojiMap = new HashMap<>();

    public MessageFormatter(GeminiHelper plugin) {
        this.plugin = plugin;
        initEmojiMap();
    }

    /**
     * Inicializa o mapa de emojis
     */
    private void initEmojiMap() {
        // Minecraft-friendly emojis (usando símbolos que funcionam no Minecraft)
        emojiMap.put("smile", "☺");
        emojiMap.put("heart", "❤");
        emojiMap.put("check", "✓");
        emojiMap.put("x", "✗");
        emojiMap.put("star", "★");
        emojiMap.put("arrow", "➤");
        emojiMap.put("diamond", "◆");
        emojiMap.put("sword", "⚔");
        emojiMap.put("pickaxe", "⛏");
        emojiMap.put("sun", "☀");
        emojiMap.put("moon", "☽");
        emojiMap.put("lightning", "⚡");
        emojiMap.put("fire", "🔥");
        emojiMap.put("water", "💧");
        emojiMap.put("creeper", "👾");
        emojiMap.put("craft", "⚒");
        emojiMap.put("crown", "👑");
        emojiMap.put("skull", "☠");
        emojiMap.put("potion", "⚗");
        emojiMap.put("warning", "⚠");
        emojiMap.put("info", "ℹ");
    }

    /**
     * Formata uma mensagem com cores e emojis
     */
    public String formatMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String formatted = message;

        // Processar formatação MiniMessage se estiver ativada
        if (plugin.useFormatting()) {
            formatted = processMiniMessage(formatted);
        } else {
            // Se não estiver usando MiniMessage, converter cores legadas
            formatted = ChatColor.translateAlternateColorCodes('&', formatted);
        }

        // Processar emojis se estiverem ativados
        if (plugin.useEmojis()) {
            formatted = processEmojis(formatted);
        }

        return formatted;
    }

    /**
     * Processa a formatação MiniMessage na mensagem
     */
    private String processMiniMessage(String message) {
        // Para simplificar, convertemos apenas algumas tags MiniMessage básicas para códigos de cores legados
        // Em uma implementação completa, você usaria a biblioteca Adventure

        String result = message;

        // Substituir tags de cores
        result = result.replaceAll("<red>", ChatColor.RED.toString());
        result = result.replaceAll("<green>", ChatColor.GREEN.toString());
        result = result.replaceAll("<blue>", ChatColor.BLUE.toString());
        result = result.replaceAll("<yellow>", ChatColor.YELLOW.toString());
        result = result.replaceAll("<gold>", ChatColor.GOLD.toString());
        result = result.replaceAll("<aqua>", ChatColor.AQUA.toString());
        result = result.replaceAll("<gray>", ChatColor.GRAY.toString());
        result = result.replaceAll("<white>", ChatColor.WHITE.toString());

        // Substituir tags de formatação
        result = result.replaceAll("<bold>", ChatColor.BOLD.toString());
        result = result.replaceAll("<italic>", ChatColor.ITALIC.toString());
        result = result.replaceAll("<underline>", ChatColor.UNDERLINE.toString());
        result = result.replaceAll("<strike>", ChatColor.STRIKETHROUGH.toString());

        // Resetar formatação
        result = result.replaceAll("<reset>", ChatColor.RESET.toString());

        // Converter & para código de cor legado
        result = ChatColor.translateAlternateColorCodes('&', result);

        return result;
    }

    /**
     * Processa emojis na mensagem
     */
    private String processEmojis(String message) {
        if (!plugin.useEmojis()) {
            return message;
        }

        StringBuffer sb = new StringBuffer();
        Matcher matcher = emojiPattern.matcher(message);

        while (matcher.find()) {
            String emojiName = matcher.group(1).toLowerCase();
            String emoji = emojiMap.getOrDefault(emojiName, ":" + emojiName + ":");
            matcher.appendReplacement(sb, emoji);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Envia uma mensagem formatada para um jogador
     */
    public void sendFormattedMessage(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        // Aplicar prefixo do assistente
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getAssistantPrefix());
        String fullMessage = prefix + formatMessage(message);

        player.sendMessage(fullMessage);
    }
}