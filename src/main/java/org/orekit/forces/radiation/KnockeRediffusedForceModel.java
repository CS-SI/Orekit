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

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Central body model. */
    private final double equatorialRadius;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** Angular equatorial latitude separation for surface element computation in rad. */
    private final double dLatitude;

    /** Angular longitude separation for surface element computation in rad. */
    private final double dLongitude;

    /** Constructor.
     * @param sun Sun model
     * @param equatorialRadius Central Body model
     * @param spacecraft Spacecraft
     * @param numberOfLatitudeSlices number of latitude slices for elementary area cut
     * @param numberOfLongitudeSlices number of latitude slices for elementary area cut
     */
    public KnockeRediffusedForceModel (final ExtendedPVCoordinatesProvider sun,
                                       final double equatorialRadius,
                                       final RadiationSensitive spacecraft,
                                       final int numberOfLatitudeSlices,
                                       final int numberOfLongitudeSlices) {
        this.sun              = sun;
        this.equatorialRadius = equatorialRadius;
        this.spacecraft       = spacecraft;
        this.dLatitude        = FastMath.PI / numberOfLatitudeSlices;
        this.dLongitude       = 2 * FastMath.PI / numberOfLongitudeSlices;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s,
                                 final double[] parameters) {

        // Initialize latitude and longitude
        double currentLatitude  = FastMath.PI - dLatitude / 2;
        double currentLongitude = -dLongitude / 2;

        // Get date
        final AbsoluteDate date = s.getDate();

        // Get frame
        final Frame frame = s.getFrame();

        // Get satellite position
        final Vector3D satellitePosition = s.getPVCoordinates().getPosition();
        // Initialize rediffused flux
        Vector3D rediffusedFlux = new Vector3D(0.0, 0.0, 0.0);

        // Slice spherical Earth into elementary areas
        while (currentLatitude + dLatitude <= FastMath.PI) {

            // Next latitude portion
            currentLatitude += dLatitude;

            while (currentLongitude + dLongitude <= 2 * FastMath.PI) {

                // Next longitude portion
                currentLongitude += dLongitude;

                // Compute Earth element projected area vector
                final Vector3D projectedAreaVector = computeProjectedAreaVector(satellitePosition,
                                                                                currentLatitude, currentLongitude,
                                                                                dLatitude, dLongitude);

                // Check if satellite sees the elementary area
                if (projectedAreaVector.getNorm() > 0.0) {

                    // Compute Earth emissivity and albedo
                    final double e = computeEmissivity(date, currentLatitude);

                    double a = 0.0;

                    // Check if elementary area is in day light
                    final double sunElevation = Vector3D.angle(computeSphericalElementaryVector(currentLatitude, currentLongitude),
                                                               sun.getPVCoordinates(date, frame).getPosition());

                    if (sunElevation < FastMath.PI / 2 ) {

                        // Elementary area is in day light
                        a = computeAlbedo(date, currentLatitude);
                    }

                    // Compute solar flux
                    final double solarFlux = computeSolarFlux(date, frame);

                    // Compute elementary area contribution to rediffused flux
                    final double albedoAndIR = a * solarFlux * FastMath.cos(sunElevation) +
                                               e * solarFlux / 4;
                    final Vector3D elementaryFlux = projectedAreaVector.scalarMultiply(albedoAndIR);

                    // Add elementary contribution to total rediffused flux
                    rediffusedFlux = rediffusedFlux.add(elementaryFlux);
                }
            }
        }

        return spacecraft.radiationPressureAcceleration(date, frame, satellitePosition, s.getAttitude().getRotation(),
                                                        s.getMass(), rediffusedFlux, parameters);
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

        // Compute Earth - Sun distance in UA
        final double earthSunDistance = sun.getPVCoordinates(date, frame).getPosition().getNorm() / Constants.JPL_SSD_ASTRONOMICAL_UNIT;

        // Compute Solar flux
        return ES_COEFF * Constants.SPEED_OF_LIGHT / (earthSunDistance * earthSunDistance);
    }

    /** Compute Earth element projected area vector.
     * @param satellitePosition satellite position with respect to Earth center
     * @param phi the surface element center equatorial latitude in rad
     * @param psi the surface element center longitude in rad
     * @param dPhi the equatorial latitude angular separation in rad
     * @param dPsi the longitude angular separation in rad
     * @return the Earth element projected area vector
     */
    private Vector3D computeProjectedAreaVector(final Vector3D satellitePosition,
                                       final double phi,
                                       final double psi,
                                       final double dPhi,
                                       final double dPsi) {

        // Get Earth suface element center
        final Vector3D elementaryAreaCenterPosition = computeSphericalElementaryVector(phi, psi).
                                                      scalarMultiply(equatorialRadius);

        // Compute Earth surface element center - satellite vector
        final Vector3D r = satellitePosition.subtract(elementaryAreaCenterPosition);

        // Get satellite elevation angle
        final double alpha = Vector3D.angle(elementaryAreaCenterPosition, r);

        // Check if satellite sees the surface element
        if (alpha < FastMath.PI / 2) {

            // Compute surface element area
            final double dA = equatorialRadius * equatorialRadius *
                                FastMath.cos(phi) * dPhi * dPsi;

            // Get Earth surface element center - satellite distance
            final double rNorm = r.getNorm();

            // Compute Earth element projected area vector
            return r.scalarMultiply(dA * FastMath.cos(alpha) / (FastMath.PI * rNorm * rNorm * rNorm));

        } else {
            return new Vector3D(0.0, 0.0, 0.0);
        }

    }

    /** Compute spherical elementary area vector.
     * @param phi the surface element center equatorial latitude in rad
     * @param psi the surface element center longitude in rad
     * @return the spherical elementary area vector
     */
    private Vector3D computeSphericalElementaryVector(final double phi,
                                                      final double psi) {

        // Get angle sinuses and cosinuses
        final double sinPhi = FastMath.sin(phi);
        final double cosPhi = FastMath.cos(phi);
        final double sinPsi = FastMath.sin(psi);
        final double cosPsi = FastMath.cos(psi);

        // Get Earth suface element center
        final double x = cosPhi * cosPsi;
        final double y = cosPhi * sinPsi;
        final double z = sinPhi;
        return new Vector3D(x, y, z);
    }
}
