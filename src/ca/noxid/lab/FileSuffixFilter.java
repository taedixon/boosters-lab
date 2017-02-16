package ca.noxid.lab;

import java.io.File;
import java.io.FilenameFilter;

public class FileSuffixFilter implements FilenameFilter {

	String suffix;

	public FileSuffixFilter(String s) {
		suffix = s;
		if (suffix == null) suffix = "";
	}

	@Override
	public boolean accept(File dir, String name) {
		return name.endsWith(suffix);
	}

}
