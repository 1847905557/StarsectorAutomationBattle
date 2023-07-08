package data.missions.scripts.automation;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.missions.scripts.AI_missionUtils.Fleet;
import data.missions.scripts.automation.AutomationUtils.AutomationField;

import java.util.ArrayList;
import java.util.List;

public class AutomationController {

	public static final float DEFAULT_INTERVAL = 0.15f;
	public static final IntervalUtil INTERVAL = new IntervalUtil(DEFAULT_INTERVAL, DEFAULT_INTERVAL);
	public static final List<AutomationField> LIST = new ArrayList<>();

	public static void processAll(CombatEngineAPI engine) {

		float amount = engine.getElapsedInLastFrame();
		INTERVAL.advance(amount);
		if (!INTERVAL.intervalElapsed()) return;

		for (ShipAPI ship : engine.getShips()) {
			if (!ship.isAlive()) continue;
			if (ship.isFighter()) continue;
			if (ship.isStation()) continue;
			if (ship.isStationModule()) continue;

			CombatTaskManagerAPI task = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly());
			if (task.isFullAssault()) continue;
			if (task.isInFullRetreat()) continue;

			AutomationProcessor automationProcessor = getAutomationProcessor(engine, ship);
			if (automationProcessor != null && !automationProcessor.isEmpty()) {
				AutomationField field = AutomationUtils.getAutomationField(engine, ship.getOwner(), ship.isAlly());
				automationProcessor.advance(engine, ship, field);
				field.registerShipCache(ship);

				if (!LIST.contains(field)) {
					LIST.add(field);
				}
			}
		}

		for (AutomationField field : LIST) {
			field.pruneAssignments();
		}

		LIST.clear();
	}

	public static final String DATA_KEY_PLAYER = "AutomationController_Player";
	public static final String DATA_KEY_ENEMY = "AutomationController_Enemy";

	public static AutomationProcessor getAutomationProcessor(CombatEngineAPI engine, ShipAPI ship) {
		String key = DATA_KEY_ENEMY;
		if (ship.getOriginalOwner() == Misc.OWNER_PLAYER) key = DATA_KEY_PLAYER;

		FleetMemberAPI member = ship.getFleetMember();
		if (member == null) return null;

		Fleet fleet = (Fleet)engine.getCustomData().get(key);
		if (fleet == null) return null;
		if (fleet.hasTag("default_ai")) return null;

		return fleet.getAutomationProcessor(member);
	}
}
