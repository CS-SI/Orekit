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
 * @since 14.0
 * @author Romain Serra
 * @see SignalEmissionCondition
 */
public class FieldSignalEmissionCondition<T extends CalculusFieldElement<T>> {

    /** Signal emission date. */
    private final FieldAbsoluteDate<T> emissionDate;

    /** Receiver position's vector at signal emission. */
    private final FieldVector3D<T> emitterPosition;

    /** Frame where position is given. */
    private final Frame referenceFrame;

    /**
     * Constructor.
     * @param emissionDate emission date
     * @param emitterPosition emitter position
     * @param referenceFrame frame where position is given
     */
    public FieldSignalEmissionCondition(final FieldAbsoluteDate<T> emissionDate, final FieldVector3D<T> emitterPosition,
                                        final Frame referenceFrame) {
        this.emissionDate = emissionDate;
        this.emitterPosition = emitterPosition;
        this.referenceFrame = referenceFrame;
    }

    /**
     * Constructor from non-Field values.
     * @param field field
     * @param signalEmissionCondition non-field emission condition
     */
    public FieldSignalEmissionCondition(final Field<T> field, final SignalEmissionCondition signalEmissionCondition) {
        this(new FieldAbsoluteDate<>(field, signalEmissionCondition.getEmissionDate()),
                new FieldVector3D<>(field, signalEmissionCondition.getEmitterPosition()),
                signalEmissionCondition.getReferenceFrame());
    }

    /**
     * Method returning a non-field emission condition.
     * @return non-field version
     */
    public SignalEmissionCondition toEmissionCondition() {
        return new SignalEmissionCondition(getEmissionDate().toAbsoluteDate(), getEmitterPosition().toVector3D(),
                getReferenceFrame());
    }

    public FieldVector3D<T> getEmitterPosition() {
        return emitterPosition;
    }

    public FieldAbsoluteDate<T> getEmissionDate() {
        return emissionDate;
    }

    public Frame getReferenceFrame() {
        return referenceFrame;
    }
}
