/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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


import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Test;

public class OrekitMessagesTest {

    @Test
    public void testMessageNumber() {
        Assert.assertEquals(136, OrekitMessages.values().length);
    }

    @Test
    public void testAllKeysPresentInPropertiesFiles() {
        for (final String language : new String[] { "de", "el", "en", "es", "fr", "gl", "it", "no", "ro" } ) {
            ResourceBundle bundle =
                ResourceBundle.getBundle("assets/org/orekit/localization/OrekitMessages",
                                         new Locale(language), new OrekitMessages.UTF8Control());
            for (OrekitMessages message : OrekitMessages.values()) {
                final String messageKey = message.toString();
                boolean keyPresent = false;
                for (final Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
                    keyPresent |= messageKey.equals(keys.nextElement());
                }
                Assert.assertTrue("missing key \"" + message.name() + "\" for language " + language,
                                  keyPresent);
            }
            Assert.assertEquals(language, bundle.getLocale().getLanguage());
        }

    }

    @Test
    public void testAllPropertiesCorrespondToKeys() {
        for (final String language : new String[] { "de", "el", "en", "es", "fr", "gl", "it", "no", "ro" } ) {
            ResourceBundle bundle =
                ResourceBundle.getBundle("assets/org/orekit/localization/OrekitMessages",
                                         new Locale(language), new OrekitMessages.UTF8Control());
            for (final Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements();) {
                final String propertyKey = keys.nextElement();
                try {
                    Assert.assertNotNull(OrekitMessages.valueOf(propertyKey));
                } catch (IllegalArgumentException iae) {
                    Assert.fail("unknown key \"" + propertyKey + "\" in language " + language);
                }
            }
            Assert.assertEquals(language, bundle.getLocale().getLanguage());
        }

    }

    @Test
    public void testNoMissingFrenchTranslation() {
        for (OrekitMessages message : OrekitMessages.values()) {
            String translated = message.getLocalizedString(Locale.FRENCH);
            Assert.assertFalse(message.name(), translated.toLowerCase().contains("missing translation"));
        }
    }

    @Test
    public void testNoOpEnglishTranslation() {
        for (OrekitMessages message : OrekitMessages.values()) {
            String translated = message.getLocalizedString(Locale.ENGLISH);
            Assert.assertEquals(message.getSourceString(), translated);
        }
    }

    @Test
    public void testVariablePartsConsistency() {
        for (final String language : new String[] { "de", "el", "en", "es", "fr", "gl", "it", "no", "ro" } ) {
            Locale locale = new Locale(language);
            for (OrekitMessages message : OrekitMessages.values()) {
                MessageFormat source     = new MessageFormat(message.getSourceString());
                MessageFormat translated = new MessageFormat(message.getLocalizedString(locale));
                Assert.assertEquals(message.name() + " (" + language + ")",
                                    source.getFormatsByArgumentIndex().length,
                                    translated.getFormatsByArgumentIndex().length);
            }
        }
    }

}
