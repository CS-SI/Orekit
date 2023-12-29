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

import org.hipparchus.util.Pair;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeInterpolator;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitHermiteInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AbstractTimeInterpolator;
import org.orekit.time.TimeInterpolator;
import org.orekit.time.TimeStamped;
import org.orekit.time.TimeStampedDouble;
import org.orekit.time.TimeStampedDoubleHermiteInterpolator;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.AbsolutePVCoordinatesHermiteInterpolator;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinatesHermiteInterpolator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic class for spacecraft state interpolator.
 * <p>
 * The user can specify what interpolator to use for each attribute of the spacecraft state. However, at least one
 * interpolator for either orbit or absolute position-velocity-acceleration is needed. All the other interpolators can be
 * left to null if the user do not want to interpolate these values.
 *
 * @author Luc Maisonobe
 * @author Vincent Cucchietti
 * @see SpacecraftState
 */
public class SpacecraftStateInterpolator extends AbstractTimeInterpolator<SpacecraftState> {

    /**
     * Output frame.
     * <p><b>Must be inertial</b> if interpolating spacecraft states defined by orbit</p>
     */
    private final Frame outputFrame;

    /** Orbit interpolator. */
    private final TimeInterpolator<Orbit> orbitInterpolator;

    /** Absolute position-velocity-acceleration interpolator. */
    private final TimeInterpolator<AbsolutePVCoordinates> absPVAInterpolator;

    /** Mass interpolator. */
    private final TimeInterpolator<TimeStampedDouble> massInterpolator;

    /** Attitude interpolator. */
    private final TimeInterpolator<Attitude> attitudeInterpolator;

    /** Additional state interpolator. */
    private final TimeInterpolator<TimeStampedDouble> additionalStateInterpolator;

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
     *
     * @see AbstractTimeInterpolator
     */
    public SpacecraftStateInterpolator(final Frame outputFrame) {
        this(DEFAULT_INTERPOLATION_POINTS, outputFrame);
    }

