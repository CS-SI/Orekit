package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.SynchronizedFrame;
import fr.cs.aerospace.orekit.propagation.EquinoctialGaussEquations;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class represents the gravitational field of a celestial body.
 * <p>
 * The gravitational field of a central body is split in two parts. The first
 * one is the central attraction which is a single coefficient. The second one
 * is the perturbing acceleration which is expressed using spherical harmonics.
 * </p>
 * 
 * @version $Id$
 * @author L. Maisonobe
 * @author E. Delente
 */

public class CunninghamAttractionModel implements ForceModel {
  
  /**
   * Creates a new instance of CentralBodyPotential.
   * 
   * @param mu central body attraction coefficient
   * @param centralBodyFrame rotating body frame
   * @param equatorialRadius reference equatorial radius of the potential
   * @param C normalized coefficients array (cosine part)
   * @param S normalized coefficients array (sine part)
   */
  public CunninghamAttractionModel(double mu, SynchronizedFrame centralBodyFrame,
                                      double equatorialRadius, double[][] C,
                                      double[][] S) {
    
    
    this.bodyFrame = centralBodyFrame;
    this.equatorialRadius = equatorialRadius;
    this.mu = mu;
    this.C = C;
    this.S = S;
    ndeg = C.length - 1;
    mord = C[ndeg].length - 1;
    
    Vi = new double[C.length][];
    Vr = new double[C.length][];
    
    ViderX= new double[C.length][];
    ViderY= new double[C.length][];
    ViderZ= new double[C.length][];
    
    VrderX= new double[C.length][];
    VrderY= new double[C.length][];
    VrderZ= new double[C.length][]; 
    
    int j = 0;
    
    for(int i =0; i< C.length ; i++) { 

      Vi[i]= new double[i+1];
      Vr[i]= new double[i+1];
      
      ViderX[i]= new double[i+1];
      ViderY[i]= new double[i+1];
      ViderZ[i]= new double[i+1];
      
      VrderX[i]= new double[i+1];
      VrderY[i]= new double[i+1];
      VrderZ[i]= new double[i+1]; 
    }
    
  }
  
