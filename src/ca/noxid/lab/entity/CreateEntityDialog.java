package ca.noxid.lab.entity;


import ca.noxid.uiComponents.FormattedUpdateTextField;
import ca.noxid.uiComponents.UpdateTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Vector;

public class CreateEntityDialog extends JDialog {
	private static final long serialVersionUID = -4046455183490545635L;
	private static final java.text.NumberFormat nf =
			FormattedUpdateTextField.getNumberOnlyFormat(1, 4);
	private JTextField short1f = new UpdateTextField("NEW");
	private JTextField short2f = new UpdateTextField("NPC");
	private JTextField longnamef = new UpdateTextField("New Entity");
	private JTextField rectLf = new FormattedUpdateTextField(nf);
	private JTextField rectUf = new FormattedUpdateTextField(nf);
	private JTextField rectRf = new FormattedUpdateTextField(nf);
	private JTextField rectDf = new FormattedUpdateTextField(nf);
	private JTextPane descf = new JTextPane();
	private JList<String> categories = new JList<>();
	private Vector<String> listContent = new Vector<>();
	private JTextField catf = new UpdateTextField("CATEGORY");
	private JTextField subcatf = new UpdateTextField("SUBCATEGORY");
	
	public EntityData ent;

	CreateEntityDialog(Frame aFrame) {
		super(aFrame, true);
		this.setTitle("New Entity Metadata");
		JPanel pane = new JPanel();
		JScrollPane jsp;
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		JPanel namePane = new JPanel();
		namePane.setLayout(new BoxLayout(namePane, BoxLayout.Y_AXIS));
		namePane.setBorder(BorderFactory.createTitledBorder("Short Name"));
		namePane.add(short1f);
		namePane.add(short2f);
		pane.add(namePane);
		//pane.add(new JLabel("Long Name"));
		namePane = new JPanel();
		namePane.setLayout(new BoxLayout(namePane, BoxLayout.Y_AXIS));
		namePane.setBorder(BorderFactory.createTitledBorder("Long Name"));
		namePane.add(longnamef);
		pane.add(namePane);
		pane.add(buildRectPane());
		//pane.add(new JLabel("Description:"));
		namePane = new JPanel();
		namePane.setLayout(new BoxLayout(namePane, BoxLayout.Y_AXIS));
		namePane.setBorder(BorderFactory.createTitledBorder("Description"));
		jsp = new JScrollPane(descf);
		descf.setPreferredSize(new Dimension(152, 100));
		descf.setText("This entity has not yet been defined");
		namePane.add(jsp);
		pane.add(namePane);
		jsp = new JScrollPane(categories);
		categories.setPreferredSize(new Dimension(160, 100));
		JPanel buttPane = new JPanel();
		buttPane.setLayout(new BoxLayout(buttPane, BoxLayout.Y_AXIS));
		buttPane.setBorder(BorderFactory.createTitledBorder("Categories"));
		buttPane.add(jsp);
		buttPane.add(new JLabel(" ")); //spacer
		buttPane.add(catf);
		buttPane.add(subcatf);
		JPanel buttSubpane = new JPanel();
		JButton button;
		button = new JButton(new AbstractAction() {

			private static final long serialVersionUID = -6866172728788089012L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String cat  = catf.getText().toUpperCase();
				String subcat = subcatf.getText().toUpperCase();
				if (!cat.equals("")&&!subcat.equals("")) {
					String put = cat+":"+subcat;
					if (!listContent.contains(put)) {
						listContent.add(put);
						categories.setListData(listContent);
					}
				}
			}
			
		});
		button.setText("ADD CATEGORY");
		buttSubpane.add(button);
		button = new JButton(new AbstractAction() {

			private static final long serialVersionUID = -6447179322287448588L;

			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> sel = categories.getSelectedValuesList();
				listContent.removeAll(sel);
				categories.setListData(listContent);
			}
			
		});
		button.setText("DEL CATEGORY");
		buttSubpane.add(button);
		buttPane.add(buttSubpane);
		pane.add(buttPane);
		JPanel actionsPane = new JPanel();
		button = new JButton(new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				ent = new EntityData(-1);
				ent.setName(longnamef.getText());
				ent.setShort1(short1f.getText());
				ent.setShort2(short2f.getText());
				ent.setFramerect(new java.awt.Rectangle(
					Integer.parseInt(rectLf.getText()),
					Integer.parseInt(rectUf.getText()),
					Integer.parseInt(rectRf.getText()),
					Integer.parseInt(rectDf.getText())
				));
				ent.setDesc(descf.getText());
				for(String s : listContent) {
					int index = s.indexOf(':');
					ent.addSubcat(s.substring(0, index), s.substring(index+1));
				}
				dispose();
			}			
		});
		button.setText("ACCEPT");
		actionsPane.add(button);
		button = new JButton(new AbstractAction() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				ent = null;
				dispose();
			}			
		});
		button.setText("CANCEL");
		actionsPane.add(button);
		pane.add(actionsPane);
		
		this.setContentPane(pane);
		
		Point ep = aFrame.getLocationOnScreen();
		ep.x += aFrame.getWidth()/2;
		ep.y += aFrame.getHeight()/2;
		this.pack();
		ep.x -= this.getWidth()/2;
		ep.y -= this.getHeight()/2;
		this.setLocation(ep);
		this.setVisible(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	private JPanel buildRectPane() {
		JPanel retVal = new JPanel();
		retVal.setBorder(BorderFactory.createTitledBorder("Preview Rect")); //$NON-NLS-1$
		retVal.setLayout(new GridLayout(3, 3));
		retVal.add(new JPanel());
		retVal.add(this.rectUf);
		rectUf.setText("48");
		//hitboxU.addFocusListener(this);
		retVal.add(new JPanel());
		retVal.add(this.rectLf);
		rectLf.setText("48");
		//hitboxL.addFocusListener(this);
		retVal.add(new JPanel());
		retVal.add(this.rectRf);
		rectRf.setText("64");
		//hitboxR.addFocusListener(this);
		retVal.add(new JPanel());
		retVal.add(this.rectDf);
		rectDf.setText("64");
		//hitboxD.addFocusListener(this);
		return retVal;
	}
}
