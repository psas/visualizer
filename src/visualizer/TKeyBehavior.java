package visualizer;

/*
 *      TKeyBehavior.java       2001, 2002 Martin Barbisch
 *
 *      recognizes several keys
 *
 *      Studienarbeit at the University of Stuttgart, VIS group
 */

import java.awt.AWTEvent;
import java.awt.event.*;
import java.util.Enumeration;

import javax.media.j3d.*;
import javax.swing.Timer;
import javax.vecmath.*;

public class TKeyBehavior extends Behavior
{
	private static final int FORWARD = KeyEvent.VK_W;
	private static final int BACK = KeyEvent.VK_S;
	private static final int LEFT = KeyEvent.VK_A;
	private static final int RIGHT = KeyEvent.VK_D;
	private static final int STOP = KeyEvent.VK_SPACE;
	private static final int RESET = KeyEvent.VK_R;
	private static final int INVERT = KeyEvent.VK_I;
	private static final int PILOT = KeyEvent.VK_P;

	private static final int GEO_M = KeyEvent.VK_G;
	private static final int FILLED = KeyEvent.VK_1;
	private static final int WIRE_F = KeyEvent.VK_2;
	private static final int MINUS = KeyEvent.VK_3;
	private static final int PLUS = KeyEvent.VK_4;
	private static final int LAUNCH = KeyEvent.VK_ENTER;
	private static final int SPEED_INCREASE = KeyEvent.VK_X;
	private static final int SPEED_DECREASE = KeyEvent.VK_Z;

	private Terrain TheTerrain;
	private FlightPattern fp = null;
	private final Timer t;
	private final TransformGroup TheRocket;
	private final TransformGroup TheCamera;
	private final Visualizer visualizer;

	private WakeupCondition KeyCriterion;
	long start = 0;
	long stop = 0;
	int speed = 0;

	private float Speed = 0.0f; // current speed
	private float Strafe = 0.0f; // strafing speed (left/right)
	private float SpeedInc = 0.02f; // speed increase
	private boolean Reset = false; // true, if view reset demanded
	private boolean Invert = false; // true, if invert view demanded
	private boolean Pilot = false; // use auto pilot?
	private boolean Flying = false;

	public TKeyBehavior(Terrain terrain, float speedInc, TransformGroup rocket,
			TransformGroup camera, String pattern, int delay, Visualizer visualizer)
	{
		TheTerrain = terrain;
		SpeedInc = speedInc;
		TheCamera = camera;
		TheRocket = rocket;
		this.visualizer = visualizer;
		t = new Timer(delay, null);
		if(pattern.equals("Spiral"))
			fp = new SpiralFlightPattern();
		else if(pattern.equals("Arc"))
			fp = new ArcFlightPattern();
		else
			fp = new LineFlightPattern();
	}

