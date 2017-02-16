package com.carrotlord.string;

//Language = Java 6
//credit to carrotlord for pretty much the entire contents of this file
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

public class StrTools {

	//Before using this method, make sure the textbox is selected by the user.

	/**component = The text component that the user is pressing Ctrl+F
	            in. For example, if your text editor box is named
	            myTextBox, then use myTextBox as a passed argument.
	            (This should work with JTextArea, JTextPane, and JEditorPane).
	textToFind = If the user wants to find the text "Kitty", you should
	             pass "Kitty" as an argument for this.
	isWrappedDocument = Is the document wrapped around infinitely? If not,
	                    searching will stop at the end of the document.
	                    If yes, searching will begin again at the beginning
	                    of document until the search item is found.
	isCaseSensitive = Do we want to match capitalization or not?
	isDirectionDown = Are we searching downwards? If not, then search upwards.*/
	public static void findAndSelectTextInTextComponent(JTextComponent component,
	                                      String textToFind,
	                                      boolean isWrappedDocument,
	                                      boolean isCaseSensitive,
	                                      boolean isDirectionDown) {
	    int currentPosition = component.getCaretPosition();
	    int returns = 0;
	    //weed out carriage return characters
	    String txt = component.getText();
	    for (int i = 0; i < currentPosition; i++)
	    {
	    	if (txt.charAt(i) == '\r')
	    	{
	    		currentPosition++;
	    		returns++;
	    	}
	    }
	    int foundPosition = findTextInTextComponent(component,textToFind,
	                                                currentPosition,isWrappedDocument,
	                                                isCaseSensitive,isDirectionDown);
	    if (foundPosition == -1 && !isWrappedDocument) {
	        String textBoxContents = component.getText();
	        boolean doesQueryExistInDocumentIgnoringCase =
	                (!containsIgnoreCase(textBoxContents,textToFind) &&
	                 !isCaseSensitive);
	        boolean doesQueryExistInDocumentWithCase =
	                (!textBoxContents.contains(textToFind) && isCaseSensitive);
	        if (doesQueryExistInDocumentIgnoringCase ||
	                doesQueryExistInDocumentWithCase) {
	            msgBox("Cannot find \""+textToFind+"\" inside this\n"+
	                          "this document.");
	        } else {
	            if (isDirectionDown)
	                msgBox("Reached end of document.");
	            else
	                msgBox("Reached beginning of document.");
	        }
	    } else if (foundPosition == -1) {
	        //We know the document must be wrapped. We really actually can't find the
	        //text:
	        msgBox("Cannot find \""+textToFind+"\" inside this\n"+
	                      "this document.");
	    } else {
	        //We successfully found the text. Select it:
	        int endPosition = foundPosition+textToFind.length();
	        if (isDirectionDown) {
	            component.setCaretPosition(foundPosition - returns);
	            component.moveCaretPosition(endPosition - returns);
	        } else {
	            component.setCaretPosition(endPosition - returns);
	            component.moveCaretPosition(foundPosition - returns);
	        }
	    }
	}

	/** Finds the position of specified text inside a text component. Will return -1
	 *  if the text is not found. */
	private static int findTextInTextComponent(JTextComponent component,
	                                    String textToFind,int startPos,boolean isWrappedDocument,
	                                    boolean isCaseSensitive,boolean isDirectionDown) {
	    int foundIndex;
	    String text = component.getText();
	    if (isDirectionDown) {
	        if (isCaseSensitive)
	            foundIndex = search(text,startPos,textToFind);
	        else
	            foundIndex = searchIgnoreCase(text,startPos,textToFind);
	    } else {
	        int newStartPos = startPos - 1;
	        if (newStartPos <= 0)
	            newStartPos = startPos;
	        if (isCaseSensitive) {
	            foundIndex = searchBackwards(text,newStartPos,
	                         textToFind);
	        } else {
	            foundIndex = searchBackwardsIgnoreCase(text,newStartPos,textToFind);
	        }
	    }
	    if (foundIndex == -1 && isWrappedDocument && isDirectionDown) {
	        if (isCaseSensitive)
	            foundIndex = search(text,0,textToFind);
	        else
	            foundIndex = searchIgnoreCase(text,0,textToFind);
	    } else if (foundIndex == -1 && isWrappedDocument) {
	        //We know that the searching is upward because we already checked
	        //if it was downward and disproved that.
	        if (isCaseSensitive)
	            foundIndex = searchBackwards(text,textToFind);
	        else
	            foundIndex = searchBackwardsIgnoreCase(text,
	                         textToFind);
	    }
	    return foundIndex;
	}

