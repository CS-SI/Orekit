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
package org.orekit.estimation.iod;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * Abstract Class to build IOD based on Angles Only measurements. An orbit is determined from three position vectors.
 * <p>
 * References: Vallado, D., Fundamentals of Astrodynamics and Applications Curtis, Orbital Mechanics for Engineering
 * Students
 *
 * @author Julien Asquier
 * @since 11.3.3
 */
public abstract class AbstractAnglesOnlyIod {

    /** Gravitational Constant. */
    private final double mu;

    /** Final Frame desired to build the result Orbit. */
    private final Frame outputFrame;

    /** GCRF Frame used for intermediate conversion between ECEF and ECI frames. */
    private final Frame GCRF = FramesFactory.getGCRF();

    /** ITRF frame used for intermediate conversion between ECEF and ECI frames. */
    private final Frame ITRF = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

    /**
     * Constructor.
     *
     * @param mu gravitational constant
     * @param outputFrame Final Frame expected
     */
    protected AbstractAnglesOnlyIod(final double mu, final Frame outputFrame) {
        this.mu          = mu;
        this.outputFrame = outputFrame;
    }

    /** @return gravitational Constant */
    public double getMu() {
        return mu;
    }

    /** @return orbit output frame */
    public Frame getOutputFrame() {
        return outputFrame;
    }

    /**
     * Execute the estimate method with 3 AngularRaDec measurements as inputs.
     *
     * @param raDec1 1st Radec measurement
     * @param raDec2 2nd Radec measurement
     * @param raDec3 3rd Radec measurement
     *
     * @return orbit estimation of the called child IOD
     */
    public Orbit estimate(final AngularRaDec raDec1, final AngularRaDec raDec2, final AngularRaDec raDec3) {

        // Extraction of the position vector of the station at the second time used as a reference, in GCRF
        final PVCoordinates pv1 = raDec1.getStation().getBaseFrame().getPVCoordinates(raDec1.getDate(), GCRF);
        final PVCoordinates pv2 = raDec2.getStation().getBaseFrame().getPVCoordinates(raDec2.getDate(), GCRF);
        final PVCoordinates pv3 = raDec3.getStation().getBaseFrame().getPVCoordinates(raDec3.getDate(), GCRF);

        // Extraction of the AbsoluteDate of the 3 observations
        final AbsoluteDate obsDate1 = raDec1.getDate();
        final AbsoluteDate obsDate2 = raDec2.getDate();
        final AbsoluteDate obsDate3 = raDec3.getDate();

        // Extraction of the Line of Sight of the 3 observations
        final Vector3D los1 = raDec1.getLineOfSight();
        final Vector3D los2 = raDec2.getLineOfSight();
        final Vector3D los3 = raDec3.getLineOfSight();

        // Using the child IOD estimate method to get the estimated Orbit
        return estimate(pv1, obsDate1, los1, pv2, obsDate2, los2, pv3, obsDate3, los3);

    }

    /**
     * Estimate method with the 3 PVCoordinates of the station, at the 3 Dates of observations and the 3 Line Of Sight
     * vectors at these dates.
     * <p>
     * <b>WARNING : The observations must be expressed in an ECI frame.</b>
     *
     * @param obsPva1 reference PVCoordinates for station position (position at 1st Observation Date)
     * @param obsPva2 reference PVCoordinates for station position (position at 2nd Observation Date)
     * @param obsPva3 reference PVCoordinates for station position (position at 3rd Observation Date)
     * @param obsDate1 1st Observation Date
     * @param obsDate2 2nd Observation Date
     * @param obsDate3 3rd Observation Date
     * @param los1 line Of Sight vector at 1st Observation Date
     * @param los2 line Of Sight vector at 2nd Observation Date
     * @param los3 line Of Sight vector at 3rd Observation Date
     *
     * @return orbit estimation of the called child IOD
     */
    protected abstract Orbit estimate(PVCoordinates obsPva1, AbsoluteDate obsDate1, Vector3D los1,
                                      PVCoordinates obsPva2, AbsoluteDate obsDate2, Vector3D los2,
                                      PVCoordinates obsPva3, AbsoluteDate obsDate3, Vector3D los3);

