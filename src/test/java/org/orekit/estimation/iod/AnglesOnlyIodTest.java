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
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.IERSConventions;

public class AnglesOnlyIodTest extends AbstractIodTest {

    /** 1st observation Date. */
    AbsoluteDate obsDate1;
    
    /** 2nd observation Date. */
    AbsoluteDate obsDate2;
    
    /** 3rd observation Date. */
    AbsoluteDate obsDate3;
    
    /** reference propagator. */
    KeplerianPropagator propRef;


    @BeforeEach
    public void specificSetup() {

        final DateComponents dateComp = new DateComponents(DateComponents.FIFTIES_EPOCH, 21915);
        this.obsDate2 = new AbsoluteDate(dateComp, TimeScalesFactory.getUTC());

        // Date of the 1st measurement, according to 2nd measurement
        this.obsDate1 = obsDate2.shiftedBy(-1000);

        // Date of the 3rd measurement, according to 2nd measurement
        this.obsDate3 = obsDate2.shiftedBy(1000);

        // creating the propagator
        final KeplerianOrbit kepOrbitRef = new KeplerianOrbit(7197934.0, 0., FastMath.toRadians(98.71),
                                                              0., FastMath.toRadians(100.41), 0.,
                                                              PositionAngle.MEAN, gcrf, obsDate2,
                                                              Constants.WGS84_EARTH_MU);
        this.propRef = new KeplerianPropagator(kepOrbitRef);
    }
    
    /**
     * Validation of the different estimate methods of the AnglesOnlyIod abstract class, starting from ECEF frame.
     */
    @Test
    public void testEcefEstimateMethods() {

        // Getting the 3 Line Of Sight at the 3 observation dates
        final Vector3D los1 = getLOSAngles(propRef, obsDate1);
        final Vector3D los2 = getLOSAngles(propRef, obsDate2);
        final Vector3D los3 = getLOSAngles(propRef, obsDate3);

        // Creating the radec for the radec estimate method
        final AngularRaDec raDec1 = new AngularRaDec(observer, itrf, obsDate1,
                                                     new double[] { los1.getAlpha(), los1.getDelta() },
                                                     new double[] { 1.0, 1.0 }, new double[] { 1.0, 1.0 },
                                                     new ObservableSatellite(0));

        final AngularRaDec raDec2 = new AngularRaDec(observer, itrf, obsDate2,
                                                     new double[] { los2.getAlpha(), los2.getDelta() },
                                                     new double[] { 1.0, 1.0 }, new double[] { 1.0, 1.0 },
                                                     new ObservableSatellite(0));
        final AngularRaDec raDec3 = new AngularRaDec(observer, itrf, obsDate3,
                                                     new double[] { los3.getAlpha(), los3.getDelta() },
                                                     new double[] { 1.0, 1.0 }, new double[] { 1.0, 1.0 },
                                                     new ObservableSatellite(0));

        // Getting the observer PVCoordinates at the 3 dates
        final TimeStampedPVCoordinates obsPva1 = observer.getBaseFrame().getPVCoordinates(obsDate1, itrf);
        final TimeStampedPVCoordinates obsPva2 = observer.getBaseFrame().getPVCoordinates(obsDate2, itrf);
        final TimeStampedPVCoordinates obsPva3 = observer.getBaseFrame().getPVCoordinates(obsDate3, itrf);

        // computation of the estimated orbit
        final IodGauss iodgauss = new IodGauss(mu, eme2000);

        // Estimation of the orbit
        final Orbit estimatedOrbit1 = iodgauss.estimate(itrf,
                                                        obsPva1.getPosition(), obsDate1, los1,
                                                        obsPva2.getPosition(), obsDate2, los2,
                                                        obsPva3.getPosition(), obsDate3, los3);
        final Orbit estimatedOrbit2 = iodgauss.estimate(raDec1, raDec2, raDec3);
        final Orbit estimatedOrbit3 = iodgauss.estimate(obsPva2, itrf, obsDate1, los1,
                                                        obsDate2, los2, obsDate3, los3);
        final Orbit estimatedOrbit4 = iodgauss.estimate(obsPva2.getPosition(), itrf, obsDate1, los1,
                                                        obsDate2, los2, obsDate3, los3);

        // Relative error for the different estimate methods, compared to the first one.
        final double relativeRangeError1    = getRelativeRangeError(estimatedOrbit1, estimatedOrbit2);
        final double relativeVelocityError1 = getRelativeVelocityError(estimatedOrbit1, estimatedOrbit2);

        final double relativeRangeError2    = getRelativeRangeError(estimatedOrbit1, estimatedOrbit3);
        final double relativeVelocityError2 = getRelativeVelocityError(estimatedOrbit1, estimatedOrbit3);

        final double relativeRangeError3    = getRelativeRangeError(estimatedOrbit1, estimatedOrbit4);
        final double relativeVelocityError3 = getRelativeVelocityError(estimatedOrbit1, estimatedOrbit4);

        Assertions.assertEquals(0, relativeRangeError1, 10E-13);
        Assertions.assertEquals(0, relativeVelocityError1, 10E-13);
        Assertions.assertEquals(0, relativeRangeError2, 0);
        Assertions.assertEquals(0, relativeVelocityError2, 0);
        Assertions.assertEquals(0, relativeRangeError3, 0);
        Assertions.assertEquals(0, relativeVelocityError3, 0);

    }

