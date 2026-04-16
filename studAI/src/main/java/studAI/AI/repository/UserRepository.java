
package studAI.AI.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import studAI.AI.user.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // Essencial para o Login
    Optional<User> findByEmail(String email);
    
    // Útil caso queiramos checar existência rápida no Cadastro
    boolean existsByEmail(String email);
}
