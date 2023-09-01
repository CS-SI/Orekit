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
package org.orekit.forces.radiation;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.analysis.polynomials.PolynomialsUtils;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.forces.ForceModel;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

/** The Knocke Earth Albedo and IR emission force model.
 * <p>
 * This model is based on "EARTH RADIATION PRESSURE EFFECTS ON SATELLITES", 1988, by P. C. Knocke, J. C. Ries, and B. D. Tapley.
 * </p> <p>
 * This model represents the effects of radiation pressure coming from the Earth.
 * It considers Solar radiation which has been reflected by Earth (albedo) and Earth infrared emissions.
 * The planet is considered as a sphere and is divided into elementary areas.
 * Each elementary area is considered as a plane and emits radiation according to Lambert's law.
 * The flux the satellite receives is then equal to the sum of the elementary fluxes coming from Earth.
 * </p> <p>
 * The radiative model of the satellite, and its ability to diffuse, reflect  or absorb radiation is handled
 * by a {@link RadiationSensitive radiation sensitive model}.
 * </p> <p>
 * <b>Caution:</b> This model is only suitable for Earth. Using it with another central body is prone to error..
 * </p>
 *
 * @author Thomas Paulet
 * @since 10.3
 */
public class KnockeRediffusedForceModel implements ForceModel {

    /** Earth rotation around Sun pulsation in rad/sec. */
    private static final double EARTH_AROUND_SUN_PULSATION = MathUtils.TWO_PI / Constants.JULIAN_YEAR;

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

    /** Sun model. */
    private final ExtendedPVCoordinatesProvider sun;

    /** Spacecraft. */
    private final RadiationSensitive spacecraft;

    /** Angular resolution for emissivity and albedo computation in rad. */
    private final double angularResolution;

    /** Earth equatorial radius in m. */
    private double equatorialRadius;

    /** Reference date for periodic terms: December 22nd 1981.
     * Without more precision, the choice is to set it at midnight, UTC. */
    private final AbsoluteDate referenceEpoch;

    /** Default Constructor.
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}</p>.
     * @param sun Sun model
     * @param spacecraft the object physical and geometrical information
     * @param equatorialRadius the Earth equatorial radius in m
     * @param angularResolution angular resolution in rad
     */
    @DefaultDataContext
    public KnockeRediffusedForceModel (final ExtendedPVCoordinatesProvider sun,
                                       final RadiationSensitive spacecraft,
                                       final double equatorialRadius,
                                       final double angularResolution) {

        this(sun, spacecraft, equatorialRadius, angularResolution, DataContext.getDefault().getTimeScales().getUTC());
    }

    /** General constructor.
     * @param sun Sun model
     * @param spacecraft the object physical and geometrical information
     * @param equatorialRadius the Earth equatorial radius in m
     * @param angularResolution angular resolution in rad
     * @param utc the UTC time scale to define reference epoch
     */
    public KnockeRediffusedForceModel (final ExtendedPVCoordinatesProvider sun,
                                       final RadiationSensitive spacecraft,
                                       final double equatorialRadius,
                                       final double angularResolution,
                                       final TimeScale utc) {
        this.sun               = sun;
        this.spacecraft        = spacecraft;
        this.equatorialRadius  = equatorialRadius;
        this.angularResolution = angularResolution;
        this.referenceEpoch    = new AbsoluteDate(1981, 12, 22, 0, 0, 0.0, utc);
    }


    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
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
        final Vector3D satellitePosition = s.getPosition();

        // Get Sun position
        final Vector3D sunPosition = sun.getPosition(date, frame);

        // Get spherical Earth model
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(equatorialRadius, 0.0, frame);

        // Project satellite on Earth as vector
        final Vector3D projectedToGround = satellitePosition.normalize().scalarMultiply(equatorialRadius);

        // Get elementary vector east for Earth browsing using rotations
        final Vector3D east = earth.transform(satellitePosition, frame, date).getEast();

        // Initialize rediffused flux with elementary flux coming from the circular area around the projected satellite
        final double centerArea = MathUtils.TWO_PI * equatorialRadius * equatorialRadius *
                                 (1.0 - FastMath.cos(angularResolution));
        Vector3D rediffusedFlux = computeElementaryFlux(s, projectedToGround, sunPosition, earth, centerArea);

