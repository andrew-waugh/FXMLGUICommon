/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FXMLGUICommon;

import VERSCommon.AppError;
import VERSCommon.AppFatal;
import java.util.logging.Handler;

/**
 * This interface is implemented by code that actually does a task.
 * 
 * @author Andrew
 */
public abstract class FXMLTask {
    
    /**
     * Tell the implementation of FXMLTask of the Log Handler and a reporting
     * callback to be used during execution of the task. The log handler is
     * where status information updates are to be logged during execution of the
     * task. At the completion of each unit of work the 'reporting' callback is
     * to be called to update the status and to check if execution has been
     * canceled.
     * 
     * @param lh a log handler
     * @param reporting a reporting callback
     * @throws AppFatal if anything went wrong in registering
     */
    public abstract void register(Handler lh, FXMLProgressController.DoTask reporting) throws AppFatal;
    
    /**
     * Count the number of units of work to be done in this task. Used to report
     * on how far the processing has been completed. Note that at the completion
     * of each unit of work, the callback registered in register() should be
     * called.
     * 
     * @return number of units of work to do
     */
    public abstract int countUnitsOfWork();
    
    /**
     * Actually do the work.
     * @throws AppFatal if anything went wrong that means termination
     * @throws AppError if anything went wrong, but processing can continue
     */
    public abstract void doTask() throws AppFatal, AppError; 
    
    /**
     * Free any resources allocated in this task.
     */
    public abstract void free();
}
