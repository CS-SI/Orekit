package org.orekit.propagation.numerical;


import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.forces.ForceModel;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.DataDictionary;
import org.orekit.utils.DerivativeStateUtils;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SwitchEventHandler implements EventHandler {

    private final EventHandler wrappedHandler;
    private final NumericalPropagationHarvester matricesHarvester;
    private final NumericalTimeDerivativesEquations timeDerivativesEquations;
    private final AttitudeProvider attitudeProvider;
    private boolean isForward;
    private FieldEventDetector<Gradient> switchFieldDetector;

    SwitchEventHandler(final EventHandler wrappedHandler, final NumericalPropagationHarvester matricesHarvester,
                       final NumericalTimeDerivativesEquations timeDerivativesEquations,
                       final AttitudeProvider attitudeProvider) {
        this.wrappedHandler = wrappedHandler;
        this.matricesHarvester = matricesHarvester;
        this.timeDerivativesEquations = timeDerivativesEquations;
        this.attitudeProvider = attitudeProvider;
    }

    @Override
    public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
        if (switchFieldDetector == null) {
            return wrappedHandler.resetState(detector, oldState);
        } else {
            final SpacecraftState newState = updateState(oldState);
            switchFieldDetector = null;
            return newState;
        }
    }

    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target, final EventDetector detector) {
        isForward = initialState.getDate().isBeforeOrEqualTo(target);
        switchFieldDetector = null;
        EventHandler.super.init(initialState, target, detector);
    }

    @Override
    public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
        final Action action = wrappedHandler.eventOccurred(s, detector, increasing);
        if (action == Action.RESET_DERIVATIVES) {
            switchFieldDetector = findSwitchDetector(matricesHarvester, detector, s);
            if (switchFieldDetector != null) {
                return Action.RESET_STATE;
            }
        }
        return action;
    }

    private FieldEventDetector<Gradient> findSwitchDetector(final AbstractMatricesHarvester harvester,
                                                            final EventDetector detector, final SpacecraftState state) {
        final int variablesNumber = harvester.getStateDimension() + 1;
        final GradientField field = GradientField.getField(variablesNumber);
        Stream<FieldEventDetector<Gradient>> fieldDetectorsStream = attitudeProvider.getFieldEventDetectors(field);
        for (final ForceModel forceModel : timeDerivativesEquations.getForceModels()) {
            fieldDetectorsStream = Stream.concat(fieldDetectorsStream, forceModel.getFieldEventDetectors(field));
        }
        final List<FieldEventDetector<Gradient>> fieldDetectors = fieldDetectorsStream
                .filter(fieldDetector -> !fieldDetector.dependsOnTimeOnly()).collect(Collectors.toList());
        final FieldSpacecraftState<Gradient> fieldState = new FieldSpacecraftState<>(field, state);
        fieldDetectors.forEach(fieldDetector -> fieldDetector.init(fieldState, fieldState.getDate()));
        final double g = detector.g(state);
        return findSwitchDetector(g, fieldState, fieldDetectors);
    }

    private FieldEventDetector<Gradient> findSwitchDetector(final double g, final FieldSpacecraftState<Gradient> fieldState,
                                                            final List<FieldEventDetector<Gradient>> fieldDetectors) {
        for (final FieldEventDetector<Gradient> fieldDetector : fieldDetectors) {
            final Gradient fieldG = fieldDetector.g(fieldState);
            if (FastMath.abs(fieldG.getValue() - g) < 1e-15) {
                return fieldDetector;
            }
        }
        return null;
    }

    private SpacecraftState updateState(final SpacecraftState stateAtSwitch) {
        final double[] deltaDerivatives = computeDeltaDerivatives(stateAtSwitch);
        double norm = 0.;
        for (final double deltaDerivative : deltaDerivatives) {
            norm += FastMath.abs(deltaDerivative);
        }
        if (norm == 0.) {
            return stateAtSwitch;
        } else {
            return updateAdditionalVariables(stateAtSwitch, deltaDerivatives);
        }
    }

    private double[] computeDeltaDerivatives(final SpacecraftState stateAtSwitch) {
        final double[] derivatives = timeDerivativesEquations.computeTimeDerivatives(stateAtSwitch);
        final TimeStampedPVCoordinates pvCoordinates = stateAtSwitch.getPVCoordinates();
        final double twoThreshold = 2. * switchFieldDetector.getThreshold().getValue();
        final double dt = isForward ? -twoThreshold : twoThreshold;
        final TimeStampedPVCoordinates pvCoordinatesBefore = pvCoordinates.shiftedBy(dt);
        final SpacecraftState stateBeforeSwitch = buildState(stateAtSwitch, pvCoordinatesBefore);
        final double[] derivativesBefore = timeDerivativesEquations.computeTimeDerivatives(stateBeforeSwitch);
        final double[] deltaDerivatives = new double[4];
        for (int i = 0; i < deltaDerivatives.length; i++) {
            deltaDerivatives[i] = derivativesBefore[i + 3] - derivatives[i + 3];
        }
        return deltaDerivatives;
    }

    private SpacecraftState buildState(final SpacecraftState templateState,
                                       final TimeStampedPVCoordinates pvCoordinates) {
        final SpacecraftState state;
        if (templateState.isOrbitDefined()) {
            final Orbit templateOrbit = templateState.getOrbit();
            final CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, templateState.getFrame(),
                    templateOrbit.getMu());
            final Attitude attitude = attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
            state = new SpacecraftState(orbit, attitude);
        } else {
            final AbsolutePVCoordinates absolutePVCoordinates = new AbsolutePVCoordinates(templateState.getFrame(),
                    pvCoordinates);
            final Attitude attitude = attitudeProvider.getAttitude(absolutePVCoordinates,
                    absolutePVCoordinates.getDate(), absolutePVCoordinates.getFrame());
            state = new SpacecraftState(absolutePVCoordinates, attitude);
        }
        return state.withMass(templateState.getMass())
                .withAdditionalData(templateState.getAdditionalDataValues())
                .withAdditionalStatesDerivatives(templateState.getAdditionalStatesDerivatives());
    }


    private FieldSpacecraftState<Gradient> buildFieldState(final FieldSpacecraftState<Gradient> templateState,
                                       final TimeStampedFieldPVCoordinates<Gradient> pvCoordinates) {
        final FieldSpacecraftState<Gradient> state;
        if (templateState.isOrbitDefined()) {
            final FieldOrbit<Gradient> templateOrbit = templateState.getOrbit();
            final FieldCartesianOrbit<Gradient> orbit = new FieldCartesianOrbit<>(pvCoordinates, templateState.getFrame(),
                    templateOrbit.getMu());
            final FieldAttitude<Gradient> attitude = attitudeProvider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
            state = new FieldSpacecraftState<>(orbit, attitude);
        } else {
            final FieldAbsolutePVCoordinates<Gradient> absolutePVCoordinates = new FieldAbsolutePVCoordinates<>(templateState.getFrame(),
                    pvCoordinates);
            final FieldAttitude<Gradient> attitude = attitudeProvider.getAttitude(absolutePVCoordinates,
                    absolutePVCoordinates.getDate(), absolutePVCoordinates.getFrame());
            state = new FieldSpacecraftState<>(absolutePVCoordinates, attitude);
        }
        return state.withMass(templateState.getMass());
    }

    private SpacecraftState updateAdditionalVariables(final SpacecraftState stateAtSwitch,
                                                      final double[] deltaDerivatives) {
        final RealMatrix product = computeUpdateMatrix(stateAtSwitch, deltaDerivatives);
        final String stmName = matricesHarvester.getStmName();
        final double[] flatStm = stateAtSwitch.getAdditionalState(stmName);
        final RealMatrix oldStm = MatrixUtils.createRealMatrix(matricesHarvester.toSquareMatrix(flatStm).getData());
        final RealMatrix stm = product.multiply(oldStm);
        final DataDictionary additionalData = new DataDictionary(stateAtSwitch.getAdditionalDataValues());
        additionalData.remove(stmName);
        additionalData.put(stmName, matricesHarvester.toArray(stm.getData()));
        for (final String parameterName: matricesHarvester.getJacobiansColumnsNames()) {
            final double[] oldParameterDerivatives = stateAtSwitch.getAdditionalState(parameterName);
            additionalData.remove(parameterName);
            final double[] parameterDerivatives = product.operate(oldParameterDerivatives);
            additionalData.put(parameterName, parameterDerivatives);
        }
        return stateAtSwitch.withAdditionalData(additionalData);
    }

    private RealMatrix computeUpdateMatrix(final SpacecraftState stateAtSwitch,
                                           final double[] deltaDerivatives) {
        final double[][] lhsMatrix = new double[7][8];
        for (int i = 0; i < lhsMatrix.length; i++) {
            lhsMatrix[i][i] = 1;
        }
        lhsMatrix[3][7] = deltaDerivatives[0];
        lhsMatrix[4][7] = deltaDerivatives[1];
        lhsMatrix[5][7] = deltaDerivatives[2];
        lhsMatrix[6][7] = deltaDerivatives[3];
        final GradientField field = switchFieldDetector.getThreshold().getField();
        final int freeParameters = field.getOne().getFreeParameters();
        final Gradient dt = Gradient.variable(freeParameters, freeParameters - 1, 0.);
        final FieldSpacecraftState<Gradient> fieldTemplateState = DerivativeStateUtils.buildSpacecraftStateGradient(field,
                stateAtSwitch, attitudeProvider);
        final FieldSpacecraftState<Gradient> fieldState = buildFieldState(fieldTemplateState,
                fieldTemplateState.getPVCoordinates().shiftedBy(dt));
        final Gradient g = switchFieldDetector.g(fieldState);
        final RealMatrix rhsMatrix = MatrixUtils.createRealIdentityMatrix(8);
        rhsMatrix.setRow(rhsMatrix.getRowDimension() - 1, g.getGradient());
        final DecompositionSolver solver = new QRDecomposition(rhsMatrix, Precision.SAFE_MIN).getSolver();
        final RealMatrix inverted = solver.getInverse();
        return MatrixUtils.createRealMatrix(lhsMatrix).multiply(inverted.getSubMatrix(0, 7, 0, 6));
    }
}

