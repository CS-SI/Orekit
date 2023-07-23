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
package org.orekit.frames.encounter;

import org.hipparchus.CalculusFieldElement;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/**
 * Enum for encounter local orbital frame.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public enum EncounterLOFType {

    /**
     * Default encounter local orbital frame based on Figure 1.3 from Romain Serra's thesis.
     * <p>
     * Note that <b>it is up to the user</b> to choose which object should be at the origin.
     */
    DEFAULT {
        /** {@inheritDoc} */
        @Override
        public DefaultEncounterLOF getFrame(final PVCoordinates other) {
            return new DefaultEncounterLOF(other);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> EncounterLOF getFrame(
                final FieldPVCoordinates<T> other) {
            return new DefaultEncounterLOF(other);
        }

    },
    /**
     * Valsecchi encounter local orbital frame based on Valsecchi formulation from : "Valsecchi, G. B., Milani, A., Gronchi,
     * G. F. &amp; Ches- ley, S. R. Resonant returns to close approaches: Analytical theory. Astronomy &amp; Astrophysics
     * 408, 1179–1196 (2003)."
     * <p>
     * Note that <b>it is up to the user</b> to choose which object should be at the origin.
     */
    VALSECCHI {
        /** {@inheritDoc} */
        @Override
        public ValsecchiEncounterFrame getFrame(final PVCoordinates other) {
            return new ValsecchiEncounterFrame(other);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> EncounterLOF getFrame(
                final FieldPVCoordinates<T> other) {
            return new ValsecchiEncounterFrame(other);
        }

    };

    /**
     * Get encounter local orbital frame associated to this enum.
     *
     * @param other other object {@link PVCoordinates position and velocity coordinates} that is not the origin of the
     * encounter frame
     *
     * @return encounter local orbital frame associated to this enum
     */
    public abstract EncounterLOF getFrame(PVCoordinates other);

    /**
     * Get encounter local orbital frame associated to this enum.
     *
     * @param other other object {@link PVCoordinates position and velocity coordinates} that is not the origin of the
     * encounter frame
     * @param <T> type of the field elements
     *
     * @return encounter local orbital frame associated to this enum
     */
    public abstract <T extends CalculusFieldElement<T>> EncounterLOF getFrame(FieldPVCoordinates<T> other);
}