  /**
   * Compute the contribution of the central body potential to the perturbing
   * acceleration.
   * <p>
   * The central part of the acceleration (mu/r<sup>2</sup> term) is not
   * computed here, only the <em>perturbing</em> acceleration is considered.
   * </p>
   * 
   * @param date current date
   * @param pvCoordinates the position end velocity
   * @param adder object where the contribution should be added
   */
  public void addContribution(AbsoluteDate date, PVCoordinates pvCoordinates,
                              EquinoctialGaussEquations adder)
  throws OrekitException {
    
    // Construction of the potential array V(n,m)
    Vector3D relative = bodyFrame.getTransformTo(adder.getFrame() , date)
    .getInverse().transformPosition(pvCoordinates.getPosition());
    
    double x = relative.getX();
    double y = relative.getY();
    double z = relative.getZ();
    
    // Definition of intermediate variables
    double r2 =x * x + y * y + z * z;
    
    double r = Math.sqrt(r2);
    if (r <= equatorialRadius) {
      throw new OrekitException("underground trajectory (r = {0})",
                                new String[] {
          Double.toString(r)
      });
    }
    double onr2 = 1/r2;
    double xonr2 = x / r2;
    double yonr2 = y / r2;
    double zonr2 = z / r2;
    
    // diagonal and diagonal + 1 
    Vr[0][0] = 1.0 / r;
    Vi[0][0] = 0;
    for (int n = 1; n<= mord; n++) {
      Vr[n][n] = ( 2*n - 1 )  * ( xonr2*Vr[n-1][n-1] - yonr2*Vi[n-1][n-1] );
      Vi[n][n] = ( 2*n - 1 )  * ( yonr2*Vr[n-1][n-1] + xonr2*Vi[n-1][n-1] ); 
      Vr[n][n-1] = ( 2*n - 1 ) * zonr2 * Vr[n-1][n-1];
      Vi[n][n-1] = ( 2*n - 1 ) * zonr2 * Vi[n-1][n-1];
      
    }

    // columns
    
    for (int m = 0; m<= mord; m++) {
      for (int n = m+2; n<= ndeg; n++) {
        Vr[n][m] = ((2*n - 1)*zonr2*Vr[n-1][m] - (n+m-1)* onr2 *Vr[n-2][m])/(n-m);
        Vi[n][m] = ((2*n - 1)*zonr2*Vi[n-1][m] - (n+m-1)* onr2 *Vi[n-2][m])/(n-m);
      }
    }

    
    //-********************************************************************************************/
    
    double r3 = r2*r;
    double r4 = r2*r2;
    
    double x2 = x*x;
    double y2 = y*y;
    double z2 = z*z;
    
    double rx = (y2+z2-x2)/r4;
    double ix = -2*x*y/r4;
    
    double c1x = -2*x*z/r4;
    double c2x = -2*x/r4;
    
    double ry = -(y*x)/r4;
    double iy = (x2+z2-y2)/r4;
    
    double c1y = -2*y*z/r4;
    double c2y = -2*y/r4;
    
    double rz = -2*z*x/r4;
    double iz = -2*z*y/r4;
    
    double c1z = (x2+y2-z2)/r4;
    double c2z = -2*z/r4;
//    double calc =  3 * x * (4*z2 -  (x2+y2) )/(2*r3*r4) ;
    double sigma = x2 +y2;
    double calc20 = (2*z2 - sigma) / (2*r3*r2);
    //    System.out.println("V21   :  " +  calc);
    // diagonal  
    
    VrderX[0][0] = -x / r3;
    ViderX[0][0] = 0;
    VrderY[0][0] = -y / r3;
    ViderY[0][0] = 0;
    VrderZ[0][0] = -z / r3;
    ViderZ[0][0] = 0;
    
    for (int n = 1; n<= mord; n++) {
      VrderX[n][n] = (2*n-1)*(Vr[n-1][n-1]*rx-Vi[n-1][n-1]*ix +
                              xonr2*VrderX[n-1][n-1]-yonr2*ViderX[n-1][n-1]);
      
      ViderX[n][n] = (2*n-1)*(Vr[n-1][n-1]*ix+Vi[n-1][n-1]*rx + 
                              yonr2*VrderX[n-1][n-1]+xonr2*ViderX[n-1][n-1]);
      
      VrderY[n][n] = (2*n-1)*(Vr[n-1][n-1]*ry-Vi[n-1][n-1]*iy +
                              xonr2*VrderY[n-1][n-1]-yonr2*ViderY[n-1][n-1]);                                                                      
      
      ViderY[n][n] = (2*n-1)*(Vr[n-1][n-1]*iy+Vi[n-1][n-1]*ry + 
                              yonr2*VrderY[n-1][n-1]+xonr2*ViderY[n-1][n-1]);              
      
      VrderZ[n][n] = (2*n-1)*(Vr[n-1][n-1]*rz-Vi[n-1][n-1]*iz + 
                              xonr2*VrderZ[n-1][n-1]-yonr2*ViderZ[n-1][n-1]);
      
      ViderZ[n][n] = (2*n-1)*(Vr[n-1][n-1]*iz+Vi[n-1][n-1]*rz + 
                              yonr2*VrderZ[n-1][n-1]+xonr2*ViderZ[n-1][n-1]);
      
      VrderX[n][n-1] = ( 2 * n - 1 ) * ( c1x* Vr[n-1][n-1] + zonr2*VrderX[n-1][n-1]);
      ViderX[n][n-1] = ( 2 * n - 1 ) * ( c1x* Vi[n-1][n-1] + zonr2*ViderX[n-1][n-1]);        
      VrderY[n][n-1] = ( 2 * n - 1 ) * ( c1y* Vr[n-1][n-1] + zonr2*VrderY[n-1][n-1]);
      ViderY[n][n-1] = ( 2 * n - 1 ) * ( c1y* Vi[n-1][n-1] + zonr2*ViderY[n-1][n-1]);        
      VrderZ[n][n-1] = ( 2 * n - 1 ) * ( c1z* Vr[n-1][n-1] + zonr2*VrderZ[n-1][n-1]);
      ViderZ[n][n-1] = ( 2 * n - 1 ) * ( c1z* Vi[n-1][n-1] + zonr2*ViderZ[n-1][n-1]);      
    }
    
    // columns
    
    for (int m = 0; m<= mord; m++) {
      for (int n = m+2; n<= ndeg; n++) {
        VrderX[n][m] = (2.0*n - 1)/(n - m) * ( c1x* Vr[n-1][m] + zonr2*VrderX[n-1][m]) - (n+m-1.0)/(n-m)*(c2x*Vr[n-2][m] + zonr2*VrderX[n-2][m]);
        ViderX[n][m] = (2.0*n - 1)/(n - m) * ( c1x* Vi[n-1][m] + zonr2*ViderX[n-1][m]) - (n+m-1.0)/(n-m)*(c2x*Vi[n-2][m] + zonr2*ViderX[n-2][m]);
        VrderY[n][m] = (2.0*n - 1)/(n - m) * ( c1y* Vr[n-1][m] + zonr2*VrderY[n-1][m]) - (n+m-1.0)/(n-m)*(c2y*Vr[n-2][m] + zonr2*VrderY[n-2][m]);
        ViderY[n][m] = (2.0*n - 1)/(n - m) * ( c1y* Vi[n-1][m] + zonr2*ViderY[n-1][m]) - (n+m-1.0)/(n-m)*(c2y*Vi[n-2][m] + zonr2*ViderY[n-2][m]);
        VrderZ[n][m] = (2.0*n - 1)/(n - m) * ( c1z* Vr[n-1][m] + zonr2*VrderZ[n-1][m]) - (n+m-1.0)/(n-m)*(c2z*Vr[n-2][m] + zonr2*VrderZ[n-2][m]);
        ViderZ[n][m] = (2.0*n - 1)/(n - m) * ( c1z* Vi[n-1][m] + zonr2*ViderZ[n-1][m]) - (n+m-1.0)/(n-m)*(c2z*Vi[n-2][m] + zonr2*ViderZ[n-2][m]);
      }
    }
    
    //******************-**********************************************************************
    
    double vdX = 0.0;
    double vdY = 0.0;
    double vdZ = 0.0;
    double rn = 1.0;
    for (int n=0 ; n<=ndeg ; n++) {
      for (int m=0 ; m<=n ; m++) {
        vdX += rn*(C[n][m]*VrderX[n][m]+S[n][m]*ViderX[n][m]);
        vdY += rn*(C[n][m]*VrderY[n][m]+S[n][m]*ViderY[n][m]);
        vdZ += rn*(C[n][m]*VrderZ[n][m]+S[n][m]*ViderZ[n][m]);
      }
      rn *= equatorialRadius;
    }
    Vector3D accInBody = new Vector3D(mu*vdX,mu*vdY,mu*vdZ);
    Vector3D accInInert = bodyFrame.getTransformTo(adder.getFrame(), date).transformVector(accInBody);
    adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());
    
  }
 
  
  public SWF[] getSwitchingFunctions() {
    return null;
  }
  
  /** Initialisation of potential array. */
  
  private double[][] Vi;
  private double[][] Vr;
  
  private double[][] ViderX;
  private double[][] VrderX;
  private double[][] ViderY;
  private double[][] VrderY;
  private double[][] ViderZ;
  private double[][] VrderZ;
  
  /** Equatorial radius of the Central Body. */
  private double equatorialRadius;
  
  /** Intermediate variables. */
  private double mu;
  
  /** First normalized potential tesseral coefficients array. */
  private double[][] C;
  
  /** Second normalized potential tesseral coefficients array. */
  private double[][] S;
  
  /** Definition of degree, order and maximum potential size. */
  private int ndeg;
  
  private int mord;
  
  /** Rotating body. */
  private SynchronizedFrame bodyFrame;
  
}
