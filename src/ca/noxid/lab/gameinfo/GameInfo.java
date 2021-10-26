package ca.noxid.lab.gameinfo;
import ca.noxid.lab.BlConfig;
import ca.noxid.lab.EditorApp;
import ca.noxid.lab.FileSuffixFilter;
import ca.noxid.lab.Messages;
import ca.noxid.lab.entity.EntityData;
import ca.noxid.lab.mapdata.Mapdata;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.lab.script.TscLexer;
import ca.noxid.lab.script.TscPane;
import ca.noxid.lab.script.TscToken;
import com.carrotlord.string.StrTools;

import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;


public class GameInfo {
	//Map list variables
	private Vector<Mapdata> mapdataStore;
	private Vector<Mapdata> tempMapdata = new Vector<>();
	private File base;
	private File dataDir;
	private BlConfig gameConfig;
	private static final boolean GIRS_SPECIAL_MAP_SORT = false;
	
	private static final String entityInfo_head = "//num	short1	short2	long rect desc category\r\n" +
			"// tab delimited\r\n" +
			"//Contributors: Wistil (cannibalized CE's entity list)\r\n" +
			"//		Noxid (most of entities 0-180 & some more)\r\n" +
			"//		Carrotlord (rects for entities 180+)\r\n" + 
			"//     Bombchu Link (updated and refreshed framarects.)\r\n";
	
	public BlConfig getConfig() {return gameConfig;}
	
	//public static int NUM_ENTITY_TYPE;
	Map<String, EntityCategory> categoryMap;
	private ArrayList<EntityData> masterEntityList = new ArrayList<>(); //TODO NASTY HACKS
	public EntityData[] getAllEntities() {return masterEntityList.toArray(new EntityData[masterEntityList.size()]);}
	public void setEntities(ArrayList<EntityData> dataCopy) {
		masterEntityList = dataCopy;
		populateCategoryLists();
	}
	private CSExe executable;
	
	public boolean canPatch() {return executable != null;}
	
	public void patch(ByteBuffer data, int offset) {
		if (executable != null) {
			executable.patch(data, offset);
		} else {
			StrTools.msgBox(Messages.getString("GameInfo.0")); //$NON-NLS-1$
		}
	}
	
	//various static npc image files
	private File mycharFile;
	private File npcReguFile;
	private File npcSymFile;
	private File armsImageFile;
	private File faceFile;
	private File itemImageFile;
	
	//getter and setter
	public File getDataDirectory() {return dataDir;}

	/**
	 * Use <code>{@link #getConfig()}.{@link BlConfig#getImageExtension() getImageExtension()}</code> instead.
	 */
	@Deprecated
	public String getImgExtension() {return gameConfig.getImageExtension();}
	public Mapdata getMapdata(int i) {return mapdataStore.get(i);}
	public Mapdata[] getMapdata() {return mapdataStore.toArray(new Mapdata[mapdataStore.size()]);}
	public Mapdata getMapdataTemp(int i) {return tempMapdata.get(i);}
	public File getMyCharFile() {return mycharFile;}
	public File getNpcRegu() {return npcReguFile;}
	public File getNpcSym() {return npcSymFile;}
	public File getArmsImageFile() {return armsImageFile;}
	public File getFaceFile() {return faceFile;}
	public File getItemImageFile() {return itemImageFile;}
	
	public enum MOD_TYPE {MOD_CS, MOD_KS, MOD_CS_PLUS, MOD_MR, MOD_GUXT, DUMMY}
	public MOD_TYPE type;
	
	public static final String[] sfxNames = loadSfxNames();

	public File getBase() {
		return base;
	}
	
	public GameInfo(File base) throws IOException {
		this.base = base;
		mapdataStore = new Vector<>();
		categoryMap = new HashMap<>();
		String defaultImageExt = ".pbm";
		if (base.toString().endsWith(".exe")) { //$NON-NLS-1$
			dataDir = new File(base.getParent() + "/data"); //$NON-NLS-1$
			if (dataDir.list(new FileSuffixFilter("stprj")).length > 0) {
				type = MOD_TYPE.MOD_GUXT;
                // I don't actually think this ever worked properly, since it's going to certainly damage the EXE in modern versions.
                // Better to have executable be null in this case.
				// executable = new GuxtExe(base, gameConfig.getEncoding());
			} else {
				type = MOD_TYPE.MOD_CS;
				try {
                    executable = new CSExe(base); //can fix swdata
                    getExeData(executable);
                    defaultImageExt = ".bmp";
                } catch (Exception ioe) {
				    ioe.printStackTrace();
                    StrTools.msgBox(Messages.getString("GameInfo.95")); //$NON-NLS-1$
                    // executable may be null.
                }
			}
		} else if (base.toString().endsWith(".tbl")){ //$NON-NLS-1$
			type = MOD_TYPE.MOD_CS_PLUS;
			dataDir = base.getParentFile();
            defaultImageExt = ".bmp";
		} else if (base.toString().endsWith(".bin")) { //$NON-NLS-1$
			if (base.getName().equals("mrmap.bin")) { //$NON-NLS-1$
				//moustache
				type = MOD_TYPE.MOD_MR;
				dataDir = base.getParentFile();
                defaultImageExt = ".png";
			} else {
				type = MOD_TYPE.MOD_KS;
				dataDir = base.getParentFile();
                defaultImageExt = ".png";
			}
		} else if (base.toString().endsWith(".pxm")) { //$NON-NLS-1$
			type = MOD_TYPE.MOD_CS_PLUS;
			dataDir = base.getParentFile().getParentFile();
            defaultImageExt = ".bmp";
		} else if (base.toString().endsWith(".csmap")) { //$NON-NLS-1$
			type = MOD_TYPE.MOD_CS;
			dataDir = base.getParentFile().getParentFile();
            defaultImageExt = ".bmp";
		}
		gameConfig = new BlConfig(dataDir, type);
		String imageExtension = gameConfig.getImageExtension();
		if (".pbm".equals(imageExtension))
			gameConfig.setImageExtension((imageExtension = defaultImageExt));
		fillMapdata(base);
		mycharFile = new File(dataDir + "/MyChar" + imageExtension); //$NON-NLS-1$
		mycharFile = ResourceManager.checkBase(mycharFile);
		npcReguFile = new File(dataDir + "/Npc/NpcRegu" + imageExtension); //$NON-NLS-1$
		npcReguFile = ResourceManager.checkBase(npcReguFile);
		npcSymFile = new File(dataDir + "/Npc/NpcSym" + imageExtension); //$NON-NLS-1$
		npcSymFile = ResourceManager.checkBase(npcSymFile);
		itemImageFile = new File(dataDir + "/ItemImage" + imageExtension); //$NON-NLS-1$
		itemImageFile = ResourceManager.checkBase(itemImageFile);
		faceFile = new File(dataDir + "/Face" + imageExtension); //$NON-NLS-1$
		faceFile = ResourceManager.checkBase(faceFile);
		armsImageFile = new File(dataDir + "/ArmsImage" + imageExtension); //$NON-NLS-1$
		armsImageFile = ResourceManager.checkBase(armsImageFile);
		loadNpcTbl(ResourceManager.checkBase(new File(dataDir + "/npc.tbl"))); //$NON-NLS-1$
	}
	