    /**
     * Execute the estimate method with a reference station position vector, a frame, 3 Dates of observations and the 3 Line
     * Of Sight vectors at these dates. The reference vector is considered to be at the position of the station at the second
     * observation date (Vallado technique).
     *
     * @param obsVector reference Vector for station position (position at second Observation Date)
     * @param frameObserver frame of the reference vector.
     * @param obsDate1 1st Observation Date
     * @param obsDate2 2nd Observation Date
     * @param obsDate3 3rd Observation Date
     * @param los1 line Of Sight vector at 1st Observation Date
     * @param los2 line Of Sight vector at 2nd Observation Date
     * @param los3 line Of Sight vector at 3rd Observation Date
     *
     * @return orbit estimation of the called child IOD
     */
    public Orbit estimate(final Vector3D obsVector, final Frame frameObserver,
                          final AbsoluteDate obsDate1, final Vector3D los1,
                          final AbsoluteDate obsDate2, final Vector3D los2,
                          final AbsoluteDate obsDate3, final Vector3D los3) {

        // Build of the 3 PVCoordinates of the station position at the 3 different observations times
        final PVCoordinates[] pvList = this.buildObserverPositions(new PVCoordinates(obsVector), frameObserver,
                                                                   obsDate1, obsDate2, obsDate3);

        // Using the child IOD estimate method to get the estimated Orbit
        return estimate(pvList[0], obsDate1, los1, pvList[1], obsDate2, los2, pvList[2], obsDate3, los3);

    }

    /**
     * Building of the 3 PVCoordinates corresponding to the 3 observations based on a reference position vector, considered
     * to be the position vector of the station at the second date, the frame of the station and the 3 observations dates. If
     * not, a transformation of the vector to ITRF2010 at the second date to efficiently use the buildObserverPosition
     * method.
     *
     * @param pvObserver reference PVCoordinates for station position (pv at second Observation Date)
     * @param frameObserver frame of the reference pv.
     * @param obsDate1 1st Observation Date
     * @param obsDate2 2nd Observation Date
     * @param obsDate3 3rd Observation Date
     *
     * @return PV list of the 3 PVs station positions
     */
    public PVCoordinates[] buildObserverPositions(final PVCoordinates pvObserver, final Frame frameObserver,
                                                  final AbsoluteDate obsDate1, final AbsoluteDate obsDate2,
                                                  final AbsoluteDate obsDate3) {
        PVCoordinates pv2Int            = pvObserver;
        Frame         frameIntermediate = frameObserver;

        // If the frame of the vector is in ECI frame, then to use the buildObserverPosition to consider different dates
        // it is needed to express the vector in an ECEF frame as an intermediate transformation. the ITRF2010 is used,
        // even if any kind of ECEF frame could do the work (due to the fact that it is an intermediate transformation).
        if (frameObserver.isPseudoInertial()) {
            pv2Int            = frameIntermediate.getTransformTo(ITRF, obsDate2).transformPVCoordinates(pvObserver);
            frameIntermediate = ITRF;

        }
        final PVCoordinates pv1 = buildObserverPosition(pv2Int, frameIntermediate, obsDate1);
        final PVCoordinates pv2 = buildObserverPosition(pv2Int, frameIntermediate, obsDate2);
        final PVCoordinates pv3 = buildObserverPosition(pv2Int, frameIntermediate, obsDate3);

        return new PVCoordinates[] { pv1, pv2, pv3 };
    }

    /**
     * Building of the PV observer position from a vector, an initial frame and a date of observation.
     *
     * @param pvObserver position/Velocity of the station
     * @param initialFrame initial frame
     * @param obsDate observation Date
     *
     * @return PVCoordinates of the observer at the observation date in GCRF
     */
    private PVCoordinates buildObserverPosition(final PVCoordinates pvObserver, final Frame initialFrame,
                                                final AbsoluteDate obsDate) {
        // The transformation in GCRF is used as an intermediate transformation for the PV to be in a ECI frame necessary
        // for the child IOD estimation methods
        return initialFrame.getTransformTo(GCRF, obsDate).transformPVCoordinates(pvObserver);

    }

