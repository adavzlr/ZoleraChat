/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zolera.chat.ui;

import java.awt.Color;
import javax.swing.text.*;
import zolera.chat.client.ClientModel;
import zolera.chat.client.ProcessMessagesDelegate;
import zolera.chat.client.TerminateClientException;
import zolera.chat.infrastructure.ChatMessage;
import zolera.chat.infrastructure.ServerConfiguration;

/**
 *
 * @author ortiz
 */
public class ChatView extends javax.swing.JFrame {
    
    private ServerConfiguration config;
    private ClientModel client;
    private String lastMsgUser;

    /**
     * Creates new form ChatFrame
     * @param model
     * @param user
     * @param room
     */
    public ChatView(ClientModel model, String room, String user) {
        if (model == null)
            throw new IllegalArgumentException("Expecting a model");
        
        config      = ServerConfiguration.getGlobal();
        client      = model;
        lastMsgUser = null;
        
        ProcessMessagesDelegate procMsg = new ProcessMessagesDelegate() {
            @Override
            public void process(ChatMessage[] batch) {
                printMessageBatch(batch, false);
            }
        };
        ProcessMessagesDelegate procLogMsg = new ProcessMessagesDelegate() {
            @Override
            public void process(ChatMessage[] batch) {
                printMessageBatch(batch, true);
            }
        };
        
        initComponents();
        
        try {
            client.join(room, user, procMsg, procLogMsg);
        }
        catch (TerminateClientException tce) {
            GUIView.terminateClient(null, client, tce, false);
        }
        
        lblStatus.setText(getStatusBarInfo());
        txfMessage.requestFocus();
    }
    
    
    
    private static SimpleAttributeSet getSenderStyle() {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setBold(style, true);
        StyleConstants.setForeground(style, Color.BLUE);
        return style;
    }
    
    private static SimpleAttributeSet getTextStyle() {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, Color.BLACK);
        return style;
    }
    
    private static void addSysMsgStyle(SimpleAttributeSet sender, SimpleAttributeSet text) {
        StyleConstants.setItalic(text, true);
        StyleConstants.setForeground(text, Color.GREEN);
    }
    
    private static void addHistoricStyle(SimpleAttributeSet sender, SimpleAttributeSet text) {
        StyleConstants.setForeground(sender, Color.LIGHT_GRAY);
        StyleConstants.setForeground(text, Color.LIGHT_GRAY);
    }
    
    
    
    private String getStatusBarInfo() {
        return client.getUsername() + " @ " + client.getRoomname()
               + " (" + client.getMessageCount() + " messages) :"
               + client.getServerId();
    }
    
    
    
    private void printMessage(String sender, SimpleAttributeSet senderStyle, String text, SimpleAttributeSet textStyle) {
        try {
            StyledDocument doc = txpLog.getStyledDocument();
            
            if (sender != null)
                doc.insertString(doc.getLength(), sender + "\n", senderStyle);
            
            doc.insertString(doc.getLength(), text + "\n", textStyle);
            lblStatus.setText(getStatusBarInfo());
        }
        catch (BadLocationException ble) {
            throw new IllegalStateException("Invalid location for chat pane", ble);
        }
    }
    
    private void printMessageBatch(ChatMessage[] batch, boolean historic) {
        for (int m = 0; m < batch.length; m++) {
            ChatMessage msg    = batch[m];
            String      sender = msg.getSenderName();
            String      text   = "\t" + msg.getMessageText();
            boolean     sysmsg = sender.equals(config.getSystemMessagesUsername());
            
            SimpleAttributeSet senderStyle = getSenderStyle();
            SimpleAttributeSet textStyle   = getTextStyle();
            
            // prepare styles
            if (sysmsg)
                addSysMsgStyle(senderStyle, textStyle);
            if (historic)
                addHistoricStyle(senderStyle, textStyle);
            
            // set printing information and keep track of sender headers
            if (sysmsg) {
                sender      = null; // don't print sender
                lastMsgUser = null; // always print sender header after a sys msg
                text        = ">>>" + text;
            }
            else if (sender.equals(lastMsgUser)) {
                sender = null; // don't print sender
            }
            else {
                lastMsgUser = sender;
            }
            
            printMessage(sender, senderStyle, text, textStyle);
        }
    }
    
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scpLogScroll = new javax.swing.JScrollPane();
        txpLog = new javax.swing.JTextPane();
        pnlSendBar = new javax.swing.JPanel();
        txfMessage = new javax.swing.JTextField();
        btnSend = new javax.swing.JButton();
        pnlStatusBar = new javax.swing.JPanel();
        lblStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("ZoleraChat");
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(400, 500));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        txpLog.setEditable(false);
        scpLogScroll.setViewportView(txpLog);

        txfMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        btnSend.setText("Send");
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlSendBarLayout = new javax.swing.GroupLayout(pnlSendBar);
        pnlSendBar.setLayout(pnlSendBarLayout);
        pnlSendBarLayout.setHorizontalGroup(
            pnlSendBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlSendBarLayout.createSequentialGroup()
                .addComponent(txfMessage)
                .addGap(0, 0, 0)
                .addComponent(btnSend, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlSendBarLayout.setVerticalGroup(
            pnlSendBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(btnSend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(txfMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pnlStatusBar.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        pnlStatusBar.setPreferredSize(new java.awt.Dimension(0, 20));

        lblStatus.setFocusable(false);

        javax.swing.GroupLayout pnlStatusBarLayout = new javax.swing.GroupLayout(pnlStatusBar);
        pnlStatusBar.setLayout(pnlStatusBarLayout);
        pnlStatusBarLayout.setHorizontalGroup(
            pnlStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pnlStatusBarLayout.setVerticalGroup(
            pnlStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblStatus)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scpLogScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addComponent(pnlSendBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pnlStatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(scpLogScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(pnlSendBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pnlStatusBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        String message = txfMessage.getText();
        
        if (message.equals(""))
            return;
        
        try {
            client.send(message);
        }
        catch(TerminateClientException tce) {
            GUIView.terminateClient(this, client, tce, false);
        }
        
        txfMessage.setText(null);
        txfMessage.requestFocus();
    }//GEN-LAST:event_btnSendActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        client.terminate();
        GUIView.switchView(this, new LoginView(client, null));
    }//GEN-LAST:event_formWindowClosed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSend;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JPanel pnlSendBar;
    private javax.swing.JPanel pnlStatusBar;
    private javax.swing.JScrollPane scpLogScroll;
    private javax.swing.JTextField txfMessage;
    private javax.swing.JTextPane txpLog;
    // End of variables declaration//GEN-END:variables
}
