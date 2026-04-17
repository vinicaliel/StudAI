package studAI.AI.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import studAI.AI.models.Summary;
import studAI.AI.services.SummaryService;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAndSummarize(@RequestParam("file") MultipartFile file) {
        try {
            // Lembra do JwtFilter? Ele pega o e-mail de quem mandou a requisição e deixa salvo aqui pra gente usar!
            String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Manda rodar o fluxo completo!
            Summary result = summaryService.processPdfAndSummarize(file, userEmail);
            
            // Retorna o resumo perfeito e estruturado pro navegador
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("Erro ao processar PDF: " + e.getMessage());
            e.printStackTrace();
            // Retorna a mensagem de erro (ex: "Você atingiu o limite de 10 resumos") para o Frontend mostrar
            return ResponseEntity.badRequest().body(e.getMessage()); 
        }
    }
}