        // Sectorize the part of Earth which is seen by the satellite into crown sectors with constant angular resolution
        for (double eastAxisOffset = 1.5 * angularResolution;
             eastAxisOffset < FastMath.asin(equatorialRadius / satellitePosition.getNorm());
             eastAxisOffset = eastAxisOffset + angularResolution) {

            // Build rotation transformations to get first crown elementary sector center
            final Transform eastRotation = new Transform(date,
                                                          new Rotation(east, eastAxisOffset, RotationConvention.VECTOR_OPERATOR));

            // Get first elementary crown sector center
            final Vector3D firstCrownSectorCenter = eastRotation.transformPosition(projectedToGround);

            // Browse the entire crown
            for (double radialAxisOffset = 0.5 * angularResolution;
                 radialAxisOffset < MathUtils.TWO_PI;
                 radialAxisOffset = radialAxisOffset + angularResolution) {

                // Build rotation transformations to get elementary area center
                final Transform radialRotation  = new Transform(date,
                                                                new Rotation(projectedToGround, radialAxisOffset, RotationConvention.VECTOR_OPERATOR));

                // Get current elementary crown sector center
                final Vector3D currentCenter = radialRotation.transformPosition(firstCrownSectorCenter);

                // Compute current elementary crown sector area, it results of the integration of an elementary crown sector
                // over the angular resolution
                final double sectorArea = equatorialRadius * equatorialRadius *
                                          2.0 * angularResolution * FastMath.sin(0.5 * angularResolution) * FastMath.sin(eastAxisOffset);

                // Add current sector contribution to total rediffused flux
                rediffusedFlux = rediffusedFlux.add(computeElementaryFlux(s, currentCenter, sunPosition, earth, sectorArea));
            }
        }

