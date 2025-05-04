package org.orekit.propagation.numerical;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.utils.DerivativeStateUtils;

interface SwitchManager {

    double[] computeCartesianDerivativesAfter(final SpacecraftState state);

    double[] computeCartesianDerivativesBefore(final SpacecraftState state);

    default FieldSpacecraftState<Gradient> updateState(final SpacecraftState stateBefore,
                                                       final FieldEventDetector<Gradient> detector,
                                                       final AttitudeProvider attitudeProvider,
                                                       final boolean isForward) {
        final Gradient threshold = detector.getThreshold();
        final GradientField field = threshold.getField();
        final int variableNumber = threshold.getFreeParameters();
        final Gradient dt = Gradient.variable(variableNumber, variableNumber - 1, 1.);
        final FieldSpacecraftState<Gradient> fieldState = DerivativeStateUtils.buildSpacecraftStateGradient(field,
                stateBefore, attitudeProvider);
        return new FieldSpacecraftState<>(dt.getField(), stateBefore);
    }
}
