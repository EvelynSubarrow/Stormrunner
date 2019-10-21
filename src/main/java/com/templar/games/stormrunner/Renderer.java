/*
 * Decompiled with CFR 0.147-SNAPSHOT.
 */
package com.templar.games.stormrunner;

import com.templar.games.stormrunner.ClickDelegator;
import com.templar.games.stormrunner.GameApplet;
import com.templar.games.stormrunner.GameState;
import com.templar.games.stormrunner.Grid;
import com.templar.games.stormrunner.Map;
import com.templar.games.stormrunner.MapCell;
import com.templar.games.stormrunner.PhysicalObject;
import com.templar.games.stormrunner.Position;
import com.templar.games.stormrunner.Robot;
import com.templar.games.stormrunner.Scene;
import com.templar.games.stormrunner.Shroud;
import com.templar.games.stormrunner.ShroudEvent;
import com.templar.games.stormrunner.ShroudListener;
import com.templar.games.stormrunner.templarutil.audio.AudioManager;
import com.templar.games.stormrunner.templarutil.audio.SoundListener;
import com.templar.games.stormrunner.templarutil.gui.ImageContainer;
import com.templar.games.stormrunner.templarutil.gui.ReportingComponentListener;
import com.templar.games.stormrunner.templarutil.gui.SimpleContainer;
import com.templar.games.stormrunner.templarutil.util.OrderedTable;
import moe.evelyn.games.stormrunner.Main;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.ImageObserver;
import java.util.Enumeration;
import java.util.Vector;

