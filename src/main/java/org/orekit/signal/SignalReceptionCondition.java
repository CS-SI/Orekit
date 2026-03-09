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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
 * Data container describing signal reception conditions.
 * @since 14.0
 * @author Romain Serra
 */
public class SignalReceptionCondition {

    /** Signal reception date. */
    private final AbsoluteDate receptionDate;

    /** Receiver position's vector at signal reception. */
    private final Vector3D receiverPosition;

    /** Frame where position is given. */
    private final Frame referenceFrame;

    /**
     * Constructor.
     * @param receptionDate reception date
     * @param receiverPosition receiver position
     * @param referenceFrame frame where position is given
     */
    public SignalReceptionCondition(final AbsoluteDate receptionDate, final Vector3D receiverPosition,
                                    final Frame referenceFrame) {
        this.receptionDate = receptionDate;
        this.receiverPosition = receiverPosition;
        this.referenceFrame = referenceFrame;
    }

    public Vector3D getReceiverPosition() {
        return receiverPosition;
    }

    public AbsoluteDate getReceptionDate() {
        return receptionDate;
    }

    public Frame getReferenceFrame() {
        return referenceFrame;
    }
}
