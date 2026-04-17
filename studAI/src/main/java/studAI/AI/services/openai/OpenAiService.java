package studAI.AI.services.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class OpenAiService {

    // Puxa sua chave secreta do .env!
    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public JsonNode generateSummary(String extractedText) throws Exception {
        // Regra de Ouro: Forçamos a IA a não conversar, apenas cuspir o JSON estruturado!
        String prompt = "Você é um assistente acadêmico. Leia este texto e retorne APENAS um JSON estrito (sem crases de markdown) contendo as seguintes chaves: " +
                "\"title\" (String), " +
                "\"shortSummary\" (String), " +
                "\"mainTopics\" (String), " +
                "\"importantPoints\" (String), " +
                "\"simplifiedExplanation\" (String).\n\nTexto: " + extractedText;

        String requestBody = """
            {
                "model": "gpt-3.5-turbo",
                "messages": [{"role": "user", "content": %s}],
                "temperature": 0.5
            }
            """.formatted(objectMapper.writeValueAsString(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode rootNode = objectMapper.readTree(response.body());

        if (rootNode.has("error")) {
            throw new RuntimeException("Erro da OpenAI: " + rootNode.get("error").get("message").asText());
        }

        // Entra no retorno da IA e transforma o texto dela em um Objeto JSON
        String content = rootNode.path("choices").get(0).path("message").path("content").asText();
        return objectMapper.readTree(content); 
    }
}
