package data.missions.scripts.automation.filters;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.FilterAPI;

import java.util.Iterator;
import java.util.List;

public class TargetSpeedFilter implements FilterAPI<ShipAPI> {

	@Override
	public void filterTargets(ShipAPI source, List<ShipAPI> targets, String[] params, CombatEngineAPI engine) {

		float selfSpeed = source.getMaxSpeed();
		float minSpeed = -1f;
		float maxSpeed = -1f;
		if (params.length == 1) maxSpeed = AutomationUtils.getSelfFromString(params[0], selfSpeed); // always actual number
		else {
			minSpeed = AutomationUtils.getSelfFromString(params[0], selfSpeed);
			maxSpeed = AutomationUtils.getSelfFromString(params[1], selfSpeed);
		}

		if (maxSpeed <= 0f) {
			targets.clear();
			return;
		}

		Iterator<ShipAPI> iterator = targets.listIterator();
		while (iterator.hasNext()) {
			ShipAPI target = iterator.next();

			float speed = target.getMaxSpeed();
			if (speed > maxSpeed || speed < minSpeed) {
				iterator.remove();
			}
		}
	}
}
