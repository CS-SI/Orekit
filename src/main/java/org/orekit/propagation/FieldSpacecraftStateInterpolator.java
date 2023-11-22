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
package org.orekit.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Pair;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FieldAttitudeInterpolator;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.FieldOrbitHermiteInterpolator;
import org.orekit.time.AbstractFieldTimeInterpolator;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeInterpolator;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeStampedField;
import org.orekit.time.TimeStampedFieldHermiteInterpolator;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinatesHermiteInterpolator;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldAngularCoordinatesHermiteInterpolator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generic class for spacecraft state interpolator.
 * <p>
 * The user can specify what interpolator to use for each attribute of the spacecraft state.  However, at least one
 * interpolator for either orbit or absolute position-velocity-acceleration is needed. All the other interpolators can be
 * left to null if the user do not want to interpolate these values.
 *
 * @param <KK> type of the field element
 *
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @see SpacecraftState
 */
public class FieldSpacecraftStateInterpolator<KK extends CalculusFieldElement<KK>>
        extends AbstractFieldTimeInterpolator<FieldSpacecraftState<KK>, KK> {

    /**
     * Output frame.
     * <p><b>Must be inertial</b> if interpolating spacecraft states defined by orbit</p>
     */
    private final Frame outputFrame;

    /** Orbit interpolator. */
    private final FieldTimeInterpolator<FieldOrbit<KK>, KK> orbitInterpolator;

    /** Absolute position-velocity-acceleration interpolator. */
    private final FieldTimeInterpolator<FieldAbsolutePVCoordinates<KK>, KK> absPVAInterpolator;

    /** Mass interpolator. */
    private final FieldTimeInterpolator<TimeStampedField<KK>, KK> massInterpolator;

    /** Attitude interpolator. */
    private final FieldTimeInterpolator<FieldAttitude<KK>, KK> attitudeInterpolator;

    /** Additional state interpolator. */
    private final FieldTimeInterpolator<TimeStampedField<KK>, KK> additionalStateInterpolator;

    /**
     * Simplest constructor to create a default Hermite interpolator for every spacecraft state field.
     * <p>
     * The interpolators will have the following configuration :
     * <ul>
     *     <li>Same frame for coordinates and attitude </li>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold of {@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s</li>
     *     <li>Use of position and two time derivatives for absolute position-velocity-acceleration coordinates interpolation</li>
     *     <li>Use of angular and first time derivative for attitude interpolation</li>
     * </ul>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> output frame <b>must be inertial</b> if this instance is going to interpolate between
     * tabulated spacecraft states defined by orbit, will throw an error otherwise.
     *
     * @param outputFrame output frame
     */
    public FieldSpacecraftStateInterpolator(final Frame outputFrame) {
        this(DEFAULT_INTERPOLATION_POINTS, outputFrame);
    }

    /**
     * Constructor to create a customizable Hermite interpolator for every spacecraft state field.
     * <p>
     * The interpolators will have the following configuration :
     * <ul>
     *     <li>Same frame for coordinates and attitude </li>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold of {@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s</li>
     *     <li>Use of position and two time derivatives for absolute position-velocity-acceleration coordinates interpolation</li>
     *     <li>Use of angular and first time derivative for attitude interpolation</li>
     * </ul>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> output frame <b>must be inertial</b> if this instance is going to interpolate between
     * tabulated spacecraft states defined by orbit, will throw an error otherwise.
     *
     * @param interpolationPoints number of interpolation points
     * @param outputFrame output frame
     */
    public FieldSpacecraftStateInterpolator(final int interpolationPoints, final Frame outputFrame) {
        this(interpolationPoints, outputFrame, outputFrame);
    }

    /**
     * Constructor to create a customizable Hermite interpolator for every spacecraft state field.
     * <p>
     * The interpolators will have the following configuration :
     * <ul>
     *     <li>Default extrapolation threshold of {@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s</li>
     *     <li>Use of position and two time derivatives for absolute position-velocity-acceleration coordinates interpolation</li>
     *     <li>Use of angular and first time derivative for attitude interpolation</li>
     * </ul>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> output frame <b>must be inertial</b> if this instance is going to interpolate between
     * tabulated spacecraft states defined by orbit, will throw an error otherwise.
     *
     * @param interpolationPoints number of interpolation points
     * @param outputFrame output frame
     * @param attitudeReferenceFrame reference frame from which attitude is defined
     */
    public FieldSpacecraftStateInterpolator(final int interpolationPoints, final Frame outputFrame,
                                            final Frame attitudeReferenceFrame) {
        this(interpolationPoints, AbstractTimeInterpolator.DEFAULT_EXTRAPOLATION_THRESHOLD_SEC,
             outputFrame, attitudeReferenceFrame);
    }

    /**
     * Constructor to create a customizable Hermite interpolator for every spacecraft state field.
     * <p>
     * The interpolators will have the following configuration :
     * <ul>
     *     <li>Use of position and two time derivatives for absolute position-velocity-acceleration coordinates interpolation</li>
     *     <li>Use of angular and first time derivative for attitude interpolation</li>
     * </ul>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> output frame <b>must be inertial</b> if this instance is going to interpolate between
     * tabulated spacecraft states defined by orbit, will throw an error otherwise.
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param outputFrame output frame
     * @param attitudeReferenceFrame reference frame from which attitude is defined
     */
    public FieldSpacecraftStateInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                            final Frame outputFrame, final Frame attitudeReferenceFrame) {
        this(interpolationPoints, extrapolationThreshold, outputFrame, attitudeReferenceFrame,
             CartesianDerivativesFilter.USE_PVA, AngularDerivativesFilter.USE_RR);
    }

    /**
     * Constructor.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     * <p>
     * <b>BEWARE:</b> output frame <b>must be inertial</b> if this instance is going to interpolate between
     * tabulated spacecraft states defined by orbit, will throw an error otherwise.
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param outputFrame output frame
     * @param pvaFilter filter for derivatives from the sample to use in position-velocity-acceleration interpolation
     * @param attitudeReferenceFrame reference frame from which attitude is defined
     * @param angularFilter filter for derivatives from the sample to use in attitude interpolation
     */
    public FieldSpacecraftStateInterpolator(final int interpolationPoints,
                                            final double extrapolationThreshold,
                                            final Frame outputFrame,
                                            final Frame attitudeReferenceFrame,
                                            final CartesianDerivativesFilter pvaFilter,
                                            final AngularDerivativesFilter angularFilter) {

        this(outputFrame,
             new FieldOrbitHermiteInterpolator<>(interpolationPoints, extrapolationThreshold, outputFrame, pvaFilter),
             new FieldAbsolutePVCoordinatesHermiteInterpolator<>(interpolationPoints, extrapolationThreshold, outputFrame,
                                                                 pvaFilter),
             new TimeStampedFieldHermiteInterpolator<>(interpolationPoints, extrapolationThreshold),
             new FieldAttitudeInterpolator<>(attitudeReferenceFrame,
                                             new TimeStampedFieldAngularCoordinatesHermiteInterpolator<KK>(
                                                     interpolationPoints, extrapolationThreshold, angularFilter)),
             new TimeStampedFieldHermiteInterpolator<>(interpolationPoints, extrapolationThreshold));
    }

    /**
     * Constructor.
     * <p>
     * <b>BEWARE:</b> output frame <b>must be inertial</b> if interpolated spacecraft states are defined by orbit. Throws an
     * error otherwise.
     *
     * @param outputFrame output frame
     * @param orbitInterpolator orbit interpolator
     * @param absPVAInterpolator absolute position-velocity-acceleration
     * @param massInterpolator mass interpolator
     * @param attitudeInterpolator attitude interpolator
     * @param additionalStateInterpolator additional state interpolator
     */
    public FieldSpacecraftStateInterpolator(final Frame outputFrame,
                                            final FieldTimeInterpolator<FieldOrbit<KK>, KK> orbitInterpolator,
                                            final FieldTimeInterpolator<FieldAbsolutePVCoordinates<KK>, KK> absPVAInterpolator,
                                            final FieldTimeInterpolator<TimeStampedField<KK>, KK> massInterpolator,
                                            final FieldTimeInterpolator<FieldAttitude<KK>, KK> attitudeInterpolator,
                                            final FieldTimeInterpolator<TimeStampedField<KK>, KK> additionalStateInterpolator) {
        super(DEFAULT_INTERPOLATION_POINTS, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
        checkAtLeastOneInterpolator(orbitInterpolator, absPVAInterpolator);
        this.outputFrame                 = outputFrame;
        this.orbitInterpolator           = orbitInterpolator;
        this.absPVAInterpolator          = absPVAInterpolator;
        this.massInterpolator            = massInterpolator;
        this.attitudeInterpolator        = attitudeInterpolator;
        this.additionalStateInterpolator = additionalStateInterpolator;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The additional states that are interpolated are the ones already present in the first neighbor instance. The sample
     * instances must therefore have at least the same additional states as this neighbor instance. They may have more
     * additional states, but the extra ones will be ignored.
     * <p>
     * All the sample instances <em>must</em> be based on similar trajectory data, i.e. they must either all be based on
     * orbits or all be based on absolute position-velocity-acceleration. Any inconsistency will trigger an
     * {@link OrekitIllegalArgumentException}.
     *
     * @throws OrekitIllegalArgumentException if there are states defined by orbits and absolute
     * position-velocity-acceleration coordinates
     * @throws OrekitIllegalArgumentException if there is no defined interpolator for given sample spacecraft state
     * definition type
     */
    @Override
    public FieldSpacecraftState<KK> interpolate(final FieldAbsoluteDate<KK> interpolationDate,
                                                final Collection<FieldSpacecraftState<KK>> sample) {

        final List<FieldSpacecraftState<KK>> sampleList = new ArrayList<>(sample);

        // Convert to spacecraft state for consistency check
        final List<SpacecraftState> nonFieldSampleList =
                sampleList.stream().map(FieldSpacecraftState::toSpacecraftState).collect(Collectors.toList());

        // If sample is empty, an error will be thrown in super method
        if (!sampleList.isEmpty()) {

            // Check given that given states definition are consistent
            // (all defined by either orbits or absolute position-velocity-acceleration coordinates)
            SpacecraftStateInterpolator.checkStatesDefinitionsConsistency(nonFieldSampleList);

            // Check interpolator and sample consistency
            SpacecraftStateInterpolator.checkSampleAndInterpolatorConsistency(nonFieldSampleList,
                                                                              orbitInterpolator != null,
                                                                              absPVAInterpolator != null);
        }

        return super.interpolate(interpolationDate, sample);
    }

    /** {@inheritDoc} */
    @Override
    public List<FieldTimeInterpolator<? extends FieldTimeStamped<KK>, KK>> getSubInterpolators() {

        // Add all sub interpolators that are defined
        final List<FieldTimeInterpolator<? extends FieldTimeStamped<KK>, KK>> subInterpolators = new ArrayList<>();

        addOptionalSubInterpolatorIfDefined(orbitInterpolator, subInterpolators);
        addOptionalSubInterpolatorIfDefined(absPVAInterpolator, subInterpolators);
        addOptionalSubInterpolatorIfDefined(massInterpolator, subInterpolators);
        addOptionalSubInterpolatorIfDefined(attitudeInterpolator, subInterpolators);
        addOptionalSubInterpolatorIfDefined(additionalStateInterpolator, subInterpolators);

        return subInterpolators;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FieldSpacecraftState<KK> interpolate(final InterpolationData interpolationData) {

        // Get interpolation date
        final FieldAbsoluteDate<KK> interpolationDate = interpolationData.getInterpolationDate();

        // Get first state definition
        final FieldSpacecraftState<KK> earliestState   = interpolationData.getNeighborList().get(0);
        final boolean                  areOrbitDefined = earliestState.isOrbitDefined();

        // Prepare samples
        final List<FieldAttitude<KK>> attitudes = new ArrayList<>();

        final List<TimeStampedField<KK>> masses = new ArrayList<>();

        final List<FieldArrayDictionary<KK>.Entry> additionalEntries =
                earliestState.getAdditionalStatesValues().getData();
        final Map<String, List<Pair<FieldAbsoluteDate<KK>, KK[]>>> additionalSample =
                createAdditionalStateSample(additionalEntries);

        final List<FieldArrayDictionary<KK>.Entry> additionalDotEntries =
                earliestState.getAdditionalStatesDerivatives().getData();
        final Map<String, List<Pair<FieldAbsoluteDate<KK>, KK[]>>> additionalDotSample =
                createAdditionalStateSample(additionalDotEntries);

        // Fill interpolators with samples
        final List<FieldSpacecraftState<KK>>       samples      = interpolationData.getCachedSamples().getAll();
        final List<FieldOrbit<KK>>                 orbitSample  = new ArrayList<>();
        final List<FieldAbsolutePVCoordinates<KK>> absPVASample = new ArrayList<>();
        for (FieldSpacecraftState<KK> state : samples) {
            final FieldAbsoluteDate<KK> currentDate = state.getDate();

            // Add orbit sample if state is defined with an orbit
            if (state.isOrbitDefined()) {
                orbitSample.add(state.getOrbit());
            }
            // Add absolute position-velocity-acceleration sample if state is defined with an absolute position-velocity-acceleration
            else {
                absPVASample.add(state.getAbsPVA());
            }

            // Add mass sample
            if (massInterpolator != null) {
                masses.add(new TimeStampedField<>(state.getMass(), state.getDate()));
            }

            // Add attitude sample if it is interpolated
            if (attitudeInterpolator != null) {
                attitudes.add(state.getAttitude());
            }

            if (additionalStateInterpolator != null) {

                // Add all additional state values if they are interpolated
                for (final Map.Entry<String, List<Pair<FieldAbsoluteDate<KK>, KK[]>>> entry : additionalSample.entrySet()) {
                    entry.getValue().add(new Pair<>(currentDate, state.getAdditionalState(entry.getKey())));
                }

                // Add all additional state derivative values if they are interpolated
                for (final Map.Entry<String, List<Pair<FieldAbsoluteDate<KK>, KK[]>>> entry : additionalDotSample.entrySet()) {
                    entry.getValue().add(new Pair<>(currentDate, state.getAdditionalStateDerivative(entry.getKey())));
                }
            }

        }

        // Interpolate mass
        final KK one = interpolationData.getOne();
        final KK interpolatedMass;
        if (massInterpolator != null) {
            interpolatedMass = massInterpolator.interpolate(interpolationDate, masses).getValue();
        }
        else {
            interpolatedMass = one.multiply(SpacecraftState.DEFAULT_MASS);
        }

        // Interpolate additional states and derivatives
        final FieldArrayDictionary<KK> interpolatedAdditional;
        final FieldArrayDictionary<KK> interpolatedAdditionalDot;
        if (additionalStateInterpolator != null) {
            interpolatedAdditional    = interpolateAdditionalState(interpolationDate, additionalSample);
            interpolatedAdditionalDot = interpolateAdditionalState(interpolationDate, additionalDotSample);
        }
        else {
            interpolatedAdditional    = null;
            interpolatedAdditionalDot = null;
        }

        // Interpolate orbit
        if (areOrbitDefined && orbitInterpolator != null) {
            final FieldOrbit<KK> interpolatedOrbit =
                    orbitInterpolator.interpolate(interpolationDate, orbitSample);

            final FieldAttitude<KK> interpolatedAttitude =
                    interpolateAttitude(interpolationDate, attitudes, interpolatedOrbit);

            return new FieldSpacecraftState<>(interpolatedOrbit, interpolatedAttitude, interpolatedMass,
                                              interpolatedAdditional, interpolatedAdditionalDot);
        }
        // Interpolate absolute position-velocity-acceleration
        else if (!areOrbitDefined && absPVAInterpolator != null) {

            final FieldAbsolutePVCoordinates<KK> interpolatedAbsPva =
                    absPVAInterpolator.interpolate(interpolationDate, absPVASample);

            final FieldAttitude<KK> interpolatedAttitude =
                    interpolateAttitude(interpolationDate, attitudes, interpolatedAbsPva);

            return new FieldSpacecraftState<>(interpolatedAbsPva, interpolatedAttitude, interpolatedMass,
                                              interpolatedAdditional, interpolatedAdditionalDot);
        }
        // Should never happen
        else {
            throw new OrekitInternalError(null);
        }

    }

    /** Get output frame.
     * @return output frame
     */
    public Frame getOutputFrame() {
        return outputFrame;
    }

    /** Get orbit interpolator.
     * @return optional orbit interpolator
     *
     * @see Optional
     */
    public Optional<FieldTimeInterpolator<FieldOrbit<KK>, KK>> getOrbitInterpolator() {
        return Optional.ofNullable(orbitInterpolator);
    }

    /** Get absolute position-velocity-acceleration interpolator.
     * @return optional absolute position-velocity-acceleration interpolator
     *
     * @see Optional
     */
    public Optional<FieldTimeInterpolator<FieldAbsolutePVCoordinates<KK>, KK>> getAbsPVAInterpolator() {
        return Optional.ofNullable(absPVAInterpolator);
    }

    /** Get mass interpolator.
     * @return optional mass interpolator
     *
     * @see Optional
     */
    public Optional<FieldTimeInterpolator<TimeStampedField<KK>, KK>> getMassInterpolator() {
        return Optional.ofNullable(massInterpolator);
    }

    /** Get attitude interpolator.
     * @return optional attitude interpolator
     *
     * @see Optional
     */
    public Optional<FieldTimeInterpolator<FieldAttitude<KK>, KK>> getAttitudeInterpolator() {
        return Optional.ofNullable(attitudeInterpolator);
    }

    /** Get additional state interpolator.
     * @return optional additional state interpolator
     *
     * @see Optional
     */
    public Optional<FieldTimeInterpolator<TimeStampedField<KK>, KK>> getAdditionalStateInterpolator() {
        return Optional.ofNullable(additionalStateInterpolator);
    }

    /**
     * Check that at least one interpolator is defined.
     *
     * @param orbitInterpolatorToCheck orbit interpolator
     * @param absPVAInterpolatorToCheck absolute position-velocity-acceleration interpolator
     */
    private void checkAtLeastOneInterpolator(final FieldTimeInterpolator<FieldOrbit<KK>, KK> orbitInterpolatorToCheck,
                                             final FieldTimeInterpolator<FieldAbsolutePVCoordinates<KK>, KK> absPVAInterpolatorToCheck) {
        if (orbitInterpolatorToCheck == null && absPVAInterpolatorToCheck == null) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NO_INTERPOLATOR_FOR_STATE_DEFINITION);
        }
    }

    /**
     * Create empty samples for given additional entries.
     *
     * @param additionalEntries tabulated additional entries
     *
     * @return empty samples for given additional entries
     */
    private Map<String, List<Pair<FieldAbsoluteDate<KK>, KK[]>>> createAdditionalStateSample(
            final List<FieldArrayDictionary<KK>.Entry> additionalEntries) {
        final Map<String, List<Pair<FieldAbsoluteDate<KK>, KK[]>>> additionalSamples =
                new HashMap<>(additionalEntries.size());

        for (final FieldArrayDictionary<KK>.Entry entry : additionalEntries) {
            additionalSamples.put(entry.getKey(), new ArrayList<>());
        }

        return additionalSamples;
    }

    /**
     * Interpolate additional state values.
     *
     * @param interpolationDate interpolation date
     * @param additionalSamples additional state samples
     *
     * @return interpolated additional state values
     */
    private FieldArrayDictionary<KK> interpolateAdditionalState(final FieldAbsoluteDate<KK> interpolationDate,
                                                                final Map<String, List<Pair<FieldAbsoluteDate<KK>, KK[]>>> additionalSamples) {
        final Field<KK> field = interpolationDate.getField();
        final FieldArrayDictionary<KK> interpolatedAdditional;

        if (additionalSamples.isEmpty()) {
            interpolatedAdditional = null;
        }
        else {
            interpolatedAdditional = new FieldArrayDictionary<>(field, additionalSamples.size());
            for (final Map.Entry<String, List<Pair<FieldAbsoluteDate<KK>, KK[]>>> entry : additionalSamples.entrySet()) {

                // Get current entry
                final List<Pair<FieldAbsoluteDate<KK>, KK[]>> currentAdditionalSamples = entry.getValue();

                // Extract number of values for this specific entry
                final int nbOfValues = currentAdditionalSamples.get(0).getValue().length;

                // For each value of current additional state entry
                final KK[] currentInterpolatedAdditional = MathArrays.buildArray(field, nbOfValues);
                for (int i = 0; i < nbOfValues; i++) {

                    // Create final index for lambda expression use
                    final int currentIndex = i;

                    // Create sample for specific value of current additional state values
                    final List<TimeStampedField<KK>> currentValueSample = new ArrayList<>();

                    currentAdditionalSamples.forEach(currentSamples -> currentValueSample.add(
                            new TimeStampedField<>(currentSamples.getValue()[currentIndex], currentSamples.getFirst())));

                    // Interpolate
                    currentInterpolatedAdditional[i] =
                            additionalStateInterpolator.interpolate(interpolationDate, currentValueSample).getValue();
                }

                interpolatedAdditional.put(entry.getKey(), currentInterpolatedAdditional);
            }
        }
        return interpolatedAdditional;
    }

    /**
     * Interpolate attitude.
     * <p>
     * If no attitude interpolator were defined, create a default inertial provider with respect to the output frame.
     *
     * @param interpolationDate interpolation date
     * @param attitudes attitudes sample
     * @param pvProvider position-velocity-acceleration coordinates provider
     *
     * @return interpolated attitude if attitude interpolator is present, default attitude otherwise
     */
    private FieldAttitude<KK> interpolateAttitude(final FieldAbsoluteDate<KK> interpolationDate,
                                                  final List<FieldAttitude<KK>> attitudes,
                                                  final FieldPVCoordinatesProvider<KK> pvProvider) {
        if (attitudes.isEmpty()) {
            final AttitudeProvider attitudeProvider = new FrameAlignedProvider(outputFrame);
            return attitudeProvider.getAttitude(pvProvider, interpolationDate, outputFrame);
        }
        else {
            return attitudeInterpolator.interpolate(interpolationDate, attitudes);
        }
    }
}
