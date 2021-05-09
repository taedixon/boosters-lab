package ca.noxid.lab;

import ca.noxid.lab.rsrc.ResourceManager;

import com.carrotlord.string.StrTools;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

//import java.awt.Color;
//import java.awt.Frame;
//import java.io.InputStream;


public class HelpDialog extends JFrame implements TreeSelectionListener, HyperlinkListener {

	private static final long serialVersionUID = -638474828863650692L;
	private DefaultTreeModel tModel;
	private JEditorPane display;
	//private static final Color listBG = Color.decode("0x919DA5");
	//private Vector<URL> resourceVec;

	/**
	 * Constructs a (hopefully) generic help dialog from
	 * a class that has a "help" resource folder containing
	 * at least an "index.txt"
	 *
	 * @param helpDir The class of the application which holds the tutorial resources
	 */
	HelpDialog(URL helpDir) {
		super();
		if (EditorApp.blazed) {
			this.setCursor(ResourceManager.cursor);
		}
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setTitle(Messages.getString("HelpDialog.0")); //$NON-NLS-1$
		Scanner sc = null;
		try {
			sc = new Scanner(new URL(helpDir + File.separator + "index.txt").openStream()); //$NON-NLS-1$
		} catch (FileNotFoundException err) {
			StrTools.msgBox(Messages.getString("HelpDialog.2")); //$NON-NLS-1$
			System.out.println(err.getMessage());
			err.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		//create the tree
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("the root");
		tModel = new DefaultTreeModel(top);
		createNodes(top, sc, helpDir);
		JTree index = new JTree(tModel);
		index.addTreeSelectionListener(this);
		index.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		index.setRootVisible(false);
		index.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		display = new JEditorPane();
		display.addHyperlinkListener(this);
		display.setEditable(false);
		display.setContentType("text/html"); //$NON-NLS-1$
		try {
			display.setPage(new URL(helpDir + File.separator + "splash.htm")); //$NON-NLS-1$
		} catch (IOException e) {
			e.printStackTrace();
		}
		JScrollPane indexScroll = new JScrollPane(index);
		JScrollPane editScroll = new JScrollPane(display);
		this.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, indexScroll, editScroll));
		this.setSize(640, 400);
		indexScroll.setMinimumSize(new Dimension(110, 400));
		this.validate();
	}

	private void createNodes(DefaultMutableTreeNode root, Scanner sc, URL helpDir) {
		DefaultMutableTreeNode category = root;
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.startsWith("!")) { //is a category //$NON-NLS-1$
				if (line.equals("!!")) {
					category = (DefaultMutableTreeNode) category.getParent();
				} else {
					DefaultMutableTreeNode newCat = new DefaultMutableTreeNode(line.substring(1));
					tModel.insertNodeInto(newCat, category, category.getChildCount());
					category = newCat;
				}
			} else { //is a document
				int semiIndex = line.indexOf(';');
				String name, file;
				if (semiIndex == -1) {
					//improperly formatted! for shame...
					name = line;
					file = line;
				} else {
					file = line.substring(0, semiIndex);
					name = line.substring(semiIndex + 1);
				}
				//System.out.println(file);
				try {
					HelpSubject content = new HelpSubject(
							name,
							new URL(helpDir + File.separator + file));
					//category.add(new DefaultMutableTreeNode(content));
					tModel.insertNodeInto(new DefaultMutableTreeNode(content),
							category, category.getChildCount());
				} catch (MalformedURLException err) {
					err.printStackTrace();
				}
			}
		}
	}

	private class HelpSubject {
		String name;
		URL location;

		HelpSubject(String n, URL u) {
			name = n;
			location = u;
		}

		public String toString() {
			return name;
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
		{
			return;
		}

		HelpSubject book = null;
		try {
			Object nodeInfo = node.getUserObject();
			System.out.println(nodeInfo);
			if (node.isLeaf()) {
				book = (HelpSubject) nodeInfo;
				//System.out.println(book.location);
				File f = new File(book.location.getFile());
				if (!f.exists())
					throw new FileNotFoundException(f.getAbsolutePath());
				display.setPage(book.location);
			}
		} catch (IOException err) {
			err.printStackTrace();
			StrTools.msgBox(Messages.getString("HelpDialog.3") + new File(book.location.getFile()).getAbsolutePath()); //$NON-NLS-1$
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent event) {
		if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			try {
				URL link = event.getURL();
				File f = new File(link.getFile());
				if (!f.exists())
					throw new FileNotFoundException(f.getAbsolutePath());
				display.setPage(link);
				display.setCursor(null);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				StrTools.msgBox(Messages.getString("HelpDialog.4") + new File(event.getURL().getFile()).getAbsolutePath()); //$NON-NLS-1$
			}
		}
	}
}
