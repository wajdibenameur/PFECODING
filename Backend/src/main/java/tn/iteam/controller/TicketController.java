package tn.iteam.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iteam.domain.Ticket;
import tn.iteam.dto.TicketAssignmentRequestDTO;
import tn.iteam.dto.TicketCreateRequestDTO;
import tn.iteam.dto.TicketDecisionRequestDTO;
import tn.iteam.dto.TicketInterventionRequestDTO;
import tn.iteam.dto.TicketResponseDTO;
import tn.iteam.dto.TicketStatusUpdateRequestDTO;
import tn.iteam.dto.TicketUserDTO;
import tn.iteam.dto.ZabbixProblemDTO;
import tn.iteam.enums.Priority;
import tn.iteam.enums.TicketStatus;
import tn.iteam.mapper.TicketMapper;
import tn.iteam.service.TicketService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "API de création, suivi et traitement des tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;

    // ================= CREATE FROM ZABBIX =================
    @PostMapping("/from-problem")
    @Operation(summary = "Créer un ticket depuis un incident", description = "Crée un ticket à partir d'un incident de supervision existant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide")
    })
    public ResponseEntity<TicketResponseDTO> createFromProblem(
            @RequestBody ZabbixProblemDTO problem,
            @Parameter(description = "Identifiant de l'utilisateur créateur du ticket", required = true)
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(ticketMapper.toResponse(ticketService.createFromProblem(problem, userId)));
    }

    // ================= CREATE MANUAL =================
    @PostMapping
    @Operation(summary = "Créer un ticket manuel", description = "Crée un ticket manuel sans dépendre d'un incident externe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide")
    })
    public ResponseEntity<TicketResponseDTO> createManual(
            @RequestBody TicketCreateRequestDTO request
    ) {
        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? Priority.valueOf(request.getPriority().toUpperCase()) : Priority.MEDIUM)
                .hostId(request.getHostId())
                .monitoringSource(request.getMonitoringSource())
                .externalProblemId(request.getExternalProblemId())
                .resourceRef(request.getResourceRef())
                .externalProblem(Boolean.TRUE.equals(request.getExternalProblem()))
                .build();

        return ResponseEntity.ok(ticketMapper.toResponse(ticketService.createManual(ticket, request.getCreatorId())));
    }

    // ================= ASSIGN =================
    @PutMapping("/{id}/assign")
    @Operation(summary = "Assigner un ticket", description = "Assigne un ticket à un utilisateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket assigné avec succès"),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public ResponseEntity<TicketResponseDTO> assign(
            @Parameter(description = "Identifiant du ticket", required = true)
            @PathVariable Long id,
            @RequestBody TicketAssignmentRequestDTO request
    ) {
        return ResponseEntity.ok(ticketMapper.toResponse(ticketService.assign(id, request.getUserId())));
    }

    // ================= UPDATE STATUS =================
    @PutMapping("/{id}/status")
    @Operation(summary = "Modifier le statut d'un ticket", description = "Met à jour le statut d'un ticket et sa résolution éventuelle.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut mis à jour avec succès"),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public ResponseEntity<TicketResponseDTO> updateStatus(
            @Parameter(description = "Identifiant du ticket", required = true)
            @PathVariable Long id,
            @RequestBody TicketStatusUpdateRequestDTO request
    ) {
        TicketStatus status = TicketStatus.valueOf(request.getStatus().toUpperCase());
        return ResponseEntity.ok(ticketMapper.toResponse(ticketService.updateStatus(id, status, request.getResolution())));
    }

    // ================= VALIDATE =================
    @PutMapping("/{id}/validate")
    @Operation(summary = "Valider un ticket", description = "Valide un ticket par un administrateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket validé avec succès"),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public ResponseEntity<TicketResponseDTO> validate(
            @Parameter(description = "Identifiant du ticket", required = true)
            @PathVariable Long id,
            @RequestBody TicketDecisionRequestDTO request
    ) {
        return ResponseEntity.ok(ticketMapper.toResponse(ticketService.validate(id, request.getAdminId())));
    }

    // ================= REJECT =================
    @PutMapping("/{id}/reject")
    @Operation(summary = "Rejeter un ticket", description = "Rejette un ticket et enregistre éventuellement la raison du rejet.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket rejeté avec succès"),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public ResponseEntity<TicketResponseDTO> reject(
            @Parameter(description = "Identifiant du ticket", required = true)
            @PathVariable Long id,
            @RequestBody TicketDecisionRequestDTO request
    ) {
        return ResponseEntity.ok(ticketMapper.toResponse(ticketService.reject(id, request.getAdminId(), request.getReason())));
    }

    // ================= ADD COMMENT =================
    @PostMapping("/{id}/comment")
    @Operation(summary = "Ajouter un commentaire", description = "Ajoute un commentaire à un ticket existant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commentaire ajouté avec succès"),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public ResponseEntity<TicketResponseDTO> addComment(
            @Parameter(description = "Identifiant du ticket", required = true)
            @PathVariable Long id,
            @Parameter(description = "Commentaire à ajouter", required = true)
            @RequestParam String comment,
            @Parameter(description = "Identifiant de l'utilisateur auteur du commentaire", required = true)
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(ticketMapper.toResponse(ticketService.addComment(id, comment, userId)));
    }

    @PostMapping("/{id}/interventions")
    @Operation(summary = "Ajouter une intervention", description = "Ajoute une intervention détaillée à un ticket.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Intervention ajoutée avec succès"),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public ResponseEntity<TicketResponseDTO> addIntervention(
            @Parameter(description = "Identifiant du ticket", required = true)
            @PathVariable Long id,
            @RequestBody TicketInterventionRequestDTO request
    ) {
        ticketService.addIntervention(id, request.getUserId(), request.getAction(), request.getComment(), request.getResult());
        return ticketService.getById(id)
                .map(ticketMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ================= GET ALL (PAGINATION) =================
    @GetMapping
    @Operation(summary = "Rechercher des tickets", description = "Retourne la liste paginée des tickets avec filtres optionnels.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tickets récupérés avec succès")
    })
    public ResponseEntity<Page<TicketResponseDTO>> getAll(
            @Parameter(description = "Filtre optionnel sur le statut du ticket")
            @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Filtre optionnel sur la priorité du ticket")
            @RequestParam(required = false) Priority priority,
            @Parameter(description = "Filtre optionnel sur la source de supervision")
            @RequestParam(required = false) String source,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ticketService.search(status, priority, source, pageable).map(ticketMapper::toResponse));
    }

    // ================= FILTER BY STATUS =================
    @GetMapping("/status/{status}")
    @Operation(summary = "Lister les tickets par statut", description = "Retourne les tickets paginés correspondant au statut demandé.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tickets récupérés avec succès")
    })
    public ResponseEntity<Page<TicketResponseDTO>> getByStatus(
            @Parameter(description = "Statut du ticket à filtrer", required = true)
            @PathVariable TicketStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ticketService.getByStatus(status, pageable).map(ticketMapper::toResponse));
    }

    @GetMapping("/users")
    @Operation(summary = "Lister les utilisateurs assignables", description = "Retourne la liste des utilisateurs pouvant être assignés à un ticket.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateurs récupérés avec succès")
    })
    public ResponseEntity<List<TicketUserDTO>> getAssignableUsers() {
        return ResponseEntity.ok(ticketService.getAssignableUsers().stream().map(ticketMapper::toUser).toList());
    }

    // ================= GET BY ID =================
    @GetMapping("/{id}")
    @Operation(summary = "Consulter un ticket", description = "Retourne le détail d'un ticket à partir de son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket récupéré avec succès"),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public ResponseEntity<TicketResponseDTO> getById(@Parameter(description = "Identifiant du ticket", required = true) @PathVariable Long id) {

        Optional<Ticket> ticket = ticketService.getById(id);

        return ticket.map(ticketMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un ticket", description = "Supprime définitivement un ticket.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ticket supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Ticket introuvable")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "Identifiant du ticket", required = true) @PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
