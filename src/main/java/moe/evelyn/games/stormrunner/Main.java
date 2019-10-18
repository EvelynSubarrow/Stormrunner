package moe.evelyn.games.stormrunner;

import com.templar.games.stormrunner.GameApplet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class Main
{
    public static JFrame frame;
    public static GameApplet applet;

    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        Main.frame = frame;

        frame.setUndecorated(false);
        frame.setMaximumSize(new Dimension(500, 400));
        frame.setMinimumSize(new Dimension(500, 400));
        frame.setLocation(0, 0);

        GameApplet applet = new GameApplet();
        Main.applet = applet;

        frame.getContentPane().add(applet);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.pack();
        applet.init();
        applet.start();
        frame.setVisible(true);

        frame.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                Main.applet.redimension(Main.frame.getSize());
            }
        });
    }
}
