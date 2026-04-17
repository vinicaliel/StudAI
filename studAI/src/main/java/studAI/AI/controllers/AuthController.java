package studAI.AI.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import studAI.AI.auth.dto.AuthRequest;
import studAI.AI.auth.dto.AuthResponse;
import studAI.AI.services.AuthService;

@RestController
@RequestMapping("/api/auth") // Esta é a URL que configuramos para ficar 100% aberta no Security
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
