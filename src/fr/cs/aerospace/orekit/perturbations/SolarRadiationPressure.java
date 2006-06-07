package fr.cs.aerospace.orekit.perturbations;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Constants;
import fr.cs.aerospace.orekit.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.SimpleVehicle;
import fr.cs.aerospace.orekit.OrekitException;
import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class represents the solar radiation pressure of the sun.
 
 * <p>The solar radiation pressure depends on the surface which is collided by.
 * photons.</p>
 
 * @version $Id$
 * @author E. Delente
 */

public class SolarRadiationPressure implements ForceModel {

    // SRP Ratio between 0.0 (total eclipse) and 1.0 (full lighting)
    private double ratio;
    
    // Definition of the Sun
    private ThirdBody Sun;
    
    
    /** Gets the swithching functions related to umbra and penumbra passes
    */
    public SWF[] getSwitchingFunctions() {
        // This function return a table containing the 2 swithing functions
        return new SWF[] {new Umbraswitch(), new Penumbraswitch()};
    }
        
    
    /** Create a SRP and define the SRP parameters.
    */
    public SolarRadiationPressure() {

      // SRP Ratio between 0.0 (total eclipse) and 1.0 (full lighting)
      ratio = 1.0;

      // Definition of the Sun
      Sun = new ThirdBody_Sun();
      
    }
    
    
    /** Get the Sat-Sun vector
    * @param t current time offset from the reference epoch (s)
    * @param position the satellite's position
    */
    private Vector3D getSatSunVector(RDate t, Vector3D position) throws OrekitException {
      Vector3D SatSunvector = new Vector3D();
      SatSunvector.setCoordinates(Sun.getPosition(t).getX() - position.getX(),
                                  Sun.getPosition(t).getY() - position.getY(),
                                  Sun.getPosition(t).getZ() - position.getZ());
      return SatSunvector;
    }
    
    
    /** Get the radiation source
     */
    public ThirdBody getSun() {
      return Sun;
    }

    
    /** Get the angle Sat-Sun / Sat-Central Body
    * @param t current time offset from the reference epoch (s)
    * @param position the satellite's position
    */
    private double getSatSunSatCentralAngle(RDate t, Vector3D position) throws OrekitException{
        
      // Satellite-Sun vector
      Vector3D SatSunvector = getSatSunVector(t, position);
      
      // Calculation of the Sat-Sun-Sat-Earth angle
      return Vector3D.angle(position, SatSunvector);
    }

    
    /** Get the lighting ratio
    * @param t current time offset from the reference epoch (s)
    * @param position the satellite's position
    */
    public double getRatio(RDate t, Vector3D position) throws OrekitException {
      
      // Satellite-Sun vector
      Vector3D SatSunvector = getSatSunVector(t, position);
      // Definition of the Earth's apparent radius
      if (position.getNorm()<Constants.Epsilon) { throw new OrekitException("sat-central body distance is equal to 0");}
      double EarthApparentRadius = Constants.CentralBodyradius / position.getNorm();
        
      // Definition of the Sun's apparent radius
      if (SatSunvector.getNorm()<Constants.Epsilon) { throw new OrekitException("sat-sun distance is equal to 0");}
      double SunApparentRadius = Constants.Sunradius / SatSunvector.getNorm();
      
      // Retrieve the Sat-Sun / Sat-Central body angle
      double SatSunSatCentralAngle = getSatSunSatCentralAngle(t, position);          
      
      // Test if the satellite is in the penumbra
      
      if ((SatSunSatCentralAngle - EarthApparentRadius - SunApparentRadius <= 0.0) 
       && (SatSunSatCentralAngle - EarthApparentRadius + SunApparentRadius >= 0.0)){
        // Test if the satellite is in the umbra
        if (SatSunSatCentralAngle - EarthApparentRadius < 0.0) {
            ratio = 0.0; // The satellite is in the umbra
        }
        else {
          // Calculation of the PRS ratio
          if (SatSunSatCentralAngle<Constants.Epsilon) { throw new OrekitException("sat-sun / sat-central body angle is equal to 0");}
          
          double alpha1 = (SatSunSatCentralAngle * SatSunSatCentralAngle - 
                          (EarthApparentRadius - SunApparentRadius) * 
                          (SunApparentRadius + EarthApparentRadius)) / 
                          (2 * SatSunSatCentralAngle);
          
          double alpha2 = (SatSunSatCentralAngle * SatSunSatCentralAngle + 
                          (EarthApparentRadius - SunApparentRadius) * 
                          (SunApparentRadius + EarthApparentRadius)) / 
                          (2 * SatSunSatCentralAngle);

          double P1 = Math.PI * SunApparentRadius * SunApparentRadius - 
                      SunApparentRadius * SunApparentRadius * 
                      Math.acos(alpha1 / SunApparentRadius) + alpha1 * 
                      Math.sqrt(SunApparentRadius * SunApparentRadius - 
                      alpha1 * alpha1);
           
          double P2 = EarthApparentRadius * EarthApparentRadius * 
                      Math.acos(alpha2 / EarthApparentRadius) - alpha2 * 
                      Math.sqrt(EarthApparentRadius * EarthApparentRadius - 
                      alpha2 * alpha2);
            
          ratio = (P1 - P2) / (Math.PI * SunApparentRadius * SunApparentRadius);
          // The satellite is in the penumbra, 0.0 <= ratio <= 1.0
        }
      }
      else {
          ratio = 1.0; // The satellite is out of the shade
      }
      return ratio;
    }
    
    
    /** This class defines the Umbra switching function (when the satellite is 
     entering the umbra zone.
    */
    private class Umbraswitch implements SWF {
        
