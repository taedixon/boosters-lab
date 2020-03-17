package ca.noxid.lab;

import ca.noxid.lab.gameinfo.GameInfo;
import com.carrotlord.string.StrTools;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class BlConfig {
	private static final String CONFIGNAME = "bl.ini";
	private int lineResolution = 4;
	private int entityResolution = 16;
	private int tileSize = 16;
	private int tilesetWidth = 16;
	private int mapMinX = 21;
	private int mapMinY = 16;
	private boolean useScriptSource = true;
	private String tilesetPrefix = "Prt";
	private String npcPrefix = "Npc";
	private String backgroundPrefix = "bk";
	private int gradientLayerAlpha = 50;
	private String encoding = "UTF-8";
	private String imageExtension = ".pbm";

	private static final String[] fluff = {
			" - Line resolution",
			" - Entity Resolution",
			" - Tile Size",
			" - Tileset Width",
			" - Map min x",
			" - Map min y",
			" - Use ScriptSource files",
			" - Identifies file as a tileset",
			" - Identifies file as an NPC sheet",
			" - Identifies file as a background",
			" - Used for the alpha slider",
			" - Character encoding",
			" - Image extension",
	};

	private File configFile;

	public int getTileSize() {
		return tileSize;
	}

	public int getEntityRes() {
		return entityResolution;
	}

	public int getLineRes() {
		return lineResolution;
	}

	public int getTilesetWidth() {
		return tilesetWidth;
	}

	public int getMapMinX() {
		return mapMinX;
	}

	public int getMapMinY() {
		return mapMinY;
	}

	public boolean getUseScriptSource() {
		return useScriptSource;
	}

	public String getTilesetPrefix() {
		return tilesetPrefix;
	}

	public String getNpcPrefix() {
		return npcPrefix;
	}

	public String getBackgroundPrefix() {
		return backgroundPrefix;
	}

	public int getGradientAlpha() {
		return gradientLayerAlpha;
	}

	public void setGradientAlpha(int val) {
		if (val < 0) val = 0;
		if (val > 100) val = 100;
		gradientLayerAlpha = val;
	}

	public String getImageExtension() {
		return imageExtension;
	}

	public void setImageExtension(String imageExtension) {
		this.imageExtension = imageExtension;
	}

	public String getEncoding() {
		return encoding;
	}

	public void set(String[] vals) {
		if (vals.length >= 6) {
			lineResolution = Integer.parseInt(vals[0]);
			entityResolution = Integer.parseInt(vals[1]);
			tileSize = Integer.parseInt(vals[2]);
			tilesetWidth = Integer.parseInt(vals[3]);
			mapMinX = Integer.parseInt(vals[4]);
			mapMinY = Integer.parseInt(vals[5]);
		}
		if (vals.length >= 10) {
			useScriptSource = Boolean.parseBoolean(vals[6]);
			tilesetPrefix = vals[7];
			npcPrefix = vals[8];
			backgroundPrefix = vals[9];
		}
		if (vals.length >= 11) {
			gradientLayerAlpha = Integer.parseInt(vals[10]);
		}
		if (vals.length >= 12) {
			encoding = vals[11];
		}
		if (vals.length >= 13) {
			imageExtension = vals[12];
		}
	}

	public BlConfig(File configFolder, GameInfo.MOD_TYPE type) {
		configFile = solveLegacyDirectory(configFolder);
		if (type == GameInfo.MOD_TYPE.MOD_CS) {
			tileSize = 16;
		}
		if (configFile.exists()) {
			ArrayList<String> configValues = new ArrayList<>();
			try (Scanner sc = new Scanner(configFile)) {
				while (sc.hasNextLine())
					configValues.add(sc.nextLine());
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			set(configValues.toArray(new String[0]));
		}
	}

	public void save() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
			Object[] vals = {
					lineResolution, entityResolution, tileSize, tilesetWidth,
					mapMinX, mapMinY, useScriptSource, tilesetPrefix, npcPrefix, backgroundPrefix,
					gradientLayerAlpha, encoding, imageExtension
			};
			for (int i = 0; i < vals.length; i++) {
				out.write(vals[i] + fluff[i] + System.lineSeparator());
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			StrTools.msgBox("Error writing to " + configFile + " !");
		}
	}

	/**
	 * Previously. bl.ini was stored directly in the project's Data Directory.
	 * Attempt to resolve this
	 * @param dataDir
	 * @return
	 */
	private File solveLegacyDirectory(File dataDir) {
		File canonicalFile = new File(dataDir, ".boostlab/" + CONFIGNAME);
		if (canonicalFile.exists()) {
			if (canonicalFile.isDirectory())
				// you what
				canonicalFile.delete();
			return canonicalFile;
		}
		// the new-style project directory may not exist. If it doesn't, create it.
		canonicalFile.getParentFile().mkdirs();
		File legacyFile = new File(dataDir, CONFIGNAME);
		if (legacyFile.exists()) {
			try {
				canonicalFile.getParentFile().mkdirs();
				Files.copy(legacyFile.toPath(), canonicalFile.toPath(), REPLACE_EXISTING);
				return canonicalFile;
			} catch (IOException e) {
				e.printStackTrace();
				return legacyFile;
			}
		} else {
			// there is no ini file..
			return canonicalFile;
		}
	}
}
