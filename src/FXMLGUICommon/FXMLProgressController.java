/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FXMLGUICommon;

import VERSCommon.AppError;
import VERSCommon.AppFatal;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.json.simple.JSONObject;

/**
 * This class reports on the progress and results of creating or verifying a
 * manifest. It consists of three inter-related classes - the
 * FXMLProgressController itself which runs the GUI screen, and two internal
 * classes: ManifestService and DoManifestTask. These implement a thread that
 * manage the actual work. The separate thread allows the GUI to remain
 * responsive while the work is actually being carried out.
 *
 * Note this class doesn't actually do the work - it handles communication
 * between the thread that manages the GUI and the thread that does the work.
 *
 * @author Andrew
 */
public class FXMLProgressController extends FXMLBaseController {

    @FXML
    private AnchorPane rootAP;
    @FXML
    private Button finishB;
    @FXML
    private ProgressBar processedPB;
    @FXML
    private TextArea warningTA;
    @FXML
    private Label currentlyProcessingL;
    @FXML
    private Label countL;
    @FXML
    private Button logB;

    JobBase job;                    // information shared between scenes
    HostServices hostServices;
    TaskService cms;        // The service to handle processing
    boolean finished;           // true if the run has completed, but the window is not closed

    static String defaultText = "Processing - no errors or warnings generated yet\n";


    /**
     * Initializes the controller class. This is called when the GUI screen is
     * created.
     */
    public void initialize() {
        processedPB.setProgress(0);
        finished = false;

        try {
            initTooltips();
        } catch (AppFatal af) {
            System.err.println(af.toString());
        }
    }

    /**
     * Put a tool tip on each control
     */
    private void initTooltips() throws AppFatal {
        JSONObject json = openTooltips();
        createTooltip(warningTA, (String) json.get("report"));
        createTooltip(processedPB, (String) json.get("progress"));
        createTooltip(currentlyProcessingL, (String) json.get("processing"));
        createTooltip(finishB, (String) json.get("cancel"));
        createTooltip(logB, (String) json.get("log"));
    }

    /**
     * This is called by the SetupRunController when it is desired to start a
     * new processing run. It passes in the details of the job to be run and
     * starts the service
     *
     * @param job information about manifest to be created
     * @param baseDirectory the base directory
     */
    public void generate(JobBase job, File baseDirectory) {

        this.job = job;
        this.baseDirectory = baseDirectory;
        finished = false;

        // if log file is to be generated, set the button
        if (job.logFile != null) {
            logB.setText("Logging to: " + job.logFile.toString());
        }

        // start the service that will do the work (which can be reused for multiple tasks)
        cms = new TaskService();
        cms.start(); // start the service
    }

    /**
     * Callback when user presses a close button or the close window button
     */
    @FXML
    private void handleCloseAction(ActionEvent event) throws Exception {
        shutdown();
    }

    /**
     * Callback if the user presses the log button
     *
     * @param event
     */
    @FXML
    private void handleLogAction(ActionEvent event) {
        File f;

        // ask user what file to log to
        f = browseForSaveFile("Select log file", ".txt", job.logFile);
        if (f == null) {
            return;
        }
        job.logFile = f.toPath();
        logB.setText("Logging to: " + job.logFile.toString());

        // if the run has finished, log the results
        if (finished) {
            logResults();
        }
    }

    /**
     * Called when it is necessary to close this window
     */
    public void shutdown() {
        cms.cancel();
        final Stage stage = (Stage) rootAP.getScene().getWindow();
        stage.close();
    }

