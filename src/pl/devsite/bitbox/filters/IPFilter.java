/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.devsite.bitbox.filters;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableFilter;

/**
 *
 * @author dmn
 */
public class IPFilter implements SendableFilter {

    private boolean interactive;
    private boolean directoryListing;
    private final ArrayList<String> allowedIP;
    private final ArrayList<String> bannedIP;

    public IPFilter(boolean interactive, boolean directoryListing, final ArrayList<String> allowedIP, final ArrayList<String> bannedIP) {
        this.interactive = interactive;
        this.directoryListing = directoryListing;
        this.allowedIP = allowedIP;
        this.bannedIP = bannedIP;
    }

    @Override
    public boolean isAllowed(Sendable sendable, Object user, Object authenticatedUser) {
        /*
        Sendable parent = sendable;
        StringBuilder sb = new StringBuilder(sendable.toString());
        while (parent.getParent() != null) {
        parent = parent.getParent();
        sb.insert(0, parent.toString() + "/");
        }
        System.out.println("**** " + user + "@" + sb.toString());
         */
        if ("127.0.0.1".equals(user)) {
            return true;
        }
        if (!directoryListing && sendable.hasChildren()) {
            return false;
        }
        if (bannedIP.size() > 0) {
            for (String ip : bannedIP) {
                if (ip.equals(user)) {
                    return false;
                }
            }
        }
        for (String ip : allowedIP) {
            if (ip.equals(user)) {
                return true;
            }
        }
        if (interactive) {
            final JOptionPane optionPane = new JOptionPane(
                    "Do you accept connections from " + user + "?",
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION);

            final JDialog dialog = new JDialog((Frame) null,
                    "Incoming connection warning",
                    true);
            dialog.setContentPane(optionPane);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent we) {
                }
            });
            optionPane.addPropertyChangeListener(
                    new PropertyChangeListener() {

                        public void propertyChange(PropertyChangeEvent e) {
                            String prop = e.getPropertyName();

                            if (dialog.isVisible() && (e.getSource() == optionPane) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                                dialog.setVisible(false);
                            }
                        }
                    });
            dialog.pack();
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            int value = ((Integer) optionPane.getValue()).intValue();
            if (value == JOptionPane.YES_OPTION) {
                allowedIP.add(user.toString());
                return true;
            } else {
                bannedIP.add(user.toString());
                return false;
            }
        }
        if (allowedIP.size() == 0) {
            return true;
        }

        return false;
    }
}
