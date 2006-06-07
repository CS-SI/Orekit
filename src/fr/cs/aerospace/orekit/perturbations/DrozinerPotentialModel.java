package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.OrbitDerivativesAdder;
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

public class DrozinerPotentialModel extends CentralBodyPotential {

    /** Ellipsoidic body de reference. */
    private EllipsoidicBody ellipsoidicBody;
    
    /** Droziner Earth's mu. */
    private double drozinerEarthMu;

    /** Rotation of the reference body in the inertial frame. */
    private Rotation rot;
    
   /** Creates a new instance of CentralBodyPotential.
   * Build a spherical potential without perturbing acceleration
   * @param name name of the model
   * @param mu central body attraction coefficient
   */
    public DrozinerPotentialModel(String name, double mu) {
        super(name,mu);
        ellipsoidicBody = new MyEllipsoidicBody();
        rot = new Rotation();
    }
    
    /** Creates a new instance of CentralBodyPotential.
     * @param name name of the model
     * @param mu central body attraction coefficient
     * @param equatorialRadius equatorial radius used for spherical harmonics
     * modelization
     * @param c normalized coefficients array (cosine part)
     * @param s normalized coefficients array (sine part)
     * @param degree degree of potential
     */
    public DrozinerPotentialModel(String name, double mu, double equatorialRadius, double[] J, double[][] C, double[][] S) {
        super(name,mu,equatorialRadius,J,C,S);
        ellipsoidicBody = new MyEllipsoidicBody("Clarke", 6378249.20, 0.00341);
        rot = new Rotation();
    }
    
