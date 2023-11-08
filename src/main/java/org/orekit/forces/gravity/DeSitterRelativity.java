/* Contributed to the public domain
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

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.CelestialBody;
import org.orekit.data.DataContext;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

/**
 * De Sitter post-Newtonian correction force due to general relativity.
 * <p>
 * De Sitter term causes a precession of the orbital plane at a rate of 19 mas per year.
 * </p>
 * @see "Petit, G. and Luzum, B. (eds.), IERS Conventions (2010), Chapter 10,
 * General relativistic models for space-time coordinates and equations of motion (2010)"
 *
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class DeSitterRelativity implements ForceModel {

    /** Suffix for parameter name for attraction coefficient enabling Jacobian processing. */
    public static final String ATTRACTION_COEFFICIENT_SUFFIX = " attraction coefficient";

    /** Central attraction scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double MU_SCALE = FastMath.scalb(1.0, 32);

    /** The Sun. */
    private final CelestialBody sun;

    /** The Earth. */
    private final CelestialBody earth;

    /** Driver for gravitational parameter. */
    private final ParameterDriver gmParameterDriver;

    /**
     * Constructor.
     * <p>It uses the {@link DataContext#getDefault()} to initialize the celestial bodies.</p>
     */
    @DefaultDataContext
    public DeSitterRelativity() {
        this(DataContext.getDefault().getCelestialBodies().getEarth(),
             DataContext.getDefault().getCelestialBodies().getSun());
    }

    /**
     * Simple constructor.
     * @param earth the Earth
     * @param sun the Sun
     */
    public DeSitterRelativity(final CelestialBody earth, final CelestialBody sun) {
        gmParameterDriver = new ParameterDriver(sun.getName() + ThirdBodyAttraction.ATTRACTION_COEFFICIENT_SUFFIX,
                                                sun.getGM(), MU_SCALE,
                                                0.0, Double.POSITIVE_INFINITY);
        this.earth = earth;
        this.sun   = sun;
    }

    /**
     * Get the sun model used to compute De Sitter effect.
     * @return the sun model
     */
    public CelestialBody getSun() {
        return sun;
    }

    /**
     * Get the Earth model used to compute De Sitter effect.
     * @return the earth model
     */
    public CelestialBody getEarth() {
        return earth;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // Useful constant
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

        // Sun's gravitational parameter
        final double gm = parameters[0];

        // Satellite velocity with respect to the Earth
        final PVCoordinates pvSat = s.getPVCoordinates();
        final Vector3D vSat = pvSat.getVelocity();

        // Coordinates of the Earth with respect to the Sun
        final PVCoordinates pvEarth = earth.getPVCoordinates(s.getDate(), sun.getInertiallyOrientedFrame());
        final Vector3D pEarth = pvEarth.getPosition();
        final Vector3D vEarth = pvEarth.getVelocity();

        // Radius
        final double r  = pEarth.getNorm();
        final double r3 = r * r * r;

        // Eq. 10.12
        return new Vector3D((-3.0 * gm) / (c2 * r3), vEarth.crossProduct(pEarth).crossProduct(vSat));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        // Useful constant
        final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;

        // Sun's gravitational parameter
        final T gm = parameters[0];

        // Satellite velocity with respect to the Earth
        final FieldPVCoordinates<T> pvSat = s.getPVCoordinates();
        final FieldVector3D<T> vSat = pvSat.getVelocity();

        // Coordinates of the Earth with respect to the Sun
        final FieldPVCoordinates<T> pvEarth = earth.getPVCoordinates(s.getDate(), sun.getInertiallyOrientedFrame());
        final FieldVector3D<T> pEarth = pvEarth.getPosition();
        final FieldVector3D<T> vEarth = pvEarth .getVelocity();

        // Radius
        final T r  = pEarth.getNorm();
        final T r3 = r.multiply(r).multiply(r);

        // Eq. 10.12
        return new FieldVector3D<>(gm.multiply(-3.0).divide(r3.multiply(c2)), vEarth.crossProduct(pEarth).crossProduct(vSat));
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(gmParameterDriver);
    }

}
