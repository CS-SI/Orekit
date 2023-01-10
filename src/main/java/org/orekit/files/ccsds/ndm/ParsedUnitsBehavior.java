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
package org.orekit.files.ccsds.ndm;

import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.units.Unit;

/** Behavior adopted for units that have been parsed from a CCSDS message.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum ParsedUnitsBehavior {

    /** Ignore parsed units, just relying on CCSDS standard.
     * <p>
     * When this behavior is selected having a unit parsed as second
     * when CCSDS mandates kilometer will be accepted.
     * </p>
     */
    IGNORE_PARSED {
        /** {@inheritDoc} */
        @Override
        public Unit select(final Unit message, final Unit standard) {
            return standard;
        }
    },

    /** Allow compatible units, performing conversion.
     * <p>
     * When this behavior is selected having a unit parsed as second
     * when CCSDS mandates kilometer will be refused, but having a unit
     * parsed as meter will be accepted, with proper conversion performed.
     * Missing units (i.e. units parsed as {@link Unit#NONE}) are considered
     * to be standard.
     * </p>
     */
    CONVERT_COMPATIBLE {
        /** {@inheritDoc} */
        @Override
        public Unit select(final Unit message, final Unit standard) {
            if (message == Unit.NONE) {
                return standard;
            } else if (message.sameDimension(standard)) {
                return message;
            } else {
                throw new OrekitException(OrekitMessages.INCOMPATIBLE_UNITS,
                                          message.getName(), standard.getName());
            }
        }
    },

    /** Enforce strict compliance with CCSDS standard.
     * <p>
     * When this behavior is selected having a unit parsed as second
     * or as meter when CCSDS mandates kilometer will both be refused.
     * Missing units (i.e. units parsed as {@link Unit#NONE}) are considered
     * to be standard.
     * </p>
     */
    STRICT_COMPLIANCE {
        /** {@inheritDoc} */
        @Override
        public Unit select(final Unit message, final Unit standard) {
            if (message == Unit.NONE ||
                Precision.equals(message.getScale(), standard.getScale(), 1) &&
                message.sameDimension(standard)) {
                return standard;
            } else {
                throw new OrekitException(OrekitMessages.INCOMPATIBLE_UNITS,
                                          message.getName(), standard.getName());
            }
        }
    };

    /** Select the unit to use for interpreting parsed value.
     * @param message unit parsed in the CCSDS message
     * @param standard unit mandated by the standard
     * @return selected unit
     */
    public abstract Unit select(Unit message, Unit standard);

}