    /**
     * Validation of the different estimate methods of the AnglesOnlyIod abstract class, starting from ECI frame.
     */
    @Test
    public void testEciEstimateMethods() {

        // Getting the 3 Line Of Sight at the 3 observation dates
        final Vector3D los1 = getLOSAngles(propRef, obsDate1);
        final Vector3D los2 = getLOSAngles(propRef, obsDate2);
        final Vector3D los3 = getLOSAngles(propRef, obsDate3);

        // Creating the radec for the radec estimate method
        final AngularRaDec raDec1 = new AngularRaDec(observer, gcrf, obsDate1,
                                                     new double[] { los1.getAlpha(), los1.getDelta() },
                                                     new double[] { 1.0, 1.0 }, new double[] { 1.0, 1.0 },
                                                     new ObservableSatellite(0));

        final AngularRaDec raDec2 = new AngularRaDec(observer, gcrf, obsDate2,
                                                     new double[] { los2.getAlpha(), los2.getDelta() },
                                                     new double[] { 1.0, 1.0 }, new double[] { 1.0, 1.0 },
                                                     new ObservableSatellite(0));
        final AngularRaDec raDec3 = new AngularRaDec(observer, gcrf, obsDate3,
                                                     new double[] { los3.getAlpha(), los3.getDelta() },
                                                     new double[] { 1.0, 1.0 }, new double[] { 1.0, 1.0 },
                                                     new ObservableSatellite(0));

        // Getting the observer PVCoordinates at the 3 dates
        final TimeStampedPVCoordinates obsPva1 = observer.getBaseFrame().getPVCoordinates(obsDate1, gcrf);
        final TimeStampedPVCoordinates obsPva2 = observer.getBaseFrame().getPVCoordinates(obsDate2, gcrf);
        final TimeStampedPVCoordinates obsPva3 = observer.getBaseFrame().getPVCoordinates(obsDate3, gcrf);

        // computation of the estimated orbit for the estimation methods
        final IodGauss iodgauss = new IodGauss(mu, eme2000);

        final Orbit estimatedOrbit1 = iodgauss.estimate(gcrf,
                                                        obsPva1.getPosition(), obsDate1, los1,
                                                        obsPva2.getPosition(), obsDate2, los2,
                                                        obsPva3.getPosition(), obsDate3, los3);

        final Orbit estimatedOrbit2 = iodgauss.estimate(raDec1, raDec2, raDec3);

        final Orbit estimatedOrbit3 = iodgauss.estimate(obsPva2, gcrf, obsDate1, los1,
                                                        obsDate2, los2, obsDate3, los3);

        final Orbit estimatedOrbit4 = iodgauss.estimate(obsPva2.getPosition(), gcrf, obsDate1, los1,
                                                        obsDate2, los2, obsDate3, los3);

        // Relative error as a reference the first estimated orbit
        final double relativeRangeError1    = getRelativeRangeError(estimatedOrbit1, estimatedOrbit2);
        final double relativeVelocityError1 = getRelativeVelocityError(estimatedOrbit1, estimatedOrbit2);

        final double relativeRangeError2    = getRelativeRangeError(estimatedOrbit1, estimatedOrbit3);
        final double relativeVelocityError2 = getRelativeVelocityError(estimatedOrbit1, estimatedOrbit3);

        final double relativeRangeError3    = getRelativeRangeError(estimatedOrbit1, estimatedOrbit4);
        final double relativeVelocityError3 = getRelativeVelocityError(estimatedOrbit1, estimatedOrbit4);

        Assertions.assertEquals(0, relativeRangeError1, 10E-13);
        Assertions.assertEquals(0, relativeVelocityError1, 10E-13);

        Assertions.assertEquals(0, relativeRangeError2, 10E-11);
        Assertions.assertEquals(0, relativeVelocityError2, 10E-10);

        Assertions.assertEquals(0, relativeRangeError3, 10E-11);
        Assertions.assertEquals(0, relativeVelocityError3, 10E-10);

    }

