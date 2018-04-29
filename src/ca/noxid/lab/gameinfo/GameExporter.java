package ca.noxid.lab.gameinfo;

import ca.noxid.lab.Messages;
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
			throw new IOException(Messages.getString("GameExporter.0")); //$NON-NLS-1$
		}
		switch (game.type) {
		case MOD_CS:
			File curLoc = game.getExe().getFile();
			File curData = game.getDataDirectory();
			
			
			File newLoc = new File(dir + File.separator + curLoc.getName());
			File newdata = new File(dir + "/data"); //$NON-NLS-1$
			File newNpc = new File(newdata + "/Npc"); //$NON-NLS-1$
			newNpc.mkdirs();
			File newStage = new File(newdata + "/Stage"); //$NON-NLS-1$
			newStage.mkdirs();
			Files.copy(curLoc.toPath(), newLoc.toPath(),
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES);
			
			//config shit
			File docon = new File(curLoc.getParent() + "/DoConfig.exe"); //$NON-NLS-1$
			if (docon.exists()) {
				File newcon = new File(newLoc.getParent() + "/DoConfig.exe"); //$NON-NLS-1$
				Files.copy(docon.toPath(), newcon.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
			}
			File conf = new File(curLoc.getParent() + "/Config.dat"); //$NON-NLS-1$
			if (conf.exists()) {
				File newconf = new File(newLoc.getParent() + "/Config.dat"); //$NON-NLS-1$
				Files.copy(conf.toPath(), newconf.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.COPY_ATTRIBUTES);
			}
			
			
			Set<File> copiedFiles = new HashSet<>();
			//copy all the trash in the data root
			String[] names = new String[] {
					"Arms" + game.getImgExtension(), //$NON-NLS-1$
					"ArmsImage" + game.getImgExtension(), //$NON-NLS-1$
					"ArmsItem.tsc", //$NON-NLS-1$
					"Bullet" + game.getImgExtension(), //$NON-NLS-1$
					"Caret" + game.getImgExtension(), //$NON-NLS-1$
					"casts" + game.getImgExtension(), //$NON-NLS-1$
					"Credit.tsc", //$NON-NLS-1$
					"Face" + game.getImgExtension(), //$NON-NLS-1$
					"Fade" + game.getImgExtension(), //$NON-NLS-1$
					"Head.tsc", //$NON-NLS-1$
					"ItemImage" + game.getImgExtension(), //$NON-NLS-1$
					"Loading" + game.getImgExtension(), //$NON-NLS-1$
					"MyChar" + game.getImgExtension(), //$NON-NLS-1$
					"npc.tbl", //$NON-NLS-1$
					"StageImage" + game.getImgExtension(), //$NON-NLS-1$
					"StageSelect.tsc", //$NON-NLS-1$
					"TextBox" + game.getImgExtension(), //$NON-NLS-1$
					"Title" + game.getImgExtension(), //$NON-NLS-1$
					"/Npc/" + game.getConfig().getNpcPrefix() + "Regu" + game.getImgExtension(), //$NON-NLS-1$ //$NON-NLS-2$
					"/Npc/" + game.getConfig().getNpcPrefix() + "Sym" + game.getImgExtension(), //$NON-NLS-1$ //$NON-NLS-2$
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
				mapfiles.put(new File(curData + "/Npc/" + game.getConfig().getNpcPrefix() + data.getNPC1() + game.getImgExtension()), //$NON-NLS-1$
						new File(newNpc + File.separator + game.getConfig().getNpcPrefix() + data.getNPC1() + game.getImgExtension()));
				mapfiles.put(new File(curData + "/Npc/" + game.getConfig().getNpcPrefix() + data.getNPC2() + game.getImgExtension()), //$NON-NLS-1$
						new File(newNpc + File.separator + game.getConfig().getNpcPrefix() + data.getNPC2() + game.getImgExtension()));
				mapfiles.put(new File(curData + "/Stage/" + game.getConfig().getTilesetPrefix() + data.getTileset() + game.getImgExtension()), //$NON-NLS-1$
						new File(newStage + File.separator + game.getConfig().getTilesetPrefix() + data.getTileset() + game.getImgExtension()));
				mapfiles.put(new File(curData + "/Stage/" + data.getTileset() + ".pxa"), //$NON-NLS-1$ //$NON-NLS-2$
						new File(newStage + File.separator + data.getTileset() + ".pxa")); //$NON-NLS-1$
				mapfiles.put(new File(curData + "/Stage/" + data.getFile() + ".pxm"), //$NON-NLS-1$ //$NON-NLS-2$
						new File(newStage + File.separator + data.getFile() + ".pxm")); //$NON-NLS-1$
				mapfiles.put(new File(curData + "/Stage/" + data.getFile() + ".pxe"), //$NON-NLS-1$ //$NON-NLS-2$
						new File(newStage + File.separator + data.getFile() + ".pxe")); //$NON-NLS-1$
				mapfiles.put(new File(curData + "/Stage/" + data.getFile() + ".tsc"), //$NON-NLS-1$ //$NON-NLS-2$
						new File(newStage + File.separator + data.getFile() + ".tsc")); //$NON-NLS-1$
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
			JOptionPane.showMessageDialog(null, Messages.getString("GameExporter.1")); //$NON-NLS-1$
			break;
		case MOD_KS:
			JOptionPane.showMessageDialog(null, Messages.getString("GameExporter.1")); //$NON-NLS-1$
			break;
		case MOD_MR:
			JOptionPane.showMessageDialog(null, Messages.getString("GameExporter.1")); //$NON-NLS-1$
			break;
		default:
			JOptionPane.showMessageDialog(null, Messages.getString("GameExporter.2")); //$NON-NLS-1$
			break;
		
		}
	}
}