        return spacecraft.radiationPressureAcceleration(s, rediffusedFlux, parameters);
    }


    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {
        // Get date
        final FieldAbsoluteDate<T> date = s.getDate();

        // Get frame
        final Frame frame = s.getFrame();

        // Get zero
        final T zero = date.getField().getZero();

        // Get satellite position
        final FieldVector3D<T> satellitePosition = s.getPosition();

        // Get Sun position
        final FieldVector3D<T> sunPosition = sun.getPosition(date, frame);

        // Get spherical Earth model
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(equatorialRadius, 0.0, frame);

        // Project satellite on Earth as vector
        final FieldVector3D<T> projectedToGround = satellitePosition.normalize().scalarMultiply(equatorialRadius);

        // Get elementary vector east for Earth browsing using rotations
        final FieldVector3D<T> east = earth.transform(satellitePosition, frame, date).getEast();

        // Initialize rediffused flux with elementary flux coming from the circular area around the projected satellite
        final T centerArea = zero.getPi().multiply(2.0).multiply(equatorialRadius).multiply(equatorialRadius).
                        multiply(1.0 - FastMath.cos(angularResolution));
        FieldVector3D<T> rediffusedFlux = computeElementaryFlux(s, projectedToGround, sunPosition, earth, centerArea);

        // Sectorize the part of Earth which is seen by the satellite into crown sectors with constant angular resolution
        for (double eastAxisOffset = 1.5 * angularResolution;
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
            for (double radialAxisOffset = 0.5 * angularResolution;
                 radialAxisOffset < MathUtils.TWO_PI;
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
                                              2.0 * angularResolution * FastMath.sin(0.5 * angularResolution) * FastMath.sin(eastAxisOffset));

                // Add current sector contribution to total rediffused flux
                rediffusedFlux = rediffusedFlux.add(computeElementaryFlux(s, currentCenter, sunPosition, earth, sectorArea));
            }
        }

        return spacecraft.radiationPressureAcceleration(s, rediffusedFlux, parameters);
    }


    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return spacecraft.getRadiationParametersDrivers();
    }

    /** Compute Earth albedo.
     * Albedo value represents the fraction of solar radiative flux that is reflected by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @return the albedo in [0;1]
     */
    private double computeAlbedo(final AbsoluteDate date, final double phi) {

        // Get duration since coefficient reference epoch
        final double deltaT = date.durationFrom(referenceEpoch);

        // Compute 1rst Legendre polynomial coeficient
        final SinCos sc = FastMath.sinCos(EARTH_AROUND_SUN_PULSATION * deltaT);
        final double A1 = C0 +
                          C1 * sc.cos() +
                          C2 * sc.sin();

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
     * @param <T> extends CalculusFieldElement
     * @return the albedo in [0;1]
     */
    private <T extends CalculusFieldElement<T>> T computeAlbedo(final FieldAbsoluteDate<T> date, final T phi) {

        // Get duration since coefficient reference epoch
        final T deltaT = date.durationFrom(referenceEpoch);

        // Compute 1rst Legendre polynomial coeficient
        final FieldSinCos<T> sc = FastMath.sinCos(deltaT.multiply(EARTH_AROUND_SUN_PULSATION));
        final T A1 = sc.cos().multiply(C1).add(
                     sc.sin().multiply(C2)).add(C0);

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
     * Emissivity is used to compute the infrared flux that is emitted by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @return the emissivity in [0;1]
     */
    private double computeEmissivity(final AbsoluteDate date, final double phi) {

        // Get duration since coefficient reference epoch
        final double deltaT = date.durationFrom(referenceEpoch);

        // Compute 1rst Legendre polynomial coefficient
        final SinCos sc = FastMath.sinCos(EARTH_AROUND_SUN_PULSATION * deltaT);
        final double E1 = K0 +
                          K1 * sc.cos() +
                          K2 * sc.sin();

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
     * Emissivity is used to compute the infrared flux that is emitted by Earth.
     * Its value is in [0;1].
     * @param date the date
     * @param phi the equatorial latitude in rad
     * @param <T> extends CalculusFieldElement
     * @return the emissivity in [0;1]
     */
    private <T extends CalculusFieldElement<T>> T computeEmissivity(final FieldAbsoluteDate<T> date, final T phi) {

        // Get duration since coefficient reference epoch
        final T deltaT = date.durationFrom(referenceEpoch);

        // Compute 1rst Legendre polynomial coeficient
        final FieldSinCos<T> sc = FastMath.sinCos(deltaT.multiply(EARTH_AROUND_SUN_PULSATION));
        final T E1 = sc.cos().multiply(K1).add(
                     sc.sin().multiply(K2)).add(K0);

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
     * @param sunPosition the Sun position in an Earth centered frame
     * @return the total solar flux impacting Earth in J/m^3
     */
    private double computeSolarFlux(final Vector3D sunPosition) {

        // Compute Earth - Sun distance in UA
        final double earthSunDistance = sunPosition.getNorm() / Constants.JPL_SSD_ASTRONOMICAL_UNIT;

        // Compute Solar flux
        return ES_COEFF * Constants.SPEED_OF_LIGHT / (earthSunDistance * earthSunDistance);
    }


    /** Compute total solar flux impacting Earth.
     * @param sunPosition the Sun position in an Earth centered frame
     * @param <T> extends CalculusFieldElement
     * @return the total solar flux impacting Earth in J/m^3
     */
    private <T extends CalculusFieldElement<T>> T computeSolarFlux(final FieldVector3D<T> sunPosition) {

        // Compute Earth - Sun distance in UA
        final T earthSunDistance = sunPosition.getNorm().divide(Constants.JPL_SSD_ASTRONOMICAL_UNIT);

        // Compute Solar flux
        return earthSunDistance.multiply(earthSunDistance).reciprocal().multiply(ES_COEFF * Constants.SPEED_OF_LIGHT);
    }


    /** Compute elementary rediffused flux on satellite.
     * @param state the current spacecraft state
     * @param elementCenter the position of the considered area center
     * @param sunPosition the position of the Sun in the spacecraft frame
     * @param earth the Earth model
     * @param elementArea the area of the current element
     * @return the rediffused flux from considered element on the spacecraft
     */
    private Vector3D computeElementaryFlux(final SpacecraftState state,
                                           final Vector3D elementCenter,
                                           final Vector3D sunPosition,
                                           final OneAxisEllipsoid earth,
                                           final double elementArea) {

        // Get satellite position
        final Vector3D satellitePosition = state.getPosition();

        // Get current date
        final AbsoluteDate date = state.getDate();

        // Get frame
        final Frame frame = state.getFrame();

        // Get solar flux impacting Earth
        final double solarFlux = computeSolarFlux(sunPosition);

        // Get satellite viewing angle as seen from current elementary area
        final double alpha = Vector3D.angle(elementCenter, satellitePosition);

        // Check that satellite sees the current area
        if (FastMath.abs(alpha) < MathUtils.SEMI_PI) {

            // Get current elementary area center latitude
            final double currentLatitude = earth.transform(elementCenter, frame, date).getLatitude();

            // Compute Earth emissivity value
            final double e = computeEmissivity(date, currentLatitude);

            // Initialize albedo
            double a = 0.0;

            // Check if elementary area is in day light
            final double sunAngle = Vector3D.angle(elementCenter, sunPosition);

            if (FastMath.abs(sunAngle) < MathUtils.SEMI_PI) {
                // Elementary area is in day light, compute albedo value
                a = computeAlbedo(date, currentLatitude);
            }

            // Compute elementary area contribution to rediffused flux
            final double albedoAndIR = a * solarFlux * FastMath.cos(sunAngle) +
                                       e * solarFlux * 0.25;

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
     * @param sunPosition the position of the Sun in the spacecraft frame
     * @param earth the Earth model
     * @param elementArea the area of the current element
     * @param <T> extends CalculusFieldElement
     * @return the rediffused flux from considered element on the spacecraft
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> computeElementaryFlux(final FieldSpacecraftState<T> state,
                                                                                   final FieldVector3D<T> elementCenter,
                                                                                   final FieldVector3D<T> sunPosition,
                                                                                   final OneAxisEllipsoid earth,
                                                                                   final T elementArea) {

        // Get satellite position
        final FieldVector3D<T> satellitePosition = state.getPosition();

        // Get current date
        final FieldAbsoluteDate<T> date = state.getDate();

        // Get frame
        final Frame frame = state.getFrame();

        // Get zero
        final T zero = date.getField().getZero();

        // Get solar flux impacting Earth
        final T solarFlux = computeSolarFlux(sunPosition);

        // Get satellite viewing angle as seen from current elementary area
        final T alpha = FieldVector3D.angle(elementCenter, satellitePosition);

        // Check that satellite sees the current area
        if (FastMath.abs(alpha).getReal() < MathUtils.SEMI_PI) {

            // Get current elementary area center latitude
            final T currentLatitude = earth.transform(elementCenter, frame, date).getLatitude();

            // Compute Earth emissivity value
            final T e = computeEmissivity(date, currentLatitude);

            // Initialize albedo
            T a = zero;

            // Check if elementary area is in day light
            final T sunAngle = FieldVector3D.angle(elementCenter, sunPosition);

            if (FastMath.abs(sunAngle).getReal() < MathUtils.SEMI_PI) {
                // Elementary area is in day light, compute albedo value
                a = computeAlbedo(date, currentLatitude);
            }

            // Compute elementary area contribution to rediffused flux
            final T albedoAndIR = a.multiply(solarFlux).multiply(FastMath.cos(sunAngle)).add(
                                  e.multiply(solarFlux).multiply(0.25));

            // Compute elementary area - satellite vector and distance
            final FieldVector3D<T> r = satellitePosition.subtract(elementCenter);
            final T rNorm = r.getNorm();

            // Compute attenuated projected elemetary area vector
            final FieldVector3D<T> projectedAreaVector = r.scalarMultiply(elementArea.multiply(FastMath.cos(alpha)).divide(
                                                                          rNorm.multiply(rNorm).multiply(rNorm).multiply(zero.getPi())));

            // Compute elementary radiation flux from current elementary area
            return projectedAreaVector.scalarMultiply(albedoAndIR.divide(Constants.SPEED_OF_LIGHT));

        } else {

            // Spacecraft does not see the elementary area
            return new FieldVector3D<T>(zero, zero, zero);
        }

    }

}
