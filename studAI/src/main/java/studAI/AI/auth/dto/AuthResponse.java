package studAI.AI.auth.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String planType; // Para sabermos lá na tela se é FREE ou não
}