/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FXMLGUICommon;

import VERSCommon.AppError;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * This class encapsulates the VEO generation
 *
 * @author Andrew
 */
public abstract class JobBase {
    
    // these are set from the subclass
    public String title;       // title to go in report
    public String version;     // version to go in report
    
    // these are set automatically during execution
    public FXMLTask task;      // task to carry out
    public FXMLProgressController.DoTask callback; // callback to report progress
    public int totalObjects;   // total objects to process
    
    // these are set by calling application
    public Path logFile;       // file to save the log
    public boolean verbose;    // true if verbose output
    public boolean debug;      // true if debugging output

    /**
     * Constructor
     */
    public JobBase() {
        title = null;
        verbose = false;
        debug = false;
        logFile = null;
        version = null;
        task = null;
        callback = null;
    }

    /**
     * Deconstructor
     */
    protected void free() {
        title = null;
        logFile = null;
        version = null;
        task.free();
        task = null;
        callback = null;
    }

    /**
     * Check to see if sufficient information has been entered to perform the
     * requested task
     *
     * @return true if so
     */
    abstract public boolean validate();
    
    /**
     * Save the Job to a file.
     * 
     * @param file the file to save the job to.
     * @throws AppError
     */
    abstract public void saveJob(Path file) throws AppError;
    
    /**
     * Read a JSON file
     *
     * @param file file containing the JSON Object
     * @return a JSONObject containing the contents of the file
     * @throws AppError
     */
    protected JSONObject loadJSONObject(Path file) throws AppError {
        JSONParser parser = new JSONParser();
        JSONObject j1;
        FileReader fr;
        BufferedReader br;
        String s;

        // set up the default job
        // setDefault();
        
        // overwrite it with the saved job
        try {
            fr = new FileReader(file.toFile());
            br = new BufferedReader(fr);
            j1 = (JSONObject) parser.parse(br);
        } catch (ParseException pe) {
            throw new AppError("Failed parsing Job file: " + pe.toString());
        } catch (IOException ioe) {
            throw new AppError("Failed reading Job file: " + ioe.toString());
        }
        
        try {
            br.close();
            fr.close();
        } catch (IOException ioe) {
            /* ignore */
        }
        
        // get common properties
        if ((s = (String) j1.get("logFile")) != null) {
            logFile = Paths.get(s);
        }
        verbose = ((Boolean) j1.get("verboseReporting"));
        debug = ((Boolean) j1.get("debugReporting"));
        
        return j1;
    }

    /**
     * Create a JSON file capturing a JSON Object.
     *
     * @param file the job file to be created
     * @param jo a JSON object containing the job
     * @throws AppError
     */
    protected void saveJSONObject(Path file, JSONObject jo) throws AppError {
        FileWriter fw;
        BufferedWriter bw;

        try {
            fw = new FileWriter(file.toFile());
            bw = new BufferedWriter(fw);
        } catch (IOException ioe) {
            throw new AppError("Failed reading Job file: " + ioe.toString());
        }
        
        // add the standard configuration
        if (logFile != null) {
            jo.put("logFile", logFile.toString());
        }
        jo.put("verboseReporting", verbose);
        jo.put("debugReporting", debug);

        try {
            bw.write(prettyPrintJSON(jo.toString()));
        } catch (IOException ioe) {
            throw new AppError("Failed trying to write Job file: " + ioe.toString());
        }
        try {
            bw.close();
            fw.close();
        } catch (IOException ioe) {
            /* ignore */ }
    }

    private String prettyPrintJSON(String in) {
        StringBuffer sb;
        int i, j, indent;
        char ch;

        sb = new StringBuffer();
        indent = 0;
        for (i = 0; i < in.length(); i++) {
            ch = in.charAt(i);
            switch (ch) {
                case '{':
                    indent++;
                    sb.append("{");
                    break;
                case '}':
                    indent--;
                    sb.append("}");
                    break;
                case '[':
                    indent++;
                    sb.append("[\n");
                    for (j = 0; j < indent; j++) {
                        sb.append(" ");
                    }
                    break;
                case ']':
                    indent--;
                    sb.append("]");
                    break;
                case ',':
                    sb.append(",\n");
                    for (j = 0; j < indent; j++) {
                        sb.append(" ");
                    }
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }
}