public class Renderer
extends ImageContainer
implements FocusListener,
ShroudListener {
    public static int TILEWIDTH = 50;
    public static int TILEHEIGHT = 50;
    public static int WINDOWWIDTH = 480;
    public static int WINDOWHEIGHT = 360;
    public static int BUFFERWIDTH = 500;
    public static int BUFFERHEIGHT = 400;

    //public static final int WINDOWTILEWIDTH = (int)Math.ceil(9.0);
    //public static final int WINDOWTILEHEIGHT = (int)Math.ceil(7.0);

    public static int WINDOWTILEWIDTH = (int)Math.ceil(15.0);
    public static int WINDOWTILEHEIGHT = (int)Math.ceil(11.0);

    protected Scene CurrentScene;
    protected Map map;
    protected Dimension MapSize;
    protected Shroud shroud;
    protected GameState State;
    protected Vector ObjectList;
    protected OrderedTable Layers = new OrderedTable();
    protected boolean Visible = false;
    protected Vector Players = new Vector();
    protected Vector Loopers = new Vector();
    protected Vector CurrentlyLooping = new Vector();
    protected Point Offset;
    protected Point NewOffset;
    protected PhysicalObject FollowingObject;
    protected ImageContainer Surface;
    protected GroundComponent ground;
    protected Image GroundBufferImage;
    protected Graphics GroundBufferGraphics;
    protected boolean Added = false;
    protected Vector VisibleObjects = new Vector();
    protected Component InternalFocus;
    protected KeyHandler kh;
    private Object BlitLock = new Object();

    public Renderer(GameState gameState) {
        this.currentDimensions = Main.frame.getSize();
        this.setBounds(new Dimension(currentDimensions.width,currentDimensions.height));
        this.State = gameState;
        this.Offset = new Point(0, 0);
        this.kh = new KeyHandler();
        this.addKeyListener(this.kh);
        this.addFocusListener(this);
    }

    private ClickDelegator clickDelegator;
    private Dimension currentDimensions;

    public void setBounds(Dimension newDimensions)
    {
        // 500/400
        this.setBufferSize(newDimensions);
        this.Surface = new ImageContainer();
        // 500/400
        this.Surface.setBufferSize(newDimensions);
        // 480/360
        this.Surface.setBounds(0, 0, newDimensions.width, newDimensions.height);

        WINDOWWIDTH = newDimensions.width-20;
        WINDOWHEIGHT = newDimensions.height-40;
        BUFFERWIDTH = newDimensions.width;
        BUFFERHEIGHT = newDimensions.height;

        WINDOWTILEWIDTH = (int)Math.ceil(newDimensions.width/TILEWIDTH);
        WINDOWTILEHEIGHT = (int)Math.ceil(newDimensions.height/TILEHEIGHT);
    }

    public synchronized void redimension(Dimension newDimensions)
    {
        this.currentDimensions = newDimensions;
        this.setSize(newDimensions.width, newDimensions.height);
        setBounds(newDimensions);

        setScene(CurrentScene);
    }

    public void invalidate() {
        super.invalidate();
        this.createBufferImage();
    }

    protected void createBufferImage() {
        if (this.Added && this.getSize().width > 0 && this.getSize().height > 0) {
            if (this.GroundBufferGraphics != null) {
                this.GroundBufferGraphics.dispose();
            }
            if (this.GroundBufferImage != null) {
                this.GroundBufferImage.flush();
            }
            // 500/400
            this.GroundBufferImage = this.createImage(currentDimensions.width, currentDimensions.height);
            this.GroundBufferGraphics = this.GroundBufferImage.getGraphics();
        }
    }

    public void removeNotify() {
        super.removeNotify();
        this.Added = false;
    }

    public void addNotify() {
        super.addNotify();
        this.Added = true;
        if (this.GroundBufferImage == null) {
            this.createBufferImage();
        }
    }

    public void setScene(Scene scene) {
        this.unregisterAll();
        if (this.shroud != null) {
            this.shroud.removeShroudListener(this);
        }
        this.Surface.unregisterAll();
        this.Layers.clear();
        this.CurrentScene = scene;
        this.map = scene.getMap();
        this.MapSize = this.map.getSize();
        this.clickDelegator = new ClickDelegator(this, this.State.getCurrentScene());
        // 480/360
        clickDelegator.setBounds(0, 0, currentDimensions.width-20, currentDimensions.height-40);
        this.add(clickDelegator);
        this.shroud = this.State.getCurrentScene().getShroud();
        // 480/360
        this.shroud.setSize(new Dimension(currentDimensions.width, currentDimensions.height));
        this.shroud.setLocation(0, 0);
        this.shroud.addShroudListener(this);
        this.register(this.shroud);
        Enumeration enumeration = scene.getLayers().elements();
        while (enumeration.hasMoreElements()) {
            this.addLayer((String)enumeration.nextElement());
        }
        this.ObjectList = this.State.getCurrentScene().getObjects();
        Enumeration enumeration2 = this.ObjectList.elements();
        while (enumeration2.hasMoreElements()) {
            PhysicalObject physicalObject = (PhysicalObject)enumeration2.nextElement();
            this.addObject(physicalObject);
        }
        this.ground = new GroundComponent();
        this.ground.setBounds(this.getBounds());
        this.Surface.register(this.ground);
        this.register(this.Surface);
    }

    public void addObject(PhysicalObject physicalObject) {
        if (physicalObject.getLayer().compareTo("") != 0 && physicalObject.getLayer() != null) {
            SimpleContainer simpleContainer = null;
            this.addLayer(physicalObject.getLayer());
            simpleContainer = (SimpleContainer)this.Layers.get(physicalObject.getLayer());
            physicalObject.setLocation(Position.mapToScreen(physicalObject.getPosition()));
            physicalObject.setSize(physicalObject.getShapeSize().width * 50, physicalObject.getShapeSize().height * 50);
            physicalObject.setRenderer(this);
            if (this.isInWindow(physicalObject)) {
                simpleContainer.add(physicalObject);
                if (this.isInWindow(physicalObject)) {
                    this.VisibleObjects.addElement(physicalObject);
                }
            }
        }
    }

    public void removeObject(PhysicalObject physicalObject) {
        int n;
        Vector vector;
        if (this.Loopers.contains(physicalObject)) {
            vector = physicalObject.getLoopList();
            for (n = 0; n < vector.size(); ++n) {
                String string = (String)vector.elementAt(n);
                GameApplet.audio.stop(string, physicalObject);
            }
            this.Loopers.removeElement(physicalObject);
            vector.removeAllElements();
            this.CurrentlyLooping.removeElement(physicalObject);
        }
        if (this.Players.contains(physicalObject)) {
            vector = physicalObject.getPlayList();
            for (n = 0; n < vector.size(); ++n) {
                GameApplet.audio.stop((String)vector.elementAt(n), physicalObject);
            }
            vector.removeAllElements();
            this.Players.removeElement(physicalObject);
        }
        ((SimpleContainer)this.Layers.get(physicalObject.getLayer())).remove(physicalObject);
        this.VisibleObjects.removeElement(physicalObject);
        physicalObject.setRenderer(null);
    }

    private void addLayer(String string) {
        if (!this.Layers.containsKey(string)) {
            if (string.compareTo("Robot") == 0) {
                this.addLayer("Robot Effects");
            }
            SimpleContainer simpleContainer = new SimpleContainer();
            simpleContainer.setSize(this.MapSize.width * 50, this.MapSize.height * 50);
            Point point = Position.mapToScreen(this.getOffset());
            simpleContainer.setLocation(-point.x, -point.y);
            this.Layers.put((Object)string, simpleContainer);
            this.Surface.register(simpleContainer);
            simpleContainer.removeReportingComponentListener(this.Surface);
            if (string.compareTo("Robot") == 0) {
                this.addLayer("Ground Effects");
            }
        }
    }

    private void removeLayer(String string) {
        if (this.Layers.containsKey(string)) {
            SimpleContainer simpleContainer = (SimpleContainer)this.Layers.get(string);
            simpleContainer.removeAll();
            this.Surface.unregister(simpleContainer);
        }
    }

    public boolean isVisible() {
        return this.Visible;
    }

    public void setVisible(boolean bl) {
        Object object;
        PhysicalObject physicalObject;
        this.Visible = bl;
        if (bl) {
            Enumeration enumeration = this.Loopers.elements();
            while (enumeration.hasMoreElements()) {
                PhysicalObject physicalObject2 = (PhysicalObject)enumeration.nextElement();
                if (!this.isInWindow(physicalObject2)) continue;
                this.CurrentlyLooping.addElement(physicalObject2);
                Vector vector = physicalObject2.getLoopList();
                for (int i = 0; i < vector.size(); ++i) {
                    String string = (String)vector.elementAt(i);
                    GameApplet.audio.loop(string, physicalObject2);
                }
            }
            return;
        }
        Enumeration enumeration = this.Players.elements();
        while (enumeration.hasMoreElements()) {
            physicalObject = (PhysicalObject)enumeration.nextElement();
            object = physicalObject.getPlayList();
            for (int i = 0; i < ((Vector)object).size(); ++i) {
                GameApplet.audio.stop((String)((Vector)object).elementAt(i), physicalObject);
            }
            ((Vector)object).removeAllElements();
        }
        this.Players.removeAllElements();
        Enumeration newObject = this.CurrentlyLooping.elements();
        while (newObject.hasMoreElements()) {
            physicalObject = (PhysicalObject)newObject.nextElement();
            Vector vector = physicalObject.getLoopList();
            for (int i = 0; i < vector.size(); ++i) {
                String string = (String)vector.elementAt(i);
                GameApplet.audio.stop(string, physicalObject);
            }
        }
        this.CurrentlyLooping.removeAllElements();
    }

    public Point getOffset() {
        return new Point(this.Offset);
    }

    public void setOffsetToCenter(Point point) {
        int n = Math.round((float)WINDOWTILEWIDTH / 2.0f) - 1;
        int n2 = Math.round((float)WINDOWTILEHEIGHT / 2.0f) - 1;
        Point point2 = new Point(point);
        point2.translate(-n, -n2);
        this.softSetOffset(point2);
    }

    public void softSetOffset(Point point) {
        Point point2 = new Point();
        point2.x = Math.max(Math.min(point.x, this.MapSize.width - WINDOWTILEWIDTH - 1), 0);
        point2.y = Math.max(Math.min(point.y, this.MapSize.height - WINDOWTILEHEIGHT - 1), 0);
        this.setOffset(point2);
    }

    public boolean setOffset(Point point) {
        if (point.x < 0 || point.y < 0 || point.x >= this.MapSize.width - WINDOWTILEWIDTH || point.y >= this.MapSize.height - WINDOWTILEHEIGHT) {
            return false;
        }

        if (!point.equals(NewOffset) ^ FollowingObject!=null) {
            this.invalidate();
        }
        
        this.NewOffset = point;
        this.repaint();
        return true;
    }

    public void refreshBuffer() {
        if (this.NewOffset != null) {
            int n = this.Offset.x - this.NewOffset.x;
            int n2 = this.Offset.y - this.NewOffset.y;
            if (n != 0 || n2 != 0) {
                Cloneable cloneable;
                PhysicalObject physicalObject;
                String string;
                this.Offset = this.NewOffset;
                if (!(this.BufferImage == null || this.isEntireScreenTainted() || this.getBlit() != null || Math.abs(n) >= WINDOWTILEWIDTH && Math.abs(n2) >= WINDOWTILEHEIGHT)) {
                    if (this.isBufferTainted()) {
                        this.translateTaintArea(n * 50, n2 * 50);
                        this.Surface.translateTaintArea(n * 50, n2 * 50);
                    }
                    this.setBlit(n * 50, n2 * 50);
                    this.Surface.setBlit(n * 50, n2 * 50);
                    this.ground.setGroundBlit(n * 50, n2 * 50);
                    int n3 = n < 0 ? (WINDOWTILEWIDTH + n + 1) * 50 : 0;
                    int n4 = Math.abs(n) * 50;
                    cloneable = new Rectangle(n3, 0, n4, 400);
                    int n5 = n2 < 0 ? (WINDOWTILEHEIGHT + n2 + 1) * 50 : 0;
                    int n6 = Math.abs(n2) * 50;
                    Rectangle rectangle = new Rectangle(0, n5, 500, n6);
                    if (n != 0 && n2 != 0) {
                        ((Rectangle)cloneable).add(rectangle);
                    } else if (n2 != 0) {
                        cloneable = rectangle;
                    }
                    this.ground.taintGround((Rectangle)cloneable);
                    this.Surface.taintBuffer((Rectangle)cloneable);
                } else {
                    this.ground.taintGround();
                    this.Surface.taintBuffer();
                }
                Enumeration enumeration = this.Layers.elements();
                while (enumeration.hasMoreElements()) {
                    ((Container)enumeration.nextElement()).setLocation(-this.Offset.x * 50, -this.Offset.y * 50);
                }
                this.shroud.setOffset(this.Offset);
                Vector<PhysicalObject> vector = new Vector<PhysicalObject>();
                Vector newCloneable = this.CurrentScene.getObjects();
                for (int i = 0; i < newCloneable.size(); ++i) {
                    PhysicalObject physicalObject2 = (PhysicalObject)newCloneable.elementAt(i);
                    SimpleContainer simpleContainer = (SimpleContainer)this.Layers.get(physicalObject2.getLayer());
                    if (this.isInWindow(physicalObject2)) {
                        if (!this.VisibleObjects.contains(physicalObject2)) {
                            simpleContainer.add(physicalObject2);
                        }
                        vector.addElement(physicalObject2);
                        continue;
                    }
                    if (!this.VisibleObjects.contains(physicalObject2)) continue;
                    simpleContainer.remove(physicalObject2);
                }
                this.VisibleObjects = vector;
                Enumeration enumeration2 = this.CurrentlyLooping.elements();
                Vector<PhysicalObject> vector2 = new Vector<PhysicalObject>();
                while (enumeration2.hasMoreElements()) {
                    physicalObject = (PhysicalObject)enumeration2.nextElement();
                    if (this.isInWindow(physicalObject)) continue;
                    Vector vector3 = physicalObject.getLoopList();
                    for (int i = 0; i < vector3.size(); ++i) {
                        string = (String)vector3.elementAt(i);
                        GameApplet.audio.stop(string, physicalObject);
                    }
                    vector2.addElement(physicalObject);
                }
                for (int i = 0; i < vector2.size(); ++i) {
                    this.CurrentlyLooping.removeElement(vector2.elementAt(i));
                }
                enumeration2 = this.Loopers.elements();
                while (enumeration2.hasMoreElements()) {
                    physicalObject = (PhysicalObject)enumeration2.nextElement();
                    if (!this.isInWindow(physicalObject) || this.CurrentlyLooping.contains(physicalObject)) continue;
                    this.CurrentlyLooping.addElement(physicalObject);
                    Vector vector4 = physicalObject.getLoopList();
                    for (int i = 0; i < vector4.size(); ++i) {
                        string = (String)vector4.elementAt(i);
                        GameApplet.audio.loop(string, physicalObject);
                    }
                }
            }
            this.NewOffset = null;
        }
        super.refreshBuffer();
    }

    public void setObjectToFollow(PhysicalObject physicalObject) {
        Position position;
        this.FollowingObject = physicalObject;
        if (physicalObject != null && (position = physicalObject.getPosition()) != null) {
            Point point = position.getMapPoint();
            this.setOffsetToCenter(point);
        }
    }

    public PhysicalObject getObjectToFollow() {
        return this.FollowingObject;
    }

    public void reportNewPosition(PhysicalObject physicalObject) {
        if (this.Visible) {
            if (this.FollowingObject == physicalObject) {
                this.setOffsetToCenter(physicalObject.getPosition().getMapPoint());
            }
            if (this.isInWindow(physicalObject)) {
                if (!this.VisibleObjects.contains(physicalObject)) {
                    Vector vector = physicalObject.getLoopList();
                    if (vector.size() > 0 && !this.CurrentlyLooping.contains(physicalObject)) {
                        this.CurrentlyLooping.addElement(physicalObject);
                        for (int i = 0; i < vector.size(); ++i) {
                            String string = (String)vector.elementAt(i);
                            GameApplet.audio.loop(string, physicalObject);
                        }
                    }
                    this.addObject(physicalObject);
                    this.VisibleObjects.addElement(physicalObject);
                    return;
                }
            } else if (this.VisibleObjects.contains(physicalObject)) {
                if (this.CurrentlyLooping.contains(physicalObject)) {
                    Vector vector = physicalObject.getLoopList();
                    for (int i = 0; i < vector.size(); ++i) {
                        String string = (String)vector.elementAt(i);
                        GameApplet.audio.stop(string, physicalObject);
                    }
                    this.CurrentlyLooping.removeElement(physicalObject);
                }
                ((SimpleContainer)this.Layers.get(physicalObject.getLayer())).remove(physicalObject);
                this.VisibleObjects.removeElement(physicalObject);
            }
        }
    }

    public void shroudChanged(ShroudEvent shroudEvent) {
        Rectangle rectangle = shroudEvent.getAffectedArea();
        if (rectangle.intersects(new Rectangle(this.Offset.x, this.Offset.y, WINDOWTILEWIDTH, WINDOWTILEHEIGHT))) {
            rectangle.setLocation(rectangle.x * 50 - this.Offset.x * 50, rectangle.y * 50 - this.Offset.y * 50);
            rectangle.setSize(rectangle.width * 50, rectangle.height * 50);
            this.taintBuffer(rectangle);
            this.repaint(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }
    }

    public void soundStopped(PhysicalObject physicalObject, int n) {
        if (physicalObject.getPlayList().size() == 0) {
            this.Players.removeElement(physicalObject);
        }
    }

    public void playSound(PhysicalObject physicalObject, String string) {
        if (this.isInWindow(physicalObject) && this.Visible) {
            this.Players.addElement(physicalObject);
            GameApplet.audio.play(string, physicalObject);
        }
    }

    public void loopSound(PhysicalObject physicalObject, String string) {
        this.Loopers.addElement(physicalObject);
        if (this.isInWindow(physicalObject) && this.Visible) {
            this.CurrentlyLooping.addElement(physicalObject);
            GameApplet.audio.loop(string, physicalObject);
        }
    }

    public void stopSound(PhysicalObject physicalObject, String string) {
        if (physicalObject.getLoopList().size() == 0) {
            this.CurrentlyLooping.removeElement(physicalObject);
            this.Loopers.removeElement(physicalObject);
        }
        if (physicalObject.getPlayList().size() == 0) {
            this.Players.removeElement(physicalObject);
        }
        GameApplet.audio.stop(string, physicalObject);
    }

    public boolean isInWindow(PhysicalObject physicalObject) {
        boolean bl = false;
        Rectangle rectangle = new Rectangle(this.Offset.x, this.Offset.y, WINDOWTILEWIDTH + 1, WINDOWTILEHEIGHT + 1);
        Position position = physicalObject.getPosition();
        Dimension dimension = physicalObject.getShapeSize();
        Rectangle rectangle2 = new Rectangle(position.x, position.y, dimension.width, dimension.height);
        bl = rectangle2.intersects(rectangle);
        return bl;
    }

    public Dimension getSize() {
        // 480/360
        return new Dimension(currentDimensions.width, currentDimensions.height);
    }

    public Dimension getMinimumSize() {
        return this.getSize();
    }

    public Dimension getPreferredSize() {
        return this.getSize();
    }

    public Dimension getMaximumSize() {
        return this.getSize();
    }

    public Component getInternalFocus() {
        return this.InternalFocus;
    }

    public void setInternalFocus(Component component) {
        this.InternalFocus = component;
        if (component != null) {
            component.requestFocus();
            return;
        }
        this.requestFocus();
    }

    public void focusGained(FocusEvent focusEvent) {
        if (this.InternalFocus != null) {
            this.InternalFocus.requestFocus();
        }
    }

    public void focusLost(FocusEvent focusEvent) {
    }

    public void setGameState(GameState gameState) {
        this.State = gameState;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer(this.getClass().getName());
        stringBuffer.append("@");
        stringBuffer.append(this.hashCode());
        stringBuffer.append("\n");
        stringBuffer.append(super.toString());
        return stringBuffer.toString();
    }

    public OrderedTable getLayers() {
        return this.Layers;
    }
protected class GroundComponent
extends Component {
        // 480/360
    //private final Rectangle DEFAULT_GROUND_TAINT = new Rectangle(0, 0, 780, 560);
    private boolean GroundBufferTainted = true;
    private Rectangle GroundTaintArea = getDefaultTaint();
    private Point GroundBlit;

    private Rectangle getDefaultTaint() {
        return new Rectangle(0,0, Renderer.this.currentDimensions.width, Renderer.this.currentDimensions.height);
    }

    public void taintGround() {
        this.taintGround(this.getDefaultTaint());
    }

    public void taintGround(Rectangle rectangle) {
        Rectangle rectangle2 = new Rectangle(rectangle);
        this.GroundBufferTainted = true;
        if (this.GroundTaintArea == null) {
            this.GroundTaintArea = rectangle2;
            return;
        }
        this.GroundTaintArea.add(rectangle2);
    }

    public void setGroundBlit(int n, int n2) {
        this.GroundBlit = new Point(n, n2);
    }

    public Point getGroundBlit() {
        return this.GroundBlit;
    }

    public void update(Graphics graphics) {
        this.paint(graphics);
    }

    public void paint(Graphics graphics) {
        if (this.GroundBufferTainted) {
            if (this.GroundTaintArea == null) {
                this.GroundTaintArea = this.getDefaultTaint();
            }
            if (this.GroundBlit != null) {
                Rectangle rectangle = Renderer.this.GroundBufferGraphics.getClipBounds();
                // 500/400
                Renderer.this.GroundBufferGraphics.setClip(0, 0, Renderer.this.currentDimensions.width, Renderer.this.currentDimensions.height);
                // 500/400
                Renderer.this.GroundBufferGraphics.copyArea(0, 0, Renderer.this.currentDimensions.width, Renderer.this.currentDimensions.height, this.GroundBlit.x, this.GroundBlit.y);
                Renderer.this.GroundBufferGraphics.setClip(rectangle);
                this.GroundBlit = null;
            }
            int n = Renderer.this.Offset.x + (int)Math.floor((float)this.GroundTaintArea.x / 50.0f);
            int n2 = n + (int)Math.ceil((float)this.GroundTaintArea.width / 50.0f);
            int n3 = Renderer.this.Offset.y + (int)Math.floor((float)this.GroundTaintArea.y / 50.0f);
            int n4 = n3 + (int)Math.ceil((float)this.GroundTaintArea.height / 50.0f);
            n2 = Math.min(Renderer.this.MapSize.width - 1, n2);
            n4 = Math.min(Renderer.this.MapSize.height - 1, n4);
            int n5 = (n - Renderer.this.Offset.x) * 50;
            int n6 = (n3 - Renderer.this.Offset.y) * 50;
            int n7 = n3;
            while (n7 <= n4) {
                int n8 = n;
                while (n8 <= n2) {
                    Image image = Renderer.this.map.getCell(n8, n7).getAppearance();
                    GameApplet.thisApplet.hitCache(image);
                    Renderer.this.GroundBufferGraphics.drawImage(image, n5, n6, null);
                    ++n8;
                    n5 += 50;
                }
                n5 = (n - Renderer.this.Offset.x) * 50;
                ++n7;
                n6 += 50;
            }
            this.GroundBufferTainted = false;
            this.GroundTaintArea = null;
        }
        graphics.drawImage(Renderer.this.GroundBufferImage, 0, 0, null);
    }

    protected GroundComponent() {
        // wat
        // Renderer.this = Renderer.this;
    }
}
protected class KeyHandler
extends KeyAdapter {
    public void keyPressed(KeyEvent keyEvent) {
        Point point = new Point(Renderer.this.getOffset());
        int n = 1;
        switch (keyEvent.getKeyCode()) {
            case 36: {
                Renderer.this.setObjectToFollow(null);
                point.translate(-n, -n);
                Renderer.this.setOffset(point);
                break;
            }
            case 35: {
                Renderer.this.setObjectToFollow(null);
                point.translate(-n, n);
                Renderer.this.setOffset(point);
                return;
            }
            case 33: {
                Renderer.this.setObjectToFollow(null);
                point.translate(n, -n);
                Renderer.this.setOffset(point);
                break;
            }
            case 34: {
                Renderer.this.setObjectToFollow(null);
                point.translate(n, n);
                Renderer.this.setOffset(point);
                break;
            }
            case 40: {
                Renderer.this.setObjectToFollow(null);
                point.translate(0, n);
                Renderer.this.setOffset(point);
                break;
            }
            case 38: {
                Renderer.this.setObjectToFollow(null);
                point.translate(0, -n);
                Renderer.this.setOffset(point);
                break;
            }
            case 37: {
                Renderer.this.setObjectToFollow(null);
                point.translate(-n, 0);
                Renderer.this.setOffset(point);
                break;
            }
            case 39: {
                Renderer.this.setObjectToFollow(null);
                point.translate(n, 0);
                Renderer.this.setOffset(point);
                break;
            }
            case 32: {
                if (Renderer.this.State == null) break;
                Renderer.this.setObjectToFollow(Renderer.this.State.getCurrentRobot());
                break;
            }
            case 71: {
                GameApplet gameApplet = GameApplet.thisApplet;
                if (gameApplet.getGrid().isOn()) {
                    gameApplet.getGrid().burnGridOff();
                    break;
                }
                gameApplet.getGrid().burnGridOn();
                break;
            }
        }
    }

    protected KeyHandler() {
        // wat
        // Renderer.this = Renderer.this;
    }
}

}
