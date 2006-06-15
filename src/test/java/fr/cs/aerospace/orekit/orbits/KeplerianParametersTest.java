package fr.cs.aerospace.orekit.orbits;

import junit.framework.*;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;

// $Id$
public class KeplerianParametersTest extends TestCase {

  public KeplerianParametersTest(String name) {
    super(name);
  }

  public void testKeplerianToKeplerian() {
    
    // elliptic orbit
    KeplerianParameters kep =
      new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                              0.048363, KeplerianParameters.MEAN_ANOMALY);
    double mu = 3.9860047e14;
    
    Vector3D pos = kep.getPosition(mu);
    Vector3D vit = kep.getVelocity(mu);
    
    KeplerianParameters param = new KeplerianParameters(pos,vit,mu);
    assertEquals(param.getA(), kep.getA(), Utils.epsilonTest * kep.getA());
    assertEquals(param.getE(), kep.getE(), Utils.epsilonE * Math.abs(kep.getE()));
    assertEquals(Utils.trimAngle(param.getI(), kep.getI()), kep.getI(), Utils.epsilonAngle * Math.abs(kep.getI()));
    assertEquals(Utils.trimAngle(param.getPerigeeArgument(), kep.getPerigeeArgument()), kep.getPerigeeArgument(), Utils.epsilonAngle * Math.abs(kep.getPerigeeArgument()));
    assertEquals(Utils.trimAngle(param.getRightAscensionOfAscendingNode(), kep.getRightAscensionOfAscendingNode()), kep.getRightAscensionOfAscendingNode(), Utils.epsilonAngle * Math.abs(kep.getRightAscensionOfAscendingNode()));
    assertEquals(Utils.trimAngle(param.getMeanAnomaly(), kep.getMeanAnomaly()), kep.getMeanAnomaly(), Utils.epsilonAngle * Math.abs(kep.getMeanAnomaly()));
    
    // circular orbit
    KeplerianParameters kepCir =
      new KeplerianParameters(24464560.0, 0.0, 0.122138, 3.10686, 1.00681,
                              0.048363, KeplerianParameters.MEAN_ANOMALY);
    
    Vector3D posCir = kepCir.getPosition(mu);
    Vector3D vitCir = kepCir.getVelocity(mu);
    
