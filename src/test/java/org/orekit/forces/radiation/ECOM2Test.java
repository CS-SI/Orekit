package org.orekit.forces.radiation;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;

public class ECOM2Test {
    
    @Before
    public void setUp() throws OrekitException {
        //Utils.setDataRoot("regular-data:potential/shm-format");
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
    }
    private NumericalPropagator setUpPropagator(Orbit orbit, double dP,
                                                OrbitType orbitType, PositionAngle angleType,
                                                ForceModel... models)
        {

        final double minStep = 0.001;
        final double maxStep = 1000;

        double[][] tol = NumericalPropagator.tolerances(dP, orbit, orbitType);
        NumericalPropagator propagator =
            new NumericalPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]));
        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(angleType);
        for (ForceModel model : models) {
            propagator.addForceModel(model);
        }
        return propagator;
    }
    
    private SpacecraftState shiftState(SpacecraftState state, OrbitType orbitType, PositionAngle angleType,
                                       double delta, int column) {

        double[][] array = stateToArray(state, orbitType, angleType, true);
        array[0][column] += delta;

        return arrayToState(array, orbitType, angleType, state.getFrame(), state.getDate(),
                            state.getMu(), state.getAttitude());

    }
    
    private SpacecraftState arrayToState(double[][] array, OrbitType orbitType, PositionAngle angleType,
                                         Frame frame, AbsoluteDate date, double mu,
                                         Attitude attitude) {
        Orbit orbit = orbitType.mapArrayToOrbit(array[0], array[1], angleType, date, mu, frame);
        return (array.length > 6) ?
               new SpacecraftState(orbit, attitude) :
               new SpacecraftState(orbit, attitude, array[0][6]);
    }
    
    private double[][] stateToArray(SpacecraftState state, OrbitType orbitType, PositionAngle angleType,
                                    boolean withMass) {
          double[][] array = new double[2][withMass ? 7 : 6];
          orbitType.mapOrbitToArray(state.getOrbit(), angleType, array[0], array[1]);
          if (withMass) {
              array[0][6] = state.getMass();
          }
          return array;
    }
       
    private void fillJacobianModelColumn(double[][] jacobian, int column,
                                    OrbitType orbitType, PositionAngle angleType, double h,
                                    Vector3D sM4h, Vector3D sM3h,
                                    Vector3D sM2h, Vector3D sM1h,
                                    Vector3D sP1h, Vector3D sP2h,
                                    Vector3D sP3h, Vector3D sP4h) {
        
        jacobian[0][column] = ( -3 * (sP4h.getX() - sM4h.getX()) +
                        32 * (sP3h.getX() - sM3h.getX()) -
                       168 * (sP2h.getX() - sM2h.getX()) +
                       672 * (sP1h.getX() - sM1h.getX())) / (840 * h);
        jacobian[1][column] = ( -3 * (sP4h.getY() - sM4h.getY()) +
                        32 * (sP3h.getY() - sM3h.getY()) -
                       168 * (sP2h.getY() - sM2h.getY()) +
                       672 * (sP1h.getY() - sM1h.getY())) / (840 * h);
        jacobian[2][column] = ( -3 * (sP4h.getZ() - sM4h.getZ()) +
                        32 * (sP3h.getZ() - sM3h.getZ()) -
                       168 * (sP2h.getZ() - sM2h.getZ()) +
                       672 * (sP1h.getZ() - sM1h.getZ())) / (840 * h);
    }
    
    
    @Test
    public void testJacobianModelMatrix() {
        final DSFactory factory = new DSFactory(6, 1);
        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        
        //Values for the orbit definition
        final DerivativeStructure x         = factory.variable(0, -2747600.0);
        final DerivativeStructure y         = factory.variable(1, 22572100.0);
        final DerivativeStructure z         = factory.variable(2, 13522760.0);
        final DerivativeStructure xDot      = factory.variable(3,  -2729.5);
        final DerivativeStructure yDot      = factory.variable(4, 1142.7);
        final DerivativeStructure zDot      = factory.variable(5, -2523.9);
        
        //Build Orbit and spacecraft state
        final Field<DerivativeStructure>                field   = x.getField();
        final DerivativeStructure                       one     = field.getOne();
        final FieldVector3D<DerivativeStructure>        pos     = new FieldVector3D<DerivativeStructure>(x, y, z);
        final FieldVector3D<DerivativeStructure>        vel     = new FieldVector3D<DerivativeStructure>(xDot, yDot, zDot);
        final FieldPVCoordinates<DerivativeStructure>   dsPV    = new FieldPVCoordinates<DerivativeStructure>(pos, vel);
        final FieldAbsoluteDate<DerivativeStructure>    dsDate  = new FieldAbsoluteDate<>(field, new AbsoluteDate(2016, 2, 13, 2, 31, 30, TimeScalesFactory.getUTC()));
        final DerivativeStructure                       mu      = one.multiply(Constants.EGM96_EARTH_MU);                             
        final FieldOrbit<DerivativeStructure>           dsOrbit = new FieldCartesianOrbit<DerivativeStructure>(dsPV, FramesFactory.getEME2000(), dsDate, mu);
        final FieldSpacecraftState<DerivativeStructure> dsState = new FieldSpacecraftState<DerivativeStructure>(dsOrbit);
        
        //Build the forceModel
        final ECOM2  forceModel = new ECOM2(2, 2, 1e-7, -1e-4, 1e-4, CelestialBodyFactory.getSun(), Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
       
        //Compute acceleration with state derivatives
        final FieldVector3D<DerivativeStructure> acc = forceModel.acceleration(dsState, forceModel.getParameters(field));
        final double[] accX = acc.getX().getAllDerivatives();
        final double[] accY = acc.getY().getAllDerivatives();
        final double[] accZ = acc.getZ().getAllDerivatives();
        
        //Build the non-field element from the field element
        final Orbit           orbit     = dsOrbit.toOrbit();
        final SpacecraftState state     = dsState.toSpacecraftState(); 
        final double[][] refDeriv = new double[3][6];
        final OrbitType     orbitType = orbit.getType();
        final PositionAngle angleType = PositionAngle.MEAN;
        double dP = 0.001;
        double[] steps = NumericalPropagator.tolerances(1000000 * dP, orbit, orbitType)[0];
        AbstractIntegratedPropagator propagator = setUpPropagator(orbit, dP, orbitType, angleType, gravityField, forceModel);
        
        //Compute derivatives with finite-difference method
        for(int i = 0; i < 6; i++) {
            propagator.resetInitialState(shiftState(state, orbitType, angleType, -4 * steps[i], i));
            SpacecraftState sM4h = propagator.propagate(state.getDate());
            Vector3D accM4 = forceModel.acceleration(sM4h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, -3 * steps[i], i));
            SpacecraftState sM3h = propagator.propagate(state.getDate());
            Vector3D accM3 = forceModel.acceleration(sM3h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, -2 * steps[i], i));
            SpacecraftState sM2h = propagator.propagate(state.getDate());
            Vector3D accM2 = forceModel.acceleration(sM2h, forceModel.getParameters()); 
 
            propagator.resetInitialState(shiftState(state, orbitType, angleType, -1 * steps[i] , i));
            SpacecraftState sM1h = propagator.propagate(state.getDate());
            Vector3D accM1 = forceModel.acceleration(sM1h, forceModel.getParameters()); 
           
            propagator.resetInitialState(shiftState(state, orbitType, angleType, 1 * steps[i], i));
            SpacecraftState  sP1h = propagator.propagate(state.getDate());
            Vector3D accP1 = forceModel.acceleration(sP1h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, 2 * steps[i], i));
            SpacecraftState sP2h = propagator.propagate(state.getDate());
            Vector3D accP2 = forceModel.acceleration(sP2h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, 3 * steps[i], i));
            SpacecraftState sP3h = propagator.propagate(state.getDate());
            Vector3D accP3 = forceModel.acceleration(sP3h, forceModel.getParameters()); 
            
            propagator.resetInitialState(shiftState(state, orbitType, angleType, 4 * steps[i], i));
            SpacecraftState sP4h = propagator.propagate(state.getDate());
            Vector3D accP4 = forceModel.acceleration(sP4h, forceModel.getParameters()); 
            fillJacobianModelColumn(refDeriv, i, orbitType, angleType, steps[i],
                               accM4, accM3, accM2, accM1,
                               accP1, accP2, accP3, accP4);
        }
        
        //Compare state derivatives with finite-difference ones.
        for (int i = 0; i < 6; i++) {
            final double errorX = (accX[i + 1] - refDeriv[0][i]) / refDeriv[0][i];
            Assert.assertEquals(0, errorX, 1e-10);
            final double errorY = (accY[i + 1] - refDeriv[1][i]) / refDeriv[1][i];
            Assert.assertEquals(0, errorY, 1e-10);
            final double errorZ = (accZ[i + 1] - refDeriv[2][i]) / refDeriv[2][i];
            Assert.assertEquals(0, errorZ, 1e-10);
        }
        
    }
    
  
}
