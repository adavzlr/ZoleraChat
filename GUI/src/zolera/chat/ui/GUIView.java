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
    
    public static final int LOGIN = 1;
    public static final int CHAT  = 2;
    public static final int ERROR = 3;
    private JFrame currentView;
    
    private Exception pendingException;
    private int       serverId;
    private String    username;
    private String    roomname;
    
    private ClientModel client;
    private ServerConfiguration config;
    private ProcessMessagesDelegate procMsg;
    private ProcessMessagesDelegate procLogMsg;
    
    public GUIView() {
        currentView = null;
        
        pendingException = null;
        serverId         = -1;
        username         = null;
        roomname         = null;
        
        client     = new ClientModel();
        config     = ServerConfiguration.getGlobal();
        procMsg    = null;
        procLogMsg = null;
    }
    
    public ClientModel getClient() {
        return client;
    }
    
    public Exception getPendingException() {
        return pendingException;
    }
    
    public void setPendingException(Exception ex) {
        pendingException = ex;
    }
    
    /*
    public void setServerId(int id)
    throws TerminateClientException {
        if (id < 0 || id > config.getRegistryAddressesListLength())
            throw new TerminateClientException("Invalid server id (" + id + ")");
        
        serverId = id;
    }
    
    public int getServerId() {
        return serverId;
    }
    
    public void setUsername(String name)
    throws TerminateClientException {
        if (name == null || !name.matches(config.getUsernamePattern()))
            throw new TerminateClientException("Invalid username '" + name + "'");
        
        username = name;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setRoomname(String name)
    throws TerminateClientException {
        if (name == null || !name.matches(config.getRoomnamePattern()))
            throw new TerminateClientException("Invalid roomname '" + name + "'");
        
        roomname = name;
    }
    
    public String getRoomname() {
        return roomname;
    }
    
    public void setDelegates(ProcessMessagesDelegate msgDelegate, ProcessMessagesDelegate logDelegate)
    throws TerminateClientException {
        if (procMsg == null || logDelegate == null)
            throw new TerminateClientException("Invalid delegates (msg=" + procMsg + ", log=" + logDelegate + ")");
        
        procMsg    = msgDelegate;
        procLogMsg = logDelegate;
    }
    */
    
    
    
    /*
    public void switchView(int viewId) {
        // dispose previous view
        if (currentView != null)
            currentView.dispose();
        
        // create new view
        switch(viewId) {
        case LOGIN:
            currentView = new LoginView(this);
            break;
        case CHAT:
            currentView = new ChatView(this);
            break;
        case ERROR:
        default:
            if (viewId != ERROR)
                setPendingException(new TerminateClientException("Invalid view id (" + viewId + ")"));
            
            if (DebuggingTools.DEBUG_MODE)
                currentView = new ErrorView(this);
            else
                currentView = new LoginView(this);
            
            break;
        }
        
        // display new view
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentView.setVisible(true);
            }
        });
    }
    */
    
    public void switchView(JFrame view) {
        if (currentView != null)
            currentView.dispose();
        
        // display new view
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                currentView.setVisible(true);
            }
        });
    }
    
    public void terminateClient(TerminateClientException tce) {
        JFrame errorView;
        setPendingException(tce);
        
        if (DebuggingTools.DEBUG_MODE)
            errorView = new ErrorView(this);
        else
            errorView = new LoginView(this);
        
        switchView(errorView);
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
        controller.switchView(new LoginView(controller));
    }
}
