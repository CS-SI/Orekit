package org.orekit.estimation;

import org.orekit.estimation.measurements.GroundStation;

import java.util.List;

/**
 * Utility class for station data providers.
 */
public interface StationDataProvider {

    List<GroundStation> getStations();

}
