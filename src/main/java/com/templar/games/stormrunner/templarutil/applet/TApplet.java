package com.templar.games.stormrunner.templarutil.applet;

import com.templar.games.stormrunner.GameApplet;
import moe.evelyn.games.stormrunner.Main;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class TApplet extends Applet implements AppletContext {
    private HashMap<String,InputStream> streams = new HashMap<String,InputStream>();

    public Image getImage(URL uRL, String string) {
        InputStream inputStream = this.getClass().getResourceAsStream("/" + string);
        if (inputStream == null) {
            return super.getImage(uRL, string);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        try {
            int n;
            byte[] arrby = new byte[4096];
            while ((n = bufferedInputStream.read(arrby)) > 0) {
                byteArrayOutputStream.write(arrby, 0, n);
            }
            Image image = this.getToolkit().createImage(byteArrayOutputStream.toByteArray());
            return image;
        }
        catch (IOException iOException) {
            System.err.println("getImage(): Failed attempt to acquire Image Resource:");
            iOException.printStackTrace();
            return null;
        }
    }

    @Override
    public AppletContext getAppletContext()
    {
        return this;
    }

    @Override
    public Applet getApplet(String name)
    {
        return this;
    }

    // I'm not sure this is actually used
    @Override
    public Enumeration<Applet> getApplets()
    {
        ArrayList<Applet> applets = new ArrayList<Applet>();
        applets.add(this);
        return Collections.enumeration(applets);
    }

    @Override
    public void showDocument(URL url)
    {
        System.out.print ("Showing document ");
        System.out.println(url);
    }

    @Override
    public void showDocument(URL url, String target)
    {
        if(url.getFile().contains("datalog")) {
            String rawDatalogName = url.getFile().substring(1).replace(".asp", "");
            System.out.print("Asked to play datalog ");
            System.out.println(rawDatalogName);

            // You might think this is a bad way to do this, and you'd probably be right!
            // TODO: not this
            for(char c : "123abc".toCharArray()) {
                GameApplet.audio.stop("datalog" + c);
            }

            GameApplet.audio.play(rawDatalogName);
        } else {
            System.out.print ("Showing document ");
            System.out.print(url);
            System.out.print(" target ");
            System.out.println(target);
        }
    }

    @Override
    public void setStream(String key, InputStream stream) throws IOException
    {
        this.streams.put(key, stream);
    }

    @Override
    public InputStream getStream(String key)
    {
        return this.streams.get(key);
    }

    @Override
    public Iterator<String> getStreamKeys()
    {
        return streams.keySet().iterator();
    }

    @Override
    public String getParameter(String name)
    {
        // These are the values defined in the applet tag
        if (name.equals("HelpURL")) return "help.html";
        if (name.equals("HelpTarget")) return "helpwindow";
        return null;
    }

    @Override
    public URL getDocumentBase()
    {
        try {
            return new URL("http://stormrunner.mindstorms.net/");
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
