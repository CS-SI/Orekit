/* Copyright 2002-2022 CS GROUP
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

import org.orekit.files.ccsds.ndm.odm.StateVectorKey;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for state vector data.
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
        checkNotNaN(x, StateVectorKey.X);
        checkNotNaN(y, StateVectorKey.Y);
        checkNotNaN(z, StateVectorKey.Z);
        checkNotNaN(xDot, StateVectorKey.X_DOT);
        checkNotNaN(yDot, StateVectorKey.Y_DOT);
        checkNotNaN(zDot, StateVectorKey.Z_DOT);

    }

    /**
     * Get object Position Vector X component.
     * @return object Position Vector X component
     */
    public double getX() {
        return x;
    }

    /**
     * Set object Position Vector X component.
     * @param X object Position Vector X component
     */
    public void setX(final double X) {
        refuseFurtherComments();
        this.x = X;
    }

    /**
     * Get object Position Vector Y component.
     * @return object Position Vector Y component
     */
    public double getY() {
        return y;
    }

    /**
     * Set object Position Vector Y component.
     * @param Y object Position Vector Y component
     */
    public void setY(final double Y) {
        refuseFurtherComments();
        this.y = Y;
    }

    /**
     * Get object Position Vector Z component.
     * @return object Position Vector Z component
     */
    public double getZ() {
        return z;
    }

    /**
     * Set object Position Vector Z component.
     * @param Z object Position Vector Z component
     */
    public void setZ(final double Z) {
        refuseFurtherComments();
        this.z = Z;
    }

    /**
     * Get object Velocity Vector X component.
     * @return object Velocity Vector X component
     */
    public double getXdot() {
        return xDot;
    }

    /**
     * Set object Velocity Vector X component.
     * @param Xdot object Velocity Vector X component
     */
    public void setXdot(final double Xdot) {
        refuseFurtherComments();
        this.xDot = Xdot;
    }

    /**
     * Get object Velocity Vector Y component.
     * @return object Velocity Vector Y component
     */
    public double getYdot() {
        return yDot;
    }

    /**
     * Set object Velocity Vector Y component.
     * @param Ydot object Velocity Vector Y component
     */
    public void setYdot(final double Ydot) {
        refuseFurtherComments();
        this.yDot = Ydot;
    }

    /**
     * Get object Velocity Vector Z component.
     * @return object Velocity Vector Z component
     */
    public double getZdot() {
        return zDot;
    }

    /**
     * Set object Velocity Vector Z component.
     * @param Zdot object Velocity Vector Z component
     */
    public void setZdot(final double Zdot) {
        refuseFurtherComments();
        this.zDot = Zdot;
    }


}
