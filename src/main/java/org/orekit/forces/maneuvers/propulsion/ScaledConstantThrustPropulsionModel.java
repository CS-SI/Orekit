/* Copyright 2002-2020 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/** Thrust propulsion model with parameters (for estimation) represented by scale factors
 *  on the X, Y and Z axis of the spacecraft frame.
 * @author Maxime Journot
 */
public class ScaledConstantThrustPropulsionModel implements ThrustPropulsionModel {

    /** Parameter name for the scale factor on the X component of the thrust in S/C frame. */
    public static final String THRUSTX_SCALE_FACTOR = "TX scale factor";
    /** Parameter name for the scale factor on the Y component of the thrust in S/C frame. */
    public static final String THRUSTY_SCALE_FACTOR = "TY scale factor";
    /** Parameter name for the scale factor on the Z component of the thrust in S/C frame. */
    public static final String THRUSTZ_SCALE_FACTOR = "TZ scale factor";

    /** Thrust scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double THRUST_SCALE = FastMath.scalb(1.0, -5);

    /** Parameter driver for the scale factor on the X component of the thrust in S/C frame. */
    private final ParameterDriver scaleFactorThrustXDriver;
    /** Parameter driver for the scale factor on the Y component of the thrust in S/C frame. */
    private final ParameterDriver scaleFactorThrustYDriver;
    /** Parameter driver for the scale factor on the Z component of the thrust in S/C frame. */
    private final ParameterDriver scaleFactorThrustZDriver;

    /** Constant flow rate value (kg/s). */
    private final double flowRate;

    /** Initial thrust vector in S/C frame (N). */
    private final Vector3D initialThrustVector;

    /** User-defined name of the maneuver.
     * This String attribute is empty by default.
     * It is added as a prefix to the parameter drivers of the maneuver.
     * The purpose is to differentiate between drivers in the case where several maneuvers
     * were added to a propagator force model.
     * Additionally, the user can retrieve the whole maneuver by looping on the force models of a propagator,
     * scanning for its name.
     */
    private final String name;

    /** Constructor with infinite possible deviation for the scale factors.
     * @param thrust the thrust (N)
     * @param isp the isp (s)
     * @param direction in spacecraft frame
     * @param name the name of the maneuver
     */
    public ScaledConstantThrustPropulsionModel(final double thrust,
                                               final double isp,
                                               final Vector3D direction,
                                               final String name) {
        this(thrust, isp, direction, name, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Constructor with min/max deviation for the scale factors.
     * Typical usage is, for example, if you know that your propulsion system
     * usually has an error of less than 10% then set the min/max to respectively 0.9 and 1.1.
     * @param thrust the thrust (N)
     * @param isp the isp (s)
     * @param direction in spacecraft frame
     * @param name the name of the maneuver
     * @param minScaleFactor minimum value for scale factor on X, Y and Z axis
     * @param maxScaleFactor maximum value for scale factor on X, Y and Z axis
     */
    public ScaledConstantThrustPropulsionModel(final double thrust,
                                               final double isp,
                                               final Vector3D direction,
                                               final String name,
                                               final double minScaleFactor,
                                               final double maxScaleFactor) {
        this.name = name;
        this.flowRate = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);
        this.initialThrustVector = direction.normalize().scalarMultiply(thrust);

        // Build the parameter drivers, using maneuver name as prefix
        ParameterDriver scaleFactorThrustXD = null;
        ParameterDriver scaleFactorThrustYD = null;
        ParameterDriver scaleFactorThrustZD = null;

        try {
            scaleFactorThrustXD = new ParameterDriver(name + THRUSTX_SCALE_FACTOR, 1., THRUST_SCALE,
                                                      minScaleFactor, maxScaleFactor);
            scaleFactorThrustYD = new ParameterDriver(name + THRUSTY_SCALE_FACTOR, 1., THRUST_SCALE,
                                                      minScaleFactor, maxScaleFactor);
            scaleFactorThrustZD = new ParameterDriver(name + THRUSTZ_SCALE_FACTOR, 1., THRUST_SCALE,
                                                      minScaleFactor, maxScaleFactor);
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        }

        this.scaleFactorThrustXDriver   = scaleFactorThrustXD;
        this.scaleFactorThrustYDriver   = scaleFactorThrustYD;
        this.scaleFactorThrustZDriver   = scaleFactorThrustZD;
    }

    /** Get the thrust vector in S/C frame from scale factors (N).
     * @param scaleFactorX thrust vector scale factor on X axis of S/C frame
     * @param scaleFactorY thrust vector scale factor on Y axis of S/C frame
     * @param scaleFactorZ thrust vector scale factor on Z axis of S/C frame
     * @return thrust vector in S/C frame
     */
    private Vector3D getThrustVector(final double scaleFactorX,
                                     final double scaleFactorY,
                                     final double scaleFactorZ) {
        return new Vector3D(initialThrustVector.getX() * scaleFactorX,
                            initialThrustVector.getY() * scaleFactorY,
                            initialThrustVector.getZ() * scaleFactorZ);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final SpacecraftState s) {
        return getThrustVector(scaleFactorThrustXDriver.getValue(),
                               scaleFactorThrustYDriver.getValue(),
                               scaleFactorThrustZDriver.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final SpacecraftState s) {
        return flowRate;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {
            scaleFactorThrustXDriver, scaleFactorThrustYDriver, scaleFactorThrustZDriver
        };
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final SpacecraftState s, final double parameters[]) {
        return getThrustVector(parameters[0], parameters[1], parameters[2]);
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final SpacecraftState s, final double[] parameters) {
        return flowRate;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> getThrustVector(final FieldSpacecraftState<T> s,
                                                                            final T parameters[]) {
        return new FieldVector3D<T>(parameters[0].multiply(initialThrustVector.getX()),
                        parameters[1].multiply(initialThrustVector.getY()),
                        parameters[2].multiply(initialThrustVector.getZ()));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T getFlowRate(final FieldSpacecraftState<T> s,
                                                         final T[] parameters) {
        return parameters[0].getField().getZero().add(flowRate);
    }
}
