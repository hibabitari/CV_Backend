package com.ahmed.pfa.cvplatform.dto;

/**
 * Données binaires d'un CV pour téléchargement (propriétaire ou admin).
 */
public record CVDownloadResult(String fileName, String contentType, byte[] content) {
}
