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
              "title": "Infraestrutura de Data Center: Cabeamento, Normas e Energia",
              "shortSummary": "O material analisa os subsistemas de cabeamento estruturado e sua relacao com confiabilidade operacional em data centers. Tambem discute normas tecnicas, criterios de implantacao e calculos eletricos basicos para dimensionamento. A parte final detalha estrategias de redundancia energetica para continuidade de servico.",
              "mainTopics": "- Pagina 1: Fundamentos de sistemas estruturados e funcao de cada subsistema.\\n- Pagina 2: Normas de padronizacao e criterios de conformidade tecnica.\\n- Pagina 3: Calculos basicos de sistemas eletricos para cargas e distribuicao.\\n- Pagina 4: Arquiteturas de energia redundante e analise de disponibilidade.",
              "importantPoints": "- Cabeamento horizontal e vertical cumprem papeis distintos na topologia e impactam manutencao.\\n- Normas como NBR 14565 e EIA/TIA orientam interoperabilidade, seguranca e expansao futura.\\n- Dimensionamento eletrico inadequado aumenta risco de falhas e indisponibilidade.\\n- Redundancia N+1 e 2N reduz ponto unico de falha em ambientes criticos.",
              "simplifiedExplanation": "Pense no data center como um organismo: o cabeamento e o sistema nervoso, as normas sao as regras que garantem que tudo converse corretamente, e a energia redundante e o plano de contingencia. O texto mostra que desempenho e confiabilidade nao dependem de um unico elemento, mas da integracao entre projeto fisico, padroes tecnicos e estrategia energetica."
            }
            """;

            return objectMapper.readTree(mockJson);
        }

        // ===============================
        // 🧠 PROMPT
        // ===============================
        String prompt = """
        Você é um professor universitário técnico e didático.
        Analise profundamente o material acadêmico abaixo.

        OBJETIVO:
        - produzir um resumo TRABALHADO (não superficial)
        - explicar os conceitos importantes com contexto e impacto prático
        - analisar por página quando o conteúdo tiver marcadores de página

        RETORNE APENAS JSON VÁLIDO neste formato EXATO:
        {
          "title": "string",
          "shortSummary": "string",
          "mainTopics": "string",
          "importantPoints": "string",
          "simplifiedExplanation": "string"
        }

        REGRAS DE QUALIDADE:
        - title: título técnico e específico do material
        - shortSummary: entre 4 e 7 frases, em tom didático de preparação para prova, sem generalidades vagas
        - shortSummary deve ser encorpado (aprox. 180 a 260 palavras), com visão geral + objetivos de estudo
        - mainTopics: OBRIGATORIAMENTE com explicação por página, no formato:
          "Página X: tópico A, tópico B, tópico C"
          "Explicação: análise técnica do que esses tópicos significam, como se conectam e por que importam."
          (duas linhas por página, separadas por quebra de linha)
        - Para cada página, a explicação deve ter no mínimo 4 frases
        - Em cada explicação de página, inclua obrigatoriamente:
          1) importância acadêmica/técnica do assunto
          2) usabilidade/aplicação nos dias atuais
          3) impacto prático quando bem aplicado
          4) risco ou limitação quando ignorado
        - Quando houver fórmulas no conteúdo, inclua por página:
          a) a fórmula
          b) o significado de cada variável/símbolo
          c) a função da fórmula (para que ela serve no contexto)
          d) 1 exemplo numérico curto de aplicação com resultado
        - Se não houver fórmulas explícitas, use pelo menos 1 exemplo prático aplicado ao contexto da página
        - Evite descrições genéricas; use análise contextual e objetiva
        - importantPoints: liste entre 8 e 15 pontos importantes com:
          conceito + por que importa + consequência prática + dica de estudo para prova
          (uma linha por ponto, com "- ")
        - importantPoints deve conter no minimo 12 pontos
        - simplifiedExplanation: explicação detalhada em linguagem clara,
          conectando os tópicos, mostrando aplicações reais e incluindo
          3 a 5 miniquestões de revisão (pergunta e resposta curta)
        - simplifiedExplanation deve ser longo e encorpado (aprox. 350 a 700 palavras),
          funcionando como texto de revisão final para prova
        - não deixe campos vazios
        - não use markdown fora das quebras de linha e prefixo "- "

        LOGICA DIDATICA ADICIONAL (SEM ALTERAR O FORMATO FINAL EXIGIDO):
        - Aja como professor universitario especialista em preparacao para provas e concursos.
        - Estruture mentalmente a resposta como um material completo de estudo:
          introducao, topicos organizados, pontos importantes, exemplos, calculos (quando houver),
          questao estilo prova e resumo final com dicas de estudo.
        - Como o formato de saida deve permanecer com os 5 campos atuais, mapeie assim:
          1) title -> titulo tecnico do material
          2) shortSummary -> introducao didatica + objetivo de aprendizagem
          3) mainTopics -> topicos organizados por pagina com explicacao aprofundada,
             exemplos de uso atual e, quando houver, formulas com variaveis e exemplo resolvido
          4) importantPoints -> pontos criticos para prova + erros comuns + dicas praticas
          5) simplifiedExplanation -> sintese final robusta + miniquestoes estilo prova
             (pergunta, resposta e justificativa curta) + estrategias de revisao
        - Em calculos/formulas, sempre informar:
          formula, significado de cada termo, funcao da formula no contexto e exemplo aplicado.
        - Evite repeticao vazia; cada pagina deve agregar aprendizado novo e util para estudo.
        - Crie explicitamente um topico "Formulas Utilizadas" dentro de mainTopics:
          liste apenas formulas que aparecem ou sao claramente derivadas do documento;
          para cada formula, explique variaveis, finalidade e quando aplicar.
        - Crie explicitamente um topico "Exemplos de Questoes" dentro de mainTopics:
          inclua pelo menos 5 questoes estilo prova com resposta curta e justificativa objetiva.
        - PERMITIDO e RECOMENDADO usar conteudos do proprio PDF para enriquecer a didatica:
          trechos relevantes, termos tecnicos originais, exemplos descritos no material e
          dados numericos presentes no texto.
        - Quando citar conteudo do PDF, integre esse conteudo com explicacao (nao apenas copie),
          mostrando o que significa e por que aquilo e importante para prova e aplicacao pratica.
        - Em exemplos de calculo, priorize valores e cenarios do proprio PDF quando disponiveis;
          se nao houver valores explicitos, crie um exemplo coerente e informe que e ilustrativo.
        - Se o documento nao trouxer formulas, no topico "Formulas Utilizadas" escreva
          "Nenhuma formula explicita identificada no documento" e inclua no minimo 1 formula
          basica relacionada ao tema apenas como reforco didatico, sinalizando que e complementar.

        IMPORTANTE:
        - retorne somente o objeto JSON
        - não inclua texto adicional antes/depois

        Conteúdo:
        """ + extractedText;

        // ===============================
        // 📡 REQUEST JSON (CORRIGIDO)
        // ===============================
        ObjectNode requestJson = objectMapper.createObjectNode();

        requestJson.put("model", "gpt-4o-mini");
        requestJson.put("temperature", 0.9);
        requestJson.put("max_tokens", 7000);

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