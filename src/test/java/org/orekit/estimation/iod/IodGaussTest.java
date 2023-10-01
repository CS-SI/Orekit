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
import org.junit.jupiter.api.Test;

import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.Constants;

/**
 * Data based on "Fundamentals of Astrodynamics and Applications", Vallado
 * and "On the performance analysis of Initial Orbit Determination algorithms", Juan Carlos Dolado & Carlos Yanez.
 * @author Asquier Julien
 * @since 11.3.2
 */
public class IodGaussTest extends AbstractIodTest {

     /** Value from example of Vallado , example 7.2.
     * Caution : Values will not be strictly the same as with Vallado results due to difference of use in
     * EOP. It was verified that the differences came from this. see issue #982 and topic related on Orekit forum. */
    @Test
    public void testGaussVallado() {

        final AbsoluteDate obsDate2 = new AbsoluteDate(2012, 8, 20,
                                                       11, 48, 28, TimeScalesFactory.getUTC());

        // Date of the 1st measurement, according to 2nd measurement
        final AbsoluteDate obsDate1 = obsDate2.shiftedBy(-480);
        // Date of the 3rd measurement, according to 2nd measurement
        final AbsoluteDate obsDate3 = obsDate2.shiftedBy(240);

        // Angular value of the 1st radec
        final double   ra1  = FastMath.toRadians(0.9399130);
        final double   dec1 = FastMath.toRadians(18.667717);
        final double[] ang1 = { ra1, dec1 };

        // Angular value of the 2nd radec
        final double   ra2  = FastMath.toRadians(45.025748);
        final double   dec2 = FastMath.toRadians(35.664741);
        final double[] ang2 = { ra2, dec2 };

        // Angular value of the 3rd radec
        final double   ra3  = FastMath.toRadians(67.886655);
        final double   dec3 = FastMath.toRadians(36.996583);
        final double[] ang3 = { ra3, dec3 };

        // Computation of the lines of sight
        final Vector3D los1 = new Vector3D(ang1[0], ang1[1]);
        final Vector3D los2 = new Vector3D(ang2[0], ang2[1]);
        final Vector3D los3 = new Vector3D(ang3[0], ang3[1]);

        // Computation of the observer coordinates at the 3 dates, based on the values of the Vallado example 7.2
        final Vector3D obsP1 = new Vector3D(4054880.1594, 2748194.4767, 4074236.1653);
        final Vector3D obsP2 = new Vector3D(3956223.5179, 2888232.0864, 4074363.4118);
        final Vector3D obsP3 = new Vector3D(3905072.3452, 2956934.5902, 4074429.3009);

        // computation of the estimated orbit based on Gauss method
        final IodGauss iodgauss = new IodGauss(mu);
        final Orbit estimatedOrbit = iodgauss.estimate(eme2000, obsP1, obsDate1, los1,
                                                       obsP2, obsDate2, los2, obsP3, obsDate3, los3);

        // Expected results from Vallado example, with Orekit EOP (difference existing from the Vallado example)
        final PVCoordinates pvOrbit = estimatedOrbit.getPVCoordinates();
        Assertions.assertEquals(6313395.577554352, pvOrbit.getPosition().getX(), 10E-12);
        Assertions.assertEquals(5247523.665414684, pvOrbit.getPosition().getY(), 10E-12);
        Assertions.assertEquals(6467724.780943674, pvOrbit.getPosition().getZ(), 10E-12);

        Assertions.assertEquals(-4101.302835071493, pvOrbit.getVelocity().getX(), 10E-12);
        Assertions.assertEquals(4699.692868088214, pvOrbit.getVelocity().getY(), 10E-12);
        Assertions.assertEquals(1692.5478178378698, pvOrbit.getVelocity().getZ(), 10E-12);
    }

