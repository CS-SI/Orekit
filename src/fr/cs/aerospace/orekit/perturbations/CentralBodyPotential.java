package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.CartesianDerivativesAdder;
import fr.cs.aerospace.orekit.OrbitalParameters;
import fr.cs.aerospace.orekit.CartesianParameters;
import fr.cs.aerospace.orekit.Constants;
import fr.cs.aerospace.orekit.perturbations.EllipsoidicBody;
import fr.cs.aerospace.orekit.OrekitException;

import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.geometry.Rotation;
/**
 * This class represents the gravitational field of a celestial body.
 
 * <p>The gravitational field of a central body is split in two parts.
 * The first one is the central attraction which is a single coefficient.
 * The second one is the perturbing acceleration which is expressed using
 * spherical harmonics.</p>
 
 * @version $Id$
 * @author L. Maisonobe
 * @author E. Delente
 */

public abstract class CentralBodyPotential implements ForceModel {
    
    /*** Name of the model. */
    protected String name;
    
    /** Central body attraction coefficient. */
    protected double mu;
    
    /** Equatorial radius of the Central Body. */
    protected double equatorialRadius;
    
    /** Central Body potential field vector. */    
    protected Vector3D acceleration;
    
    /** First normalized potential zonal coefficients array. */    
    protected double[] J;
    
    /** First normalized potential tesseral coefficients array. */    
    protected double[][] C;
    
    /** Second normalized potential tesseral coefficients array. */    
    protected double[][] S;
    
    /** Definition of degree, order and maximum potential size. */
    protected int ndeg;
    protected int nord;
    protected int maxpot = 100;
        
    /** Initialization of the acceleration. */
    protected double[] fpot;
    
    
   /** Create a new instance of CentralBodyPotential.
    * Build a spherical potential without perturbing acceleration
    * @param name name of the model
    * @param mu central body attraction coefficient
    */
    public CentralBodyPotential(String name, double mu) {
        this.name = name;
        this.mu   = mu;
        equatorialRadius = Double.NaN;
        acceleration = new Vector3D();
        J = null;
        C = null;
        S = null;
        ndeg = 0;
        nord = 0;
        maxpot = 100;
        fpot = new double[3];
    }
    
    
    /** Create a new instance of CentralBodyPotential.
     * @param name name of the model
     * @param mu central body attraction coefficient
     * @param equatorialRadius equatorial radius used for spherical harmonics
     * modeling
     * @param c normalized coefficients array (cosine part)
     * @param s normalized coefficients array (sine part)
     * @param degree degree of potential
     */
    public CentralBodyPotential(String name, double mu,
                                double equatorialRadius, double[] J,
                                double[][] C, double[][] S) {
        this.name = name;
        this.mu   = mu;
        this.equatorialRadius = equatorialRadius;
        acceleration = new Vector3D();
        this.J = J;
        this.C = C;
        this.S = S;
        ndeg = J.length-1;
        nord = S.length-1;
        maxpot = 100;
        fpot = new double[3];
    }
    
    
    /** Initialize the parameters of an instance of CentralBodyPotential.
     */
    public abstract void ResetPotentialModel();
    
    
    /** Compute the contribution of the central body potential to the
    * perturbing acceleration, using the Drozyner algorithm.
    * The central part of the acceleration (mu/r^2 term) is not computed here,
    * only the <em>perturbing</em> acceleration is considered, not the main
    * part.
    * @param t current date
    * @param position current position (m)
    * @param velocity current velocity (m/s)
    * @param Attitude current Attitude
    * @param adder object where the contribution should be added
    */
    public abstract void addContribution(RDate t, Vector3D position, Vector3D velocity, Attitude Attitude, OrbitDerivativesAdder adder) throws OrekitException;
    
    
    /** Get the switching functions. */
    public SWF[] getSwitchingFunctions() {
        return null;
    }
    
    
    public static void main(String args[]){
                  double equatorialRadius = 6378.13E3;
       double mu = 3.98600E14;
       
       RDate date = new RDate(RDate.J2000Epoch, 0.0);
       
//       double[] J = new double[50+1];
//       double[][] C = new double[50+1][50+1];
//       double[][] S = new double[50+1][50+1];
//       for (int i = 0; i<=2; i++) {
//          J[i] = 0.0;
//           for (int j = 0; j<=2; j++) {
//           C[i][j] = 0.0;
//           S[i][j] = 0.0;
//           }
//      }
//
////      J[1] = 0.0;
////      J[2] = 0.0;
////      J[3] = 0.0;
////      C[0][0] = 1.0;
////      C[1][0] = - J[1];
////      C[1][1] = 0.0;
////      S[1][0] = 0.0;
////      S[1][1] = 0.0;
////  
////      C[2][0] = - J[2];
////      C[3][0] = - J[3];
//////      C[2][1] = 1.0;
//////      S[2][1] = 1.0;
//////      C[1][2] = 1.0;
//////      S[1][2] = 1.0;
////      C[2][2] = 1.0;
////      S[2][2] = 1.0;
////      
//       J[0]=0.0;
//       C[0][0]=1.0;
//       J[1]=0.0;
//       C[1][0]=0.0;
//       for (int i = 2; i<=50; i++) {
//          J[i] = 1.0;
//          C[i][0] = -J[i];
//           for (int j = 1; j<=50; j++) {
//           C[i][j] = 1.0;
//           S[i][j] = 1.0;
//           }
//       }

       Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
       Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);                         
       Attitude attitude = new Attitude();
       OrbitalParameters op = new CartesianParameters();
       op.reset(position, velocity, mu);
       OrbitDerivativesAdder adder = new CartesianDerivativesAdder(op, mu);
       
       // Acceleration
       double xDotDot = 0;
       double yDotDot = 0;
       double zDotDot = 0;

    PotentialCoefficientsTab GEM10Tab = 
    new PotentialCoefficientsTab("D:\\Mes Documents\\EDelente\\JAVA\\GEM10B.txt");
        
    GEM10Tab.read();
    int ndeg = GEM10Tab.getNdeg();
    double[] J   = new double[ndeg];
    double[][] C = new double[ndeg][ndeg];
    double[][] S = new double[ndeg][ndeg];
    
    C = GEM10Tab.getNormalizedClm();
    S = GEM10Tab.getNormalizedSlm();
    for (int i=0; i < 10; i++) {
        for (int j = 0; j < 10; j++) {
            System.out.println("C[" + i + "][" + j + "]= " + C[i][j]);
            System.out.println("S[" + i + "][" + j + "]= " + S[i][j]);
        }
    }
    
    CunninghamPotentialModel CBP = new CunninghamPotentialModel("cbp", mu,
                                 equatorialRadius, J, C, S);  
       
    }
    /////////////////////////////////////////////////
    
    
    
    
    
    
    
    
}

