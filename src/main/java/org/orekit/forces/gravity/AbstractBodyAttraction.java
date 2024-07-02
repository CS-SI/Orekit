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

import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.forces.ForceModel;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;

/** Abstract class for body attraction force model.
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
     */
    protected CelestialBody getBody() {
        return body;
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
