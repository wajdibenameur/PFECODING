package tn.iteam.domain;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "zkbio_problem")
@Setter@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZkBioProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String problemId;
    private String device;      // nom du device ou utilisateur
    private String description;
    private Boolean active;
    private String status;
    @Column(name = "started_at")
    private Long startedAt;
    @Column(name = "resolved_at")
    private Long resolvedAt;
    private String source = "ZKBIO";
    private Long eventId;       // optionnel
}
