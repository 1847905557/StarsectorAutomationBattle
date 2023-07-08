package data.missions.scripts.automation.filters;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.AutomationProcessor;
import data.missions.scripts.automation.FilterAPI;
import org.lazywizard.lazylib.MathUtils;

import java.util.Iterator;
import java.util.List;

public class DistanceFilter implements FilterAPI<CombatEntityAPI> {

	@Override
	public void filterTargets(ShipAPI source, List<CombatEntityAPI> targets, String[] params, CombatEngineAPI engine) {

		float minRange = -1f;
		float maxRange = -1f;
		if (params.length == 1) maxRange = Float.parseFloat(params[0]); // always actual number
		else {
			minRange = Float.parseFloat(params[0]);
			maxRange = Float.parseFloat(params[1]);
		}

		if (maxRange <= 0f) {
			targets.clear();
			return;
		}

		Iterator<CombatEntityAPI> iterator = targets.listIterator();
		while (iterator.hasNext()) {
			CombatEntityAPI target = iterator.next();

			float range = MathUtils.getDistance(source, target);
			if (range > maxRange || range < minRange) {
				iterator.remove();
			}
		}
	}
}
