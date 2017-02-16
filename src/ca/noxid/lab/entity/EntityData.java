package ca.noxid.lab.entity;
import ca.noxid.lab.Changeable;
import ca.noxid.lab.Messages;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/*
 * NPC.tbl field order
	unsigned short *npctbl_Flags = NULL;
	unsigned short *npctbl_Health = NULL;
	unsigned char *npctbl_Tileset = NULL;
	unsigned char *npctbl_DeathSound = NULL;
	unsigned char *npctbl_HurtSound = NULL;
	unsigned char *npctbl_Size = NULL;
	unsigned int *npctbl_Exp = NULL;
	unsigned int *npctbl_Damage = NULL;
	unsigned char *npctbl_Hitbox = NULL;
	unsigned char *npctbl_Displaybox =  NULL;
 */
public class EntityData  implements Changeable{
	private String name;
	public String getName() {return name;}
	public void setName(String s) {
		if (name == null || !name.equals(s)) {
			name = s;
			markChanged();
		}
	}
	private String shortName1;
	public String getShort1() {return shortName1;}
	public void setShort1(String s) {
		if (shortName1 == null || !shortName1.equals(s)) {
			shortName1 = s;
			markChanged();
		}
	}
	private String shortName2;
	public String getShort2() {return shortName2;}
	public void setShort2(String s) {
		if (shortName2 == null || !shortName2.equals(s)) {
			shortName2 = s;
			markChanged();
		}
	}
	private String description;
	public String getDesc() {return description;}
	public void setDesc(String s) {
		if (description == null || !description.equals(s)) {
			description = s;
			markChanged();
		}
	}
	private Rectangle frameRect;
	public Rectangle getFramerect() {return frameRect;}
	public void setFramerect(Rectangle r) {
		if (frameRect == null || !frameRect.equals(r)) {
			frameRect = r;
			markChanged();
		}
	}
	private int entityNum;
	public int getID() {return entityNum;}
	public void setID(int i) {
		this.entityNum = i;
	}
	
	private int tbl_HP;
	public int getHP() {return tbl_HP;}
	public void setHP(int health) {
		if (tbl_HP != health) {
			tbl_HP = health;
			markChanged();
		}
	}
	private Rectangle tbl_display;
	public Rectangle getDisplay() {return new Rectangle(tbl_display);}
	public void setDisplay(Rectangle r) {
		if (tbl_display == null || !tbl_display.equals(r)) {
			tbl_display = r;
			markChanged();
		}
	}
	private Rectangle tbl_hitbox;
	public Rectangle getHit() {return new Rectangle(tbl_hitbox);}
	public void setHit(Rectangle r) {
		if (tbl_hitbox == null || !tbl_hitbox.equals(r)) {
			tbl_hitbox = r;
			markChanged();
		}
	}
	private int tbl_tileset;
	public int getTileset() {return tbl_tileset;}
	public void setTileset(int t) {
		if (tbl_tileset != t) {
			tbl_tileset = t;
			markChanged();
		}
	}
	private int tbl_exp;
	public int getXP() {return tbl_exp;}
	public void setXP(int xp) {
		if (tbl_exp != xp) {
			tbl_exp = xp;
			markChanged();
		}
	}
	private int tbl_damage;
	public int getDmg() {return tbl_damage;}
	public void setDmg(int d) {
		if (tbl_damage != d) {
			tbl_damage = d;
			markChanged();
		}
	}
	private int tbl_flags;
	public int getFlags() {return tbl_flags;}
	public void setFlags(int f) {
		if (tbl_flags != f) {
			tbl_flags = f;
			markChanged();
		}
	}
	private int tbl_deathSound;
	public int getDeath() {return tbl_deathSound;}
	public void setDeath(int snd) {
		if (tbl_deathSound != snd) {
			tbl_deathSound = snd;
			markChanged();
		}
	}
	private int tbl_hurtSound;
	public int getHurt() {return tbl_hurtSound;}
	public void setHurt(int snd) {
		if (tbl_hurtSound != snd) {
			tbl_hurtSound = snd;
			markChanged();
		}
	}
	private int tbl_size;
	public int getSize() {return tbl_size;}
	public void setSize(int s) {
		if (tbl_size != s) {
			tbl_size = s;
			markChanged();
		}
	}
	
	//for changeable
	private boolean modified = false;
	
	//key = cat ; list = subcats
	public Map<String, ArrayList<String>> categories = new HashMap<>();
	
	public static final String[] flagNames = initFlagNames();
	
