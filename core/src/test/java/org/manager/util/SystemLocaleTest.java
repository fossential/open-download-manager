package org.manager.util;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemLocaleTest {
    @Test
    void localeCategoriesAndLanguagePriorityFollowDesktopEnvironment() {
        assertEquals(Locale.CANADA_FRENCH, SystemLocale.display(Map.of("LANG", "en_US.UTF-8",
                "LC_MESSAGES", "fr_CA.UTF-8"), Locale.US));
        assertEquals(Locale.US, SystemLocale.display(Map.of("LANG", "fr_FR.UTF-8",
                "LC_ALL", "en_US.UTF-8"), Locale.FRANCE));
        assertEquals(Locale.FRANCE, SystemLocale.display(Map.of("LANG", "en_US.UTF-8",
                "LANGUAGE", "it:fr_FR:en"), Locale.US));
        assertEquals(Locale.ENGLISH, SystemLocale.display(Map.of("LC_ALL", "C",
                "LANGUAGE", "fr"), Locale.FRANCE));
        assertEquals(Locale.ENGLISH, SystemLocale.display(Map.of("LC_ALL", "C.UTF-8",
                "LANGUAGE", "fr"), Locale.FRANCE));
        assertEquals(Locale.ENGLISH, SystemLocale.display(Map.of("LANG", "it_IT.UTF-8"), Locale.ITALY));
        assertEquals(Locale.CANADA_FRENCH, SystemLocale.display(Map.of(), Locale.CANADA_FRENCH));
    }

    @ParameterizedTest
    @CsvSource({"de_DE, de-DE", "de_AT, de-AT", "es_ES, es-ES", "es_MX, es-MX",
        "ja_JP, ja-JP", "pt_PT, pt-PT", "pt_BR, pt-BR", "zh_CN, zh-CN", "zh_SG, zh-SG"})
    void newLanguagesAndTheirRegionsFollowDesktopPreferences(String name, String tag) {
        Locale expected = Locale.forLanguageTag(tag);
        assertEquals(expected, SystemLocale.display(Map.of("LANG", name + ".UTF-8"), Locale.US));
        assertEquals(expected, SystemLocale.display(Map.of("LANG", "en_US.UTF-8",
                "LANGUAGE", "it:" + name + ":fr:en"), Locale.US));
        assertEquals(Locale.ENGLISH, SystemLocale.display(Map.of("LC_ALL", "C.UTF-8",
                "LANGUAGE", name), expected));
    }
}
