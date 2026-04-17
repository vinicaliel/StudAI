package studAI.AI.services.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class OpenAiService {

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public JsonNode generateSummary(String extractedText) throws Exception {

        // ===============================
        // 🔒 VALIDAÇÃO E LIMITE
        // ===============================
        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException("Texto extraído está vazio.");
        }

        int MAX_LENGTH = 15000;
        if (extractedText.length() > MAX_LENGTH) {
            extractedText = extractedText.substring(0, MAX_LENGTH);
        }

        // ===============================
        // 🧪 MOCK (SEM API KEY)
        // ===============================
        if (apiKey == null || apiKey.contains("cole_sua_chave") ||
                apiKey.trim().isEmpty() || apiKey.contains("${OPENAI")) {

            System.out.println("⚠️ Simulando resposta da OpenAI ⚠️");

            String mockJson = """
            {
              "title": "Análise Simulada do Documento",
              "pages": [
                {
                  "page": 1,
                  "topics": ["Cabeamento estruturado", "Normas técnicas"],
                  "detailedExplanation": "O documento apresenta conceitos fundamentais sobre cabeamento estruturado e normas aplicáveis em ambientes críticos.",
                  "importantConcepts": ["NBR 14565", "EIA/TIA 569-A"],
                  "conceptsExplanation": "Essas normas garantem padronização e qualidade na implementação de redes."
                }
              ],
              "finalSummary": "O material aborda fundamentos de infraestrutura de redes e energia em ambientes como data centers.",
              "simplifiedExplanation": "Basicamente, o conteúdo explica como organizar redes e energia de forma segura e padronizada."
            }
            """;

            return objectMapper.readTree(mockJson);
        }

        // ===============================
        // 🧠 PROMPT
        // ===============================
        String prompt = """
        Você é um professor universitário altamente técnico e detalhista.

        Analise o conteúdo fornecido como material acadêmico.

        Sua tarefa NÃO é resumir superficialmente.
        Sua tarefa é ANALISAR e EXPLICAR profundamente.

        Retorne um JSON com a seguinte estrutura:

        {
          "title": "...",
          "pages": [
            {
              "page": 1,
              "topics": ["..."],
              "detailedExplanation": "...",
              "importantConcepts": ["..."],
              "conceptsExplanation": "..."
            }
          ],
          "finalSummary": "...",
          "simplifiedExplanation": "..."
        }

        REGRAS:
        - Analise como conteúdo universitário
        - NÃO simplifique demais
        - NÃO responda superficialmente
        - Explique cada conceito com profundidade
        - Identifique tópicos relevantes
        - Seja técnico, mas claro

        IMPORTANTE:
        - Retorne apenas JSON válido
        - Não use markdown

        Conteúdo:
        """ + extractedText;

        // ===============================
        // 📡 REQUEST JSON (CORRIGIDO)
        // ===============================
        ObjectNode requestJson = objectMapper.createObjectNode();

        requestJson.put("model", "gpt-4o-mini");
        requestJson.put("temperature", 0.7);
        requestJson.put("max_tokens", 4000);

        // messages
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);

        messages.add(message);
        requestJson.set("messages", messages);

        // response_format
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_object");

        requestJson.set("response_format", responseFormat);

        String requestBody = objectMapper.writeValueAsString(requestJson);

        // ===============================
        // 🌐 REQUEST HTTP
        // ===============================
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode rootNode = objectMapper.readTree(response.body());

        // ===============================
        // ❌ TRATAMENTO DE ERRO
        // ===============================
        if (rootNode.has("error")) {
            throw new RuntimeException("Erro da OpenAI: " +
                    rootNode.get("error").get("message").asText());
        }

        // ===============================
        // 📦 EXTRAÇÃO DO RESULTADO
        // ===============================
        String content = rootNode
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        return objectMapper.readTree(content);
    }
}