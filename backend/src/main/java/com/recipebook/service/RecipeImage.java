package com.recipebook.service;

import com.recipebook.model.ImageCredit;

/** Automatisch gefundenes Rezeptbild: lokal gespeicherte data-URL plus Fotografen-Nennung. */
public record RecipeImage(String dataUrl, ImageCredit credit) {
}
