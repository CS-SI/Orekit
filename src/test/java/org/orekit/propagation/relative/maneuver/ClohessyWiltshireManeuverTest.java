package org.orekit.propagation.relative.maneuver;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireProvider;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireRendezVous;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

class ClohessyWiltshireManeuverTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Test of the maneuver class on a two impulse rendezvous scenario, impulse dates and deltaV are computed using
     * computeRendezVous method from ClohessyWiltshireRendezVous class. At the end of the propagation, i.e. one second
     * after second maneuver, the chaser spacecraft relative position must be (0,0,0) and the relative velocity must be
     * (0.,0.,0.).
     */
    @Test
    void rdvManeuverTest() {
        Utils.setDataRoot("regular-data");

        final double n = 0.00115697; // Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU / (n * n), 1. / 3.); // Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate rdvDate = epoch.shiftedBy(8 * 3600);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngleType.MEAN,
                                                              FramesFactory.getGCRF(), epoch,
                                                              Constants.EIGEN5C_EARTH_MU);

        // Target's QSW LOF
        final LocalOrbitalFrame targetLof =
                        new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, new KeplerianPropagator(targetOrbit),
                                              "QSW LOF target");

        // Start and end conditions of the transfer, expressed in the target's LOF (data from Curtis book, example 7.2)
        TimeStampedPVCoordinates pvtChaserInitial =
                        new TimeStampedPVCoordinates(epoch, new Vector3D(20.0e3, 20.0e3, 20.0e3),
                                                     new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(rdvDate, Vector3D.ZERO, Vector3D.ZERO);

        final TimeStampedPVCoordinates pvtChaserInitialInert =
                        targetLof.getTransformTo(FramesFactory.getEME2000(), epoch)
                                 .transformPVCoordinates(pvtChaserInitial);
        final ClohessyWiltshireProvider cwProvider =
                        new ClohessyWiltshireProvider(targetOrbit, pvtChaserInitialInert, FramesFactory.getEME2000());

        final AbsoluteDate firstImpulseDate = new AbsoluteDate(2000, 1, 1, 11, 58, 55.816, TimeScalesFactory.getUTC());
        final AbsoluteDate secondImpulseDate = new AbsoluteDate(2000, 1, 1, 19, 58, 55.816, TimeScalesFactory.getUTC());

        final TwoImpulseTransfer cwRDV =
                        ClohessyWiltshireRendezVous.computeRendezVous(pvtChaserInitial, pvtChaserFinal, targetLof,
                                                                      targetOrbit);

        final EventDetector firstImpulseTrigger = new DateDetector(firstImpulseDate);
        final EventDetector secondImpulseTrigger = new DateDetector(secondImpulseDate);

        final Vector3D deltaV1 = cwRDV.getDeltaV1();
        final Vector3D deltaV2 = cwRDV.getDeltaV2();

        final ClohessyWiltshireManeuver maneuver1 =
                        new ClohessyWiltshireManeuver(firstImpulseTrigger, deltaV1, cwProvider);

        final ClohessyWiltshireManeuver maneuver2 =
                        new ClohessyWiltshireManeuver(secondImpulseTrigger, deltaV2, cwProvider);
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);

        propagator.addEventDetector(maneuver1);
        propagator.addEventDetector(maneuver2);

        final SpacecraftState finalTarget = propagator.propagate(rdvDate.shiftedBy(1.));
        final double[] finalChaser = cwProvider.getAdditionalData(finalTarget);

        Assertions.assertEquals(0, finalChaser[0], 1.607e-8);
        Assertions.assertEquals(0, finalChaser[1], 6.357e-7);
        Assertions.assertEquals(0, finalChaser[2], 3.649e-12);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[3], 1.285e-11);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[4], 3.013e-11);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[5], 1.066e-14);
    }
}
