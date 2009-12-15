/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pnginclipboard;

import javax.swing.UIManager;

/**
 *
 * @author lubino
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
          System.out.println("Error setting native LAF: " + e);
        }
        Dialog d = new Dialog();
        d.setVisible(true);
        d.bind(new ClipboardBase64());
    }

}
