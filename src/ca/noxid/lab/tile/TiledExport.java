package ca.noxid.lab.tile;

import ca.noxid.lab.BlConfig;
import ca.noxid.lab.mapdata.MapInfo;

public class TiledExport {

	String outerTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<map version=\"1.2\" tiledversion=\"1.2.4\" orientation=\"orthogonal\" \n" +
			"  renderorder=\"right-down\" width=\"%d\" height=\"%d\" tilewidth=\"%d\" \n" +
			"  tileheight=\"%d\" infinite=\"0\" nextlayerid=\"%d\" nextobjectid=\"1\">\n" +
			"  <tileset firstgid=\"0\" source=\"%s.tsx\"/>" +
			"  %s" +
			"</map>";
	String layerTemplate = "<layer id=\"%d\" name=\"%s\" width=\"%d\" height=\"%d\">" +
			"<data encoding=\"csv\">\n%s</data></layer>";

	public String getTiledXml(MapInfo map) {
		StringBuilder sb = new StringBuilder();
		int i = 1;
		for (TileLayer layer : map.getMap()) {
			sb.append(String.format(layerTemplate,
					i,
					layer.getName(),
					layer.getWidth(),
					layer.getHeight(),
					toCSV(layer)));
			i++;
		}
		BlConfig config = map.getConfig();
		return String.format(outerTemplate,
				map.getMapX(),
				map.getMapY(),
				config.getTileSize(),
				config.getTileSize(),
				i,
				map.getTileset(),
				sb.toString());
	}

	private String toCSV(TileLayer layer) {
		StringBuilder sb = new StringBuilder();
		for (int y = 0; y < layer.getHeight(); y++) {
			for (int x = 0; x < layer.getWidth(); x++) {
				sb.append(layer.getTile(x, y));
				if (x < layer.getWidth()-1 || y < layer.getHeight()-1) {
					sb.append(',');
				}
			}
			sb.append('\n');
		}
		return  sb.toString();
	}
}
