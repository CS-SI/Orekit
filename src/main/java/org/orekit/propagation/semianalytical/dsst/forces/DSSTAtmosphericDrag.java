/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** Atmospheric drag contribution to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *  <p>
 *  The drag acceleration is computed as follows:<br>
 *  &gamma; = (1/2 &rho; C<sub>D</sub> A<sub>Ref</sub> / m) * |v<sub>atm</sub> - v<sub>sat</sub>| *
 *  (v<sub>atm</sub> - v<sub>sat</sub>)
 *  </p>
 *
 * @author Pascal Parraud
 */
public class DSSTAtmosphericDrag extends AbstractGaussianContribution {

    /** Threshold for the choice of the Gauss quadrature order. */
    private static final double GAUSS_THRESHOLD = 6.0e-10;

    /** Upper limit for atmospheric drag (m) . */
    private static final double ATMOSPHERE_ALTITUDE_MAX = 1000000.;

    /** Atmospheric model. */
    private final Atmosphere atmosphere;

    /** Cross sectionnal area of satellite. */
    private final double     area;

    /** Coefficient 1/2 * C<sub>D</sub> * A<sub>Ref</sub>. */
    private final double     kRef;

    /** Critical distance from the center of the central body for entering/leaving the atmosphere. */
    private final double     rbar;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param cd drag coefficient
     * @param area cross sectionnal area of satellite
     */
    public DSSTAtmosphericDrag(final Atmosphere atmosphere, final double cd, final double area) {
        super(GAUSS_THRESHOLD);
        this.atmosphere = atmosphere;
        this.area = area;
        this.kRef = 0.5 * cd * area;
        this.rbar = ATMOSPHERE_ALTITUDE_MAX + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
    }

    /** Get the atmospheric model.
     * @return atmosphere model
     */
    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    /** Get the cross sectional area of satellite.
     * @return cross sectional area (m<sup>2</sup>)
     */
    public double getArea() {
        return area;
    }

    /** Get the drag coefficient.
     *  @return drag coefficient
     */
    public double getCd() {
        return 2 * kRef / area;
    }

    /** Get the critical distance.
     *  <p>
     *  The critical distance from the center of the central body aims at
     *  defining the atmosphere entry/exit.
     *  </p>
     *  @return the critical distance from the center of the central body (m)
     */
    public double getRbar() {
        return rbar;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet, Short Periodic Variations are set to null
        return new double[] {0., 0., 0., 0., 0., 0.};
    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** {@inheritDoc} */
    protected Vector3D getAcceleration(final SpacecraftState state,
                                       final Vector3D position, final Vector3D velocity)
        throws OrekitException {
        final AbsoluteDate date = state.getDate();
        final Frame frame = state.getFrame();
        // compute atmospheric density (assuming it doesn't depend on the date)
        final double rho = atmosphere.getDensity(date, position, frame);
        // compute atmospheric velocity (assuming it doesn't depend on the date)
        final Vector3D vAtm = atmosphere.getVelocity(date, position, frame);
        // compute relative velocity
        final Vector3D vRel = vAtm.subtract(velocity);
        // compute compound drag coefficient
        final double bc = kRef / state.getMass();
        // compute drag acceleration
        return new Vector3D(bc * rho * vRel.getNorm(), vRel);
    }

    /** {@inheritDoc} */
    protected double[] getLLimits(final SpacecraftState state) throws OrekitException {
        final double perigee = a * (1. - ecc);
        // Trajectory entirely out of the atmosphere
        if (perigee > rbar) {
            return new double[2];
        }
        final double apogee  = a * (1. + ecc);
        // Trajectory entirely within of the atmosphere
        if (apogee < rbar) {
            return new double[] {-FastMath.PI, FastMath.PI};
        }
        // Else, trajectory partialy within of the atmosphere
        final double fb = FastMath.acos(((a * (1. - ecc * ecc) / rbar) - 1.) / ecc);
        final double wW = FastMath.atan2(h, k);
        return new double[] {wW - fb, wW + fb};
    }

}
