package com.recipebook.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RecipeBodySizeFilterTest {

    private final RecipeBodySizeFilter filter = new RecipeBodySizeFilter();

    private MockHttpServletResponse send(String method, String uri, int length) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContent(new byte[length]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void oversizedRecipeWritesAreRejected() throws Exception {
        int tooBig = (int) RecipeBodySizeFilter.MAX_BYTES + 1;
        MockHttpServletResponse created = send("POST", "/api/recipes", tooBig);
        assertEquals(413, created.getStatus());
        assertTrue(created.getContentAsString().contains("kleineres Foto"));
        assertEquals(413, send("PUT", "/api/recipes/12", tooBig).getStatus());
    }

    @Test
    void normalRecipesAndOtherEndpointsPass() throws Exception {
        assertEquals(200, send("POST", "/api/recipes", 1000).getStatus());
        assertEquals(200, send("PUT", "/api/recipes/12", (int) RecipeBodySizeFilter.MAX_BYTES).getStatus());
        assertEquals(200, send("POST", "/api/ingredient-ai/parse", (int) RecipeBodySizeFilter.MAX_BYTES + 1).getStatus());
    }

    @Test
    void scansMayBeUpToEightMegabytes() throws Exception {
        assertEquals(200, send("POST", "/api/recipes/scan", (int) RecipeBodySizeFilter.MAX_SCAN_BYTES).getStatus());
        MockHttpServletResponse tooBig = send("POST", "/api/recipes/scan", (int) RecipeBodySizeFilter.MAX_SCAN_BYTES + 1);
        assertEquals(413, tooBig.getStatus());
        assertTrue(tooBig.getContentAsString().contains("8 MB"));
        assertEquals(8L * 1024 * 1024, RecipeBodySizeFilter.MAX_SCAN_BYTES);
    }
}
