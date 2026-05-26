package com.esunbank.financialpreference.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HtmlSanitizerTest {

    @Test
    void nullInput_returnsNull() {
        assertNull(HtmlSanitizer.clean(null));
    }

    @Test
    void plainAscii_passThrough() {
        assertEquals("A1236456789", HtmlSanitizer.clean("A1236456789"));
    }

    @Test
    void chineseText_passThrough() {
        assertEquals("玉山美元定存", HtmlSanitizer.clean("玉山美元定存"));
    }

    @Test
    void scriptTag_removed() {
        assertEquals("", HtmlSanitizer.clean("<script>alert(1)</script>"));
    }

    @Test
    void inlineBoldTag_stripped_textKept() {
        assertEquals("bold text", HtmlSanitizer.clean("<b>bold</b> text"));
    }

    @Test
    void onerrorHandler_stripped() {
        // img tag itself is stripped (empty policy); the alt text body is empty
        assertEquals("", HtmlSanitizer.clean("<img src=x onerror=alert(1)>"));
    }

    @Test
    void encodedEntities_preserved() {
        assertEquals("&lt;script&gt;", HtmlSanitizer.clean("&lt;script&gt;"));
    }
}
