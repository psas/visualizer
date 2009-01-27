package visualizer;
/*
 *      TEachFrameBehavior.java       2001, 2002 Martin Barbisch
 *
 *      update Terrain
 *
 *      Studienarbeit at the University of Stuttgart, VIS group
 */

import java.util.Enumeration;
import javax.media.j3d.*;
import javax.vecmath.*;

public class TEachFrameBehavior extends Behavior {

        private final Vector3d UP_D = new Vector3d(0, 1, 0);    // up vector
        private final Vector3f UP_F = new Vector3f(0, 1, 0);    // up vector

        private Terrain        TheTerrain;                      // ref to Terrain
        private TransformGroup ViewTG;                          // ref to viewer's TG
        private TKeyBehavior   KeyBeh;                          // ref. to get velocity
        private TMouseBehavior MouseBeh;                        // ref. to get view direction

        private WakeupOnElapsedFrames WOEF                      // wake up each frame
                = new WakeupOnElapsedFrames(0);

        private Transform3D T3D      = new Transform3D();       // viewer's T3D
        private Transform3D LastT3D  = new Transform3D();       // viewer's last T3D
        private Transform3D InitPos  = new Transform3D();       // for view reset
        private Transform3D Rotation = new Transform3D();       // for rotation (auto pilot)

        private Vector3f Pos    = new Vector3f();               // to get viewer's position
        private Vector3f Dir    = new Vector3f();               // to get viewing direction
        private Vector3f Str    = new Vector3f();               // to calc strafe direction

        private Point3d  Eye    = new Point3d();                // parameters for
        private Point3d  Center = new Point3d();                // T3D.lookAt

        private boolean OnceAgain  = true;                      // to prevent jagged edges
        private long    LastUpdate = 0;                         // time of last update

        TEachFrameBehavior(Terrain t, TransformGroup tg, TKeyBehavior kb, TMouseBehavior mb) {

               //--- set references -------------------------------------------
                TheTerrain = t;
                ViewTG     = tg;
                KeyBeh     = kb;
                MouseBeh   = mb;

               //--- create InitPos T3D ---------------------------------------
                InitPos.lookAt(new Point3d(15, 15, 15), new Point3d(), UP_D);
                InitPos.invert();
        }

        public void initialize() {
                LastUpdate = System.currentTimeMillis();
                wakeupOn(WOEF);         // setting initial wakeup condition
        }

        public void processStimulus(Enumeration criteria) {
               //--- change mouse look up/down? ------------------------------------------
                if (KeyBeh.isInvertDemanded()) {

                        MouseBeh.toggleInvert();
                }

               //--- check whether a view reset is demanded ------------------------------
                if (KeyBeh.isResetDemanded()) {

                        T3D.set(InitPos);
                        LastT3D.setZero();

                        MouseBeh.forceT3DRead();

                        ViewTG.setTransform(T3D);
                }
                else {

                       //--- calc. new pos. and view.dir. --------------------------------
                        ViewTG.getTransform(T3D);
                        T3D.get(Pos);

                        long  now    = System.currentTimeMillis();
                        float tFact  = (now - LastUpdate) / 1000.0f;
                        float speed  = KeyBeh.getSpeed()  * tFact;
                        float strafe = KeyBeh.getStrafe() * tFact;

                        if (KeyBeh.isPilot()) {

                               //--- auto pilot ON ------------------------------------
                                MouseBeh.forceT3DRead();
                                MouseBeh.getDir(Dir);

                                Str.cross(Dir, UP_F);

                                Rotation.rotY((0.5f + strafe*10.0f) * tFact);

                                Eye.set(Pos.x, Pos.y, Pos.z);
                                Rotation.transform(Eye);

                                Center.set(0, 0, 0);
                                MouseBeh.forceT3DRead();
                        }
                        else {
                               //--- auto pilot OFF -----------------------------------
                                MouseBeh.getDir(Dir);

                                Str.cross(Dir, UP_F);

                                Eye.   set(Pos.x + Dir.x * speed + Str.x * strafe,
                                           Pos.y + Dir.y * speed + Str.y * strafe,
                                           Pos.z + Dir.z * speed + Str.z * strafe );
                                Center.set(Pos.x + Dir.x * 1000.0f,
                                           Pos.y + Dir.y * 1000.0f,
                                           Pos.z + Dir.z * 1000.0f);
                        }

                        T3D.lookAt(Eye, Center, UP_D);
                        T3D.invert();

                        LastUpdate = now;
                }

               //--- update Terrain/view (maybe) -----------------------------------------
                if (!T3D.equals(LastT3D)) {

                        TheTerrain.updateTerrain();
                        ViewTG.setTransform(T3D);

                        LastT3D.set(T3D);

                        OnceAgain = true;
                }
                else if (OnceAgain) {

                        TheTerrain.updateTerrain();

                        OnceAgain = false;
                }

                wakeupOn(WOEF);
        }
}
