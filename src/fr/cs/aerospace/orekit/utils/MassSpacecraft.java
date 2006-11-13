package fr.cs.aerospace.orekit.utils;

import fr.cs.aerospace.orekit.models.spacecraft.Spacecraft;


public class MassSpacecraft implements Spacecraft {

  public MassSpacecraft() {
    this.mass = -1.0;
  }
  
  public MassSpacecraft(double mass) {
    this.mass = mass;
  }  
  
  public double getMass() {
    return mass;
  }

  public void setMass(double mass) {
    this.mass = mass;
  }

  private double mass;
}
