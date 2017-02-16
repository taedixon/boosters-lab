package ca.noxid.lab.entity;

import ca.noxid.lab.gameinfo.GameInfo;
import ca.noxid.lab.mapdata.MapInfo;
import ca.noxid.lab.rsrc.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class EntityListRender extends DefaultListCellRenderer {
	private static final long serialVersionUID = 6831932103635388073L;
	
	ResourceManager iMan;
	MapInfo data;
	GameInfo exeData;
	
	EntityData ent;
	
	EntityListRender(MapInfo src, ResourceManager r, GameInfo exe) {
		super();
		iMan = r;
		data = src;
		exeData = exe;
	}
	
	@Override
	public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, 
			Object value, int index, boolean isSelected, boolean hasFocus) {
		if (!(value instanceof EntityData)) {
			super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
		} else {
			EntityData e = (EntityData)value;
			this.setText(e.toString());
			this.setForeground(Color.white);
			ent = e;
			this.setToolTipText(e.getDesc());
			this.setSize(32, 128);
			if (isSelected) {
				this.setBackground(Color.DARK_GRAY);
			} else {
				this.setBackground(Color.black);
			}
			if (hasFocus) {
				this.setBorder(BorderFactory.createLineBorder(Color.green));
			} else {
				this.setBorder(null);
			}
			int h = e.getFramerect().height - e.getFramerect().y;
			if (h < 16) h = 16;
			if (h > 64) h = 64;
			this.setPreferredSize(new Dimension(184, h));
		}		
		return this;
	}
	
	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g.create();
		g2d.setBackground(this.getBackground());
		g2d.setColor(this.getForeground());
		g2d.clearRect(0, 0, this.getWidth(), this.getHeight());
		drawSprite(g2d, ent);
		int strH, strV;
		strH = ent.getFramerect().width - ent.getFramerect().x;
		strV = this.getHeight()/2 + 4;
		if (strV < 8) strV = 8;
		if (strH > 40) strH = 40;
		Rectangle strArea = g2d.getFontMetrics().getStringBounds(getText(), g2d).getBounds();
		strArea.translate(strH, strV);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) 0.25));
		g2d.setColor(this.getBackground());
		g2d.fillRect(strArea.x, strArea.y, strArea.width, strArea.height);
		g2d.setComposite(AlphaComposite.Src);
		g2d.setColor(this.getForeground());
		g2d.drawString(getText(), strH, strV);
		g2d.setColor(Color.gray);
		g2d.drawRect(strArea.x, strArea.y, strArea.width, strArea.height);
		
	}
	
	private void drawSprite(Graphics2D g2d, EntityData inf) {

		Rectangle frameRect = inf.getFramerect();
		BufferedImage srcImg;
		int tilesetNum = inf.getTileset();
		if (tilesetNum == 0x15)
			srcImg = iMan.getImg(data.getNpc1());
		else if (tilesetNum == 0x16)
			srcImg = iMan.getImg(data.getNpc2());
		else if (tilesetNum == 0x14) //npc sym
			srcImg = iMan.getImg(exeData.getNpcSym());
		else if (tilesetNum == 0x17) //npc regu
			srcImg = iMan.getImg(exeData.getNpcRegu());
		else if (tilesetNum == 0x2) //map tileset
			srcImg = iMan.getImg(data.getTileset());
		else if (tilesetNum == 0x10) //npc myChar
			srcImg = iMan.getImg(exeData.getMyCharFile());
		else
			return;
		int srcW = frameRect.width - frameRect.x;
		int srcH = frameRect.height - frameRect.y;
		int srcX = frameRect.x;
		int srcY = frameRect.y;
		int srcX2 = frameRect.width;
		int srcY2 = frameRect.height;
		if (exeData.getConfig().getTileSize() == 16) {
			srcX /= 2;
			srcY /= 2;
			srcX2 /= 2;
			srcY2 /= 2;
		}
		
		g2d.drawImage(srcImg,1, 1, srcW + 1, srcH + 1,
				srcX, srcY, srcX2, srcY2, this);
	}		
}
