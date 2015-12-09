/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.estimation.measurements;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
//import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Class modeling an Azimuth-Elevation measurement from a ground station.
 * @author Thierry Ceolin
 * @since 7.1
 */
public class Angular extends AbstractMeasurement<Angular> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.estimation.Parameter}
     * name conflict occurs
     */
    public Angular(final GroundStation station, final AbsoluteDate date,
                final double[] angular, final double[] sigma, final double[] baseWeight)
        throws OrekitException {
        super(date, angular, sigma, baseWeight);
        this.station = station;
        addSupportedParameter(station);
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** {@inheritDoc} */
    @Override
    protected Evaluation<Angular> theoreticalEvaluation(final int iteration, final SpacecraftState state)
        throws OrekitException {

        // Station in Topocentric Frame
        final PVCoordinates stationIntopocentric = PVCoordinates.ZERO;

        // station position in satellite inertial frame at signal arrival
        final Transform topo2iner =
                station.getOffsetFrame().getTransformTo(state.getFrame(), getDate());
        final PVCoordinates stationArrival = topo2iner.transformPVCoordinates(PVCoordinates.ZERO);
        //final AngularCoordinates ac_topoInert = topo2iner.getAngular();
        //final Vector3D coordI = ac_topoInert.getRotation().applyTo(stationIntopocentric.getPosition());

        final String NameIner = state.getFrame().getName();
        //System.out.println("Inertial Frame Name : " + NameIner + "\n");
        //System.out.println("Station Arrival : " + stationArrival.getPosition() + "\n");

        // Station dans Planetocentric
        final Transform iner2planeto =
                        state.getFrame().getTransformTo(station.getOffsetFrame().getParent(), getDate());
        final PVCoordinates stationArrival1 = iner2planeto.transformPVCoordinates(stationArrival);
        final String NameTopo = station.getOffsetFrame().getName();
        final AbsoluteDate datecourante = getDate();
        ///System.out.println("Current Date : " + datecourante + "\n");
        //System.out.println("Station Topocentric Name : " + NameTopo + "\n");
        //System.out.println("Station Arrival : " + stationArrival1.getPosition() + "\n");

        final Transform planeto2topo = station.getOffsetFrame().getParent().getTransformTo(station.getOffsetFrame(), getDate());
        final Transform topo2planeto = planeto2topo.getInverse();
        //final PVCoordinates stationArrival2 = planetoToTopo.transformPVCoordinates(PVCoordinates.ZERO);
        final Vector3D stationArrival2 = planeto2topo.transformPosition(stationArrival1.getPosition());
        final String NamePlaneto = station.getOffsetFrame().getParent().getName();
        //System.out.println("Station Planetocentric Name : " + NamePlaneto + "\n");
        //System.out.println("Station Arrival2 : " + stationArrival2 + "\n");

        // take propagation time into account
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)
        final double          tauD         = station.downlinkTimeOfFlight(state, getDate());
        final double          delta        = getDate().durationFrom(state.getDate());
        final double          dt           = delta - tauD;
        final SpacecraftState transitState = state.shiftedBy(dt);
        final Vector3D        transit      = transitState.getPVCoordinates().getPosition();

        // Transformation from planetocentric frame to inertial frame
        final Transform planeto2iner = station.getBaseFrame().getParent().getTransformTo(state.getFrame(), getDate());

        // station (east-north-zenith) frame in planetocentric frame
        final FieldVector3D<DerivativeStructure> eastP   = station.getOffsetDerivatives(6, 3, 4, 5).getEast();
        final FieldVector3D<DerivativeStructure> northP  = station.getOffsetDerivatives(6, 3, 4, 5).getNorth();
        final FieldVector3D<DerivativeStructure> zenithP = station.getOffsetDerivatives(6, 3, 4, 5).getZenith();

        // Station origin in planetocentric frame
        final FieldVector3D<DerivativeStructure> Qpla = station.getOffsetDerivatives(6, 3, 4, 5).getOrigin();

        // Station origin in Inertial frame
        final FieldVector3D<DerivativeStructure> Qiner = planeto2iner.transformVector(Qpla);

        // station (east-north-zenith) frame in satellite inertial frame
        final FieldVector3D<DerivativeStructure> east   = planeto2iner.transformVector(eastP);
        final FieldVector3D<DerivativeStructure> north  = planeto2iner.transformVector(northP);
        final FieldVector3D<DerivativeStructure> zenith = planeto2iner.transformVector(zenithP);

        // satellite vector expressed in satellite inertial frame
        final FieldVector3D<DerivativeStructure> Pi = new FieldVector3D<DerivativeStructure> (new DerivativeStructure(6, 1, 0, transit.getX()),
                                                                                              new DerivativeStructure(6, 1, 1, transit.getY()),
                                                                                              new DerivativeStructure(6, 1, 2, transit.getZ()));
        // satellite vector expressed in topocentric frame
        //final Transform iner2topo = state.getFrame().getTransformTo(station.getOffsetFrame(), getDate());
        //final FieldVector3D<DerivativeStructure> P = iner2topo.transformPosition(Pi);

        //

        // station-satellite vector expressed in planetocentric frame
        //final FieldVector3D<DerivativeStructure> QP = P.subtract(Q);

        // station-satellite vector expressed in Inertial frame
        final FieldVector3D<DerivativeStructure> QP = Pi.subtract(Qiner);

        final DerivativeStructure azimuth   = DerivativeStructure.atan2(QP.dotProduct(east), QP.dotProduct(north));
        final DerivativeStructure elevation = (QP.dotProduct(zenith).divide(QP.getNorm())).asin();
        //System.out.println("Reference azimuth : " + azimuth.getValue() + "\n");
        //System.out.println("Reference elevation : " + elevation.getValue() + "\n");

        // New Way, computation in Planetocentric frame
        // satellite vector expressed in planetocentric frame
        //final FieldVector3D<DerivativeStructure> Ppla   = iner2planeto.transformVector(Pi);
        // Station-satellite vector expressed in planetocentric frame
        //final FieldVector3D<DerivativeStructure> QPpla = Ppla.subtract(Qpla);
//        // Station-satellite vector expressed in planetocentric frame
//        final FieldVector3D<DerivativeStructure> QPtopo = planeto2topo.transformVector(QPpla);
//        final FieldVector3D<DerivativeStructure> northtopo = planeto2topo.transformVector(northP);
//        final FieldVector3D<DerivativeStructure> easttopo = planeto2topo.transformVector(eastP);
//        final FieldVector3D<DerivativeStructure> zenithtopo = planeto2topo.transformVector(zenithP);
//
//        final DerivativeStructure azimuthpla   = DerivativeStructure.atan2(QPpla.dotProduct(east), QPpla.dotProduct(north));
//        final DerivativeStructure elevationpla = (QPpla.dotProduct(zenith).divide(QPpla.getNorm())).asin();
//        System.out.println("azimuth Planeto : " + azimuthpla.getValue() + "\n");
//        System.out.println("elevation Planeto: " + elevationpla.getValue() + "\n");

        // prepare the evaluation
        final Evaluation<Angular> evaluation = new Evaluation<Angular>(this, iteration, transitState);

        // Azimuth - Elevation values
        evaluation.setValue(azimuth.getValue(), elevation.getValue());

        // Computation in Inertial (OK)
        // partial derivatives of Azimuth with respect to state
        final double[] dAzOndP = new double[] {azimuth.getPartialDerivative(1, 0, 0, 0, 0, 0),
                                               azimuth.getPartialDerivative(0, 1, 0, 0, 0, 0),
                                               azimuth.getPartialDerivative(0, 0, 1, 0, 0, 0),
                                               azimuth.getPartialDerivative(1, 0, 0, 0, 0, 0) * dt,
                                               azimuth.getPartialDerivative(0, 1, 0, 0, 0, 0) * dt,
                                               azimuth.getPartialDerivative(0, 0, 1, 0, 0, 0) * dt
        };
        //System.out.println("dAzOndP : " + dAzOndP[0] + "   " + dAzOndP[1] + "   " + dAzOndP[2] + "   " + dAzOndP[3] + "   " + dAzOndP[4] + "   " + dAzOndP[5] + "\n");

        // partial derivatives of Elevation with respect to state
        final double[] dElOndP = new double[] {elevation.getPartialDerivative(1, 0, 0, 0, 0, 0),
                                               elevation.getPartialDerivative(0, 1, 0, 0, 0, 0),
                                               elevation.getPartialDerivative(0, 0, 1, 0, 0, 0),
                                               elevation.getPartialDerivative(1, 0, 0, 0, 0, 0) * dt,
                                               elevation.getPartialDerivative(0, 1, 0, 0, 0, 0) * dt,
                                               elevation.getPartialDerivative(0, 0, 1, 0, 0, 0) * dt
        };
        //System.out.println("dElOndP : " + dElOndP[0] + "   " + dElOndP[1] + "   " + dElOndP[2] + "   " + dElOndP[3] + "   " + dElOndP[4] + "   " + dElOndP[5] + "\n");

//        // Computation in Topocentric
////        // Try 1
//        final AngularCoordinates ac = planeto2iner.getAngular();
////        final AngularCoordinates ac = iner2planeto.getAngular();
//        final AngularCoordinates ac1 = topo2planeto.getAngular();
//        final AngularCoordinates ac2 = topo2iner.getAngular();
//////        final Transform InertoTopo = topoToInert.getInverse();
//////       final AngularCoordinates ac3 = iner2topo.getAngular();
//////
//        double[][] matJ = new double[3][3];
//        //final Transform topo2planeto = planeto2topo.getInverse();
//        topo2planeto.getJacobian(CartesianDerivativesFilter.USE_P, matJ);
////        final Transform Topo2planeto = planeto2topo.getInverse();
//        //planeto2topo.getJacobian(CartesianDerivativesFilter.USE_P, matJ);
////        //InerToPlaneto.getJacobian(CartesianDerivativesFilter.USE_P, matJ);
//        final RealMatrix matrix = new Array2DRowRealMatrix(matJ, false);
//
//        final Vector3D tto  = new Vector3D (azimuthtopo.getPartialDerivative(1, 0, 0, 0, 0, 0),
//                                            azimuthtopo.getPartialDerivative(0, 1, 0, 0, 0, 0),
//                                            azimuthtopo.getPartialDerivative(0, 0, 1, 0, 0, 0));
//
//        final double[] dAzOndPtmp2 = matrix.operate(tto.toArray());
//
//        planeto2iner.getJacobian(CartesianDerivativesFilter.USE_P, matJ);
//        final RealMatrix matrix2 = new Array2DRowRealMatrix(matJ, false);
//        final double[] prod1 = matrix.operate(dAzOndPtmp2);
//
//
//////        final double[] ttoo  = new double[] {
//////                                             azimuthtopo.getPartialDerivative(1, 0, 0, 0, 0, 0),
//////                                             azimuthtopo.getPartialDerivative(0, 1, 0, 0, 0, 0),
//////                                             azimuthtopo.getPartialDerivative(0, 0, 1, 0, 0, 0) };
//////        final Vector3D coordP = stationArrival1.getPosition();
//////        System.out.println("Station dans Planeto : " + coordP + "\n");
//////        final Vector3D coordI = ac.getRotation().applyTo(stationArrival1.getPosition());
//////        System.out.println("Station dans Inertiel : " + coordI + "\n");
////
////        final double[] dAzOndPtmp2 = matrix.operate(tto.toArray());
////        final Vector3D dAzOndPtmp1 = new Vector3D(dAzOndPtmp2[0], dAzOndPtmp2[1], dAzOndPtmp2[2]);
////
//        //final Vector3D dAzOndPtmp = ac2.getRotation().applyTo(tto);
//        //final Vector3D dAzOndPtmp = planeto2iner.transformVector(dAzOndPtmp1);
//
////        final double[] prod1 = new double[] {
////                                              dAzOndPtmp.getX(),
////                                              dAzOndPtmp.getY(),
////                                              dAzOndPtmp.getZ()
////       };
////
////        //final double[] dAzOndPtmp = matrix.operate(tto.toArray());
//////        final AngularCoordinates ac5 = TopoToPlaneto.getAngular();
//////        final Vector3D dAzOndPtmp1 = ac5.getRotation().applyTo(tto);
//////        final Vector3D dAzOndPtmp = ac.getRotation().applyTo(dAzOndPtmp1);
//////        final double[] prod1 = dAzOndPtmp.toArray();
//////
//////        final Vector3D dAzOndPtmp1 = new Vector3D(dAzOndPtmp[0], dAzOndPtmp[1], dAzOndPtmp[2]);
//////        final Vector3D prod11 = ac.getRotation().applyTo(dAzOndPtmp1);
//////        final double[] prod1 = new double[] {
//////                                              prod11.getX(),
//////                                              prod11.getY(),
//////                                              prod11.getZ() };
////
////        //final double[] prod1 = matrix.operate(tto.toArray());
////        //final double[] prod1 = dAzOndPtmp.toArray();
//////        final double[] dAzOndPtmp = matrix.operate(ttoo);
//////        final Vector3D dAzOndPtmp1 = new Vector3D(dAzOndPtmp[0], dAzOndPtmp[1], dAzOndPtmp[2]);
//////        final Vector3D prod1 = ac.getRotation().applyTo(dAzOndPtmp1);
//////        final double[] prod11 = new double[] {
//////prod1.getX(),
//////prod1.getY(),
//////prod1.getZ()
//////        };
//
////        final double[] dAzOndP1 = new double[] {prod1[0],
////                                                prod1[1],
////                                                prod1[2],
////                                                prod1[0] * dt,
////                                                prod1[1] * dt,
////                                                prod1[2] * dt
////        };
////        System.out.println("dAzOndP1 : " + dAzOndP1[0] + "   " + dAzOndP1[1] + "   " + dAzOndP1[2] + "   " + dAzOndP1[3] + "   " + dAzOndP1[4] + "   " + dAzOndP1[5] + "\n");
//
////
//////      prod1[0],
//////      prod1[1],
//////      prod1[2],
//////      prod1[0] * dt,
//////      prod1[1] * dt,
//////      prod1[2] * dt};
////        // partial derivatives of Azimuth with respect to state
//////        final double[] dAzOndP = new double[] {
//////                                               prod1[0],
//////                                               prod1[1],
//////                                               prod1[2],
//////                                               prod1[0] * dt,
//////                                               prod1[1] * dt,
//////                                               prod1[2] * dt};
//////
////
////
//////        final AngularCoordinates ac1 = topoToInert.getAngular();
//////        final Vector3D tto1  = new Vector3D (azimuthtopo.getPartialDerivative(1, 0, 0, 0, 0, 0),
//////                                            azimuthtopo.getPartialDerivative(0, 1, 0, 0, 0, 0),
//////                                            azimuthtopo.getPartialDerivative(0, 0, 1, 0, 0, 0));
//////
//////        final Vector3D dAzOndPtmp1 = ac1.getRotation().applyTo(tto1);
//////        final double[] prod11 = dAzOndPtmp1.toArray();
//////
//////        // partial derivatives of Azimuth with respect to state
//////        final double[] dAzOndP1 = new double[] {
//////                                               prod11[0],
//////                                               prod11[1],
//////                                               prod11[2],
//////                                               prod11[0] * dt,
//////                                               prod11[1] * dt,
//////                                               prod11[2] * dt
//////            azimuth.getPartialDerivative(1, 0, 0, 0, 0, 0),
//////            azimuth.getPartialDerivative(0, 1, 0, 0, 0, 0),
//////            azimuth.getPartialDerivative(0, 0, 1, 0, 0, 0),
//////            azimuth.multiply(dt).getPartialDerivative(1, 0, 0, 0, 0, 0),
//////            azimuth.multiply(dt).getPartialDerivative(0, 1, 0, 0, 0, 0),
//////            azimuth.multiply(dt).getPartialDerivative(0, 0, 1, 0, 0, 0)
//////        };
////        System.out.println("dAzOndP1 : " + dAzOndP1[0] + "   " + dAzOndP1[1] + "   " + dAzOndP1[2] + "   " + dAzOndP1[3] + "   " + dAzOndP1[4] + "   " + dAzOndP1[5] + "\n");
////
////     // Try 2
////        final Vector3D ttu  = new Vector3D (elevationtopo.getPartialDerivative(1, 0, 0, 0, 0, 0),
////                                            elevationtopo.getPartialDerivative(0, 1, 0, 0, 0, 0),
////                                            elevationtopo.getPartialDerivative(0, 0, 1, 0, 0, 0));
////
////        final Vector3D dElOndPtmp = ac2.getRotation().applyTo(ttu);
////        final double[] prod2 = new double[] {
////                                             dElOndPtmp.getX(),
////                                             dElOndPtmp.getY(),
////                                             dElOndPtmp.getZ()};
////        //final double[] prod2 = matrix.operate(dElOndPtmp.toArray());
////        //final double[] prod2 = dElOndPtmp.toArray();
////
////        // New way
//////        final Vector3D ttu1  = new Vector3D (elevationtopo.getPartialDerivative(1, 0, 0, 0, 0, 0),
//////                                             elevationtopo.getPartialDerivative(0, 1, 0, 0, 0, 0),
//////                                             elevationtopo.getPartialDerivative(0, 0, 1, 0, 0, 0));
//////
//////        final Vector3D dElOndPtmp1 = ac1.getRotation().applyTo(ttu1);
//////        //final double[] prod2 = matrix.operate(dElOndPtmp.toArray());
//////        final double[] prod22 = dElOndPtmp1.toArray();
//////
//////
////        // partial derivatives of Elevation with respect to state
////        final double[] dElOndP1 = new double[] {
////                                               prod2[0],
////                                               prod2[1],
////                                               prod2[2],
////                                               prod2[0] * dt,
////                                               prod2[1] * dt,
////                                               prod2[2] * dt
//////            elevation.getPartialDerivative(1, 0, 0, 0, 0, 0),
//////            elevation.getPartialDerivative(0, 1, 0, 0, 0, 0),
//////            elevation.getPartialDerivative(0, 0, 1, 0, 0, 0),
//////            elevation.multiply(dt).getPartialDerivative(1, 0, 0, 0, 0, 0),
//////            elevation.multiply(dt).getPartialDerivative(0, 1, 0, 0, 0, 0),
//////            elevation.multiply(dt).getPartialDerivative(0, 0, 1, 0, 0, 0)
////        };
////        System.out.println("dElOndP : " + dElOndP[0] + "   " + dElOndP[1] + "   " + dElOndP[2] + "   " + dElOndP[3] + "   " + dElOndP[4] + "   " + dElOndP[5] + "\n");
//////        final double[] dElOndP1 = new double[] {
//////                                               prod22[0],
//////                                               prod22[1],
//////                                               prod22[2],
//////                                               prod22[0] * dt,
//////                                               prod22[1] * dt,
//////                                               prod22[2] * dt
////////            elevation.multiply(dt).getPartialDerivative(0, 0, 1, 0, 0, 0)
//////        };
////        System.out.println("dElOndP1 : " + dElOndP1[0] + "   " + dElOndP1[1] + "   " + dElOndP1[2] + "   " + dElOndP1[3] + "   " + dElOndP1[4] + "   " + dElOndP1[5] + "\n");

        evaluation.setStateDerivatives(dAzOndP, dElOndP);

        if (station.isEstimated()) {

            // New way, computation in Topocentric frame
            final FieldVector3D<DerivativeStructure> northtopo = planeto2topo.transformVector(northP);
            final FieldVector3D<DerivativeStructure> easttopo = planeto2topo.transformVector(eastP);
            final FieldVector3D<DerivativeStructure> zenithtopo = planeto2topo.transformVector(zenithP);

            // station-satellite vector expressed in topocentric frame
            final Transform iner2topo = state.getFrame().getTransformTo(station.getOffsetFrame(), getDate());
            final FieldVector3D<DerivativeStructure> QPtopo = iner2topo.transformVector(QP);

            final DerivativeStructure azimuthtopo   = DerivativeStructure.atan2(QPtopo.dotProduct(easttopo), QPtopo.dotProduct(northtopo));
            final DerivativeStructure elevationtopo = (QPtopo.dotProduct(zenithtopo).divide(QPtopo.getNorm())).asin();
            //System.out.println("azimuth from topo : " + azimuthtopo.getValue() + "\n");
            //System.out.println("elevation from topo : " + elevationtopo.getValue() + "\n");

            final DerivativeStructure sinsite = QPtopo.dotProduct(zenithtopo).divide(QPtopo.getNorm());
            final double coef0 = sinsite.getValue();
            final double coef1 = 1.0 / FastMath.sqrt(1.0 - coef0 * coef0);

            final double[] stasat = new double[] {
                                                   QPtopo.getX().getValue(),
                                                   QPtopo.getY().getValue(),
                                                   QPtopo.getZ().getValue()
            };

            final double[] zen = new double[] {
                                                   zenithtopo.getX().getValue(),
                                                   zenithtopo.getY().getValue(),
                                                   zenithtopo.getZ().getValue()
            };

            final double[] dZendQ = new double[] {
                                                    zenithP.getX().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                                    zenithP.getY().getPartialDerivative(0, 0, 0, 1, 0, 0),
                                                    zenithP.getZ().getPartialDerivative(0, 0, 0, 1, 0, 0)
            };

            final double coeff2 = (zen[0] + stasat[0] * zen[0] + stasat[1] * zen[1] + stasat[2] * zen[2]) * QPtopo.getNorm().getValue();
            final double coeff3 = (stasat[0] * dZendQ[0] + stasat[1] * dZendQ[1] + stasat[2] * dZendQ[2]) * stasat[0] / QPtopo.getNorm().getValue();

            final double dSiteOnDXsta = coef1 * (coeff2 - coeff3) / ( QPtopo.getNorm().getValue() * QPtopo.getNorm().getValue() );
            //System.out.println("dSiteOnDXsta : " + dSiteOnDXsta + "\n");

            //final AngularCoordinates ac = topoToInert.getAngular().revert();
            //final AngularCoordinates ac2 = topoToInert.getAngular();
            //final Vector3D omega        = ac.getRotationRate();
            //final Vector3D V1 = new Vector3D(omega.getX(), omega.getY(), omega.getZ());
            //final Vector3D V2 = new Vector3D(OQ.getX().getValue(), OQ.getY().getValue(), OQ.getZ().getValue());
            //final Vector3D V3 = V1.crossProduct(V2);

//            final double dx = dt * omega.getX();
//            final double dy = dt * omega.getY();
//            final double dz = dt * omega.getZ();

            // partial derivatives with respect to parameters
            // partial derivatives of Azimuth with respect to parameters in topocentric frame
            final Vector3D dAzOndQ = new Vector3D(azimuthtopo.getPartialDerivative(0, 0, 0, 1, 0, 0),
                                                  azimuthtopo.getPartialDerivative(0, 0, 0, 0, 1, 0),
                                                  azimuthtopo.getPartialDerivative(0, 0, 0, 0, 0, 1));

            // partial derivatives of Elevation with respect to parameters in topocentric frame
            final Vector3D dElOndQ = new Vector3D(elevationtopo.getPartialDerivative(0, 0, 0, 1, 1, 0),
                                                  elevationtopo.getPartialDerivative(0, 0, 0, 0, 1, 0),
                                                  elevationtopo.getPartialDerivative(0, 0, 0, 0, 0, 1));
            //final Vector3D dElOndQI = new Vector3D(QP.getX().multiply(zenithX.getPartialDerivative(0,0,0,1,0,0)) + QP.getY().multiply(zenithY.getPartialDerivative(0,0,0,1,0,0)) + QP.getZ().multiply(zenithZ.getPartialDerivative(0,0,0,1,0,0)) - ,);

            // convert to topocentric frame, as the station position
            // offset parameter is expressed in this frame
            //final AngularCoordinates ac = topoToInert.getAngular().revert();

            //final Vector3D dAzOndQT = ac.getRotation().applyTo(dAzOndQI);
            //final Vector3D dElOndQT = ac.getRotation().applyTo(dElOndQI);
            //final Vector3D dAzOndQT2 = ac2.getRotation().applyTo(dAzOndQI);
            //final Vector3D dElOndQT2 = ac2.getRotation().applyTo(dElOndQI);
            //final Vector3D dAzOndQT3 = topoToInert.transformVector(dAzOndQI);
            //final Vector3D dElOndQT3 = topoToInert.transformVector(dElOndQT);
            //evaluation.setParameterDerivatives(station.getName(), dAzOndQT.toArray(), dElOndQT.toArray());
            evaluation.setParameterDerivatives(station.getName(), dAzOndQ.toArray(), dElOndQ.toArray());
            //evaluation.setParameterDerivatives(station.getName(), dAzOndQT3.toArray(), dElOndQT3.toArray());
        }

        return evaluation;
    }

}
