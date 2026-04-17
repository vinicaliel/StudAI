package studAI.AI.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import studAI.AI.models.Material;
import studAI.AI.models.Summary;
import studAI.AI.repository.MaterialRepository;
import studAI.AI.repository.SummaryRepository;
import studAI.AI.repository.UserRepository;
import studAI.AI.services.aws.S3Service; // Ajuste este import caso salvou o s3 service em outro pacote
import studAI.AI.services.openai.OpenAiService;
import studAI.AI.user.User;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class SummaryService {

    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;
    private final SummaryRepository summaryRepository;
    private final S3Service s3Service;
    private final OpenAiService openAiService;

    public SummaryService(UserRepository userRepository, MaterialRepository materialRepository, 
                          SummaryRepository summaryRepository, S3Service s3Service, 
                          OpenAiService openAiService) {
        this.userRepository = userRepository;
        this.materialRepository = materialRepository;
        this.summaryRepository = summaryRepository;
        this.s3Service = s3Service;
        this.openAiService = openAiService;
    }

    public Summary processPdfAndSummarize(MultipartFile file, String userEmail) throws Exception {
        
        // 1. CHECAGEM DE LIMITES DO MVP (A Mágica da Monetização)
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (user.getPlanType().equals("FREE") && user.getMonthlyUsageCount() >= 10) {
            throw new RuntimeException("Você atingiu o limite de 10 resumos gratuitos neste mês! Faça o Upgrade.");
        }

        // 2. Extrai o texto do PDF
        String extractedText;
        int totalPages;
        try (InputStream is = file.getInputStream(); PDDocument document = PDDocument.load(is)) {
            totalPages = document.getNumberOfPages();
            extractedText = extractTextWithPageMarkers(document, 10);
        }

        // Limita o tamanho do texto para economizar dinheiro com a OpenAI (Ex: Corta nas 15 mil letras)
        if (extractedText.length() > 15000) {
            extractedText = extractedText.substring(0, 15000);
        }

        // 3. Faz o Backup do PDF pro nosso MinIO
        String s3Key = s3Service.uploadFile(file, user.getId().toString());

        // 4. Salva o histórico no Banco de Dados
        Material material = new Material();
        material.setUser(user);
        material.setFileName(file.getOriginalFilename());
        material.setS3Key(s3Key);
        material.setTotalPages(totalPages);
        material.setStatus("COMPLETED");
        materialRepository.save(material);

        // 5. Manda para a OpenAI e aguarda a resposta
        JsonNode iaResponse = openAiService.generateSummary(extractedText);

        // 6. COBRA 1 USO DA COTA DO USUÁRIO!
        user.setMonthlyUsageCount(user.getMonthlyUsageCount() + 1);
        userRepository.save(user);

        // 7. Salva o super resumo no banco
        Summary summary = new Summary();
        summary.setMaterial(material);
        summary.setTitle(getAsTextOrDefault(iaResponse, "title", "Resumo do Material"));
        summary.setShortSummary(resolveShortSummary(iaResponse));
        summary.setMainTopics(resolveMainTopics(iaResponse));
        summary.setImportantPoints(resolveImportantPoints(iaResponse, summary.getMainTopics()));
        summary.setSimplifiedExplanation(resolveSimplifiedExplanation(iaResponse, summary.getMainTopics()));
        summary.setIntroduction(resolveIntroduction(iaResponse, summary.getShortSummary()));
        summary.setFormulasUsed(resolveFormulasUsed(iaResponse, summary.getMainTopics()));
        summary.setExamQuestions(resolveExamQuestions(iaResponse));
        summary.setStudyTips(resolveStudyTips(iaResponse));
        summary.setFinalSummary(resolveFinalSummary(iaResponse, summary.getSimplifiedExplanation()));
        summary.setPageAnalysisJson(resolvePageAnalysisJson(iaResponse));
        summary.setModelUsed("gpt-4o-mini");
        summary.setPromptVersion("v2-didatic-summary");
        summary.setWordCount(calculateWordCount(summary));
        summary.setQualityScore(calculateQualityScore(summary));
        
        return summaryRepository.save(summary);
    }

    private String extractTextWithPageMarkers(PDDocument document, int maxPages) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        int pagesToProcess = Math.min(document.getNumberOfPages(), maxPages);
        StringBuilder builder = new StringBuilder();

        for (int page = 1; page <= pagesToProcess; page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            String pageText = stripper.getText(document).trim();
            if (!pageText.isBlank()) {
                builder.append("### PAGINA ").append(page).append('\n');
                builder.append(pageText).append("\n\n");
            }
        }

        return builder.toString().trim();
    }

    private String getAsTextOrDefault(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("").trim();
        return value.isEmpty() ? fallback : value;
    }

    private String resolveShortSummary(JsonNode iaResponse) {
        String shortSummary = iaResponse.path("shortSummary").asText("").trim();
        if (!shortSummary.isEmpty()) {
            return shortSummary;
        }
        String finalSummary = iaResponse.path("finalSummary").asText("").trim();
        return finalSummary.isEmpty() ? "Resumo indisponivel para este material." : finalSummary;
    }

    private String resolveSimplifiedExplanation(JsonNode iaResponse, String mainTopics) {
        String simplifiedExplanation = iaResponse.path("simplifiedExplanation").asText("").trim();
        if (isUsefulText(simplifiedExplanation)) {
            return simplifiedExplanation;
        }
        if (isUsefulText(mainTopics)) {
            return "Explicacao geral baseada nos topicos identificados:\n" + mainTopics;
        }
        return "Explicacao detalhada indisponivel para este material.";
    }

    private String resolveIntroduction(JsonNode iaResponse, String shortSummary) {
        String introduction = iaResponse.path("introduction").asText("").trim();
        if (isUsefulText(introduction)) {
            return introduction;
        }
        return shortSummary;
    }

    private String resolveFormulasUsed(JsonNode iaResponse, String mainTopics) {
        String formulasUsed = iaResponse.path("formulasUsed").asText("").trim();
        if (isUsefulText(formulasUsed)) {
            return formulasUsed;
        }

        String normalizedTopics = mainTopics == null ? "" : mainTopics.toLowerCase();
        if (normalizedTopics.contains("formula") || normalizedTopics.contains("potencia")
                || normalizedTopics.contains("cálculo") || normalizedTopics.contains("calculo")) {
            return """
                    - Formula identificada no material: revisar variaveis, unidade de medida e contexto de aplicacao.
                    - Exemplo orientado: substituir valores, calcular passo a passo e validar unidade final.
                    """.trim();
        }

        return "Nenhuma formula explicita identificada no documento.";
    }

    private String resolveExamQuestions(JsonNode iaResponse) {
        String examQuestions = iaResponse.path("examQuestions").asText("").trim();
        if (isUsefulText(examQuestions)) {
            return examQuestions;
        }

        return """
                1) Questao: Qual e o conceito central do material e por que ele e cobrado em prova?
                Resposta: O conceito central envolve a relacao entre fundamentos teoricos e aplicacao pratica.
                Justificativa: Bancas costumam avaliar entendimento conceitual com impacto real de uso.

                2) Questao: Cite um ponto critico do tema e a consequencia de ignora-lo.
                Resposta: Ignorar os fundamentos tecnicos leva a erros de interpretacao e aplicacao.
                Justificativa: A prova exige capacidade de identificar risco, causa e efeito.

                3) Questao: Como aplicar o conteudo em um cenario atual?
                Resposta: Mapear o problema, selecionar o conceito correto e justificar a decisao.
                Justificativa: Questoes modernas cobram transferencia de conhecimento para casos praticos.
                """.trim();
    }

    private String resolveStudyTips(JsonNode iaResponse) {
        String studyTips = iaResponse.path("studyTips").asText("").trim();
        if (isUsefulText(studyTips)) {
            return studyTips;
        }
        return """
                - Estude por blocos: teoria, aplicacao, erros comuns e revisao.
                - Transforme cada ponto importante em pergunta de prova objetiva e discursiva.
                - Faca revisao ativa 24h depois para consolidar memoria de longo prazo.
                - Resolva exemplos e simule explicacao oral para fixar entendimento.
                """.trim();
    }

    private String resolveFinalSummary(JsonNode iaResponse, String simplifiedExplanation) {
        String finalSummary = iaResponse.path("finalSummary").asText("").trim();
        if (isUsefulText(finalSummary)) {
            return finalSummary;
        }
        return simplifiedExplanation;
    }

    private String resolvePageAnalysisJson(JsonNode iaResponse) {
        String pageAnalysisJson = iaResponse.path("pageAnalysisJson").asText("").trim();
        if (isUsefulText(pageAnalysisJson)) {
            return pageAnalysisJson;
        }

        JsonNode pages = iaResponse.path("pages");
        if (!pages.isMissingNode() && !pages.isNull() && pages.isArray()) {
            return pages.toString();
        }
        return "[]";
    }

    private String resolveMainTopics(JsonNode iaResponse) {
        String mainTopics = iaResponse.path("mainTopics").asText("").trim();
        if (!mainTopics.isEmpty()) {
            return mainTopics;
        }

        JsonNode pages = iaResponse.path("pages");
        if (!pages.isArray() || pages.isEmpty()) {
            return "Topicos principais nao identificados.";
        }

        List<String> lines = new ArrayList<>();
        for (JsonNode pageNode : pages) {
            int page = pageNode.path("page").asInt();
            JsonNode topics = pageNode.path("topics");
            String detailedExplanation = pageNode.path("detailedExplanation").asText("").trim();
            if (topics.isArray() && !topics.isEmpty()) {
                List<String> topicList = new ArrayList<>();
                for (JsonNode topic : topics) {
                    String topicText = topic.asText("").trim();
                    if (!topicText.isEmpty()) {
                        topicList.add(topicText);
                    }
                }
                if (!topicList.isEmpty()) {
                    lines.add("Pagina " + page + ": " + String.join(", ", topicList));
                    if (!detailedExplanation.isEmpty()) {
                        lines.add("Explicacao: " + detailedExplanation);
                    }
                }
            }
        }

        return lines.isEmpty() ? "Topicos principais nao identificados." : String.join("\n", lines);
    }

    private String resolveImportantPoints(JsonNode iaResponse, String mainTopics) {
        String importantPoints = iaResponse.path("importantPoints").asText("").trim();
        if (isUsefulText(importantPoints)) {
            return importantPoints;
        }

        JsonNode pages = iaResponse.path("pages");
        if (pages.isArray() && !pages.isEmpty()) {
            List<String> points = new ArrayList<>();
            for (JsonNode pageNode : pages) {
                JsonNode concepts = pageNode.path("importantConcepts");
                String explanation = pageNode.path("conceptsExplanation").asText("").trim();
                if (!concepts.isArray() || concepts.isEmpty()) {
                    continue;
                }
                for (JsonNode concept : concepts) {
                    String conceptText = concept.asText("").trim();
                    if (!conceptText.isEmpty()) {
                        if (!explanation.isEmpty()) {
                            points.add("- " + conceptText + ": " + explanation);
                        } else {
                            points.add("- " + conceptText);
                        }
                    }
                }
            }
            if (!points.isEmpty()) {
                return String.join("\n", points);
            }
        }

        if (isUsefulText(mainTopics)) {
            return buildImportantPointsFromMainTopics(mainTopics);
        }

        return """
                - Estrutura do conteudo: organize o estudo por paginas e identifique conceitos-chave.
                - Revisao ativa: transforme cada topico em pergunta de prova e responda sem consultar o material.
                - Aplicacao pratica: relacione cada conceito com um exemplo real de uso.
                - Erros comuns: anote confusoes frequentes e como evita-las na prova.
                """.trim();
    }

    private boolean isUsefulText(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return !normalized.equalsIgnoreCase("Pontos importantes nao identificados.")
                && !normalized.equalsIgnoreCase("Topicos principais nao identificados.")
                && !normalized.equalsIgnoreCase("Resumo indisponivel para este material.");
    }

    private String buildImportantPointsFromMainTopics(String mainTopics) {
        List<String> points = new ArrayList<>();
        String[] lines = mainTopics.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            String normalized = line.toLowerCase();
            if (!normalized.startsWith("pagina") && !normalized.startsWith("página")) {
                continue;
            }

            int separator = line.indexOf(':');
            if (separator < 0 || separator + 1 >= line.length()) {
                continue;
            }
            String pageLabel = line.substring(0, separator).trim();
            String topicsText = line.substring(separator + 1).trim();
            if (topicsText.isEmpty()) {
                continue;
            }

            String[] topics = topicsText.split(",");
            for (String topicRaw : topics) {
                String topic = topicRaw.trim();
                if (topic.isEmpty()) {
                    continue;
                }
                points.add("- " + pageLabel + " - " + topic
                        + ": conceito central para prova; estude definicao, contexto historico/tecnico e aplicacao atual.");
            }
        }

        if (points.size() < 8) {
            points.add("- Estrategia de prova: converta explicacoes longas em mapas mentais por pagina para revisar mais rapido.");
            points.add("- Questoes discursivas: pratique respostas comparando causa, efeito e relevancia dos topicos.");
            points.add("- Questoes objetivas: crie alternativas com pegadinhas em conceitos semelhantes para fixar diferencas.");
            points.add("- Revisao final: priorize topicos com maior conexao entre teoria e aplicacao pratica.");
        }

        return String.join("\n", points);
    }

    private int calculateWordCount(Summary summary) {
        StringBuilder merged = new StringBuilder();
        appendIfPresent(merged, summary.getShortSummary());
        appendIfPresent(merged, summary.getMainTopics());
        appendIfPresent(merged, summary.getImportantPoints());
        appendIfPresent(merged, summary.getSimplifiedExplanation());
        appendIfPresent(merged, summary.getIntroduction());
        appendIfPresent(merged, summary.getFormulasUsed());
        appendIfPresent(merged, summary.getExamQuestions());
        appendIfPresent(merged, summary.getStudyTips());
        appendIfPresent(merged, summary.getFinalSummary());

        String allText = merged.toString().trim();
        if (allText.isEmpty()) {
            return 0;
        }
        return allText.split("\\s+").length;
    }

    private void appendIfPresent(StringBuilder builder, String text) {
        if (text != null && !text.isBlank()) {
            builder.append(text).append(' ');
        }
    }

    private int calculateQualityScore(Summary summary) {
        int score = 0;

        if (isUsefulText(summary.getShortSummary())) {
            score += 15;
        }
        if (isUsefulText(summary.getMainTopics())) {
            score += 20;
        }
        if (isUsefulText(summary.getImportantPoints())) {
            score += 20;
        }
        if (isUsefulText(summary.getSimplifiedExplanation())) {
            score += 20;
        }
        if (isUsefulText(summary.getFormulasUsed())) {
            score += 10;
        }
        if (isUsefulText(summary.getExamQuestions())) {
            score += 10;
        }
        if (isUsefulText(summary.getStudyTips())) {
            score += 5;
        }

        return Math.min(score, 100);
    }
}
