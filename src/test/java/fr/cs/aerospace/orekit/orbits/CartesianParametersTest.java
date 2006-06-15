package fr.cs.aerospace.orekit.orbits;

import junit.framework.*;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.orbits.CartesianParameters;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;

public class CartesianParametersTest extends TestCase {

  public CartesianParametersTest(String name) {
    super(name);
  }

  public void testCartesianToCartesian() {
    
    Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
    Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
    double mu = 3.9860047e14;
    
    CartesianParameters p = new CartesianParameters(position, velocity, mu);
    
    assertEquals(p.getPosition(mu).getX(), position.getX(), Utils.epsilonTest * Math.abs(position.getX()));
    assertEquals(p.getPosition(mu).getY(), position.getY(), Utils.epsilonTest * Math.abs(position.getY()));
    assertEquals(p.getPosition(mu).getZ(), position.getZ(), Utils.epsilonTest * Math.abs(position.getZ()));
    assertEquals(p.getVelocity(mu).getX(), velocity.getX(), Utils.epsilonTest * Math.abs(velocity.getX()));
    assertEquals(p.getVelocity(mu).getY(), velocity.getY(), Utils.epsilonTest * Math.abs(velocity.getY()));
    assertEquals(p.getVelocity(mu).getZ(), velocity.getZ(), Utils.epsilonTest * Math.abs(velocity.getZ()));
  }
  
  public void testCartesianToEquinoctial() {
    
    Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
    Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
    double mu = 3.9860047e14;
    
    CartesianParameters p = new CartesianParameters(position, velocity, mu);
    
    assertEquals(42255170.0028257,  p.getA(), Utils.epsilonTest * p.getA());
    assertEquals(0.592732497856475e-03,  p.getEquinoctialEx(), Utils.epsilonE * Math.abs(p.getE()));
    assertEquals(-0.206274396964359e-02, p.getEquinoctialEy(), Utils.epsilonE * Math.abs(p.getE()));
    assertEquals(Math.sqrt(Math.pow(0.592732497856475e-03,2)+Math.pow(-0.206274396964359e-02,2)), p.getE(), Utils.epsilonAngle * Math.abs(p.getE()));
    assertEquals(Utils.trimAngle(2*Math.asin(Math.sqrt((Math.pow(0.128021863908325e-03,2)+Math.pow(-0.352136186881817e-02,2))/4.)),p.getI()), p.getI(), Utils.epsilonAngle * Math.abs(p.getI()));
    assertEquals(Utils.trimAngle(0.234498139679291e+01,p.getLM()), p.getLM(), Utils.epsilonAngle * Math.abs(p.getLM()));
  }
  
  public void testCartesianToKeplerian(){
    
    Vector3D position = new Vector3D(-26655470.0, 29881667.0,-113657.0);
    Vector3D velocity = new Vector3D(-1125.0,-1122.0,195.0);
    double mu = 3.9860047e14;
    
    CartesianParameters p = new CartesianParameters(position, velocity, mu);
    KeplerianParameters kep = new KeplerianParameters(p, mu);
    
    assertEquals(22979265.3030773,  p.getA(), Utils.epsilonTest  * p.getA());
    assertEquals(0.743502611664700, p.getE(), Utils.epsilonE     * Math.abs(p.getE()));
    assertEquals(0.122182096220906, p.getI(), Utils.epsilonAngle * Math.abs(p.getI()));
    double pa = kep.getPerigeeArgument();
    assertEquals(Utils.trimAngle(3.09909041016672, pa), pa,
                 Utils.epsilonAngle * Math.abs(pa));
    double raan = kep.getRightAscensionOfAscendingNode();
    assertEquals(Utils.trimAngle(2.32231010979999, raan), raan,
                 Utils.epsilonAngle * Math.abs(raan));
    double m = kep.getMeanAnomaly();
    assertEquals(Utils.trimAngle(3.22888977629034, m), m,
                 Utils.epsilonAngle * Math.abs(Math.abs(m)));
  }
  
  public void testPositionVelocityNorms(){
   
    Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
    Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
    double mu = 3.9860047e14;
    
    CartesianParameters p = new CartesianParameters(position, velocity, mu);
    
    double e       = p.getE();
    double v       = new KeplerianParameters(p, mu).getTrueAnomaly();
    double ksi     = 1 + e * Math.cos(v);
    double nu      = e * Math.sin(v);
    double epsilon = Math.sqrt((1 - e) * (1 + e));

    double a  = p.getA();
    double na = Math.sqrt(mu / a);

    // validation of: r = a .(1 - e2) / (1 + e.cos(v))
    assertEquals(a * epsilon * epsilon / ksi,
                 p.getPosition(mu).getNorm(),
                 Utils.epsilonTest * Math.abs(p.getPosition(mu).getNorm()));
    
    // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) ) 
    assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon,
                 p.getVelocity(mu).getNorm(),
                 Utils.epsilonTest * Math.abs(p.getVelocity(mu).getNorm()));

  }
  
  public void testGeometry() {
    
    Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
    Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
    double mu = 3.9860047e14;
    
    Vector3D momentum = Vector3D.crossProduct(position, velocity);
    momentum.normalizeSelf();

    EquinoctialParameters p = new EquinoctialParameters(position, velocity, mu);
    
    double apogeeRadius  = p.getA() * (1 + p.getE());
    double perigeeRadius = p.getA() * (1 - p.getE());
    
    for (double lv = 0; lv <= 2 * Math.PI; lv += 2 * Math.PI/100.) {
      p.setLv(lv);
      position = p.getPosition(mu);
      
      // test if the norm of the position is in the range [perigee radius, apogee radius]
      // Warning: these tests are without absolute value by choice
      assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
      assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));
      // assertTrue(position.getNorm() <= apogeeRadius);
      // assertTrue(position.getNorm() >= perigeeRadius);
      
      position.normalizeSelf();
      velocity = p.getVelocity(mu);
      velocity.normalizeSelf();
      
      // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here
      
      // test of orthogonality between position and momentum
      assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
      // test of orthogonality between velocity and momentum
      assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
    }
  }


  
  public static Test suite() {
    return new TestSuite(CartesianParametersTest.class);
  }
}

