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
package fr.cs.orekit.propagation;

import java.io.Serializable;

import org.apache.commons.math.geometry.RotationOrder;

import fr.cs.orekit.attitudes.Attitude;
import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.attitudes.LofOffset;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/** This class is the representation of a complete state holding orbit, attitude
 * and mass information.
 *
 * <p> It contains an {@link Orbit orbital state} at a current
 * {@link AbsoluteDate} both handled by an {@link OrbitOld}, plus the current
 * mass and attitude.
 * </p>
 * <p>
 * The instance <code>SpacecraftState</code> is guaranteed to be immutable.
 * </p>
 * @see fr.cs.orekit.propagation.numerical.NumericalModel
 * @author Fabien Maussion
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class SpacecraftState implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -6033960736544357234L;

    /** Orbital state. */
    private final Orbit orbit;

    /** Current mass (kg). */
    private final double mass;

    /** Attitude. */
    private final Attitude attitude;

    /** Create a new instance from orbital state and mass.
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @param attitude attitude
     */
    public SpacecraftState(final Orbit orbit, final double mass,
                           final Attitude attitude) {
        this.orbit    = orbit;
        this.mass     = mass;
        this.attitude = attitude;
    }

    /** Create a new instance from orbital state and mass.
     * <p>The attitude law is set to a default perfectly
     * {@link LofOffset LOF-aligned} law.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @exception OrekitException is attitude law cannot compute the current state
     */
    public SpacecraftState(final Orbit orbit, final double mass)
        throws OrekitException {
        this.orbit    = orbit;
        this.mass     = mass;
        final AttitudeLaw lofAligned = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        this.attitude = lofAligned.getState(orbit.getDate(),
                                            orbit.getPVCoordinates(),
                                            orbit.getFrame());
    }

    /** Create a new instance from orbital state only.
     * <p>Gives an arbitrary value (1000 kg) for the mass and
     * set the attitude law to a default perfectly
     * {@link LofOffset LOF-aligned} law.</p>
     * @param orbit the orbit
     * @exception OrekitException is attitude law cannot compute the current state
     */
    public SpacecraftState(final Orbit orbit)
        throws OrekitException  {
        this(orbit, 1000.0);
    }

    /** Gets the current orbit.
     * @return the orbit
     */
    public Orbit getOrbit() {
        return orbit;
    }

    /** Gets the current mass.
     * @return the mass (kg)
     */
    public double getMass() {
        return mass;
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

    /** Get the inertial frame.
     * @return mu central attraction coefficient (m^3/s^2)
     */
    public double getMu() {
        return orbit.getMu();
    }

    /** Gets the attitude.
     * @return the attitude.
     */
    public Attitude getAttitude() {
        return attitude;
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
     * @return pvCoordinates in orbit definition frame 
     * @exception OrekitException if the transformation between frames cannot be computed
     */
    public PVCoordinates getPVCoordinates(final Frame outputFrame) 
        throws OrekitException {
        return orbit.getPVCoordinates(outputFrame);
    }

}
