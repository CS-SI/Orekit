/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/**
 * Atmospheric drag contribution for {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator}.
 * <p>
 * The drag acceleration is computed as follows:<br>
 * &gamma; = (1/2 &rho; C<sub>D</sub> A<sub>Ref</sub> / m) * |v<sub>atm</sub> - v<sub>sat</sub>| *
 * (v<sub>atm</sub> - v<sub>sat</sub>)
 * </p>
 *
 * @author Pascal Parraud
 */
public class DSSTAtmosphericDrag extends AbstractDSSTGaussianContribution {

    // Quadrature parameters
    /** Number of points desired for quadrature (must be between 2 and 5 inclusive). */
    private static final int[]    NB_POINTS         = {
        5, 5, 5, 5, 5, 5
    };

    /** Relative accuracy of the result. */
    private static final double[] RELATIVE_ACCURACY = {
        1.e-3, 1.e-3, 1.e-3, 1.e-3, 1.e-3, 1.e-3
    };

    /** Absolute accuracy of the result. */
    private static final double[] ABSOLUTE_ACCURACY = {
        1.e-20, 1.e-20, 1.e-20, 1.e-20, 1.e-20, 1.e-20
    };

    /** Maximum number of evaluations. */
    private static final int[]    MAX_EVAL          = {
        1000000, 1000000, 1000000, 1000000, 1000000, 1000000
    };

    /** Atmospheric model. */
    private final Atmosphere      atmosphere;

    /** Coefficient 1/2 * C<sub>D</sub> * A<sub>Ref</sub>. */
    private final double          kRef;

    /** Critical distance from the center of the central body for entering/leaving the atmosphere. */
    private double                rbar;

    /** Cross sectionnal area of satellite. */
    private final double          area;

    /** Simple constructor.
     * @param atmosphere atmospheric model
     * @param cd drag coefficient
     * @param area cross sectionnal area of satellite
     */
    public DSSTAtmosphericDrag(final Atmosphere atmosphere, final double cd, final double area) {
        this.atmosphere = atmosphere;
        this.area = area;
        this.kRef = 0.5 * cd * area;
        this.rbar = Double.NEGATIVE_INFINITY;
    }

    /** Get the critical distance.
     * @return the critical distance from the center of the central body
     */
    public double getRbar() {
        return rbar;
    }

    /** Get the atmospheric model.
     * @return atmosphere model
     */
    public final Atmosphere getAtmosphere() {
        return atmosphere;
    }

    /** Set the critical distance.
     * The critical distance from the center of the central body aims at
     * defining the atmosphere entry/exit.
     * @param rbar the critical distance to set
     */
    public void setRbar(final double rbar) {
        this.rbar = rbar;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet
        // Short Periodic Variations are set to null
        return new double[] {
            0., 0., 0., 0., 0., 0.
        };
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
        final double[] ll = {
            -FastMath.PI, FastMath.PI
        };
        final double r = state.getOrbit().getPVCoordinates().getPosition().getNorm();
        // TODO : to be validated
        if (r < rbar) {
            final double a = state.getA();
            final double e = state.getE();
            final double w = ((KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state.getOrbit())).getPerigeeArgument();
            final double W = ((KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state.getOrbit())).getRightAscensionOfAscendingNode();
            final double fb = FastMath.acos(((a * (1. - e * e) / rbar) - 1.) / e);
            final double wW = w + getRetrogradeFactor() * W;
            ll[0] = -fb + wW;
            ll[1] = fb + wW;
        }
        return ll;
    }

    /** {@inheritDoc} */
    protected int getNbPoints(final int element) {
        return NB_POINTS[element];
    }

    /** {@inheritDoc} */
    protected double getRelativeAccuracy(final int element) {
        return RELATIVE_ACCURACY[element];
    }

    /** {@inheritDoc} */
    protected double getAbsoluteAccuracy(final int element) {
        return ABSOLUTE_ACCURACY[element];
    }

    /** {@inheritDoc} */
    protected int getMaxEval(final int element) {
        return MAX_EVAL[element];
    }

    /** Get the cross sectional area of satellite.
     * @return cross sectional area
     */
    public double getArea() {
        return area;
    }

    /** Get the drag coefficient.
     * @return drag coefficient
     */
    public final double getCd() {
        return 2 * kRef / area;
    }

    /** {@inheritDoc} */
    public void initialize(final SpacecraftState initialState) {
        // Nothing to do
    }

}
