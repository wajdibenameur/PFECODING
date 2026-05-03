package tn.iteam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Corps de requête pour valider ou rejeter un ticket")
public class TicketDecisionRequestDTO {
    @Schema(description = "Identifiant de l'administrateur qui prend la décision", example = "2")
    private Long adminId;
    @Schema(description = "Raison du rejet, si le ticket est rejeté", example = "Informations insuffisantes")
    private String reason;
}