    /**
     * Validation of the building of the 3 observer PVCoordinates for the 3 observations
     */
    @Test
    public void testBuildObserverPositions() {

        // Getting the observer PVCoordinates at the 3 dates
        final TimeStampedPVCoordinates obsPva1Ref = observer.getBaseFrame().getPVCoordinates(obsDate1, gcrf);
        final TimeStampedPVCoordinates obsPva2    = observer.getBaseFrame().getPVCoordinates(obsDate2, itrf);
        final TimeStampedPVCoordinates obsPva2Ref = observer.getBaseFrame().getPVCoordinates(obsDate2, gcrf);
        final TimeStampedPVCoordinates obsPva3Ref = observer.getBaseFrame().getPVCoordinates(obsDate3, gcrf);

        // computation of the estimated orbit
        final IodLaplace iodLaplace = new IodLaplace(mu, gcrf);

        PVCoordinates[] pvList = iodLaplace.buildObserverPositions(obsPva2, itrf, obsDate1, obsDate2, obsDate3);

        // Difference between the reference and the estimated pvList, for position, velocity and acceleration
        double diffPosition = obsPva1Ref.getPosition().distance(pvList[0].getPosition());
        double diffVelocity = obsPva1Ref.getVelocity().distance(pvList[0].getVelocity());
        double diffAcc      = obsPva1Ref.getAcceleration().distance(pvList[0].getAcceleration());

        double diffPosition2 = obsPva2Ref.getPosition().distance(pvList[1].getPosition());
        double diffVelocity2 = obsPva2Ref.getVelocity().distance(pvList[1].getVelocity());
        double diffAcc2      = obsPva2Ref.getAcceleration().distance(pvList[1].getAcceleration());

        double diffPosition3 = obsPva3Ref.getPosition().distance(pvList[2].getPosition());
        double diffVelocity3 = obsPva3Ref.getVelocity().distance(pvList[2].getVelocity());
        double diffAcc3      = obsPva3Ref.getAcceleration().distance(pvList[2].getAcceleration());

        Assertions.assertEquals(0, diffPosition, 10E-8);
        Assertions.assertEquals(0, diffVelocity, 10E-13);
        Assertions.assertEquals(0, diffAcc, 10E-17);

        Assertions.assertEquals(0, diffPosition2, 10E-8);
        Assertions.assertEquals(0, diffVelocity2, 10E-13);
        Assertions.assertEquals(0, diffAcc2, 10E-17);

        Assertions.assertEquals(0, diffPosition3, 10E-8);
        Assertions.assertEquals(0, diffVelocity3, 10E-13);
        Assertions.assertEquals(0, diffAcc3, 10E-17);

    }

