package studAI.AI.config; // Lembre de ajustar caso coloque em outra pasta!

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extrai o cabeçalho "Authorization" da requisição
        final String authHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        // 2. Confere se o Token veio e se ele começa com "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // Pega o token tirando a palavra "Bearer "
            try {
                email = jwtUtil.getEmailFromToken(jwt);
            } catch (Exception e) {
                System.out.println("Erro ao ler Token JWT: " + e.getMessage());
            }
        }

        // 3. Se achamos um email válido no token e o usuário ainda não está logado na requisição atual...
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            if (jwtUtil.validateToken(jwt)) {
                // Autentica ele no Spring Security liberando a rota!
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, null, new ArrayList<>()); // Lista vazia pq não estamos usando regras avançadas de "Roles" no MVP
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
