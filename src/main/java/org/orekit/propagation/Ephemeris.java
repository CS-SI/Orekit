/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.MathUtils;
import org.orekit.attitudes.Attitude;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;

/** This class is designed to accept and handle tabulated orbital entries.
 * Tabulated entries are classified and then extrapolated in way to obtain
 * continuous output, with accuracy and computation methods configured by the user.
 *
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class Ephemeris implements BoundedPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 7364933371749057468L;

    /** All entries. */
    private final SortedSet<TimeStamped> data;

    /** Previous state in the cached selection. */
    private SpacecraftState previous;

    /** Next state in the cached selection. */
    private SpacecraftState next;

    /** Constructor with tabulated entries.
     * @param tabulatedStates states table
     */
    public Ephemeris(final SpacecraftState[] tabulatedStates) {

        if (tabulatedStates.length < 2) {
            throw new IllegalArgumentException("There should be at least 2 entries.");
        }

        data = new TreeSet<TimeStamped>(new ChronologicalComparator());
        for (int i = 0; i < tabulatedStates.length; ++i) {
            data.add(tabulatedStates[i]);
        }

        previous = null;
        next     = null;

    }

    /** Get the first date of the range.
     * @return the first date of the range
     */
    public AbsoluteDate getMinDate() {
        return data.first().getDate();
    }

    /** Get the last date of the range.
     * @return the last date of the range
     */
    public AbsoluteDate getMaxDate() {
        return data.last().getDate();
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate date) {
        // Check if date is in the specified range
        if (enclosinbracketDate(date)) {

            final double tp = date.durationFrom(previous.getDate());
            final double tn = next.getDate().durationFrom(date);
            if (tp == 0 && tn == 0) {
                return previous;
            }
            // Classical interpolation
            return new SpacecraftState(getInterpolatedOp(tp, tn, date),
                                       interpolatedAttitude(date, tp, tn),
                                       interpolatedMass(tp, tn));

        }
        // outside date range, return null
        return null;
    }

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

    /** Get the interpolated orbital parameters.
     * @param tp time in seconds since previous date
     * @param tn time in seconds until next date
     * @param date desired date for the state
     * @return the new equinoctial parameters
     */
    private Orbit getInterpolatedOp(final double tp, final double tn, final AbsoluteDate date) {

        final double dt = tp + tn;
        final double cP = tp / dt;
        final double cN = tn / dt;

        final double a  = cN * previous.getA()  + cP * next.getA();
        final double ex = cN * previous.getEquinoctialEx() + cP * next.getEquinoctialEx();
        final double ey = cN * previous.getEquinoctialEy() + cP * next.getEquinoctialEy();
        final double hx = cN * previous.getHx() + cP * next.getHx();
        final double hy = cN * previous.getHy() + cP * next.getHy();
        final double lv = cN * previous.getLv() + cP * MathUtils.normalizeAngle(next.getLv(), previous.getLv());

        return new EquinoctialOrbit(a, ex, ey, hx, hy, lv,
                                         EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                         previous.getFrame(), date, previous.getMu());

    }

    /** Get the interpolated Attitude.
     * @param date interpolation date
     * @param tp time in seconds since previous date
     * @param tn time in seconds until next date
     * @return the new attitude kinematics
     */
    private Attitude interpolatedAttitude(final AbsoluteDate date, final double tp, final double tn) {

        final double dt = tp + tn;

        final Transform prevToNext =
            new Transform(new Transform(previous.getAttitude().getRotation().revert()),
                          new Transform(next.getAttitude().getRotation()));

        final Rotation newRot = new Rotation(prevToNext.getRotation().getAxis(),
                                             tp * prevToNext.getRotation().getAngle() / dt);
        Vector3D newInstRotAxis;
        if (prevToNext.getRotationRate().getNorm() != 0) {
            newInstRotAxis = new Vector3D(tp * prevToNext.getRotationRate().getNorm() / dt,
                                          prevToNext.getRotationRate().normalize());
        } else {
            newInstRotAxis = Vector3D.ZERO;
        }

        final Transform newTrans =
            new Transform(new Transform(previous.getAttitude().getRotation()),
                          new Transform(newRot, newInstRotAxis));

        return new Attitude(date, previous.getFrame(), newTrans.getRotation(), newTrans.getRotationRate());

    }

    /** Get the interpolated Mass.
     * @param tp time in seconds since previous date
     * @param tn time in seconds until next date
     * @return the new mass
     */
    private double interpolatedMass(final double tp, final double tn) {
        return (tn * previous.getMass() + tp * next.getMass()) / (tn + tp);
    }

    /** Find the states bracketing a date.
     * @param date date to bracket
     * @return true if bracketing states have been found
     */
    private boolean enclosinbracketDate(final AbsoluteDate date) {

        if (date.durationFrom(getMinDate()) < 0 || date.durationFrom(getMaxDate()) > 0) {
            return false;
        }

        if (date.durationFrom(getMinDate()) == 0) {
            previous = (SpacecraftState) data.first();
            final Iterator<TimeStamped> i = data.iterator();
            i.next();
            next = (SpacecraftState) i.next();
            return true;
        }

        // don't search if the cached selection is fine
        if ((previous != null) && (date.durationFrom(previous.getDate()) >= 0) &&
            (next != null) && (date.durationFrom(next.getDate()) < 0)) {
            // the current selection is already good
            return true;
        }

        // search bracketing states
        previous = (SpacecraftState) data.headSet(date).last();
        next     = (SpacecraftState) data.tailSet(date).first();

        return true;
    }

}







