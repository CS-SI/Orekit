/* Copyright 2022-2024 Romain Serra
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
package org.orekit.forces.gravity;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.Collections;
import java.util.List;

/** Abstract class for non-central body attraction force model.
 *
 * @author Romain Serra
 */
public abstract class AbstractBodyAttraction implements ForceModel {

    /** Suffix for parameter name for attraction coefficient enabling Jacobian processing. */
    public static final String ATTRACTION_COEFFICIENT_SUFFIX = " attraction coefficient";

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** The body to consider. */
    private final CelestialBody body;

    /** Drivers for body attraction coefficient. */
    private final ParameterDriver gmParameterDriver;

    /** Simple constructor.
     * @param body the third body to consider
     */
    protected AbstractBodyAttraction(final CelestialBody body) {
        this.body = body;
        this.gmParameterDriver = new ParameterDriver(body.getName() + ATTRACTION_COEFFICIENT_SUFFIX,
                body.getGM(), MU_SCALE, 0.0, Double.POSITIVE_INFINITY);
    }

    /** Getter for the body's name.
     * @return the body's name
     */
    public String getBodyName() {
        return body.getName();
    }

    /** Protected getter for the body.
     * @return the third body considered
     * @deprecated in next major release this shall be removed as the class will not have to use a CelestialBody
     */
    @Deprecated
    protected CelestialBody getBody() {
        return body;
    }

    /**
     * Get the body's position vector.
     * @param date date
     * @param frame frame
     * @return position
     * @since 12.2
     */
    protected Vector3D getBodyPosition(final AbsoluteDate date, final Frame frame) {
        return body.getPosition(date, frame);
    }

    /**
     * Get the body's position vector.
     * @param date date
     * @param frame frame
     * @param <T> field type
     * @return position
     * @since 12.2
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getBodyPosition(final FieldAbsoluteDate<T> date,
                                                                                   final Frame frame) {
        return body.getPosition(date, frame);
    }

    /**
     * Get the body's position-velocity-acceleration vector.
     * @param date date
     * @param frame frame
     * @return PV
     * @since 12.2
     */
    protected TimeStampedPVCoordinates getBodyPVCoordinates(final AbsoluteDate date, final Frame frame) {
        return body.getPVCoordinates(date, frame);
    }

    /**
     * Get the body's position-velocity-acceleration vector.
     * @param date date
     * @param frame frame
     * @param <T> field type
     * @return PV
     * @since 12.2
     */
    protected <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getBodyPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                        final Frame frame) {
        return body.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(gmParameterDriver);
    }
}