	public void initialize()
	{
		WakeupCriterion[] keyEvents = new WakeupCriterion[2];

		keyEvents[0] = new WakeupOnAWTEvent(KeyEvent.KEY_PRESSED);
		keyEvents[1] = new WakeupOnAWTEvent(KeyEvent.KEY_RELEASED);
		KeyCriterion = new WakeupOr(keyEvents);

		wakeupOn(KeyCriterion);
		Transform3D first = new Transform3D();
		TheCamera.getTransform(first);
		final Vector3d vec = new Vector3d();
		first.get(vec);
		t.setRepeats(false);
		t.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				Flying = true;
				double[] points = fp.getNewCoords((System.currentTimeMillis()-start)*speed);
				if(points == null)
				{
					Flying = false;
					return;
				}
				float x = (float) points[0];
				float y = (float) points[1];
				float z = (float) points[2];
				Transform3D trans = new Transform3D();
				float lx = (float) vec.x;
				float ly = (float) vec.y;
				float lz = (float) vec.z;
				trans.lookAt(new Point3d(lx + x, ly + y, lz + z), new Point3d(x, y, z), new Vector3d(0, 1, 0));
				trans.invert();
				TheCamera.setTransform(trans);

				Transform3D objectTrans = new Transform3D();
				objectTrans.setTranslation(new Vector3d(x, y, z));
				TheRocket.setTransform(objectTrans);
				t.restart();
			}
		});
	}

	public void processStimulus(Enumeration criteria)
	{
		WakeupCriterion wakeup;
		AWTEvent[] event;
		while (criteria.hasMoreElements())
		{
			wakeup = (WakeupCriterion) criteria.nextElement();
			if (!(wakeup instanceof WakeupOnAWTEvent))
			{
				continue;
			}
			event = ((WakeupOnAWTEvent) wakeup).getAWTEvent();
			for (int i = 0; i < event.length; i++)
			{

				if (event[i].getID() == KeyEvent.KEY_PRESSED)
				{

					processKeyEvent((KeyEvent) event[i]);
				}
			}
		}
		wakeupOn(KeyCriterion);
	}

	private void processKeyEvent(KeyEvent event)
	{
		int keycode = event.getKeyCode();
		if (keycode == FORWARD) {	Speed += SpeedInc;	}
		else if (keycode == BACK) {	Speed -= SpeedInc;	}
		else if (keycode == LEFT) {	Strafe -= SpeedInc;	}
		else if (keycode == RIGHT) {	Strafe += SpeedInc;	}
		else if (keycode == STOP) {	Speed = 0.0f;	Strafe = 0.0f;	Pilot = false;	}
		else if (keycode == INVERT) {	Invert = true;		}
		else if (keycode == RESET) {	Speed = 0.0f;	Strafe = 0.0f;	Reset = true;	}
		else if (keycode == PILOT) {
			if (Pilot)
			{
				Pilot = false;
				Speed = 0.0f;
				Strafe = 0.0f;
			} else
			{
				Pilot = true;
			}
		}
		else if (keycode == GEO_M) {	TheTerrain.toggleGeoMorphing();	}
		else if (keycode == MINUS) {	TheTerrain.lessDetail(true);	}
		else if (keycode == PLUS) {	TheTerrain.moreDetail(true);	}
		else if (keycode == FILLED) {	TheTerrain.setFilledPolys(false); }
		else if (keycode == WIRE_F) {	TheTerrain.setFilledPolys(true); }
		else if (keycode == SPEED_INCREASE) {
			if(speed == -1)
				stopRocket();
			else if(speed == 0)
				startRocket(1);
			else
				moveRocket(1);
		}
		else if (keycode == SPEED_DECREASE) {
			if(speed == 1)
				stopRocket();
			else if(speed == 0)
				startRocket(-1);
			else
				moveRocket(-1);
		}
	}

	private void moveRocket(int i)
	{
		int new_speed = speed + i;
		start = System.currentTimeMillis() - (System.currentTimeMillis() - start) * speed / new_speed;
		speed = new_speed;
		visualizer.setSpeed(String.valueOf(speed));
	}

	private void stopRocket()
	{
		stop = System.currentTimeMillis();
		t.stop();
		speed = 0;
		visualizer.setSpeed(String.valueOf(speed));
		Flying = false;
	}

	private void startRocket(int i)
	{
		start += System.currentTimeMillis()-stop;
		stop = 0;
		start = System.currentTimeMillis() - (System.currentTimeMillis() - start) * speed / i;
		speed = i;
		t.start();
		visualizer.setSpeed(String.valueOf(speed));
	}

	public boolean isResetDemanded()
	{
		if (Reset)
		{
			Reset = false;
			return true;
		} else
			return false;
	}

	public boolean isInvertDemanded()
	{
		if (Invert)
		{
			Invert = false;
			return true;
		} else
			return false;
	}

	public boolean isPilot() {	return Pilot;	}
	public float getSpeed() {	return Speed;	}
	public float getStrafe() {	return Strafe;	}
}
