package fr.cs.aerospace.orekit.models.spacecraft;

public interface ManeuverSpacecraft extends Spacecraft{
	  
      /** Get the mass.
	   * @return mass (kg)
	   */
	  public double getMass();

	  /** Set the mass.
	   * @param mass new mass (kg)
	   */
	  public void setMass(double mass);
      
      /** Get the outflow.
       * @return outflow (kg/s)
       */
      public double getOutFlow();

      /** Get the Isp.
       * @return Isp (s)
       */
      public double getIsp();
      
      /** Get g0 assoviated to the Isp.
       * @return g0 (m/s^2)
       */
      public double getg0();

}