    /** Non-regression test case based on LEO-1 case:
     * "On the performance analysis of Initial Orbit Determination algorithms" with unnoisy data. We have better
     * results on 10^-1 order of magnitude compared to the relative error results of the paper.
     */
    @Test
    public void testGaussLeoSSO() {

        final DateComponents dateComp = new DateComponents(DateComponents.FIFTIES_EPOCH, 21915);
        final AbsoluteDate   obsDate2 = new AbsoluteDate(dateComp, TimeScalesFactory.getUTC());
        // taking the value of the LEO-1 case, and doing a keplerian propagation
        final KeplerianOrbit kepOrbitRef = new KeplerianOrbit(7197934.7, 0., FastMath.toRadians(98.71),
                                                              0., FastMath.toRadians(100.41), 0.,
                                                              PositionAngleType.MEAN, gcrf, obsDate2, Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propRef = new KeplerianPropagator(kepOrbitRef);

        // computation of the estimated orbits for 3 different time of propagation
        final Orbit estimatedOrbit1 = getGaussEstimation(-4, 4, obsDate2, propRef);
        final Orbit estimatedOrbit2 = getGaussEstimation(-37, 37, obsDate2, propRef);
        final Orbit estimatedOrbit3 = getGaussEstimation(-90, 90, obsDate2, propRef);

        //  Computation of the relative errors
        final double relativeRangeError1    = getRelativeRangeError(estimatedOrbit1, kepOrbitRef);
        final double relativeVelocityError1 = getRelativeVelocityError(estimatedOrbit1, kepOrbitRef);
        final double relativeRangeError2    = getRelativeRangeError(estimatedOrbit2, kepOrbitRef);
        final double relativeVelocityError2 = getRelativeVelocityError(estimatedOrbit2, kepOrbitRef);
        final double relativeRangeError3    = getRelativeRangeError(estimatedOrbit3, kepOrbitRef);
        final double relativeVelocityError3 = getRelativeVelocityError(estimatedOrbit3, kepOrbitRef);

        Assertions.assertEquals(0, relativeRangeError1, 10E-6);
        Assertions.assertEquals(0, relativeVelocityError1, 10E-6);

        Assertions.assertEquals(0, relativeRangeError2, 10E-4);
        Assertions.assertEquals(0, relativeVelocityError2, 10E-4);

        Assertions.assertEquals(0, relativeRangeError3, 10E-3);
        Assertions.assertEquals(0, relativeVelocityError3, 10E-3);

    }

    /**   Non-regression test case based on MEO case:
     *   "On the performance analysis of Initial Orbit Determination algorithms" with unnoisy data. We have better
     *   results on 10^-1 order of magnitude compared to the relative error results of the paper.
     */
    @Test
    public void testGaussMEO() {

        final DateComponents dateComp = new DateComponents(DateComponents.FIFTIES_EPOCH, 21915);
        final AbsoluteDate   obsDate2 = new AbsoluteDate(dateComp, TimeScalesFactory.getUTC());
        final KeplerianOrbit kepOrbitRef = new KeplerianOrbit(29600136., 0., FastMath.toRadians(56.),
                                                              0., FastMath.toRadians(55.41), 0.,
                                                              PositionAngleType.MEAN, gcrf, obsDate2, Constants.WGS84_EARTH_MU);
        // taking the value of the MEO case, and doing a keplerian propagation
        final KeplerianPropagator propRef = new KeplerianPropagator(kepOrbitRef);

        // computation of the estimated orbits for 3 different time of propagation
        final Orbit estimatedOrbit1 = getGaussEstimation(-35, 35, obsDate2, propRef);
        final Orbit estimatedOrbit2 = getGaussEstimation(-305, 305, obsDate2, propRef);
        final Orbit estimatedOrbit3 = getGaussEstimation(-760, 760, obsDate2, propRef);

        // computation of the relative errors
        final double relativeRangeError1    = getRelativeRangeError(estimatedOrbit1, kepOrbitRef);
        final double relativeVelocityError1 = getRelativeVelocityError(estimatedOrbit1, kepOrbitRef);
        final double relativeRangeError2    = getRelativeRangeError(estimatedOrbit2, kepOrbitRef);
        final double relativeVelocityError2 = getRelativeVelocityError(estimatedOrbit2, kepOrbitRef);
        final double relativeRangeError3    = getRelativeRangeError(estimatedOrbit3, kepOrbitRef);
        final double relativeVelocityError3 = getRelativeVelocityError(estimatedOrbit3, kepOrbitRef);

        Assertions.assertEquals(0, relativeRangeError1, 10E-6);
        Assertions.assertEquals(0, relativeVelocityError1, 10E-6);

        Assertions.assertEquals(0, relativeRangeError2, 10E-4);
        Assertions.assertEquals(0, relativeVelocityError2, 10E-4);

        Assertions.assertEquals(0, relativeRangeError3, 10E-3);
        Assertions.assertEquals(0, relativeVelocityError3, 10E-3);

    }

    /** Non-regression test case based on GEO-1 case:
     * "On the performance analysis of Initial Orbit Determination algorithms" with unnoisy data. We have better
     * results on 10^-1 order of magnitude compared to the relative error results of the paper.
     */
    @Test
    public void testGaussGEO() {

        final DateComponents dateComp = new DateComponents(DateComponents.FIFTIES_EPOCH, 21915);
        final AbsoluteDate   obsDate2 = new AbsoluteDate(dateComp, TimeScalesFactory.getUTC());
        final KeplerianOrbit kepOrbitRef = new KeplerianOrbit(42164000., 0., 0., 0.,
                                                              FastMath.toRadians(107.33), 0., PositionAngleType.MEAN, gcrf,
                                                              obsDate2, mu);
        // taking the value of the GEO case, and doing a keplerian propagation
        final KeplerianPropagator propRef    = new KeplerianPropagator(kepOrbitRef);

        // computation of the estimated orbits for 3 different time of propagation
        final Orbit estimatedOrbit1 = getGaussEstimation(-60, 60, obsDate2, propRef);
        final Orbit estimatedOrbit2 = getGaussEstimation(-520, 520, obsDate2, propRef);
        final Orbit estimatedOrbit3 = getGaussEstimation(-1300, 1300, obsDate2, propRef);

        // computation of the relative errors
        final double relativeRangeError1    = getRelativeRangeError(estimatedOrbit1, kepOrbitRef);
        final double relativeVelocityError1 = getRelativeVelocityError(estimatedOrbit1, kepOrbitRef);
        final double relativeRangeError2    = getRelativeRangeError(estimatedOrbit2, kepOrbitRef);
        final double relativeVelocityError2 = getRelativeVelocityError(estimatedOrbit2, kepOrbitRef);
        final double relativeRangeError3    = getRelativeRangeError(estimatedOrbit3, kepOrbitRef);
        final double relativeVelocityError3 = getRelativeVelocityError(estimatedOrbit3, kepOrbitRef);

        Assertions.assertEquals(0, relativeRangeError1, 10E-6);
        Assertions.assertEquals(0, relativeVelocityError1, 10E-6);

        Assertions.assertEquals(0, relativeRangeError2, 10E-4);
        Assertions.assertEquals(0, relativeVelocityError2, 10E-4);

        Assertions.assertEquals(0, relativeRangeError3, 10E-3);
        Assertions.assertEquals(0, relativeVelocityError3, 10E-3);

    }

    /** Non-regression test case comparing the results between the different IOD methods. The relative error taking as
     * a reference the other IOD is giving an error of the position and velocity with an approximation < 10^-2. Gooding
     * was not made due to not having good tuning parameters
     */
    @Test
    public void testGaussComparisonLeoSSO() {
        final DateComponents dateComp = new DateComponents(DateComponents.FIFTIES_EPOCH, 21915);
        final AbsoluteDate   obsDate2 = new AbsoluteDate(dateComp, TimeScalesFactory.getUTC());
        final KeplerianOrbit kepOrbitRef = new KeplerianOrbit(7197934.0, 0., FastMath.toRadians(98.71),
                                                              0., FastMath.toRadians(100.41), 0.,
                                                              PositionAngleType.MEAN, gcrf, obsDate2, Constants.WGS84_EARTH_MU);
        final KeplerianPropagator propRef    = new KeplerianPropagator(kepOrbitRef);

        // Date of the 1st measurement, according to 2nd measurement
        final AbsoluteDate obsDate1 = obsDate2.shiftedBy(-120);

        // Date of the 3rd measurement, according to 2nd measurement
        final AbsoluteDate obsDate3 = obsDate2.shiftedBy(120);

        // Computation of the LOS angles
        // Computation of the observer coordinates
        final Vector3D obsP1 = observer.getBaseFrame().getPosition(obsDate1, gcrf);
        final TimeStampedPVCoordinates obsP2 = observer.getBaseFrame().getPVCoordinates(obsDate2, gcrf);
        final Vector3D obsP3 = observer.getBaseFrame().getPosition(obsDate3, gcrf);

        final Vector3D los1 = getLOSAngles(propRef, obsDate1);
        final Vector3D los2 = getLOSAngles(propRef, obsDate2);
        final Vector3D los3 = getLOSAngles(propRef, obsDate3);

        // Computation of the estimated orbit with iod gauss
        final IodGauss iodgauss = new IodGauss(mu);
        final Orbit estimatedGauss = iodgauss.estimate(eme2000, obsP1, obsDate1, los1,
                                                       obsP2.getPosition(), obsDate2, los2, obsP3, obsDate3, los3);
        // Computation of the estimated orbit with iod laplace
        final IodLaplace iodLaplace = new IodLaplace(Constants.WGS84_EARTH_MU);
        final Orbit estimatedLaplace = iodLaplace.estimate(eme2000, obsP2, obsDate1,
                                                           los1, obsDate2, los2, obsDate3, los3);
        // Computation of the estimated orbit with iod lambert
        final IodLambert iodLambert = new IodLambert(Constants.WGS84_EARTH_MU);
        final Orbit estimatedKepLambert = iodLambert.estimate(eme2000, true, 0,
                                                              propRef.getPosition(obsDate1, eme2000), obsDate1,
                                                              propRef.getPosition(obsDate2,
                                                                                  eme2000), obsDate2);
        final Orbit estimatedLambert = new CartesianOrbit(estimatedKepLambert);
        // Computation of the estimated orbit with iod gibbs
        final IodGibbs iodGibbs = new IodGibbs(Constants.WGS84_EARTH_MU);
        final Orbit estimatedKepGibbs = iodGibbs.estimate(eme2000, propRef.getPosition(obsDate1, eme2000),
                                                          obsDate1, propRef.getPosition(obsDate2, eme2000), obsDate2,
                                                          propRef.getPosition(obsDate3, eme2000), obsDate3);
        final Orbit estimatedGibbs = new CartesianOrbit(estimatedKepGibbs);

        // Relative error with reference keplerian propagation
        final double relativeRangeError1    = getRelativeRangeError(estimatedGauss, kepOrbitRef);
        final double relativeVelocityError1 = getRelativeVelocityError(estimatedGauss, kepOrbitRef);

        // Relative error with reference Gibbs estimation
        final double relativeRangeError2    = getRelativeRangeError(estimatedGauss, estimatedGibbs);
        final double relativeVelocityError2 = getRelativeVelocityError(estimatedGauss, estimatedGibbs);

        // Relative error with reference Laplace estimation
        final double relativeRangeError3    = getRelativeRangeError(estimatedGauss, estimatedLaplace);
        final double relativeVelocityError3 = getRelativeVelocityError(estimatedGauss, estimatedLaplace);

        // Relative error with reference Lambert estimation
        final double relativeRangeError4    = getRelativeRangeError(estimatedGauss, estimatedLambert);
        final double relativeVelocityError4 = getRelativeVelocityError(estimatedGauss, estimatedLambert);

        Assertions.assertEquals(0, relativeRangeError1, 10E-2);
        Assertions.assertEquals(0, relativeVelocityError1, 10E-2);

        Assertions.assertEquals(0, relativeRangeError2, 10E-2);
        Assertions.assertEquals(0, relativeVelocityError2, 10E-2);

        Assertions.assertEquals(0, relativeRangeError3, 10E-2);
        Assertions.assertEquals(0, relativeVelocityError3, 10E-2);

        Assertions.assertEquals(0, relativeRangeError4, 10E-2);
        Assertions.assertEquals(0, relativeVelocityError4, 10E-2);

    }

    @Test
    public void testLaplaceKeplerianWithAzEl() {
        // Settings
        final AbsoluteDate date = new AbsoluteDate(2019, 9, 29, 22, 0, 2.0, TimeScalesFactory.getUTC());
        final KeplerianOrbit kep = new KeplerianOrbit(6798938.970424857, 0.0021115522920270016, 0.9008866630545347,
            1.8278985811406743, -2.7656136723308524,
            0.8823034512437679, PositionAngleType.MEAN, gcrf,
            date, Constants.EGM96_EARTH_MU);
        final KeplerianPropagator prop = new KeplerianPropagator(kep);

        // Measurements
        final AngularAzEl azEl1 = getAzEl(prop, date);
        final AngularAzEl azEl2 = getAzEl(prop, date.shiftedBy(5.0));
        final AngularAzEl azEl3 = getAzEl(prop, date.shiftedBy(10.0));

        // With only 3 measurements, we can expect ~400 meters error in position and ~1 m/s in velocity
        final Orbit estOrbit = new IodGauss(Constants.EGM96_EARTH_MU).estimate(gcrf, azEl1, azEl2, azEl3);

        // Verify
        final TimeStampedPVCoordinates truth = prop.getPVCoordinates(azEl2.getDate(), gcrf);
        Assertions.assertEquals(0.0, Vector3D.distance(truth.getPosition(), estOrbit.getPosition()), 262.0);
        Assertions.assertEquals(0.0, Vector3D.distance(truth.getVelocity(), estOrbit.getPVCoordinates().getVelocity()), 0.3);
    }

    // Private method to have a gauss estimated orbit
    private Orbit getGaussEstimation(final double deltaT1, final double deltaT3, final AbsoluteDate obsDate2,
                                     final Propagator prop) {

        // Date of the 1st measurement and 3rd, according to 2nd measurement
        final AbsoluteDate obsDate1 = obsDate2.shiftedBy(deltaT1);
        final AbsoluteDate obsDate3 = obsDate2.shiftedBy(deltaT3);

        // Computation of the 3 LOS
        final Vector3D los1 = getLOSAngles(prop, obsDate1);
        final Vector3D los2 = getLOSAngles(prop, obsDate2);
        final Vector3D los3 = getLOSAngles(prop, obsDate3);

        // Computation of the observer coordinates at the 3 times of measurements
        final Vector3D obsPva  = observer.getBaseFrame().getPosition(obsDate1, gcrf);
        final Vector3D obsPva2 = observer.getBaseFrame().getPosition(obsDate2, gcrf);
        final Vector3D obsPva3 = observer.getBaseFrame().getPosition(obsDate3, gcrf);

        // Gauss estimation
        final IodGauss iodgauss = new IodGauss(mu);
        return iodgauss.estimate(gcrf, obsPva, obsDate1, los1, obsPva2, obsDate2, los2, obsPva3, obsDate3, los3);

    }

}
