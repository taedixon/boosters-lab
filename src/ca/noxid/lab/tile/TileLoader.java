package ca.noxid.lab.tile;

import ca.noxid.lab.mapdata.MapInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface TileLoader {
	List<TileLayer> getLayers();

	List<LineSeg> getLines();

	List<MapPoly> getPolygons();

	void loadMap(File pxmFile, MapInfo mapInfo) throws IOException;
}
