package ca.noxid.lab.tile;

import ca.noxid.lab.mapdata.MapInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TiledLoader implements TileLoader {

	public int width;
	public int height;

	List<TileLayer> tiles = new LinkedList<>();

	public TiledLoader() {
	}

	@Override
	public List<TileLayer> getLayers() {
		return tiles;
	}

	@Override
	public List<LineSeg> getLines() {
		return new LinkedList<>();
	}

	@Override
	public List<MapPoly> getPolygons() {
		return new LinkedList<>();
	}

	@Override
	public void loadMap(File mapFile, MapInfo info) throws IOException {

		// i'd use readstring but intellij can't find it for some reason
		var lines = Files.readAllLines(mapFile.toPath());
		StringBuilder sb = new StringBuilder();
		lines.forEach(sb::append);
		JSONObject data = new JSONObject(sb.toString());
		tiles = new LinkedList<>();

		width = data.getInt("width");
		height = data.getInt("height");
		JSONArray layers = data.getJSONArray("layers");
		var allLayers = new LinkedList<JSONObject>();
		for (int i = 0; i < layers.length(); i++) {
			allLayers.add(layers.getJSONObject(i));
		}

		tiles = allLayers.stream()
				.filter(l -> "tilelayer".equals(l.getString("type")))
				.sorted(Comparator.comparingInt(a -> a.getInt("id")))
				.map(layer -> {
					String name = layer.getString("name");
					TileLayer blLayer = new TileLayer(name, width, height, info.getConfig(), info.getTilesetImage());
					JSONArray tiledata = layer.getJSONArray("data");
					for (int i = 0; i < tiledata.length(); i++) {
						int tileX = i % width;
						int tileY = i / width;
						int tile = tiledata.getInt(i);
						if (tile > 0) tile--;
						blLayer.setTile(tileX, tileY, tile);
					}
					return blLayer;
				}).collect(Collectors.toList());
	}
}
