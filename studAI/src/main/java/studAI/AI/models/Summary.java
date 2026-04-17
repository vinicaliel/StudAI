package studAI.AI.models; 

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "summaries")
@Getter
@Setter
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Conecta 1 Resumo para exatamente 1 Material
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    private Material material;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "short_summary", nullable = false, columnDefinition = "TEXT")
    private String shortSummary;

    @Column(name = "main_topics", nullable = false, columnDefinition = "TEXT")
    private String mainTopics;

    @Column(name = "important_points", nullable = false, columnDefinition = "TEXT")
    private String importantPoints;

    @Column(name = "simplified_explanation", nullable = false, columnDefinition = "TEXT")
    private String simplifiedExplanation;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