    /**
     * Validation of the building of the 3 observer PVCoordinates for the 3 observations, using the building of ground
     * station method
     */
    @Test
    public void testBuildObserverPositions2() {

        // Getting the observer PVCoordinates at the 3 dates
        final TimeStampedPVCoordinates obsPva1Ref = observer.getBaseFrame().getPVCoordinates(obsDate1, gcrf);
        final TimeStampedPVCoordinates obsPva2Ref = observer.getBaseFrame().getPVCoordinates(obsDate2, gcrf);
        final TimeStampedPVCoordinates obsPva3Ref = observer.getBaseFrame().getPVCoordinates(obsDate3, gcrf);

        // computation of the estimated orbit based on Gauss method
        final IodGauss iodgauss = new IodGauss(mu, eme2000);

        PVCoordinates[] pvList = iodgauss.buildObserverPositions(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                 Constants.WGS84_EARTH_FLATTENING,
                                                                 IERSConventions.IERS_2010, false,
                                                                 AbsoluteDate.J2000_EPOCH, FastMath.toRadians(40),
                                                                 FastMath.toRadians(-110), 2000.0, obsDate1, obsDate2,
                                                                 obsDate3);

        // Difference between the reference and the estimated pvList, for position, velocity and acceleration
        Assertions.assertEquals(0, obsPva1Ref.getPosition().distance(pvList[0].getPosition()), 0);
        Assertions.assertEquals(0, obsPva1Ref.getVelocity().distance(pvList[0].getVelocity()), 0);
        Assertions.assertEquals(0, obsPva1Ref.getAcceleration().distance(pvList[0].getAcceleration()), 0);

        Assertions.assertEquals(0, obsPva2Ref.getPosition().distance(pvList[1].getPosition()), 0);
        Assertions.assertEquals(0, obsPva2Ref.getVelocity().distance(pvList[1].getVelocity()), 0);
        Assertions.assertEquals(0, obsPva2Ref.getAcceleration().distance(pvList[1].getAcceleration()), 0);

        Assertions.assertEquals(0, obsPva3Ref.getPosition().distance(pvList[2].getPosition()), 0);
        Assertions.assertEquals(0, obsPva3Ref.getVelocity().distance(pvList[2].getVelocity()), 0);
        Assertions.assertEquals(0, obsPva3Ref.getAcceleration().distance(pvList[2].getAcceleration()), 0);

    }

    /**
     * Validation of the building of the observer PVCoordinates for an observation, using the building of ground
     * station method
     */
    @Test
    public void testBuildObserverPosition() {

        // computation of the estimated orbit based on Gauss method
        final IodGauss iodgauss = new IodGauss(mu, eme2000);

        final TimeStampedPVCoordinates obsPva2 = observer.getBaseFrame().getPVCoordinates(obsDate2, gcrf);

        PVCoordinates pvStation = iodgauss.buildObserverPosition(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                 Constants.WGS84_EARTH_FLATTENING,
                                                                 IERSConventions.IERS_2010, false,
                                                                 AbsoluteDate.J2000_EPOCH, FastMath.toRadians(40),
                                                                 FastMath.toRadians(-110), 2000.0, obsDate2);

        Assertions.assertEquals(0, obsPva2.getPosition().distance(pvStation.getPosition()), 0);
        Assertions.assertEquals(0, obsPva2.getVelocity().distance(pvStation.getVelocity()), 0);
        Assertions.assertEquals(0, obsPva2.getAcceleration().distance(pvStation.getAcceleration()), 0);

    }
}