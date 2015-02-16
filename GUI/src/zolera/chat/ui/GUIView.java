/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zolera.chat.ui;

import javax.swing.*;
import zolera.chat.client.*;
import zolera.chat.infrastructure.*;

/**
 *
 * @author ortiz
 */
public class GUIView {
    
    // We don't expect instantiation of this class
    private GUIView() {}
        
    public static void switchView(JFrame fromView, JFrame toView) {
        if (fromView != null)
            fromView.dispose();
        
        // display new view
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                toView.setVisible(true);
            }
        });
    }
    
    public static void terminateClient(JFrame fromView, ClientModel client, Exception ex, boolean showConsole) {
        JFrame errorView;
        
        client.terminate();
        
        if (showConsole || DebuggingTools.DEBUG_MODE)
            errorView = new ErrorView(client, ex);
        else
            errorView = new LoginView(client, ex);
        
        switchView(fromView, errorView);
    }
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the System look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LoginView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        GUIView controller = new GUIView();
        ClientModel client = new ClientModel();
        controller.switchView(null, new LoginView(client, null));
    }
}
