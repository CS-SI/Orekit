package org.orekit.propagation.relative.maneuver;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireProvider;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireRendezVous;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

class FieldClohessyWiltshireManeuverTest {

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
        final Binary64Field field = Binary64Field.getInstance();
        final double n = 0.00115697;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU / (n * n), 1. / 3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate rdvDate = epoch.shiftedBy(8 * 3600);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngleType.MEAN,
                                                              FramesFactory.getGCRF(), epoch,
                                                              Constants.EIGEN5C_EARTH_MU);

        final FieldKeplerianOrbit<Binary64> fieldTargetOrbit = new FieldKeplerianOrbit<>(field, targetOrbit);

        // Target's QSW LOF
        final LocalOrbitalFrame targetLOF = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW,
                                                                  new KeplerianPropagator(fieldTargetOrbit.toOrbit()),
                                                                  "QSW LOF");

        // Start and end conditions of the transfer, expressed in the target's QSW LOF (data from Curtis book, example 7.2)
        TimeStampedPVCoordinates pvtChaserInitial =
                        new TimeStampedPVCoordinates(epoch, new Vector3D(20.0e3, 20.0e3, 20.0e3),
                                                     new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(rdvDate, Vector3D.ZERO, Vector3D.ZERO);
        TimeStampedFieldPVCoordinates<Binary64> pvtFieldChaserInitial =
                        new TimeStampedFieldPVCoordinates<>(field, pvtChaserInitial);
        TimeStampedFieldPVCoordinates<Binary64> pvtFieldChaserFinal =
                        new TimeStampedFieldPVCoordinates<>(field, pvtChaserFinal);

        final FieldClohessyWiltshireProvider<Binary64> cwProvider =
                        new FieldClohessyWiltshireProvider<>(fieldTargetOrbit, pvtFieldChaserInitial);
        final FieldAbsoluteDate<Binary64> firstImpulseDate = new FieldAbsoluteDate<>(field,
                                                                                     new AbsoluteDate(2000, 1, 1, 11,
                                                                                                      58, 55.816,
                                                                                                      TimeScalesFactory.getUTC()));
        final FieldAbsoluteDate<Binary64> secondImpulseDate = new FieldAbsoluteDate<>(field,
                                                                                      new AbsoluteDate(2000, 1, 1, 19,
                                                                                                       58, 55.816,
                                                                                                       TimeScalesFactory.getUTC()));

        final FieldTwoImpulseTransfer<Binary64> cwRdv =
                        (new FieldClohessyWiltshireRendezVous<Binary64>()).computeRendezVous(pvtFieldChaserInitial,
                                                                                             pvtFieldChaserFinal,
                                                                                             targetLOF,
                                                                                             fieldTargetOrbit);

        final FieldEventDetector<Binary64> firstImpulseTrigger = new FieldDateDetector<>(firstImpulseDate);
        final FieldEventDetector<Binary64> secondImpulseTrigger = new FieldDateDetector<>(secondImpulseDate);

        final FieldVector3D<Binary64> deltaV1 = cwRdv.getDeltaV1();
        final FieldVector3D<Binary64> deltaV2 = cwRdv.getDeltaV2();

        final FieldClohessyWiltshireManeuver<Binary64> maneuver1 =
                        new FieldClohessyWiltshireManeuver<>(firstImpulseTrigger, deltaV1, cwProvider);
        final FieldClohessyWiltshireManeuver<Binary64> maneuver2 =
                        new FieldClohessyWiltshireManeuver<>(secondImpulseTrigger, deltaV2, cwProvider);

        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(fieldTargetOrbit);

        propagator.addAdditionalDataProvider(cwProvider);
        propagator.addEventDetector(maneuver1);
        propagator.addEventDetector(maneuver2);

        final FieldSpacecraftState<Binary64> finalTarget =
                        propagator.propagate(new FieldAbsoluteDate<>(field, rdvDate).shiftedBy(1.));
        final Binary64[] finalChaser = cwProvider.getAdditionalData(finalTarget);

        Assertions.assertEquals(0, finalChaser[0].getReal(), 8.00e-10);
        Assertions.assertEquals(0, finalChaser[1].getReal(), 1.254e-12);
        Assertions.assertEquals(0, finalChaser[2].getReal(), 9.095e-13);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[3].getReal(),
                                5.149e-13);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[4].getReal(),
                                1.254e-12);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[5].getReal(),
                                1e-16);
    }
}
