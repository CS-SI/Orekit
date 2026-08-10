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
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenRendezVous;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

class YamanakaAnkersenManeuverTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Test of the maneuver class on a two impulse rendezvous scenario, impulse dates and deltaV are computed using
     * computeRendezVous method from RDV class. At the end of the propagation, i.e. one second after second maneuver,
     * the chaser spacecraft relative position must be (0,0,0) and the relative velocity must be (0.,0.,0.).
     */
    @Test
    void rdvManeuverTest() {
        Utils.setDataRoot("regular-data");

        final double n = 0.00115697;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU / (n * n), 1. / 3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate rdvDate = epoch.shiftedBy(8 * 3600);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngleType.MEAN,
                                                              FramesFactory.getGCRF(), epoch,
                                                              Constants.EIGEN5C_EARTH_MU);

        // Target's LVLH CCSDS LOF
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS,
                                                                  new KeplerianPropagator(targetOrbit),
                                                                  "LVLH CCSDS LOF target");
        // Target's LVLH QSW LOF
        final LocalOrbitalFrame targetLofQSW =
                        new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, new KeplerianPropagator(targetOrbit),
                                              "QSW LOF target");

        // Start and end conditions of the transfer, expressed in the target's QSW LOF (Curtis book, example 7.2)
        TimeStampedPVCoordinates pvtChaserInitialQSW =
                        new TimeStampedPVCoordinates(epoch, new Vector3D(20.0e3, 20.0e3, 20.0e3),
                                                     new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinalQSW =
                        new TimeStampedPVCoordinates(rdvDate, Vector3D.ZERO, Vector3D.ZERO);

        //Conversion of transfer conditions to LVLH CCSDS frame to use computeRendezVous Method of YamanakaAnkersenRendezVous class.
        TimeStampedPVCoordinates pvtChaserInitial =
                        targetLofQSW.getTransformTo(targetLof, pvtChaserInitialQSW.getDate())
                                    .transformPVCoordinates(pvtChaserInitialQSW);
        TimeStampedPVCoordinates pvtChaserFinal = targetLofQSW.getTransformTo(targetLof, pvtChaserFinalQSW.getDate())
                                                              .transformPVCoordinates(pvtChaserFinalQSW);

        final YamanakaAnkersenProvider yaProvider =
                        new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial, "YamanakaAnkersen");

        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);

        final TwoImpulseTransfer yaRDV =
                        YamanakaAnkersenRendezVous.computeRendezVous(pvtChaserInitial, pvtChaserFinal, targetLof,
                                                                     targetOrbit, new KeplerianPropagator(targetOrbit));
        final AbsoluteDate firstImpulseDate = yaRDV.getPvt1().getDate();
        final AbsoluteDate secondImpulseDate = yaRDV.getPvt2().getDate();
        // The first impulse date is the same as the starting propagation date, this date is slightly shifted to ensure that the maneuver will be performed.
        final DateDetector firstImpulseTrigger = new DateDetector(firstImpulseDate.shiftedBy(1e-12));
        final DateDetector secondImpulseTrigger = new DateDetector(secondImpulseDate);

        final Vector3D deltaV1 = yaRDV.getDeltaV1();
        final Vector3D deltaV2 = yaRDV.getDeltaV2();

        final YamanakaAnkersenManeuver maneuver1 =
                        new YamanakaAnkersenManeuver(firstImpulseTrigger, deltaV1, yaProvider);

        final YamanakaAnkersenManeuver maneuver2 =
                        new YamanakaAnkersenManeuver(secondImpulseTrigger, deltaV2, yaProvider);
        propagator.addAdditionalDataProvider(yaProvider);
        propagator.addEventDetector(maneuver1);
        propagator.addEventDetector(maneuver2);

        final SpacecraftState finalTarget = propagator.propagate(rdvDate.shiftedBy(1.));
        final double[] finalChaser = yaProvider.getAdditionalData(finalTarget);

        Assertions.assertEquals(0, finalChaser[0], 7.330e-8);
        Assertions.assertEquals(0, finalChaser[1], 4.931e-10);
        Assertions.assertEquals(0, finalChaser[2], 1.543e-9);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[3], 6.943e-12);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[4], 9.882e-14);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[5], 1.152e-12);
    }
}
