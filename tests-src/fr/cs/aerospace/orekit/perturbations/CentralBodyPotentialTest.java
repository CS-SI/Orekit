package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.*;
import fr.cs.aerospace.orekit.errors.OrekitException;
import junit.framework.*;

public class CentralBodyPotentialTest extends TestCase {

    public CentralBodyPotentialTest(String name) {
    super(name);
  }
    
   public void aaatestCentralBodyPotential() throws OrekitException {       
    //----------------------------------
//       double equatorialRadius = 6378.13E3;
//       double mu = 3.98600E14;
//       
//       RDate date = new RDate(RDate.J2000Epoch, 0.0);
//       
////       double[] J = new double[50+1];
////       double[][] C = new double[50+1][50+1];
////       double[][] S = new double[50+1][50+1];
////       for (int i = 0; i<=2; i++) {
////          J[i] = 0.0;
////           for (int j = 0; j<=2; j++) {
////           C[i][j] = 0.0;
////           S[i][j] = 0.0;
////           }
////      }
////
//////      J[1] = 0.0;
//////      J[2] = 0.0;
//////      J[3] = 0.0;
//////      C[0][0] = 1.0;
//////      C[1][0] = - J[1];
//////      C[1][1] = 0.0;
//////      S[1][0] = 0.0;
//////      S[1][1] = 0.0;
//////  
//////      C[2][0] = - J[2];
//////      C[3][0] = - J[3];
////////      C[2][1] = 1.0;
////////      S[2][1] = 1.0;
////////      C[1][2] = 1.0;
////////      S[1][2] = 1.0;
//////      C[2][2] = 1.0;
//////      S[2][2] = 1.0;
//////      
////       J[0]=0.0;
////       C[0][0]=1.0;
////       J[1]=0.0;
////       C[1][0]=0.0;
////       for (int i = 2; i<=50; i++) {
////          J[i] = 1.0;
////          C[i][0] = -J[i];
////           for (int j = 1; j<=50; j++) {
////           C[i][j] = 1.0;
////           S[i][j] = 1.0;
////           }
////       }
//
//    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
//    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
//    Attitude attitude = new Attitude();
//    OrbitalParameters op = new CartesianParameters();
//    op.reset(position, velocity, mu);
//    OrbitDerivativesAdder adder = new CartesianDerivativesAdder(op, mu);
//       
//    // Acceleration initialisation
//    double xDotDot = 0;
//    double yDotDot = 0;
//    double zDotDot = 0;
//
//    // Definition of the potential coefficients table
//    PotentialCoefficientsTab GEM10Tab = 
//    new PotentialCoefficientsTab("D:\\Mes Documents\\EDelente\\JAVA\\GEM10B.txt");
//    
//    // Reading the file
//    GEM10Tab.read();
//    
//    // Retrieval of C and S coefficients
//    int ndeg = GEM10Tab.getNdeg();
//    double[] J   = new double[ndeg];
//    double[][] C = new double[ndeg][ndeg];
//    double[][] S = new double[ndeg][ndeg];
//    
//    C = GEM10Tab.getNormalizedClm();
//    S = GEM10Tab.getNormalizedSlm();
//    for (int i = 0; i < ndeg; i++) {
//        J[i] = - C[i][0];
//    }
//        
//    // Creation of the Cunningham potential model
//    CunninghamPotentialModel CBP = new CunninghamPotentialModel("cbp", mu,
//                                 equatorialRadius, J, C, S);
//    
//    // Add the potential contribution to the acceleration
//    CBP.addContribution(date, position, velocity, attitude, adder);
  }
   
   public void aaatestPotentialbuilder() throws OrekitException{       
//    //----------------------------------
//       double mu = 1.0;
//       double equatorialRadius = 1.0;
//       RDate date = new RDate(RDate.J2000Epoch, 0.0);
//       Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
//       Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
//       Attitude attitude = new Attitude();
//       OrbitalParameters op = new CartesianParameters();
//       op.reset(position, velocity, mu);
//       OrbitDerivativesAdder adder = new CartesianDerivativesAdder(op, mu);
//       double[] J = new double[10+1];
//       double[][] C = new double[10+1][10+1];
//       double[][] S = new double[10+1][10+1];  
//       for (int i = 0; i<=10; i++) {
//          J[i] = 0.0;
//           for (int j = 0; j<=10; j++) {
//           C[i][j] = 0.0;
//           S[i][j] = 0.0;
//           }
//       }
//       // Acceleration
//       double xDotDot = 0;
//       double yDotDot = 0;
//       double zDotDot = 0;
//       
//       double[][] V = new double[13+1][13+1];
//       
//     CunninghamPotentialModel CBP = new CunninghamPotentialModel("cbp", mu,
//                                 equatorialRadius, J, C, S);
//     CBP.CunninghamPotentialBuilder(10, 10, 10, position, V);
  }
   
  public void aaatestPotentialDerivatives() throws OrekitException {       
    //----------------------------------
//       double mu = 1.0;
//       double equatorialRadius = 1.0;
//       RDate date = new RDate(RDate.J2000Epoch, 0.0);
//       Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
//       Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
//       Attitude attitude = new Attitude();
//       OrbitalParameters op = new CartesianParameters();
//       op.reset(position, velocity, mu);
//       OrbitDerivativesAdder adder = new CartesianDerivativesAdder(op, mu);
//       double[] J = new double[10+1];
//       double[][] C = new double[10+1][10+1];
//       double[][] S = new double[10+1][10+1];  
//       for (int i = 0; i<=10; i++) {
//         J[i] = 1.0;
//           for (int j = 0; j<=10; j++) {
//           C[i][j] = 1.0;
//           S[i][j] = 1.0;
//           }
//       }
//       
//       // Acceleration
//       double xDotDot = 0;
//       double yDotDot = 0;
//       double zDotDot = 0;
//       
//       double[][] V = new double[13+1][13+1];
//       double[] reald = new double[3];
//       double[] imd = new double[3];
//     CunninghamPotentialModel CBP = new CunninghamPotentialModel("cbp", mu,
//                                 equatorialRadius, J, C, S);
//     CBP.CunninghamPotentialBuilder(10, 10, 10, position, V);
//     CBP.CunninghamPotentialDerivativesBuilder(1, 1, 13, V, reald, imd);
//       
//        System.out.println("reald[0] = " + reald[0]);
//        System.out.println("reald[1] = " + reald[1]);
//        System.out.println("reald[2] = " + reald[2]);
//        System.out.println("imd[0] = " + imd[0]);
//        System.out.println("imd[1] = " + imd[1]);
//        System.out.println("imd[2] = " + imd[2]);
  }  
 
  public static Test suite() {
    return new TestSuite(CentralBodyPotentialTest.class);
  }

}
