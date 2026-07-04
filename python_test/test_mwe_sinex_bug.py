import pytest
import orekit_jpype

orekit_jpype.initVM()

from org.orekit.utils import Constants
from org.orekit.data import DataSource
from org.orekit.time import TimeScalesFactory
from orekit_jpype.pyhelpers import setup_orekit_data


def test_sinex_load_slr():
    setup_orekit_data()
    data_source = DataSource("SLRF2020_POS+VEL_2025.05.13.snx")
    try:
        # Orekit 13
        from org.orekit.files.sinex import SinexParser

        sinex_parser = SinexParser(TimeScalesFactory.getTimeScales())
        stations_sinex = sinex_parser.parse(data_source)
    except ImportError:
        # Orekit 12
        from org.orekit.files.sinex import SinexLoader

        stations_sinex = SinexLoader(data_source)

    stations_map = stations_sinex.getStations()
    station_data = stations_map.get("7840")

    pos = station_data.getPosition()
    assert pos.getX() == pytest.approx(0.403346347630212e07, 1e-9)
    assert pos.getY() == pytest.approx(0.236627872673329e05, 1e-9)
    assert pos.getZ() == pytest.approx(0.492430535075408e07, 1e-9)

    vel = station_data.getVelocity()
    assert vel.getX() == pytest.approx(
        -0.131572303249976e-01 / Constants.JULIAN_YEAR, 1e-9
    )
    assert vel.getY() == pytest.approx(
        0.170848681043556e-01 / Constants.JULIAN_YEAR, 1e-9
    )
    assert vel.getZ() == pytest.approx(
        0.981395859644500e-02 / Constants.JULIAN_YEAR, 1e-9
    )
