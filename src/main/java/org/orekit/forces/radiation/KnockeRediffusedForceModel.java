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

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.analysis.polynomials.PolynomialsUtils;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.AbstractForceModel;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

/** The Knocke Earth Albedo and IR emission force model.
 * <p>
 * This model is based on "EARTH RADIATION PRESSURE EFFECTS ON SATELLITES", 1988, by P. C. Knocke, J. C. Ries, and B. D. Tapley.
 * </p> <p>
 * This model represents the effects fo radiation pressure coming from the Earth.
 * It considers Solar radiation which has been reflected by Earth (albedo) and Earth infrared emissions.
 * The planet is considered as a sphere and is divided into elementary areas.
 * Each elementary area is considered as a plane and emits radiation according to Lambert's law.
 * The flux the satellite receives is then equal to the sum of the elementary fluxes coming from Earth.
 * </p> <p>
 * The radiative model of the satellite, and its ability to diffuse, reflect  or absorb radiation is handled
 * by a {@link RadiationSensitive radiation sensitive model}.
 * </p> <p>
 * Caution! The spacecraft state must be defined in an Earth centered frame.
 * </p>
 *
 * @author Thomas Paulet
 */


public class KnockeRediffusedForceModel extends AbstractForceModel {

    /** Earth rotation around Sun pulsation in rad/sec. */
    private static final double EARTH_AROUND_SUN_PULSATION = 2 * FastMath.PI / Constants.JULIAN_YEAR;

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

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** Angular resolution for emissivity and albedo computation in rad. */
    private final double angularResolution;

    /** Earth equatorial radius in m. */
    private double equatorialRadius;

