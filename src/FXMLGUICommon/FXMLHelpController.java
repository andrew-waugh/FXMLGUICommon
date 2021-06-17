/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FXMLGUICommon;

import VERSCommon.AppError;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebView;

/**
 * FXML Controller class
 *
 * @author Andrew
 */
public class FXMLHelpController implements Initializable {
    private String hashTag;    
    
    @FXML
    protected ScrollPane webViewScrollPane;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {              
    }
    
    /**
     * Loads a HTML file into the scroll pane of the window
     * 
     * @param htmlFile to load
     */
    public void loadContent(String htmlFile) throws AppError {
        WebView webView = new WebView();
        URL url;
        String htmlPath;
        
        // convert to URL
        if ((url = getClass().getResource(htmlFile)) == null) {
            throw new AppError("HTML file '"+htmlFile+"' was not found. (FXMLHelpController.loadContent())");
        }
        htmlPath = url.toExternalForm();
        
        if (hashTag != null && hashTag.length() > 0) {
            if (hashTag.charAt(0) != '#') htmlPath += "#";            
            htmlPath += hashTag;
        }        
        
        webView.setFontSmoothingType(FontSmoothingType.GRAY);
        webView.getEngine().load(htmlPath);
        webViewScrollPane.setContent(webView); 
    }
    
    public void setHashTag(String str) {        
        this.hashTag = str;
    }
    public String getHashTag() {
        return this.hashTag;
    }
    
}
