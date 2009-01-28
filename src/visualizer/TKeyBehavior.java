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
import javax.vecmath.Vector3d;

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

	private Terrain TheTerrain;
	private final TransformGroup TheRocket;
	private final TransformGroup TheCamera;
	private final Timer t = new Timer(100, null);

	private WakeupCondition KeyCriterion;

	private float Speed = 0.0f; // current speed
	private float Strafe = 0.0f; // strafing speed (left/right)
	private float SpeedInc = 0.02f; // speed increase
	private boolean Reset = false; // true, if view reset demanded
	private boolean Invert = false; // true, if invert view demanded
	private boolean Pilot = false; // use auto pilot?
	private boolean Flying = false;

	public TKeyBehavior(Terrain terrain, float speedInc, TransformGroup b, TransformGroup c)
	{
		TheTerrain = terrain;
		SpeedInc = speedInc;
		TheRocket = b;
		TheCamera = c;
	}

	public void initialize()
	{
		WakeupCriterion[] keyEvents = new WakeupCriterion[2];

		keyEvents[0] = new WakeupOnAWTEvent(KeyEvent.KEY_PRESSED);
		keyEvents[1] = new WakeupOnAWTEvent(KeyEvent.KEY_RELEASED);
		KeyCriterion = new WakeupOr(keyEvents);

		wakeupOn(KeyCriterion);
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
		else if (keycode == LAUNCH)
		{
			final Transform3D trans = new Transform3D();
			final Transform3D objectTrans = new Transform3D();
			Transform3D first = new Transform3D();
			TheCamera.getTransform(first);
			final Vector3d vec = new Vector3d();
			first.get(vec);
			t.setRepeats(false);
			t.addActionListener(new ActionListener()
			{
				FlightPattern fp = new SpiralFlightPattern();
				double[] xcoords = fp.getXCoords();
				double[] ycoords = fp.getYCoords();
				double[] zcoords = fp.getZCoords();
				int i = 0;
				public void actionPerformed(ActionEvent ae)
				{
					Flying = true;
					try
					{
						if(i < xcoords.length)
						{
							float x = (float) xcoords[i];
							float y = (float) ycoords[i];
							float z = (float) zcoords[i];
							x /= 10.0;
							y /= 10.0;
							z /= 10.0;
							float lx = (float) vec.x;
							float ly = (float) vec.y;
							float lz = (float) vec.z;
							trans.setTranslation(new Vector3d(lx + x, ly + y, lz + z));
							TheCamera.setTransform(trans);

							TheRocket.getTransform(objectTrans);
							objectTrans.setTranslation(new Vector3d(x, y, z));
							TheRocket.setTransform(objectTrans);
							i++;
							t.restart();
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});
			if(!Flying)
				t.start();
			else
			{
				t.stop();
				Flying = false;
			}
		}
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
