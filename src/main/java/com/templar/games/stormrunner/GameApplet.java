package com.templar.games.stormrunner;

import com.ms.security.PermissionID;
import com.ms.security.PolicyEngine;
import com.templar.games.stormrunner.GameState;
import com.templar.games.stormrunner.Grid;
import com.templar.games.stormrunner.LoadSaveFrame;
import com.templar.games.stormrunner.OpsPanel;
import com.templar.games.stormrunner.OptionsPanel;
import com.templar.games.stormrunner.PhysicalObject;
import com.templar.games.stormrunner.ProgressComponent;
import com.templar.games.stormrunner.Renderer;
import com.templar.games.stormrunner.Robot;
import com.templar.games.stormrunner.Scene;
import com.templar.games.stormrunner.SceneBuilder;
import com.templar.games.stormrunner.StatusPanel;
import com.templar.games.stormrunner.World;
import com.templar.games.stormrunner.build.BuildPanel;
import com.templar.games.stormrunner.build.CargoBay;
import com.templar.games.stormrunner.objects.Trigger;
import com.templar.games.stormrunner.program.editor.Editor;
import com.templar.games.stormrunner.templarutil.Debug;
import com.templar.games.stormrunner.templarutil.applet.TApplet;
import com.templar.games.stormrunner.templarutil.audio.AppletAudioDevice;
import com.templar.games.stormrunner.templarutil.audio.AudioDevice;
import com.templar.games.stormrunner.templarutil.audio.AudioManager;
import com.templar.games.stormrunner.templarutil.audio.NullAudioDevice;
import com.templar.games.stormrunner.templarutil.audio.SunAudioDevice;
import com.templar.games.stormrunner.templarutil.gui.ImageComponent;
import com.templar.games.stormrunner.templarutil.gui.ImageFilenameProvider;
import com.templar.games.stormrunner.templarutil.gui.ImagePaintListener;
import com.templar.games.stormrunner.templarutil.gui.MessageDialog;
import com.templar.games.stormrunner.templarutil.gui.SimpleContainer;
import com.templar.games.stormrunner.templarutil.util.ImageRetriever;
import com.templar.games.stormrunner.templarutil.util.UtilityThread;
import com.templar.games.stormrunner.util.FocusCatcher;
import com.templar.games.stormrunner.util.MonitoredInputStream;
import com.templar.games.stormrunner.util.ProgressListener;
import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import moe.evelyn.games.stormrunner.Main;
import netscape.security.ForbiddenTargetException;
import netscape.security.PrivilegeManager;

