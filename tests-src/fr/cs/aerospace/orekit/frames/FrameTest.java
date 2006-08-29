package fr.cs.aerospace.orekit.frames;

import java.util.Random;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FrameTest extends TestCase {
  
  public void testSameFrameRoot() {
    Random random = new Random(0x29448c7d58b95565l);
    Frame  frame  = Frame.getJ2000();
    checkNoTransform(frame.getTransformTo(frame), random);
  }
  
  public void testSameFrameNoRoot() {
    Random random = new Random(0xc6e88d0f53e29116l);
    Transform t   = randomTransform(random);
    Frame frame   = new Frame(Frame.getJ2000(), t);
    checkNoTransform(frame.getTransformTo(frame), random);
  }

  public void testSimilarFrames() {
    Random random = new Random(0x1b868f67a83666e5l);
    Transform t   = randomTransform(random);
    Frame frame1  = new Frame(Frame.getJ2000(), t);
    Frame frame2  = new Frame(Frame.getJ2000(), t);
    checkNoTransform(frame1.getTransformTo(frame2), random);
  }

  public void testFromParent() {
    Random random = new Random(0xb92fba1183fe11b8l);
    Transform fromJ2000  = randomTransform(random);
    Frame frame = new Frame(Frame.getJ2000(), fromJ2000);
    Transform toJ2000 = frame.getTransformTo(Frame.getJ2000());
    checkNoTransform(new Transform(fromJ2000, toJ2000), random);
  }

  public void testDecomposedTransform() {
    Random random = new Random(0xb7d1a155e726da57l);
    Transform t1  = randomTransform(random);
    Transform t2  = randomTransform(random);
    Transform t3  = randomTransform(random);
    Frame frame1 =
      new Frame(Frame.getJ2000(), new Transform(new Transform(t1, t2), t3));
    Frame frame2 =
      new Frame(new Frame(new Frame(Frame.getJ2000(), t1), t2), t3);
    checkNoTransform(frame1.getTransformTo(frame2), random);
  }

  private Transform randomTransform(Random random) {
    Transform transform = new Transform();
    for (int i = random.nextInt(10); i > 0; --i) {
      if (random.nextBoolean()) {
        Vector3D u = new Vector3D(random.nextDouble() * 1000.0,
                                  random.nextDouble() * 1000.0,
                                  random.nextDouble() * 1000.0);
        transform = new Transform(transform, new Transform(u));
      } else {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        Rotation r = new Rotation(q0 / q, q1 / q, q2 / q, q3 / q);
        transform = new Transform(transform, new Transform(r));
      }
    }
    return transform;
  }

  private void checkNoTransform(Transform transform, Random random) {
    for (int i = 0; i < 100; ++i) {
      Vector3D a = new Vector3D(random.nextDouble(),
                                random.nextDouble(),
                                random.nextDouble());
      Vector3D b = transform.transformDirection(a);
      assertEquals(0, Vector3D.subtract(a, b).getNorm(), 1.0e-10);
      Vector3D c = transform.transformPosition(a);
      assertEquals(0, Vector3D.subtract(a, c).getNorm(), 1.0e-10);
    }
  }
  
//  // Test case : validation of specific transformations
//  public void testFrame_WithTransformation(){
//    try {
//      // init of the frames
//      ComputationEngine comput = new ComputationEngine(vdi);
//      
//      // ########################
//      // coherced values for TM
//      
//      // attitude transformation J2000 -> Gatv   
//      Rotation AtvAttitude = new Rotation(RotationOrder.ZYX, 205.490486405701, -7.12618724276964, 41.1364190578881);      
//      TMatv.setAtvAttitude(new Quat4d(AtvAttitude.getQ1(),AtvAttitude.getQ2(),AtvAttitude.getQ3(),AtvAttitude.getQ0() )); // valeurs bidons
//      
//      // ATV COM position (origin Gatv) expressed in ECEF (Oecef = OJ2000)
//      TMatv.setAtvPosition(new Point3d(1337749., -4262689., -5021426.)); // m
//      TMatv.setAtvVelocity(new Point3d(7476., 204., 1828.)); // m/s
//      
//      // vector OTatv to OGatv (in Tatv frame) =  Position of COM (origin of Gatv) 
//      TMatv.setAtvLocation(new Point3d(5, 0.01,0.02)); // valeurs bidons en m (compatible avec la taille ATV)
//      
//      // ########################
//      
//      
//      // completion of frames due to TM values
//      comput.updateFromTM(TMatv, TMiss);       
//      
//      System.out.println(" >>>>> With transformation ");
//      
//      // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//      // Test case : validation of origin translation of the frame
//      
//      //--------------------------------------------------
//      // origin DUA frame -> to be expressed in Tatv i.e. vector Oatv to ODUA in Tatv frame 
//      Vector3D v = new Vector3D(0,0,0); 
//      Vector3D u = Frame.applyTransform(v, comput.getDUA(), comput.getTatv());
//      
//      assertEquals(u.getX(), 8.506, Math.abs(u.getNorm())*epsilonTest);
//      assertEquals(u.getY(), 0, Math.abs(u.getNorm())*epsilonTest);
//      assertEquals(u.getZ(), 0, Math.abs(u.getNorm())*epsilonTest);
//      
//      // origin Tatv frame -> to be transformed in DUA
//      v = new Vector3D(0,0,0); 
//      u = Frame.applyTransform(v, comput.getTatv(), comput.getDUA());
//      
//      assertEquals(u.getX(), -8.506, Math.abs(u.getNorm())*epsilonTest);
//      assertEquals(u.getY(), 0, Math.abs(u.getNorm())*epsilonTest);
//      assertEquals(u.getZ(), 0, Math.abs(u.getNorm())*epsilonTest);
//      
//      
//      //--------------------------------------------------
//      // origin OPh frame -> to be expressed in DUA i.e. vector ODUA to OPh in DUA frame 
//      v = new Vector3D(0,0,0); 
//      // computation of ODUA to OPH in DUA frame
//      u = Frame.applyTransform(v, comput.getPH(), comput.getDUA());
//      
//      // OPh to Odua (in Tatv frame)
//      Vector3D OPHtoODUAinTatv =  new Vector3D(vdi.getVdiFrames().getPositionPh2DuaInAtv().x,
//                                               vdi.getVdiFrames().getPositionPh2DuaInAtv().y,
//                                               vdi.getVdiFrames().getPositionPh2DuaInAtv().z);
//      v = new Vector3D(0,0,0); 
//      // computation of OTatv to Odua in Tatv frame
//      Vector3D OATVtoODUAinTATV = Frame.applyTransform(v, comput.getDUA(), comput.getTatv());
//      
//      // computation of Oatv to OPh in Tatv
//      Vector3D OATVtoOPhinTATV = Vector3D.subtract(OATVtoODUAinTATV, OPHtoODUAinTatv);
//      
//      // transformation in DUA
//      Vector3D OATVtoOPhinDUA = Frame.applyTransform(OATVtoOPhinTATV, comput.getTatv(), comput.getDUA());
//      
//      v = new Vector3D(0,0,0); 
//      // computation of Odua to Oatv in DUA frame
//      Vector3D ODUAtoOATVinDUA = Frame.applyTransform(v, comput.getTatv(), comput.getDUA());
//      
//      // computation of ODUA to OPH in DUA frame
//      Vector3D ODUAtoOPHinDUA = Vector3D.add(ODUAtoOATVinDUA,OATVtoOPhinDUA);
//      
//      System.out.println("@@@@@@@@@@@@@@ DEBUGGER cas PH to DUA");
////    assertEquals(u.getX(), ODUAtoOPHinDUA.getX(), Math.abs(u.getNorm())*epsilonTest);
////    assertEquals(u.getY(), ODUAtoOPHinDUA.getY(), Math.abs(u.getNorm())*epsilonTest);
////    assertEquals(u.getZ(), ODUAtoOPHinDUA.getZ(), Math.abs(u.getNorm())*epsilonTest);
////    
//      
//      
//      // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//      // Test case : put the COM of ATV at the center of the Earth for norm validation
//      TMatv.setAtvPosition(new Point3d(0,0,0)); // valeurs bidons (en km remise en m)
//      // upadte of frames due to TM values
//      comput.updateFromTM(TMatv, TMiss);
//      
//      v = new Vector3D(-2.5, 1.2, -0.28);
//      u = Frame.applyTransform(v, comput.getGatv(), comput.getJ2000());
//      
//      System.out.println(" ... with COM ATV at the center of the Earth !!! Norms must be equals"); 
//      System.out.println("Before transformation in Gatv: x = " + v.getX() + " y = " + v.getY() + " z = " + v.getZ() + " norme = " + v.getNorm());
//      System.out.println("After  transformation in J2000: x = " + u.getX() + " y = " + u.getY() + " z = " + u.getZ() + " norme = " + u.getNorm());
//      assertEquals(v.getNorm(), u.getNorm(), Math.abs(v.getNorm())*epsilonTest);
//      
//      
//      // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//      // Test case : two ways transform (come back at the original state)
//      TMatv.setAtvPosition(new Point3d(6778e3, 6778e3, 2e3)); // valeurs bidons (en km remise en m)
//      // update of frames due to TM values
//      comput.updateFromTM(TMatv, TMiss);
//      
//      
//      // in a sensor frame
//      
//      v = new Vector3D(-450., -450., -250.);
//      Vector3D uTatv = Frame.applyTransform(v, comput.getTGM2(), comput.getTatv());
//      Vector3D vTGM2= Frame.applyTransform(uTatv, comput.getTatv(), comput.getTGM2());
//      
//      assertEquals(vTGM2.getX(), v.getX(), Math.abs(v.getNorm())*epsilonTest);
//      assertEquals(vTGM2.getY(), v.getY(), Math.abs(v.getNorm())*epsilonTest);
//      assertEquals(vTGM2.getZ(), v.getZ(), Math.abs(v.getNorm())*epsilonTest);
//      
//      // TODO ajouter des cas tests !!!!!!!!!!!!!!!!!!!!!!!!!
//      
//      
//    } catch (GmsException gmsE){
//      fail(gmsE.getMessage());
//    }
//  }
//  
//  // Test case : in order to draw each frames with a 3D drawing tools
//  //             and to compare with specifications documents
//  public void testFrame_ForDrawing(){
//    try {
//      // init of the frames
//      ComputationEngine comput = new ComputationEngine(vdi);
//      
//      // ########################
//      // coherced values for TM
//      
//      // attitude transformation J2000 -> Gatv   
//      Rotation AtvAttitude = new Rotation(RotationOrder.ZYX, 205.490486405701, -7.12618724276964, 41.1364190578881);      
//      TMatv.setAtvAttitude(new Quat4d(AtvAttitude.getQ1(),AtvAttitude.getQ2(),AtvAttitude.getQ3(),AtvAttitude.getQ0() )); // valeurs bidons
//      
//      // ATV COM position (origin Gatv) expressed in ECEF (Oecef = OJ2000)
//      TMatv.setAtvPosition(new Point3d(1337749., -4262689., -5021426.)); // m
//      TMatv.setAtvVelocity(new Point3d(7476., 204., 1828.)); // m/s
//      
//      // vector OTatv to OGatv (in Tatv frame) =  Position of COM (origin of Gatv) 
//      TMatv.setAtvLocation(new Point3d(5, 0.01,0.02)); // valeurs bidons en m (compatible avec la taille ATV)
//      
//      // ########################
//      
//      
//      // completion of frames due to TM values
//      comput.updateFromTM(TMatv, TMiss);
//      
//      // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
//      
//      Vector3D i = Vector3D.plusI;
//      Vector3D j = Vector3D.plusJ;
//      Vector3D k = Vector3D.plusK;
//      
//      
//      
//    } catch (GmsException gmsE){
//      fail(gmsE.getMessage());
//    }
//  }

  public static Test suite() {
    return new TestSuite(FrameTest.class);
  }
  
}
