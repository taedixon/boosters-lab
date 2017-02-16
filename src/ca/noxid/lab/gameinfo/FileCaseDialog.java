package ca.noxid.lab.gameinfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

public class FileCaseDialog extends JFrame implements ActionListener {
	private static final long serialVersionUID = -8871341512354301742L;
	private GameInfo exe;
	
	private Map<String, String> mismatches;
	private Set<String> missing;
	private Set<String> orphans;
	
	public FileCaseDialog(GameInfo game) {
		super();
		mismatches = new HashMap<>();
		missing = new HashSet<>();
		orphans = new HashSet<>();
		exe = game;
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
		findMatches();
		
		contentPane.add(new JLabel("mismatches"));
		JList<String> mismatchesLeft = new JList<>();
		JList<String> mismatchesRight = new JList<>();
		Vector<String> leftData = new Vector<>();
		Vector<String> rightData = new Vector<>();
		for (String gamefile : mismatches.keySet()) {
			leftData.add(gamefile);
			rightData.add(mismatches.get(gamefile));			
		}
		mismatchesLeft.setListData(leftData);
		mismatchesRight.setListData(rightData);
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.LINE_AXIS));
		listPane.add(mismatchesLeft);
		listPane.add(mismatchesRight);		
		JScrollPane jsp = new JScrollPane(listPane);
		jsp.setMaximumSize(new Dimension(9999, 300));
		contentPane.add(jsp);
		
		contentPane.add(new JLabel("referenced but missing"));
		JList<String> missingList = new JList<>();
		missingList.setListData(missing.toArray(new String[missing.size()]));
		jsp = new JScrollPane(missingList);
		jsp.setMaximumSize(new Dimension(9999, 300));
		contentPane.add(jsp);
		
		contentPane.add(new JLabel("Found but not referenced"));
		JList<String> orphanList = new JList<>();
		orphanList.setListData(orphans.toArray(new String[orphans.size()]));
		jsp = new JScrollPane(orphanList);
		jsp.setMaximumSize(new Dimension(9999, 300));
		contentPane.add(jsp);
		
		this.setContentPane(contentPane);
		this.pack();
		this.setVisible(true);
	}
	
	private void findMatches() {
		Set<File> datafiles = getDataFiles();	
		Set<File> matches = new HashSet<>();
		int ddirlen = exe.getDataDirectory().getAbsolutePath().length();
		for (File gamefile : exe.allGameFiles()) {
			boolean matched = false;
			String gf = gamefile.getAbsolutePath().substring(ddirlen);
			for (File dirfile : datafiles) {
				String df = dirfile.getAbsolutePath().substring(ddirlen);
				if (gamefile.getAbsolutePath().equalsIgnoreCase(dirfile.getAbsolutePath())) {
					matches.add(dirfile);
					if (!gamefile.getAbsolutePath().equals(dirfile.getAbsolutePath())) {
						mismatches.put(gf, df);
					}
					matched = true;
					break;
				}
			}
			if (!matched) {
				missing.add(gf);
			}
		}
		datafiles.removeAll(matches);
		orphans = new HashSet<>();
		for (File f : datafiles) {
			orphans.add(f.getAbsolutePath().substring(ddirlen));
		}
	}
	
	private Set<File> getDataFiles() {
		Set<File> fl = new HashSet<>();
		Stack<File> dirs = new Stack<>();
		dirs.add(exe.getDataDirectory());
		while (!dirs.isEmpty()) {
			File dir = dirs.pop();
			//noinspection ConstantConditions
			for (File f : dir.listFiles()) {
				if (f.isDirectory()) {
					dirs.push(f);
				} else {
					fl.add(f);
				}
			}
		}
		return fl;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
