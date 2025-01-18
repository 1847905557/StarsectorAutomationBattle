/*
 * By Tartiflette
 * Enable a free camera navigation of the battlespace as well as some limited time controls
 */
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.util.List;

public class AI_freeCamPlugin extends BaseEveryFrameCombatPlugin {

	public static final String ID = "freeCam";
	public static final float DEFAULT_TIME_MULT = 1.5f;
	public static final float BOOST_TIME_MULT = 4f;
	public static final float STATIC_TIME_MULT = 1f;

	private final Logger log = Global.getLogger(AI_freeCamPlugin.class);
	private CombatEngineAPI engine = null;

	private int freeCam = 0;
	private boolean camToggle = false, zoomIn = false, zoomOut = false, message = false;
	private float mapX, mapY, screenX, screenY, scale = 5, zoomX = 0, zoomY = 0;
	private Vector2f target = new Vector2f();

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;

		screenX = Global.getSettings().getScreenWidth();
		screenY = Global.getSettings().getScreenHeight();

		zoomX = screenX;
		zoomY = screenY;

		mapX = engine.getMapWidth();
		mapY = engine.getMapHeight();

		freeCam = 0;
	}

	@Override
	public void advance(float amount, List<InputEventAPI> events) {

		if (engine == null) return;

		ShipAPI player = engine.getPlayerShip();
		if (player.isAlive() && !player.isShuttlePod()) {
			freeCam = 0;
			engine.getViewport().setExternalControl(false);
			engine.getTimeMult().modifyMult(ID, STATIC_TIME_MULT);
			return;
		}

		////////////////////////////////////
		//                                //
		//           TIME TOOLS           //
		//                                //
		////////////////////////////////////

		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			if (!message) {
				message = true;
				engine.getCombatUI().addMessage(0, "Engaged x4 speed-up.");
			}
			engine.getTimeMult().modifyMult(ID, BOOST_TIME_MULT);
		} else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
			if (!message) {
				message = true;
				engine.getCombatUI().addMessage(0, "Engaged normal time flow.");
			}
			engine.getTimeMult().modifyMult(ID, STATIC_TIME_MULT);
		} else {
			if (message) {
				message = false;
				engine.getCombatUI().addMessage(0, "Engaged x1.5 speed-up.");
			}
			engine.getTimeMult().modifyMult(ID, DEFAULT_TIME_MULT);
		}

		////////////////////////////////////
		//                                //
		//          FREE CAMERA           //
		//                                //
		////////////////////////////////////

		if (Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
			camToggle = true;
		} else if (camToggle) {
			camToggle = false;

			freeCam++;

			switch (freeCam) {
				case 1:
					log.info("CURSOR FREECAM MODE");
					break;
				case 2:
					log.info("SCREENSPACE FREECAM MODE");
					break;
				case 3:
					freeCam = 1;
					log.info("CURSOR FREECAM MODE");
					break;
				default:
					break;
			}

			engine.getViewport().setExternalControl(freeCam > 0);
			target = new Vector2f();
			scale = mapY / screenY;
			zoomX = engine.getViewport().getVisibleWidth();
			zoomY = engine.getViewport().getVisibleHeight();

			log.info("Mouse mult: " + scale);
			log.info("Screen size: " + screenX + " x " + screenY);
			log.info("Battle size " + mapX + " x " + mapY);

			//display toggle type
			MagicRender.screenspace(Global.getSettings().getSprite("misc", "AI_cam" + freeCam), MagicRender.positioning.CENTER, new Vector2f(0, (screenY / 2) - 48), new Vector2f(), new Vector2f(96, 48), new Vector2f(), 0, 0, Color.WHITE, false, 0.1f, 1.3f, 0.1f);
		}

		if (freeCam == 2) {
			for (InputEventAPI i : events) {

				if (i.isRMBDownEvent()) {
					// log.info("Right-click down");
					zoomOut = true;
				}

				if (i.isRMBUpEvent()) {
					// log.info("Right-click up.");
					zoomOut = false;
				}

				if (i.isLMBDownEvent()) {
					// log.info("Left-click down");
					zoomIn = true;
				}

				if (i.isLMBUpEvent()) {
					// log.info("Left-click up.");
					zoomIn = false;
				}

				if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && i.isAltDown()) {
					freeCam = 0;
					camToggle = true;
					// log.info("Reset cam.");
				}

				if (i.isMouseMoveEvent()) {
					target = new Vector2f(i.getX() - (screenX / 2), i.getY() - (screenY / 2));
					target.scale(scale);
				}
			}

			Vector2f move = new Vector2f(engine.getViewport().getCenter());
			Vector2f.sub(target, move, move);
			move.scale(amount * 7);
			Vector2f.add(move, engine.getViewport().getCenter(), move);

			if (zoomIn) {
				zoomX -= screenX * amount * 3;
				zoomX = Math.max(screenX / 2, zoomX);
				zoomY -= screenY * amount * 3;
				zoomY = Math.max(screenY / 2, zoomY);
				engine.getViewport().set(engine.getViewport().getLLX(), engine.getViewport().getLLY(), zoomX, zoomY);
			} else if (zoomOut) {
				zoomX += screenX * amount * 3;
				zoomX = Math.min(screenX * 10, zoomX);
				zoomY += screenY * amount * 3;
				zoomY = Math.min(screenY * 10, zoomY);
				engine.getViewport().set(engine.getViewport().getLLX(), engine.getViewport().getLLY(), zoomX, zoomY);
			}

			engine.getViewport().setCenter(move);
		} else if (freeCam == 1) {
			for (InputEventAPI i : events) {
				if (i.isRMBDownEvent()) {
					// log.info("Right-click down");
					zoomOut = true;
				}
				if (i.isRMBUpEvent()) {
					// log.info("Right-click up.");
					zoomOut = false;
				}
				if (i.isLMBDownEvent()) {
					// log.info("Left-click down");
					zoomIn = true;
				}

				if (i.isLMBUpEvent()) {
					// log.info("Left-click up.");
					zoomIn = false;
				}

				if (!i.isCtrlDown() && i.isAltDown()) {
					freeCam = -1;
					camToggle = true;
					log.info("Reset cam.");
				}

				if (i.isMouseMoveEvent()) {
					target = new Vector2f(i.getX() - (screenX / 2), i.getY() - (screenY / 2));

					if (target.lengthSquared() > Math.pow(screenY / 2, 2)) {
						//clamp max offset
						target = MathUtils.getPointOnCircumference(null, screenY / 2, VectorUtils.getFacing(target));
					}
				}
			}

			float smooth = target.lengthSquared() / (float) Math.pow(screenY / 4, 2f);

			Vector2f move = new Vector2f(target);
			move.scale(amount * smooth);
			Vector2f.add(move, engine.getViewport().getCenter(), move);

			move = new Vector2f(Math.max(-mapX / 2, Math.min(mapX / 2, move.x)), Math.max(-mapY / 2, Math.min(mapY / 2, move.y)));

			if (zoomIn) {
				zoomX -= screenX * amount * 3;
				zoomX = Math.max(screenX / 2, zoomX);
				zoomY -= screenY * amount * 3;
				zoomY = Math.max(screenY / 2, zoomY);
				engine.getViewport().set(engine.getViewport().getLLX(), engine.getViewport().getLLY(), zoomX, zoomY);
			} else if (zoomOut) {
				zoomX += screenX * amount * 3;
				zoomX = Math.min(screenX * 10, zoomX);
				zoomY += screenY * amount * 3;
				zoomY = Math.min(screenY * 10, zoomY);
				engine.getViewport().set(engine.getViewport().getLLX(), engine.getViewport().getLLY(), zoomX, zoomY);
			}

			engine.getViewport().setCenter(move);
		}
	}
}