package fr.cs.orekit.propagation;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.attitudes.Attitude;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.time.AbsoluteDate;

/** This class is designed to accept and handle tabulated orbital entries.
 * Tabulated entries are classified and then extrapolated in way to obtain
 * continuous output, with accuracy and computation methods configured by the user.
 *
 * @author F. Maussion
 */
public class TabulatedEphemeris implements BoundedEphemeris {

    /** Serializable UID. */
    private static final long serialVersionUID = 3896701058258948968L;

    /** All entries. */
    private final TreeSet data;

    /** Enclosing states */
    private SpacecraftState previous;
    private SpacecraftState next;

    /** Constructor with tabulated entries.
     * @param tabulatedStates states table
     */
    public TabulatedEphemeris(SpacecraftState[] tabulatedStates) {

        if (tabulatedStates.length < 2) {
            throw new IllegalArgumentException("There should be at least 2 entries.");
        }

        data = new TreeSet(new StateComparator());
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
        return ((SpacecraftState) data.first()).getDate();
    }

    /** Get the last date of the range.
     * @return the last date of the range
     */
    public AbsoluteDate getMaxDate() {
        return ((SpacecraftState) data.last()).getDate();
    }

    /** Get the state at a specific date.
     * @param date desired date for the state
     * @return the state at the specified date; null if date is out of range
     */
    public SpacecraftState getSpacecraftState(AbsoluteDate date) {
        // Check if date is in the specified range
        if (enclosinbracketDate(date)) {

            final double tp = date.minus(previous.getDate());
            final double tn = next.getDate().minus(date);
            if (tp == 0 && tn == 0) {
                return previous;
            }
            // Classical interpolation
            return new SpacecraftState(new Orbit(date, getInterpolatedOp(tp, tn)),
                                       interpolatedMass(tp, tn),
                                       interpolatedAttitude(tp, tn));

        }
        // outside date range, return null
        return null;
    }

    /** Get the interpolated orbital parameters.
     * @param tp time in seconds since previous date
     * @param tn time in seconds until next date
     * @return the new equinoctial paramteters
     */
    private OrbitalParameters getInterpolatedOp(double tp, double tn) {

        final double dt = tp + tn;
        final double cP = tp / dt;
        final double cN = tn / dt;

        final double a  = cN * previous.getA()  + cP *next.getA();
        final double ex = cN * previous.getEx() + cP *next.getEx();
        final double ey = cN * previous.getEy() + cP *next.getEy();
        final double hx = cN * previous.getHx() + cP *next.getHx();
        final double hy = cN * previous.getHy() + cP *next.getHy();
        final double lv = cN * previous.getLv() + cP *next.getLv();

        return new EquinoctialParameters(a, ex, ey, hx, hy, lv,
                                         EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                         previous.getFrame());

    }

    /** Get the interpolated Attitude.
     * @param tp time in seconds since previous date
     * @param tn time in seconds until next date
     * @return the new attitude kinematics
     */
    private Attitude interpolatedAttitude(double tp, double tn) {

        final double dt = tp + tn;

        final Transform prevToNext =
            new Transform(new Transform(previous.getAttitude().getRotation().revert()),
                          new Transform(next.getAttitude().getRotation()));

        final Rotation newRot = new Rotation(prevToNext.getRotation().getAxis(),
                                             tp * prevToNext.getRotation().getAngle() / dt);
        Vector3D newInstRotAxis;
        if (prevToNext.getRotAxis().getNorm() != 0) {
            newInstRotAxis = new Vector3D(tp * prevToNext.getRotAxis().getNorm() / dt,
                                          prevToNext.getRotAxis().normalize());
        } else {
            newInstRotAxis = new Vector3D();
        }

        final Transform newTrans =
            new Transform(new Transform(previous.getAttitude().getRotation()),
                          new Transform(newRot, newInstRotAxis));

        return new Attitude(previous.getFrame(), newTrans.getRotation(), newTrans.getRotAxis());

    }

    /** Get the interpolated Mass.
     * @param tp time in seconds since previous date
     * @param tn time in seconds until next date
     * @return the new mass
     */
    private double interpolatedMass(double tp, double tn) {
        return (tn * previous.getMass() + tp * next.getMass()) / (tn + tp);
    }

    /** Find the states bracketing a date.
     * @param date date to bracket
     * @return true if bracketing states have been found
     */
    private boolean enclosinbracketDate(AbsoluteDate date) {

        if (date.minus(getMinDate()) < 0 || date.minus(getMaxDate()) > 0) {
            return false;
        }

        if (date.minus(getMinDate()) == 0) {
            previous = (SpacecraftState) data.first();
            final Iterator i = data.iterator();
            i.next();
            next = (SpacecraftState) i.next();
            return true;
        }

        // don't search if the cached selection is fine
        if ((previous != null) && (date.minus(previous.getDate()) >= 0) &&
            (next != null) && (date.minus(next.getDate()) < 0)) {
            // the current selection is already good
            return true;
        }

        // search bracketing states
        previous = (SpacecraftState) data.headSet(date).last();
        next     = (SpacecraftState) data.tailSet(date).first();

        return true;
    }

    /** Specialized comparator handling both {@link SpacecraftState}
     * and {@link AbsoluteDate} instances.
     */
    private static class StateComparator implements Comparator, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 2878055547954956150L;

        /** Build a comparator for either {@link AbsoluteDate} or
         * {@link SpacecraftState} instances.
         * @param o1 first object
         * @param o2 second object
         * return a negative integer if o1 is before o2, 0 if they are
         * are the same time, a positive integer otherwise
         */
        public int compare(Object o1, Object o2) {
            final AbsoluteDate d1 =
                (o1 instanceof AbsoluteDate) ? ((AbsoluteDate) o1) : ((SpacecraftState) o1).getDate();
            final AbsoluteDate d2 =
                (o2 instanceof AbsoluteDate) ? ((AbsoluteDate) o2) : ((SpacecraftState) o2).getDate();
            return d1.compareTo(d2);
        }

    }

}







