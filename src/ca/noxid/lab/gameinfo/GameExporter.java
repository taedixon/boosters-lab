package ca.noxid.lab.gameinfo;

import ca.noxid.lab.mapdata.Mapdata;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class GameExporter {
	GameInfo game;

	public GameExporter(GameInfo info) {
		game = info;
	}
	
	public void exportTo(File dir) throws IOException {
		if (game == null) {
			throw new IOException("No game to copy");
		}
		switch (game.type) {
		case MOD_CS:
			File curLoc = game.getExe().getFile();
			File curData = game.getDataDirectory();
			
			
			File newLoc = new File(dir + File.separator + curLoc.getName());
			File newdata = new File(dir + "/data");
			File newNpc = new File(newdata + "/Npc");
			newNpc.mkdirs();
			File newStage = new File(newdata + "/Stage");
			newStage.mkdirs();
			Files.copy(curLoc.toPath(), newLoc.toPath(),
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES);
			
			//config shit
			File docon = new File(curLoc.getParent() + "/DoConfig.exe");
			if (docon.exists()) {
				File newcon = new File(newLoc.getParent() + "/DoConfig.exe");
				Files.copy(docon.toPath(), newcon.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
			}
			File conf = new File(curLoc.getParent() + "/Config.dat");
			if (conf.exists()) {
				File newconf = new File(newLoc.getParent() + "/Config.dat");
				Files.copy(conf.toPath(), newconf.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
			}
			
			
			Set<File> copiedFiles = new HashSet<>();
			//copy all the trash in the data root
			String[] names = new String[] {
					"Arms" + game.getImgExtension(),
					"ArmsImage" + game.getImgExtension(),
					"ArmsItem.tsc",
					"Bullet" + game.getImgExtension(),
					"Caret" + game.getImgExtension(),
					"casts" + game.getImgExtension(),
					"Credit.tsc",
					"Face" + game.getImgExtension(),
					"Fade" + game.getImgExtension(),
					"Head.tsc",
					"ItemImage" + game.getImgExtension(),
					"Loading" + game.getImgExtension(),
					"MyChar" + game.getImgExtension(),
					"npc.tbl",
					"StageImage" + game.getImgExtension(),
					"StageSelect.tsc",
					"TextBox" + game.getImgExtension(),
					"Title" + game.getImgExtension(),
					"/Npc/" + game.getConfig().getNpcPrefix() + "Regu" + game.getImgExtension(),
					"/Npc/" + game.getConfig().getNpcPrefix() + "Sym" + game.getImgExtension(),
			};
			
			for (String n : names) {
				File cf = new File(curData + File.separator + n);
				File nf = new File(newdata + File.separator + n);
				Files.copy(cf.toPath(), nf.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
			}
			
			//copy the maps files
			for (Mapdata data : game.getMapdata()) {
				HashMap<File,File> mapfiles = new HashMap<>();
				mapfiles.put(new File(curData + File.separator + data.getBG() + game.getImgExtension()),
						new File(newdata + File.separator + data.getBG() + game.getImgExtension()));
				mapfiles.put(new File(curData + "/Npc/" + game.getConfig().getNpcPrefix() + data.getNPC1() + game.getImgExtension()),
						new File(newNpc + File.separator + game.getConfig().getNpcPrefix() + data.getNPC1() + game.getImgExtension()));
				mapfiles.put(new File(curData + "/Npc/" + game.getConfig().getNpcPrefix() + data.getNPC2() + game.getImgExtension()),
						new File(newNpc + File.separator + game.getConfig().getNpcPrefix() + data.getNPC2() + game.getImgExtension()));
				mapfiles.put(new File(curData + "/Stage/" + game.getConfig().getTilesetPrefix() + data.getTileset() + game.getImgExtension()),
						new File(newStage + File.separator + game.getConfig().getTilesetPrefix() + data.getTileset() + game.getImgExtension()));
				mapfiles.put(new File(curData + "/Stage/" + data.getTileset() + ".pxa"),
						new File(newStage + File.separator + data.getTileset() + ".pxa"));
				mapfiles.put(new File(curData + "/Stage/" + data.getFile() + ".pxm"),
						new File(newStage + File.separator + data.getFile() + ".pxm"));
				mapfiles.put(new File(curData + "/Stage/" + data.getFile() + ".pxe"),
						new File(newStage + File.separator + data.getFile() + ".pxe"));
				mapfiles.put(new File(curData + "/Stage/" + data.getFile() + ".tsc"),
						new File(newStage + File.separator + data.getFile() + ".tsc"));
				for (File f : mapfiles.keySet()) {
					if (!copiedFiles.contains(f)) {
						copiedFiles.add(f);
						Files.copy(f.toPath(), mapfiles.get(f).toPath(),
								StandardCopyOption.REPLACE_EXISTING,
								StandardCopyOption.COPY_ATTRIBUTES);
					}
				}
			}
			break;
		case MOD_CS_PLUS:
			JOptionPane.showMessageDialog(null, "Not implemented");
			break;
		case MOD_KS:
			JOptionPane.showMessageDialog(null, "Not implemented");
			break;
		case MOD_MR:
			JOptionPane.showMessageDialog(null, "Not implemented");
			break;
		default:
			JOptionPane.showMessageDialog(null, "Unknown type");
			break;
		
		}
	}
}