	/**
	 * Searches for a substring inside a mainString, where the searching begins
	 * at beginIndex. Returns the offset where the substring starts.
	 * @param mainString
	 * @param beginIndex
	 * @param subString
	 * @return
	 */
	public static int search(String mainString,int beginIndex,
	                         String subString) {
	    //Chop off the part of the string before the beginIndex, since we don't
	    //need that part.
	    //Equivalent to Python's mainString = mainString[beginIndex:]
	    mainString = slice(mainString,beginIndex);
	    int maxLen = mainString.length();
	    int subLen = subString.length();
	    char firstChar = subString.charAt(0);
	    int carriageReturns = 0;
	    for (int x=0; x+subLen <= maxLen; x++) {
	        //To avoid wasting time, we make sure the first character is
	        //a match, and then check the whole string.
	    	if (mainString.charAt(x) == '\r') {
	    		carriageReturns++;
	     	} else if (mainString.charAt(x) == firstChar) {
	            String testString = slice(mainString,x,x+subLen);
	            if (testString.equals(subString))
	                return beginIndex + x - carriageReturns;
	        }
	    }
	    return -1;
	}

	/**
	 * Searches for a substring inside a mainString, where the searching begins
	 * at beginIndex. Returns the offset where the substring starts.<br />
	 * Does not pay attention to case (capitalization).
	 * @param mainString
	 * @param beginIndex
	 * @param subString
	 * @return
	 */
	public static int searchIgnoreCase(String mainString,int beginIndex,
	                                   String subString) {
	    //Remove the part of the string before the beginIndex, since we don't
	    //need that part.
	    //Equivalent to Python's mainString = mainString[beginIndex:]
	    mainString = slice(mainString,beginIndex);
	    int maxLen = mainString.length();
	    int subLen = subString.length();
	    String firstChar = ""+subString.charAt(0);
	    int carriageReturns = 0;
	    for (int x=0; x+subLen <= maxLen; x++) {
	        //To avoid wasting time, we make sure the first character is
	        //a match, and then check the whole string.
	        String checkedChar = ""+mainString.charAt(x);
	        if (checkedChar.equals("\r")) {
	        	carriageReturns++;
	        } else if (checkedChar.equalsIgnoreCase(firstChar)) {
	            String testString = slice(mainString,x,x+subLen);
	            if (testString.equalsIgnoreCase(subString))
	                return beginIndex + x - carriageReturns;
	        }
	    }
	    return -1;
	}

	public static int searchBackwards(String mainString,String subString) {
	    int maxLen = mainString.length();
	    return searchBackwards(mainString,maxLen - 1,subString);
	}

	public static int searchBackwardsIgnoreCase(String mainString,
	                                            String subString) {
	    int maxLen = mainString.length();
	    return searchBackwardsIgnoreCase(mainString,maxLen - 1,subString);
	}

	/**
	 * Searches for a substring inside a mainString, where the searching begins
	 * at beginIndex. Returns the offset where the substring starts.<br />
	 * The searching is done backwards by decrementing the beginning index.
	 * @param mainString
	 * @param beginIndex
	 * @param subString
	 * @return
	 */
	public static int searchBackwards(String mainString,int startPos,
	                                  String subString) {
	    int subLen = subString.length();
	    char firstChar = subString.charAt(0);
	    for (int x = startPos; x >= 0; x--) {
	        //To avoid wasting time, we make sure the first character is
	        //a match, and then check the whole string.
	        if (mainString.charAt(x) == firstChar) {
	            String testString = StrTools.slice(mainString,x,x+subLen);
	            if (testString.equals(subString))
	                return x;
	        }
	    }
	    return -1;
	}
	    
