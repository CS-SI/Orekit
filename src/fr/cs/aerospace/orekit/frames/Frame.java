package fr.cs.aerospace.orekit.frames;

import java.util.HashMap;
import java.util.LinkedList;

import org.spaceroots.mantissa.geometry.Rotation;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.errors.Translator;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** Tridimensional references frames class.
 * 
 * <p>This class is the base class for all frames in OREKIT. The frames are
 * linked together in a tree with the J2000 frame as the root of the tree.
 * Each frame is defined by transforms combining any number of translations and
 * rotations from a reference frame which is its parent frame in the tree
 * structure.</p>
 * <p>When we say a transform t is <em>from frame<sub>A</sub>
 * to frame<sub>B</sub></em>, we mean that if the coordinates of some absolute
 * vector (say the direction of a distant star for example) has coordinates
 * u<sub>A</sub> in frame<sub>A</sub> and u<sub>B</sub> in frame<sub>B</sub>,
 * then u<sub>B</sub>={@link
 * fr.cs.aerospace.orekit.frames.Transform#transformVector(Vector3D)
 * t.transformVector(u<sub>A</sub>)}.
 * <p>The transforms may be constant or varying. For simple fixed transforms,
 * using this base class is sufficient. For varying transforms (time-dependant
 * or telemetry-based for example), it may be useful to define specific subclasses
 * that will implement {@link #updateFrame(AbsoluteDate)} or that will 
 * add some specific <code>updateFromTelemetry(telemetry)</code>
 * methods that will compute the transform and call internally 
 * the {@link #updateTransform(Transform)} method.</p>
 * 
 * @author G. Prat
 * @author L. Maisonobe
 */
public class Frame {

  /** Get the uniq J2000 frame.
   * @return the uniq instance of the J2000 frame
   */
  public static Frame getJ2000() {
    if (j2000 == null) {
      j2000 = new Frame("J2000");
    }
    return j2000;
  }

  /** Get the uniq Veis 1950 frame.
   * <p>This frame is sometimes refered to as
   * <em>&gamma;<sub>50</sub> CNES</em></p>
   * @return the uniq instance of the Veis 1950 frame
   */
  public static Frame getVeis1950() {
    if (veis1950 == null) {
      double q1 = -2.01425201682020570e-5;
      double q2 = -2.43283773387856897e-3;
      double q3 =  5.59078052583013584e-3;
      double q0 = Math.sqrt(1.0 - q1 * q1 - q2 * q2 - q3 * q3);
      veis1950 =
        new Frame(getJ2000(),
                  new Transform(new Rotation(q0, q1, q2, q3, true)),
                  "Veis1950");
    }
    return veis1950;
  }

  /** Private constructor used only for the J2000 root frame.
   * @param name name of the frame
   */
  private Frame(String name) {
    parent    = null;
    transform = new Transform();
    commons   = new HashMap();
    this.name = name;
  }

  /** Build a frame from its transform with respect to its parent.
   * <p>The convention for the transform is that it is from parent
   * frame to instance. This means that the two following frames
   * are similar:</p>
   * <pre>
   * Frame frame1 = new Frame(Frame.getJ2000(), new Transform(t1, t2));
   * Frame frame2 = new Frame(new Frame(Frame.getJ2000(), t1), t2);
   * </pre>
   * @param parent parent frame (must be non-null)
   * @param transform transform from parent frame to instance
   * @param name name of the frame
   * @exception IllegalArgumentException if the parent frame is null
   */
  public Frame(Frame parent, Transform transform, String name)
   throws IllegalArgumentException {

    if (parent == null) {
      String message = Translator.getInstance().translate("null parent frame");
      throw new IllegalArgumentException(message);
    }
    this.name      = name;
    this.parent    = parent;
    this.transform = transform;
    commons        = new HashMap();

  }
    
  /** Get the name.
   * @return the name 
   */
  public String getName() {
	  return this.name;
  }
  
  /** New definition of the java.util toString() method.
   * @return the name 
   */ 
  public String toString() {
	  return this.name;
  }

  /** Get the parent frame
   * @return parent frame
   */  
  public Frame getParent() {
	  return parent;
  }
  
  /** Update the transform from the parent frame to the instance.
   * @param transform new transform from parent frame to instance
   */
  public void updateTransform(Transform transform) {
    this.transform = transform;
  }

  /** Get the transform from the instance to another frame.
   * @param destination destination frame to which we want to transform vectors
   * @param date the date (can be null if it is sure than no date dependant frame is used)
   * @return transform from the instance to the destination frame
   * @throws OrekitException if some frame specific error occurs
   */
  public Transform getTransformTo(Frame destination, AbsoluteDate date)
    throws OrekitException {

    // common ancestor to both frames in the frames tree
    Frame common = findCommon(this, destination);

    // transform from common to instance
    Transform commonToInstance = new Transform();
    for (Frame frame = this; frame != common; frame = frame.parent) {
      frame.updateFrame(date);
      commonToInstance =
        new Transform(frame.transform, commonToInstance);
    }

    // transform from destination up to common
    Transform commonToDestination = new Transform();
    for (Frame frame = destination; frame != common; frame = frame.parent) {
      frame.updateFrame(date);
      commonToDestination =
        new Transform(frame.transform, commonToDestination);
    }

    // transform from instance to destination via common
    return new Transform(commonToInstance.getInverse(), commonToDestination);

  }
  
  /** Update the frame to the given date.
   * <p>This method is called each time {@link #getTransformTo(Frame, AbsoluteDate)}
   * is called. Default behaviour is to do nothing. The proper way to build
   * a date-dependant frame is to extend {@link Frame} and implement this method which
   * will have to call {@link #updateTransform(Transform)} with the new transform </p>
   * @param date new value of the  date
   * @exception OrekitException if some frame specific error occurs
   */
  protected void updateFrame(AbsoluteDate date) throws OrekitException {
    
  }
  
  /** Find the deepest common ancestor of two frames in the frames tree.
   * @param from origin frame
   * @param to destination frame
   * @return an ancestor frame of both <code>from</code> and <code>to</code>
   */
  private static Frame findCommon(Frame from, Frame to) {

    // have we already computed the common frame for this pair ?
    Frame common = (Frame) from.commons.get(to);
    if (common != null) {
      return common;
    }

    // definitions of the path up to the head tree for each frame
    LinkedList pathFrom = from.pathToRoot();
    LinkedList pathTo   = to.pathToRoot();

    if (pathFrom.isEmpty()||pathTo.contains(from)) { // handle root case and same branch case
      common = from;
    }
    if (pathTo.isEmpty()||pathFrom.contains(to)) { // handle root case and same branch case
      common = to;
    }
    if (common != null) {
      from.commons.put(to, common);
      to.commons.put(from, common);
      return common;
    }
  
    // at this stage pathFrom contains at least one frame
    Frame lastFrom = (Frame) pathFrom.removeLast();
    common = lastFrom; // common must be one of the instance of Frame already defined

    // at the beginning of the loop pathTo contains at least one frame
    for (Frame lastTo = (Frame) pathTo.removeLast();
         (lastTo == lastFrom) && (lastTo != null) && (lastFrom != null);
         // in order to deal with the end of the list which throwed an exception
         lastTo = (Frame) (pathTo.isEmpty() ? null : pathTo.removeLast())) {

      common = lastFrom;

      // in order to deal with the end of the list which throwed an exception
      lastFrom = (Frame) (pathFrom.isEmpty() ? null : pathFrom.removeLast());
    }

    from.commons.put(to, common);
    to.commons.put(from, common);
    return common;

  }

  /** Get the path from instance frame to the root frame.
   * @return path from instance to root, excluding instance itself
   * (empty if instance is root)
   */
  private LinkedList pathToRoot() {
    LinkedList path = new LinkedList();
    for (Frame frame = parent; frame != null; frame = frame.parent) {
      path.add(frame);
    }
    return path;
  }

  /**  parent frame (only J2000 doesn't have a parent). */
  private final Frame parent;

  /** Transform from parent frame to instance. */
  private Transform transform;

  /** Map of deepest frames commons with other frames. */
  private HashMap commons;

  /** J2000 root frame. */
  private static Frame j2000 = null;
  
  /** Mean equator 1950 frame. */
  private static Frame veis1950 = null;
  
  /** Instance name. */
  private final String name;

}
