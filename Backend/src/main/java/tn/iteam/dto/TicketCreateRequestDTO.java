package tn.iteam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Corps de requête pour la création manuelle d'un ticket")
public class TicketCreateRequestDTO {
    @Schema(description = "Titre du ticket", example = "Perte de connectivité sur un routeur")
    private String title;
    @Schema(description = "Description détaillée du ticket", example = "Le routeur principal ne répond plus au ping.")
    private String description;
    @Schema(description = "Priorité du ticket", example = "HIGH")
    private String priority;
    @Schema(description = "Identifiant de l'utilisateur créateur", example = "1")
    private Long creatorId;
    @Schema(description = "Identifiant de l'hôte concerné si disponible", example = "10101")
    private Long hostId;
    @Schema(description = "Source de supervision associée", example = "ZABBIX")
    private String monitoringSource;
    @Schema(description = "Identifiant externe du problème si disponible", example = "evt-445")
    private String externalProblemId;
    @Schema(description = "Référence technique ou ressource concernée", example = "router-core-01")
    private String resourceRef;
    @Schema(description = "Indique si le ticket provient d'un problème externe", example = "false")
    private Boolean externalProblem;
}
