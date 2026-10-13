package com.recipebook.service;

import com.recipebook.model.Recipe;
import com.recipebook.translation.TranslatedRecipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharePreviewServiceTest {

    @Mock
    private ShareLinkService shareLinkService;

    @Mock
    private RecipeTranslationService translationService;

    private SharePreviewService service;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        service = new SharePreviewService(shareLinkService, translationService);
        ReflectionTestUtils.setField(service, "appUrl", "https://pastoors.cloud");
        recipe = new Recipe();
        recipe.setId(5L);
        recipe.setTitle("Linsen-Dal");
        recipe.setBaseServings(4);
        recipe.setPrepTimeMinutes(45);
        recipe.setTags(List.of("Curry", "Vegan"));
    }

    private static String jpegDataUrl(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLUE);
        g.fillRect(width / 2 - 10, height / 2 - 10, 20, 20);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    @Test
    void preview_usesTitleTagsServingsTimeAndAbsoluteUrls() throws IOException {
        recipe.setImageUrl(jpegDataUrl(400, 300));
        when(shareLinkService.getSharedRecipeEntity("tok_A-1")).thenReturn(recipe);
        when(translationService.current(recipe, "de")).thenReturn(Optional.empty());

        SharePreview preview = service.preview("tok_A-1");

        assertEquals("Linsen-Dal", preview.title());
        assertEquals("Curry · Vegan · 4 Portionen · 45 Min.", preview.description());
        assertEquals("https://pastoors.cloud/share/tok_A-1", preview.pageUrl());
        assertTrue(preview.imageUrl().matches("https://pastoors\\.cloud/api/share/tok_A-1/og-image\\?v=[0-9a-z]+"),
                preview.imageUrl());
    }

    @Test
    void preview_prefersTheStoredGermanTranslationWithoutTranslatingAnew() {
        recipe.setTitle("Turkish green beans");
        when(shareLinkService.getSharedRecipeEntity("tok")).thenReturn(recipe);
        when(translationService.current(recipe, "de")).thenReturn(Optional.of(new TranslatedRecipe(5L, "en", "de",
                TranslatedRecipe.TRANSLATED, "Türkische grüne Bohnen", null, List.of(), List.of(), "m", null)));

        assertEquals("Türkische grüne Bohnen", service.preview("tok").title());
        verify(translationService, never()).translate(any(), anyString());
    }

    @Test
    void preview_imageUrlChangesWithThePhotoAndHasNoVersionWithoutPhoto() throws IOException {
        when(shareLinkService.getSharedRecipeEntity("tok")).thenReturn(recipe);
        when(translationService.current(recipe, "de")).thenReturn(Optional.empty());

        assertEquals("https://pastoors.cloud/api/share/tok/og-image", service.preview("tok").imageUrl());
        recipe.setImageUrl(jpegDataUrl(400, 300));
        String first = service.preview("tok").imageUrl();
        recipe.setImageUrl(jpegDataUrl(500, 300));
        assertNotEquals(first, service.preview("tok").imageUrl());
    }

    @Test
    void preview_fallsBackToGenericTagsForInvalidOrExpiredLinks() {
        when(shareLinkService.getSharedRecipeEntity("gone")).thenThrow(new ResponseStatusException(HttpStatus.GONE));
        when(shareLinkService.getSharedRecipeEntity("nope")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        for (String token : List.of("gone", "nope")) {
            SharePreview preview = service.preview(token);
            assertEquals("Pastoors Familienrezepte", preview.title());
            assertEquals("https://pastoors.cloud/", preview.pageUrl());
            assertEquals("https://pastoors.cloud/api/share/og-image", preview.imageUrl());
        }
    }

    @Test
    void preview_doesNotLookUpMalformedTokens() {
        assertEquals("Pastoors Familienrezepte", service.preview("a\"><script>").title());
        assertEquals("Pastoors Familienrezepte", service.preview("x".repeat(65)).title());
        verifyNoInteractions(shareLinkService);
    }

    @Test
    void description_onlyUsesFieldsThatExist() {
        Recipe bare = new Recipe();
        bare.setBaseServings(null);
        assertEquals("Ein Rezept aus der Familien-Rezeptsammlung.", SharePreviewService.description(bare));

        bare.setBaseServings(2);
        bare.setServingsTo(3);
        bare.setPrepTimeMinutes(75);
        assertEquals("2–3 Portionen · 1 Std. 15 Min.", SharePreviewService.description(bare));

        bare.setBaseServings(1);
        bare.setServingsTo(null);
        bare.setPrepTimeMinutes(120);
        bare.setTags(List.of(" Suppe ", ""));
        assertEquals("Suppe · 1 Portion · 2 Std.", SharePreviewService.description(bare));
    }

    @Test
    void previewImage_isA1200x630JpegBelow300KbCroppedFromTheCenter() throws IOException {
        recipe.setImageUrl(jpegDataUrl(1600, 1600));
        when(shareLinkService.getSharedRecipeEntity("tok")).thenReturn(recipe);

        byte[] jpeg = service.previewImage("tok");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));

        assertEquals(1200, image.getWidth());
        assertEquals(630, image.getHeight());
        assertTrue(jpeg.length < SharePreviewService.MAX_IMAGE_BYTES, String.valueOf(jpeg.length));
        Color center = new Color(image.getRGB(600, 315));
        assertTrue(center.getBlue() > 200 && center.getRed() < 60, center.toString());
        assertSame(jpeg, service.previewImage("tok"));
    }

    @Test
    void previewImage_staysBelow300KbForNoisyPhotos() throws IOException {
        BufferedImage noise = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(1);
        for (int y = 0; y < noise.getHeight(); y++) {
            for (int x = 0; x < noise.getWidth(); x++) noise.setRGB(x, y, random.nextInt(0xFFFFFF));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(noise, "png", out);
        recipe.setImageUrl("data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray()));
        when(shareLinkService.getSharedRecipeEntity("tok")).thenReturn(recipe);

        byte[] jpeg = service.previewImage("tok");

        assertTrue(jpeg.length <= SharePreviewService.MAX_IMAGE_BYTES, String.valueOf(jpeg.length));
        assertEquals(1200, ImageIO.read(new ByteArrayInputStream(jpeg)).getWidth());
    }

    @Test
    void previewImage_usesTheAppLogoWithoutOrWithUnreadablePhoto() throws IOException {
        when(shareLinkService.getSharedRecipeEntity("tok")).thenReturn(recipe);
        byte[] fallback = service.fallbackImage();
        BufferedImage logo = ImageIO.read(new ByteArrayInputStream(fallback));
        assertEquals(1200, logo.getWidth());
        assertEquals(630, logo.getHeight());

        assertSame(fallback, service.previewImage("tok"));
        recipe.setImageUrl("https://images.example.com/photo.jpg");
        assertSame(fallback, service.previewImage("tok"));
        recipe.setImageUrl("data:image/jpeg;base64,AAEC");
        assertSame(fallback, service.previewImage("tok"));
    }

    @Test
    void previewImage_rejectsInvalidLinks() {
        when(shareLinkService.getSharedRecipeEntity("gone")).thenThrow(new ResponseStatusException(HttpStatus.GONE));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.previewImage("gone"));
        assertEquals(HttpStatus.GONE, e.getStatusCode());
    }
}
