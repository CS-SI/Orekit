/* Copyright 2002-2020 CS GROUP
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
package org.orekit.forces.radiation;

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.analysis.polynomials.PolynomialsUtils;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

/** The Knocke Earth Albedo and IR emission force model.
 * <p>
 * This model is based on "EARTH RADIATION PRESSURE EFFECTS ON SATELLITES", 1988, by P. C. Knocke, J. C. Ries, and B. D. Tapley.
 * </p> <p>
 * This model represents the effects fo radiation pressure coming from the Earth.
 * It considers Solar radiation which have been reflected by Earth (albedo) and Earth infrared emissions.
 * The planet is considered as a sphere and is divided into segments.
 * Each segment is considered as a plane which emits radiation according to Lambert's law.
 * The flux the satellite receives is then equal to the sum of the elementary fluxes coming from Earth.
 * </p> <p>
 * The radiative model of the satellite, and its ability to diffuse, reflect  or absorb radiation is handled
 * by a {@link RadiationSensitive radiation sensitive model}.
 * </p>
 * @author Thomas Paulet
 */


public class KnockeRediffusedForceModel extends AbstractRediffusedForceModel {

    /** Coefficient for solar irradiance computation. */
    private static final double ES_COEFF = 4.5606E-6;

    /** First coefficient for albedo computation. */
    private static final double A0 = 0.34;

    /** Second coefficient for albedo computation. */
    private static final double C0 = 0.;

    /** Third coefficient for albedo computation. */
    private static final double C1 = 0.10;

    /** Fourth coefficient for albedo computation. */
    private static final double C2 = 0.;

    /** Fifth coefficient for albedo computation. */
    private static final double A2 = 0.29;

    /** First coefficient for Earth emissivity computation. */
    private static final double E0 = 0.68;

    /** Second coefficient for Earth emissivity computation. */
    private static final double K0 = 0.;

    /** Third coefficient for Earth emissivity computation. */
    private static final double K1 = -0.07;

    /** Fourth coefficient for Earth emissivity computation. */
    private static final double K2 = 0.;

    /** Fifth coefficient for Earth emissivity computation. */
    private static final double E2 = -0.18;

    /** Reference date for periodic terms: December 22nd 1981. */
    private static final AbsoluteDate T0 = new AbsoluteDate(1981, 12, 22, 0, 0, 0., TimeScalesFactory.getUTC());

    /** Margin to prevent Albedo computation while satellite is in umbra. */
    private static final double ANGULAR_MARGIN = 1.0e-10;

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Central body model. */
    private final double equatorialRadius;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;


    /** Constructor.
     * @param sun Sun model
     * @param equatorialRadius Central Body model
     * @param spacecraft Spacecraft
     */
    public KnockeRediffusedForceModel (final ExtendedPVCoordinatesProvider sun,
                                       final double equatorialRadius,
                                       final RadiationSensitive spacecraft) {
        super();
        this.sun              = sun;
        this.equatorialRadius = equatorialRadius;
        this.spacecraft       = spacecraft;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s,
                                 final double[] parameters) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return spacecraft.getRadiationParametersDrivers();
    }

    /** Compute Earth albedo.
     * Albedo value represents the fraction of solar radiative flux that is reflected by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @return the albedo in [0;1]
     */
    private static double computeAlbedo(final AbsoluteDate date, final double phi) {

        // Get duration since coefficient reference epoch
        final double deltaT = date.durationFrom(T0);

        // Compute 1rst Legendre polynomial coeficient
        final double A1 = C0 +
                          C1 * FastMath.cos(Constants.IERS96_EARTH_ANGULAR_VELOCITY * deltaT) +
                          C2 * FastMath.sin(Constants.IERS96_EARTH_ANGULAR_VELOCITY * deltaT);

        // Get 1rst and 2nd order Legendre polynomials
        final PolynomialFunction firstLegendrePolynomial  = PolynomialsUtils.createLegendrePolynomial(1);
        final PolynomialFunction secondLegendrePolynomial = PolynomialsUtils.createLegendrePolynomial(2);

        // Get latitude sinus
        final double sinPhi = FastMath.sin(phi);

        // Compute albedo
        return A0 +
               A1 * firstLegendrePolynomial.value(sinPhi) +
               A2 * secondLegendrePolynomial.value(sinPhi);

    }

    /** Compute Earth emisivity.
     * Emissivity is used to compute the infrared flux that is emited by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @return the emissivity in [0;1]
     */
    private static double computeEmissivity(final AbsoluteDate date, final double phi) {

        // Get duration since coefficient reference epoch
        final double deltaT = date.durationFrom(T0);

        // Compute 1rst Legendre polynomial coeficient
        final double E1 = K0 +
                          K1 * FastMath.cos(Constants.IERS96_EARTH_ANGULAR_VELOCITY * deltaT) +
                          K2 * FastMath.sin(Constants.IERS96_EARTH_ANGULAR_VELOCITY * deltaT);

        // Get 1rst and 2nd order Legendre polynomials
        final PolynomialFunction firstLegendrePolynomial  = PolynomialsUtils.createLegendrePolynomial(1);
        final PolynomialFunction secondLegendrePolynomial = PolynomialsUtils.createLegendrePolynomial(2);

        // Get latitude sinus
        final double sinPhi = FastMath.sin(phi);

        // Compute albedo
        return E0 +
               E1 * firstLegendrePolynomial.value(sinPhi) +
               E2 * secondLegendrePolynomial.value(sinPhi);

    }

    /** Compute total solar flux impacting Earth.
     * @param date the date
     * @param frame the Earth centered frame
     * @return the total solar flux impacting Earth in J/m^3
     */
    private double computeSolarFlux(final AbsoluteDate date, final Frame frame) {

        // Get Earth - Sun distance in UA
        final double earthSunDistance = sun.getPVCoordinates(date, frame).getPosition().getNorm() / Constants.JPL_SSD_ASTRONOMICAL_UNIT;

        // Compute Solar flux
        return ES_COEFF * Constants.SPEED_OF_LIGHT / (earthSunDistance * earthSunDistance);
    }
}