    public void logResults() {
        ObservableList<CharSequence> ol;
        FileWriter fw;
        BufferedWriter bw;
        TimeZone tz;
        SimpleDateFormat sdf;
        StringBuilder sb = new StringBuilder();
        int i;
        String s;

        // if not logging, just return
        if (job.logFile == null) {
            return;
        }

        // if logging, get all the text from the warning text area and save it
        // to the specified file
        try {
            ol = warningTA.getParagraphs();
            fw = new FileWriter(job.logFile.toFile());
            bw = new BufferedWriter(fw);

            bw.write("********************************************************************************\n");
            bw.write("*                                                                              *\n");
            if (job.title != null) {
                for (i = 0; i < job.title.length(); i++) {
                    sb.append(job.title.charAt(i));
                    sb.append(' ');
                }
                s = sb.toString();
                bw.write("*");
                for (i = 0; i < (80 - s.length()) - 1; i = i + 2) {
                    bw.write(" ");
                }
                bw.write(s);
                for (i = 0; i < (80 - s.length()) - 1; i = i + 2) {
                    bw.write(" ");
                }
                bw.write("*\n");
            }
            bw.write("*                                                                              *\n");
            if (job.version != null) {
                bw.write("*");
                s = "Version "+job.version;
                for (i = 0; i < (80 - s.length()) - 1; i = i + 2) {
                    bw.write(" ");
                }
                bw.write(s);
                for (i = 0; i < (80 - s.length()) - 1; i = i + 2) {
                    bw.write(" ");
                }
                bw.write("*\n");
            }
            bw.write("*                                                                              *\n");
            bw.write("********************************************************************************\n");
            bw.write("\n");
            bw.write("Run: ");
            tz = TimeZone.getTimeZone("GMT+10:00");
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss+10:00");
            sdf.setTimeZone(tz);
            bw.write(sdf.format(new Date()));
            bw.write("\n");
            bw.write(job.toString());
            bw.write("\n");
            bw.write("********************************************************************************\n");
            bw.write("Output:\n");
            bw.write("********************************************************************************\n");
            for (i = 0; i < ol.size(); i++) {
                bw.write(ol.get(i).toString());
                bw.write("\n");
            }
            bw.write("********************************************************************************\n");

            bw.close();
            fw.close();
        } catch (IOException ioe) {
            System.out.println(ioe.toString());
        }
        logB.setText("Logged to: " + job.logFile.toString());
    }


    /**
     * Create a service that performs the task
     */
    private class TaskService extends Service<ArrayList<String>> {

