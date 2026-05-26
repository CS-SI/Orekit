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
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenProvider;
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenRendezVous;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class FieldYamanakaAnkersenManeuverTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-6;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    // Test of the maneuver class on a two impulse rendezvous scenario, impulse dates and deltaV are computed using computeRendezVous method from ClohessyWiltshireRendezVous class.
    // At the end of the propagation, i.e. one second after second maneuver, the chaser spacecraft relative position must be (0,0,0) and the relative velocity must be (0.,0.,0.).

    @Test
    public void rdvManeuverTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final double n = 0.00115697;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n), 1./3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate rdvDate = epoch.shiftedBy(8*3600);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        final FieldKeplerianOrbit<Binary64> fieldTargetOrbit = new FieldKeplerianOrbit<>(field, targetOrbit);

        // Target's LVLH CCSDS LOF
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, new KeplerianPropagator(targetOrbit), "LVLH CCSDS LOF target");
        // Target's LVLH QSW LOF
        final LocalOrbitalFrame targetLofQSW = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, new KeplerianPropagator(targetOrbit), "QSW LOF target");

        // Start and end conditions of the transfer, expressed in the target's  QSW LOF (data from Curtis book, example 7.2)
        TimeStampedPVCoordinates pvtChaserInitialQSW = new TimeStampedPVCoordinates(epoch, new Vector3D(20.0e3, 20.0e3, 20.0e3), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinalQSW = new TimeStampedPVCoordinates(rdvDate, Vector3D.ZERO, Vector3D.ZERO);
        //Conversion of transfer conditions to LVLH CCSDS frame to use computeRendezVous Method of YamanakaAnkersenRendezVous class.
        TimeStampedPVCoordinates pvtChaserInitial = targetLofQSW.getTransformTo(targetLof,pvtChaserInitialQSW.getDate()).transformPVCoordinates(pvtChaserInitialQSW);
        TimeStampedPVCoordinates pvtChaserFinal = targetLofQSW.getTransformTo(targetLof, pvtChaserFinalQSW.getDate()).transformPVCoordinates(pvtChaserFinalQSW);


        TimeStampedFieldPVCoordinates<Binary64> pvtFieldChaserInitial = new TimeStampedFieldPVCoordinates<>(field,pvtChaserInitial);
        TimeStampedFieldPVCoordinates<Binary64> pvtFieldChaserFinal = new TimeStampedFieldPVCoordinates<>(field,pvtChaserFinal);

        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(fieldTargetOrbit,pvtFieldChaserInitial);
        final FieldAbsoluteDate<Binary64> firstImpulseDate = new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()));
        final FieldAbsoluteDate<Binary64> secondImpulseDate = new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,19,58,55.816,TimeScalesFactory.getUTC()));

        final FieldTwoImpulseTransfer<Binary64> yaRdv = (new FieldYamanakaAnkersenRendezVous<Binary64>()).computeRendezVous(pvtFieldChaserInitial, pvtFieldChaserFinal, targetLof, fieldTargetOrbit, new FieldKeplerianPropagator<>(fieldTargetOrbit));

        final FieldEventDetector<Binary64> firstImpulseTrigger = new FieldDateDetector<>(firstImpulseDate.shiftedBy(1e-12));
        final FieldEventDetector<Binary64> secondImpulseTrigger = new FieldDateDetector<>(secondImpulseDate);

        final FieldVector3D<Binary64> deltaV1 = yaRdv.getDeltaV1();
        final FieldVector3D<Binary64> deltaV2 = yaRdv.getDeltaV2();

        final FieldYamanakaAnkersenManeuver<Binary64> maneuver1 = new FieldYamanakaAnkersenManeuver<>(firstImpulseTrigger,deltaV1,yaProvider);
        final FieldYamanakaAnkersenManeuver<Binary64> maneuver2 = new FieldYamanakaAnkersenManeuver<>(secondImpulseTrigger,deltaV2,yaProvider);

        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(fieldTargetOrbit);

        propagator.addAdditionalDataProvider(yaProvider);
        propagator.addEventDetector(maneuver1);
        propagator.addEventDetector(maneuver2);

        final FieldSpacecraftState<Binary64> finalTarget = propagator.propagate(new FieldAbsoluteDate<>(field, rdvDate).shiftedBy(1.));
        final Binary64[] finalChaser = yaProvider.getAdditionalData(finalTarget);

        Assertions.assertEquals(0, finalChaser[0].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, finalChaser[1].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, finalChaser[2].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[3].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[4].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0., maneuver2.getRelativeProvider().getAdditionalData(finalTarget)[5].getReal(), NUMERICAL_TOLERANCE);
    }
}
