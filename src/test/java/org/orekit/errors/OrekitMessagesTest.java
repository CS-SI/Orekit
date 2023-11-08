/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.errors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

public class OrekitMessagesTest {

    private final String[] LANGUAGES_LIST = { "da", "de", "el", "en", "es", "fr", "gl", "it", "no", "ro" };

    @Test
    public void testMessageNumber() {
        Assertions.assertEquals(289, OrekitMessages.values().length);
    }

    @Test
    public void testAllKeysPresentInPropertiesFiles() {
        for (final String language : LANGUAGES_LIST) {
            ResourceBundle bundle = ResourceBundle.getBundle("assets/org/orekit/localization/OrekitMessages",
                    Locale.forLanguageTag(language), new OrekitMessages.UTF8Control());
            for (OrekitMessages message : OrekitMessages.values()) {
                final String messageKey = message.toString();
                boolean keyPresent = false;
                for (final Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
                    keyPresent |= messageKey.equals(keys.nextElement());
                }
                Assertions.assertTrue(keyPresent,"missing key \"" + message.name() + "\" for language " + language);
            }
            Assertions.assertEquals(language, bundle.getLocale().getLanguage());
        }

    }

    @Test
    public void testAllPropertiesCorrespondToKeys() {
        for (final String language : LANGUAGES_LIST) {
            ResourceBundle bundle = ResourceBundle.getBundle("assets/org/orekit/localization/OrekitMessages",
                    Locale.forLanguageTag(language), new OrekitMessages.UTF8Control());
            for (final Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
                final String propertyKey = keys.nextElement();
                try {
                    Assertions.assertNotNull(OrekitMessages.valueOf(propertyKey));
                } catch (IllegalArgumentException iae) {
                    Assertions.fail("unknown key \"" + propertyKey + "\" in language " + language);
                }
            }
            Assertions.assertEquals(language, bundle.getLocale().getLanguage());
        }

    }

    @Test
    public void testNoMissingFrenchTranslation() {

        for (OrekitMessages message : OrekitMessages.values()) {
            String translated = message.getLocalizedString(Locale.FRENCH);
            // To detect a missing translation, check if the returned string is the original text in English.
            Assertions.assertNotEquals(message.name(), translated, message.getSourceString());
         }
    }

    @Test
    public void testNoOpEnglishTranslation() {
        for (OrekitMessages message : OrekitMessages.values()) {
            String translated = message.getLocalizedString(Locale.ENGLISH);

            // Check that the original message is not empty.
            Assertions.assertFalse(message.getSourceString().length() == 0,message.name());

            // Check that both texts are the same
            Assertions.assertEquals(message.getSourceString(), translated,message.name());

        }
    }

    @Test
    public void testVariablePartsConsistency() {
        for (final String language : LANGUAGES_LIST) {
            Locale locale = Locale.forLanguageTag(language);
            for (OrekitMessages message : OrekitMessages.values()) {
                MessageFormat source = new MessageFormat(message.getSourceString());
                MessageFormat translated = new MessageFormat(message.getLocalizedString(locale));
                Assertions.assertEquals(source.getFormatsByArgumentIndex().length,
                        translated.getFormatsByArgumentIndex().length,message.name() + " (" + language + ")");
            }
        }
    }

}
