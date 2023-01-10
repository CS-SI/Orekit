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
package org.orekit.files.ccsds.ndm.adm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.orekit.files.ccsds.utils.lexical.ParseToken;
import org.orekit.files.ccsds.utils.lexical.TokenType;
import org.orekit.files.ccsds.utils.lexical.XmlTokenBuilder;
import org.orekit.utils.units.Unit;
import org.orekit.utils.units.UnitsCache;

/** Builder for rotation angles and rates.
 * <p>
 * Instances of this class are immutable.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class RotationXmlTokenBuilder implements XmlTokenBuilder {

    /** Attribute defining rotation angle. */
    private static final String ANGLE = "angle";

    /** Attribute defining rotation rate. */
    private static final String RATE = "rate";

    /** Attribute name for units. */
    private static final String UNITS = "units";

    /** Cache for parsed units. */
    private final UnitsCache cache;

    /** Simple constructor.
     */
    public RotationXmlTokenBuilder() {
        this.cache = new UnitsCache();
    }

    /** {@inheritDoc} */
    @Override
    public List<ParseToken> buildTokens(final boolean startTag, final boolean isLeaf, final String qName,
                                        final String content, final Map<String, String> attributes,
                                        final int lineNumber, final String fileName) {

        // get the token name from the first attribute found
        String name = attributes.get(ANGLE);
        if (name == null) {
            name = attributes.get(RATE);
        }

        if (startTag) {
            return Collections.singletonList(new ParseToken(TokenType.START, name, content, Unit.NONE, lineNumber, fileName));
        } else {
            final List<ParseToken> built = new ArrayList<>(2);
            if (isLeaf) {
                // get units
                final Unit units = cache.getUnits(attributes.get(UNITS));

                built.add(new ParseToken(TokenType.ENTRY, name, content, units, lineNumber, fileName));
            }
            built.add(new ParseToken(TokenType.STOP, name, null, Unit.NONE, lineNumber, fileName));
            return built;
        }

    }

}