    KeplerianParameters paramCir = new KeplerianParameters(posCir,vitCir,mu);
    assertEquals(paramCir.getA(), kepCir.getA(), Utils.epsilonTest * kepCir.getA());
    assertEquals(paramCir.getE(), kepCir.getE(), Utils.epsilonE * Math.max(1.,Math.abs(kepCir.getE())));
    assertEquals(Utils.trimAngle(paramCir.getI(), kepCir.getI()), kepCir.getI(), Utils.epsilonAngle * Math.abs(kepCir.getI()));
    assertEquals(Utils.trimAngle(paramCir.getLM(), kepCir.getLM()), kepCir.getLM(), Utils.epsilonAngle * Math.abs(kepCir.getLM()));
    assertEquals(Utils.trimAngle(paramCir.getLE(), kepCir.getLE()), kepCir.getLE(), Utils.epsilonAngle * Math.abs(kepCir.getLE()));
    assertEquals(Utils.trimAngle(paramCir.getLv(), kepCir.getLv()), kepCir.getLv(), Utils.epsilonAngle * Math.abs(kepCir.getLv()));
    
  }
    
  public void testKeplerianToCartesian() {
    
    KeplerianParameters kep =
      new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                              0.048363, KeplerianParameters.MEAN_ANOMALY);
    double mu = 3.9860047e14;
    
    Vector3D pos = kep.getPosition(mu);
    Vector3D vit = kep.getVelocity(mu);
    assertEquals(-0.107622532467967e+07, pos.getX(), Utils.epsilonTest * Math.abs(pos.getX()));
    assertEquals(-0.676589636432773e+07, pos.getY(), Utils.epsilonTest * Math.abs(pos.getY()));
    assertEquals(-0.332308783350379e+06, pos.getZ(), Utils.epsilonTest * Math.abs(pos.getZ()));
    
    assertEquals( 0.935685775154103e+04, vit.getX(), Utils.epsilonTest * Math.abs(vit.getX()));
    assertEquals(-0.331234775037644e+04, vit.getY(), Utils.epsilonTest * Math.abs(vit.getY()));
    assertEquals(-0.118801577532701e+04, vit.getZ(), Utils.epsilonTest * Math.abs(vit.getZ()));
  }
  
  public void testKeplerianToEquinoctial() {
    
    KeplerianParameters kep =
      new KeplerianParameters(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                              0.048363, KeplerianParameters.MEAN_ANOMALY);
    
    assertEquals(24464560.0, kep.getA(), Utils.epsilonTest * kep.getA());
    assertEquals(-0.412036802887626, kep.getEquinoctialEx(), Utils.epsilonE * Math.abs(kep.getE()));
    assertEquals(-0.603931190671706, kep.getEquinoctialEy(), Utils.epsilonE * Math.abs(kep.getE()));
    assertEquals(Utils.trimAngle(2*Math.asin(Math.sqrt((Math.pow(0.652494417368829e-01,2)+Math.pow(0.103158450084864,2))/4.)),kep.getI()), kep.getI(), Utils.epsilonAngle * Math.abs(kep.getI()));
    assertEquals(Utils.trimAngle(0.416203300000000e+01,kep.getLM()), kep.getLM(),Utils.epsilonAngle * Math.abs(kep.getLM()));

}

  public void testAnomaly() {
  
    Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
    Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
    double mu = 3.9860047e14;
    
    KeplerianParameters p = new KeplerianParameters(position, velocity, mu);

    // elliptic orbit
    double e = p.getE();
    double eRatio = Math.sqrt((1 - e) / (1 + e));
 
    double v = 1.1;
    // formulations for elliptic case 
    double E = 2 * Math.atan(eRatio * Math.tan(v / 2));
    double M = E - e * Math.sin(E);

    p.setTrueAnomaly(v);
    assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
    assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
    assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));
    p.setTrueAnomaly(0);

    p.setEccentricAnomaly(E);
    assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
    assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
    assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));
    p.setTrueAnomaly(0);

    p.setMeanAnomaly(M);
    assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
    assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
    assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));

    // circular orbit
    p.setE(0);
    
    E = v;
    M = E;
    
    p.setTrueAnomaly(v);
    assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
    assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
    assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));
    p.setTrueAnomaly(0);

    p.setEccentricAnomaly(E);
    assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
    assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
    assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));
    p.setTrueAnomaly(0);

    p.setMeanAnomaly(M);
    assertEquals(p.getTrueAnomaly(), v, Utils.epsilonAngle * Math.abs(v));
    assertEquals(p.getEccentricAnomaly(), E, Utils.epsilonAngle * Math.abs(E));
    assertEquals(p.getMeanAnomaly(), M, Utils.epsilonAngle * Math.abs(M));

  }
  
  public void testPositionVelocityNorms() {
    double mu = 3.9860047e14;
 
    // elliptic and non equatorial orbit 
    KeplerianParameters p =
      new KeplerianParameters(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                              0.67, KeplerianParameters.TRUE_ANOMALY);
  
    double e       = p.getE();
    double v       = p.getTrueAnomaly();
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

    
    //  circular and equatorial orbit
    KeplerianParameters pCirEqua =
      new KeplerianParameters(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                              0.67, KeplerianParameters.TRUE_ANOMALY);

    e       = pCirEqua.getE();
    v       = pCirEqua.getTrueAnomaly();
    ksi     = 1 + e * Math.cos(v);
    nu      = e * Math.sin(v);
    epsilon = Math.sqrt((1 - e) * (1 + e));

    a  = pCirEqua.getA();
    na = Math.sqrt(mu / a);

    // validation of: r = a .(1 - e2) / (1 + e.cos(v))
    assertEquals(a * epsilon * epsilon / ksi,
             pCirEqua.getPosition(mu).getNorm(),
             Utils.epsilonTest * Math.abs(pCirEqua.getPosition(mu).getNorm()));
    
    // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) ) 
    assertEquals(na * Math.sqrt(ksi * ksi + nu * nu) / epsilon,
             pCirEqua.getVelocity(mu).getNorm(),
             Utils.epsilonTest * Math.abs(pCirEqua.getVelocity(mu).getNorm()));
  }
  
  public void testGeometry() {
    double mu = 3.9860047e14;
    
    // elliptic and non equatorial orbit 
    KeplerianParameters p =
      new KeplerianParameters(24464560.0, 0.7311, 2.1, 3.10686, 1.00681,
                              0.67, KeplerianParameters.TRUE_ANOMALY);
    
    Vector3D position = p.getPosition(mu);
    Vector3D velocity = p.getVelocity(mu);
    Vector3D momentum = Vector3D.crossProduct(position,velocity);
    momentum.normalizeSelf();
    
    double apogeeRadius  = p.getA() * (1 + p.getE());
    double perigeeRadius = p.getA() * (1 - p.getE());
    
    for (double lv = 0; lv <= 2 * Math.PI; lv += 2 * Math.PI/100.) {
      p.setTrueAnomaly(lv);
      position = p.getPosition(mu);
      
      // test if the norm of the position is in the range [perigee radius, apogee radius]
      assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
      assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));
      
      position.normalizeSelf();
      velocity = p.getVelocity(mu);
      velocity.normalizeSelf();
      
      // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here
      
      // test of orthogonality between position and momentum
      assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
      // test of orthogonality between velocity and momentum
      assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

    }
    
    // apsides
    p.setTrueAnomaly(0);
    assertEquals(p.getPosition(mu).getNorm(), perigeeRadius, perigeeRadius * Utils.epsilonTest);
    
    p.setTrueAnomaly(Math.PI);
    assertEquals(p.getPosition(mu).getNorm(), apogeeRadius, apogeeRadius * Utils.epsilonTest);
    
    // nodes
    // descending node
    p.setTrueAnomaly(Math.PI - p.getPerigeeArgument());
    assertTrue(Math.abs(p.getPosition(mu).getZ()) < p.getPosition(mu).getNorm() * Utils.epsilonTest);
    assertTrue(p.getVelocity(mu).getZ() < 0);
    
    // ascending node
    p.setTrueAnomaly(2.0 * Math.PI - p.getPerigeeArgument());
    assertTrue(Math.abs(p.getPosition(mu).getZ()) < p.getPosition(mu).getNorm() * Utils.epsilonTest);
    assertTrue(p.getVelocity(mu).getZ() > 0);
 
    
    //  circular and equatorial orbit
    KeplerianParameters pCirEqua =
      new KeplerianParameters(24464560.0, 0.1e-10, 0.1e-8, 3.10686, 1.00681,
                              0.67, KeplerianParameters.TRUE_ANOMALY);
   
    position = pCirEqua.getPosition(mu);
    velocity = pCirEqua.getVelocity(mu);
    momentum = Vector3D.crossProduct(position,velocity);
    momentum.normalizeSelf();
    
    apogeeRadius  = pCirEqua.getA() * (1 + pCirEqua.getE());
    perigeeRadius = pCirEqua.getA() * (1 - pCirEqua.getE());
    // test if apogee equals perigee
    assertEquals(perigeeRadius, apogeeRadius, 1.e+4 * Utils.epsilonTest * apogeeRadius);
 
    for (double lv = 0; lv <= 2 * Math.PI; lv += 2 * Math.PI/100.) {
      pCirEqua.setTrueAnomaly(lv);
      position = pCirEqua.getPosition(mu);
      
      // test if the norm pf the position is in the range [perigee radius, apogee radius]
      // Warning: these tests are without absolute value by choice
      assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
      assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));
      
      position.normalizeSelf();
      velocity = pCirEqua.getVelocity(mu);
      velocity.normalizeSelf();
      
      // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here
      
      // test of orthogonality between position and momentum
      assertTrue(Math.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
      // test of orthogonality between velocity and momentum
      assertTrue(Math.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);

    }
  }

  public void testSymmetry() {
    
    // elliptic and non equatorail orbit
    Vector3D position = new Vector3D(-4947831., -3765382., -3708221.);
    Vector3D velocity = new Vector3D(-2079., 5291., -7842.);
    double mu = 3.9860047e14;
    
    KeplerianParameters p = new KeplerianParameters(position, velocity, mu);
    Vector3D positionOffset = new Vector3D(p.getPosition(mu));
    Vector3D velocityOffset = new Vector3D(p.getVelocity(mu));
    
    positionOffset.subtractFromSelf(position);
    velocityOffset.subtractFromSelf(velocity);

    assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
    assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);
   
    // circular and equatorial orbit
    position = new Vector3D(1742382., -2.440243e7, -0.014517);
    velocity = new Vector3D(4026.2, 287.479, -3.e-6);
   
    
    p = new KeplerianParameters(position, velocity, mu);
    positionOffset = new Vector3D(p.getPosition(mu));
    velocityOffset = new Vector3D(p.getVelocity(mu));
    
    positionOffset.subtractFromSelf(position);
    velocityOffset.subtractFromSelf(velocity);

    assertTrue(positionOffset.getNorm() < Utils.epsilonTest);
    assertTrue(velocityOffset.getNorm() < Utils.epsilonTest);   
 
  }
  
  public static Test suite() {
    return new TestSuite(KeplerianParametersTest.class);
  }
}
