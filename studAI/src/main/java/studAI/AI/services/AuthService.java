package studAI.AI.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import studAI.AI.auth.dto.AuthRequest;
import studAI.AI.auth.dto.AuthResponse;
import studAI.AI.config.JwtUtil;
import studAI.AI.user.User;
import studAI.AI.repository.UserRepository;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(AuthRequest request) {
        // Valida se o email já existe
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Este e-mail já está em uso!");
        }

        // Monta o novo usuário no BD (a senha aqui é transformada em HASH para ngm ler puro)
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        // FUTURO MVP: Logica de mandar email c/ código entra aqui!

        User savedUser = userRepository.save(user); // Salva no Postgres!
        
        // Gera o token oficial JWT de 24 horas usando o JwtUtil!
        String token = jwtUtil.generateToken(savedUser);
        
        return new AuthResponse(token, savedUser.getEmail(), savedUser.getPlanType());
    }

    public AuthResponse login(AuthRequest request) {
        // Pega o usuário
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("E-mail não cadastrado!"));

        // Confere a senha crua com a senha criptografada do banco
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Senha incorreta!");
        }

        // Devolve igualzinho: O Token pro navegador
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getPlanType());
    }
}
