/*
 * Copyright (C) 2011 Christoph Lehner
 *
 * Adapted from SPIRESFetcher.java and SPIRESBibtexFilterReader.java of JabRef 2.7
 * (Written by Fedor Bezrukov)
 *
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package INSPIREfetcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.imports.BibtexParser;

import net.sf.jabref.imports.EntryFetcher;
import net.sf.jabref.imports.ImportInspector;
import net.sf.jabref.imports.ParserResult;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

class INSPIREBibtexFilterReader extends FilterReader {

    protected BufferedReader in;

    private String line;
    private int pos;
    private boolean pre;

    INSPIREBibtexFilterReader(Reader _in) { 
    	super(_in);
    	in = new BufferedReader(_in);
    	pos=-1;
    	pre=false;
    }

    private String readpreLine() throws IOException {
    	String l;
    	do {
    		l=in.readLine();
    		if (l==null)
    			return null;
    		if (l.contains("<pre>")) {
    			pre = true;
    			l=in.readLine();
    		}
    		if (l.contains("</pre>"))
    			pre = false;
    	} while (!pre);
    	return l;
    }
    
    private String fixBibkey(String in) {
    	if (in== null)
    		return null;
    	//System.out.println(in);
    	if ( in.matches("@Article\\{.*,") ) {
    		//System.out.println(in.replace(' ','_'));
    		return in.replace(' ', '_');
    	} else
    		return in;
    }

    public int read() throws IOException {
    	if ( pos<0 ) {
    		line=fixBibkey(readpreLine());
    		pos=0;
	    	if ( line == null )
	    		return -1;
    	}
    	if ( pos>=line.length() ) {
    		pos=-1;
    		return '\n';
    	}
    	return line.charAt(pos++);
    }

}


public class INSPIREfetcher implements EntryFetcher {

    public static String inspireHost="inspirehep.net";

    public INSPIREfetcher() {
    }
    
    public String constructUrl(String key) {
	String identifier = "";

	try {
	    identifier = URLEncoder.encode(key, "UTF-8");
	} catch (UnsupportedEncodingException e) {
	    return "";
	}
	StringBuffer sb = new StringBuffer("http://").append(inspireHost)
	    .append("/");
	sb.append("search").append("?");
	sb.append("ln=en&action_search=Search&sf=&so=d&rm=&rg=100&sc=0&of=hx&");
	sb.append("p=");
	sb.append(identifier);
	return sb.toString();
    }

    private BibtexDatabase importInspireEntries(String key, OutputPrinter frame) {
	String url = constructUrl(key);
	try {
	    HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
	    conn.setRequestProperty("User-Agent", "Jabref");
	    InputStream inputStream = conn.getInputStream();
	    
	    INSPIREBibtexFilterReader reader = new INSPIREBibtexFilterReader(
									   new InputStreamReader(inputStream));
	    
	    ParserResult pr = BibtexParser.parse(reader);
	    
	    return pr.getDatabase();
	} catch (IOException e) {
	    frame.showMessage( Globals.lang(
					    "An Exception ocurred while accessing '%0'", url)
			       + "\n\n" + e.toString(), Globals.lang(getKeyName()),
			       JOptionPane.ERROR_MESSAGE);
	} catch (RuntimeException e) {
	    frame.showMessage( Globals.lang(
					    "An Error occurred while fetching from SPIRES source (%0):",
					    new String[] { url })
			       + "\n\n" + e.getMessage(), Globals.lang(getKeyName()),
			       JOptionPane.ERROR_MESSAGE);
	}
	return null;
    }
    
    public String getHelpPage() {
	return null;
    }

    public URL getIcon() {
	return GUIGlobals.getIconUrl("www");
    }
    
    public String getKeyName() {
	return "Fetch INSPIRE";
    }

    public JPanel getOptionsPanel() {
	// we have no additional options
	return null;
    }

    public String getTitle() {
	return Globals.menuTitle(getKeyName());
    }
    
    public void cancelled() {
    }
    
    public void done(int entriesImported) {
    }
    
    public void stopFetching() {
    }
    
    public boolean processQuery(String query, ImportInspector dialog,
				OutputPrinter frame) {
	try {
	    frame.setStatus("Fetching entries from Inspire");
	    /* query the archive and load the results into the BibtexEntry */
	    BibtexDatabase bd = importInspireEntries(query,frame);
	    
	    /* addInspireURLtoDatabase(bd); */
	    
	    frame.setStatus("Adding fetched entries");
	    /* add the entry to the inspection dialog */
	    if (bd.getEntryCount() > 0)
		for (BibtexEntry entry : bd.getEntries())
		    dialog.addEntry(entry);
	    
	    /* update the dialogs progress bar */
	    // dialog.setProgress(i + 1, keys.length);
	    /* inform the inspection dialog, that we're done */
	} catch (Exception e) {
	    frame.showMessage(Globals.lang("Error while fetching from Inspire: ")
			      + e.getMessage());
	    e.printStackTrace();
	}
	return true;
    }
}
