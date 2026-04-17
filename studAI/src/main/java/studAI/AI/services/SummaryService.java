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
        String extractedText = "";
        try (InputStream is = file.getInputStream(); PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
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
        summary.setTitle(iaResponse.path("title").asText());
        summary.setShortSummary(iaResponse.path("shortSummary").asText());
        summary.setMainTopics(iaResponse.path("mainTopics").asText());
        summary.setImportantPoints(iaResponse.path("importantPoints").asText());
        summary.setSimplifiedExplanation(iaResponse.path("simplifiedExplanation").asText());
        
        return summaryRepository.save(summary);
    }
}
