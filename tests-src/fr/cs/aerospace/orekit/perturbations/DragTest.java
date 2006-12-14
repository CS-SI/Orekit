package fr.cs.aerospace.orekit.perturbations;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.bodies.OneAxisEllipsoid;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.ITRF2000Frame;
import fr.cs.aerospace.orekit.frames.Transform;
import fr.cs.aerospace.orekit.models.perturbations.SimpleExponentialAtmosphere;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

import junit.framework.*;

public class DragTest extends TestCase {

    public DragTest(String name) {
    super(name);
  }
   
    public void testExpAtmosphere() throws OrekitException {
      Vector3D posInJ2000 = new Vector3D(10000,Vector3D.plusI);
      AbsoluteDate date = AbsoluteDate.J2000Epoch;
      Frame itrf = new ITRF2000Frame(date, true);
      SimpleExponentialAtmosphere atm = new SimpleExponentialAtmosphere(
                    new OneAxisEllipsoid(Utils.ae, 1.0 / 298.257222101), itrf,
                    0.0004, 42000.0, 7500.0);
      Vector3D vel = atm.getVelocity(date, posInJ2000, Frame.getJ2000());
            
      Transform toBody = Frame.getJ2000().getTransformTo(itrf, date);
      Vector3D test = Vector3D.crossProduct(toBody.getRotAxis(),posInJ2000);
      test = test.subtract(vel);
      
      assertEquals(0, test.getNorm(), 2.1e-5);
           
   }
       
  public static Test suite() {
    return new TestSuite(DragTest.class);
  }

}