public class GameApplet
extends TApplet
implements ImageRetriever,
ImagePaintListener,
ImageFilenameProvider {
    public static final double VERSION = 1.1;
    public static final double SAVE_FORMAT_VERSION = 0.5;
    public static final double MINIMUM_SAVE_FORMAT_VERSION = 0.4;
    public static final String NEWGAME_SCENE = "com/templar/games/stormrunner/media/scenes/newgame.pac";
    public static final int IMAGECACHE_DELAY = 20000;
    public static final int BAY_STATE = 0;
    public static final int VIEW_STATE = 1;
    public static final int OPT_STATE = 2;
    public static final Color GRID_COLOR = new Color(255, 255, 0);
    public static GameApplet thisApplet;
    public static AppletContext appletContext;
    public static AudioManager audio;
    protected URL HelpURL;
    protected String HelpTarget;
    protected ProgressComponent progressDialog;
    protected GameState CurrentGameState;
    protected StatusPanel statpanel;
    protected OpsPanel opspanel;
    protected Renderer CurrentRenderer;
    protected Editor CurrentEditor;
    protected Grid CurrentGrid;
    protected CargoBay bay;
    protected BuildPanel buildpanel;
    protected OptionsPanel optionspanel;
    protected SimpleContainer OptionsScreen;
    protected SimpleContainer ViewScreen;
    protected SimpleContainer BayScreen;
    protected SimpleContainer TopLayer;
    protected int State = 2;
    protected int LastState;
    protected boolean[] StatusMinimized;
    protected MediaTracker CacheTracker = new MediaTracker(this);
    protected Hashtable<String, ImageTrack> images = new Hashtable();
    protected Hashtable<Image, String> imageFilename = new Hashtable();
    protected Vector ImageCacheActiveList = new Vector();
    protected boolean playing;
    protected Object PaintLock = new Object();
    private Image buffer;
    private Graphics graphics;
    private UtilityThread cacheThread;
    private LoadSaveFrame savegameWindow;
    protected double VersionOfCurrentSavegame = -1.0;
    private transient boolean wasMuted;
    private FocusCatcher FocusFixer;

    public void init() {
        thisApplet = this;
        appletContext = this.getAppletContext();
        this.enableEvents(16L);
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put("ButtonClick", "com/templar/games/stormrunner/media/sounds/interface/button_click-16.au");
        hashtable.put("ButtonRun", "com/templar/games/stormrunner/media/sounds/interface/button_run.au");
        hashtable.put("ButtonStop", "com/templar/games/stormrunner/media/sounds/interface/button_stop.au");
        hashtable.put("ButtonError", "com/templar/games/stormrunner/media/sounds/interface/error.au");
        hashtable.put("DeathAlarm", "com/templar/games/stormrunner/media/sounds/interface/beep_death.au");
        hashtable.put("PanelZip", "com/templar/games/stormrunner/media/sounds/interface/panelzip.au");
        hashtable.put("ProgramClick", "com/templar/games/stormrunner/media/sounds/interface/program_click.au");
        hashtable.put("Achilles", "com/templar/games/stormrunner/media/sounds/robots/achilles.au");
        hashtable.put("Arachnae", "com/templar/games/stormrunner/media/sounds/robots/arachne.au");
        hashtable.put("Hermes", "com/templar/games/stormrunner/media/sounds/robots/hermes.au");
        hashtable.put("AchillesCover", "com/templar/games/stormrunner/media/sounds/robots/achilles-cut.au");
        hashtable.put("ArachnaeCover", "com/templar/games/stormrunner/media/sounds/robots/arachne-cut.au");
        hashtable.put("HermesCover", "com/templar/games/stormrunner/media/sounds/robots/hermes-cut.au");
        hashtable.put("RobotStart", "com/templar/games/stormrunner/media/sounds/effects/startup-running.au");
        hashtable.put("Robot-Happy", "com/templar/games/stormrunner/media/sounds/robotvoices/happy.au");
        hashtable.put("Robot-Scared", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-scared_1.au");
        hashtable.put("Robot-ReplyScared", "com/templar/games/stormrunner/media/sounds/robotvoices/reply-scared.au");
        hashtable.put("Robot-Alarm", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-alarm.au");
        hashtable.put("Robot-Alert", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-alert.au");
        hashtable.put("Robot-Angry", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-angry.au");
        hashtable.put("Robot-Deny", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-deny.au");
        hashtable.put("Robot-Question", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-question.au");
        hashtable.put("Robot-Scream", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-scream.au");
        hashtable.put("Robot-NotCompute", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-nocompute.au");
        hashtable.put("Robot-Nonsense-1", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-nonsense01.au");
        hashtable.put("Robot-Nonsense-2", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-nonsense02.au");
        hashtable.put("Robot-Nonsense-3", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-nonsense03.au");
        hashtable.put("Robot-Nonsense-4", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-nonsense04.au");
        hashtable.put("Robot-Nonsense-5", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-nonsense05.au");
        hashtable.put("Robot-Nonsense-6", "com/templar/games/stormrunner/media/sounds/robotvoices/rcx-nonsense06.au");
        hashtable.put("Build-Arm-Tools", "com/templar/games/stormrunner/media/sounds/effects/build_arm-tools.au");
        hashtable.put("Build-Dismantle", "com/templar/games/stormrunner/media/sounds/effects/build_dismantle.au");
        hashtable.put("Build-Elevator", "com/templar/games/stormrunner/media/sounds/effects/build_elevator.au");
        hashtable.put("Build-ArmWrist", "com/templar/games/stormrunner/media/sounds/effects/build_armwrist.au");
        hashtable.put("Build-FrontArm", "com/templar/games/stormrunner/media/sounds/effects/build_front-assarm.au");
        hashtable.put("Build-RearTopArm-Move", "com/templar/games/stormrunner/media/sounds/effects/build_reartop-arm-move.au");
        hashtable.put("Build-RearTopArm-End", "com/templar/games/stormrunner/media/sounds/effects/build_reartop-arm-end.au");
        hashtable.put("Build-SensorArm-Beat", "com/templar/games/stormrunner/media/sounds/effects/build_sensorarm-beat.au");
        hashtable.put("Build-SensorArm-End", "com/templar/games/stormrunner/media/sounds/effects/build_sensorarm-end.au");
        hashtable.put("Build-SensorArm-Move-Fadeout", "com/templar/games/stormrunner/media/sounds/effects/build_sensorarm-move-fadeout.au");
        hashtable.put("ShipDoor", "com/templar/games/stormrunner/media/sounds/effects/shipdoor.au");
        hashtable.put("Piledriver-Strike", "com/templar/games/stormrunner/media/sounds/assemblies/piledriver-strike.au");
        hashtable.put("Arm-Up", "com/templar/games/stormrunner/media/sounds/assemblies/arm-up.au");
        hashtable.put("Arm-Down", "com/templar/games/stormrunner/media/sounds/assemblies/arm-down.au");
        hashtable.put("Arm-Store", "com/templar/games/stormrunner/media/sounds/assemblies/arm-store.au");
        hashtable.put("Arm-Retrieve", "com/templar/games/stormrunner/media/sounds/assemblies/arm-retrieve.au");
        hashtable.put("CannonFire", "com/templar/games/stormrunner/media/sounds/effects/cannon-fire.au");
        hashtable.put("CannonMiss", "com/templar/games/stormrunner/media/sounds/effects/cannon-miss.au");
        hashtable.put("CannonHit", "com/templar/games/stormrunner/media/sounds/effects/cannon-hit.au");
        hashtable.put("CannonScan", "com/templar/games/stormrunner/media/sounds/effects/cannon-scan.au");
        hashtable.put("FieryDeath", "com/templar/games/stormrunner/media/sounds/deaths/death-lava.au");
        hashtable.put("PlantNotice", "com/templar/games/stormrunner/media/sounds/effects/plant-notice.au");
        hashtable.put("PlantDeath", "com/templar/games/stormrunner/media/sounds/deaths/death-plant.au");
        hashtable.put("CliffSlip", "com/templar/games/stormrunner/media/sounds/deaths/death-cliff-slip.au");
        try {
            audio = new AudioManager(new SunAudioDevice(this), hashtable);
        }
        catch (Exception exception) {
            System.err.println("Stormrunner: Error initializing AudioManager. Falling back to AppletAudioDevice.");
            try {
                audio = new AudioManager(new AppletAudioDevice(this), hashtable);
            }
            catch (Exception exception2) {
                System.err.println("Stormrunner: Error initializing AppletAudioDevice. Sounds disabled.");
                audio = new AudioManager(new NullAudioDevice(), null);
            }
        }
        this.startup();
    }

    public void startup() {
        this.setLayout(null);
        String string = this.getParameter("HelpURL");
        if (string != null) {
            try {
                try {
                    this.HelpURL = new URL(string);
                }
                catch (MalformedURLException malformedURLException) {
                    this.HelpURL = new URL(this.getDocumentBase(), string);
                }
                this.HelpTarget = this.getParameter("HelpTarget");
            }
            catch (MalformedURLException malformedURLException) {
                System.err.println("Stormrunner: GameApplet: Parameter HelpURL is malformed. Help button will not function.");
            }
        } else {
            System.err.println("Stormrunner: GameApplet: Parameter HelpURL was not found in the Applet tag. Help button will not function.");
        }
        ImageComponent.addImagePaintListener(this);
        ImageComponent.setImageFilenameProvider(this);
        ImageComponent imageComponent = new ImageComponent(this.getImage("com/templar/games/stormrunner/media/images/mapinterface/mappanel_logo.gif"));
        ImageComponent imageComponent2 = new ImageComponent(this.getImage("com/templar/games/stormrunner/media/images/mapinterface/mappanel_top.gif"));
        ImageComponent imageComponent3 = new ImageComponent(this.getImage("com/templar/games/stormrunner/media/images/mapinterface/mappanel_bottom.gif"));
        ImageComponent imageComponent4 = new ImageComponent(this.getImage("com/templar/games/stormrunner/media/images/mapinterface/mappanel_left.gif"));
        ImageComponent imageComponent5 = new ImageComponent(this.getImage("com/templar/games/stormrunner/media/images/mapinterface/mappanel_right.gif"));
        imageComponent.setLocation(0, 0);
        imageComponent.setSize(imageComponent.getSize());
        imageComponent2.setSize(imageComponent2.getSize());
        imageComponent2.setLocation(imageComponent.getSize().width, 0);
        imageComponent4.setSize(imageComponent4.getSize());
        imageComponent4.setLocation(0, imageComponent.getSize().height);
        imageComponent5.setSize(imageComponent5.getSize());
        imageComponent5.setLocation(this.getSize().width - imageComponent5.getSize().width, 0);
        imageComponent3.setSize(imageComponent3.getSize());
        imageComponent3.setLocation(imageComponent4.getSize().width, this.getSize().height - imageComponent3.getSize().height);
        this.StatusMinimized = new boolean[3];
        this.StatusMinimized[0] = true;
        this.StatusMinimized[1] = false;
        this.StatusMinimized[2] = true;
        this.CurrentGrid = new Grid(50, 50, GRID_COLOR);
        this.CurrentGrid.setBounds(0, 0, this.getSize().width, this.getSize().height);
        this.CurrentGrid.setOn(true);
        this.opspanel = new OpsPanel(this, this);
        this.opspanel.setLocation(0, this.getSize().height - this.opspanel.getSize().height);
        boolean bl = false;
        boolean bl2 = false;
        if (this.optionspanel != null) {
            if (!this.optionspanel.getEnabled()) {
                bl = true;
            }
            if (this.progressDialog != null) {
                bl2 = true;
            }
        }
        this.optionspanel = new OptionsPanel(this);
        this.optionspanel.setLocation(0, 0);
        if (bl) {
            this.optionspanel.setEnabled(false);
        }
        if (bl2) {
            this.optionspanel.add(this.progressDialog, 0);
        }
        this.bay = new CargoBay(this);
        this.bay.setLocation(0, 0);
        this.ViewScreen = new SimpleContainer();
        // 0, 0, this.getSize().width, this.getSize().height
        this.ViewScreen.setBounds(Main.frame.getBounds());
        this.BayScreen = new SimpleContainer();
        this.BayScreen.setBounds(0, 0, this.getSize().width, this.getSize().height);
        this.OptionsScreen = new SimpleContainer();
        this.OptionsScreen.setBounds(0, 0, this.getSize().width, this.getSize().height);
        this.TopLayer = new SimpleContainer();
        // 0, 0, this.getSize().width, this.getSize().height
        this.TopLayer.setBounds(Main.frame.getBounds());
        this.OptionsScreen.add(this.optionspanel);
        this.ViewScreen.add(imageComponent);
        this.ViewScreen.add(imageComponent2);
        this.ViewScreen.add(imageComponent4);
        this.ViewScreen.add(imageComponent5);
        this.ViewScreen.add(imageComponent3);
        this.BayScreen.add(this.bay);
        this.TopLayer.add(this.OptionsScreen, 0);
        this.add(this.TopLayer);
        if (System.getProperty("java.vendor").contains("Netscape")) {
            this.FocusFixer = new FocusCatcher(this.CurrentRenderer, this);
            this.add(this.FocusFixer, 0);
        }
        try {
            this.cacheThread = new UtilityThread(20000, this, this.getClass().getMethod("cacheClean", null), false);
            this.cacheThread.start();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void startup(GameState gameState) {
        this.startup();
        this.CurrentGameState = gameState;
        this.CurrentGameState.setAppletRef(this);
        this.CurrentRenderer = this.CurrentGameState.getRenderer();
        this.CurrentRenderer.setLocation(0, 0);
        // 480/360
        this.CurrentRenderer.setSize(780, 560);
        this.CurrentGameState.getCurrentScene().setRenderer(this.CurrentRenderer);
        this.CurrentRenderer.setOffsetToCenter(this.CurrentGameState.getCurrentScene().getRobotStart());
        this.buildpanel = new BuildPanel(this, this.bay, this);
        this.buildpanel.setLocation(0, 0);
        this.statpanel = new StatusPanel(this, this.CurrentGameState);
        this.statpanel.setLocationHolder(this.getSize().width - this.statpanel.getSize().width - 5, this.getSize().height - this.statpanel.getSize().height);
        this.statpanel.setLocation(this.getSize().width - this.statpanel.getSize().width - 5, this.getSize().height - 20);
        this.CurrentEditor = new Editor(this.CurrentGameState, this, null, this);
        this.CurrentEditor.setLocation(0, 35);
        this.CurrentRenderer.setInternalFocus(this.CurrentEditor);
        this.BayScreen.add(this.buildpanel, 0);
        this.ViewScreen.add(this.CurrentEditor, 0);
        this.ViewScreen.add(this.CurrentGrid);
        this.ViewScreen.add(this.CurrentRenderer);
    }

    public void setState(int n) {
        this.opspanel.setState(n);
    }

    public void internalSetState(int n) {
        if (this.State != n) {
            this.StatusMinimized[this.State] = this.statpanel.isMinimized();
            this.LastState = this.State;
            this.State = n;
            this.toggleScreen(this.LastState, false);
            this.toggleScreen(this.State, true);
            this.repaint();
        }
    }

    protected void toggleScreen(int n, boolean bl) {
        if (bl) {
            switch (n) {
                case 0: {
                    this.ImageCacheActiveList.removeAllElements();
                    this.ImageCacheActiveList.addElement("opspanel");
                    this.ImageCacheActiveList.addElement("statuspanel");
                    this.TopLayer.add(this.BayScreen);
                    if (this.FocusFixer != null) {
                        this.FocusFixer.setFocusTarget(this.buildpanel);
                    }
                    this.buildpanel.requestFocus();
                    this.statpanel.setMinimized(this.StatusMinimized[0]);
                    return;
                }
                case 1: {
                    this.ImageCacheActiveList.removeAllElements();
                    this.ImageCacheActiveList.addElement("opspanel");
                    this.ImageCacheActiveList.addElement("statuspanel");
                    this.TopLayer.add(this.ViewScreen);
                    this.CurrentRenderer.setVisible(true);
                    if (this.FocusFixer != null) {
                        this.FocusFixer.setFocusTarget(this.CurrentRenderer);
                    }
                    this.CurrentRenderer.requestFocus();
                    this.statpanel.setMinimized(this.StatusMinimized[1]);
                    return;
                }
                case 2: {
                    this.CurrentGameState.stop();
                    this.TopLayer.remove(this.statpanel);
                    this.TopLayer.remove(this.opspanel);
                    this.TopLayer.add(this.OptionsScreen, 0);
                    return;
                }
            }
            return;
        }
        switch (n) {
            case 0: {
                this.TopLayer.remove(this.BayScreen);
                break;
            }
            case 1: {
                this.TopLayer.remove(this.ViewScreen);
                this.CurrentRenderer.setVisible(false);
                break;
            }
            case 2: {
                this.CurrentGameState.start();
                this.TopLayer.remove(this.OptionsScreen);
                this.TopLayer.add(this.statpanel, 0);
                this.TopLayer.add(this.opspanel, 0);
                break;
            }
        }
    }

    public int getState() {
        return this.State;
    }

    public GameState getGameState() {
        return this.CurrentGameState;
    }

    public BuildPanel getBuildPanel() {
        return this.buildpanel;
    }

    public Editor getEditor() {
        return this.CurrentEditor;
    }

    public URL getHelpURL() {
        return this.HelpURL;
    }

    public String getHelpTarget() {
        return this.HelpTarget;
    }

    public void start() {
        if (this.CurrentGameState != null) {
            this.CurrentGameState.start();
        }
    }

    public void stop() {
        if (this.CurrentGameState != null) {
            this.CurrentGameState.stop();
        }
    }

    public Grid getGrid() {
        return this.CurrentGrid;
    }

    public StatusPanel getStatusPanel() {
        return this.statpanel;
    }

    public Vector getImageCacheActiveList() {
        return this.ImageCacheActiveList;
    }

    public void flushImageCache() {
        Enumeration enumeration = this.images.elements();
        while (enumeration.hasMoreElements()) {
            ImageTrack imageTrack = (ImageTrack)enumeration.nextElement();
            int n = this.checkImage(imageTrack.image, this);
            boolean bl = (n & 0x20) != 0;
            String string = this.imageFilename.get(imageTrack.image);
            if (string != null) {
                for (int i = 0; bl && i < this.ImageCacheActiveList.size(); ++i) {
                    String string2 = (String)this.ImageCacheActiveList.elementAt(i);
                    if (!string.contains(string2)) continue;
                    bl = false;
                }
            }
            if (!bl) continue;
            imageTrack.time = -1L;
            imageTrack.image.flush();
        }
        System.gc();
    }

    public synchronized boolean cacheClean() {
        long l = System.currentTimeMillis();
        Enumeration enumeration = this.images.elements();
        while (enumeration.hasMoreElements()) {
            ImageTrack imageTrack = (ImageTrack)enumeration.nextElement();
            int n = this.checkImage(imageTrack.image, this);
            boolean bl = (n & 0x20) != 0;
            String string = (String)this.imageFilename.get(imageTrack.image);
            if (string != null) {
                for (int i = 0; bl && i < this.ImageCacheActiveList.size(); ++i) {
                    String string2 = (String)this.ImageCacheActiveList.elementAt(i);
                    if (!string.contains(string2)) continue;
                    bl = false;
                }
            }
            if (imageTrack.time == -1L || !bl || imageTrack.time + 20000L >= l) continue;
            imageTrack.time = -1L;
            imageTrack.image.flush();
        }
        Runtime.getRuntime().gc();
        return true;
    }

    public Image getImage(URL uRL, String string) {
        if (string == null) {
            Debug.println("getImage(null,null) called.");
            return null;
        }
        Object v = this.images.get(string);
        if (v != null) {
            ImageTrack imageTrack = (ImageTrack)v;
            return imageTrack.image;
        }
        Image image = super.getImage(uRL, string);
        if (image == null) {
            Debug.println("super.getImage(" + uRL + "," + string + ") returned null");
        } else {
            ImageTrack imageTrack = new ImageTrack(image);
            this.images.put(string, imageTrack);
            this.imageFilename.put(image, string);
        }
        return image;
    }

    public Image getImage(String string) {
        if (!"".equals(string) && string != null) {
            return this.getImage(this.getDocumentBase(), string);
        }
        return null;
    }

    public String getImageFilename(Image image) {
        return this.imageFilename.get(image);
    }

    public void imagePainted(ImageComponent imageComponent, Image image) {
        this.hitCache(image, imageComponent);
    }

    public void hitCache(Image image) {
        this.hitCache(image, null);
    }

    public synchronized void hitCache(Image image, ImageComponent imageComponent) {
        block23: {
            if (image == null) {
                Debug.println("Image passed to hitCache is null");
                return;
            }
            try {
                Object v = this.imageFilename.get(image);
                if (v == null) break block23;
                String string = (String)v;
                ImageTrack imageTrack = this.images.get(string);
                if (imageTrack == null) {
                    Debug.println("There's a null entry in the ImageCache.");
                    Debug.println(String.valueOf(string) + "==" + imageTrack);
                }
                if (imageComponent != null) {
                    ImageComponent imageComponent2 = imageComponent;
                    synchronized (imageComponent2) {
                        if (imageTrack.time == -1L) {
                            this.prepareImage(imageTrack.image, imageComponent);
                            try {
                                int n = 0;
                                int n2 = this.checkImage(imageTrack.image, imageComponent);
                                while ((n2 & 0xF0) == 0) {
                                    if (n > 2) {
                                        System.out.println("Potential deadlock waiting for Image:");
                                        System.out.println(string);
                                        System.out.print("Flags: ");
                                        if ((n2 & 0x80) != 0) {
                                            System.out.print(" ABORT ");
                                        }
                                        if ((n2 & 0x40) != 0) {
                                            System.out.print(" ERROR ");
                                        }
                                        if ((n2 & 0x20) != 0) {
                                            System.out.print(" ALLBITS ");
                                        }
                                        if ((n2 & 0x10) != 0) {
                                            System.out.print(" FRAMEBITS ");
                                        }
                                        if ((n2 & 8) != 0) {
                                            System.out.print(" SOMEBITS ");
                                        }
                                        if ((n2 & 2) != 0) {
                                            System.out.print(" HEIGHT ");
                                        }
                                        if ((n2 & 1) != 0) {
                                            System.out.print(" WIDTH ");
                                        }
                                        if ((n2 & 4) != 0) {
                                            System.out.print(" PROPERTIES ");
                                        }
                                        System.out.println("");
                                        break;
                                    }
                                    ++n;
                                    imageComponent.wait(1000L);
                                    n2 = this.checkImage(imageTrack.image, imageComponent);
                                }
                            }
                            catch (InterruptedException interruptedException) {
                                System.out.println("Someday I'd like to meet the kind of thread that interrupts this sort of thing.");
                                interruptedException.printStackTrace();
                            }
                        }
                    }
                }
                this.CacheTracker.addImage(imageTrack.image, 0);
                try {
                    this.CacheTracker.waitForID(0, 1000L);
                }
                catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                this.CacheTracker.removeImage(imageTrack.image, 0);
                imageTrack.time = System.currentTimeMillis();
                return;
            }
            catch (OutOfMemoryError outOfMemoryError) {
                System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
                System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
                System.out.println("           OOME Exception caught in hitcache!");
                System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
                System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
                throw outOfMemoryError;
            }
        }
    }

    protected void setupBuffer() {
        this.buffer = this.createImage(this.getSize().width, this.getSize().height);
        this.graphics = this.buffer.getGraphics();
    }

    public Object getPaintLock() {
        return this.PaintLock;
    }

    public boolean imageUpdate(Image image, int n, int n2, int n3, int n4, int n5) {
        return false;
    }

    public void update(Graphics graphics) {
        this.paint(graphics);
    }

    public void paint(Graphics graphics) {
        Object object = this.PaintLock;
        synchronized (object) {
            if (this.graphics == null) {
                this.setupBuffer();
            }
            this.graphics.setClip(graphics.getClip());
            super.paint(this.graphics);
            graphics.drawImage(this.buffer, 0, 0, null);
            this.getToolkit().sync();
            return;
        }
    }

    public int getLastState() {
        return this.LastState;
    }

    public OptionsPanel getOptionsPanel() {
        return this.optionspanel;
    }

    public void mute() {
        this.wasMuted = audio.isMuted();
        audio.mute();
    }

    public void unMute() {
        if (!this.wasMuted) {
            audio.unMute();
        }
    }

    public boolean isMidGame() {
        return this.playing;
    }

    public void setMidGame(boolean bl) {
        this.playing = bl;
    }

    public void sendStatusMessage(String string) {
        this.sendStatusMessage(string, true);
    }

    public void sendStatusMessage(String string, boolean bl) {
        this.statpanel.addStatusMessage(string);
        if (bl && this.State == 2) {
            this.setState(this.LastState);
        }
    }

    public void newGame() {
        URL uRL = null;
        InputStream inputStream = this.getClass().getResourceAsStream("/com/templar/games/stormrunner/media/scenes/newgame.pac");
        try {
            if (inputStream == null) {
                uRL = new URL(this.getDocumentBase(), NEWGAME_SCENE);
                inputStream = uRL.openStream();
            }
            this.optionspanel.setEnabled(false);
            DataInputStream dataInputStream = new DataInputStream(new MonitoredInputStream(inputStream, null));
            int n = dataInputStream.readInt();
            byte[] arrby = new byte[n];
            dataInputStream.readFully(arrby);
            this.progressDialog = new ProgressComponent("Please Wait...", n + 100000, this);
            this.progressDialog.setLocation(5, 337);
            this.optionspanel.add(this.progressDialog, 0);
            this.progressDialog.setVisible(true);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new MonitoredInputStream(new ByteArrayInputStream(arrby), this.progressDialog));
            Loader loader = new Loader(this, bufferedInputStream);
            UtilityThread utilityThread = new UtilityThread(0, loader, loader.getClass().getMethod("newgame", null), false);
            utilityThread.start();
            Debug.println(utilityThread);
            return;
        }
        catch (Exception exception) {
            Debug.println("Couldn't load " + uRL + ".\n" + exception);
            exception.printStackTrace();
            return;
        }
    }

    public void saveGame() {
        Container container;
        if (!this.playing) {
            Debug.println("Can't save if you're not playing.");
            audio.play("ButtonError");
            return;
        }
        try {
            PrivilegeManager.enablePrivilege("UniversalFileAccess");
            PrivilegeManager.enablePrivilege("UniversalPropertyRead");
            PolicyEngine.assertPermission(PermissionID.FILEIO);
        }
        catch (ForbiddenTargetException forbiddenTargetException) {
            System.err.println("Stormrunner: saveGame(): User clicked Deny.");
            return;
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
        if (this.buildpanel.isBayOccupied()) {
            this.setState(0);
            this.sendStatusMessage("Cannot save while a robot is in the bay. Store, Engage, or Dismantle this robot first.", false);
            return;
        }
        Frame frame = null;
        for (container = this; container != null && !(container instanceof Frame); container = container.getParent()) {
        }
        if (container == null) {
            Debug.println("cant find a frame");
            return;
        }
        frame = (Frame)container;
        if (this.savegameWindow == null) {
            this.savegameWindow = new LoadSaveFrame(frame, 1);
        } else {
            this.savegameWindow.toFront();
        }
        File file = this.savegameWindow.getResponse();
        if (file == null) {
            Debug.println("Aborting save");
            this.savegameWindow = null;
            return;
        }
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
            Loader loader = new Loader(this, objectOutputStream, this.CurrentGameState.getTickCount(), this.savegameWindow.getDescription());
            loader.activate();
        }
        catch (Exception exception) {
            System.err.println("Stormrunner: EXCEPTION DURING SAVE ATTEMPT! (" + file + ")");
            exception.printStackTrace();
            new com.templar.games.stormrunner.templarutil.gui.MessageDialog(frame, "Error!", "Error saving game!", "Abort");
        }
        try {
            PrivilegeManager.revertPrivilege("UniversalFileAccess");
            PrivilegeManager.revertPrivilege("UniversalPropertyRead");
            PolicyEngine.revertPermission(PermissionID.FILEIO);
        }
        catch (Exception exception) {
            System.err.println("Stormrunner: Sanity check: failure during revertPrivilege following save.");
            exception.printStackTrace();
        }
        this.savegameWindow = null;
    }

    public void loadGame() {
        Container container;
        try {
            PrivilegeManager.enablePrivilege("UniversalFileAccess");
            PrivilegeManager.enablePrivilege("UniversalPropertyRead");
            PolicyEngine.assertPermission(PermissionID.FILEIO);
        }
        catch (ForbiddenTargetException forbiddenTargetException) {
            System.err.println("Stormrunner: loadGame(): User clicked Deny.");
            return;
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
        Frame frame = null;
        for (container = this; container != null && !(container instanceof Frame); container = container.getParent()) {
        }
        if (container == null) {
            Debug.println("cant find a frame");
            return;
        }
        frame = (Frame)container;
        if (this.savegameWindow == null) {
            this.savegameWindow = new LoadSaveFrame(frame, 0);
        } else {
            this.savegameWindow.toFront();
        }
        File file = this.savegameWindow.getResponse();
        if (file == null) {
            Debug.println("Aborting load");
            this.savegameWindow = null;
            return;
        }
        Loader loader = null;
        try {
            this.optionspanel.setEnabled(false);
            this.progressDialog = new ProgressComponent("Please Wait...", (int)file.length() + 8192, this);
            this.progressDialog.setLocation(5, 337);
            this.optionspanel.add(this.progressDialog, 0);
            this.progressDialog.setVisible(true);
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(new MonitoredInputStream(new FileInputStream(file), this.progressDialog))));
            loader = new Loader(this, objectInputStream);
            new UtilityThread(0, loader, loader.getClass().getMethod("activate", null), false).start();
        }
        catch (Exception exception) {
            if (loader != null) {
                loader.failout("Error loading game! Contact bugs@legomindstorm.com for assistance.", "Stormrunner: Error loading saved game from file: " + file, exception);
                return;
            }
            System.err.println("Stormrunner: EXCEPTION DURING SAVE ATTEMPT! (" + file + ")");
            exception.printStackTrace();
            new com.templar.games.stormrunner.templarutil.gui.MessageDialog(frame, "Error!", "Error loading saved game!", "Abort");
            return;
        }
        try {
            PrivilegeManager.revertPrivilege("UniversalFileAccess");
            PrivilegeManager.revertPrivilege("UniversalPropertyRead");
            PolicyEngine.revertPermission(PermissionID.FILEIO);
        }
        catch (Exception exception) {
            System.err.println("Stormrunner: Sanity check: failure during revertPrivilege following load.");
            exception.printStackTrace();
        }
        this.savegameWindow = null;
    }

    public double getLoadingVersion() {
        return this.VersionOfCurrentSavegame;
    }
class ImageTrack {
    public Image image;
    public long time;

    public ImageTrack(Image image) {
        //GameApplet.this = GameApplet.this;
        this.image = image;
        this.time = -1L;
    }

    public String toString() {
        StringBuilder stringBuffer = new StringBuilder("ImageTrack[");
        if (GameApplet.this.imageFilename.containsKey(this.image)) {
            stringBuffer.append(GameApplet.this.imageFilename.get(this.image));
        } else {
            stringBuffer.append("null");
        }
        stringBuffer.append(",");
        stringBuffer.append(this.time);
        stringBuffer.append("]");
        return stringBuffer.toString();
    }
}
public class Loader {
    int which;
    InputStream inp;
    OutputStream out;
    GameApplet applet;
    long tick;
    String desc;

    public Loader(GameApplet gameApplet2, InputStream inputStream) {
        //GameApplet.this = GameApplet.this;
        this.inp = inputStream;
        this.applet = gameApplet2;
        this.which = 1;
    }

    public Loader(GameApplet gameApplet2, OutputStream outputStream, long l, String string) {
        //GameApplet.this = GameApplet.this;
        this.out = outputStream;
        this.which = 0;
        this.applet = gameApplet2;
        this.desc = string;
        this.tick = l;
    }

    public boolean newgame() {
        Scene scene = null;
        try {
            scene = SceneBuilder.readScene(this.applet, this.inp, GameApplet.this.progressDialog);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        GameState gameState = new GameState(this.applet);
        World world = new World();
        world.addScene(scene);
        Debug.println("adding scene to empty world");
        gameState.setWorld(world);
        Debug.println("adding 2048 here");
        GameApplet.this.progressDialog.notifyProgress(25000);
        if (GameApplet.this.CurrentGameState != null) {
            GameApplet.this.CurrentGameState.dispose();
            GameApplet.this.CurrentGameState = null;
        }
        this.applet.removeAll();
        GameApplet.this.CurrentGrid = null;
        GameApplet.this.statpanel = null;
        GameApplet.this.CurrentRenderer = null;
        GameApplet.this.CurrentEditor = null;
        GameApplet.this.opspanel = null;
        GameApplet.this.bay.clearRamp();
        GameApplet.this.bay = null;
        GameApplet.this.buildpanel = null;
        System.gc();
        GameApplet.this.cacheThread.politeStop();
        GameApplet.this.startup(gameState);
        GameApplet.this.progressDialog.setValue(GameApplet.this.progressDialog.getMaximum());
        String string = GameApplet.this.getParameter("username");
        if (string == null) {
            gameState.setUsername("Unknown");
        } else {
            gameState.setUsername(string);
        }
        gameState.setUserRank("Maint. Spec. 5th Class");
        if (GameApplet.this.getParameter("cheats") == null) {
            gameState.setSecurityLevel(1);
            gameState.setPolymetals(80);
            gameState.setEnergyUnits(80);
        } else {
            gameState.setSecurityLevel(5);
            gameState.setPolymetals(999);
            gameState.setEnergyUnits(999);
        }
        GameApplet.this.setState(0);
        GameApplet.this.playing = true;
        GameApplet.this.optionspanel.setEnabled(true);
        GameApplet.this.optionspanel.remove(GameApplet.this.progressDialog);
        GameApplet.this.progressDialog = null;
        return false;
    }

    public boolean activate() {
        String string = null;
        try {
            if (this.which == 0) {
                string = "Error saving game! Contact bugs@legomindstorm.com for assistance.";
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = !(this.out instanceof ObjectOutputStream) ? new ObjectOutputStream(byteArrayOutputStream) : (ObjectOutputStream)this.out;
                objectOutputStream.writeDouble(0.5);
                objectOutputStream.writeDouble(1.1);
                objectOutputStream.writeObject(this.desc);
                objectOutputStream.writeLong(this.tick);
                objectOutputStream.writeObject(GameApplet.this.CurrentGameState);
                objectOutputStream.writeInt(GameApplet.this.statpanel.getPanelState());
                objectOutputStream.writeInt(GameApplet.this.LastState);
                objectOutputStream.close();
            } else {
                string = "Error loading game! Contact bugs@legomindstorm.com for assistance.";
                ObjectInputStream objectInputStream = !(this.inp instanceof ObjectInputStream) ? new ObjectInputStream(this.inp) : (ObjectInputStream)this.inp;
                double d = objectInputStream.readDouble();
                if (d < 0.4) {
                    this.failout("That save file doesn't work with this version of the game.", "Save file version (" + d + ") < MINIMUM_SAVE_FORMAT_VERSION (" + 0.4 + ")\nUnable to load.", null);
                    return false;
                }
                GameApplet.this.VersionOfCurrentSavegame = objectInputStream.readDouble();
                objectInputStream.readObject();
                this.tick = objectInputStream.readLong();
                GameState gameState = (GameState)objectInputStream.readObject();
                objectInputStream.readInt();
                int n = objectInputStream.readInt();
                objectInputStream.close();
                gameState.setTickCount(this.tick);
                if (GameApplet.this.CurrentGameState != null) {
                    GameApplet.this.CurrentGameState.dispose();
                }
                GameApplet.this.CurrentGameState = null;
                GameApplet.this.CurrentRenderer = null;
                thisApplet.removeAll();
                GameApplet.this.CurrentGrid = null;
                GameApplet.this.statpanel = null;
                GameApplet.this.CurrentEditor = null;
                GameApplet.this.opspanel = null;
                GameApplet.this.bay.clearRamp();
                GameApplet.this.bay = null;
                GameApplet.this.buildpanel = null;
                System.gc();
                GameApplet.this.cacheThread.politeStop();
                GameApplet.this.progressDialog.notifyProgress(4096);
                GameApplet.this.startup(gameState);
                GameApplet.this.buildpanel.setUsername(gameState.getUsername());
                GameApplet.this.buildpanel.setUserRank(gameState.getUserRank());
                GameApplet.this.buildpanel.setSecurityLevel(gameState.getSecurityLevel());
                GameApplet.this.buildpanel.setPolymetals(gameState.getPolymetals());
                GameApplet.this.buildpanel.setEnergyUnits(gameState.getEnergyUnits());
                Enumeration enumeration = gameState.getCurrentScene().getObjects().elements();
                while (enumeration.hasMoreElements()) {
                    PhysicalObject physicalObject = (PhysicalObject)enumeration.nextElement();
                    if (!(physicalObject instanceof Trigger)) continue;
                    ((Trigger)(physicalObject)).setGameState(gameState);
                }
                GameApplet.this.progressDialog.notifyProgress(2048);
                GameApplet.this.statpanel.reportNewCurrentRobot(GameApplet.this.CurrentGameState.getCurrentRobot());
                GameApplet.this.CurrentEditor.setRobot(GameApplet.this.CurrentGameState.getCurrentRobot());
                this.applet.setState(n);
                GameApplet.this.playing = true;
                GameApplet.this.progressDialog.notifyProgress(2048);
                GameApplet.this.optionspanel.remove(GameApplet.this.progressDialog);
                GameApplet.this.progressDialog = null;
                GameApplet.this.optionspanel.setEnabled(true);
            }
        }
        catch (Exception exception) {
            this.failout(string, string, exception);
        }
        return false;
    }

    public void failout(String string, String string2, Throwable throwable) {
        Container container;
        System.err.println(string2);
        if (throwable != null) {
            throwable.printStackTrace();
        }
        Frame frame;
        for (container = thisApplet; container != null && !(container instanceof Frame); container = container.getParent()) {
        }
        if (container == null) {
            System.err.println("Stormrunner: Loader: failout(): Cant find a frame!");
            return;
        }
        frame = (Frame)container;
        new com.templar.games.stormrunner.templarutil.gui.MessageDialog(frame, "Error!", string, "Abort");
        if (GameApplet.this.progressDialog != null) {
            GameApplet.this.optionspanel.remove(GameApplet.this.progressDialog);
            GameApplet.this.progressDialog = null;
        }
        GameApplet.this.optionspanel.setEnabled(true);
        GameApplet.this.optionspanel.repaint();
    }
}

}
