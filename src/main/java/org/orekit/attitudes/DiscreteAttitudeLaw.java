/* Copyright 2010 Centre National d'Études Spatiales
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
package org.orekit.attitudes;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeStamped;

/** This class handles discrete (i.e ephemeris like) attitude law.

 * <p>
 * This class represents the attitude law where the satellite z axis is
 * pointing to the body frame center.</p>
 * <p>
 * The object <code>BodyCenterPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class DiscreteAttitudeLaw implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -3122873481236995517L;

    /** Attitude ephemeris array. */
    SortedSet<TimeStamped> attSortedEphem;

    /** Previous EOP entry. */
    protected Attitude prevAtt;

    /** Next EOP entry. */
    protected Attitude nextAtt;

    /** Offset from previous date. */
    protected double dtP;

    /** Offset to next date. */
    protected double dtN;


    /** Creates new instance.
     * @param bodyFrame Body frame
     */
    public DiscreteAttitudeLaw(final Collection<Attitude> attitudeEphem) {
        TreeSet<TimeStamped> attSortedEphem = new TreeSet<TimeStamped>(new ChronologicalComparator());
        attSortedEphem.addAll(attitudeEphem);
        this.attSortedEphem = attSortedEphem;
    }

    /**{@inheritDoc} */
    public Attitude getAttitude(final AbsoluteDate date)
        throws OrekitException {

        if (prepareInterpolation(date)) {
            
            // Recompute next attitude rotation is reference frame has changed 
            // (spin is ignored and will be recomputed later)
            Rotation nextRot = nextAtt.getRotation();
            Transform tPrevToNext = prevAtt.getReferenceFrame().getTransformTo(nextAtt.getReferenceFrame(), date);
            Rotation nextRotRe = nextRot.applyTo(tPrevToNext.getRotation());

            final AbsoluteDate nextDate = nextAtt.getDate();
            final AbsoluteDate prevDate = prevAtt.getDate();       
            final double dt = date.durationFrom(prevDate);
            final double dtTot = nextDate.durationFrom(prevDate);                

            // Computation of the interpolated rotation
            final Rotation prevRot = prevAtt.getRotation();

            Rotation evol = nextRotRe.applyTo(prevRot.revert());
            Vector3D axis = evol.getAxis();
            double ang = evol.getAngle();
            double omega = ang / dtTot;
            double interpolatedAngle = dt*omega;
            Rotation complementaryRot = new Rotation(axis, interpolatedAngle);
            Rotation rot = complementaryRot.applyTo(prevRot);

            // Computation of the interpolated spin
            Vector3D spin = new Vector3D(omega, axis);

            return new Attitude(date, prevAtt.getReferenceFrame(), rot, spin);

        } else {
            return null;
        }
    }

    /** Prepare interpolation between two entries.
     * @param  date target date
     * @return true if there are entries bracketing the target date
     */
    protected synchronized boolean prepareInterpolation(final AbsoluteDate date) {

        // compute offsets assuming the current selection brackets the date
        dtP = (prevAtt == null) ? -1.0 : date.durationFrom(prevAtt.getDate());
        dtN = (nextAtt == null) ? -1.0 : nextAtt.getDate().durationFrom(date);

        // check if bracketing was correct
        if ((dtP < 0) || (dtN < 0)) {

            // bad luck, we need to recompute brackets
            if (!selectBracketingEntries(date)) {
                // the specified date is outside of supported range
                return false;
            }

            // recompute offsets
            dtP = date.durationFrom(prevAtt.getDate());
            dtN = nextAtt.getDate().durationFrom(date);

        }

        return true;

    }

    /** Select the entries bracketing a specified date.
     * <p>If the date is either before the first entry or after the last entry,
     * previous and next will be set to null.</p>
     * @param  date target date
     * @return true if the date was found in the tables
     */
    protected boolean selectBracketingEntries(final AbsoluteDate date) {
        try {
            // select the bracketing elements
            nextAtt = (Attitude) (attSortedEphem.tailSet(date).first());
            prevAtt = (Attitude) (attSortedEphem.headSet(nextAtt).last());
            return true;
        } catch (NoSuchElementException nsee) {
            prevAtt = null;
            nextAtt = null;
            return false;
        }
    }

 
    
}