	public static void writeDefaultFiles(File dest) {
		if (!dest.isDirectory()) {
			try {
				FileOutputStream oStream = new FileOutputStream(dest);
				FileChannel chan = oStream.getChannel();
				Mapdata dummyMap = new Mapdata(0);
				ByteBuffer dBuf = dummyMap.toBuf(MOD_TYPE.MOD_KS, "UTF-8");
				ByteBuffer nMaps = ByteBuffer.allocate(4);
				nMaps.put(0, (byte) 1);
				chan.write(nMaps);
				chan.write(dBuf);
				chan.close();
				oStream.close();
			} catch (IOException err) {
				err.printStackTrace();
				StrTools.msgBox(Messages.getString("GameInfo.6")); //$NON-NLS-1$
				return;
			}
			
			//setup the data structure
			File dataDir = dest.getParentFile();
			File stageDir = new File(dataDir + "/Stage"); //$NON-NLS-1$
			if (!stageDir.exists()) {
				stageDir.mkdirs();
			}
			File npcDir = new File(dataDir + "/Npc"); //$NON-NLS-1$
			if (!npcDir.exists()) {
				npcDir.mkdirs();
			}
			//write all default files
			File[] rawArray = new File[] {
					new File(dataDir + "/bk0.png"), //$NON-NLS-1$
					new File(stageDir + "/Prt0.png"), //$NON-NLS-1$
					new File(stageDir + "/0.pxa"), //$NON-NLS-1$
					new File(npcDir + "/Npc0.png"), //$NON-NLS-1$
					new File(npcDir + "/NpcRegu.png"), //$NON-NLS-1$
					new File(npcDir + "/NpcSym.png"), //$NON-NLS-1$
					new File(dataDir + "/MyChar.png"), //$NON-NLS-1$
			};
			String[] names = new String[] {
					"bk0.png", //$NON-NLS-1$
					"Prt0.png", //$NON-NLS-1$
					"0.pxa", //$NON-NLS-1$
					"Npc0.png", //$NON-NLS-1$
					"NpcRegu.png", //$NON-NLS-1$
					"NpcSym.png", //$NON-NLS-1$
					"MyChar.png" //$NON-NLS-1$
			};
			try {
				for (int i = 0; i < rawArray.length; i++) {
					File outFile = rawArray[i];
					InputStream is = EditorApp.class.getResourceAsStream("rsrc/" +names[i]); //$NON-NLS-1$
					BufferedOutputStream fs = new BufferedOutputStream(
							new FileOutputStream(outFile));
					int c;
					while ((c = is.read()) != -1) fs.write(c);
					fs.close();
					is.close();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				StrTools.msgBox(Messages.getString("GameInfo.24")); //$NON-NLS-1$
			} catch (IOException e) {
				e.printStackTrace();
				StrTools.msgBox(Messages.getString("GameInfo.25")); //$NON-NLS-1$
			}
		}			
	}
	private static String[] loadSfxNames() {
		File sfxFile = new File("sfxList.txt"); //$NON-NLS-1$
		String[] results = new String[0];
		ArrayList<String> lineHolder = new ArrayList<>();
		try {
			Scanner sc = new Scanner(sfxFile);
			//String currentLine;
			while (sc.hasNextLine()) {
				lineHolder.add(sc.nextLine());				
			}
			sc.close();
			results = lineHolder.toArray(new String[lineHolder.size()]);
		} catch (FileNotFoundException err) {
			StrTools.msgBox(Messages.getString("GameInfo.14")); //$NON-NLS-1$
		}		
		return results;
	}
	
	public void loadImages(ResourceManager iMan) {
		iMan.reloadImage(mycharFile, 1);
		iMan.reloadImage(npcReguFile, 1);
		iMan.reloadImage(npcSymFile, 1);
		iMan.reloadImage(itemImageFile, 1);
		iMan.reloadImage(faceFile, 1);
		iMan.reloadImage(armsImageFile, 1);
	}
	
	public String[] getMapNames() {
		ArrayList<Mapdata> mapdataCopy = new ArrayList<>();
		mapdataCopy.addAll(mapdataStore);
		if (GIRS_SPECIAL_MAP_SORT) {
			Collections.sort(mapdataCopy, new Comparator<Mapdata>() {
				@Override
				public int compare(Mapdata o1, Mapdata o2) {
					// TODO Auto-generated method stub
					return o1.getFile().toLowerCase().compareTo(o2.getFile().toLowerCase());
				}				
			});
		}
		String[] retVal = new String[mapdataCopy.size()];
		for (int i = 0; i < retVal.length; i++) {
			retVal[i] = mapdataCopy.get(i).toString();
		}
		return retVal;
	}
	
	public File getScriptFile(int mapNum) {
		Mapdata currentMap = mapdataStore.get(mapNum);
		File script = new File(dataDir + File.separator + 
				"Stage" + File.separator + currentMap.getFile() + ".tsc"); //$NON-NLS-1$ //$NON-NLS-2$
		return ResourceManager.checkBase(script);
	}
	
	public File getScriptSource(int mapNum) {
		Mapdata currentMap = mapdataStore.get(mapNum);
		File source = new File(dataDir + File.separator + 
				"Stage" + File.separator +  //$NON-NLS-1$
				"ScriptSource" + File.separator + currentMap.getFile() + ".txt");  //$NON-NLS-1$ //$NON-NLS-2$
		return ResourceManager.checkBase(source);
	}
	
	public String getShortName(int map) {
		return mapdataStore.get(map).getFile();
	}	
	public String getLongName(int map) {
		return mapdataStore.get(map).getMapname();
	}
	
	public String[] getTilesets() {
		String imageExtension = gameConfig.getImageExtension();
		ArrayList<String> flist = new ArrayList<>();
		File stageDir = new File(dataDir + "/Stage"); //$NON-NLS-1$
		File[] fileList = stageDir.listFiles(new TilesetFilter());
		for (File f : fileList) {
			flist.add(f.getName().replace(imageExtension, "")); //$NON-NLS-1$
		}
		File baseDir = ResourceManager.getBaseFolder(dataDir);
		if (baseDir != null) {
			stageDir = new File(baseDir + "/Stage"); //$NON-NLS-1$

			fileList = stageDir.listFiles(new TilesetFilter());
			if (fileList != null) {
				for (File f : fileList) {
					flist.add(f.getName().replace(imageExtension, "")); //$NON-NLS-1$
				} 
			} 
		}
		return flist.toArray(new String[flist.size()]);
	}
	public String[] getNpcSheets() {
		String imageExtension = gameConfig.getImageExtension();
		ArrayList<String> flist = new ArrayList<>();
		File stageDir = new File(dataDir + "/Npc"); //$NON-NLS-1$
		File[] fileList = stageDir.listFiles(new NpcFilter());
		if (fileList != null) {
			for (File f : fileList) {
				flist.add(f.getName().replace(imageExtension, "")); //$NON-NLS-1$
			} 
		} 
		
		File baseDir = ResourceManager.getBaseFolder(dataDir);
		if (baseDir != null) {
			stageDir = new File(baseDir + "/Npc"); //$NON-NLS-1$

			fileList = stageDir.listFiles(new NpcFilter());
			if (fileList != null) {
				for (File f : fileList) {
					flist.add(f.getName().replace(imageExtension, "")); //$NON-NLS-1$
				} 
			} 
		}
		return flist.toArray(new String[flist.size()]);
	}
	
	public String[] getBackgrounds() {
		String imageExtension = gameConfig.getImageExtension();
		ArrayList<String> flist = new ArrayList<>();
		File[] fileList = dataDir.listFiles(new BackgroundFilter());
		for (File f : fileList) {
			flist.add(f.getName().replace(imageExtension, "")); //$NON-NLS-1$
		}

		
		File baseDir = ResourceManager.getBaseFolder(dataDir);
		if (baseDir != null) {
			fileList = baseDir.listFiles(new BackgroundFilter());
			if (fileList != null) {
				for (File f : fileList) {
					flist.add(f.getName().replace(imageExtension, "")); //$NON-NLS-1$
				} 
			} 
		}
		return flist.toArray(new String[flist.size()]);
	}
	
	private void loadNpcTbl(File tblFile) {
		FileChannel inChan;
		ByteBuffer dBuf;
		FileInputStream inStream;
		int calculated_npcs;
		
		if (tblFile == null || !tblFile.exists()) {
			//generate default npc.tbl
			try {
				tblFile = new File(dataDir + File.separator + "npc.tbl"); //$NON-NLS-1$
				InputStream is = ResourceManager.class.getResourceAsStream("npc.tbl"); //$NON-NLS-1$
				BufferedOutputStream fs = new BufferedOutputStream(
						new FileOutputStream(tblFile));
				int c;
				while ((c = is.read()) != -1) fs.write(c);
				fs.close();
				is.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		try {
			inStream = new FileInputStream(tblFile);
			calculated_npcs = (int) (tblFile.length() / 24);
			inChan = inStream.getChannel();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		short[] flagDat;
		short[] healthDat;
		byte[] tilesetDat;
		byte[] deathDat;
		byte[] hurtDat;
		byte[] sizeDat;
		int[] expDat;
		int[] damageDat;
		byte[] hitboxDat;
		byte[] displayDat;
		
		//String[] nameDat = new String[NUM_ENTITY_TYPE];
		//Rectangle[] frameDat = new Rectangle[NUM_ENTITY_TYPE];
		try {
			//read flags section
			dBuf = ByteBuffer.allocateDirect(2*calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			flagDat = new short[calculated_npcs];
			for (int i = 0; i < flagDat.length; i++) {
				flagDat[i] = dBuf.getShort();
			}
	
			//read health section
			dBuf = ByteBuffer.allocate(2*calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			healthDat = new short[calculated_npcs];
			for (int i = 0; i < healthDat.length; i++) {
				healthDat[i] = dBuf.getShort();
			}
	
			//read tileset section
			dBuf = ByteBuffer.allocate(calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			tilesetDat = dBuf.array();
	
			//read death sound section
			dBuf = ByteBuffer.allocate(calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			deathDat = dBuf.array();
	
			//read hurt sound section
			dBuf = ByteBuffer.allocate(calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			hurtDat = dBuf.array();
	
			//read size section
			dBuf = ByteBuffer.allocate(calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			sizeDat = dBuf.array();
	
			//read experience section
			dBuf = ByteBuffer.allocate(4*calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			expDat = new int[calculated_npcs];
			for (int i = 0; i < expDat.length; i++) {
				expDat[i] = dBuf.getInt();
			}
	
			//read damage section
			dBuf = ByteBuffer.allocate(4*calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			damageDat = new int[calculated_npcs];
			for (int i = 0; i < damageDat.length; i++) {
				damageDat[i] = dBuf.getInt();
			}
	
			//read hitbox section
			dBuf = ByteBuffer.allocate(4*calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			hitboxDat = dBuf.array();
	
			//read hitbox section
			dBuf = ByteBuffer.allocate(4*calculated_npcs);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			displayDat = dBuf.array();
			//finished reading file
			inChan.close();
			inStream.close();
			
			//build the master list
			for (int i = 0; i < calculated_npcs; i++) {
				EntityData e = new EntityData(i, damageDat[i], deathDat[i],
						expDat[i], flagDat[i], healthDat[i], hurtDat[i],
						sizeDat[i], tilesetDat[i], 
						new Rectangle( displayDat[i*4],
							displayDat[i*4 + 1],
							displayDat[i*4 + 2],
							displayDat[i*4 + 3]	),
						new Rectangle( hitboxDat[i*4],
							hitboxDat[i*4 + 1],
							hitboxDat[i*4 + 2],
							hitboxDat[i*4 + 3] ) );
				masterEntityList.add(i, e);
			}
			
			/*GROSS NASTY HAcK PLS IGNORE
			if (calculated_npcs < NUM_ENTITY_TYPE)
				for (int i = calculated_npcs; i < NUM_ENTITY_TYPE; i++) {
					EntityData e = new EntityData(i, 0, 0, 0, 0, 10, 0, 1, 1, new Rectangle(8, 8, 8, 8), new Rectangle(4, 4, 4, 4));
					allCat.addEntity(e, "All");
					masterEntityList.add(i, e);
				}
				*/
			
			
			//now read supplementary info from metadata file
			Scanner sc = new Scanner(new File("entityInfo.txt")); //$NON-NLS-1$
			int lineNum = 0;
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				lineNum++;
				if (line.equals("") || line.startsWith("/")) //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				if (line.startsWith("#")) { //$NON-NLS-1$
					try {
						//entity number entry
						//System.out.println(line);
						line = line.substring(1); //throw away the marker
						line = line.substring(0, line.indexOf(';')); //throw away the trailing garbage
						Scanner lineScan = new Scanner(line);
						lineScan.useDelimiter("\\t+"); //$NON-NLS-1$
						int entityNum = lineScan.nextInt();
						//retrieve entity
						if (entityNum > masterEntityList.size()) {
							continue; //ignore invalid IDs
						}
						EntityData e = masterEntityList.get(entityNum);
						//capture info
						e.setShort1(lineScan.next());
						e.setShort2(lineScan.next());
						e.setName(lineScan.next());
						//System.out.println(e.getName());
						e.setFramerect(str2Rect(lineScan.next()));
						e.setDesc(lineScan.next());
						//add to specialized categories
						if (lineScan.hasNext()) {
							String catStr = lineScan.next();
							Scanner catScan = new Scanner(catStr);
							while (catScan.hasNext()) {
								catStr = catScan.next();
								if (catStr.equals("")) continue; //$NON-NLS-1$
								if (catStr.indexOf(':') == -1) {
									System.err.println(Messages.getString("GameInfo.36")  //$NON-NLS-1$
											+ catStr + Messages.getString("GameInfo.37") + line); //$NON-NLS-1$
									continue;
								}
								String category = catStr.substring(0, catStr.indexOf(':'));
								String subCategory = catStr.substring(catStr.indexOf(':') + 1);
								e.addSubcat(category, subCategory);
							}
							catScan.close();
						}
						lineScan.close();
					} catch (Exception err) {
						StrTools.msgBox("Error parsing EntityInfo.txt at line:"+lineNum);
						System.err.println("Error parsing EntityInfo.txt at line:"+lineNum);
						System.err.println(line);
						err.printStackTrace();
					}
					//System.out.println(e);
				}
			}
			/*
			String[] categoryNames = categoryMap.keySet().toArray(new String[0]);
			for (String s : categoryNames) {
				System.out.println("Category: " + s);
				categoryMap.get(s).printContents();
			}
			*/
			sc.close();
		} catch (FileNotFoundException err) {
			StrTools.msgBox(Messages.getString("GameInfo.39")); //$NON-NLS-1$
			err.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		populateCategoryLists();
	}
	
	private void populateCategoryLists() {
		categoryMap = new HashMap<>();
		EntityCategory allCat = new EntityCategory();
		categoryMap.put("All", allCat); //$NON-NLS-1$
		for (EntityData e : masterEntityList){
			allCat.addEntity(e, "All"); //$NON-NLS-1$
			for (String category : e.categories.keySet()) {
				for (String subCategory : e.categories.get(category)) {
					if (!categoryMap.containsKey(category))
						categoryMap.put(category, new EntityCategory());
					EntityCategory eCat = categoryMap.get(category);
					eCat.addEntity(e, "All"); //$NON-NLS-1$
					eCat.addEntity(e, subCategory);
				}
			}
			
		}
	}
	
	
	public void saveNpcTbl() {
		File tblFile = new File(dataDir + File.separator + "npc.tbl"); //$NON-NLS-1$
		int NUM_ENTITY_TYPE = masterEntityList.size();
		short[] flagDat = new short[NUM_ENTITY_TYPE];
		short[] healthDat = new short[NUM_ENTITY_TYPE];
		byte[] tilesetDat = new byte[NUM_ENTITY_TYPE];
		byte[] deathDat = new byte[NUM_ENTITY_TYPE];
		byte[] hurtDat = new byte[NUM_ENTITY_TYPE];
		byte[] sizeDat = new byte[NUM_ENTITY_TYPE];
		int[] expDat = new int[NUM_ENTITY_TYPE];
		int[] damageDat = new int[NUM_ENTITY_TYPE];
		byte[] hitboxDat = new byte[NUM_ENTITY_TYPE*4];
		byte[] displayDat = new byte[NUM_ENTITY_TYPE*4];
		for (int i = 0; i < NUM_ENTITY_TYPE; i++) {
			EntityData e = masterEntityList.get(i);
			flagDat[i] = (short) e.getFlags();
			healthDat[i] = (short) e.getHP();
			tilesetDat[i] = (byte) e.getTileset();
			deathDat[i] = (byte) e.getDeath();
			hurtDat[i] = (byte) e.getHurt();
			sizeDat[i] = (byte) e.getSize();
			expDat[i] = e.getXP();
			damageDat[i] = e.getDmg();
			Rectangle rect = e.getHit();
			hitboxDat[i*4] = (byte) rect.x;
			hitboxDat[i*4 + 1] = (byte) rect.y;
			hitboxDat[i*4 + 2] = (byte) rect.width;
			hitboxDat[i*4 + 3] = (byte) rect.height;
			rect = e.getDisplay();
			displayDat[i*4] = (byte) rect.x;
			displayDat[i*4 + 1] = (byte) rect.y;
			displayDat[i*4 + 2] = (byte) rect.width;
			displayDat[i*4 + 3] = (byte) rect.height;

			
			e.markUnchanged();
		}
		
		//commit
		try {
			FileOutputStream oStream;
			FileChannel chan;
			
			oStream = new FileOutputStream(tblFile);
			chan = oStream.getChannel();
			
			ByteBuffer buf;
			
			buf = ByteBuffer.allocate(NUM_ENTITY_TYPE * 2);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			for (short s : flagDat) {
				buf.putShort(s);
			}
			buf.flip();
			chan.write(buf);
			
			buf = ByteBuffer.allocate(NUM_ENTITY_TYPE * 2);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			for (short s : healthDat) {
				buf.putShort(s);
			}
			buf.flip();
			chan.write(buf);

			buf = ByteBuffer.wrap(tilesetDat);
			chan.write(buf);
			
			buf = ByteBuffer.wrap(deathDat);
			chan.write(buf);
			
			buf = ByteBuffer.wrap(hurtDat);
			chan.write(buf);
			
			buf = ByteBuffer.wrap(sizeDat);
			chan.write(buf);

			buf = ByteBuffer.allocate(NUM_ENTITY_TYPE * 4);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			for (int v : expDat) { //v for value
				buf.putInt(v);
			}
			buf.flip();
			chan.write(buf);

			buf = ByteBuffer.allocate(NUM_ENTITY_TYPE * 4);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			for (int v : damageDat) { //v for value
				buf.putInt(v);
			}
			buf.flip();
			chan.write(buf);
			
			buf = ByteBuffer.wrap(hitboxDat);
			chan.write(buf);
			
			buf = ByteBuffer.wrap(displayDat);
			chan.write(buf);
			
			chan.close();
			oStream.close();
		} catch (IOException err) {
			StrTools.msgBox(Messages.getString("GameInfo.41")); //$NON-NLS-1$
		}		
		saveEntityInfo();
	}
	
	public void saveEntityInfo() {
		File infoFile = new File("entityInfo.txt");
		File backup = new File("entityInfo_backup.txt");
		if (!backup.exists()) {
			infoFile.renameTo(backup);
		}
		try {
			BufferedWriter o = new BufferedWriter(new FileWriter(infoFile));
			o.write(entityInfo_head);
			for (EntityData e : masterEntityList) {
				String output = "#" + e.getID() + "\t";
				output += e.getShort1() + "\t" + e.getShort2() + "\t";
				output += e.getName() + "\t";
				output += rect2Str(e.getFramerect()) + "\t";
				output += e.getDesc() + "\t";
				for (String c : e.categories.keySet()) {
					for (String sc : e.categories.get(c)) {
						output += c + ":" + sc + " ";
					}
				}
				output += ";\r\n";
				o.write(output);
			}
			o.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Rectangle str2Rect(String s) {
		Scanner sc = new Scanner(s);
		sc.useDelimiter(":"); //$NON-NLS-1$
		int l, u, r, d;
		l = sc.nextInt();
		u = sc.nextInt();
		r = sc.nextInt();
		d = sc.nextInt();
		sc.close();
		return new Rectangle(l, u, r, d);
	}
	
	private String rect2Str(Rectangle r) {
		return "" + r.x + ":" + r.y + ":" + r.width + ":" + r.height;
	}

	private void getExeData(CSExe exe) throws IOException
	{
		final int imgStrOffset1 = 0x8C285;
		final int imgStrOffset2 = 0x8C309;
		final int imgStrOffset3 = 0x8C32D;
		ByteBuffer imgExt = exe.read(imgStrOffset1, 4);
		String extstr = new String(imgExt.array());
		if (!extstr.equals(".bmp")) { //$NON-NLS-1$
			StrTools.msgBox(Messages.getString("GameInfo.29") + extstr + Messages.getString("GameInfo.30")); //$NON-NLS-1$ //$NON-NLS-2$
			imgExt.position(0);
			imgExt.put(new byte[] {'.','b','m','p'});
			exe.patch(imgExt, imgStrOffset1);
			exe.patch(imgExt, imgStrOffset2);
			exe.patch(imgExt, imgStrOffset3);
			changeFileExt(dataDir, extstr, ".bmp"); //$NON-NLS-1$
		}
	}
	
	private void changeFileExt(File baseDir, String oldExt, String newExt) {
		//noinspection ConstantConditions
		for (File f : baseDir.listFiles()) {
			if (f.isDirectory()) {
				changeFileExt(f, oldExt, newExt);
			} else {
				String fname = f.getName();
				if (fname.endsWith(oldExt)) {
					f.renameTo(new File(f.toString().replace(oldExt, newExt)));
				}
			}
		}
	}
	
	private void fillMapdata(File f) throws IOException {
		FileChannel inChan;
		String encoding = gameConfig.getEncoding();
		
		FileInputStream inStream;
		inStream = new FileInputStream(f);
		inChan = inStream.getChannel();
		
		if (type == MOD_TYPE.MOD_CS) //$NON-NLS-1$
		{
            if (executable == null) {
        		//standard CS mod, executable failed to initialize
        		StrTools.msgBox(Messages.getString("GameInfo.47")); //$NON-NLS-1$
            } else {
            	//standard CS mod
                ByteBuffer bb = executable.loadMaps();
                for (int i = 0; i < executable.getMapdataSize(); i++)
                    mapdataStore.add(new Mapdata(i, bb, type, encoding));
            }
		} else if (f.getName().endsWith("tbl")) { //CS+ type //$NON-NLS-1$
            //int maps array data
			int numMaps = (int) (f.length() / 229);
			ByteBuffer dBuf = ByteBuffer.allocate(numMaps * 229);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			
			for (int i = 0; i < numMaps; i++) //for each map
				mapdataStore.add(new Mapdata(i, dBuf, type, encoding));
			inChan.close();
			inStream.close();
		} else if (f.getName().endsWith(".bin")) { //$NON-NLS-1$
            // Possibly GIR/Noxid's "MR" engine. Sorry if I broke this with my meddling. - 20kdc
			/*
			typedef struct {
			    char tileset[16];
			    char filename[16];
			    char scrollType;
			    char bgName[16];
			    char npc1[16];
			    char npc2[16];
			    char bossNum;
			    char mapName[34];
			} MapData; <116>
			*/
			ByteBuffer uBuf = ByteBuffer.allocate(4);
			uBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(uBuf);
			uBuf.flip();
			int nMap = uBuf.getInt();
			ByteBuffer dBuf = ByteBuffer.allocate(nMap*116);
			dBuf.order(ByteOrder.LITTLE_ENDIAN);
			inChan.read(dBuf);
			dBuf.flip();
			//loop
			for (int i = 0; i < nMap; i++)
				mapdataStore.add(new Mapdata(i, dBuf, type, encoding));
		}
	}
	
	public void saveMapData(int map) throws IOException {
		if (type == MOD_TYPE.DUMMY) {
			return;
		}
		String encoding = gameConfig.getEncoding();
		Mapdata dat = mapdataStore.get(map);
		if (!dat.isModified())
			return; //short-circuit
		File tblFile;
		FileInputStream inStream = null;
		FileOutputStream oStream = null;
		FileChannel chan;
		int mapdataSize;
		if (type == MOD_TYPE.MOD_CS) {
			if (executable != null) {
				executable.saveMap(dat.toBuf(MOD_TYPE.MOD_CS, encoding), dat.getMapnum());
			} else {
				StrTools.msgBox(Messages.getString("GameInfo.50")); //$NON-NLS-1$
			}
		} else {
			if (type == MOD_TYPE.MOD_CS_PLUS) {
				tblFile = new File(dataDir + "/stage.tbl"); //$NON-NLS-1$
				mapdataSize = 229;
			} else if (type == MOD_TYPE.MOD_KS) { //if type == KS
				tblFile = new File(dataDir + "/dsmap.bin"); //$NON-NLS-1$
				mapdataSize = 116;
			} else if (type == MOD_TYPE.MOD_MR) {
				tblFile = new File(dataDir + "/mrmap.bin"); //$NON-NLS-1$
				mapdataSize = 116;
			} else {
				StrTools.msgBox(Messages.getString("GameInfo.33")); //$NON-NLS-1$
				return;
			}
			try {
				inStream = new FileInputStream(tblFile);
				chan = inStream.getChannel();
				ByteBuffer dBuf;
				int newPos;
				if (type == MOD_TYPE.MOD_CS_PLUS) {
					dBuf = ByteBuffer.allocate(mapdataStore.size() * mapdataSize);
					newPos = map*mapdataSize;
				} else {
					dBuf = ByteBuffer.allocate(mapdataStore.size() * mapdataSize + 4);
					newPos = map*mapdataSize + 4;
				}
				dBuf.order(ByteOrder.LITTLE_ENDIAN);
				chan.read(dBuf);
				chan.close();
				inStream.close();
				dBuf.position(0);
				dBuf.putInt(mapdataStore.size());
				dBuf.position(newPos);
				dBuf.put(mapdataStore.get(map).toBuf(type, encoding));
				oStream = new FileOutputStream(tblFile);
				chan = oStream.getChannel();
				dBuf.position(0);
				chan.write(dBuf);
				//System.out.println("wrote mapdata " + map);
			} catch (IOException err) {
				StrTools.msgBox(Messages.getString("GameInfo.53") + tblFile); //$NON-NLS-1$
				err.printStackTrace();
			}
		}
		dat.markUnchanged(); //I may regret doing this early
		if (inStream != null)
			inStream.close();
		if (oStream != null)
			oStream.close();
	}
	
	public void exportMapdata(String outFilename, MOD_TYPE format) throws IOException {
		String encoding = gameConfig.getEncoding();
		File outFile = new File(dataDir + "/" + outFilename); //$NON-NLS-1$
		FileOutputStream oStream = new FileOutputStream(outFile);
		FileChannel chan = oStream.getChannel();
		for (Mapdata m : mapdataStore) {
			ByteBuffer dBuf = m.toBuf(format, encoding);
			chan.write(dBuf);
		}
		chan.close();
		oStream.close();
	}
	
	public void commitChanges() {
		for (Mapdata d : mapdataStore) {
			if (d.isModified()) {
				try {
					this.saveMapData(d.getMapnum());
				} catch (IOException e) {
					e.printStackTrace();
					StrTools.msgBox(Messages.getString("GameInfo.55") + d.getMapnum()); //$NON-NLS-1$
				}
			}
		}
		if (executable != null) {
			executable.commit();
		} 
	}
	
	public boolean areUnsavedChanges() {
		boolean changes = (executable != null && executable.isModified());
		for (Mapdata d : mapdataStore) {
			changes |= d.isModified();
		}
		return changes;
	}
	
	public EntityData getEntityInfo(int n) {
		EntityCategory allCat = categoryMap.get("All"); //$NON-NLS-1$
		return allCat.getEntity("All", n);
	}
	
	
	class EntityCategory {
		private Map<String, EntitySubcat> subcatMap;
		
		EntityCategory() {
			subcatMap = new HashMap<>();
			subcatMap.put("All", new EntitySubcat()); //$NON-NLS-1$
		}
		
		public String[] getSubcatNames() {
			Set<String> var = subcatMap.keySet();
			return var.toArray(new String[var.size()]);
		}

		@SuppressWarnings("UnusedDeclaration")
		public boolean hasSubcat(String name) {
			return subcatMap.containsKey(name);
		}
		
		public void addEntity(EntityData e, String subcatName) {
			if (!subcatMap.containsKey(subcatName))
				subcatMap.put(subcatName, new EntitySubcat());
			EntitySubcat eSub = subcatMap.get(subcatName);
			eSub.addEntity(e);
		}
		
		public EntityData getEntity(String subcat, int num) {
				EntitySubcat sub = subcatMap.get(subcat);
				return sub.getEntity(num);
		}
			
		@SuppressWarnings("UnusedDeclaration")
		public void printContents() {
			String[] names = getSubcatNames();
			for (String s : names) {
				System.out.println("Subcategory: " + s); //$NON-NLS-1$
				subcatMap.get(s).printContents();
			}
		}
		
		public EntitySubcat getSubcat(String name) {
			return subcatMap.get(name);
		}
	}
	
	class EntitySubcat {
		private Vector<EntityData> entityList;
		public Vector<EntityData> getList() {return entityList;}
		EntitySubcat() {
			entityList = new Vector<>();
		}
		public void addEntity(EntityData e) {
			entityList.add(e);
		}
		public EntityData getEntity(int index) {
			if (entityList.size() > index)
				return entityList.get(index);
			else
				return null;
		}
		public void printContents() {
			for (EntityData anEntityList : entityList) {
				System.out.println(anEntityList);
			}
		}
	}
	
	private class TilesetFilter implements FilenameFilter {
		@Override
		public boolean accept(File arg0, String arg1) {
			return arg1.startsWith(gameConfig.getTilesetPrefix()) && arg1.endsWith(gameConfig.getImageExtension());
		}		
	}
	private class NpcFilter implements FilenameFilter {
		@Override
		public boolean accept(File arg0, String arg1) {
			return arg1.startsWith(gameConfig.getNpcPrefix()) && arg1.endsWith(gameConfig.getImageExtension());
		}		
	}
	private class BackgroundFilter implements FilenameFilter {
		@Override
		public boolean accept(File arg0, String arg1) {
			return arg1.startsWith(gameConfig.getBackgroundPrefix()) && arg1.endsWith(gameConfig.getImageExtension());
		}		
	}
	
	public void prepareToDeleteMaps() {
		if (executable != null)
			executable.prepareToDeleteMaps();
	}

	/**
	 * p
	 * @param selectedIndex the index of the map to delete
	 * @param parent can be null
	 */
	public void deleteMap(int selectedIndex, EditorApp parent) {
		if (mapdataStore.size() > selectedIndex) {
			mapdataStore.remove(selectedIndex);
			revalidateMapNumbers(parent);
			//check to update the exe
			if (executable != null) {
				executable.setMapdataSize(mapdataStore.size());
			}
		}
	}
	
	public void doneDeletingMaps() {
		if (executable != null)
			executable.doneDeletingMaps();
	}
	
	private void revalidateMapNumbers(EditorApp parent) {
		Vector<Integer> oldNums = new Vector<>();
		Vector<Integer> newNums = new Vector<>();
		for (int i = 0; i < mapdataStore.size(); i++) {
			int oldMap = mapdataStore.get(i).getMapnum();
			if (i != oldMap) {
				oldNums.add(oldMap);
				newNums.add(i);
				mapdataStore.get(i).setMapnum(i);
			}
		}
		changeScriptMapnums(oldNums, newNums);
		if (parent != null) 
			parent.updateOpenScripts(oldNums, newNums);		
	}
	
	public Mapdata addMap() {
		Mapdata d = new Mapdata(mapdataStore.size());
		mapdataStore.add(d);
		if (executable != null) {
			try {
				executable.saveMap(d.toBuf(this.type, gameConfig.getEncoding()), d.getMapnum());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //saveMap will automatically increase size as necessary
		}
		return d;
	}
	
	public Mapdata duplicateMap(int selectedIndex) {
		Mapdata nd = null;
		if (selectedIndex >= 0 && selectedIndex < mapdataStore.size()) {
			Mapdata durr = mapdataStore.get(selectedIndex);
			nd = durr.clone();
			copyMapFiles(durr, nd);
			nd.setMapnum(mapdataStore.size());
			mapdataStore.add(nd);
		}
		return nd;
	}
	
	private void copyMapFiles(Mapdata src, Mapdata dest) {
		//pxm, pxe, tsc
		String[] extArray = {".pxm", ".pxe", ".tsc"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		File srcf, destf;
		
		try {
			for (String s : extArray) {
				srcf = new File(dataDir + "/Stage/" + src.getFile() + s); //$NON-NLS-1$
				destf = new File(dataDir + "/Stage/" + dest.getFile() + s); //$NON-NLS-1$
				copyFile(srcf, destf);
			}
		} catch (IOException err) {
			StrTools.msgBox(Messages.getString("GameInfo.44")); //$NON-NLS-1$
			err.printStackTrace();
		}
	}
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
	public Mapdata addTempMap() {
		Mapdata d = new Mapdata(tempMapdata.size());
		tempMapdata.add(d);
		return d;
	}
	
	public String[] getEntityCategories() {
		Set<String> var = categoryMap.keySet();
		return var.toArray(new String[var.size()]);
	}
	
	public String[] getEntitySubcat(String category) {
		return categoryMap.get(category).getSubcatNames();		
	}
	
	public Vector<EntityData> getEntityList(String category, String subcat) {
		if (category == null) category = "All"; //$NON-NLS-1$
		if (subcat == null) subcat = "All"; //$NON-NLS-1$
		EntityCategory cat = categoryMap.get(category);
		EntitySubcat sub = cat.getSubcat(subcat);
		return sub.getList();
	}
	
	public void execute() {
		if (type == MOD_TYPE.MOD_KS) {
			File gameLoc = dataDir.getParentFile();
			String gamePath = dataDir.getParent() + "/CsEngine.exe"; //$NON-NLS-1$
			Runtime rt = Runtime.getRuntime();
			try {
				String os = System.getProperty("os.name");
				if( !os.contains("Windows")) {
					rt.exec("wine " + gamePath, null, gameLoc);
				}
				else {
					rt.exec(gamePath, null, gameLoc);
				}
			} catch (IOException e) {
				StrTools.msgBox(Messages.getString("GameInfo.46")); //$NON-NLS-1$
				e.printStackTrace();
			}
		} else {
			if (executable != null) {
				executable.execute();
			} else {
				StrTools.msgBox(Messages.getString("GameInfo.47")); //$NON-NLS-1$
			}
		}
	}

	public void generateFlagList() throws IOException {
		//make sure we have a list to read
		if (mapdataStore == null || mapdataStore.isEmpty())
			return;
		//create something to read our input
		BufferedWriter output;
		output = new BufferedWriter(new FileWriter("FlagListing.txt")); //$NON-NLS-1$
		//this is the wicked setup I've got going for
		LinkedList<Integer> flList = new LinkedList<>();
		HashMap<Integer, Vector<String>> flLocTable = new HashMap<>();
		LinkedList<Integer> skList = new LinkedList<>();
		HashMap<Integer, Vector<String>> skLocTable = new HashMap<>();
		LinkedList<Integer> mpList = new LinkedList<>();
		HashMap<Integer, Vector<String>> mpLocTable = new HashMap<>();
		for (Mapdata d : mapdataStore)
		{
			String subDir = "/Stage/"; //$NON-NLS-1$
			String sourcePath = dataDir + subDir + d.getFile() + ".tsc"; //$NON-NLS-1$
			File sourceFile = new File(sourcePath);
			if (!sourceFile.exists()) //make sure it's actually there before we go at it
				continue;
			//parse the file
			TscLexer tLex = new TscLexer();
			tLex.reset(new StringReader(TscPane.parseScript(sourceFile, gameConfig.getEncoding())), 0, -1, 0);
			TscToken t;
			int currentEvent = 0;
			while ((t = tLex.getNextToken()) != null)
			{
				if (t.getDescription().equals("eveNum")) //$NON-NLS-1$
				{
					currentEvent = StrTools.ascii2Num_CS(t.getContents().substring(1));
					continue;
				}
				LinkedList<Integer> fList = null;
				HashMap<Integer, Vector<String>> fTable = null;
				if (t.getContents().equals("<FL+") || t.getContents().equals("<FL-") || t.getContents().equals("<FLJ")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					fList = flList;
					fTable = flLocTable;
				} else if (t.getContents().equals("<SK+") || t.getContents().equals("<SK-") || t.getContents().equals("<SKJ")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					fList = skList;
					fTable = skLocTable;
				} else if (t.getContents().equals("<MP+") || t.getContents().equals("<MPJ")) { //$NON-NLS-1$ //$NON-NLS-2
					fList = mpList;
					fTable = mpLocTable;
				} else
					continue;
				String tag = t.getContents();
				if ((t = tLex.getNextToken()) != null)
				{
					int flagNum = StrTools.ascii2Num_CS(t.getContents());
					Vector<String> locList;
					if (fList.contains(flagNum)) {
						locList = fTable.get(flagNum);
					} else {
						locList = new Vector<>();
						fTable.put(flagNum, locList);
						fList.add(flagNum);
					}
					locList.add("\t " + tag + " " + sourceFile.getName() + " event #" + currentEvent + "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				} else {//if there is a next token
					break;
				}
			} //while we have more tokens
		}//for each file
		//sort the flag list
		int[] flArray = new int[flList.size()];
		for (int i = 0; i < flList.size(); i++)
		{
			flArray[i] = flList.get(i);
		}
		int[] skArray = new int[skList.size()];
		for (int i = 0; i < skList.size(); i++)
		{
			skArray[i] = skList.get(i);
		}
		int[] mpArray = new int[mpList.size()];
		for (int i = 0; i < mpList.size(); i++)
		{
			mpArray[i] = mpList.get(i);
		}
		java.util.Arrays.sort(flArray);
		java.util.Arrays.sort(skArray);
		java.util.Arrays.sort(mpArray);
		final String lineSep = System.lineSeparator();
		for (int aFArray : flArray) {
			output.write(Messages.getString("GameInfo.1") + aFArray + lineSep); //$NON-NLS-1$ //$NON-NLS-2$
			Vector<String> locList = flLocTable.get(aFArray);
			for (String aLocList : locList) {
				output.write(aLocList);
			}
		}
		for (int aFArray : skArray) {
			output.write(Messages.getString("GameInfo.3") + aFArray + lineSep); //$NON-NLS-1$ //$NON-NLS-2$
			Vector<String> locList = skLocTable.get(aFArray);
			for (String aLocList : locList) {
				output.write(aLocList);
			}
		}
		for (int aFArray : mpArray) {
			output.write(Messages.getString("GameInfo.4") + aFArray + lineSep); //$NON-NLS-1$ //$NON-NLS-2$
			Vector<String> locList = mpLocTable.get(aFArray);
			for (String aLocList : locList) {
				output.write(aLocList);
			}
		}
		output.close();
		StrTools.msgBox(Messages.getString("GameInfo.64")); //$NON-NLS-1$
	}
	
	public void generateTRAList() throws IOException {
		//make sure we have a list to read
		if (mapdataStore == null || mapdataStore.isEmpty())
			return;
		//create something to read our input
		BufferedWriter output;
		int totalEvents = 0;
		output = new BufferedWriter(new FileWriter("TRAListing.txt")); //$NON-NLS-1$
		//this is the wicked data setup I've got going
		LinkedList<Integer> tList = new LinkedList<>(); //list of integers to index the hashmap
		HashMap<Integer, Vector<String>> locTable = new HashMap<>(); //vectors of strings (info) indexed by map number
		for (Mapdata d : mapdataStore)
		{
			String subDir = "/Stage/"; //$NON-NLS-1$
			String sourcePath = dataDir + subDir + d.getFile() + ".tsc"; //$NON-NLS-1$
			File sourceFile = new File(sourcePath);
			if (!sourceFile.exists()) //make sure it's actually there before we go at it
				continue;
			//parse the file
			TscLexer tLex = new TscLexer();
			tLex.reset(new StringReader(TscPane.parseScript(sourceFile, gameConfig.getEncoding())), 0, -1, 0);
			TscToken t;
			int currentEvent = 0;
			while ((t = tLex.getNextToken()) != null)
			{
				if (t.getDescription().equals("eveNum")) //$NON-NLS-1$
				{
					currentEvent = StrTools.ascii2Num_CS(t.getContents().substring(1));
					totalEvents++;
				} else if (t.getContents().equals("<TRA")) { //$NON-NLS-1$
					int[] params = new int[4];
					int pAssigned = 0;
					//get all parameters for command
					while (pAssigned < 4)
					{
						t = tLex.getNextToken();
						if (t == null)
							break;
						if (t.getDescription().equals("number")) //$NON-NLS-1$
						{
							params[pAssigned] = StrTools.ascii2Num_CS(t.getContents());
							pAssigned++;
						}
						
					}
					//make list
					Vector<String> locList;
					if (tList.contains(params[0]))
					{
						locList = locTable.get(params[0]);
					} else {
						locList = new Vector<>();
						locTable.put(params[0], locList);
						tList.add(params[0]);
					}
					locList.add("\t " + sourceFile.getName() + " event #" +  //$NON-NLS-1$ //$NON-NLS-2$
					currentEvent + Messages.getString("GameInfo.73") + params[2] + "," + //$NON-NLS-1$ //$NON-NLS-2$
							params[3] + Messages.getString("GameInfo.75") + params[1] + "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
					if (t == null)
						break;
				} //elseif token was tra
			} //while we have more tokens
		}//for each file
		//sort the flag list
		int[] tArray = new int[tList.size()];
		for (int i = 0; i < tList.size(); i++)
		{
			tArray[i] = tList.get(i);
		}
		java.util.Arrays.sort(tArray);
		for (int aTArray : tArray) {
			String fname = Messages.getString("GameInfo.77"); //$NON-NLS-1$
			if (aTArray < mapdataStore.size()) {
				Mapdata d = mapdataStore.get(aTArray);
				fname = d.getFile() + " " + d.getMapname(); //$NON-NLS-1$
			}
			output.write(Messages.getString("GameInfo.79") + aTArray + " "  //$NON-NLS-1$ //$NON-NLS-2$
					+ fname + "\r\n"); //$NON-NLS-1$
			Vector<String> locList = locTable.get(aTArray);
			for (String aLocList : locList) {
				output.write(aLocList);
			}
		}
		output.close();
		System.out.println(totalEvents + " total events");
		StrTools.msgBox(Messages.getString("GameInfo.82")); //$NON-NLS-1$
	}

	public void changeScriptMapnums(Vector<Integer> oldNums, Vector<Integer> newNums) {
		//make sure we have a list to read
		if (mapdataStore == null || mapdataStore.isEmpty())
			return;
		String oldTRA;// = String.format("<TRA%04d", oldNums);
		String newTRA;
		for (Mapdata d : mapdataStore)
		{
			String subDir = "/Stage/"; //$NON-NLS-1$
			String scriptPath = dataDir + subDir + d.getFile() + ".tsc"; //$NON-NLS-1$
			File scriptFile = new File(scriptPath);
			String sourcePath = dataDir + subDir + "/ScriptSource/" + d.getFile() + ".txt"; //$NON-NLS-1$ //$NON-NLS-2$
			File sourceFile = new File(sourcePath);
			if (!scriptFile.exists()) //make sure it's actually there before we go at it
				continue;
			//read its contents
			String fileContents;
			try {
				if (sourceFile.exists()) {
					fileContents = TscPane.parseScript(sourceFile, gameConfig.getEncoding());
				} else {
					fileContents = TscPane.parseScript(scriptFile, gameConfig.getEncoding());
				}
				boolean modified = false;
				for (int i = 0; i < oldNums.size(); i++) {
					oldTRA = String.format("<TRA%04d", oldNums.get(i)); //$NON-NLS-1$
					newTRA = String.format("<TRA<<%04d", newNums.get(i)); // dummy //$NON-NLS-1$
					if (fileContents.contains(oldTRA)) {
						modified = true;
						fileContents = fileContents.replaceAll(oldTRA, newTRA);
					}
				}
				if (modified) {
					for (Integer newNum : newNums) {
						oldTRA = String.format("<TRA<<%04d", newNum); //$NON-NLS-1$
						newTRA = String.format("<TRA%04d", newNum); // dummy //$NON-NLS-1$
						if (fileContents.contains(oldTRA)) {
							fileContents = fileContents.replaceAll(oldTRA, newTRA);
						}
					}
					//save source
					if (!sourceFile.exists()) {
						sourceFile.getParentFile().mkdirs();
						sourceFile.createNewFile();
					}
					BufferedWriter out = new BufferedWriter(new FileWriter(sourceFile));
					out.write(fileContents);
					out.close();
					
					//save the TSC
					TscPane.SaveTsc(fileContents, scriptFile);
				}
			} catch (IOException err) {
				err.printStackTrace();
			}
		}//for each file
	}
	
	public void moveMap(int srcMap, int destMap, EditorApp parent) {
		Mapdata moving = mapdataStore.get(srcMap);
		mapdataStore.remove(srcMap);
		mapdataStore.insertElementAt(moving, destMap);
		revalidateMapNumbers(parent);
	}
	public CSExe getExe() {
		return executable;
	}
	
	public Set<File> allGameFiles() {
		Set<File> foundFiles = new HashSet<>();
		if (dataDir == null) return foundFiles;
		if (type == MOD_TYPE.MOD_CS) {
			foundFiles.addAll(CS_DEFAULT_FILES());
		}
		String imageExtension = gameConfig.getImageExtension();
		for (Mapdata m : mapdataStore) {
			foundFiles.add(new File(dataDir + "/" + m.getBG() + imageExtension));
			foundFiles.add(new File(dataDir + "/Npc/" + gameConfig.getNpcPrefix() + m.getNPC1() + imageExtension));
			foundFiles.add(new File(dataDir + "/Npc/" + gameConfig.getNpcPrefix() + m.getNPC2() + imageExtension));
			foundFiles.add(new File(dataDir + "/Stage/" + gameConfig.getTilesetPrefix() + m.getTileset() + imageExtension));
			foundFiles.add(new File(dataDir + "/Stage/" + m.getTileset() + ".pxa"));
			foundFiles.add(new File(dataDir + "/Stage/" + m.getFile() + ".pxm"));
			foundFiles.add(new File(dataDir + "/Stage/" + m.getFile() + ".pxe"));
			foundFiles.add(new File(dataDir + "/Stage/" + m.getFile() + ".tsc"));
		}
		
		return foundFiles;
	}

	private LinkedList<File> CS_DEFAULT_FILES() {
		LinkedList<File> flist = new LinkedList<>();
		String imageExtension = gameConfig.getImageExtension();
		flist.add(new File(dataDir + "/ArmsItem.tsc"));
		flist.add(new File(dataDir + "/Credit.tsc"));
		flist.add(new File(dataDir + "/Head.tsc"));
		flist.add(new File(dataDir + "/StageSelect.tsc"));
		flist.add(new File(dataDir + "/npc.tbl"));
		flist.add(new File(dataDir + "/Face" + imageExtension));
		flist.add(new File(dataDir + "/Loading" + imageExtension));
		flist.add(new File(dataDir + "/TextBox" + imageExtension));
		flist.add(new File(dataDir + "/Title" + imageExtension));
		flist.add(new File(dataDir + "/ItemImage" + imageExtension));
		flist.add(new File(dataDir + "/StageImage" + imageExtension));
		flist.add(new File(dataDir + "/Fade" + imageExtension));
		flist.add(new File(dataDir + "/Caret" + imageExtension));
		flist.add(new File(dataDir + "/Bullet" + imageExtension));
		flist.add(new File(dataDir + "/casts" + imageExtension));
		flist.add(new File(dataDir + "/Arms" + imageExtension));
		flist.add(new File(dataDir + "/ArmsImage" + imageExtension));
		flist.add(new File(dataDir + "/MyChar" + imageExtension));
		flist.add(new File(dataDir + "/Npc/NpcRegu" + imageExtension));
		flist.add(new File(dataDir + "/Npc/NpcSym" + imageExtension));
		return flist;
	}
}