    /**
     * Constructor to create a customizable Hermite interpolator for every spacecraft state field.
     * <p>
     * The interpolators will have the following configuration :
     * <ul>
     *     <li>Same frame for coordinates and attitude </li>
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
     *
     * @see AbstractTimeInterpolator
     */
    public SpacecraftStateInterpolator(final int interpolationPoints, final Frame outputFrame) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, outputFrame, outputFrame);
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
     *
     * @see AbstractTimeInterpolator
     */
    public SpacecraftStateInterpolator(final int interpolationPoints, final Frame outputFrame,
                                       final Frame attitudeReferenceFrame) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC, outputFrame, attitudeReferenceFrame,
             CartesianDerivativesFilter.USE_PVA, AngularDerivativesFilter.USE_RR);
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
    public SpacecraftStateInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                       final Frame outputFrame, final Frame attitudeReferenceFrame) {
        this(interpolationPoints, extrapolationThreshold, outputFrame, attitudeReferenceFrame,
             CartesianDerivativesFilter.USE_PVA, AngularDerivativesFilter.USE_RR);
    }

    /**
     * Constructor to create a customizable Hermite interpolator for every spacecraft state field.
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
     * @param pvaFilter filter for derivatives from the sample to use in position-velocity-acceleration interpolation
     * @param angularFilter filter for derivatives from the sample to use in attitude interpolation
     */
    public SpacecraftStateInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                       final Frame outputFrame, final Frame attitudeReferenceFrame,
                                       final CartesianDerivativesFilter pvaFilter,
                                       final AngularDerivativesFilter angularFilter) {
        this(interpolationPoints, extrapolationThreshold, outputFrame,
             new OrbitHermiteInterpolator(interpolationPoints, extrapolationThreshold, outputFrame, pvaFilter),
             new AbsolutePVCoordinatesHermiteInterpolator(interpolationPoints, extrapolationThreshold, outputFrame,
                                                          pvaFilter),
             new TimeStampedDoubleHermiteInterpolator(interpolationPoints, extrapolationThreshold),
             new AttitudeInterpolator(attitudeReferenceFrame,
                                      new TimeStampedAngularCoordinatesHermiteInterpolator(interpolationPoints,
                                                                                           extrapolationThreshold,
                                                                                           angularFilter)),
             new TimeStampedDoubleHermiteInterpolator(interpolationPoints, extrapolationThreshold));
    }

    /**
     * Constructor with:
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold of {@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s</li>
     * </ul>
     * <p>
     * At least one interpolator for either orbit or absolute position-velocity-acceleration is needed. All the other
     * interpolators can be left to null if the user do not want to interpolate these values.
     * <p>
     * <b>BEWARE:</b> output frame <b>must be inertial</b> if interpolated spacecraft states are defined by orbit. Throws an
     * error otherwise.
     * <p>
     * <b>BEWARE:</b> it is up to the user to check the consistency of input interpolators.
     *
     * @param outputFrame output frame (inertial if the user is planning to use the orbit interpolator)
     * @param orbitInterpolator orbit interpolator (can be null if absPVAInterpolator is defined)
     * @param absPVAInterpolator absolute position-velocity-acceleration (can be null if orbitInterpolator is defined)
     * @param massInterpolator mass interpolator (can be null)
     * @param attitudeInterpolator attitude interpolator (can be null)
     * @param additionalStateInterpolator additional state interpolator (can be null)
     *
     * @see AbstractTimeInterpolator
     */
    public SpacecraftStateInterpolator(final Frame outputFrame, final TimeInterpolator<Orbit> orbitInterpolator,
                                       final TimeInterpolator<AbsolutePVCoordinates> absPVAInterpolator,
                                       final TimeInterpolator<TimeStampedDouble> massInterpolator,
                                       final TimeInterpolator<Attitude> attitudeInterpolator,
                                       final TimeInterpolator<TimeStampedDouble> additionalStateInterpolator) {
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
     * Constructor.
     * <p>
     * At least one interpolator for either orbit or absolute position-velocity-acceleration is needed. All the other
     * interpolators can be left to null if the user do not want to interpolate these values.
     * <p>
     * <b>BEWARE:</b> output frame <b>must be inertial</b> if interpolated spacecraft states are defined by orbit. Throws an
     * error otherwise.
     * <p>
     * <b>BEWARE:</b> it is up to the user to check the consistency of input interpolators.
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     * @param outputFrame output frame (inertial if the user is planning to use the orbit interpolator)
     * @param orbitInterpolator orbit interpolator (can be null if absPVAInterpolator is defined)
     * @param absPVAInterpolator absolute position-velocity-acceleration (can be null if orbitInterpolator is defined)
     * @param massInterpolator mass interpolator (can be null)
     * @param attitudeInterpolator attitude interpolator (can be null)
     * @param additionalStateInterpolator additional state interpolator (can be null)
     *
     * @since 12.0.1
     */
    public SpacecraftStateInterpolator(final int interpolationPoints, final double extrapolationThreshold,
                                       final Frame outputFrame, final TimeInterpolator<Orbit> orbitInterpolator,
                                       final TimeInterpolator<AbsolutePVCoordinates> absPVAInterpolator,
                                       final TimeInterpolator<TimeStampedDouble> massInterpolator,
                                       final TimeInterpolator<Attitude> attitudeInterpolator,
                                       final TimeInterpolator<TimeStampedDouble> additionalStateInterpolator) {
        super(interpolationPoints, extrapolationThreshold);
        checkAtLeastOneInterpolator(orbitInterpolator, absPVAInterpolator);
        this.outputFrame                 = outputFrame;
        this.orbitInterpolator           = orbitInterpolator;
        this.absPVAInterpolator          = absPVAInterpolator;
        this.massInterpolator            = massInterpolator;
        this.attitudeInterpolator        = attitudeInterpolator;
        this.additionalStateInterpolator = additionalStateInterpolator;
    }

    /**
     * Check that an interpolator exist for given sample state definition.
     *
     * @param sample sample (non empty)
     * @param orbitInterpolatorIsPresent flag defining if an orbit interpolator has been defined for this instance
     * @param absPVInterpolatorIsPresent flag defining if an absolute position-velocity-acceleration interpolator has been
     * defined for this instance
     *
     * @throws OrekitIllegalArgumentException if there is no defined interpolator for given sample spacecraft state
     * definition type
     */
    public static void checkSampleAndInterpolatorConsistency(final List<SpacecraftState> sample,
                                                             final boolean orbitInterpolatorIsPresent,
                                                             final boolean absPVInterpolatorIsPresent) {
        // Get first state definition
        final SpacecraftState earliestState = sample.get(0);

        if (earliestState.isOrbitDefined() && !orbitInterpolatorIsPresent ||
                !earliestState.isOrbitDefined() && !absPVInterpolatorIsPresent) {
            throw new OrekitIllegalArgumentException(OrekitMessages.WRONG_INTERPOLATOR_DEFINED_FOR_STATE_INTERPOLATION);
        }
    }

    /**
     * Check that all state are either orbit defined or based on absolute position-velocity-acceleration.
     *
     * @param states spacecraft state sample
     */
    public static void checkStatesDefinitionsConsistency(final List<SpacecraftState> states) {
        // Check all states handle the same additional states and are defined the same way (orbit or absolute PVA)
        final SpacecraftState s0               = states.get(0);
        final boolean         s0IsOrbitDefined = s0.isOrbitDefined();
        for (final SpacecraftState state : states) {
            s0.ensureCompatibleAdditionalStates(state);
            if (s0IsOrbitDefined != state.isOrbitDefined()) {
                throw new OrekitIllegalArgumentException(OrekitMessages.DIFFERENT_STATE_DEFINITION);
            }
        }
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
    public SpacecraftState interpolate(final AbsoluteDate interpolationDate, final Collection<SpacecraftState> sample) {

        final List<SpacecraftState> sampleList = new ArrayList<>(sample);

        // If sample is empty, an error will be thrown in super method
        if (!sample.isEmpty()) {

            // Check given that given states definition are consistent
            // (all defined by either orbits or absolute position-velocity-acceleration coordinates)
            checkStatesDefinitionsConsistency(sampleList);

            // Check interpolator and sample consistency
            checkSampleAndInterpolatorConsistency(sampleList, orbitInterpolator != null, absPVAInterpolator != null);
        }

        return super.interpolate(interpolationDate, sample);
    }

    /** {@inheritDoc} */
    @Override
    public List<TimeInterpolator<? extends TimeStamped>> getSubInterpolators() {

        // Add all sub interpolators that are defined
        final List<TimeInterpolator<? extends TimeStamped>> subInterpolators = new ArrayList<>();

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
    protected SpacecraftState interpolate(final InterpolationData interpolationData) {

        // Get first state definition
        final SpacecraftState earliestState   = interpolationData.getNeighborList().get(0);
        final boolean         areOrbitDefined = earliestState.isOrbitDefined();

        // Prepare samples
        final List<Attitude> attitudes = new ArrayList<>();

        final List<TimeStampedDouble> masses = new ArrayList<>();

        final List<DoubleArrayDictionary.Entry> additionalEntries = earliestState.getAdditionalStatesValues().getData();
        final Map<String, List<Pair<AbsoluteDate, double[]>>> additionalSample =
                createAdditionalStateSample(additionalEntries);

        final List<DoubleArrayDictionary.Entry> additionalDotEntries =
                earliestState.getAdditionalStatesDerivatives().getData();
        final Map<String, List<Pair<AbsoluteDate, double[]>>> additionalDotSample =
                createAdditionalStateSample(additionalDotEntries);

        // Fill interpolators with samples
        final List<SpacecraftState>       samples      = interpolationData.getCachedSamples().getAll();
        final List<Orbit>                 orbitSample  = new ArrayList<>();
        final List<AbsolutePVCoordinates> absPVASample = new ArrayList<>();
        for (SpacecraftState state : samples) {
            final AbsoluteDate currentDate = state.getDate();

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
                masses.add(new TimeStampedDouble(state.getMass(), state.getDate()));
            }

            // Add attitude sample if it is interpolated
            if (attitudeInterpolator != null) {
                attitudes.add(state.getAttitude());
            }

            if (additionalStateInterpolator != null) {

                // Add all additional state values if they are interpolated
                for (final Map.Entry<String, List<Pair<AbsoluteDate, double[]>>> entry : additionalSample.entrySet()) {
                    entry.getValue().add(new Pair<>(currentDate, state.getAdditionalState(entry.getKey())));
                }

                // Add all additional state derivative values if they are interpolated
                for (final Map.Entry<String, List<Pair<AbsoluteDate, double[]>>> entry : additionalDotSample.entrySet()) {
                    entry.getValue().add(new Pair<>(currentDate, state.getAdditionalStateDerivative(entry.getKey())));
                }
            }
        }

        // Interpolate mass
        final AbsoluteDate interpolationDate = interpolationData.getInterpolationDate();
        final double       interpolatedMass;
        if (massInterpolator != null) {
            interpolatedMass = massInterpolator.interpolate(interpolationDate, masses).getValue();
        } else {
            interpolatedMass = SpacecraftState.DEFAULT_MASS;
        }

        // Interpolate additional states and derivatives
        final DoubleArrayDictionary interpolatedAdditional;
        final DoubleArrayDictionary interpolatedAdditionalDot;
        if (additionalStateInterpolator != null) {
            interpolatedAdditional    = interpolateAdditionalState(interpolationDate, additionalSample);
            interpolatedAdditionalDot = interpolateAdditionalState(interpolationDate, additionalDotSample);
        } else {
            interpolatedAdditional    = null;
            interpolatedAdditionalDot = null;
        }

        // Interpolate orbit
        if (areOrbitDefined && orbitInterpolator != null) {
            final Orbit interpolatedOrbit = orbitInterpolator.interpolate(interpolationDate, orbitSample);

            final Attitude interpolatedAttitude = interpolateAttitude(interpolationDate, attitudes, interpolatedOrbit);

            return new SpacecraftState(interpolatedOrbit, interpolatedAttitude, interpolatedMass, interpolatedAdditional,
                                       interpolatedAdditionalDot);
        }
        // Interpolate absolute position-velocity-acceleration
        else if (!areOrbitDefined && absPVAInterpolator != null) {

            final AbsolutePVCoordinates interpolatedAbsPva = absPVAInterpolator.interpolate(interpolationDate, absPVASample);

            final Attitude interpolatedAttitude = interpolateAttitude(interpolationDate, attitudes, interpolatedAbsPva);

            return new SpacecraftState(interpolatedAbsPva, interpolatedAttitude, interpolatedMass, interpolatedAdditional,
                                       interpolatedAdditionalDot);
        }
        // Should never happen
        else {
            throw new OrekitInternalError(null);
        }

    }

    /**
     * Get output frame.
     *
     * @return output frame
     */
    public Frame getOutputFrame() {
        return outputFrame;
    }

    /**
     * Get orbit interpolator.
     *
     * @return optional orbit interpolator
     *
     * @see Optional
     */
    public Optional<TimeInterpolator<Orbit>> getOrbitInterpolator() {
        return Optional.ofNullable(orbitInterpolator);
    }

    /**
     * Get absolute position-velocity-acceleration interpolator.
     *
     * @return optional absolute position-velocity-acceleration interpolator
     *
     * @see Optional
     */
    public Optional<TimeInterpolator<AbsolutePVCoordinates>> getAbsPVAInterpolator() {
        return Optional.ofNullable(absPVAInterpolator);
    }

    /**
     * Get mass interpolator.
     *
     * @return optional mass interpolator
     *
     * @see Optional
     */
    public Optional<TimeInterpolator<TimeStampedDouble>> getMassInterpolator() {
        return Optional.ofNullable(massInterpolator);
    }

    /**
     * Get attitude interpolator.
     *
     * @return optional attitude interpolator
     *
     * @see Optional
     */
    public Optional<TimeInterpolator<Attitude>> getAttitudeInterpolator() {
        return Optional.ofNullable(attitudeInterpolator);
    }

    /**
     * Get additional state interpolator.
     *
     * @return optional additional state interpolator
     *
     * @see Optional
     */
    public Optional<TimeInterpolator<TimeStampedDouble>> getAdditionalStateInterpolator() {
        return Optional.ofNullable(additionalStateInterpolator);
    }

    /**
     * Check that at least one interpolator is defined.
     *
     * @param orbitInterpolatorToCheck orbit interpolator
     * @param absPVAInterpolatorToCheck absolute position-velocity-acceleration interpolator
     */
    private void checkAtLeastOneInterpolator(final TimeInterpolator<Orbit> orbitInterpolatorToCheck,
                                             final TimeInterpolator<AbsolutePVCoordinates> absPVAInterpolatorToCheck) {
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
    private Map<String, List<Pair<AbsoluteDate, double[]>>> createAdditionalStateSample(
            final List<DoubleArrayDictionary.Entry> additionalEntries) {
        final Map<String, List<Pair<AbsoluteDate, double[]>>> additionalSamples = new HashMap<>(additionalEntries.size());

        for (final DoubleArrayDictionary.Entry entry : additionalEntries) {
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
    private DoubleArrayDictionary interpolateAdditionalState(final AbsoluteDate interpolationDate,
                                                             final Map<String, List<Pair<AbsoluteDate, double[]>>> additionalSamples) {
        final DoubleArrayDictionary interpolatedAdditional;

        if (additionalSamples.isEmpty()) {
            interpolatedAdditional = null;
        } else {
            interpolatedAdditional = new DoubleArrayDictionary(additionalSamples.size());
            for (final Map.Entry<String, List<Pair<AbsoluteDate, double[]>>> entry : additionalSamples.entrySet()) {

                // Get current entry
                final List<Pair<AbsoluteDate, double[]>> currentAdditionalSamples = entry.getValue();

                // Extract number of values for this specific entry
                final int nbOfValues = currentAdditionalSamples.get(0).getValue().length;

                // For each value of current additional state entry
                final double[] currentInterpolatedAdditional = new double[nbOfValues];
                for (int i = 0; i < nbOfValues; i++) {

                    // Create final index for lambda expression use
                    final int currentIndex = i;

                    // Create sample for specific value of current additional state values
                    final List<TimeStampedDouble> currentValueSample = new ArrayList<>();

                    currentAdditionalSamples.forEach(currentSamples -> currentValueSample.add(
                            new TimeStampedDouble(currentSamples.getValue()[currentIndex], currentSamples.getFirst())));

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
    private Attitude interpolateAttitude(final AbsoluteDate interpolationDate, final List<Attitude> attitudes,
                                         final PVCoordinatesProvider pvProvider) {
        if (attitudes.isEmpty()) {
            final AttitudeProvider attitudeProvider = new FrameAlignedProvider(outputFrame);
            return attitudeProvider.getAttitude(pvProvider, interpolationDate, outputFrame);
        } else {
            return attitudeInterpolator.interpolate(interpolationDate, attitudes);
        }
    }
}
