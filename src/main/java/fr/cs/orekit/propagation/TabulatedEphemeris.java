package fr.cs.orekit.propagation;

import java.util.Iterator;
import java.util.TreeSet;
import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.attitudes.AttitudeKinematics;
import fr.cs.orekit.errors.PropagationException;
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

  /** Constructor with tabulated entries.
   * If wanted,  {@link #setPropagator(SimplePropagator)} can be called after
   * construction to set interpolation method. If not, classical
   * interpolation will be used.
   * @param tabulatedStates the entries tab
   */
  public TabulatedEphemeris(SpacecraftState[] tabulatedStates) {

    if(tabulatedStates.length<2) {
      throw new IllegalArgumentException("There should be at least 2 entries.");
    }

    datas = new TreeSet();
    for(int i=0; i<tabulatedStates.length; i++) {
      datas.add(new comparableState(tabulatedStates[i]));
    }

    firstElement = ((comparableState)(datas.first())).state;
    lastElement = ((comparableState)(datas.last())).state;

    previous = null;
    next = null;
    this.interpolationOrder = 1;
  }

  /** Constructor with tabulated entries.
   * If wanted,  {@link #setPropagator(SimplePropagator)} can be called after
   * construction to set interpolation method. If not, classical
   * interpolation will be used.
   * @param tabulatedStates the entries tab
   * @param interpolationOrder the required oder
   */
  public TabulatedEphemeris(SpacecraftState[] tabulatedStates, int interpolationOrder) {
    this(tabulatedStates);
    this.interpolationOrder = interpolationOrder;

  }

  /** Get the last date of the range.
   * @return the last date of the range
   */
  public AbsoluteDate getMaxDate() {
    return lastElement.getDate();
  }

  /** Get the first date of the range.
   * @return the first date of the range
   */
  public AbsoluteDate getMinDate() {
    return firstElement.getDate();
  }

  /** Get the state at a specific date.
   * @param date desired date for the state
   * @return the state at the specified date; null if date is out of range
   */
  public SpacecraftState getSpacecraftState(AbsoluteDate date) throws PropagationException {
    // Check if date is in the specified range
    if(enclosingStates(date)) {

      double tp = date.minus(previous.date);
      double tn = next.date.minus(date);
      if(tp==0&tn==0) {
        return previous.state;
      }
      // Classical interpolation
      return new SpacecraftState(new Orbit(date, getInterpolatedOp(tp, tn)), interpolatedMass(tp, tn), interpolatedAk(tp, tn));

    }
    // Not into date range, return null
    return null;
  }

  /** Get the interpolated orbital parameters.
   * @param tp time in seconds since previous date
   * @param tn time in seconds until next date
   * @return the new equinoctial paramteters
   */
  private OrbitalParameters getInterpolatedOp(double tp, double tn) {

    double dt = tp + tn;

    double a  = (tn*previous.state.getA() +tp*next.state.getA() ) / dt;
    double ex = (tn*previous.state.getEx()+tp*next.state.getEx()) / dt;
    double ey = (tn*previous.state.getEy()+tp*next.state.getEy()) / dt;
    double hx = (tn*previous.state.getHx()+tp*next.state.getHx()) / dt;
    double hy = (tn*previous.state.getHy()+tp*next.state.getHy()) / dt;
    double lv = (tn*previous.state.getLv()+tp*next.state.getLv()) / dt;

    return new EquinoctialParameters(a, ex, ey, hx, hy, lv,
        EquinoctialParameters.TRUE_LATITUDE_ARGUMENT, previous.state.getFrame());
  }

  /** Get the interpolated Attitude kinematics.
   * @param tp time in seconds since previous date
   * @param tn time in seconds until next date
   * @return the new attitude kinematics
   */
  private AttitudeKinematics interpolatedAk(double tp, double tn) {

    double dt = tp + tn;

    Transform prevToNext = new Transform(previous.state.getAkTransform().getInverse(),next.state.getAkTransform());

    Rotation newRot = new Rotation(prevToNext.getRotation().getAxis(), tp*prevToNext.getRotation().getAngle()/dt);
    Vector3D newInstRotAxis;
    if(prevToNext.getRotAxis().getNorm()!=0) {
      newInstRotAxis = new Vector3D(tp*prevToNext.getRotAxis().getNorm()/dt, prevToNext.getRotAxis().normalize());
    }
    else {
      newInstRotAxis = new Vector3D();
    }

    Transform newTrans = new Transform(previous.state.getAkTransform(), new Transform(newRot, newInstRotAxis));

    return new AttitudeKinematics(newTrans.getRotation(), newTrans.getRotAxis());

  }

  /** Get the interpolated Mass.
   * @param tp time in seconds since previous date
   * @param tn time in seconds until next date
   * @return the new mass
   */
  private double interpolatedMass(double tp, double tn) {
    return ( tn*previous.state.getMass()+ tp*next.state.getMass() ) / (tn + tp) ;
  }

  private boolean enclosingStates(AbsoluteDate date) {

    if(date.minus(firstElement.getDate())<0 || date.minus(lastElement.getDate())>0) {
      return false;
    }
    if(date.minus(firstElement.getDate())==0) {
      previous = (comparableState)datas.first();
      Iterator i = datas.iterator();
      i.next();
      next = (comparableState)i.next();
      return true;
    }
    // don't search if the cached selection is fine
    if ((previous != null) && (date.minus(previous.date) >= 0)
        && (next != null) && (date.minus(next.date) < 0)) {
      // the current selection is already good
      return true;
    }
    // reset the selection before the search phase
    previous = null;
    next     = null;

    comparableState opt = new comparableState(new SpacecraftState(new Orbit(date, new EquinoctialParameters(0, 0, 0, 0, 0, 0, 0, null))));

    previous = (comparableState)datas.headSet(opt).last();
    next = (comparableState)datas.tailSet(opt).first();
    return true;
  }

  /** All entries. */
  private final TreeSet datas;
  private int interpolationOrder;

  /** Enclosing states */
  private comparableState previous;
  private comparableState next;

  /** Bounds */
  private final SpacecraftState firstElement;
  private final SpacecraftState lastElement;

  /** Container class for SpacecraftStates.  */
  private class comparableState  implements Comparable {

    /** Entries */
    private final SpacecraftState state;
    private final AbsoluteDate date;

    /** Simple constructor. */
    private comparableState(SpacecraftState state) {
      this.state = state;
      this.date = state.getDate();
    }

    /** Compare an entry with another one, according to date. */
    public int compareTo(Object entry) {
      return date.compareTo(((comparableState)entry).date);
    }
  }
}







