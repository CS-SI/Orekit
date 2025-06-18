/* Copyright 2020-2025 Exotrail
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
package org.orekit.control.heuristics.lambert;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
 * Class holding values defining the boundary conditions to a Lambert arc.
 *
 * @author Romain Serra
 * @since 13.1
 */
public class LambertBoundaryConditions {

    /** Initial date. */
    private final AbsoluteDate initialDate;

    /** Initial position vector. */
    private final Vector3D initialPosition;

    /** Terminal date. */
    private final AbsoluteDate terminalDate;

    /** Terminal position vector. */
    private final Vector3D terminalPosition;

    /** Reference frame. */
    private final Frame referenceFrame;

    /**
     * Constructor.
     * @param initialDate initial date
     * @param initialPosition initial position vector
     * @param terminalDate terminal date
     * @param terminalPosition terminal position vector
     * @param referenceFrame reference frame
     */
    public LambertBoundaryConditions(final AbsoluteDate initialDate, final Vector3D initialPosition,
                                     final AbsoluteDate terminalDate, final Vector3D terminalPosition,
                                     final Frame referenceFrame) {
        this.initialDate = initialDate;
        this.initialPosition = initialPosition;
        this.terminalDate = terminalDate;
        this.terminalPosition = terminalPosition;
        this.referenceFrame = referenceFrame;
    }

    /**
     * Getter for the terminal position vector.
     * @return terminal position
     */
    public Vector3D getTerminalPosition() {
        return terminalPosition;
    }

    /**
     * Getter for the terminal date.
     * @return terminal date
     */
    public AbsoluteDate getTerminalDate() {
        return terminalDate;
    }

    /**
     * Getter for the initial position vector.
     * @return initial position
     */
    public Vector3D getInitialPosition() {
        return initialPosition;
    }

    /**
     * Getter for the initial date.
     * @return initial date
     */
    public AbsoluteDate getInitialDate() {
        return initialDate;
    }

    /**
     * Getter for the reference frame.
     * @return frame
     */
    public Frame getReferenceFrame() {
        return referenceFrame;
    }
}