        /** Maximal time interval between switching function checks*/
        double maxCheckInterval = 100.0;
        /** Convergence threshold in the event time search */
        double threshold = 1.0E-3;
        
        public void eventOccurred(RDate t, Vector3D position, Vector3D velocity) {
            }

       /** The G-function is the difference between the Sat-Sun-Sat-Earth 
       angle and the Earth's apparent radius */
        public double g(RDate t, Vector3D position, Vector3D velocity) throws OrekitException {
           double SatSunSatEarthAngle = getSatSunSatCentralAngle(t, position);
           double EarthApparentRadius = Constants.CentralBodyradius / position.getNorm();
           return SatSunSatEarthAngle - EarthApparentRadius;
        }
                
        public double getMaxCheckInterval() {
            return maxCheckInterval;
        }
        
        public double getThreshold() {
            return threshold;
        }
        
    }

    /** This class defines the penumbra switching function (when the satellite 
     is entering the penumbra zone).
    */
    private class Penumbraswitch implements SWF {
        
        /** Maximal time interval between switching function checks*/
        double maxCheckInterval = 100.0;
        /** Convergence threshold in the event time search */
        double threshold = 1.0E-3;

        public void eventOccurred(RDate t, Vector3D position, Vector3D velocity) {
            } 

        /** The G-function is the difference between the Sat-Sun-Sat-Earth 
        angle and the sum of the Earth's and Sun's apparent radius */
        public double g(RDate t, Vector3D position, Vector3D velocity) throws OrekitException {
            double satSunSatEarthAngle = getSatSunSatCentralAngle(t, position);
            double earthApparentRadius = Constants.CentralBodyradius / position.getNorm();
            // Satellite-Sun vector
            Vector3D SatSunvector = getSatSunVector(t, position);
            double sunApparentRadius = Constants.Sunradius / SatSunvector.getNorm();
            return satSunSatEarthAngle - earthApparentRadius - sunApparentRadius;
        }
        
        public double getMaxCheckInterval() {
            return maxCheckInterval;
        }
        
        public double getThreshold() {
            return threshold;
        }
        
    }

    
    /** Compute the contribution of the solar radiation pressure to the
    * perturbing acceleration.
    * @param t current date
    * @param position current position (m)
    * @param velocity current velocity (m/s)
    * @param Attitude current Attitude
    * @param adder object where the contribution should be added
    */
    public void addContribution(RDate t, Vector3D position, Vector3D velocity, 
                                Attitude Attitude, OrbitDerivativesAdder adder) throws OrekitException{
                                    
    // Creation of a simple vehicle
    SimpleVehicle vehicle = new SimpleVehicle(1500.0, 3.0, 2.0, 0.2, 0.3);
      
    // Taking into account the lighting ratio      
    double C0 = Constants.C0 * this.getRatio(t, position);
    
    // Definition of the SRP force
    double[] fsrp = new double[3];
    
    // Calculation of the distance satellite-Sun
    // Satellite-Sun vector
    Vector3D satSunvector = this.getSatSunVector(t, position);
    double distance = satSunvector.getNorm();
    
    // Calculation of the unit vector providing the incident flux direction
    Vector3D u = satSunvector;
    u.normalizeSelf();
    
    // Intermediate variables
    double d0Ond2 = (Constants.AU/distance)*(Constants.AU/distance);
    double C0d0Ond2 = C0 * d0Ond2;
    double Kd = (1.0 - vehicle.getAbsCoef(u)) * (1.0 - vehicle.getReflCoef(u));
    double Cu = 1 + 4.0/9.0 * Kd;
    
    fsrp[0] = C0d0Ond2 * vehicle.getSurface(u) * Cu * u.getX() / vehicle.getMass();
    fsrp[1] = C0d0Ond2 * vehicle.getSurface(u) * Cu * u.getY() / vehicle.getMass();
    fsrp[2] = C0d0Ond2 * vehicle.getSurface(u) * Cu * u.getZ() / vehicle.getMass();
    
    // Additition of calculated accelration to adder
    adder.addXYZAcceleration(fsrp[0], fsrp[1], fsrp[2]);
    }
    
}

