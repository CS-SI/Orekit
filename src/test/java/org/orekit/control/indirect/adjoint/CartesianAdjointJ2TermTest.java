package org.orekit.control.indirect.adjoint;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

class CartesianAdjointJ2TermTest {

    @Test
    void testGetters() {
        // GIVEN
        final double expectedMu = 1.;
        final double expectedrEq = 2.;
        final double expectedJ2 = 3.;
        final Frame frame = Mockito.mock(Frame.class);
        final CartesianAdjointJ2Term cartesianAdjointJ2Term = new CartesianAdjointJ2Term(expectedMu, expectedrEq,
                expectedJ2, frame, frame);
        // WHEN
        final double actualMu = cartesianAdjointJ2Term.getMu();
        final double actualrEq = cartesianAdjointJ2Term.getrEq();
        final double actualJ2 = cartesianAdjointJ2Term.getJ2();
        // THEN
        Assertions.assertEquals(expectedJ2, actualJ2);
        Assertions.assertEquals(expectedMu, actualMu);
        Assertions.assertEquals(expectedrEq, actualrEq);
    }

    @Test
    void testGetVelocityAdjointContributionLinearity() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final CartesianAdjointJ2Term j2Term = new CartesianAdjointJ2Term(Constants.EGM96_EARTH_MU,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, frame, frame);
        final double[] adjoint = new double[] {1, 2, 3, 4, 5, 6};
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final double[] contribution = j2Term.getVelocityAdjointContribution(date, new double[6], adjoint);
        // THEN
        final double[] doubleAdjoint = new double[6];
        for (int i = 0; i < 6; i++) {
            doubleAdjoint[i] = adjoint[i] * 2;
        }
        final double[] contributionDouble = j2Term.getVelocityAdjointContribution(date, new double[6], doubleAdjoint);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(contribution[i] * 2, contributionDouble[i]);
        }
    }

    @Test
    void testGetVelocityAdjointContributionField() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final CartesianAdjointJ2Term j2Term = new CartesianAdjointJ2Term(Constants.EGM96_EARTH_MU,
                Constants.EGM96_EARTH_EQUATORIAL_RADIUS, -Constants.EGM96_EARTH_C20, frame, frame);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(-i+1);
            fieldAdjoint[i] = field.getZero().newInstance(i);
        }
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(field);
        // WHEN
        final Binary64[] fieldContribution = j2Term.getVelocityAdjointContribution(fieldDate, fieldState, fieldAdjoint);
        // THEN
        final double[] state = new double[fieldState.length];
        for (int i = 0; i < fieldState.length; i++) {
            state[i] = fieldState[i].getReal();
        }
        final double[] adjoint = new double[fieldAdjoint.length];
        for (int i = 0; i < fieldAdjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        final double[] contribution = j2Term.getVelocityAdjointContribution(fieldDate.toAbsoluteDate(), state, adjoint);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(fieldContribution[i].getReal(), contribution[i]);
        }
    }

}
