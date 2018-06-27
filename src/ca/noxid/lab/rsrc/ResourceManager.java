package ca.noxid.lab.rsrc;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import com.carrotlord.string.StrTools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class ResourceManager {
	private HashMap<File, BufferedImage> imgMap;
	private HashMap<File, byte[]> pxaMap;
	public static final String rsrcTiles = "tiles.png";
	public static final String rsrcBackdrop = "backdrop.png";
	public static final String rsrcSplash1 = "splash_c1.png";
	public static final String rsrcSplash2 = "splash_c2.png";
	public static final String rsrcSplashMid = "splash_mid.png";
	public static final String rsrcBgWhite = "bgwhite.png";
	public static final String rsrcBgWhite2 = "bgwhite2.png";
	public static final String rsrcBgBlue = "bgblue.png";
	public static final String rsrcBgBrown = "Brown.png";
	public static final String rsrcCorner = "corner.png";
	
	public static final String rsrcCursor = "weed_cursor.png";
	public static final Cursor cursor = makeCursor();
	
	private static Cursor makeCursor() {
		Image cim;
		try {
			cim = ImageIO.read(ResourceManager.class.getResource(rsrcCursor));
			return Toolkit.getDefaultToolkit().createCustomCursor(cim, new Point(0,0), "weed");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static final String[] jarImgs = {
		rsrcTiles,
		rsrcBackdrop, //$NON-NLS-1$
		rsrcSplash1, //$NON-NLS-1$
		rsrcSplash2, //$NON-NLS-1$
		rsrcSplashMid, //$NON-NLS-1$
		rsrcBgWhite, //$NON-NLS-1$
		rsrcBgWhite2, //$NON-NLS-1$
		rsrcBgBlue, //$NON-NLS-1$
		rsrcBgBrown, //$NON-NLS-1$
		rsrcCorner, //$NON-NLS-1$
	};
	public ResourceManager() {
		imgMap = new HashMap<>();
		pxaMap = new HashMap<>();
		try {
			for (String s : jarImgs) {
				String ss = s;
				if (EditorApp.blazed) ss = "weed_" + ss;
				InputStream is = ResourceManager.class.getResourceAsStream(ss);
				imgMap.put(new File(s), ImageIO.read(is));
				is.close();
			}
		} catch (IOException e) {
			StrTools.msgBox(Messages.getString("ResourceManager.2") + //$NON-NLS-1$
					Messages.getString("ResourceManager.3") + //$NON-NLS-1$
					Messages.getString("ResourceManager.4")); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	/**
	 * This method is a proxy to the file version of this method
	 * @param srcFile location of the file to load
	 * @param filterType Specifies a specific operation to perform when loading
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void addImage(String srcFile, int filterType) {
		addImage(new File(srcFile), filterType);
	}
	
	/**
	 * Attempts to add an image to the store.
	 * Valid filter type values:
	 * 0 - No filter
	 * 1 - Convert black pixels to transparent
	 * @param srcFile image to load
	 * @param filterType filtering method
	 */
	public void addImage(File srcFile, int filterType) {	
		srcFile = checkBase(srcFile);
		try {
			if (imgMap.containsKey(srcFile))
				return;
			FileInputStream is = new FileInputStream(srcFile);
			switch (filterType) {
			case 0: //just load the image
				imgMap.put(srcFile, ImageIO.read(is));
				break;
			case 1: //black2Trans
				BufferedImage img = ImageIO.read(is);
				if (srcFile.getName().endsWith(".png")) { //$NON-NLS-1$
					imgMap.put(srcFile,	magenta2Trans(img));
				} else {
					imgMap.put(srcFile, black2Trans(img));
				}
			}
			is.close();
		} catch (IOException e) {
			StrTools.msgBox(Messages.getString("ResourceManager.6") + srcFile); //$NON-NLS-1$
			//e.printStackTrace();
		}
	}
	
	/**
	 * This method is a proxy to the file version of this method
	 * @param src location of the file to load
	 * @param filterType Specifies a specific operation to perform when loading
	 */
	@SuppressWarnings("UnusedDeclaration")
	public void reloadImage(String src, int filterType) {
		reloadImage(new File(src), filterType);
	}
	
	/**
	 * If the file exists in the repository, replace. If not, load it anyway
	 * @param srcFile image to load
	 * @param filterType filtering method
	 */
	public void reloadImage(File srcFile, int filterType) {
		srcFile = checkBase(srcFile);
		if (imgMap.containsKey(srcFile)) {
			imgMap.get(srcFile).flush();
			imgMap.remove(srcFile);
		}
		addImage(srcFile, filterType);
	}
	
	@SuppressWarnings("UnusedDeclaration")
	public java.awt.Graphics getImgGraphics(File key) {
		key = checkBase(key);
		if (imgMap.containsKey(key))
			return imgMap.get(key).getGraphics();
		System.err.println("Key not found for getImgGraphics"); //$NON-NLS-1$
		System.err.println(key);
		return null;			
	}
	
	public BufferedImage getImg(File key) {
		key = checkBase(key);
		if (imgMap.containsKey(key))
			return imgMap.get(key);
		System.err.println("Key not found for getImgGraphics"); //$NON-NLS-1$
		System.err.println(key);
		return null;	
	}
	public BufferedImage getImg(String key) {
		return getImg(new File(key));
	}
	public int getImgH(File key) {
		key = checkBase(key);
		if (imgMap.containsKey(key))
			return imgMap.get(key).getHeight();
		System.err.println("Key not found for getImgGraphics"); //$NON-NLS-1$
		System.err.println(key);
		return -1;	
	}
	public int getImgW(File key) {
		key = checkBase(key);
		if (imgMap.containsKey(key))
			return imgMap.get(key).getWidth();
		System.err.println("Key not found for getImgGraphics"); //$NON-NLS-1$
		System.err.println(key);
		return -1;	
	}
	
	private BufferedImage black2Trans(BufferedImage src)
	{
		BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		for (int y = 0; y < src.getHeight(); y++)
			for (int x = 0; x < src.getWidth(); x++)
			{
				int px = src.getRGB(x, y);
				if (px == Color.BLACK.getRGB())
					dest.setRGB(x, y, 0);
				else
					dest.setRGB(x, y, px);
			}
		
		return dest;
	}
	
	private BufferedImage magenta2Trans(BufferedImage src) {
		BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		for (int y = 0; y < src.getHeight(); y++)
			for (int x = 0; x < src.getWidth(); x++)
			{
				int px = src.getRGB(x, y);
				if (px == Color.MAGENTA.getRGB())
					dest.setRGB(x, y, 0);
				else
					dest.setRGB(x, y, px);
			}
		
		return dest;
	}
	
	public byte[] addPxa(File srcFile, int size) {
		srcFile = checkBase(srcFile);
		FileChannel inChan = null;
		if (pxaMap.containsKey(srcFile))
			return pxaMap.get(srcFile);
		byte[] pxaArray = null;
		boolean succ = false;
		try {
			FileInputStream inStream = new FileInputStream(srcFile);
			inChan = inStream.getChannel();
			ByteBuffer pxaBuf = ByteBuffer.allocate(size);//this is the max size. Indeed, the only size..
			inChan.read(pxaBuf);
			inChan.close();
			inStream.close();
			pxaBuf.flip();
			pxaArray = pxaBuf.array();
			pxaMap.put(srcFile, pxaArray);
			succ = true;
		} catch (FileNotFoundException e) {
			StrTools.msgBox(srcFile + Messages.getString("ResourceManager.11")); //$NON-NLS-1$
		} catch (IOException e) {
			StrTools.msgBox(Messages.getString("ResourceManager.12") + srcFile); //$NON-NLS-1$
			e.printStackTrace();
		} finally {
			if (inChan != null)
				try {
					inChan.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (!succ && srcFile != null) {
				byte[] dummyArray = new byte[size];
				pxaMap.put(srcFile, dummyArray);
			}
		}
		return pxaArray;
	}
	
	@SuppressWarnings("UnusedDeclaration")
	public byte[] addPxa(String src, int size) {
		return addPxa(new File(src), size);
	}
	
	public byte[] getPxa(File srcFile) {
		srcFile = checkBase(srcFile);
		return pxaMap.get(srcFile);
	}
	
	@SuppressWarnings("UnusedDeclaration")
	public byte[] getPxa(String src) {
		return getPxa(new File(src));
	}
	
	public void savePxa(File srcFile) {
		FileChannel outChan;
		try {
			FileOutputStream out = new FileOutputStream(srcFile);
			outChan = out.getChannel();
			ByteBuffer pxaBuf = ByteBuffer.wrap(getPxa(srcFile));
			outChan.write(pxaBuf);
			outChan.close();
			out.close();
		} catch (Exception e) {
			StrTools.msgBox(Messages.getString("ResourceManager.13") + srcFile //$NON-NLS-1$
					+ Messages.getString("ResourceManager.14")); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	public void purge() {
		for (BufferedImage b : imgMap.values()) {
			b.flush();
		}
		imgMap.clear();
		//load default rsrsc
		try {
			for (String s : jarImgs) {
				String ss = s;
				if (EditorApp.blazed) ss = "weed_" + ss;
				InputStream is = ResourceManager.class.getResourceAsStream(ss);
				imgMap.put(new File(s), ImageIO.read(is));
				is.close();
			}
		} catch (IOException e) {
			StrTools.msgBox(Messages.getString("ResourceManager.2") + //$NON-NLS-1$
					Messages.getString("ResourceManager.3") + //$NON-NLS-1$
					Messages.getString("ResourceManager.4")); //$NON-NLS-1$
			e.printStackTrace();
		}
		pxaMap.clear();
	}
	
	public static File getBaseFolder(File currentLoc) {
		String place = "/"; //$NON-NLS-1$
		if (currentLoc == null) {
			//System.out.println(res);
			return null;
		}
			
		while (!currentLoc.getName().equals("mod")) { //$NON-NLS-1$
			place = "/" + currentLoc.getName() + place; //$NON-NLS-1$
			if (currentLoc.getParentFile() == null) {
				return null; //heirarchy crisis
			}
			currentLoc = currentLoc.getParentFile();
		}
		//so barring shenanigans we should be in the 'mod' directory now
		currentLoc = currentLoc.getParentFile(); //modfolder (hurray unnecessarily nested folders)
		currentLoc = currentLoc.getParentFile(); //data
		currentLoc = new File(currentLoc + "/base"); //base... ofc //$NON-NLS-1$
		return currentLoc;
	}
	
	public static File checkBase(File res) {
		if (res.exists()) {
			return res; //if it's already here don't bother looking.
		}
		File currentLoc = res.getParentFile();
		String place = "/"; //$NON-NLS-1$
		if (currentLoc == null) {
			//System.out.println(res);
			return res;
		}
			
		while (!currentLoc.getName().equals("mod")) { //$NON-NLS-1$
			place = "/" + currentLoc.getName() + place; //$NON-NLS-1$
			if (currentLoc.getParentFile() == null) {
				return res; //heirarchy crisis
			}
			currentLoc = currentLoc.getParentFile();
		}
		//so barring shenanigans we should be in the 'mod' directory now
		currentLoc = currentLoc.getParentFile(); //modfolder (hurray unnecessarily nested folders)
		currentLoc = currentLoc.getParentFile(); //data
		currentLoc = new File(currentLoc + "/base"); //base... ofc //$NON-NLS-1$
		File target = new File(currentLoc + place + res.getName());
		if (target.exists()) {
			return target;
		} else {
			return res;
		}
	}
}
