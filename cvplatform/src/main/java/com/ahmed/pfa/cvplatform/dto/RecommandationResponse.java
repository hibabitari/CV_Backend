package com.ahmed.pfa.cvplatform.dto;

import com.ahmed.pfa.cvplatform.model.Recommandation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour une recommandation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommandationResponse {

    private Long id;
    private String type;
    private String texte;
    private Integer priorite;
    private String categorie;

    /**
     * Constructeur depuis Entity
     */
    public RecommandationResponse(Recommandation recommandation) {
        this.id = recommandation.getId();
        this.type = recommandation.getType().name();
        this.texte = recommandation.getTexte();
        this.priorite = recommandation.getPriorite();
        this.categorie = recommandation.getCategorie();
    }
}