package org.orekit.estimation;

import java.util.List;

import org.orekit.estimation.measurements.GroundStation;

/**
 * Utility class for station data providers.
 */
public interface StationDataProvider {

    List<GroundStation> getStations();

}