	EntityData(int num) {
		name = "unnamed"; //$NON-NLS-1$
		shortName1 = "no"; //$NON-NLS-1$
		shortName2 = "name"; //$NON-NLS-1$
		frameRect = new Rectangle(0,0,0,0);
		this.tbl_hitbox = new Rectangle(4, 4, 4, 4);
		this.tbl_display = new Rectangle(8, 8, 8, 8);
		entityNum = num;
	}
	
	private static String[] initFlagNames() {
		File flagfile = new File("flagnames.txt");
		String[] retVal;
		try {
			Scanner sc = new Scanner(flagfile);
			int lines = 0;
			retVal = new String[16];
			while (sc.hasNext() && lines < 16) {
				String line = sc.nextLine();
				retVal[lines] = line;
				lines++;
			}
			sc.close();
		} catch (FileNotFoundException e) {
			retVal = new String[] {Messages.getString("EntityData.0"), Messages.getString("EntityData.1"), Messages.getString("EntityData.2"), Messages.getString("EntityData.3"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					Messages.getString("EntityData.4"), Messages.getString("EntityData.5"), Messages.getString("EntityData.6"), Messages.getString("EntityData.7"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					Messages.getString("EntityData.8"), Messages.getString("EntityData.9"), Messages.getString("EntityData.10"), Messages.getString("EntityData.11"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					Messages.getString("EntityData.12"), Messages.getString("EntityData.13"), Messages.getString("EntityData.14"), Messages.getString("EntityData.15") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			};
			System.err.println("flagnames.txt not found <entitydata.java::initFlagNames()>");
			
		}
		return retVal;
	}
	public EntityData(int num, int dam, int deathSound, int exp, int flags, int hp, int hurt,
			int size, int tileset, Rectangle display, Rectangle hitbox) {
		name = "unnamed"; //$NON-NLS-1$
		shortName1 = "no"; //$NON-NLS-1$
		shortName2 = "name"; //$NON-NLS-1$
		frameRect = new Rectangle(0,0,0,0);
		entityNum = num;
		tbl_damage = dam;
		tbl_deathSound = deathSound;
		tbl_exp = exp;
		tbl_flags = flags;
		tbl_HP = hp;
		tbl_hurtSound = hurt;
		tbl_size = size;
		tbl_tileset = tileset;
		tbl_display = display;
		tbl_hitbox = hitbox;
	}
	
	EntityData(String n, int num) {
		name = n;
		entityNum = num;
	}
	
	EntityData(EntityData other) {
		name = other.name;
		shortName1 = other.shortName1;
		shortName2 = other.shortName2;
		description = other.description;
		frameRect = new Rectangle(other.frameRect);
		entityNum = other.entityNum;
		tbl_damage = other.tbl_damage;
		tbl_deathSound = other.tbl_deathSound;
		tbl_exp = other.tbl_exp;
		tbl_flags = other.tbl_flags;
		tbl_HP = other.tbl_HP;
		tbl_hurtSound = other.tbl_hurtSound;
		tbl_size = other.tbl_size;
		tbl_tileset = other.tbl_tileset;
		tbl_display = new Rectangle(other.tbl_display);
		tbl_hitbox = new Rectangle(other.tbl_hitbox);
		categories.putAll(other.categories);
	}
	
	public String toString() {
		return "#" + entityNum + " " + name; //$NON-NLS-1$ //$NON-NLS-2$
	}
	@Override
	public boolean isModified() {
		return modified;
	}
	@Override
	public void markUnchanged() {
		modified = false;
	}
	@Override
	public void markChanged() {
		modified = true;
	}
	
	public void initTreeRoot(DefaultTreeModel model, DefaultMutableTreeNode root) {
		root.removeAllChildren();
		DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode("categories"); //$NON-NLS-1$
		Set<String> catSet = categories.keySet();
		for (String cat : catSet) {
			DefaultMutableTreeNode category = new DefaultMutableTreeNode(cat);
			newRoot.add(category);
			ArrayList<String> subcats = categories.get(cat);
			for (String sub : subcats) {
				DefaultMutableTreeNode subcat = new DefaultMutableTreeNode(sub);
				category.add(subcat);
			}
		}
		model.insertNodeInto(newRoot, root, 0);
	}
	
	public void addSubcat(String category, String subcat) {
		if (!categories.containsKey(category)) {
			categories.put(category, new ArrayList<String>());
		}
		ArrayList<String> subcats = categories.get(category);
		if (!subcats.contains(subcat)) {
			subcats.add(subcat);
		}
	}
}
