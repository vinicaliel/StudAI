package studAI.AI.models; 

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import studAI.AI.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "materials")
@Getter
@Setter
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relaciona esse material com o dono (Usuário)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(nullable = false)
    private String status = "PENDING"; // Valores: PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
