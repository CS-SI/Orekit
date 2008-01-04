package fr.cs.orekit.tutorials;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/** The aim of this tutorial is to manipulate transforms, frames and position and velocity coordinates.
 * @author F. Maussion
 */
public class TransformAndFrames {

  public static void transform() throws OrekitException {

    // initial point in frame 1 :

    Frame frame1 = new Frame(Frame.getJ2000(), new Transform(), "frame 1");
    PVCoordinates pointP1 = new PVCoordinates(Vector3D.plusI, Vector3D.plusI);

    // translation transform
/* We want to translate frame1 to the right at the speed of 1 so that P1 is fixed in it. */

    Transform frame1toframe2 = new Transform(Vector3D.minusI, Vector3D.minusI);
      // in vectorial transform convention, the translation is actually minusI !
    Frame frame2 = new Frame(frame1, frame1toframe2, "frame 2");
    PVCoordinates pointP2 = frame1.getTransformTo(frame2, new AbsoluteDate())
                      .transformPVCoordinates(pointP1);
    System.out.println(" point P1 in frame 2 : " + pointP2);


    // rotation transform
/* We want to rotate frame1 of minus PI/2 so that P1 has now for coordinates :
 *                                  pos : (0,1,0) and vel : (-2, 1, 0) */

    Rotation R = new Rotation(Vector3D.plusK, Math.PI/2);
     // in vectorial transform convention, the rotation is actually PLUS pi/2 !
    Transform frame1toframe3 = new Transform(R, new Vector3D(0, 0, -2));
    Frame frame3 = new Frame(frame1, frame1toframe3, "frame 3");
    PVCoordinates pointP3 = frame1.getTransformTo(frame3, new AbsoluteDate())
                 .transformPVCoordinates(pointP1);
    System.out.println(" point P1 in frame 3 : " + pointP3);

    // combine translation and rotation
 /* The origin of the frame 2 should become the point P3 in frame 3. Let's check this result
  * by combining two transforms in the frame tree : */
    PVCoordinates initialPoint = new PVCoordinates(new Vector3D(0,0,0), new Vector3D(0,0,0));
    PVCoordinates finalPoint = frame2.getTransformTo(frame3, new AbsoluteDate())
                                .transformPVCoordinates(initialPoint);
    System.out.println(" origin of frame 2 expressed in frame 3 : " + finalPoint);

  }



}
