/* Copyright 2002-2026 CS GROUP
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
package org.orekit.control.relative;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

class CoellipticOrbitTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testComputeChaserOrbit() {
        // Test of the ChaserOrbit computation when 0 < theta < 90.
        final KeplerianOrbit targetOrbit1 =
                        new KeplerianOrbit(7000, 0.1, 0.2, 0., 0.3, 0, PositionAngleType.TRUE, FramesFactory.getGCRF(),
                                           AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU);
        final KeplerianOrbit chaserOrbit1 = CoellipticOrbit.computeChaserOrbit(targetOrbit1, 50, 10, 10, 50, 0.5);
        Assertions.assertEquals(6994.69483523027, chaserOrbit1.getA(), 0);
        Assertions.assertEquals(0.10714285714285715, chaserOrbit1.getE(), 0);
        Assertions.assertEquals(0.20125483918262432, chaserOrbit1.getI(), 0);
        Assertions.assertEquals(0.3034262039009563, chaserOrbit1.getRightAscensionOfAscendingNode(), 0);
        Assertions.assertEquals(targetOrbit1.getPerigeeArgument(), chaserOrbit1.getPerigeeArgument(), 0);
        Assertions.assertEquals(-0.0019293365036251503, chaserOrbit1.getTrueAnomaly(), 0);
        Assertions.assertEquals(targetOrbit1.getMu(), chaserOrbit1.getMu(), 0);
        Assertions.assertEquals(targetOrbit1.getDate().toDouble(), chaserOrbit1.getDate().toDouble(), 0);

        // Test of the ChaserOrbit computation when 90 < theta < 180.
        final KeplerianOrbit targetOrbit2 =
                        new KeplerianOrbit(7000, 0.1, 0.2, 0.5, 0.3, 0, PositionAngleType.TRUE, FramesFactory.getGCRF(),
                                           AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU);
        KeplerianOrbit chaserOrbit2 = CoellipticOrbit.computeChaserOrbit(targetOrbit2, 50, 10, 10, 50, 1.2);
        Assertions.assertEquals(6994.69483523027, chaserOrbit2.getA(), 0);
        Assertions.assertEquals(0.10714285714285715, chaserOrbit2.getE(), 0);
        Assertions.assertEquals(0.19982089120131044, chaserOrbit2.getI(), 1e-13);
        Assertions.assertEquals(0.30713712790199454, chaserOrbit2.getRightAscensionOfAscendingNode(), 1e-14);
        Assertions.assertEquals(targetOrbit2.getPerigeeArgument(), chaserOrbit2.getPerigeeArgument(), 0);
        Assertions.assertEquals(-0.005566289089951616, chaserOrbit2.getTrueAnomaly(), 1e-16);

        // Test of the ChaserOrbit computation when 180 < theta < 270.
        final KeplerianOrbit targetOrbit3 =
                        new KeplerianOrbit(7000, 0.1, 0.2, 1.5, 0.3, 0, PositionAngleType.TRUE, FramesFactory.getGCRF(),
                                           AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU);
        KeplerianOrbit chaserOrbit3 = CoellipticOrbit.computeChaserOrbit(targetOrbit3, 50, 10, 10, 50, 2.5);
        Assertions.assertEquals(6994.69483523027, chaserOrbit3.getA(), 0);
        Assertions.assertEquals(0.10714285714285715, chaserOrbit3.getE(), 0);
        Assertions.assertEquals(0.19906912003085314, chaserOrbit3.getI(), 1e-15);
        Assertions.assertEquals(0.2945329274085395, chaserOrbit3.getRightAscensionOfAscendingNode(), 0);
        Assertions.assertEquals(targetOrbit3.getPerigeeArgument(), chaserOrbit3.getPerigeeArgument(), 0);
        Assertions.assertEquals(0.006786666554093778, chaserOrbit3.getTrueAnomaly(), 0);

        // Test of the ChaserOrbit computation when 270 < theta < 360.
        final KeplerianOrbit targetOrbit4 =
                        new KeplerianOrbit(7000, 0.1, 0.2, 2.3, 0.3, 0, PositionAngleType.TRUE, FramesFactory.getGCRF(),
                                           AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU);
        KeplerianOrbit chaserOrbit4 = CoellipticOrbit.computeChaserOrbit(targetOrbit4, 50, 10, 10, 50, 3.5);
        Assertions.assertEquals(6994.69483523027, chaserOrbit4.getA(), 0);
        Assertions.assertEquals(0.10714285714285715, chaserOrbit4.getE(), 0);
        Assertions.assertEquals(0.20126610762652408, chaserOrbit4.getI(), 0);
        Assertions.assertEquals(0.29667991467832944, chaserOrbit4.getRightAscensionOfAscendingNode(), 0);
        Assertions.assertEquals(targetOrbit4.getPerigeeArgument(), chaserOrbit4.getPerigeeArgument(), 0);
        Assertions.assertEquals(0.004682476087922024, chaserOrbit4.getTrueAnomaly(), 0);
    }

    /**
     * Computation of the Co-elliptic orbit. Then propagate the target and the chaser orbit by half the target Period
     * and then by targetPeriod. Initially the chaser position in LVLH CCSDS LOF is (0,0,100). Chaser position should be
     * the same after one target Period. Chaser position after half target Period should be (0,0,-100).
     */
    @Test
    void testCoEllipticOrbitWithoutManeuver() {
        final double n = 0.0011569;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU / (n * n), 1. / 3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.1, 0., 0.0, 0.0, 0., PositionAngleType.TRUE,
                                                              FramesFactory.getGCRF(), epoch,
                                                              Constants.EIGEN5C_EARTH_MU);
        final KeplerianPropagator targetPropagator = new KeplerianPropagator(targetOrbit);

        final LocalOrbitalFrame lof =
                        new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, targetPropagator,
                                              "LVLH CCSDS LOF");
        final KeplerianOrbit chaserOrbit = CoellipticOrbit.computeChaserOrbit(targetOrbit, 100, 0, 0, 0, 0);

        final double targetPeriod = targetOrbit.getKeplerianPeriod();

        final KeplerianPropagator chaserPropagator = new KeplerianPropagator(chaserOrbit);

        final SpacecraftState chaserHalfPeriod = chaserPropagator.propagate(epoch.shiftedBy(targetPeriod / 2));
        final Vector3D chaserHalfPeriodPosition =
                        FramesFactory.getGCRF().getTransformTo(lof, chaserHalfPeriod.getDate())
                                     .transformPosition(chaserHalfPeriod.getPosition());
        TestUtils.validateVector3D(new Vector3D(0, 0, -100), chaserHalfPeriodPosition, 1e-9);

        final SpacecraftState chaserPeriod = chaserPropagator.propagate(epoch.shiftedBy(targetPeriod));
        final Vector3D chaserPeriodPosition = FramesFactory.getGCRF().getTransformTo(lof, chaserPeriod.getDate())
                                                           .transformPosition(chaserPeriod.getPosition());
        TestUtils.validateVector3D(FramesFactory.getGCRF().getTransformTo(lof, epoch.shiftedBy(targetPeriod))
                                                .transformPosition(chaserOrbit.getPosition()), chaserPeriodPosition,
                                   1.63e-9);
    }

    @Test
    void testComputeFieldChaserOrbit() {
        final Binary64Field field = Binary64Field.getInstance();
        // Test of the ChaserOrbit computation when 0 < theta < 90.
        final FieldKeplerianOrbit<Binary64> targetOrbit1 =
                        new FieldKeplerianOrbit<>(new Binary64(7000), new Binary64(0.1), new Binary64(0.2),
                                                  new Binary64(0.), new Binary64(0.3), new Binary64(0),
                                                  PositionAngleType.TRUE, FramesFactory.getGCRF(),
                                                  new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH),
                                                  new Binary64(Constants.EIGEN5C_EARTH_MU));
        final FieldKeplerianOrbit<Binary64> chaserOrbit1 =
                        CoellipticOrbit.computeChaserOrbit(targetOrbit1, new Binary64(50), new Binary64(10),
                                                           new Binary64(10), new Binary64(50), new Binary64(0.5));
        Assertions.assertEquals(6994.69483523027, chaserOrbit1.getA().getReal(), 0);
        Assertions.assertEquals(0.10714285714285715, chaserOrbit1.getE().getReal(), 0);
        Assertions.assertEquals(0.20125483918262432, chaserOrbit1.getI().getReal(), 0);
        Assertions.assertEquals(0.3034262039009563, chaserOrbit1.getRightAscensionOfAscendingNode().getReal(), 0);
        Assertions.assertEquals(targetOrbit1.getPerigeeArgument().getReal(),
                                chaserOrbit1.getPerigeeArgument().getReal(), 0);
        Assertions.assertEquals(-0.0019293365036251503, chaserOrbit1.getTrueAnomaly().getReal(), 0);
        Assertions.assertEquals(targetOrbit1.getMu().getReal(), chaserOrbit1.getMu().getReal(), 0);
        Assertions.assertEquals(targetOrbit1.getDate().toAbsoluteDate().toDouble(),
                                chaserOrbit1.getDate().toAbsoluteDate().toDouble(), 0);

        // Test of the ChaserOrbit computation when 90 < theta < 180.
        final FieldKeplerianOrbit<Binary64> targetOrbit2 =
                        new FieldKeplerianOrbit<>(new Binary64(7000), new Binary64(0.1), new Binary64(0.2),
                                                  new Binary64(0.5), new Binary64(0.3), new Binary64(0),
                                                  PositionAngleType.TRUE, FramesFactory.getGCRF(),
                                                  new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH),
                                                  new Binary64(Constants.EIGEN5C_EARTH_MU));
        FieldKeplerianOrbit<Binary64> chaserOrbit2 =
                        CoellipticOrbit.computeChaserOrbit(targetOrbit2, new Binary64(50), new Binary64(10),
                                                           new Binary64(10), new Binary64(50), new Binary64(1.2));
        Assertions.assertEquals(6994.69483523027, chaserOrbit2.getA().getReal(), 0);
        Assertions.assertEquals(0.10714285714285715, chaserOrbit2.getE().getReal(), 0);
        Assertions.assertEquals(0.19982089120131044, chaserOrbit2.getI().getReal(), 1e-13);
        Assertions.assertEquals(0.30713712790199454, chaserOrbit2.getRightAscensionOfAscendingNode().getReal(), 1e-14);
        Assertions.assertEquals(targetOrbit2.getPerigeeArgument().getReal(),
                                chaserOrbit2.getPerigeeArgument().getReal(), 0);
        Assertions.assertEquals(-0.005566289089951616, chaserOrbit2.getTrueAnomaly().getReal(), 1e-16);

        // Test of the ChaserOrbit computation when 180 < theta < 270.
        final FieldKeplerianOrbit<Binary64> targetOrbit3 =
                        new FieldKeplerianOrbit<>(new Binary64(7000), new Binary64(0.1), new Binary64(0.2),
                                                  new Binary64(1.5), new Binary64(0.3), new Binary64(0),
                                                  PositionAngleType.TRUE, FramesFactory.getGCRF(),
                                                  new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH),
                                                  new Binary64(Constants.EIGEN5C_EARTH_MU));
        FieldKeplerianOrbit<Binary64> chaserOrbit3 =
                        CoellipticOrbit.computeChaserOrbit(targetOrbit3, new Binary64(50), new Binary64(10),
                                                           new Binary64(10), new Binary64(50), new Binary64(2.5));
        Assertions.assertEquals(6994.69483523027, chaserOrbit3.getA().getReal(), 0);
        Assertions.assertEquals(0.10714285714285715, chaserOrbit3.getE().getReal(), 0);
        Assertions.assertEquals(0.19906912003085314, chaserOrbit3.getI().getReal(), 1e-15);
        Assertions.assertEquals(0.2945329274085395, chaserOrbit3.getRightAscensionOfAscendingNode().getReal(), 0);
        Assertions.assertEquals(targetOrbit3.getPerigeeArgument().getReal(),
                                chaserOrbit3.getPerigeeArgument().getReal(), 0);
        Assertions.assertEquals(0.006786666554093778, chaserOrbit3.getTrueAnomaly().getReal(), 0);

        // Test of the ChaserOrbit computation when 270 < theta < 360.
        final FieldKeplerianOrbit<Binary64> targetOrbit4 =
                        new FieldKeplerianOrbit<>(new Binary64(7000), new Binary64(0.1), new Binary64(0.2),
                                                  new Binary64(2.3), new Binary64(0.3), new Binary64(0),
                                                  PositionAngleType.TRUE, FramesFactory.getGCRF(),
                                                  new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH),
                                                  new Binary64(Constants.EIGEN5C_EARTH_MU));
        FieldKeplerianOrbit<Binary64> chaserOrbit4 =
                        CoellipticOrbit.computeChaserOrbit(targetOrbit4, new Binary64(50), new Binary64(10),
                                                           new Binary64(10), new Binary64(50), new Binary64(3.5));
        Assertions.assertEquals(6994.69483523027, chaserOrbit4.getA().getReal(), 0);
        Assertions.assertEquals(0.10714285714285715, chaserOrbit3.getE().getReal(), 0);
        Assertions.assertEquals(0.20126610762652408, chaserOrbit4.getI().getReal(), 0);
        Assertions.assertEquals(0.29667991467832944, chaserOrbit4.getRightAscensionOfAscendingNode().getReal(), 0);
        Assertions.assertEquals(targetOrbit4.getPerigeeArgument().getReal(),
                                chaserOrbit4.getPerigeeArgument().getReal(), 0);
        Assertions.assertEquals(0.004682476087922024, chaserOrbit4.getTrueAnomaly().getReal(), 0);
    }

    /**
     * Computation of the Co-elliptic orbit. Then propagate the target and the chaser orbit by half the target Period
     * and then by targetPeriod. Initially the chaser position in LVLH CCSDS LOF is (0,0,100). Chaser position should be
     * the same after one target Period. Chaser position after half target Period should be (0,0,-100).
     */
    @Test
    void testFieldCoEllipticOrbitWithoutManeuver() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = ((new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n))).pow(
                        1. / 3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit =
                        new FieldKeplerianOrbit<>(rTarget, new Binary64(0.1), new Binary64(0.), new Binary64(0.0),
                                                  new Binary64(0.0), new Binary64(0.), PositionAngleType.TRUE,
                                                  FramesFactory.getGCRF(), epoch,
                                                  new Binary64(Constants.EIGEN5C_EARTH_MU));

        final LocalOrbitalFrame lof =
                        new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, targetOrbit.toOrbit(),
                                              "LVLH CCSDS LOF");
        final FieldKeplerianOrbit<Binary64> chaserOrbit =
                        CoellipticOrbit.computeChaserOrbit(targetOrbit, new Binary64(100), new Binary64(0),
                                                           new Binary64(0), new Binary64(0), new Binary64(0));

        final Binary64 targetPeriod = targetOrbit.getKeplerianPeriod();

        final FieldKeplerianPropagator<Binary64> chaserPropagator = new FieldKeplerianPropagator<>(chaserOrbit);

        final FieldSpacecraftState<Binary64> chaserHalfPeriod =
                        chaserPropagator.propagate(epoch.shiftedBy(targetPeriod.divide(2)));
        final FieldVector3D<Binary64> chaserHalfPeriodPosition =
                        FramesFactory.getGCRF().getTransformTo(lof, chaserHalfPeriod.getDate())
                                     .transformPosition(chaserHalfPeriod.getPosition());
        TestUtils.validateVector3D(new Vector3D(0, 0, -100), chaserHalfPeriodPosition.toVector3D(), 1.e-9);

        final FieldSpacecraftState<Binary64> chaserPeriod = chaserPropagator.propagate(epoch.shiftedBy(targetPeriod));
        final FieldVector3D<Binary64> chaserPeriodPosition =
                        FramesFactory.getGCRF().getTransformTo(lof, chaserPeriod.getDate())
                                     .transformPosition(chaserPeriod.getPosition());
        TestUtils.validateVector3D(FramesFactory.getGCRF().getTransformTo(lof, epoch.shiftedBy(targetPeriod))
                                                .transformPosition(chaserOrbit.getPosition()).toVector3D(),
                                   chaserPeriodPosition.toVector3D(), 1.63e-9);
    }
}
