package org.orekit.control.indirect.adjoint;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AbstractCartesianAdjointNewtonianTermTest {

    @Test
    void testGetVelocityAdjointContributionField() {
        // GIVEN
        final AbstractCartesianAdjointNewtonianTerm adjointNewtonianTerm = Mockito.mock(AbstractCartesianAdjointNewtonianTerm.class);
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Binary64[] fieldState = MathArrays.buildArray(field, 6);
        for (int i = 0; i < fieldAdjoint.length; i++) {
            fieldState[i] = field.getZero().newInstance(-i+1);
            fieldAdjoint[i] = field.getZero().newInstance(i);
        }
        final double mu = 2.;
        Mockito.when(adjointNewtonianTerm.getFieldNewtonianVelocityAdjointContribution(mu, fieldState, fieldAdjoint)).thenCallRealMethod();
        // WHEN
        final Binary64[] fieldContribution = adjointNewtonianTerm.getFieldNewtonianVelocityAdjointContribution(mu, fieldState, fieldAdjoint);
        // THEN
        final double[] state = new double[fieldState.length];
        for (int i = 0; i < fieldState.length; i++) {
            state[i] = fieldState[i].getReal();
        }
        final double[] adjoint = new double[fieldAdjoint.length];
        for (int i = 0; i < fieldAdjoint.length; i++) {
            adjoint[i] = fieldAdjoint[i].getReal();
        }
        Mockito.when(adjointNewtonianTerm.getNewtonianVelocityAdjointContribution(mu, state, adjoint)).thenCallRealMethod();
        final double[] contribution = adjointNewtonianTerm.getNewtonianVelocityAdjointContribution(mu, state, adjoint);
        for (int i = 0; i < contribution.length; i++) {
            Assertions.assertEquals(fieldContribution[i].getReal(), contribution[i]);
        }
    }
}
