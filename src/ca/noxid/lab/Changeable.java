package ca.noxid.lab;

public interface Changeable {
	public static final String PROPERTY_EDITED = "changeable_edited_property"; //$NON-NLS-1$

	public boolean isModified();

	public void markUnchanged();

	public void markChanged();
}
