/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolable;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/** This class is the representation of a complete state holding orbit, attitude
 * and mass information at a given date.
 *
 * <p>It contains an {@link Orbit orbital state} at a current
 * {@link AbsoluteDate} both handled by an {@link Orbit}, plus the current
 * mass and attitude. Orbit and state are guaranteed to be consistent in terms
 * of date and reference frame. The spacecraft state may also contain additional
 * states, which are simply named double arrays which can hold any user-defined
 * data.
 * </p>
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple keplerian model for orbit, a linear extrapolation for attitude
 * taking the spin rate into account and no mass change. It is <em>not</em>
 * intended as a replacement for proper orbit and attitude propagation but
 * should be sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * The instance <code>SpacecraftState</code> is guaranteed to be immutable.
 * </p>
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class SpacecraftState
    implements TimeStamped, TimeShiftable<SpacecraftState>, TimeInterpolable<SpacecraftState>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130407L;

    /** Default mass. */
    private static final double DEFAULT_MASS = 1000.0;

    /**
     * tolerance on date comparison in {@link #checkConsistency(Orbit, Attitude)}. 100 ns
     * corresponds to sub-mm accuracy at LEO orbital velocities.
     */
    private static final double DATE_INCONSISTENCY_THRESHOLD = 100e-9;

    /** Orbital state. */
    private final Orbit orbit;

    /** Attitude. */
    private final Attitude attitude;

    /** Current mass (kg). */
    private final double mass;

    /** Additional states. */
    private final Map<String, double[]> additional;

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     * @exception OrekitException if default attitude cannot be computed
     */
    public SpacecraftState(final Orbit orbit)
        throws OrekitException {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             DEFAULT_MASS, null);
    }

    /** Build a spacecraft state from orbit and attitude provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude)
        throws IllegalArgumentException {
        this(orbit, attitude, DEFAULT_MASS, null);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @exception OrekitException if default attitude cannot be computed
     */
    public SpacecraftState(final Orbit orbit, final double mass)
        throws OrekitException {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, null);
    }

    /** Build a spacecraft state from orbit, attitude provider and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude, final double mass)
        throws IllegalArgumentException {
        this(orbit, attitude, mass, null);
    }

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     * @param additional additional states
     * @exception OrekitException if default attitude cannot be computed
     */
    public SpacecraftState(final Orbit orbit, final Map<String, double[]> additional)
        throws OrekitException {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             DEFAULT_MASS, additional);
    }

    /** Build a spacecraft state from orbit and attitude provider.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @param additional additional states
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude, final Map<String, double[]> additional)
        throws IllegalArgumentException {
        this(orbit, attitude, DEFAULT_MASS, additional);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @param additional additional states
     * @exception OrekitException if default attitude cannot be computed
     */
    public SpacecraftState(final Orbit orbit, final double mass, final Map<String, double[]> additional)
        throws OrekitException {
        this(orbit,
             new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, additional);
    }

    /** Build a spacecraft state from orbit, attitude provider and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional states (may be null if no additional states are available)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude,
                           final double mass, final Map<String, double[]> additional)
        throws IllegalArgumentException {
        checkConsistency(orbit, attitude);
        this.orbit      = orbit;
        this.attitude   = attitude;
        this.mass       = mass;
        if (additional == null) {
            this.additional = Collections.emptyMap();
        } else {
            this.additional = new HashMap<String, double[]>(additional.size());
            for (final Map.Entry<String, double[]> entry : additional.entrySet()) {
                this.additional.put(entry.getKey(), entry.getValue().clone());
            }
        }
    }

    /** Add an additional state.
     * <p>
     * {@link SpacecraftState SpacecraftState} instances are immutable,
     * so this method does <em>not</em> change the instance, but rather
     * creates a new instance, which has the same orbit, attitude, mass
     * and additional states as the original instance, except it also
     * has the specified state. If the original instance already had an
     * additional state with the same name, it will be overridden. If it
     * did not have any additional state with that name, the new instance
     * will have one more additional state than the original instance.
     * </p>
     * @param name name of the additional state
     * @param value value of the additional state
     * @return a new instance, with the additional state added
     * @see #hasAdditionalState(String)
     * @see #getAdditionalState(String)
     * @see #getAdditionalStates()
     */
    public SpacecraftState addAdditionalState(final String name, final double ... value) {
        final Map<String, double[]> newMap = new HashMap<String, double[]>(additional.size() + 1);
        newMap.putAll(additional);
        newMap.put(name, value.clone());
        return new SpacecraftState(orbit, attitude, mass, newMap);
    }

    /** Check orbit and attitude dates are equal.
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * are not equal
     */
    private static void checkConsistency(final Orbit orbit, final Attitude attitude)
        throws IllegalArgumentException {
        if (FastMath.abs(orbit.getDate().durationFrom(attitude.getDate())) >
            DATE_INCONSISTENCY_THRESHOLD) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                                                     orbit.getDate(), attitude.getDate());
        }
        if (orbit.getFrame() != attitude.getReferenceFrame()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.FRAMES_MISMATCH,
                                                     orbit.getFrame().getName(),
                                                     attitude.getReferenceFrame().getName());
        }
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple keplerian model for orbit, a linear extrapolation for attitude
     * taking the spin rate into account and neither mass nor additional states
     * changes. It is <em>not</em> intended as a replacement for proper orbit
     * and attitude propagation but should be sufficient for small time shifts
     * or coarse accuracy.
     * </p>
     * <p>
     * As a rough order of magnitude, the following table shows the extrapolation
     * errors obtained between this simple shift method and an {@link
     * org.orekit.propagation.analytical.EcksteinHechlerPropagator Eckstein-Heschler
     * propagator} for an 800km altitude nearly circular polar Earth orbit with
     * {@link org.orekit.attitudes.BodyCenterPointing body center pointing}. Beware
     * that these results may be different for other orbits.
     * </p>
     * <table border="1" cellpadding="5">
     * <caption>Extrapolation Error</caption>
     * <tr bgcolor="#ccccff"><th>interpolation time (s)</th>
     * <th>position error (m)</th><th>velocity error (m/s)</th>
     * <th>attitude error (&deg;)</th></tr>
     * <tr><td bgcolor="#eeeeff"> 60</td><td>  20</td><td>1</td><td>0.001</td></tr>
     * <tr><td bgcolor="#eeeeff">120</td><td> 100</td><td>2</td><td>0.002</td></tr>
     * <tr><td bgcolor="#eeeeff">300</td><td> 600</td><td>4</td><td>0.005</td></tr>
     * <tr><td bgcolor="#eeeeff">600</td><td>2000</td><td>6</td><td>0.008</td></tr>
     * <tr><td bgcolor="#eeeeff">900</td><td>4000</td><td>6</td><td>0.010</td></tr>
     * </table>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * except for the mass which stay unchanged
     */
    public SpacecraftState shiftedBy(final double dt) {
        return new SpacecraftState(orbit.shiftedBy(dt), attitude.shiftedBy(dt),
                                   mass, additional);
    }

    /** {@inheritDoc}
     * <p>
     * The additional states that are interpolated are the ones already present
     * in the instance. The sample instances must therefore have at least the same
     * additional states has the instance. They may have more additional states,
     * but the extra ones will be ignored.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     */
    public SpacecraftState interpolate(final AbsoluteDate date,
                                       final Collection<SpacecraftState> sample)
        throws OrekitException {

        // prepare interpolators
        final List<Orbit> orbits = new ArrayList<Orbit>(sample.size());
        final List<Attitude> attitudes = new ArrayList<Attitude>(sample.size());
        final HermiteInterpolator massInterpolator = new HermiteInterpolator();
        final Map<String, HermiteInterpolator> additionalInterpolators =
                new HashMap<String, HermiteInterpolator>(additional.size());
        for (final String name : additional.keySet()) {
            additionalInterpolators.put(name, new HermiteInterpolator());
        }

        // extract sample data
        for (final SpacecraftState state : sample) {
            final double deltaT = state.getDate().durationFrom(date);
            orbits.add(state.getOrbit());
            attitudes.add(state.getAttitude());
            massInterpolator.addSamplePoint(deltaT,
                                            new double[] {
                                                state.getMass()
                                            });
            for (final Map.Entry<String, HermiteInterpolator> entry : additionalInterpolators.entrySet()) {
                entry.getValue().addSamplePoint(deltaT, state.getAdditionalState(entry.getKey()));
            }
        }

        // perform interpolations
        final Orbit interpolatedOrbit       = orbit.interpolate(date, orbits);
        final Attitude interpolatedAttitude = attitude.interpolate(date, attitudes);
        final double interpolatedMass       = massInterpolator.value(0)[0];
        final Map<String, double[]> interpolatedAdditional;
        if (additional.isEmpty()) {
            interpolatedAdditional = null;
        } else {
            interpolatedAdditional = new HashMap<String, double[]>(additional.size());
            for (final Map.Entry<String, HermiteInterpolator> entry : additionalInterpolators.entrySet()) {
                interpolatedAdditional.put(entry.getKey(), entry.getValue().value(0));
            }
        }

        // create the complete interpolated state
        return new SpacecraftState(interpolatedOrbit, interpolatedAttitude,
                                   interpolatedMass, interpolatedAdditional);

    }

    /** Gets the current orbit.
     * @return the orbit
     */
    public Orbit getOrbit() {
        return orbit;
    }

    /** Get the date.
     * @return date
     */
    public AbsoluteDate getDate() {
        return orbit.getDate();
    }

    /** Get the inertial frame.
     * @return the frame
     */
    public Frame getFrame() {
        return orbit.getFrame();
    }

    /** Check if an additional state is available.
     * @param name name of the additional state
     * @return true if the additional state is available
     * @see #addAdditionalState(String, double[])
     * @see #getAdditionalState(String)
     * @see #getAdditionalStates()
     */
    public boolean hasAdditionalState(final String name) {
        return additional.containsKey(name);
    }

    /** Check if two instances have the same set of additional states available.
     * <p>
     * Only the names and dimensions of the additional states are compared,
     * not their values.
     * </p>
     * @param state state to compare to instance
     * @exception OrekitException if either instance or state supports an additional
     * state not supported by the other one
     * @exception MathIllegalStateException if an additional state does not have
     * the same dimension in both states
     */
    public void ensureCompatibleAdditionalStates(final SpacecraftState state)
        throws OrekitException, MathIllegalStateException {

        // check instance additional states is a subset of the other one
        for (final Map.Entry<String, double[]> entry : additional.entrySet()) {
            final double[] other = state.additional.get(entry.getKey());
            if (other == null) {
                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE,
                                          entry.getKey());
            }
            if (other.length != entry.getValue().length) {
                throw new MathIllegalStateException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                    other.length, entry.getValue().length);
            }
        }

        if (state.additional.size() > additional.size()) {
            // the other state has more additional states
            for (final String name : state.additional.keySet()) {
                if (!additional.containsKey(name)) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE,
                                              name);
                }
            }
        }

    }

    /** Get an additional state.
     * @param name name of the additional state
     * @return value of the additional state
     * @exception OrekitException if no additional state with that name exists
     * @see #addAdditionalState(String, double[])
     * @see #hasAdditionalState(String)
     * @see #getAdditionalStates()
     */
    public double[] getAdditionalState(final String name) throws OrekitException {
        if (!additional.containsKey(name)) {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_STATE, name);
        }
        return additional.get(name).clone();
    }

    /** Get an unmodifiable map of additional states.
     * @return unmodifiable map of additional states
     * @see #addAdditionalState(String, double[])
     * @see #hasAdditionalState(String)
     * @see #getAdditionalState(String)
     */
    public Map<String, double[]> getAdditionalStates() {
        return Collections.unmodifiableMap(additional);
    }

    /** Compute the transform from orbite/attitude reference frame to spacecraft frame.
     * <p>The spacecraft frame origin is at the point defined by the orbit,
     * and its orientation is defined by the attitude.</p>
     * @return transform from specified frame to current spacecraft frame
     */
    public Transform toTransform() {
        final AbsoluteDate date = orbit.getDate();
        return new Transform(date,
                             new Transform(date, orbit.getPVCoordinates().negate()),
                             new Transform(date, attitude.getOrientation()));
    }

    /** Get the central attraction coefficient.
     * @return mu central attraction coefficient (m^3/s^2)
     */
    public double getMu() {
        return orbit.getMu();
    }

    /** Get the keplerian period.
     * <p>The keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian period in seconds
     */
    public double getKeplerianPeriod() {
        return orbit.getKeplerianPeriod();
    }

    /** Get the keplerian mean motion.
     * <p>The keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return keplerian mean motion in radians per second
     */
    public double getKeplerianMeanMotion() {
        return orbit.getKeplerianMeanMotion();
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m)
     */
    public double getA() {
        return orbit.getA();
    }

    /** Get the first component of the eccentricity vector (as per equinoctial parameters).
     * @return e cos(ω + Ω), first component of eccentricity vector
     * @see #getE()
     */
    public double getEquinoctialEx() {
        return orbit.getEquinoctialEx();
    }

    /** Get the second component of the eccentricity vector (as per equinoctial parameters).
     * @return e sin(ω + Ω), second component of the eccentricity vector
     * @see #getE()
     */
    public double getEquinoctialEy() {
        return orbit.getEquinoctialEy();
    }

    /** Get the first component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) cos(Ω), first component of the inclination vector
     * @see #getI()
     */
    public double getHx() {
        return orbit.getHx();
    }

    /** Get the second component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) sin(Ω), second component of the inclination vector
     * @see #getI()
     */
    public double getHy() {
        return orbit.getHy();
    }

    /** Get the true latitude argument (as per equinoctial parameters).
     * @return v + ω + Ω true latitude argument (rad)
     * @see #getLE()
     * @see #getLM()
     */
    public double getLv() {
        return orbit.getLv();
    }

    /** Get the eccentric latitude argument (as per equinoctial parameters).
     * @return E + ω + Ω eccentric latitude argument (rad)
     * @see #getLv()
     * @see #getLM()
     */
    public double getLE() {
        return orbit.getLE();
    }

    /** Get the mean latitude argument (as per equinoctial parameters).
     * @return M + ω + Ω mean latitude argument (rad)
     * @see #getLv()
     * @see #getLE()
     */
    public double getLM() {
        return orbit.getLM();
    }

    // Additional orbital elements

    /** Get the eccentricity.
     * @return eccentricity
     * @see #getEquinoctialEx()
     * @see #getEquinoctialEy()
     */
    public double getE() {
        return orbit.getE();
    }

    /** Get the inclination.
     * @return inclination (rad)
     * @see #getHx()
     * @see #getHy()
     */
    public double getI() {
        return orbit.getI();
    }

    /** Get the {@link TimeStampedPVCoordinates} in orbit definition frame.
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedPVCoordinates} if it needs to keep the value for a while.
     * @return pvCoordinates in orbit definition frame
     */
    public TimeStampedPVCoordinates getPVCoordinates() {
        return orbit.getPVCoordinates();
    }

    /** Get the {@link TimeStampedPVCoordinates} in given output frame.
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedPVCoordinates} if it needs to keep the value for a while.
     * @param outputFrame frame in which coordinates should be defined
     * @return pvCoordinates in orbit definition frame
     * @exception OrekitException if the transformation between frames cannot be computed
     */
    public TimeStampedPVCoordinates getPVCoordinates(final Frame outputFrame)
        throws OrekitException {
        return orbit.getPVCoordinates(outputFrame);
    }

    /** Get the attitude.
     * @return the attitude.
     */
    public Attitude getAttitude() {
        return attitude;
    }

    /** Gets the current mass.
     * @return the mass (kg)
     */
    public double getMass() {
        return mass;
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140617L;

        /** Orbit. */
        private final Orbit orbit;

        /** Attitude and mass double values. */
        private double[] d;

        /** Additional states. */
        private final Map<String, double[]> additional;

        /** Simple constructor.
         * @param state instance to serialize
         */
        private DTO(final SpacecraftState state) {

            this.orbit      = state.orbit;
            this.additional = state.additional.isEmpty() ? null : state.additional;

            final Rotation rotation             = state.attitude.getRotation();
            final Vector3D spin                 = state.attitude.getSpin();
            final Vector3D rotationAcceleration = state.attitude.getRotationAcceleration();
            this.d = new double[] {
                rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3(),
                spin.getX(), spin.getY(), spin.getZ(),
                rotationAcceleration.getX(), rotationAcceleration.getY(), rotationAcceleration.getZ(),
                state.mass
            };

        }

        /** Replace the deserialized data transfer object with a {@link SpacecraftState}.
         * @return replacement {@link SpacecraftState}
         */
        private Object readResolve() {
            return new SpacecraftState(orbit,
                                       new Attitude(orbit.getFrame(),
                                                    new TimeStampedAngularCoordinates(orbit.getDate(),
                                                                                      new Rotation(d[0], d[1], d[2], d[3], false),
                                                                                      new Vector3D(d[4], d[5], d[6]),
                                                                                      new Vector3D(d[7], d[8], d[9]))),
                                       d[10], additional);
        }

    }

}
