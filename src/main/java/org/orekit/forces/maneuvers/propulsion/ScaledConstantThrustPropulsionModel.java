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

package org.orekit.forces.maneuvers.propulsion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Thrust propulsion model with parameters (for estimation) represented by scale factors
 *  on the X, Y and Z axis of the spacecraft frame.
 * @author Maxime Journot
 * @since 10.2
 */
public class ScaledConstantThrustPropulsionModel extends AbstractConstantThrustPropulsionModel {

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

    /** Constructor with min/max deviation for the scale factors.
     * Typical usage is, for example, if you know that your propulsion system
     * usually has an error of less than 10% then set the min/max to respectively 0.9 and 1.1.
     * @param thrust the thrust (N)
     * @param isp the isp (s)
     * @param direction in spacecraft frame
     * @param name the name of the maneuver
     */
    public ScaledConstantThrustPropulsionModel(final double thrust,
                                               final double isp,
                                               final Vector3D direction,
                                               final String name) {
        super(thrust, isp, direction, name);

        // Build the parameter drivers, using maneuver name as prefix
        this.scaleFactorThrustXDriver   = new ParameterDriver(name + THRUSTX_SCALE_FACTOR, 1., THRUST_SCALE,
                                                              Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.scaleFactorThrustYDriver   = new ParameterDriver(name + THRUSTY_SCALE_FACTOR, 1., THRUST_SCALE,
                                                              Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.scaleFactorThrustZDriver   = new ParameterDriver(name + THRUSTZ_SCALE_FACTOR, 1., THRUST_SCALE,
                                                              Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
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
        return new Vector3D(getInitialThrustVector().getX() * scaleFactorX,
                            getInitialThrustVector().getY() * scaleFactorY,
                            getInitialThrustVector().getZ() * scaleFactorZ);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector() {
        // scaleFactorThruster must be drivers with only 1 one value driven
        return getThrustVector(scaleFactorThrustXDriver.getValue(),
                               scaleFactorThrustYDriver.getValue(),
                               scaleFactorThrustZDriver.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final AbsoluteDate date) {
        return getThrustVector(scaleFactorThrustXDriver.getValue(date),
                               scaleFactorThrustYDriver.getValue(date),
                               scaleFactorThrustZDriver.getValue(date));
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate() {
        return getInitialFlowRate();
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final AbsoluteDate date) {
        return getInitialFlowRate();
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(Arrays.asList(scaleFactorThrustXDriver,
                                                          scaleFactorThrustYDriver,
                                                          scaleFactorThrustZDriver));
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final double[] parameters) {
        return getThrustVector(parameters[0], parameters[1], parameters[2]);
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final double[] parameters) {
        return getInitialFlowRate();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final T[] parameters) {
        return new FieldVector3D<>(parameters[0].multiply(getInitialThrustVector().getX()),
                        parameters[1].multiply(getInitialThrustVector().getY()),
                        parameters[2].multiply(getInitialThrustVector().getZ()));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T getFlowRate(final T[] parameters) {
        return parameters[0].getField().getZero().add(getInitialFlowRate());
    }
}
