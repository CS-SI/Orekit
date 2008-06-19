/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/** This class is the representation of a complete state holding orbit, attitude
 * and mass information.
 *
 * <p> It contains an {@link Orbit orbital state} at a current
 * {@link AbsoluteDate} both handled by an {@link Orbit}, plus the current
 * mass and attitude.
 * </p>
 * <p>
 * The instance <code>SpacecraftState</code> is guaranteed to be immutable.
 * </p>
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @author Fabien Maussion
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class SpacecraftState implements Comparable<SpacecraftState>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 6229927579350580132L;

    /** Default mass. */
    private static final double DEFAULT_MASS = 1000.0;

    /** Default attitude law. */
    private static final Attitude DEFAULT_ATTITUDE =
        new Attitude(Frame.getJ2000(), Rotation.IDENTITY, Vector3D.ZERO);

    /** Orbital state. */
    private final Orbit orbit;

    /** Attitude. */
    private final Attitude attitude;

    /** Current mass (kg). */
    private final double mass;

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     */
    public SpacecraftState(final Orbit orbit) {
        this(orbit, DEFAULT_ATTITUDE, DEFAULT_MASS);
    }

    /** Build a spacecraft state from orbit and attitude law.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude) {
        this(orbit, attitude, DEFAULT_MASS);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     */
    public SpacecraftState(final Orbit orbit, final double mass) {
        this(orbit, DEFAULT_ATTITUDE, mass);
    }

    /** Build a spacecraft state from orbit, attitude law and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude,
                           final double mass) {
        this.orbit    = orbit;
        this.attitude = attitude;
        this.mass     = mass;
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

    /** Get the central attraction coefficient.
     * @return mu central attraction coefficient (m^3/s^2)
     */
    public double getMu() {
        return orbit.getMu();
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m)
     */
    public double getA() {
        return orbit.getA();
    }

    /** Get the first component of the eccentricity vector (as per equinoctial parameters).
     * @return e cos(&omega; + &Omega;), first component of eccentricity vector
     * @see #getE()
     */
    public double getEquinoctialEx() {
        return orbit.getEquinoctialEx();
    }

    /** Get the second component of the eccentricity vector (as per equinoctial parameters).
     * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
     * @see #getE()
     */
    public double getEquinoctialEy() {
        return orbit.getEquinoctialEy();
    }

    /** Get the first component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) cos(&Omega;), first component of the inclination vector
     * @see #getI()
     */
    public double getHx() {
        return orbit.getHx();
    }

    /** Get the second component of the inclination vector (as per equinoctial parameters).
     * @return tan(i/2) sin(&Omega;), second component of the inclination vector
     * @see #getI()
     */
    public double getHy() {
        return orbit.getHy();
    }

    /** Get the true latitude argument (as per equinoctial parameters).
     * @return v + &omega; + &Omega; true latitude argument (rad)
     * @see #getLE()
     * @see #getLM()
     */
    public double getLv() {
        return orbit.getLv();
    }

    /** Get the eccentric latitude argument (as per equinoctial parameters).
     * @return E + &omega; + &Omega; eccentric latitude argument (rad)
     * @see #getLv()
     * @see #getLM()
     */
    public double getLE() {
        return orbit.getLE();
    }

    /** Get the mean latitude argument (as per equinoctial parameters).
     * @return M + &omega; + &Omega; mean latitude argument (rad)
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

    /** Get the {@link PVCoordinates} in orbit definition frame.
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link PVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link PVCoordinates} if it needs to keep the value for a while.
     * @return pvCoordinates in orbit definition frame
     */
    public PVCoordinates getPVCoordinates() {
        return orbit.getPVCoordinates();
    }

    /** Get the {@link PVCoordinates} in given output frame.
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link PVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link PVCoordinates} if it needs to keep the value for a while.
     * @param outputFrame frame in which coordinates should be defined
     * @return pvCoordinates in orbit definition frame
     * @exception OrekitException if the transformation between frames cannot be computed
     */
    public PVCoordinates getPVCoordinates(final Frame outputFrame)
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

    /** Compare chronologically the instance with another state.
     * @param other other spacecraft state to compare the instance to
     * @return a negative integer, zero, or a positive integer as this state
     * is before, simultaneous, or after the other one.
     */
    public int compareTo(SpacecraftState other) {
        return orbit.getDate().compareTo(other.orbit.getDate());
    }

}
