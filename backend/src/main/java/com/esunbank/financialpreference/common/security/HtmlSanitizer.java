package com.esunbank.financialpreference.common.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * 對使用者輸入字串套用嚴格的 HTML 清洗策略，做為 XSS 防護的最後一道閘門。
 *
 * 本系統的所有輸入欄位（userId / productName / account 等）都應為純文字，
 * 因此採用「空策略」── 任何 HTML 標籤（含 script / style / iframe 等）一律剝除，
 * 標籤內的純文字保留、HTML entity (&amp;lt; 等) 維持原樣。
 */
public final class HtmlSanitizer {

    private static final PolicyFactory STRICT_TEXT = new HtmlPolicyBuilder().toFactory();

    private HtmlSanitizer() {}

    public static String clean(String input) {
        if (input == null) return null;
        return STRICT_TEXT.sanitize(input);
    }
}