    /**
     * Execute the estimate method with a reference station PVCoordinates, a frame, 3 Dates of observations and the 3 Line Of
     * Sight vectors at these dates. The reference vector is considered to be at the position of the station at the second
     * observation date (Vallado technique).
     *
     * @param obsPva reference PVCoordinates for station position (position/velocity/acceleration at second Observation
     * Date)
     * @param frameObserver frame of the reference vector.
     * @param obsDate1 1st Observation Date
     * @param obsDate2 2nd Observation Date
     * @param obsDate3 3rd Observation Date
     * @param los1 line Of Sight vector at 1st Observation Date
     * @param los2 line Of Sight vector at 2nd Observation Date
     * @param los3 line Of Sight vector at 3rd Observation Date
     *
     * @return orbit estimation of the called child IOD
     */
    public Orbit estimate(final PVCoordinates obsPva, final Frame frameObserver,
                          final AbsoluteDate obsDate1, final Vector3D los1,
                          final AbsoluteDate obsDate2, final Vector3D los2,
                          final AbsoluteDate obsDate3, final Vector3D los3) {

        // Build of the 3 PVCoordinates of the station position at the 3 different observations times
        final PVCoordinates[] pvList = this.buildObserverPositions(obsPva, frameObserver,
                                                                   obsDate1, obsDate2, obsDate3);
        // Using the child IOD estimate method to get the estimated Orbit
        return estimate(pvList[0], obsDate1, los1, pvList[1], obsDate2, los2, pvList[2], obsDate3, los3);

    }

    /**
     * Abstract estimated method. Execute the estimate method with the 3 PVCoordinates of the station, at the 3 Dates of
     * observations and the 3 Line Of Sight vectors at these dates.
     *
     * @param frameObserver frame of the stations vectors.
     * @param obsVector1 reference Vector for station position at 1st Date
     * @param obsVector2 reference Vector for station position at 2nd Date
     * @param obsVector3 reference Vector for station position at 3rd Date
     * @param obsDate1 1st Observation Date
     * @param obsDate2 2nd Observation Date
     * @param obsDate3 3rd Observation Date
     * @param los1 line Of Sight vector at 1st Observation Date
     * @param los2 line Of Sight vector at 2nd Observation Date
     * @param los3 line Of Sight vector at 3rd Observation Date
     *
     * @return orbit estimation of the called child IOD
     */
    public Orbit estimate(final Frame frameObserver,
                          final Vector3D obsVector1, final AbsoluteDate obsDate1, final Vector3D los1,
                          final Vector3D obsVector2, final AbsoluteDate obsDate2, final Vector3D los2,
                          final Vector3D obsVector3, final AbsoluteDate obsDate3, final Vector3D los3) {

        // Build of the 3 PVCoordinates of the station position at the 3 different observations times
        final PVCoordinates pv1 = buildObserverPosition(new PVCoordinates(obsVector1), frameObserver, obsDate1);
        final PVCoordinates pv2 = buildObserverPosition(new PVCoordinates(obsVector2), frameObserver, obsDate2);
        final PVCoordinates pv3 = buildObserverPosition(new PVCoordinates(obsVector3), frameObserver, obsDate3);

        // Using the child IOD estimate method to get the estimated Orbit
        return estimate(pv1, obsDate1, los1, pv2, obsDate2, los2, pv3, obsDate3, los3);

    }

