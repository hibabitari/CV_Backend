package com.ahmed.pfa.cvplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO générique pour les réponses paginées
 * Évite l'exposition de PageImpl (Spring internal)
 *
 * @param <T> Type du contenu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    /**
     * Contenu de la page
     */
    private List<T> content;

    /**
     * Numéro de la page actuelle (0-indexed)
     */
    private int page;

    /**
     * Nombre d'éléments par page
     */
    private int size;

    /**
     * Nombre total d'éléments
     */
    private long totalElements;

    /**
     * Nombre total de pages
     */
    private int totalPages;

    /**
     * Première page?
     */
    private boolean first;

    /**
     * Dernière page?
     */
    private boolean last;

    /**
     * Constructeur depuis Spring Page
     *
     * @param page Page Spring à convertir
     */
    public PagedResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }

    /**
     * Factory method pour créer depuis Page
     *
     * @param page Page Spring
     * @param <T> Type du contenu
     * @return PagedResponse
     */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(page);
    }
}