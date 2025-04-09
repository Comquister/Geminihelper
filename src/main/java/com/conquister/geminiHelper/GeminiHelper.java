package com.conquister.geminiHelper;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.ChatColor;

import com.tcoded.folialib.FoliaLib;

import com.conquister.geminiHelper.commands.GeminiCommandExecutor;
import com.conquister.geminiHelper.commands.ReloadCommandExecutor;
import com.conquister.geminiHelper.utils.GeminiAPIClient;
import com.conquister.geminiHelper.utils.MessageFormatter;

public class GeminiHelper extends JavaPlugin {
    private String apiKey;
    private FoliaLib foliaLib;
    private GeminiAPIClient apiClient;
    private MessageFormatter messageFormatter;
    private boolean useFormatting;
    private boolean useEmojis;

    @Override
    public void onEnable() {
        // Inicializar FoliaLib
        foliaLib = new FoliaLib(this);

        // Carregar configuração e salvar configuração padrão
        saveDefaultConfig();
        apiKey = getConfig().getString("apiKey", "");
        useFormatting = getConfig().getBoolean("use-minimessage-formatting", true);
        useEmojis = getConfig().getBoolean("use-emojis", true);

        if (apiKey.isEmpty()) {
            getLogger().severe("API key do Gemini não configurada! Configure a chave em config.yml.");
            getLogger().severe("O plugin será desativado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializar o formatador de mensagens
        messageFormatter = new MessageFormatter(this);

        // Inicializar cliente da API
        apiClient = new GeminiAPIClient(this, apiKey, foliaLib, messageFormatter);

        // Registrar comandos
        getCommand("ajuda").setExecutor(new GeminiCommandExecutor(this, apiClient));
        getCommand("help").setExecutor(new GeminiCommandExecutor(this, apiClient));
        getCommand("gpt").setExecutor(new GeminiCommandExecutor(this, apiClient));
        getCommand("reloadconfig").setExecutor(new ReloadCommandExecutor(this));

        // Verificar dependências
        checkDependencies();

        getLogger().info("GeminiHelper ativado com sucesso! Usando o modelo gemini-2.0-flash");
    }

    @Override
    public void onDisable() {
        getLogger().info("GeminiHelper desativado.");
    }

    /**
     * Verifica as dependências do plugin
     */
    private void checkDependencies() {
        if (useFormatting && getServer().getPluginManager().getPlugin("adventure-platform") == null) {
            getLogger().warning("A formatação MiniMessage está ativada, mas o plugin adventure-platform não foi encontrado.");
            getLogger().warning("Para usar formatação avançada, instale o plugin adventure-platform ou desative a formatação em config.yml.");
            useFormatting = false;
        }
    }

    /**
     * Obtém a lista de plugins ativos no servidor
     */
    public String getServerPluginsInfo() {
        Plugin[] plugins = getServer().getPluginManager().getPlugins();
        StringBuilder pluginsInfo = new StringBuilder();

        for (Plugin plugin : plugins) {
            if (pluginsInfo.length() > 0) {
                pluginsInfo.append(", ");
            }
            pluginsInfo.append(plugin.getName()).append(" v").append(plugin.getDescription().getVersion());
        }

        return pluginsInfo.toString();
    }

    /**
     * Obtém a instância do FoliaLib
     */
    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    /**
     * Obtém a configuração do prompt
     */
    public String getPromptTemplate() {
        return getConfig().getString("prompt-template", "");
    }

    /**
     * Verifica se a formatação MiniMessage está ativada
     */
    public boolean useFormatting() {
        return useFormatting;
    }

    /**
     * Verifica se o uso de emojis está ativado
     */
    public boolean useEmojis() {
        return useEmojis;
    }

    /**
     * Obtém o prefixo do assistente
     */
    public String getAssistantPrefix() {
        return getConfig().getString("assistant-prefix", "&6[Assistente] &f");
    }
}