package com.conquister.geminiHelper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import me.clip.placeholderapi.PlaceholderAPI;

import com.conquister.geminiHelper.GeminiHelper;
import com.conquister.geminiHelper.utils.GeminiAPIClient;

public class GeminiCommandExecutor implements CommandExecutor {
    private final GeminiHelper plugin;
    private final GeminiAPIClient apiClient;

    public GeminiCommandExecutor(GeminiHelper plugin, GeminiAPIClient apiClient) {
        this.plugin = plugin;
        this.apiClient = apiClient;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("geminihelper.use")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Por favor, forneça uma pergunta após o comando.");
            player.sendMessage(ChatColor.YELLOW + "Exemplo: /" + label + " como encontrar diamantes?");
            return true;
        }

        // Juntar argumentos para formar a pergunta
        StringBuilder question = new StringBuilder();
        for (String arg : args) {
            question.append(arg).append(" ");
        }

        player.sendActionBar(ChatColor.GREEN + "Enviando sua pergunta para o assistente...");

        // Preparar os dados do contexto
        String contextData = prepareContextData(player, question.toString().trim());

        // Fazer requisição assíncrona para a API do Gemini
        apiClient.sendRequest(player, contextData);

        return true;
    }

    private String prepareContextData(Player player, String question) {
        // Obter coordenadas do jogador
        Location loc = player.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        // Obter plugins do servidor
        String pluginsInfo = plugin.getServerPluginsInfo();

        // Obter informações ambientais
        String environmentInfo = getPlayerEnvironmentInfo(player);

        // Verificar se o jogador está em uma caverna
        boolean isInCave = y < 60 && player.getLocation().getBlock().getLightLevel() < 8;

        // Obter o template do prompt da configuração
        String promptTemplate = plugin.getPromptTemplate();

        // Substituir placeholders no template
        String placeholderedPrompt = promptTemplate;

        // Verificar se o PlaceholderAPI está disponível
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderedPrompt = PlaceholderAPI.setPlaceholders(player, promptTemplate);
        }

        // Substituir placeholders específicos do plugin
        placeholderedPrompt = placeholderedPrompt
                .replace("%player_question%", question)
                .replace("%player_x%", String.valueOf(x))
                .replace("%player_y%", String.valueOf(y))
                .replace("%player_z%", String.valueOf(z))
                .replace("%player_environment%", environmentInfo)
                .replace("%player_location_type%", isInCave ? "em caverna" : "na superfície")
                .replace("%server_plugins%", pluginsInfo);

        return placeholderedPrompt;
    }

    /**
     * Obtém informações do mundo e bioma onde o jogador está
     */
    private String getPlayerEnvironmentInfo(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        StringBuilder info = new StringBuilder();
        info.append("Mundo: ").append(world.getName());
        info.append(", Bioma: ").append(player.getLocation().getBlock().getBiome());
        info.append(", Tempo: ").append(world.getTime());

        // Verifica se é dia ou noite
        if (world.getTime() >= 0 && world.getTime() < 12000) {
            info.append(" (dia)");
        } else {
            info.append(" (noite)");
        }

        if (world.hasStorm()) {
            info.append(", clima: chuvoso");
        } else {
            info.append(", clima: limpo");
        }

        return info.toString();
    }
}