    /**
     * Building of the 3 PVCoordinates corresponding to the 3 observations stations positions based on GroundStation object
     * information and the 3 observations dates.
     *
     * @param earthRadius reference Earth radius
     * @param earthFlattening reference Earth Flattening
     * @param iersConventions IERS conventions to build the ITRF frame related to station
     * @param eopFlag boolean to consider eop for ITRF construction
     * @param referenceEpoch reference Epoch
     * @param latitude latitude of the station (in radians)
     * @param longitude longitude of the station (in radians)
     * @param altitude altitude of the station (in meters)
     * @param obsDate1 1st Observation Date
     * @param obsDate2 2nd Observation Date
     * @param obsDate3 3rd Observation Date
     *
     * @return PV list of the 3 PVs for observer positions
     */
    public PVCoordinates[] buildObserverPositions(final double earthRadius, final double earthFlattening,
                                                  final IERSConventions iersConventions, final boolean eopFlag,
                                                  final AbsoluteDate referenceEpoch,
                                                  final double latitude,
                                                  final double longitude,
                                                  final double altitude,
                                                  final AbsoluteDate obsDate1,
                                                  final AbsoluteDate obsDate2,
                                                  final AbsoluteDate obsDate3) {

        final PVCoordinates pv1 = buildObserverPosition(earthRadius, earthFlattening, iersConventions,
                                                        eopFlag, referenceEpoch, latitude,
                                                        longitude, altitude, obsDate1);

        final PVCoordinates pv2 = buildObserverPosition(earthRadius, earthFlattening, iersConventions,
                                                        eopFlag, referenceEpoch, latitude,
                                                        longitude, altitude, obsDate2);

        final PVCoordinates pv3 = buildObserverPosition(earthRadius, earthFlattening, iersConventions,
                                                        eopFlag, referenceEpoch, latitude,
                                                        longitude, altitude, obsDate3);

        return new PVCoordinates[] { pv1, pv2, pv3 };
    }

    /**
     * Building of the PV observer position from a vector, a ECEF frame and a date of observation.
     *
     * @param earthRadius reference Earth radius
     * @param earthFlattening reference Earth Flattening
     * @param iersConventions IERS conventions to build the ITRF frame related to station
     * @param eopFlag boolean to consider eop for ITRF construction
     * @param referenceEpoch reference Epoch
     * @param latitude latitude of the station (in radians)
     * @param longitude longitude of the station (in radians)
     * @param altitude altitude of the station (in meters)
     * @param obsDate Observation Date
     *
     * @return PVCoordinates of the observer at the observation date
     */
    public PVCoordinates buildObserverPosition(final double earthRadius, final double earthFlattening,
                                               final IERSConventions iersConventions, final boolean eopFlag,
                                               final AbsoluteDate referenceEpoch,
                                               final double latitude,
                                               final double longitude,
                                               final double altitude,
                                               final AbsoluteDate obsDate) {

        final GroundStation observer = buildGroundStation(earthRadius, earthFlattening, iersConventions, eopFlag,
                                                          referenceEpoch, latitude, longitude, altitude);

        return observer.getBaseFrame().getPVCoordinates(obsDate, GCRF);
    }

    /**
     * Building of the GroundStation object.
     *
     * @param earthRadius reference Earth radius
     * @param earthFlattening reference Earth Flattening
     * @param iersConventions IERS conventions to build the ITRF frame related to station
     * @param eopFlag boolean to consider eop for ITRF construction
     * @param referenceEpoch reference Epoch
     * @param latitude latitude of the station (in radians)
     * @param longitude longitude of the station (in radians)
     * @param altitude altitude of the station (in meters)
     *
     * @return GroundStation corresponding to the observer
     */
    private GroundStation buildGroundStation(final double earthRadius, final double earthFlattening,
                                             final IERSConventions iersConventions, final boolean eopFlag,
                                             final AbsoluteDate referenceEpoch,
                                             final double latitude, final double longitude, final double altitude) {

        // We build by default the ground station in the ITRF frame, which is a ECEF frame.
        final Frame            itrf = FramesFactory.getITRF(iersConventions, eopFlag);
        final OneAxisEllipsoid body = new OneAxisEllipsoid(earthRadius, earthFlattening, itrf);
        final GroundStation observer = new GroundStation(new TopocentricFrame(body,
                                                                              new GeodeticPoint(latitude, longitude,
                                                                                                altitude), ""));
        observer.getPrimeMeridianOffsetDriver().setReferenceDate(referenceEpoch);
        observer.getPolarOffsetXDriver().setReferenceDate(referenceEpoch);
        observer.getPolarOffsetYDriver().setReferenceDate(referenceEpoch);
        return observer;
    }

}