    /** Computes the contribution of the central body potential to the
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
    
    public void addContribution(RDate t, Vector3D position, Vector3D velocity, Attitude Attitude, OrbitDerivativesAdder adder) throws OrekitException{
    
    ResetPotentialModel();
    
    rot = ellipsoidicBody.SideralTime(t,rot);
    double gst = rot.getAngle();

    // Modified time
    double offset = t.getOffset();

    // Retrieval of cartesian coordinates
    if (equatorialRadius < Constants.Epsilon) {throw new OrekitException("Equatorial radius is equal to 0");}
    double x  = position.getX() / equatorialRadius;
    double y  = position.getY() / equatorialRadius;
    double z  = position.getZ() / equatorialRadius;
    
    // Calculation of useful variables
    double r1 = Math.sqrt(x * x + y * y);
    if (r1 < Constants.Epsilon) {throw new OrekitException("Variable 'r1' is equal to 0");}
    double r  = Math.sqrt(x * x + y * y + z * z);
    if (r < Constants.Epsilon) {throw new OrekitException("Radius is equal to 0");}
    double r2 = r * r;
    double r3 = r2 * r;
    double aeOnr = 0.0;
    double zOnr = 0.0;
    double r1Onr = 0.0;
    aeOnr = 1.0 / r;
    zOnr = z / r;
    r1Onr = r1 / r;
    
    // Definition of the first acceleration terms
    double xDotDotk = 0.0;
    double yDotDotk = 0.0;
    xDotDotk = - drozinerEarthMu * x / r3;
    yDotDotk = - drozinerEarthMu * y / r3;
   
    // Zonal part of acceleration
    if (ndeg != 0) {
        double Sum1 = 0.0;
        double Sum2 = 0.0;
        double[] A = new double[ndeg+1];
        double[] B = new double[ndeg+1];
        B[0] = zOnr;
        B[1] = aeOnr * (3 * B[0] * B[0] - 1.0);
        for (int k = 2; k <= ndeg; k++) {
            double p = (double)(1 + k) / (double)k;
            B[k] = aeOnr * ((1 + p) * zOnr * B[k-1] 
                   - (double)k / (double)(k - 1) * aeOnr * B[k-2]);
            A[k] = p * aeOnr * B[k-1] - zOnr * B[k];
            Sum1 = Sum1 + J[k] * A[k];
            Sum2 = Sum2 + J[k] * B[k];
        }
    double p = - (r / r1) * (r / r1) * Sum1;
    fpot[0] = fpot[0] + xDotDotk * p;
    fpot[1] = fpot[1] + yDotDotk * p;
    fpot[2] = fpot[2] + drozinerEarthMu * Sum2 / r2;
    }
    
    // Tesseral-sectorial part of acceleration
    if (nord != 0) {
        gst = gst + Constants.w * offset;
        double singst = Math.sin(gst);
        double cosgst = Math.cos(gst);
        double xOnr1 = x / r1;
        double yOnr1 = y / r1;
        double sinl = - xOnr1 * singst + yOnr1 * cosgst;
        double cosl =   xOnr1 * cosgst + yOnr1 * singst;
        double[][] Sum1 = new double[3][nord+1];
        double[] Sum = new double[3];
        double[][] A = new double[nord+1][nord+1];
        double[][] B =  new double[nord+1][nord+1];
        double[] beta = new double[nord+1];
        beta[1] = aeOnr;
        B[1][1] = 3 * beta[1]  * zOnr  * r1Onr;
        double[] sinkl = new double[nord+1];
        double[] coskl = new double[nord+1];
        sinkl[1] = sinl;
        coskl[1] = cosl;
        double[][] H = new double[nord+1][nord+1];
        double[][] Hb = new double[nord+1][nord+1];
        double[][] D = new double[nord+1][nord+1];      
        
        for (int k = 2; k <= nord; k++) {
            sinkl[k] = sinkl[k-1] * cosl + coskl[k-1] * sinl;
            coskl[k] = coskl[k-1] * cosl - sinkl[k-1] * sinl;
            for (int j = 1; j <= k; j++) {
                H[k][j] = C[k][j] * coskl[j] + S[k][j] * sinkl[j];
                Hb[k][j] = C[k][j] * sinkl[j] - S[k][j] * coskl[j];
                if ((j >= 1) && (j <= (k - 2))) {
                    B[k][j] = aeOnr* ((double)(2 * k + 1) / (double)(k - j) * 
                              zOnr * B[k - 1][j] - (double)(k + j) / 
                              (double)(k - 1 - j) * aeOnr * B[k - 2][j]);
                    A[k][j] = (double)(k + 1) / (double)(k - j) * aeOnr * 
                              B[k - 1][j] - zOnr * B[k][j];
                }
                if (j == (k - 1)) {
                    beta[k] = (double)(2 * k - 1) * r1Onr * aeOnr * beta[k-1];
                    B[k][k-1] = (double)(2 * k + 1) * aeOnr * zOnr * B[k-1][k-1]
                                - beta[k];
                    A[k][k-1] = (double)(k + 1) * aeOnr * B[k-1][k-1] - zOnr * 
                                B[k][k-1];
                }
                if (j == k) {
                    B[k][k] = (double)(2 * k + 1) * aeOnr * r1Onr * B[k-1][k-1];
                    A[k][k] = (double)(k + 1) * r1Onr * beta[k] - zOnr * 
                              B[k][k];
                }
                D[k][j] = (double)j / (double)(k + 1) * 
                          (A[k][j] + zOnr * B[k][j]);
                Sum1[0][k] = Sum1[0][k] + A[k][j] * H[k][j];
                Sum1[1][k] = Sum1[1][k] + B[k][j] * H[k][j];
                Sum1[2][k] = Sum1[2][k] + D[k][j] * Hb[k][j];
            }
            for (int i = 0; i <= 2; i++) {
                Sum[i] = Sum[i] + Sum1[i][k];
            }
        }
        double r2Onr12 = r2 / (r1 * r1);
        double p1 = r2Onr12 * xDotDotk;
        double p2 = r2Onr12 * yDotDotk;
        fpot[0] = fpot[0] + p1 * Sum[0] - p2 * Sum[2];
        fpot[1] = fpot[1] + p2 * Sum[0] + p1 * Sum[2];
        fpot[2] = fpot[2] - drozinerEarthMu * Sum[1] / r2;
    }

    // Multiplication by the ratio mu/req^2 
    //since all the previous variables are dimensionless
    if (equatorialRadius < Constants.Epsilon) {throw new OrekitException("Equatorial radius is equal to 0");}
    double coeff = mu / (equatorialRadius * equatorialRadius);
    fpot[0] = fpot[0] * coeff;
    fpot[1] = fpot[1] * coeff;
    fpot[2] = fpot[2] * coeff;
    
    // Additition of calculated acceleration to adder
    adder.addXYZAcceleration(fpot[0], fpot[1], fpot[2]);
}
    
    /** Initialize the parameters of an instance of CentralBodyPotential.
     */
    public void ResetPotentialModel() {
        fpot = new double[3];
        rot = new Rotation();
    }
    
}