	/**
	 * Searches for a substring inside a mainString, where the searching begins
	 * at beginIndex. Returns the offset where the substring starts.<br />
	 * The searching is done backwards by decrementing the beginning index.
	 * <br />Does not care about case or capitalization.
	 * @param mainString
	 * @param beginIndex
	 * @param subString
	 * @return
	 */
	public static int searchBackwardsIgnoreCase(String mainString,int startPos,
	                                            String subString) {
	    int subLen = subString.length();
	    String firstChar = ""+subString.charAt(0);
	    for (int x = startPos; x >= 0; x--) {
	        //To avoid wasting time, we make sure the first character is
	        //a match, and then check the whole string.
	        String checkedChar = ""+mainString.charAt(x);
	        if (checkedChar.equalsIgnoreCase(firstChar)) {
	            String testString = StrTools.slice(mainString,x,x+subLen);
	            if (testString.equalsIgnoreCase(subString))
	                return x;
	        }
	    }
	    return -1;
	}

	/**
	 * slice() is a more intelligent version of String.substring(beginIndex,
	 * endIndex)
	 * If endIndex is larger than the length of mainString, maxLen is used
	 * instead of endIndex.
	 * If beginIndex is greater than or equal to endIndex, "" will be returned.
	 * (This method is intended to emulate Python's string slicing.)<br />
	 * Give a negative number for either index, and the index will count
	 * backwards from the end of the string. So index = -6 really means
	 * index = maxlength - 6
	 * @param mainString
	 * @param beginIndex
	 * @param endIndex
	 * @return
	 */
	public static String slice(String mainString,int beginIndex,int endIndex)
	{
		int maxLen = mainString.length();
		if (beginIndex < 0)
			beginIndex = maxLen + beginIndex;
		if (endIndex < 0)
			endIndex = maxLen + endIndex;
		if (endIndex > maxLen)
			endIndex = maxLen;
		if (beginIndex >= endIndex)
			return "";
		return mainString.substring(beginIndex,endIndex);
	}

	/**
	 * Returns mainString.substring(beginIndex), or returns "" if the
	 * beginIndex is greater than the length of the mainString.
	 * (This method is intended to emulate Python's string slicing.)<br />
	 * Give a negative number for beginIndex, and the slice will count
	 * BACKWARDS for the starting index.
	 * That is, slice(string,-8) is string[-8:]
	 * @param mainString
	 * @param beginIndex
	 * @return
	 */
	public static String slice(String mainString,int beginIndex)
	{
		int maxLen = mainString.length();
		if (beginIndex < 0)
			return slice(mainString,maxLen+beginIndex,maxLen);
		else if (beginIndex < maxLen)
			return mainString.substring(beginIndex);
		else
			return "";
	}

	public static boolean containsIgnoreCase(String text,String toBeFound) {
		int result = searchIgnoreCase(text,0,toBeFound);
		if (result == -1)
			return false;
		return true;
	}

	/**
	 * Shows a string message in a message box dialog.
	 * @param message
	 */
	public static void msgBox(String message)
	{
		System.out.println(message);
		JOptionPane.showMessageDialog(null,message,"Info",
				                      JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static int ascii2Num_CS(String str)
	{
		int result = 0;
		int radix = 1;
		for (int i = 0; i < str.length(); i++)
		{
			if (i > 7)
				break;
			if (i > 0)
				radix *= 10;
			
			result += (str.charAt(str.length() - i - 1) - '0') * radix;
		}
		return result;
	}
	
	public static String CString(byte[] buf, String encoding)
	{
		int l = 0;
		for (int i = 0; i < buf.length; i++)
		{
			if (buf[i] == 0) {
				l = i;
				break;
			}
		}
		byte[] cbuf = Arrays.copyOf(buf, l);
		try {
			return new String(cbuf, encoding);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "encoding_err";
	}
	public static String CString(byte[] buf)
	{
		return CString(buf, "UTF-8");
	}
}
