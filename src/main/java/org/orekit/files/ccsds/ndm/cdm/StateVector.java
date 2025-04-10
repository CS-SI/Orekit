/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.ccsds.ndm.cdm;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.files.ccsds.ndm.odm.StateVectorKey;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for state vector data.
 * <p>
 * Beware that the Orekit getters and setters all rely on SI units. The parsers
 * and writers take care of converting these SI units into CCSDS mandatory units.
 * The {@link org.orekit.utils.units.Unit Unit} class provides useful
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} methods in case the callers
 * already use CCSDS units instead of the API SI units. The general-purpose
 * {@link org.orekit.utils.units.Unit Unit} class (without an 's') and the
 * CCSDS-specific {@link org.orekit.files.ccsds.definitions.Units Units} class
 * (with an 's') also provide some predefined units. These predefined units and the
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} conversion methods are indeed
 * what the parsers and writers use for the conversions.
 * </p>
 * @author Melina Vanel
 * @since 11.2
 */
public class StateVector extends CommentsContainer {

    /** Object Position Vector X component. */
    private double x;

    /** Object Position Vector Y component. */
    private double y;

    /** Object Position Vector Z component. */
    private double z;

    /** Object Velocity Vector X component. */
    private double xDot;

    /** Object Velocity Vector Y component. */
    private double yDot;

    /** Object Velocity Vector Z component. */
    private double zDot;

    /** Simple constructor.
     */
    public StateVector() {
        x         = Double.NaN;
        y         = Double.NaN;
        z         = Double.NaN;
        xDot      = Double.NaN;
        yDot      = Double.NaN;
        zDot      = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNaN(x, StateVectorKey.X.name());
        checkNotNaN(y, StateVectorKey.Y.name());
        checkNotNaN(z, StateVectorKey.Z.name());
        checkNotNaN(xDot, StateVectorKey.X_DOT.name());
        checkNotNaN(yDot, StateVectorKey.Y_DOT.name());
        checkNotNaN(zDot, StateVectorKey.Z_DOT.name());

    }

    /**
     * Set object Position Vector X component.
     * @param X object Position Vector X component (in m)
     */
    public void setX(final double X) {
        refuseFurtherComments();
        this.x = X;
    }

    /**
     * Set object Position Vector Y component.
     * @param Y object Position Vector Y component (in m)
     */
    public void setY(final double Y) {
        refuseFurtherComments();
        this.y = Y;
    }

    /**
     * Set object Position Vector Z component.
     * @param Z object Position Vector Z component (in m)
     */
    public void setZ(final double Z) {
        refuseFurtherComments();
        this.z = Z;
    }

    /**
     * Set object Velocity Vector X component.
     * @param Xdot object Velocity Vector X component (in m/s)
     */
    public void setXdot(final double Xdot) {
        refuseFurtherComments();
        this.xDot = Xdot;
    }

    /**
     * Set object Velocity Vector Y component.
     * @param Ydot object Velocity Vector Y component (in m/s)
     */
    public void setYdot(final double Ydot) {
        refuseFurtherComments();
        this.yDot = Ydot;
    }

    /**
     * Set object Velocity Vector Z component.
     * @param Zdot object Velocity Vector Z component (in m/s)
     */
    public void setZdot(final double Zdot) {
        refuseFurtherComments();
        this.zDot = Zdot;
    }

    /**
     * Get object Position Vector.
     * @return object Position Vector (in m)
     */
    public Vector3D getPositionVector() {
        return new Vector3D(x, y, z);
    }

    /**
     * Get object Velocity Vector.
     * @return object Velocity Vector (in m/s)
     */
    public Vector3D getVelocityVector() {
        return new Vector3D(xDot, yDot, zDot);
    }


}
