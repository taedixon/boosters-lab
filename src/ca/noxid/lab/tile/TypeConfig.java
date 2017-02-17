package ca.noxid.lab.tile;

import ca.noxid.lab.EditorApp;
import com.carrotlord.string.StrTools;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TypeConfig {

	private static Map<Integer, TypeConfig> linetypeMap = initLinetypes();
	public final int id;
	public final String typeName;
	public final LINE_MODE linemode;
	public final Color topColour;
	public final Color bottomColour;
	TypeConfig(int num, String mode, String nam, String c1, String c2) {
		id = num;
		typeName = nam;
		topColour = Color.decode(c1);
		bottomColour = Color.decode(c2);
		if ("poly".equals(mode)) {
			linemode = LINE_MODE.POLYLINE;
		} else {
			linemode = LINE_MODE.SEGMENT;
		}
	}

	private static Map<Integer, TypeConfig> initLinetypes() {
		if (EditorApp.EDITOR_MODE != 2) {
			return new HashMap<>();
		}
		HashMap<Integer, TypeConfig> rv = new HashMap<>();
		rv.put(1, new TypeConfig(1, "Solid", "line", "0x0", "0xFF0000"));
		File lineConfigFile = new File("linetypes.txt");
		Scanner sc;
		try {
			sc = new Scanner(lineConfigFile);
			while (sc.hasNextLine()) {
				String[] tokens = sc.nextLine().split("\\s+");
				try {
					rv.put(Integer.parseInt(tokens[0]),
							new TypeConfig(Integer.parseInt(tokens[0]), tokens[1], tokens[2], tokens[3], tokens[4]));
				} catch (NumberFormatException err) {
					//whateverrrrrrrrr
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			StrTools.msgBox("Could not find linetypes.txt");
		}

		return rv;
	}

	public static TypeConfig getType(int id) {
		return linetypeMap.get(id);
	}

	public static TypeConfig[] getTypes() {
		Collection<TypeConfig> values = linetypeMap.values();
		return values.toArray(new TypeConfig[values.size()]);
	}

	public String toString() {
		return id + " - " + typeName;
	}

	public enum LINE_MODE {
		SEGMENT, POLYLINE
	}
}