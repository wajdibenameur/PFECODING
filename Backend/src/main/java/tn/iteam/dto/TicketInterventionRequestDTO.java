package tn.iteam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Corps de requête pour ajouter une intervention sur un ticket")
public class TicketInterventionRequestDTO {
    @Schema(description = "Identifiant de l'utilisateur ayant réalisé l'intervention", example = "7")
    private Long userId;
    @Schema(description = "Action réalisée", example = "Redémarrage de l'équipement")
    private String action;
    @Schema(description = "Commentaire complémentaire", example = "Intervention réalisée à distance")
    private String comment;
    @Schema(description = "Résultat de l'intervention", example = "Service rétabli")
    private String result;
}
