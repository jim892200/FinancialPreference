package com.esunbank.financialpreference.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SanitizingRequestWrapperTest {

    @Test
    void getParameter_stripsScriptTag() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("userId", "<script>alert(1)</script>A1");

        SanitizingRequestWrapper wrapped = new SanitizingRequestWrapper(req);

        assertEquals("A1", wrapped.getParameter("userId"));
    }

    @Test
    void getParameter_plainAscii_unchanged() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("userId", "A1236456789");

        SanitizingRequestWrapper wrapped = new SanitizingRequestWrapper(req);

        assertEquals("A1236456789", wrapped.getParameter("userId"));
    }

    @Test
    void getParameter_unknownName_returnsNull() {
        SanitizingRequestWrapper wrapped = new SanitizingRequestWrapper(new MockHttpServletRequest());
        assertNull(wrapped.getParameter("missing"));
    }

    @Test
    void getParameterValues_eachElementSanitized() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addParameter("tags", "<b>safe</b>", "<script>bad</script>x");

        SanitizingRequestWrapper wrapped = new SanitizingRequestWrapper(req);

        assertArrayEquals(new String[]{"safe", "x"}, wrapped.getParameterValues("tags"));
    }

    @Test
    void getParameterMap_allValuesSanitized() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("a", "<i>1</i>");
        req.setParameter("b", "<script>2</script>plain");

        SanitizingRequestWrapper wrapped = new SanitizingRequestWrapper(req);

        Map<String, String[]> map = wrapped.getParameterMap();
        assertArrayEquals(new String[]{"1"},      map.get("a"));
        assertArrayEquals(new String[]{"plain"}, map.get("b"));
    }
}
