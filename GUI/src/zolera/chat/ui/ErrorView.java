/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zolera.chat.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import zolera.chat.client.ClientModel;

/**
 *
 * @author ortiz
 */
public class ErrorView extends javax.swing.JFrame {
    
    private ClientModel client;
    private Exception   exception;
    
    /**
     * Creates new form ErrorView
     */
    public ErrorView(ClientModel model, Exception ex) {
        client     = model;
        exception  = ex;
        
        initComponents();
    }
    
    public String getErrorInfo() {
        if (exception == null)
            return "";
        
        StringWriter output = new StringWriter();
        exception.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scpConsoleScroll = new javax.swing.JScrollPane();
        txpErrorConsole = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Error Console");
        setMinimumSize(new java.awt.Dimension(1200, 600));
        setPreferredSize(getMinimumSize());
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        txpErrorConsole.setEditable(false);
        txpErrorConsole.setText(getErrorInfo());
        scpConsoleScroll.setViewportView(txpErrorConsole);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scpConsoleScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scpConsoleScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        GUIView.switchView(this, new LoginView(client, exception));
    }//GEN-LAST:event_formWindowClosed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scpConsoleScroll;
    private javax.swing.JTextPane txpErrorConsole;
    // End of variables declaration//GEN-END:variables
}