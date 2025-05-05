/* Copyright 2024-2025 The Johns Hopkins University Applied Physics Laboratory
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.files.iirv;

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.iirv.terms.CoordinateSystemTerm;
import org.orekit.files.iirv.terms.CrossSectionalAreaTerm;
import org.orekit.files.iirv.terms.DataSourceTerm;
import org.orekit.files.iirv.terms.DayOfYearTerm;
import org.orekit.files.iirv.terms.DragCoefficientTerm;
import org.orekit.files.iirv.terms.MassTerm;
import org.orekit.files.iirv.terms.MessageClassTerm;
import org.orekit.files.iirv.terms.MessageIDTerm;
import org.orekit.files.iirv.terms.MessageSourceTerm;
import org.orekit.files.iirv.terms.MessageTypeTerm;
import org.orekit.files.iirv.terms.OriginIdentificationTerm;
import org.orekit.files.iirv.terms.OriginatorRoutingIndicatorTerm;
import org.orekit.files.iirv.terms.PositionVectorComponentTerm;
import org.orekit.files.iirv.terms.RoutingIndicatorTerm;
import org.orekit.files.iirv.terms.SequenceNumberTerm;
import org.orekit.files.iirv.terms.SolarReflectivityCoefficientTerm;
import org.orekit.files.iirv.terms.SupportIdCodeTerm;
import org.orekit.files.iirv.terms.VectorEpochTerm;
import org.orekit.files.iirv.terms.VectorTypeTerm;
import org.orekit.files.iirv.terms.VehicleIdCodeTerm;
import org.orekit.files.iirv.terms.VelocityVectorComponentTerm;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.UTCScale;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link IIRVVector}.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class IIRVBuilder {

    /** UTC time scale. */
    private final UTCScale utc;
    /**
     * Message type, default: "00".
     */
    private MessageTypeTerm messageType = MessageTypeTerm.DEFAULT;
    /**
     * Message Identification, default: "0000000".
     */
    private MessageIDTerm messageID = new MessageIDTerm(0);
    /**
     * Message source, default: "0".
     */
    private MessageSourceTerm messageSource = MessageSourceTerm.DEFAULT;
    /**
     * Message class, default: "10" [nominal].
     */
    private MessageClassTerm messageClass = MessageClassTerm.NOMINAL;
    /**
     * Origin identification, default: " " [NASA Goddard Space Flight Center].
     */
    private OriginIdentificationTerm originIdentification = OriginIdentificationTerm.GSFC;
    /**
     * Destination routing indicator, default: "MANY".
     */
    private RoutingIndicatorTerm routingIndicator = RoutingIndicatorTerm.MANY;
    /**
     * Vector type, default: "1" [Free flight].
     */
    private VectorTypeTerm vectorType = VectorTypeTerm.FREE_FLIGHT;
    /**
     * Source of data, default: "1" [nominal/planning].
     */
    private DataSourceTerm dataSource = DataSourceTerm.NOMINAL;
    /**
     * Coordinate system, default: "1" [Geocentric True-of-Date Rotating (GTOD)].
     */
    private CoordinateSystemTerm coordinateSystem = CoordinateSystemTerm.GEOCENTRIC_TRUE_OF_DATE_ROTATING;
    /**
     * Support ID Code, default: "0000".
     */
    private SupportIdCodeTerm supportIdCode = new SupportIdCodeTerm("0000");
    /**
     * Vehicle ID Code, default: "01".
     */
    private VehicleIdCodeTerm vehicleIdCode = new VehicleIdCodeTerm("01");
    /**
     * Sequence number, default: "000".
     */
    private SequenceNumberTerm sequenceNumber = new SequenceNumberTerm(0);
    /**
     * Satellite mass (kg), default: "00156170" [unused].
     */
    private MassTerm mass = MassTerm.UNUSED;
    /**
     * Average satellite cross-sectional area (m^2), default: "00000" [unused].
     */
    private CrossSectionalAreaTerm crossSectionalArea = CrossSectionalAreaTerm.UNUSED;
    /**
     * Drag coefficient (dimensionless), default: "0000" [unused].
     */
    private DragCoefficientTerm dragCoefficient = DragCoefficientTerm.UNUSED;
    /**
     * Solar reflectivity coefficient (dimensionless), default: "00000000" [unused].
     */
    private SolarReflectivityCoefficientTerm solarReflectivityCoefficient = SolarReflectivityCoefficientTerm.UNUSED;
    /**
     * Originator of message (GCQU or GAQD), default: "GAQD".
     */
    private OriginatorRoutingIndicatorTerm originatorRoutingIndicator = OriginatorRoutingIndicatorTerm.GAQD;

    /**
     * Constructs an {@link IIRVBuilder} instance with a UTC timescale and default values for all parameters.
     *
     * @param utc UTC time scale
     */
    public IIRVBuilder(final UTCScale utc) {
        this.utc = utc;
    }

    /**
     * Constructs an IIRV object using the configured parameters.
     *
     * @param dayOfYear   Day of year, 001 to 366
     * @param vectorEpoch Vector epoch in UTC
     * @param xPosition   X component of the position vector [m]
     * @param yPosition   Y component of the position vector [m]
     * @param zPosition   Z component of the position vector [m]
     * @param xVelocity   X component of the velocity vector [m/s]
     * @param yVelocity   Y component of the velocity vector [m/s]
     * @param zVelocity   Z component of the velocity vector [m/s]
     * @return the newly constructed IIRV object
     */
    public IIRVVector buildVector(final DayOfYearTerm dayOfYear,
                                  final VectorEpochTerm vectorEpoch,
                                  final PositionVectorComponentTerm xPosition,
                                  final PositionVectorComponentTerm yPosition,
                                  final PositionVectorComponentTerm zPosition,
                                  final VelocityVectorComponentTerm xVelocity,
                                  final VelocityVectorComponentTerm yVelocity,
                                  final VelocityVectorComponentTerm zVelocity) {
        return new IIRVVector(
            messageType, messageID, messageSource, messageClass, originIdentification, routingIndicator, // Line 1
            vectorType, dataSource, coordinateSystem, supportIdCode, vehicleIdCode, sequenceNumber, dayOfYear, vectorEpoch, // Line 2
            xPosition, yPosition, zPosition, // Line 3
            xVelocity, yVelocity, zVelocity, // Line 4
            mass, crossSectionalArea, dragCoefficient, solarReflectivityCoefficient, // Line 5
            originatorRoutingIndicator, // Line 6
            utc
        );
    }

    /**
     * Constructs an IIRV vector using the configured parameters, with position, velocity, and time variables derived
     * from instances of {@link TimeStampedPVCoordinates} and {@link AbsoluteDate}.
     *
     * @param timeStampedPVCoordinates position and velocity components at a particular epoch corresponding to the
     *                                 IIRV vector
     * @return the newly constructed IIRV object at the given coordinates
     */
    public IIRVVector buildVector(final TimeStampedPVCoordinates timeStampedPVCoordinates) {

        // Retrieve the epoch associated with the given coordinates
        final AbsoluteDate epoch = timeStampedPVCoordinates.getDate();

        // Construct the IIRV time variable terms
        final DayOfYearTerm dayOfYear = new DayOfYearTerm(epoch, utc);
        final VectorEpochTerm vectorEpoch = new VectorEpochTerm(epoch, utc);

        // Construct the position component terms
        final PositionVectorComponentTerm xPosition = new PositionVectorComponentTerm(timeStampedPVCoordinates.getPosition().getX());
        final PositionVectorComponentTerm yPosition = new PositionVectorComponentTerm(timeStampedPVCoordinates.getPosition().getY());
        final PositionVectorComponentTerm zPosition = new PositionVectorComponentTerm(timeStampedPVCoordinates.getPosition().getZ());

        // Construct the velocity component terms
        final VelocityVectorComponentTerm xVelocity = new VelocityVectorComponentTerm(timeStampedPVCoordinates.getVelocity().getX());
        final VelocityVectorComponentTerm yVelocity = new VelocityVectorComponentTerm(timeStampedPVCoordinates.getVelocity().getY());
        final VelocityVectorComponentTerm zVelocity = new VelocityVectorComponentTerm(timeStampedPVCoordinates.getVelocity().getZ());

        // Construct an IIRV vector with the given terms
        return new IIRVVector(
            messageType, messageID, messageSource, messageClass, originIdentification, routingIndicator, // Line 1
            vectorType, dataSource, coordinateSystem, supportIdCode, vehicleIdCode, sequenceNumber, dayOfYear, vectorEpoch, // Line 2
            xPosition, yPosition, zPosition, // Line 3
            xVelocity, yVelocity, zVelocity, // Line 4
            mass, crossSectionalArea, dragCoefficient, solarReflectivityCoefficient, // Line 5
            originatorRoutingIndicator, // Line 6
            utc
        );
    }

    /**
     * Constructs an {@link IIRVMessage} where each {@link IIRVVector} in initialized from the inputted list of
     * {@link TimeStampedPVCoordinates}.
     *
     * @param timeStampedPVCoordinates list of time-stamped position and velocity vectors to populate the message
     * @param <C>                      type of the Cartesian coordinates
     * @return the newly constructed {@link IIRVMessage} containing the given coordinates
     */
    public <C extends TimeStampedPVCoordinates> IIRVMessage buildIIRVMessage(final List<C> timeStampedPVCoordinates) {
        final ArrayList<IIRVVector> vectors = new ArrayList<>();
        int incrementalSequenceNumber = 0;
        for (TimeStampedPVCoordinates coordinates : timeStampedPVCoordinates) {
            // Add coordinate to the list of vectors with the current sequence number
            setSequenceNumber(incrementalSequenceNumber);
            vectors.add(buildVector(coordinates));
            incrementalSequenceNumber++;
        }
        return new IIRVMessage(vectors);
    }


    /**
     * Constructs an {@link IIRVEphemerisFile} from the inputted list of {@link TimeStampedPVCoordinates}, inferring
     * the start year from the first coordinate's {@link org.orekit.time.AbsoluteDate}.
     * <p>
     * See {@link #buildIIRVMessage(List)} for {@link IIRVMessage} construction details.
     *
     * @param timeStampedPVCoordinates list of time-stamped position and velocity vectors to populate the message
     * @param <C>                      type of the Cartesian coordinates
     * @return the newly constructed {@link IIRVEphemerisFile} containing the given coordinates
     */
    public <C extends TimeStampedPVCoordinates> IIRVEphemerisFile buildEphemerisFile(final List<C> timeStampedPVCoordinates) {
        final int year = timeStampedPVCoordinates.get(0).getDate().getComponents(utc).getDate().getYear();
        return new IIRVEphemerisFile(year, buildIIRVMessage(timeStampedPVCoordinates));
    }

    /**
     * Gets the current {@link MassTerm} value.
     *
     * @return the current {@link MassTerm} value.
     */
    public MassTerm getMass() {
        return mass;
    }

    /**
     * Overrides the default Mass attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: kg
     *
     * @param mass mass value (kg) for the IIRV message
     */
    public void setMass(final MassTerm mass) {
        this.mass = mass;
    }

    /**
     * Overrides the default Mass attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: kg
     *
     * @param mass mass value (kg) for the IIRV message
     */
    public void setMass(final String mass) {
        this.mass = new MassTerm(mass);
    }

    /**
     * Overrides the default Mass attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: kg
     *
     * @param mass mass value (kg) for the IIRV message
     */
    public void setMass(final double mass) {
        this.mass = new MassTerm(mass);
    }

    /**
     * Gets the current {@link MessageTypeTerm} value.
     *
     * @return the current {@link MessageTypeTerm} value.
     */
    public MessageTypeTerm getMessageType() {
        return messageType;
    }

    /**
     * Overrides the default {@link MessageTypeTerm} attribute for the {@link IIRVVector} object being built.
     *
     * @param messageType {@link MessageTypeTerm} for the IIRV message
     */
    public void setMessageType(final String messageType) {
        this.messageType = new MessageTypeTerm(messageType);
    }

    /**
     * Overrides the default {@link MessageTypeTerm} attribute for the {@link IIRVVector} object being built.
     *
     * @param messageType {@link MessageTypeTerm} for the IIRV message
     */
    public void setMessageType(final MessageTypeTerm messageType) {
        this.messageType = messageType;
    }

    /**
     * Gets the current {@link MessageSourceTerm} value.
     *
     * @return the current {@link MessageSourceTerm} value.
     */
    public MessageSourceTerm getMessageSource() {
        return messageSource;
    }

    /**
     * Overrides the default {@link MessageSourceTerm} attribute for the {@link IIRVVector} object being built.
     *
     * @param messageSource {@link MessageSourceTerm} for the IIRV message
     */
    public void setMessageSource(final String messageSource) {
        this.messageSource = new MessageSourceTerm(messageSource);
    }

    /**
     * Overrides the default {@link MessageSourceTerm} attribute for the {@link IIRVVector} object being built.
     *
     * @param messageSource {@link MessageSourceTerm} for the IIRV message
     */
    public void setMessageSource(final MessageSourceTerm messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Gets the current {@link MessageIDTerm} value.
     *
     * @return the current {@link MessageIDTerm} value.
     */
    public MessageIDTerm getMessageID() {
        return messageID;
    }

    /**
     * Overrides the default MessageID attribute for the {@link IIRVVector} object being built.
     *
     * @param messageID message ID value for the IIRV message
     */
    public void setMessageID(final MessageIDTerm messageID) {
        this.messageID = messageID;
    }

    /**
     * Overrides the default MessageID attribute for the {@link IIRVVector} object being built.
     *
     * @param messageID message ID value for the IIRV message
     */
    public void setMessageID(final String messageID) {
        this.messageID = new MessageIDTerm(messageID);
    }

    /**
     * Overrides the default MessageID attribute for the {@link IIRVVector} object being built.
     *
     * @param messageID message ID value for the IIRV message
     */
    public void setMessageID(final long messageID) {
        this.messageID = new MessageIDTerm(messageID);
    }

    /**
     * Gets the current {@link MessageClassTerm} value.
     *
     * @return the current {@link MessageClassTerm} value.
     */
    public MessageClassTerm getMessageClass() {
        return messageClass;
    }

    /**
     * Overrides the default MessageClass attribute for the {@link IIRVVector} object being built.
     *
     * @param messageClass message class value for the IIRV message
     */
    public void setMessageClass(final MessageClassTerm messageClass) {
        this.messageClass = messageClass;
    }

    /**
     * Overrides the default MessageClass attribute for the {@link IIRVVector} object being built.
     *
     * @param messageClass message class value for the IIRV message
     */
    public void setMessageClass(final String messageClass) {
        this.messageClass = new MessageClassTerm(messageClass);
    }

    /**
     * Overrides the default MessageClass attribute for the {@link IIRVVector} object being built.
     *
     * @param messageClass message class value for the IIRV message
     */
    public void setMessageClass(final long messageClass) {
        this.messageClass = new MessageClassTerm(messageClass);
    }

    /**
     * Gets the current {@link OriginIdentificationTerm} value.
     *
     * @return the current {@link OriginIdentificationTerm} value.
     */
    public OriginIdentificationTerm getOriginIdentification() {
        return originIdentification;
    }

    /**
     * Overrides the default OriginIdentification attribute for the {@link IIRVVector} object being built.
     *
     * @param originIdentification origin identification value for the IIRV message
     */
    public void setOriginIdentification(final OriginIdentificationTerm originIdentification) {
        this.originIdentification = originIdentification;
    }

    /**
     * Overrides the default OriginIdentification attribute for the {@link IIRVVector} object being built.
     *
     * @param originIdentification origin identification value for the IIRV message
     */
    public void setOriginIdentification(final String originIdentification) {
        this.originIdentification = new OriginIdentificationTerm(originIdentification);
    }

    /**
     * Gets the current {@link RoutingIndicatorTerm} value.
     *
     * @return the current {@link RoutingIndicatorTerm} value.
     */
    public RoutingIndicatorTerm getRoutingIndicator() {
        return routingIndicator;
    }

    /**
     * Overrides the default RoutingIndicator attribute for the {@link IIRVVector} object being built.
     *
     * @param routingIndicator routing indicator term value for the IIRV message
     */
    public void setRoutingIndicator(final RoutingIndicatorTerm routingIndicator) {
        this.routingIndicator = routingIndicator;
    }

    /**
     * Overrides the default RoutingIndicator attribute for the {@link IIRVVector} object being built.
     *
     * @param routingIndicator routing indicator term value for the IIRV message
     */
    public void setRoutingIndicator(final String routingIndicator) {
        this.routingIndicator = new RoutingIndicatorTerm(routingIndicator);
    }

    /**
     * Gets the current {@link VectorTypeTerm} value.
     *
     * @return the current {@link VectorTypeTerm} value.
     */
    public VectorTypeTerm getVectorType() {
        return vectorType;
    }

    /**
     * Overrides the default VectorType attribute for the {@link IIRVVector} object being built.
     *
     * @param vectorType vector type term value for the IIRV message
     */
    public void setVectorType(final VectorTypeTerm vectorType) {
        this.vectorType = vectorType;
    }

    /**
     * Overrides the default VectorType attribute for the {@link IIRVVector} object being built.
     *
     * @param vectorType vector type term value for the IIRV message
     */
    public void setVectorType(final String vectorType) {
        this.vectorType = new VectorTypeTerm(vectorType);
    }

    /**
     * Overrides the default VectorType attribute for the {@link IIRVVector} object being built.
     *
     * @param vectorType vector type term value for the IIRV message
     */
    public void setVectorType(final long vectorType) {
        this.vectorType = new VectorTypeTerm(vectorType);
    }

    /**
     * Gets the current {@link DataSourceTerm} value.
     *
     * @return the current {@link DataSourceTerm} value.
     */
    public DataSourceTerm getDataSource() {
        return dataSource;
    }

    /**
     * Overrides the default DataSource attribute for the {@link IIRVVector} object being built.
     *
     * @param dataSource data source term value for the IIRV message
     */
    public void setDataSource(final DataSourceTerm dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Overrides the default DataSource attribute for the {@link IIRVVector} object being built.
     *
     * @param dataSource data source term value for the IIRV message
     */
    public void setDataSource(final long dataSource) {
        this.dataSource = new DataSourceTerm(dataSource);
    }

    /**
     * Overrides the default DataSource attribute for the {@link IIRVVector} object being built.
     *
     * @param dataSource data source term value for the IIRV message
     */
    public void setDataSource(final String dataSource) {
        this.dataSource = new DataSourceTerm(dataSource);
    }

    /**
     * Gets the current {@link CoordinateSystemTerm} value.
     *
     * @return the current {@link CoordinateSystemTerm} value.
     */
    public CoordinateSystemTerm getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Overrides the default CoordinateSystem attribute for the {@link IIRVVector} object being built.
     *
     * @param coordinateSystem coordinate system term value for the IIRV message
     */
    public void setCoordinateSystem(final CoordinateSystemTerm coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
    }

    /**
     * Overrides the default CoordinateSystem attribute for the {@link IIRVVector} object being built.
     *
     * @param coordinateSystem coordinate system term value for the IIRV message
     */
    public void setCoordinateSystem(final String coordinateSystem) {
        this.coordinateSystem = new CoordinateSystemTerm(coordinateSystem);
    }

    /**
     * Overrides the default CoordinateSystem attribute for the {@link IIRVVector} object being built.
     *
     * @param coordinateSystem coordinate system term value for the IIRV message
     */
    public void setCoordinateSystem(final long coordinateSystem) {
        this.coordinateSystem = new CoordinateSystemTerm(coordinateSystem);
    }

    /**
     * Gets the current {@link SupportIdCodeTerm} value.
     *
     * @return the current {@link SupportIdCodeTerm} value.
     */
    public SupportIdCodeTerm getSupportIdCode() {
        return supportIdCode;
    }

    /**
     * Overrides the default SupportIdCode attribute for the {@link IIRVVector} object being built.
     *
     * @param supportIdCode support id code value for the IIRV message
     */
    public void setSupportIdCode(final SupportIdCodeTerm supportIdCode) {
        this.supportIdCode = supportIdCode;
    }

    /**
     * Overrides the default SupportIdCode attribute for the {@link IIRVVector} object being built.
     *
     * @param supportIdCode support id code value for the IIRV message
     */
    public void setSupportIdCode(final String supportIdCode) {
        this.supportIdCode = new SupportIdCodeTerm(supportIdCode);
    }

    /**
     * Overrides the default SupportIdCode attribute for the {@link IIRVVector} object being built.
     *
     * @param supportIdCode support id code value for the IIRV message
     */
    public void setSupportIdCode(final long supportIdCode) {
        this.supportIdCode = new SupportIdCodeTerm(supportIdCode);
    }

    /**
     * Gets the current {@link VehicleIdCodeTerm} value.
     *
     * @return the current {@link VehicleIdCodeTerm} value.
     */
    public VehicleIdCodeTerm getVehicleIdCode() {
        return vehicleIdCode;
    }

    /**
     * Overrides the default VehicleIdCode attribute for the {@link IIRVVector} object being built.
     *
     * @param vehicleIdCode vehicle id code value for the IIRV message
     */
    public void setVehicleIdCode(final VehicleIdCodeTerm vehicleIdCode) {
        this.vehicleIdCode = vehicleIdCode;
    }

    /**
     * Overrides the default VehicleIdCode attribute for the {@link IIRVVector} object being built.
     *
     * @param vehicleIdCode vehicle id code value for the IIRV message
     */
    public void setVehicleIdCode(final String vehicleIdCode) {
        this.vehicleIdCode = new VehicleIdCodeTerm(vehicleIdCode);
    }

    /**
     * Overrides the default VehicleIdCode attribute for the {@link IIRVVector} object being built.
     *
     * @param vehicleIdCode vehicle id code value for the IIRV message
     */
    public void setVehicleIdCode(final long vehicleIdCode) {
        this.vehicleIdCode = new VehicleIdCodeTerm(vehicleIdCode);
    }

    /**
     * Gets the current {@link SequenceNumberTerm} value.
     *
     * @return the current {@link SequenceNumberTerm} value.
     */
    public SequenceNumberTerm getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Overrides the default SequenceNumber attribute for the {@link IIRVVector} object being built.
     *
     * @param sequenceNumber sequence number value for the IIRV message
     */
    public void setSequenceNumber(final SequenceNumberTerm sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Overrides the default SequenceNumber attribute for the {@link IIRVVector} object being built.
     *
     * @param sequenceNumber sequence number value for the IIRV message
     */
    public void setSequenceNumber(final String sequenceNumber) {
        this.sequenceNumber = new SequenceNumberTerm(sequenceNumber);
    }

    /**
     * Overrides the default SequenceNumber attribute for the {@link IIRVVector} object being built.
     *
     * @param sequenceNumber sequence number value for the IIRV message
     */
    public void setSequenceNumber(final long sequenceNumber) {
        if (sequenceNumber > SequenceNumberTerm.MAX_SEQUENCE_NUMBER) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_EXCEEDS_MAX_VECTORS, sequenceNumber);
        }
        this.sequenceNumber = new SequenceNumberTerm(sequenceNumber);
    }

    /**
     * Gets the current {@link CrossSectionalAreaTerm} value.
     *
     * @return the current {@link CrossSectionalAreaTerm} value.
     */
    public CrossSectionalAreaTerm getCrossSectionalArea() {
        return crossSectionalArea;
    }

    /**
     * Overrides the default {@link CrossSectionalAreaTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: m^2
     *
     * @param crossSectionalArea cross-sectional area value (m^2) for the IIRV message
     */
    public void setCrossSectionalArea(final CrossSectionalAreaTerm crossSectionalArea) {
        this.crossSectionalArea = crossSectionalArea;
    }

    /**
     * Overrides the default {@link CrossSectionalAreaTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: m^2
     * <p>
     * See {@link CrossSectionalAreaTerm#CrossSectionalAreaTerm(String)}
     *
     * @param crossSectionalArea cross-sectional area value (m^2) for the IIRV message
     */
    public void setCrossSectionalArea(final String crossSectionalArea) {
        this.crossSectionalArea = new CrossSectionalAreaTerm(crossSectionalArea);
    }

    /**
     * Overrides the default {@link CrossSectionalAreaTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: m^2
     * <p>
     * See {@link CrossSectionalAreaTerm#CrossSectionalAreaTerm(double)}
     *
     * @param crossSectionalArea cross-sectional area value (m^2) for the IIRV message
     */
    public void setCrossSectionalArea(final double crossSectionalArea) {
        this.crossSectionalArea = new CrossSectionalAreaTerm(crossSectionalArea);
    }

    /**
     * Gets the current {@link DragCoefficientTerm} value.
     *
     * @return the current {@link DragCoefficientTerm} value.
     */
    public DragCoefficientTerm getDragCoefficient() {
        return dragCoefficient;
    }

    /**
     * Overrides the default {@link DragCoefficientTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: dimensionless
     *
     * @param dragCoefficient drag coefficient value (dimensionless) for the IIRV message
     */
    public void setDragCoefficient(final DragCoefficientTerm dragCoefficient) {
        this.dragCoefficient = dragCoefficient;
    }

    /**
     * Overrides the default {@link DragCoefficientTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: dimensionless
     * <p>
     * See {@link DragCoefficientTerm#DragCoefficientTerm(String)}
     *
     * @param dragCoefficient drag coefficient value (dimensionless) for the IIRV message
     */
    public void setDragCoefficient(final String dragCoefficient) {
        this.dragCoefficient = new DragCoefficientTerm(dragCoefficient);
    }

    /**
     * Overrides the default {@link DragCoefficientTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: dimensionless
     * <p>
     * See {@link DragCoefficientTerm#DragCoefficientTerm(double)}
     *
     * @param dragCoefficient drag coefficient value (dimensionless) for the IIRV message
     */
    public void setDragCoefficient(final double dragCoefficient) {
        this.dragCoefficient = new DragCoefficientTerm(dragCoefficient);
    }

    /**
     * Gets the current {@link SolarReflectivityCoefficientTerm} value.
     *
     * @return the current {@link SolarReflectivityCoefficientTerm} value.
     */
    public SolarReflectivityCoefficientTerm getSolarReflectivityCoefficient() {
        return solarReflectivityCoefficient;
    }

    /**
     * Overrides the default {@link SolarReflectivityCoefficientTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: dimensionless
     *
     * @param solarReflectivityCoefficient solar reflectivity coefficient value (dimensionless) for the IIRV message
     */
    public void setSolarReflectivityCoefficient(final SolarReflectivityCoefficientTerm solarReflectivityCoefficient) {
        this.solarReflectivityCoefficient = solarReflectivityCoefficient;
    }

    /**
     * Overrides the default {@link SolarReflectivityCoefficientTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: dimensionless
     * <p>
     * See {@link SolarReflectivityCoefficientTerm#SolarReflectivityCoefficientTerm(String)}
     *
     * @param solarReflectivityCoefficient solar reflectivity coefficient value (dimensionless) for the IIRV message
     */
    public void setSolarReflectivityCoefficient(final String solarReflectivityCoefficient) {
        this.solarReflectivityCoefficient = new SolarReflectivityCoefficientTerm(solarReflectivityCoefficient);
    }

    /**
     * Overrides the default {@link SolarReflectivityCoefficientTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * Units: dimensionless
     * <p>
     * See {@link SolarReflectivityCoefficientTerm#SolarReflectivityCoefficientTerm(double)}
     *
     * @param solarReflectivityCoefficient solar reflectivity coefficient value (dimensionless) for the IIRV message
     */
    public void setSolarReflectivityCoefficient(final double solarReflectivityCoefficient) {
        this.solarReflectivityCoefficient = new SolarReflectivityCoefficientTerm(solarReflectivityCoefficient);
    }

    /**
     * Gets the current {@link OriginatorRoutingIndicatorTerm} value.
     *
     * @return the current {@link OriginatorRoutingIndicatorTerm} value.
     */
    public OriginatorRoutingIndicatorTerm getOriginatorRoutingIndicator() {
        return originatorRoutingIndicator;
    }

    /**
     * Overrides the default {@link OriginatorRoutingIndicatorTerm} attribute for the {@link IIRVVector} object being built.
     *
     * @param originatorRoutingIndicator originator routing indicator value for the IIRV message
     */
    public void setOriginatorRoutingIndicator(final OriginatorRoutingIndicatorTerm originatorRoutingIndicator) {
        this.originatorRoutingIndicator = originatorRoutingIndicator;
    }

    /**
     * Overrides the default {@link OriginatorRoutingIndicatorTerm} attribute for the {@link IIRVVector} object being built.
     * <p>
     * See {@link OriginatorRoutingIndicatorTerm#OriginatorRoutingIndicatorTerm(String)}
     *
     * @param originatorRoutingIndicator originator routing indicator value for the IIRV message
     */
    public void setOriginatorRoutingIndicator(final String originatorRoutingIndicator) {
        this.originatorRoutingIndicator = new OriginatorRoutingIndicatorTerm(originatorRoutingIndicator);
    }

    /**
     * Returns the satellite ID (set to the value of the {@link VehicleIdCodeTerm}).
     *
     * @return the satellite ID
     */
    public String getSatelliteID() {
        return vehicleIdCode.toEncodedString();
    }


}
