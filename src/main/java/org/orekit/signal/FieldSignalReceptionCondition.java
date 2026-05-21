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
 * Data container describing signal reception conditions.
 *
 * @param receptionDate    Signal reception date.
 * @param receiverPosition Receiver position's vector at signal reception.
 * @param referenceFrame   Frame where position is given.
 * @author Romain Serra
 * @see SignalReceptionCondition
 * @since 14.0
 */
public record FieldSignalReceptionCondition<T extends CalculusFieldElement<T>>(FieldAbsoluteDate<T> receptionDate,
                                                                               FieldVector3D<T> receiverPosition,
                                                                               Frame referenceFrame) {

    /**
     * Constructor from non-Field values.
     *
     * @param field                    field
     * @param signalReceptionCondition non-field reception condition
     */
    public FieldSignalReceptionCondition(final Field<T> field, final SignalReceptionCondition signalReceptionCondition) {
        this(new FieldAbsoluteDate<>(field, signalReceptionCondition.receptionDate()),
                new FieldVector3D<>(field, signalReceptionCondition.receiverPosition()),
                signalReceptionCondition.referenceFrame());
    }

    /**
     * Method returning a non-field reception condition.
     *
     * @return non-field version
     */
    public SignalReceptionCondition toReceptionCondition() {
        return new SignalReceptionCondition(receptionDate().toAbsoluteDate(), receiverPosition().toVector3D(),
                referenceFrame());
    }
}
