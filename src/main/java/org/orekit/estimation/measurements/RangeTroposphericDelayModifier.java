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

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.models.earth.SaastamoinenModel;
import org.orekit.models.earth.TroposphericDelayModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;

/** Class modifying theoretical range measurement with tropospheric delay.
 * The effect of tropospheric correction on the range is directly computed
 * through the computation of the tropospheric delay.
 *
 *
 * @author Joris Olympio
 * @since 7.1
 */
public class RangeTroposphericDelayModifier implements EvaluationModifier {

    /** Tropospheric delay model. */
    private final TroposphericDelayModel tropoModel;

    /**
     * Constructor.
     * @param model  Tropospheric delay model
     */
    public RangeTroposphericDelayModifier(final TroposphericDelayModel model) {
        tropoModel = model;
    }

    /**
     * Simple Constructor.
     */
    public RangeTroposphericDelayModifier() {
        tropoModel = SaastamoinenModel.getStandardModel();
    }

    /** Get the station height above mean sea level.
     *
     * @param station  ground station (or measuring station)
     * @return the measuring station height above sea level, m
     */
    private double getStationHeightAMSL(final GroundStation station) {
        // FIXME Il faut la hauteur par rapport au geoide WGS84+GUND = EGM2008 par exemple
        final double height = station.getBaseFrame().getPoint().getAltitude();
        return height;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double rangeErrorTroposphericModel(final GroundStation station,
                                                  final SpacecraftState state) throws OrekitException
    {
        //
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation in degrees
        final double elevation =
                station.getBaseFrame().getElevation(position,
                                                    state.getFrame(),
                                                    state.getDate());

        // only consider measures above the horizon
        if (elevation > 0) {
            // altitude AMSL in meters
            final double height = getStationHeightAMSL(station);

            // delay in meters
            final double delay = tropoModel.calculatePathDelay(elevation, height);

            return delay;
        }

        return 0;
    }

    /** Compute the Jacobian of the delay term wrt state.
     *
     * @param station station
     * @param state spacecraft state
     *
     * @return jacobian of the delay wrt state
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeErrorJacobianState(final GroundStation station,
                                          final SpacecraftState state) throws OrekitException
    {
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation in degrees
        final double elevation =
                        station.getBaseFrame().getElevation(position,
                                                            state.getFrame(),
                                                            state.getDate());

        // altitude AMSL in meters
        final double height = getStationHeightAMSL(station);

        // delay in meters
        final double delay = tropoModel.calculatePathDelay(elevation, height);

        // compute the derivative of the tropospheric delay model wrt elevation.
        // We are not interested by the derivative wrt height since the height is independant from the
        // spacecraft state.
        final double h = 1e-6; // finite difference step
        final double stepE = h * FastMath.max(1., elevation);
        final double delayE = tropoModel.calculatePathDelay(elevation + stepE, height);
        final double dDelaydElevation = (delayE - delay) / stepE;

        //
        //final double delayH = tropoModel.calculatePathDelay(elevation + stepE, height);
        //final double stepH = h * FastMath.max(1., height);
        //final double dDelaydHeight = (delayH - delay) / stepH;

        final AbsoluteDate date = state.getDate();
        final double[] dEldX = Derivatives.derivElevationWrtState(date, station, state);

        return new double[][] {
            {dDelaydElevation * dEldX[0], dDelaydElevation * dEldX[1], dDelaydElevation * dEldX[2], 0, 0, 0}
        };
    }


    /** Compute the Jacobian of the delay term wrt parameters.
     *
     * @param station station
     * @param state spacecraft state
     * @param delay current tropospheric delay
     * @return jacobian of the delay wrt station position
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeErrorJacobianParameter(final GroundStation station,
                                                   final SpacecraftState state,
                                                   final double delay) throws OrekitException
    {
        final Vector3D position = state.getPVCoordinates().getPosition();

        // elevation in degrees
        final double elevation =
                        station.getBaseFrame().getElevation(position,
                                                            state.getFrame(),
                                                            state.getDate());

        // altitude AMSL in meters
        final double height = getStationHeightAMSL(station);

        //
        final double[] dHdR = Derivatives.derivHeightWrtGroundstation(state.getDate(), station, state);
        final double[] dEdR = Derivatives.derivElevationWrtGroundstation(state.getDate(), station, state);

        // compute the derivative of the tropospheric delay model wrt height and elevation.
        final double h = 1e-6; // finite difference step
        final double stepH = h * FastMath.max(1., height);
        final double delayH = tropoModel.calculatePathDelay(elevation, height + stepH);
        final double dDelaydHeight = (delayH - delay) / stepH;
        final double stepE = h * FastMath.max(1., elevation);
        final double delayE = tropoModel.calculatePathDelay(elevation + stepE, height);
        final double dDelaydElevation = (delayE - delay) / stepE;

        return new double[][]{
            {dDelaydHeight * dHdR[0] + dDelaydElevation * dEdR[0],
             dDelaydHeight * dHdR[1] + dDelaydElevation * dEdR[1],
             dDelaydHeight * dHdR[2] + dDelaydElevation * dEdR[2]}
        };
    }

    @Override
    public List<Parameter> getSupportedParameters() {
        return null;
    }

    @Override
    public void modify(final Evaluation evaluation)
        throws OrekitException {
        final Range measure = (Range) evaluation.getMeasurement();
        final GroundStation station = measure.getStation();
        final SpacecraftState state = evaluation.getState();

        final double[] oldValue = evaluation.getValue();

        final double delay = rangeErrorTroposphericModel(station, state);

        // update measurement value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + delay;
        evaluation.setValue(newValue);

        // update measurement derivatives with jacobian of the measure wrt state
        final double[][] djac = rangeErrorJacobianState(station,
                                      state);
        final double[][] stateDerivatives = evaluation.getStateDerivatives();
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djac[irow][jcol];
            }
        }
        evaluation.setStateDerivatives(stateDerivatives);


        if (station.isEstimated()) {
            // update measurement derivatives with jacobian of the measure wrt station parameters
            final double[][] djacdp = rangeErrorJacobianParameter(station,
                                                                  state,
                                                                  delay);
            final double[][] parameterDerivatives = evaluation.getParameterDerivatives(station.getName());
            for (int irow = 0; irow < parameterDerivatives.length; ++irow) {
                for (int jcol = 0; jcol < parameterDerivatives[0].length; ++jcol) {
                    parameterDerivatives[irow][jcol] += djacdp[irow][jcol];
                }
            }
            evaluation.setParameterDerivatives(station.getName(), parameterDerivatives);
        }
    }

    /**
     * Class providing derivative of elevation and height with respect to
     * target's state or measuring station's position.
     *
     *
     * TODO to put in some other class, cause it can be used by many measurement classes (e.g. RangeRateTroposphericDelayModifier...)
     * To put in GroundStation class ?
     *
     * @author Joris Olympio
     *
     */
    public static class Derivatives {

        /** Compute the derivative of target's elevation with respect to ground station's position.
        *
        * Compute:
        * diff(Elevation{target}, P{station}) = diff(Elevation{target}, X{station, LVLH})
        *                                          * diff(X{station, LVLH}, X{station, ECI})
        *
        * @param date  current date of the measurement at the station
        * @param station ground station
        * @param state spacecraft state
        *
        * @return gradient
        * @throws OrekitException if some frame specific error occurs
        */
        public static double[] derivElevationWrtGroundstation(final AbsoluteDate date,
                                                            final GroundStation station,
                                                            final SpacecraftState state) throws OrekitException {

// FIXME for some trivial, but missed, reason this does not work!

            final Frame frameBase = station.getBaseFrame().getParent();
            final Frame frameSta = station.getBaseFrame();
            final Frame frameSat = state.getFrame();
            final Transform tSat2Sta = frameSta.getTransformTo(frameBase, date);

            final Vector3D extPoint = state.getPVCoordinates(frameSat).getPosition();
            final Vector3D extPointTopo = tSat2Sta.transformPosition(extPoint);

            // Note: El = asin(z / norm)
            // x,y,z are the cartesian components in the station frame
            // Compute dElevation / dRlvlh
            // Rlvlh: station's position expressed in the base frame.
            final double x = extPointTopo.getX();
            final double y = extPointTopo.getY();
            final double z = extPointTopo.getZ();
            final double normLvlh = extPointTopo.getNorm();
            final double normLvlhSq = FastMath.pow(normLvlh, 2);
            final double xyRsq = FastMath.sqrt(x * x + y * y);
            final double[] dEldPlvlh = new double[] {
                (x * z) / (normLvlhSq * xyRsq),
                (y * z) / (normLvlhSq * xyRsq),
                -xyRsq / normLvlhSq,
                0, 0, 0
            };

            //
            final double[][] jacSatToBody = new double[6][6];
            final Transform t = frameSat.getTransformTo(frameBase, date).getInverse();
            t.getJacobian(CartesianDerivativesFilter.USE_P, jacSatToBody);
            final double[] dEldBody = new double[3];
            for (int irow = 0; irow < 3; ++irow) {
                double val = 0;
                for (int jcol = 0; jcol < 3; ++jcol) {
                    val += dEldPlvlh[jcol] * jacSatToBody[irow][jcol];
                }
                dEldBody[irow] = val;
            }

            // Compute dXlvlh / dXsta
            // Note: this is just because the delta-position when offsetting the groundstation base frame
            // is formulated in the base frame and not the ITRF frame.
            // Otherwise, the Jacobian would be identity.
            final double[][] jacBodyToBase = new double[3][3];
            final Frame baseFrame = station.getBaseFrame();
            final Frame     bodyFrame    = baseFrame.getParent();
            final Transform tBodyToBase   = bodyFrame.getTransformTo(baseFrame, null);
            tBodyToBase.getJacobian(CartesianDerivativesFilter.USE_P, jacBodyToBase);

            // (dEl / dX)^T = (dEl / dXlvlh)^T * dXlvlh / dRsta
            final double[] dEldG = new double[] {
                dEldBody[0] * jacBodyToBase[0][0] + dEldBody[1] * jacBodyToBase[0][1] + dEldBody[2] * jacBodyToBase[0][2],
                dEldBody[0] * jacBodyToBase[1][0] + dEldBody[1] * jacBodyToBase[1][1] + dEldBody[2] * jacBodyToBase[1][2],
                dEldBody[0] * jacBodyToBase[2][0] + dEldBody[1] * jacBodyToBase[2][1] + dEldBody[2] * jacBodyToBase[2][2]
            };

            return dEldG;
        }

        /** Compute the derivative of station's geodetic height
         * with respect to station position.
         * Compute:
         * diff(H{station}, P{station}) = diff(H{station}, X{station, LVLH})
         *                                           * diff(X{station, LVLH}, X{station, ECI})
         *
         * Note the station's geodetic height is generally the result of an iterative
         * process involving the computation of both latitude and height in order
         * to account for the eccentricity of the reference ellipsoid.
         * Conversely, the station position is expressed in closed-form from
         * latitude, longitude and height.
         * Therefore, one can use Penrose pseudoinverse to compute those derivatives, instead
         * of trying to find a trick to compute the derivative result of the iterative process.
         *
         * @param date  current date of the measurement at the station
         * @param station ground station
         * @param state spacecraft state
         *
         * @return gradient
         * @throws OrekitException  when an error occur.
         */
        public static double[] derivHeightWrtGroundstation(final AbsoluteDate date,
                                                     final GroundStation station,
                                                     final SpacecraftState state)
                                                                     throws OrekitException {
            // Assume one-axis ellipsoid...
            // see OneAxisEllipsoid transform()
            //
            final Frame frame = FramesFactory.getEME2000();
            final Vector3D pos = station.getOffsetFrame().getPVCoordinates(date, frame).getPosition();
            final Vector3D posEll = station.getOffsetFrame().getParentShape().projectToGround(pos, date, frame);

            final double latitude = station.getOffsetFrame().getPoint().getLatitude();
            final double longitude = station.getOffsetFrame().getPoint().getLongitude();
            final double cPhi      = FastMath.cos(latitude);
            final double sPhi      = FastMath.sin(latitude);
            final double cLambda      = FastMath.cos(longitude);
            final double sLambda      = FastMath.sin(longitude);
            final double f = ((OneAxisEllipsoid) station.getOffsetFrame().getParentShape()).getFlattening();
            final double e2 = f * (2.0 - f);
            final double sma = ((OneAxisEllipsoid) station.getOffsetFrame().getParentShape()).getA();
            final double n         = sma / FastMath.sqrt(1.0 - e2 * sPhi * sPhi);

            // drho / dR
            final double[] dhdXlvlh = new double[] {cLambda * cPhi, sLambda * cPhi, sPhi};

            // Compute dXlvlh / dRsta
            // Note: this is just because the delta-position when offsetting the groundstation base frame
            // is formulated in the base frame and not the ITRF frame.
            // Otherwise, the Jacobian would be identity.
            final double[][] jacBodyToBase = new double[3][3];
            final Frame baseFrame = station.getBaseFrame();
            final Frame     bodyFrame    = baseFrame.getParent();
            final Transform tBodyToBase   = bodyFrame.getTransformTo(baseFrame, null);
            tBodyToBase.getJacobian(CartesianDerivativesFilter.USE_P, jacBodyToBase);

            // (dEl / dX)^T = (dEl / dXlvlh)^T * dXlvlh / dRsta
            final double[] dHdG = new double[] {
                dhdXlvlh[0] * jacBodyToBase[0][0] + dhdXlvlh[1] * jacBodyToBase[0][1] + dhdXlvlh[2] * jacBodyToBase[0][2],
                dhdXlvlh[0] * jacBodyToBase[1][0] + dhdXlvlh[1] * jacBodyToBase[1][1] + dhdXlvlh[2] * jacBodyToBase[1][2],
                dhdXlvlh[0] * jacBodyToBase[2][0] + dhdXlvlh[1] * jacBodyToBase[2][1] + dhdXlvlh[2] * jacBodyToBase[2][2]
            };

            return dHdG;
        }

        /** Compute the derivative of target's elevation
         * with respect to target state.
         *
         * Compute:
         * diff(Elevation{target}, X{target}) = diff(Elevation{target}, X{target, LVLH})
         *                                          * diff(X{target, LVLH}, X{target, ECI})
         *
         * @param date  current date of the measurement at the station
         * @param station ground station
         * @param state spacecraft state
         *
         * @return gradient
         * @throws OrekitException when a frame error occurs
         */
        public static double[] derivElevationWrtState(final AbsoluteDate date, final GroundStation station,
                                                final SpacecraftState state)
                                                                throws OrekitException {

            final Frame frameSta = station.getBaseFrame();
            final Frame frameSat = state.getFrame();
            final Transform tSatToSta = frameSat.getTransformTo(frameSta, date);

            final Vector3D extPoint = state.getPVCoordinates(frameSat).getPosition();
            final Vector3D extPointTopo = tSatToSta.transformPosition(extPoint);

            // Note: El = asin(z / norm)
            // x,y,z are the cartesian components in the station frame
            // Compute dElevation / dXlvlh
            // Xlvlh is the spacecraft state expressed in the LVLH frame.
            final double x = extPointTopo.getX();
            final double y = extPointTopo.getY();
            final double z = extPointTopo.getZ();
            final double normLvlh = extPointTopo.getNorm();
            final double normLvlhSq = FastMath.pow(normLvlh, 2);
            final double xyRsq = FastMath.sqrt(x * x + y * y);
            final double[] dEldXlvlh = new double[] {
                -(x * z) / (normLvlhSq * xyRsq),
                -(y * z) / (normLvlhSq * xyRsq),
                xyRsq / normLvlhSq,
                0, 0, 0
            };

             // Compute dXlvlh / dXsat
            final double[][] jacBodyToBase = new double[6][6];
            (tSatToSta.getInverse()).getJacobian(CartesianDerivativesFilter.USE_PV, jacBodyToBase);

            // (dEl / dX)^T = (dEl / dXlvlh)^T * (dXlvlh / dRsta)
            final double[] dEldX = new double[6];
            for (int irow = 0; irow < 6; ++irow) {
                double val = 0;
                for (int jcol = 0; jcol < 6; ++jcol) {
                    val += dEldXlvlh[jcol] * jacBodyToBase[irow][jcol];
                }
                dEldX[irow] = val;
            }

            return dEldX;
        }
    }
}
