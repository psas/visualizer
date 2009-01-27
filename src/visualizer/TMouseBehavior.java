package visualizer;

/*
 *      TMouseBehavior.java       2001, 2002 Martin Barbisch
 *
 *      mouse navigation (and region selection)
 *
 *      Studienarbeit at the University of Stuttgart, VIS group
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class TMouseBehavior extends Behavior
{
	private final Vector3f STD_DIR = new Vector3f(0.0f, 0.0f, -1.0f);
	private final Vector3d NULL_VECT = new Vector3d();

	private TransformGroup ViewTG = null; // TG to be modified
	private WakeupCriterion[] MouseEvents = new WakeupCriterion[4];
	private WakeupOr MouseCriterion;
	private Transform3D ViewT3D = new Transform3D(); // view transform
	private Transform3D TransformX = new Transform3D(); // x-axis multiplier
	private Transform3D TransformY = new Transform3D(); // y-axis multiplier
	private Vector3d VectBak = new Vector3d(); // save translation
	private int XLast = -1, YLast = -1; // keep track of movement
	private float Factor = 0.008f; // specifies x- and y-dir.
	private boolean ForceT3DRead = true; // view dir. changed externally
	private boolean Invert = false; // invert mouse up/down

	public TMouseBehavior()
	{
		super();
	}

	public void init(TransformGroup tg, float factor, boolean inv)
	{
		ViewTG = tg;
		Factor = factor;
		Invert = inv;
	}

	public void initialize()
	{
		MouseEvents[0] = new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED);
		MouseEvents[1] = new WakeupOnAWTEvent(MouseEvent.MOUSE_MOVED);
		MouseEvents[2] = new WakeupOnAWTEvent(MouseEvent.MOUSE_PRESSED);
		MouseEvents[3] = new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED);

		MouseCriterion = new WakeupOr(MouseEvents);
		wakeupOn(MouseCriterion);
	}

	public void processStimulus(Enumeration criteria)
	{
		WakeupCriterion wakeup;
		AWTEvent[] events;
		MouseEvent evt;
		while (criteria.hasMoreElements())
		{
			wakeup = (WakeupCriterion) criteria.nextElement();
			if (wakeup instanceof WakeupOnAWTEvent)
			{
				events = ((WakeupOnAWTEvent) wakeup).getAWTEvent();
				if (events.length > 0)
				{
					evt = (MouseEvent) events[events.length - 1];
					doProcess(evt);
				}
			}
		}
		wakeupOn(MouseCriterion);
	}

	private void doProcess(MouseEvent evt)
	{
		int id = evt.getID();
		if ((ViewTG != null) && (id == MouseEvent.MOUSE_DRAGGED))
		{
			// --- get coordinates -----------------

			int x = evt.getX();
			int y = evt.getY();

			// --- prevent view jump ---------------

			if (XLast == -1)
			{
				XLast = x;
			}
			if (YLast == -1)
			{
				YLast = y;
			}

			// --- set angles ----------------------

			int dx = (x - XLast);
			int dy = (y - YLast);

			float xAngle = (float) dx * Factor;
			float yAngle = (float) dy * Factor;

			if (Invert)
			{
				yAngle = -yAngle;
			}

			TransformX.rotX(-yAngle);
			TransformY.rotY(-xAngle);

			// --- set new transformation ----------

			ViewTG.getTransform(ViewT3D);

			ViewT3D.get(VectBak);

			ViewT3D.setTranslation(NULL_VECT);

			ViewT3D.mul(TransformX);
			ViewT3D.mul(TransformY);

			ViewT3D.setTranslation(VectBak);

			// --- save coordinates ----------------

			XLast = x;
			YLast = y;
		} else if ((ViewTG != null) && (id == MouseEvent.MOUSE_RELEASED))
		{

			XLast = YLast = -1;
		}
	}

	public void getDir(Vector3f v3f)
	{
		if (ForceT3DRead)
		{

			ViewTG.getTransform(ViewT3D);
			ForceT3DRead = false;
		}
		ViewT3D.transform(STD_DIR, v3f);
	}

	public void forceT3DRead()
	{
		ForceT3DRead = true;
	}

	public void toggleInvert()
	{
		Invert = (Invert) ? false : true;
	}
}
