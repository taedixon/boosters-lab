//youaresuchanidiotohmygodpaddingpaddingpadding
package ca.noxid.lab.gameinfo;

import ca.noxid.lab.EditorApp;
import ca.noxid.lab.Messages;
import ca.noxid.lab.rsrc.ResourceManager;
import ca.noxid.uiComponents.BgPanel;
import com.carrotlord.string.StrTools;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class HackDialog extends JDialog implements TreeSelectionListener{
	private static final long serialVersionUID = 8505384969881585066L;
	private static final String ATTR_POP_FROM_EXE = "populatefromexe";
	private static final String ATTR_SIZE = "size";
	private static final String ATTR_LENGTH = "length";
	private static final String ATTR_OFFSET = "offset";
	private static final String ATTR_TYPE = "type";
		private static final String FIELDTYPE_TEXT = "text";
		private static final String FIELDTYPE_LABEL = "label";
		private static final String FIELDTYPE_DATA = "data";
		private static final String FIELDTYPE_FLAG = "flags";
		private static final String FIELDTYPE_INFO = "info";
		private static final String FIELDTYPE_CHECK = "check";
		private static final String FIELDTYPE_IMAGE = "image";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TITLE = "title";
	private static final String ATTR_COL = "col";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_DEFVALUE = "defvalue";
	private static final String ATTR_SRC = "src";
	
	private static final String NODE_PANEL = "panel";
	private static final String NODE_FIELD = "field";
	private static final String NODE_HACK = "hack";
	private static final String NODE_TEXT = "text";
	private static final String NODE_CHECKBOX = "checkbox";
	private static final String NODE_CHECKED = "checked";
	private static final String NODE_UNCHECKED = "unchecked";
	
	

	private CSExe exe;
	private JPanel hackPanel;
	private DefaultTreeModel tModel;
	private JTree hackList;
	private ArrayList<PluggableComponent> components;
	private JButton commitButton = new JButton(new AbstractAction() {
		private static final long serialVersionUID = -2218624552883815326L;

		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("Commit hack");
		}
		
	});
	private static File hackDir = new File("./hacks");
	private ResourceManager iMan;

	public HackDialog(Frame aFrame, CSExe exe, ResourceManager iMan) {
		super(aFrame, true);
		if (EditorApp.blazed)
			this.setCursor(ResourceManager.cursor);
		this.iMan = iMan;
		this.setLocation(aFrame.getLocation());
		this.setTitle(Messages.getString("EditorApp.110"));
		
		this.exe = exe;
		hackPanel = new BgPanel(iMan.getImg(ResourceManager.rsrcBgBlue));
		//create the tree
				DefaultMutableTreeNode top = new DefaultMutableTreeNode("the root");
				tModel = new DefaultTreeModel(top);
				createNodes(top, hackDir);
				hackList = new JTree(tModel);
				hackList.addTreeSelectionListener(this);
				hackList.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
				hackList.setRootVisible(false);
				hackList.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		addComponentsToPane(this.getContentPane());
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		commitButton.setAction(new AbstractAction() {
			private static final long serialVersionUID = -8644389687135618723L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String actions = "Applying patches\n";
				for (PluggableComponent comp : components) {
					List<Patch> blist = comp.getData();
					if (blist != null) {
						for (Patch b : blist) {
							actions += String.format("Patching address:0x%x\n", b.offset);
							actions += "\tSize: " + b.buf.remaining() + "\n";
							actions += "\tValue: ";
							for (Byte by : b.buf.array()) {
								actions += String.format("%2x ", by);
							}
							actions += "\n";
							int off = b.offset;
							if (off != 0)
								HackDialog.this.exe.patch(b.buf, off);
							else 
								actions += "ABORTING PATCH: zero offset specified\n";
						}
					}
				}
				actions += "Committing changes\n";
				HackDialog.this.exe.commit();
				actions += "Committed.\n";
				
				JTextPane txt = new JTextPane();
				JScrollPane jsp = new JScrollPane(txt);
				jsp.setPreferredSize(new Dimension(240, 400));
				txt.setText(actions);
				
				JOptionPane.showMessageDialog(HackDialog.this, jsp);
			}
			
		});
		commitButton.setText("Commit!");
		
		this.pack();
		this.setSize(640, 480);
		this.setVisible(true);
	}
	
	private void createNodes(DefaultMutableTreeNode base, File basedir) {
		//noinspection ConstantConditions
		for (File f : basedir.listFiles()) {
			if (f.isDirectory()) {
				DefaultMutableTreeNode nn = new DefaultMutableTreeNode(f.getName());
				tModel.insertNodeInto(nn, base, base.getChildCount());
				createNodes(nn, f);
			} else {
				HackNode content = new HackNode(f.getName(), f);
				tModel.insertNodeInto(new DefaultMutableTreeNode(content), base, base.getChildCount());
			}
			
		}
	}
	
	private class HackNode{
		String name;
		File loc;
		HackNode(String n, File l) {
			name = n.replaceAll(".xml", "");
			loc = l;
		}
		
		public String toString() {return name;}
	}
	
	private void addComponentsToPane(Container c) {
		JScrollPane jsp = new JScrollPane(hackList);
		jsp.setPreferredSize(new Dimension(150, 320));
		
		c.add(jsp, BorderLayout.WEST);
		hackPanel.setPreferredSize(new Dimension(320, 320));
		hackPanel.setLayout(new BorderLayout());
		jsp = new JScrollPane(hackPanel);
		c.add(jsp);
	}

	private void loadHack(File loc) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		components = new ArrayList<>();
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(loc);
			parseDoc(doc);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private void parseDoc(Node n) {
	    int type = n.getNodeType();
	    switch (type) {
	        case Node.ATTRIBUTE_NODE:
	            //out.print("ATTR:");
	            //printlnCommon(n);
	            break;

	        case Node.DOCUMENT_NODE:
	            //out.print("DOC:");
	            //printlnCommon(n);
	            break;

	        case Node.ELEMENT_NODE:
	        	String name = n.getNodeName().toLowerCase();
	            if (name.equals(NODE_HACK)) {
	    	    	hackPanel.removeAll();
	    	    	hackPanel.add(parseHack(n), BorderLayout.CENTER);
	    	    	hackPanel.add(commitButton, BorderLayout.SOUTH);
	    	    	hackPanel.revalidate();
	            }
	        case Node.TEXT_NODE:
	            //out.print("TEXT:");
	            //printlnCommon(n);
	            break;

	        default:
	            System.out.print("UNSUPPORTED NODE: " + type);
	            //printlnCommon(n);
	            break;
	    }

	    for (Node child = n.getFirstChild(); child != null;
		         child = child.getNextSibling()) {
		        parseDoc(child);
		    }
	    //StrTools.msgBox("Error loading hack: invalid xml");
	}
	
	private JPanel parseHack(Node n) {
		JPanel pane = new BgPanel(iMan.getImg(ResourceManager.rsrcBgBlue));
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		
        NamedNodeMap atts = n.getAttributes();
        Node s = atts.getNamedItem(ATTR_NAME);
    	if (s != null) {
		    String hackName = s.getNodeValue();
    		pane.setBorder(BorderFactory.createTitledBorder(hackName));
        }
		
		for (Node child = n.getFirstChild(); child != null;
		         child = child.getNextSibling()) {
			String cType = child.getNodeName().toLowerCase();
			//System.out.println(cType);
	        switch (child.getNodeType()) {
	        case Node.ELEMENT_NODE:
	        	if (cType.equals(NODE_PANEL)) {
	        		pane.add(parsePanel(child));
	        	} else if (cType.equals(NODE_FIELD)) {
	        		pane.add(parseField(child, components));
	        	}
	        	break;
	        }
	        
	    }
		return pane;
	}
	
	private Component parsePanel(Node n) {
		JPanel pane = new JPanel();
		JScrollPane jsp = new JScrollPane(pane);
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.ipadx = 2;
		c.ipady = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		
        NamedNodeMap atts = n.getAttributes();
        Node s = atts.getNamedItem(ATTR_TITLE);
    	if (s != null) {
    		String title = s.getNodeValue();
    		pane.setBorder(BorderFactory.createTitledBorder(title));
        }
		
		for (Node child = n.getFirstChild(); child != null;
		         child = child.getNextSibling()) {
			String cType = child.getNodeName().toLowerCase();
			//System.out.println(cType);
	        switch (child.getNodeType()) {
	        case Node.ELEMENT_NODE:
        		NamedNodeMap att = child.getAttributes();
        		Node childatt;
        		if ((childatt = att.getNamedItem(ATTR_COL)) != null) {
        			c.gridx = Integer.parseInt(childatt.getNodeValue());
        		}
	        	if (cType.equals(NODE_PANEL)) {
	        		pane.add(parsePanel(child), c);
	        		//c.gridy++;
	        	} else if (cType.equals(NODE_FIELD)) {
	        		Component comp = parseField(child, components);
	        		if (comp != null) {
	        			pane.add(comp, c);
	        			//c.gridy++;
	        		}
	        	}
	        	break;
	        }
	        
	    }
		return jsp;
	}
	
	private Component parseField(Node n, List<PluggableComponent> comps) {
		PluggableComponent rv = null;
		
        NamedNodeMap atts = n.getAttributes();
        Node s = atts.getNamedItem(ATTR_TYPE);
    	if (s != null) {
    		String fieldtype = s.getNodeValue();
    		int offset = 0;
    		int dataSize;
    		if ((s = atts.getNamedItem(ATTR_OFFSET)) != null) {
    			String offstr = s.getNodeValue();
    			offset = Integer.parseInt(offstr.substring(offstr.indexOf('x') +1), 16);
    		}
		    switch (fieldtype) {
		    case FIELDTYPE_TEXT:
			    int len = 4;
			    if ((s = atts.getNamedItem(ATTR_SIZE)) != null) {
				    String lenstr = s.getNodeValue();
				    len = Integer.parseInt(lenstr);
			    }

			    rv = new PluggableTextField(offset, len);
			    break;
		    case FIELDTYPE_DATA:
			    rv = new PluggableDataField();
			    if (atts.getNamedItem(ATTR_OFFSET) != null) {
				    rv.addAttribute(ATTR_OFFSET, offset + "");
			    }
			    break;
		    case FIELDTYPE_LABEL:
			    rv = new PluggableLabel();
			    break;
		    case FIELDTYPE_FLAG:
			    rv = new PluggableFlagField(n);
			    if ((s = atts.getNamedItem(ATTR_SIZE)) != null) {
				    rv.addAttribute(ATTR_SIZE, s.getNodeValue());
			    }
			    if (atts.getNamedItem(ATTR_OFFSET) != null) {
				    rv.addAttribute(ATTR_OFFSET, offset + "");
			    }
			    break;
		    case FIELDTYPE_INFO:
			    rv = new PluggableTextPane();
			    break;
		    case FIELDTYPE_CHECK:
			    rv = new PluggableCheckbox(n);

			    if ((s = atts.getNamedItem(ATTR_NAME)) != null) {
				    rv.addAttribute(ATTR_NAME, s.getNodeValue());
			    }
			    break;
		    case FIELDTYPE_IMAGE:
		    	ImageIcon img = null;
		    	if ((s = atts.getNamedItem(ATTR_SRC)) != null) {
		    		File src = new File(s.getNodeValue());
		    		if (src.exists()) {
		    			img = new ImageIcon(Toolkit.getDefaultToolkit().getImage(src.getAbsolutePath()));
		    		}
		    	}
		    	
		    	rv = new PluggableLabel(img);
		    	break;
		    }

		    assert rv != null;
    		if ((s = atts.getNamedItem(ATTR_SIZE)) != null) {
    			dataSize = Integer.parseInt(s.getNodeValue());
			    rv.addAttribute(ATTR_SIZE, dataSize + "");
    		}
    		if ((s = atts.getNamedItem(ATTR_POP_FROM_EXE)) != null) {
    			rv.addAttribute(ATTR_POP_FROM_EXE, s.getNodeValue());
    		} else if ((s = atts.getNamedItem(ATTR_DEFVALUE)) != null) {
    			rv.addAttribute(ATTR_VALUE, s.getNodeValue());
    		}
        }
    	
    	for (Node child = n.getFirstChild(); child != null;
		         child = child.getNextSibling()) {
			String cType = child.getNodeName().toLowerCase();
			//System.out.println(cType);
	        switch (child.getNodeType()) {
	        case Node.TEXT_NODE:
	        	if (rv != null)
	        		rv.addText(child.getNodeValue().trim());
	        	break;
	        }
	        
	    }
    	if (rv != null)
    		comps.add(rv);
    	return (Component) rv;
	}

	private interface PluggableComponent  {
		abstract int getOffset();
		abstract List<Patch> getData();
		abstract void addText(String txt);
		abstract void addAttribute(String name, String value);
	}
	
	private class PluggableTextField extends JTextField implements PluggableComponent {
		int offset = 0;
		int sz;
		private static final long serialVersionUID = 3957536901297814486L;

		PluggableTextField(int offset, int len) {
			super();
			this.offset = offset;
			
			setColumns(8);
			ByteBuffer bb = exe.read(offset, len);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			int val;
			sz = len;
			switch (len) {
			case 4:
				val = bb.getInt(0);
				break;
			case 2:
				val = bb.getShort(0);
				break;
			case 1:
				val = bb.get(0);
				break;
			default:
				setText(StrTools.CString(bb.array()));
				return;
			}
			setText(val + "");
		}
		
		public int getOffset() {return offset;}

		@Override
		public List<Patch> getData() {
			if (offset == 0) return null;
			ByteBuffer bb = ByteBuffer.allocate(sz);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			int tv = 0;
			if (sz == 4 || sz == 2 || sz == 1) {
				try {
					tv = Integer.parseInt(getText());
				} catch (NumberFormatException ignored) {
					return null;
				}
			}
			
			switch (sz) {
			case 4:
				bb.putInt(tv);
				break;
			case 2:
				bb.putShort((short) tv);
				break;
			case 1:
				bb.put((byte) tv);
				break;
			default: //text content, probably
				char[] textval = getText().toCharArray();
				int xfer = 0;
				for (char c : textval) {
					bb.put((byte) c);
					if (++xfer>sz-1) break;
				}
				bb.put((byte) 0);
				break;
			}
			bb.flip();
			LinkedList<Patch> ll = new LinkedList<>();
			ll.add(new Patch(bb, offset));
			return ll;
		}

		@Override
		public void addText(String txt) {
			this.setText(txt);			
		}

		@Override
		public void addAttribute(String name, String value) {
			// TODO Auto-generated method stub
			
		}
	}
	
	class PluggableDataField extends JScrollPane implements PluggableComponent {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4695820672436845386L;
		String data;
		int offset;
		JTextPane display = new JTextPane();
		PluggableDataField() {
			this.setViewportView(display);
			display.setEnabled(false);
			setPreferredSize(new Dimension(200, 80));
		}
		
		@Override
		public int getOffset() {
			return offset;
		}

		@Override
		public List<Patch> getData() {
			int sz = data.length()/2;
			ByteBuffer rv = ByteBuffer.allocate(sz);
			for (int i = 0; i < sz; i++) {
				byte val = (byte) Integer.parseInt(data.substring(i*2, i*2+2), 16);
				rv.put(val);
			}
			rv.flip();
			LinkedList<Patch> ll = new LinkedList<>();
			ll.add(new Patch(rv, offset));
			return ll;
		}

		@Override
		public void addText(String txt) {

			data = txt.replaceAll("\\s+", "");
			display.setText(txt.replaceAll("\\s+", " "));
		}

		@Override
		public void addAttribute(String name, String value) {
			if (name.equals(ATTR_OFFSET)) {
				offset = decodeHex(value);
			}
		}
		
	}

	class PluggableLabel extends JLabel implements PluggableComponent {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6896681669212669152L;
		
		public PluggableLabel(Icon icon) {
			this.setIcon(icon);
		}
		
		public PluggableLabel() {
			this(null);
		}

		@Override
		public int getOffset() {
			return 0;
		}

		@Override
		public List<Patch> getData() {
			return null;
		}

		@Override
		public void addText(String txt) {
			if (this.getIcon() == null)
				this.setText(txt);
		}

		@Override
		public void addAttribute(String name, String value) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	class PluggableFlagField extends JPanel implements PluggableComponent {

		private static final long serialVersionUID = -4156675620479299519L;
		int offset;
		int sz;
		
		LinkedList<NCheckBox> boxes = new LinkedList<>();
		
		PluggableFlagField(Node n) {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			
			for (Node child = n.getFirstChild(); child != null;
			         child = child.getNextSibling()) {

				String cType = child.getNodeName().toLowerCase();
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					if (cType.equals(NODE_CHECKBOX)) {
						NCheckBox chk = parseCheckbox(child);
						boxes.add(chk);
						add(chk);
					}
				}
			}//check each child node
		}
		
		@Override
		public int getOffset() {
			return offset;
		}

		@Override
		public List<Patch> getData() {
			ByteBuffer rv = ByteBuffer.allocate(sz);
			rv.order(ByteOrder.LITTLE_ENDIAN);
			int retval = 0;
			for (NCheckBox chk : boxes) {
				if (chk.isSelected())
					retval |= chk.value;
			}
			switch (sz) {
			default:
			case 4:
				rv.putInt(retval);
				break;
			case 2:
				rv.putShort((short)(retval & 0xFFFF));
				break;
			case 1:
				rv.put((byte)(retval & 0xFF));
				break;
			}
			rv.flip();
			LinkedList<Patch> ll = new LinkedList<>();
			ll.add(new Patch(rv, offset));
			return ll;
		}

		@Override
		public void addText(String txt) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addAttribute(String name, String value) {
			if (name.equals(ATTR_OFFSET)) {
				offset = Integer.parseInt(value);
			} else if (name.equals(ATTR_SIZE)) {
				sz = Integer.parseInt(value);
			}
			
			if (offset != 0 && sz != 0) {
				//fill flags
				ByteBuffer dat = exe.read(offset, sz);
				int i = 0;
				for (NCheckBox check : boxes) {
					byte bv = dat.get(i/8);
					check.setSelected((bv & 1<<(i%8)) != 0);
					if (++i>sz*8) break;
				}
			}
		}
		
	}
	
	private class PluggableTextPane extends JScrollPane implements PluggableComponent {

		private static final long serialVersionUID = 4626948517206680729L;
		JTextPane textpane = new JTextPane();
		PluggableTextPane() {
			this.setViewportView(textpane);
			textpane.setEditable(false);
			textpane.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
			setPreferredSize(new Dimension(200, 80));
		}
		
		@Override
		public int getOffset() {
			return 0;
		}

		@Override
		public List<Patch> getData() {
			return null;
		}

		@Override
		public void addText(String txt) {
			textpane.setText(txt.replaceAll("\\s+", " "));		
		}

		@Override
		public void addAttribute(String name, String value) {	
		}
		
	}
	
	private class PluggableCheckbox extends JCheckBox implements PluggableComponent {

		private static final long serialVersionUID = 1L;
		private Node checkNode;
		private Node uncheckNode;
		
		PluggableCheckbox(Node n) {
			for (Node child = n.getFirstChild(); child != null;
			         child = child.getNextSibling()) {
				switch (child.getNodeType()) {
				case Node.ELEMENT_NODE:
					String ctype = child.getNodeName().toLowerCase();
					if (ctype.equals(NODE_CHECKED)) {
						checkNode = child;
					}
					if (ctype.equals(NODE_UNCHECKED)) {
						uncheckNode = child;
					}
				}
			}
		}

		@Override
		public int getOffset() {
			return 0;
		}

		@Override
		public List<Patch> getData() {
			List<Patch> rv = new LinkedList<>();
			List<PluggableComponent> comps = new LinkedList<>();
			Node using;
			if (this.isSelected()) {
				using = checkNode;
			} else {
				using = uncheckNode;
			}
			for (Node child = using.getFirstChild(); child != null;
			         child = child.getNextSibling()) {
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					if (child.getNodeName().toLowerCase().equals(NODE_FIELD)) {
						parseField(child, comps);
					}
				}
			}
			for (PluggableComponent c : comps) {
				rv.addAll(c.getData());
			}
			
			return rv;
		}

		@Override
		public void addText(String txt) {
			this.setText(this.getText() + txt);
		}

		@Override
		public void addAttribute(String name, String value) {
			if (name.equals(ATTR_NAME)) {
				this.setText(value);
			}
		}
		
	}
	
	private class Patch {
		ByteBuffer buf;
		int offset;
		
		Patch(ByteBuffer b, int offset2) {
			buf = b; offset = offset2;
		}
	}
	
	public NCheckBox parseCheckbox(Node n) {
		NCheckBox rv = new NCheckBox();
		NamedNodeMap att = n.getAttributes();
		Node attrib;
		if ((attrib = att.getNamedItem(ATTR_NAME)) != null) {
			rv.setText(attrib.getNodeValue());
		}
		if ((attrib = att.getNamedItem(ATTR_VALUE)) != null) {
			String nv = attrib.getNodeValue();
			rv.value = decodeHex(nv);
		}
		
		return rv;
	}
	
	private class NCheckBox extends JCheckBox {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2909607705303492378L;
		int value;
	}
	
	private int decodeHex(String nv) {
		if (nv.contains("x")) {
			nv = nv.substring(nv.indexOf('x')+1);
			return Integer.parseInt(nv, 16);
		} else {
			return Integer.parseInt(nv);
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent eve) {
		//Returns the last path element of the selection.
		//This method is useful only when the selection model allows a single selection.
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
	                       ((JTree) eve.getSource()).getLastSelectedPathComponent();

	    if (node == null)
	    //Nothing is selected.     
	    return;

	    Object nodeInfo = node.getUserObject();
	    if (node.isLeaf()) {
	    	HackNode book = (HackNode)nodeInfo;
	    	//System.out.println(book.location);
	        loadHack(book.loc);
	    }
	}
}
