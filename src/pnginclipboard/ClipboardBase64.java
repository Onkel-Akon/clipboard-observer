/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pnginclipboard;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;

/**
 * TODO create some description
 * <br />
 * User: Lubos Strapko
 * <br />
 * Date: Dec 12, 2009,
 * <br />
 * Time: 4:25:14 PM
 */
public class ClipboardBase64 {
    private static char[] map1 = new char[64];
    private JTextArea text;
    private EThread runner;

    public void setText(JTextArea text) {
        this.text = text;
    }

    public void setPut(boolean put) {
        System.out.println("auto put="+put);
        this.put = put;
    }

    // Returns the contents of the file in a byte array.

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }


    private static final ClipboardOwner OWNER = new ClipboardOwner() {
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
        }
    };

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) map1[i++] = c;
        for (char c = 'a'; c <= 'z'; c++) map1[i++] = c;
        for (char c = '0'; c <= '9'; c++) map1[i++] = c;
        map1[i++] = '+';
        map1[i] = '/';
    }

    /**
     * Encodes a byte array into Base64 format.
     * No blanks or line breaks are inserted.
     *
     * @param in   an array containing the data bytes to be encoded.
     * @param iLen number of bytes to process in <code>in</code>.
     * @return A character array with the Base64 encoded data.
     */
    public static char[] encode(byte[] in, int iLen) {
        int oDataLen = (iLen * 4 + 2) / 3;       // output length without padding
        int oLen = ((iLen + 2) / 3) * 4;         // output length including padding
        char[] out = new char[oLen];
        int ip = 0;
        int op = 0;
        while (ip < iLen) {
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iLen ? in[ip++] & 0xff : 0;
            int i2 = ip < iLen ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = map1[o0];
            out[op++] = map1[o1];
            out[op] = op < oDataLen ? map1[o2] : '=';
            op++;
            out[op] = op < oDataLen ? map1[o3] : '=';
            op++;
        }
        return out;
    }

    private boolean put = true;

    public void setEnabled(boolean enable) {
        System.out.println("enable="+enable);
        if ((enable && (runner == null || !runner.running)) || (!enable && runner != null && runner.running)) {
            if (runner != null) runner.running = false;
            if (enable) runner = new EThread(this);
        }
    }

    public Image getClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
            //System.out.println("Flavors:");
            String s = null;
            for (DataFlavor flavor : flavors) {
                //System.out.println(flavor);
                String type = flavor.getMimeType();
                Class<?> aClass = flavor.getRepresentationClass();
                if (type.startsWith("text/uri-list") && String.class.isAssignableFrom(aClass)) {
                    Transferable t = clipboard.getContents(null);
                    String[] files = ((String) t.getTransferData(flavor)).split("\n");
                    for (String file : files) {
                        file = file.trim();
                        if (file.startsWith("file:")) file = file.substring(5);
                        String lower = file.toLowerCase();
                        if (lower.endsWith(".png")) {
                            byte[] a = getBytesFromFile(new File(file));
                            StringBuilder sb = new StringBuilder(a.length * 2 + 27);
                            sb.append("url(data:image/png;base64,");
                            sb.append(encode(a, a.length));
                            sb.append(')');
                            if (s == null) s = sb.toString();
                            else s += "\n" + sb.toString();
                        }
                        //System.out.println("file="+file);
                    }
                } else if (type.startsWith("image/png") && InputStream.class.isAssignableFrom(aClass)) {
                    Transferable t = clipboard.getContents(null);
                    InputStream is = (InputStream) t.getTransferData(flavor);
                    int available = is.available();
                    StringBuilder sb = new StringBuilder(available * 2 + 27);
                    sb.append("url(data:image/png;base64,");
                    while (available > 0) {
                        byte[] a = new byte[available];
                        available = is.read(a);
                        char[] chars = encode(a, available);
                        sb.append(chars);
                        available = is.available();
                    }
                    sb.append(')');
                    s = sb.toString();
                }
            }
            if (s != null) {
                if (text != null) text.setText(s);
                else System.out.println(s);
                if (put) {
                    clipboard.setContents(new StringSelection(s), OWNER);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        new ClipboardBase64().setEnabled(true);
    }

    private static class EThread extends Thread {
        boolean running;
        ClipboardBase64 c;

        private EThread(ClipboardBase64 c) {
            this.c = c;
            start();
        }

        @Override
        public void run() {
            try {
                running = true;
                while (running) {
                    c.getClipboard();
                    sleep(1000);
                }
            } catch (Exception e) {
                running = false;
            }
        }

    }
}
