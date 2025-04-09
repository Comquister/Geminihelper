package com.conquister.geminiHelper.utils;

import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.conquister.geminiHelper.GeminiHelper;
import com.tcoded.folialib.FoliaLib;

public class GeminiAPIClient {
    private final GeminiHelper plugin;
    private final HttpClient client;
    private final FoliaLib foliaLib;
    private final MessageFormatter messageFormatter;

    public GeminiAPIClient(GeminiHelper plugin, String apiKey, FoliaLib foliaLib, MessageFormatter messageFormatter) {
        this.plugin = plugin;
        this.foliaLib = foliaLib;
        this.messageFormatter = messageFormatter;

        // Inicializar cliente HTTP com timeout da configuração
        FileConfiguration config = plugin.getConfig();
        int connectionTimeout = config.getInt("api.connectionTimeout", 10);

        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectionTimeout))
                .build();
    }

    public void sendRequest(Player player, String prompt) {
        // Carregar configurações atualizadas do config.yml cada vez que uma solicitação é feita
        FileConfiguration config = plugin.getConfig();
        String apiKey = config.getString("apiKey", "");

        if (apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            player.sendMessage(ChatColor.RED + "Chave de API não configurada. Configure a apiKey no arquivo config.yml.");
            return;
        }

        // Executar em uma thread separada para não bloquear o servidor
        foliaLib.getScheduler().runAsync(task -> {
            try {
                // Obter valores atualizados da configuração
                String apiBaseUrl = config.getString("api.baseUrl", "https://generativelanguage.googleapis.com/v1beta");
                String apiModel = config.getString("api.model", "gemini-2.0-flash");

                // Construir o corpo da requisição JSON usando a configuração atual
                String requestBody = createRequestBody(prompt);

                // Construir a URL completa com base nas configurações atuais
                String fullApiUrl = apiBaseUrl + "/models/" + apiModel + ":generateContent" + "?key=" + apiKey;

                // Adicionar parâmetros de URL adicionais da configuração
                if (config.contains("api.parameters")) {
                    StringBuilder urlParams = new StringBuilder();
                    for (String key : config.getConfigurationSection("api.parameters").getKeys(false)) {
                        if (!key.equals("key")) { // Evitar duplicar a chave API
                            urlParams.append("&").append(key).append("=").append(config.get("api.parameters." + key));
                        }
                    }
                    fullApiUrl += urlParams.toString();
                }

                // Construir a requisição HTTP
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(fullApiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                // Log da requisição (apenas para debug)
                plugin.getLogger().info("Enviando requisição para: " + fullApiUrl);
                // plugin.getLogger().info("Corpo da requisição: " + requestBody);

                // Enviar requisição
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Processar resposta
                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    String geminiResponse = parseGeminiResponse(responseBody);

                    // Enviar resposta ao jogador (thread principal)
                    foliaLib.getScheduler().runAtEntity(player, task1 -> {
                        messageFormatter.sendFormattedMessage(player, geminiResponse);
                    });
                } else {
                    String errorMessage = "Erro ao consultar API (código " + response.statusCode() + ")";
                    plugin.getLogger().warning(errorMessage + ": " + response.body());

                    // Enviar mensagem de erro ao jogador (thread principal)
                    foliaLib.getScheduler().runAtEntity(player, task1 -> {
                        player.sendMessage(ChatColor.RED + errorMessage);
                    });
                }

            } catch (Exception e) {
                String errorMessage = "Erro ao processar a requisição: " + e.getMessage();
                plugin.getLogger().severe(errorMessage);
                e.printStackTrace();

                // Enviar mensagem de erro ao jogador (thread principal)
                foliaLib.getScheduler().runAtEntity(player, task1 -> {
                    player.sendMessage(ChatColor.RED + "Ocorreu um erro ao processar sua pergunta. Tente novamente mais tarde.");
                });
            }
        });
    }

    private String createRequestBody(String prompt) {
        // Obter estrutura da mensagem da configuração atual
        FileConfiguration config = plugin.getConfig();
        String systemInstructionText = config.getString("api.systemInstructionText", "");

        // Usar formato personalizado da configuração, substituindo os placeholders
        String customRequestBody = config.getString("api.RequestBody", "");

        // Substituir os placeholders
        customRequestBody = customRequestBody.replace("{prompt}", escapeJsonString(prompt));

        // Substituir o placeholder de instrução se estiver presente no template
        if (customRequestBody.contains("{instriction}")) {
            customRequestBody = customRequestBody.replace("{instriction}", escapeJsonString(systemInstructionText));
        }

        return customRequestBody;
    }

    /**
     * Escape caracteres especiais em uma string para uso em JSON
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '\"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    // Caracteres Unicode que precisam de escape
                    if (ch < ' ') {
                        String hexString = Integer.toHexString(ch);
                        result.append("\\u");
                        for (int k = hexString.length(); k < 4; k++) {
                            result.append('0');
                        }
                        result.append(hexString);
                    } else {
                        result.append(ch);
                    }
                    break;
            }
        }
        return result.toString();
    }

    private String parseGeminiResponse(String responseJson) throws ParseException {
        // Verificar se há um parser personalizado na configuração
        FileConfiguration config = plugin.getConfig();
        String parserType = config.getString("api.responseParser", "default");

        if (parserType.equals("custom") && config.contains("api.customResponsePaths")) {
            try {
                // Usar caminhos JSON personalizados da configuração
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(responseJson);

                String contentPath = config.getString("api.customResponsePaths.content", "candidates.0.content.parts.0.text");
                String[] pathSegments = contentPath.split("\\.");

                Object current = jsonObject;
                for (String segment : pathSegments) {
                    if (current instanceof JSONObject) {
                        current = ((JSONObject) current).get(segment);
                    } else if (current instanceof org.json.simple.JSONArray) {
                        try {
                            int index = Integer.parseInt(segment);
                            current = ((org.json.simple.JSONArray) current).get(index);
                        } catch (NumberFormatException e) {
                            throw new ParseException(0, "Caminho de resposta inválido: " + contentPath);
                        }
                    } else {
                        throw new ParseException(0, "Caminho de resposta inválido: " + contentPath);
                    }
                }

                if (current instanceof String) {
                    return (String) current;
                } else {
                    throw new ParseException(0, "O caminho de resposta não leva a uma string: " + contentPath);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao analisar resposta com parser personalizado: " + e.getMessage());
                // Fallback para o parser padrão
            }
        }

        // Parser padrão para o formato Gemini
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(responseJson);

        try {
            // Get the candidates array
            org.json.simple.JSONArray candidates = (org.json.simple.JSONArray) jsonObject.get("candidates");

            // Get the first candidate
            JSONObject firstCandidate = (JSONObject) candidates.get(0);

            // Get the content object
            JSONObject content = (JSONObject) firstCandidate.get("content");

            // Get the parts array
            org.json.simple.JSONArray parts = (org.json.simple.JSONArray) content.get("parts");

            // Get the first part
            JSONObject firstPart = (JSONObject) parts.get(0);

            // Get the text from the first part
            return (String) firstPart.get("text");
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao analisar resposta do Gemini: " + e.getMessage());
            plugin.getLogger().warning("Resposta JSON completa: " + responseJson);

            // Retornar mensagem de erro amigável em caso de falha no parse
            return "Não foi possível processar a resposta do assistente. Por favor, tente novamente mais tarde.";
        }
    }
}