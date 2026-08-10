/* Copyright 2022-2026 Romain Serra
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
package org.orekit.signal;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Data container describing signal emission conditions.
 *
 * @param emissionDate    Signal emission date.
 * @param emitterPosition Receiver position's vector at signal emission.
 * @param referenceFrame  Frame where position is given.
 * @param <T> type of the field element
 * @author Romain Serra
 * @see SignalEmissionCondition
 * @since 14.0
 */
public record FieldSignalEmissionCondition<T extends CalculusFieldElement<T>>(FieldAbsoluteDate<T> emissionDate,
                                                                              FieldVector3D<T> emitterPosition,
                                                                              Frame referenceFrame) {

    /**
     * Constructor from non-Field values.
     *
     * @param field                   field
     * @param signalEmissionCondition non-field emission condition
     */
    public FieldSignalEmissionCondition(final Field<T> field, final SignalEmissionCondition signalEmissionCondition) {
        this(new FieldAbsoluteDate<>(field, signalEmissionCondition.emissionDate()),
                new FieldVector3D<>(field, signalEmissionCondition.emitterPosition()),
                signalEmissionCondition.referenceFrame());
    }

    /**
     * Method returning a non-field emission condition.
     *
     * @return non-field version
     */
    public SignalEmissionCondition toEmissionCondition() {
        return new SignalEmissionCondition(emissionDate().toAbsoluteDate(), emitterPosition().toVector3D(),
                referenceFrame());
    }
}