        // create the task (i.e. thread) that actually processes the job
        @Override
        protected Task<ArrayList<String>> createTask() {
            DoTask task;

            task = new DoTask(job, warningTA, processedPB, countL, finishB);

            // this event handler is called when the thread completes, and it
            // puts the response into the report list view and scrolls to the
            // end
            task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    ArrayList<String> results;

                    results = getValue();
                    if (results.size() > 0) {
                        if (warningTA.getText().equals(defaultText)) {
                            warningTA.clear();
                        }
                        for (int j = 0; j < results.size(); j++) {
                            warningTA.appendText(results.get(j));
                            warningTA.appendText("\n");
                        }
                        results.clear();
                    }
                    logResults();
                    finished = true;
                }
            });
            return task;
        }

        // the user requested to cancel the job
        /*
        @Override
        protected void cancelled() {
            System.out.println("We've been cancelled!");
        }
         */
    }

    /**
     * Create a task (i.e. a thread) that actually does the work in order
     * to ensure that the GUI remains responsive
     */
    public class DoTask extends Task<ArrayList<String>> {

        final JobBase job;
        final TextArea ta;      // the list view that will display the logging results
        final ProgressBar pb;   // the progress bar
        final Label count;
        final Button fb;        // the finish button
        FXMLTask task;      // Encapsulation of the file harvest itself
        ArrayList<String> results; // list of results generated

        public DoTask(JobBase job, TextArea ta, ProgressBar pb, Label count, Button fb) {
            this.job = job;
            this.ta = ta;
            this.pb = pb;
            this.count = count;
            this.fb = fb;
            results = new ArrayList<>();
        }

        /**
         * Actually process files. The parameters of the work to be performed
         * were provided when the task was created; the GUI fields to be updated
         * during the processing were also passed when the job was created.
         *
         * Updates to the calling GUI are scheduled via 'runLater()' as these
         * are executed in a different thread. Note that the 'runLater()' code
         * copies all of the current results and then clears the results.
         *
         * @return a list of strings containing the results of the processing
         */
        @Override
        protected ArrayList<String> call() {
            LogHandler lh;

            // create a list in which to put the results of processing 
            updateValue(results);

            try {
                lh = new LogHandler(results);
                job.task.register(lh, this);
            } catch (AppFatal af) {
                results.add("FAILED: " + af.toString());
                return results;
            }

            // set up defaults
            Platform.runLater(() -> {
                ta.insertText(0, "Stage 1: counting objects");
                count.setText("0/unknown");
                pb.setProgress(0.0);
                fb.setText("Cancel processing");
            });

            // go through list of directories, counting number of files to hash
            if (isCancelled()) {
                return results;
            }
            Platform.runLater(() -> {
                ta.clear();
                ta.insertText(0, defaultText);
                count.setText("0/" + job.totalObjects);
                pb.setProgress(0.0);
            });

            // process the directory
            try {
                job.task.doTask();
            } catch (AppFatal | AppError af) {
                System.out.println("Processing manifest error: " + af.toString());
            }

            Platform.runLater(() -> {
                if (results.size() > 0) {
                    if (ta.getText().equals(defaultText)) {
                        ta.clear();
                    }
                    for (int j = 0; j < results.size(); j++) {
                        ta.appendText(results.get(j));
                        ta.appendText("\n");
                    }
                    results.clear();
                }
                countL.setText(job.totalObjects + "/" + job.totalObjects);
                pb.setProgress(1.0);
                ta.appendText("Finished\n");
                currentlyProcessingL.setText("Completed");
                fb.setText("Close");
            });
            return results;
        }

        /**
         * The code actually doing the work calls this method when it has
         * completed a unit of work. This allows the status of the GUI to be
         * updated, and for the work to check if it has been cancelled.
         *
         * @param id a String used to indicate when the processing is up to
         * @param messages a set of Strings giving any error etc to be displayed
         * @param count current count of objects processed
         * @return true if processing should continue
         */
        public boolean updateStatus(String id, String[] messages, int count) {

            // schedule an update on the GUI
            Platform.runLater(() -> {
                currentlyProcessingL.setText(id);
                if (results.size() > 0) {
                    if (ta.getText().equals(defaultText)) {
                        ta.clear();
                    }
                    for (int j = 0; j < results.size(); j++) {
                        ta.appendText(results.get(j));
                        ta.appendText("\n");
                    }
                    results.clear();
                }
                countL.setText(count + "/" + job.totalObjects);
                pb.setProgress(((double) count ) / job.totalObjects);
            });
            return isCancelled();
        }
    }

    /**
     * Recursively count the number of normal files that would be hashed
     * contained within a given file. This is used to display the progress when
     * the hashes are calculated...
     *
     * @param f file (which could be a directory)
     * @return the number of normal files
     */
    private int count(Path f) {
        DirectoryStream<Path> ds;
        int c;

        c = 0;

        // check that file or directory exists
        if (!Files.exists(f)) {
            return 0;
        }

        // if file is a directory, go through directory and test all the files
        if (Files.isDirectory(f)) {
            try {
                ds = Files.newDirectoryStream(f);
                for (Path p : ds) {
                    c += count(p);
                }
                ds.close();
            } catch (IOException e) {
                // ignore - it's only a status
            }
        } else if (Files.isRegularFile(f)) {
            Platform.runLater(() -> {
                currentlyProcessingL.setText(f.getFileName().toString());
            });
            // ignore files that start with "~$" (i.e. Windows special) as these won't be in the manifest
            if (!f.getFileName().toString().startsWith("~$")) {
                c++;
            }
        }
        return c;
    }

    /**
     * Log Handler used to ensure any call to Log() when processing things
     * writes the log entry to the ArrayList for eventual inclusion in the
     * status update
     */
    private class LogHandler extends Handler {

        final SimpleFormatter sf;
        ArrayList<String> responses; // list of results generated

        public LogHandler(ArrayList<String> responses) {
            this.responses = responses;
            sf = new SimpleFormatter();
        }

        @Override
        public void publish(LogRecord record) {
            String s;

            s = sf.format(record);
            responses.add(s);
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() {

        }
    }
}