    /** Constructor.
     * @param sun Sun model
     * @param spacecraft Spacecraft
     * @param equatorialRadius the Earth equatorial radius in m
     * @param angularResolution angular resolution in rad
     */
    public KnockeRediffusedForceModel (final ExtendedPVCoordinatesProvider sun,
                                       final RadiationSensitive spacecraft,
                                       final double equatorialRadius,
                                       final double angularResolution) {
        this.sun               = sun;
        this.spacecraft        = spacecraft;
        this.equatorialRadius  = equatorialRadius;
        this.angularResolution = angularResolution;
    }


    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.of();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.of();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s,
                                 final double[] parameters) {

        // Get date
        final AbsoluteDate date = s.getDate();

        // Get frame
        final Frame frame = s.getFrame();

        // Get satellite position
        final Vector3D satellitePosition = s.getPVCoordinates().getPosition();

        // Get spherical Earth model
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(equatorialRadius, 0.0, frame);

        // Project satellite on Earth as geodetic point and vector
        final GeodeticPoint satelliteAsGeodeticPoint = earth.transform(satellitePosition, frame, date);
        final Vector3D projectedToGround = earth.projectToGround(satellitePosition, date, frame);

        // Get elementary vector east for Earth browsing using rotations
        final Vector3D east = satelliteAsGeodeticPoint.getEast();

        // Initialize rediffused flux with elementary flux coming from the circular area around the projected satellite
        final double centerArea = 2 * FastMath.PI * equatorialRadius * equatorialRadius *
                                 (1 - FastMath.cos(angularResolution));
        Vector3D rediffusedFlux = computeElementaryFlux(s, projectedToGround, earth, centerArea);

        // Sectorize the part of Earth which is seen by the satellite into crown sectors with constant angular resolution
        for (double eastAxisOffset = angularResolution * 3 / 2;
             eastAxisOffset < FastMath.asin(equatorialRadius / satellitePosition.getNorm());
             eastAxisOffset = eastAxisOffset + angularResolution) {

            // Build rotation transformations to get first crown elementary sector center
            final Transform eastRotation = new Transform(date,
                                                          new Rotation(east, eastAxisOffset, RotationConvention.VECTOR_OPERATOR));

            // Get first elementary crown sector center
            final Vector3D firstCrownSectorCenter = eastRotation.transformPosition(projectedToGround);

            // Browse the entire crown
            for (double radialAxisOffset =  angularResolution / 2;
                 radialAxisOffset < FastMath.PI * 2;
                 radialAxisOffset = radialAxisOffset + angularResolution) {

                // Build rotation transformations to get elementary area center
                final Transform radialRotation  = new Transform(date,
                                                                new Rotation(projectedToGround, radialAxisOffset, RotationConvention.VECTOR_OPERATOR));

                // Get current elementary crown sector center
                final Vector3D currentCenter = radialRotation.transformPosition(firstCrownSectorCenter);

                // Compute current elementary crown sector area, it results of the integration of an elementary crown sector
                // over the angular resolution
                final double sectorArea = equatorialRadius * equatorialRadius *
                                          2 * angularResolution * FastMath.sin(0.5 * angularResolution) * FastMath.sin(eastAxisOffset);

                // Add current sector contribution to total rediffused flux
                rediffusedFlux = rediffusedFlux.add(computeElementaryFlux(s, currentCenter, earth, sectorArea));
            }
        }
        return spacecraft.radiationPressureAcceleration(date, frame, satellitePosition, s.getAttitude().getRotation(),
                                                        s.getMass(), rediffusedFlux, parameters);
    }


    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {
        // Get date
        final FieldAbsoluteDate<T> date = s.getDate();

        // Get frame
        final Frame frame = s.getFrame();

        // Get zero
        final T zero = date.getField().getZero();

        // Get satellite position
        final FieldVector3D<T> satellitePosition = s.getPVCoordinates().getPosition();

        // Get spherical Earth model
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(equatorialRadius, 0.0, frame);

        // Project satellite on Earth as geodetic point and vector
        final FieldGeodeticPoint<T> satelliteAsGeodeticPoint = earth.transform(satellitePosition, frame, date);
        final FieldVector3D<T> projectedToGround = satellitePosition.normalize().scalarMultiply(equatorialRadius);

        // Get elementary vector east for Earth browsing using rotations
        final FieldVector3D<T> east = satelliteAsGeodeticPoint.getEast();

        // Initialize rediffused flux with elementary flux coming from the circular area around the projected satellite
        final T centerArea = zero.add(2 * FastMath.PI * equatorialRadius * equatorialRadius *
                                      (1 - FastMath.cos(angularResolution)));
        FieldVector3D<T> rediffusedFlux = computeElementaryFlux(s, projectedToGround, earth, centerArea);

        // Sectorize the part of Earth which is seen by the satellite into crown sectors with constant angular resolution
        for (double eastAxisOffset = angularResolution * 3 / 2;
             eastAxisOffset < FastMath.asin(equatorialRadius / satellitePosition.getNorm().getReal());
             eastAxisOffset = eastAxisOffset + angularResolution) {

            // Build rotation transformations to get first crown elementary sector center
            final FieldTransform<T> eastRotation = new FieldTransform<>(date,
                                                                        new FieldRotation<>(east,
                                                                                            zero.add(eastAxisOffset),
                                                                                            RotationConvention.VECTOR_OPERATOR));

            // Get first elementary crown sector center
            final FieldVector3D<T> firstCrownSectorCenter = eastRotation.transformPosition(projectedToGround);

            // Browse the entire crown
            for (double radialAxisOffset =  angularResolution / 2;
                 radialAxisOffset < FastMath.PI * 2;
                 radialAxisOffset = radialAxisOffset + angularResolution) {

                // Build rotation transformations to get elementary area center
                final FieldTransform<T> radialRotation  = new FieldTransform<>(date,
                                                                               new FieldRotation<>(projectedToGround,
                                                                                                   zero.add(radialAxisOffset),
                                                                                                   RotationConvention.VECTOR_OPERATOR));

                // Get current elementary crown sector center
                final FieldVector3D<T> currentCenter = radialRotation.transformPosition(firstCrownSectorCenter);

                // Compute current elementary crown sector area, it results of the integration of an elementary crown sector
                // over the angular resolution
                final T sectorArea = zero.add(equatorialRadius * equatorialRadius *
                                              2 * angularResolution * FastMath.sin(0.5 * angularResolution) * FastMath.sin(eastAxisOffset));

                // Add current sector contribution to total rediffused flux
                rediffusedFlux = rediffusedFlux.add(computeElementaryFlux(s, currentCenter, earth, sectorArea));
            }
        }
        return spacecraft.radiationPressureAcceleration(date, frame, satellitePosition, s.getAttitude().getRotation(),
                                                        s.getMass(), rediffusedFlux, parameters);
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
    public static double computeAlbedo(final AbsoluteDate date, final double phi) {

        // Get duration since coefficient reference epoch
        final double deltaT = date.durationFrom(T0);

        // Compute 1rst Legendre polynomial coeficient
        final double A1 = C0 +
                          C1 * FastMath.cos(EARTH_AROUND_SUN_PULSATION * deltaT) +
                          C2 * FastMath.sin(EARTH_AROUND_SUN_PULSATION * deltaT);

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


    /** Compute Earth albedo.
     * Albedo value represents the fraction of solar radiative flux that is reflected by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @param <T> extends RealFieldElement
     * @return the albedo in [0;1]
     */
    public static <T extends RealFieldElement<T>> T computeAlbedo(final FieldAbsoluteDate<T> date, final T phi) {

        // Get duration since coefficient reference epoch
        final T deltaT = date.durationFrom(T0);

        // Compute 1rst Legendre polynomial coeficient
        final T A1 = FastMath.cos(deltaT.multiply(EARTH_AROUND_SUN_PULSATION)).multiply(C1).add(
                     FastMath.sin(deltaT.multiply(EARTH_AROUND_SUN_PULSATION)).multiply(C2)).add(C0);

        // Get 1rst and 2nd order Legendre polynomials
        final PolynomialFunction firstLegendrePolynomial  = PolynomialsUtils.createLegendrePolynomial(1);
        final PolynomialFunction secondLegendrePolynomial = PolynomialsUtils.createLegendrePolynomial(2);

        // Get latitude sinus
        final T sinPhi = FastMath.sin(phi);

        // Compute albedo
        return firstLegendrePolynomial.value(sinPhi).multiply(A1).add(
               secondLegendrePolynomial.value(sinPhi).multiply(A2)).add(A0);

    }

    /** Compute Earth emisivity.
     * Emissivity is used to compute the infrared flux that is emited by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @return the emissivity in [0;1]
     */
    public static double computeEmissivity(final AbsoluteDate date, final double phi) {

        // Get duration since coefficient reference epoch
        final double deltaT = date.durationFrom(T0);

        // Compute 1rst Legendre polynomial coeficient
        final double E1 = K0 +
                          K1 * FastMath.cos(EARTH_AROUND_SUN_PULSATION * deltaT) +
                          K2 * FastMath.sin(EARTH_AROUND_SUN_PULSATION * deltaT);

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


    /** Compute Earth emisivity.
     * Emissivity is used to compute the infrared flux that is emited by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @param <T> extends RealFieldElement
     * @return the emissivity in [0;1]
     */
    public static <T extends RealFieldElement<T>> T computeEmissivity(final FieldAbsoluteDate<T> date, final T phi) {

        // Get duration since coefficient reference epoch
        final T deltaT = date.durationFrom(T0);

        // Compute 1rst Legendre polynomial coeficient
        final T E1 = FastMath.cos(deltaT.multiply(EARTH_AROUND_SUN_PULSATION)).multiply(K1).add(
                     FastMath.sin(deltaT.multiply(EARTH_AROUND_SUN_PULSATION)).multiply(K2)).add(K0);

        // Get 1rst and 2nd order Legendre polynomials
        final PolynomialFunction firstLegendrePolynomial  = PolynomialsUtils.createLegendrePolynomial(1);
        final PolynomialFunction secondLegendrePolynomial = PolynomialsUtils.createLegendrePolynomial(2);

        // Get latitude sinus
        final T sinPhi = FastMath.sin(phi);

        // Compute albedo
        return firstLegendrePolynomial.value(sinPhi).multiply(E1).add(
               secondLegendrePolynomial.value(sinPhi).multiply(E2)).add(E0);

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


    /** Compute total solar flux impacting Earth.
     * @param date the date
     * @param frame the Earth centered frame
     * @param <T> extends RealFieldElement
     * @return the total solar flux impacting Earth in J/m^3
     */
    private <T extends RealFieldElement<T>> T computeSolarFlux(final FieldAbsoluteDate<T> date, final Frame frame) {

        // Compute Earth - Sun distance in UA
        final T earthSunDistance = sun.getPVCoordinates(date, frame).getPosition().getNorm().divide(Constants.JPL_SSD_ASTRONOMICAL_UNIT);

        // Compute Solar flux
        return earthSunDistance.multiply(earthSunDistance).reciprocal().multiply(ES_COEFF * Constants.SPEED_OF_LIGHT);
    }


    /** Compute elementary rediffused flux on satellite.
     * @param state the current spacecraft state
     * @param elementCenter the position of the considered area center
     * @param earth the Earth model
     * @param elementArea the area of the current element
     * @return the rediffused flux from considered element on the spacecraft
     */
    private Vector3D computeElementaryFlux(final SpacecraftState state,
                                           final Vector3D elementCenter,
                                           final OneAxisEllipsoid earth,
                                           final double elementArea) {

        // Get satellite position
        final Vector3D satellitePosition = state.getPVCoordinates().getPosition();

        // Get current date
        final AbsoluteDate date = state.getDate();

        // Get frame
        final Frame frame = state.getFrame();

        // Get satellite viewing angle as seen from current elementary area
        final double alpha = Vector3D.angle(elementCenter, satellitePosition);

        // Check that satellite sees the current area
        if (FastMath.abs(alpha) < FastMath.PI / 2) {

            // Get current elementary area center latitude
            final double currentLatitude = earth.transform(elementCenter, frame, date).getLatitude();

            // Compute Earth emissivity value
            final double e = computeEmissivity(date, currentLatitude);

            // Initialize albedo
            double a = 0.0;

            // Check if elementary area is in day light
            final double sunAngle = Vector3D.angle(elementCenter, sun.getPVCoordinates(date, frame).getPosition());

            if (FastMath.abs(sunAngle) < FastMath.PI / 2 ) {
                // Elementary area is in day light, compute albedo value
                a = computeAlbedo(date, currentLatitude);
            }

            // Compute solar flux
            final double solarFlux = computeSolarFlux(date, frame);

            // Compute elementary area contribution to rediffused flux
            final double albedoAndIR = a * solarFlux * FastMath.cos(sunAngle) +
                                       e * solarFlux / 4;

            // Compute elementary area - satellite vector and distance
            final Vector3D r = satellitePosition.subtract(elementCenter);
            final double rNorm = r.getNorm();

            // Compute attenuated projected elemetary area vector
            final Vector3D projectedAreaVector = r.scalarMultiply(elementArea * FastMath.cos(alpha) /
                                                                 (FastMath.PI * rNorm * rNorm * rNorm));

            // Compute elementary radiation flux from current elementary area
            return projectedAreaVector.scalarMultiply(albedoAndIR / Constants.SPEED_OF_LIGHT);

        } else {

            // Spacecraft does not see the elementary area
            return new Vector3D(0.0, 0.0, 0.0);
        }

    }


    /** Compute elementary rediffused flux on satellite.
     * @param state the current spacecraft state
     * @param elementCenter the position of the considered area center
     * @param earth the Earth model
     * @param elementArea the area of the current element
     * @param <T> extends RealFieldElement
     * @return the rediffused flux from considered element on the spacecraft
     */
    private <T extends RealFieldElement<T>> FieldVector3D<T> computeElementaryFlux(final FieldSpacecraftState<T> state,
                                                                                   final FieldVector3D<T> elementCenter,
                                                                                   final OneAxisEllipsoid earth,
                                                                                   final T elementArea) {

        // Get satellite position
        final FieldVector3D<T> satellitePosition = state.getPVCoordinates().getPosition();

        // Get current date
        final FieldAbsoluteDate<T> date = state.getDate();

        // Get frame
        final Frame frame = state.getFrame();

        // Get zero
        final T zero = date.getField().getZero();

        // Get satellite viewing angle as seen from current elementary area
        final T alpha = FieldVector3D.angle(elementCenter, satellitePosition);

        // Check that satellite sees the current area
        if (FastMath.abs(alpha).getReal() < FastMath.PI / 2) {

            // Get current elementary area center latitude
            final T currentLatitude = earth.transform(elementCenter, frame, date).getLatitude();

            // Compute Earth emissivity value
            final T e = computeEmissivity(date, currentLatitude);

            // Initialize albedo
            T a = zero;

            // Check if elementary area is in day light
            final T sunAngle = FieldVector3D.angle(elementCenter, sun.getPVCoordinates(date, frame).getPosition());

            if (FastMath.abs(sunAngle).getReal() < FastMath.PI / 2 ) {
                // Elementary area is in day light, compute albedo value
                a = computeAlbedo(date, currentLatitude);
            }

            // Compute solar flux
            final T solarFlux = computeSolarFlux(date, frame);

            // Compute elementary area contribution to rediffused flux
            final T albedoAndIR = a.multiply(solarFlux).multiply(FastMath.cos(sunAngle)).add(
                                  e.multiply(solarFlux).multiply(0.25));

            // Compute elementary area - satellite vector and distance
            final FieldVector3D<T> r = satellitePosition.subtract(elementCenter);
            final T rNorm = r.getNorm();

            // Compute attenuated projected elemetary area vector
            final FieldVector3D<T> projectedAreaVector = r.scalarMultiply(elementArea.multiply(FastMath.cos(alpha)).divide(
                                                                          rNorm.multiply(rNorm).multiply(rNorm).multiply(FastMath.PI)));

            // Compute elementary radiation flux from current elementary area
            return projectedAreaVector.scalarMultiply(albedoAndIR.divide(Constants.SPEED_OF_LIGHT));

        } else {

            // Spacecraft does not see the elementary area
            return new FieldVector3D<T>(zero, zero, zero);
        }

    }

}