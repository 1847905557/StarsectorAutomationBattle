package data.missions.scripts.automation.filters;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.FilterAPI;

import java.util.Iterator;
import java.util.List;

public class HullTypeFilter implements FilterAPI<ShipAPI> {

	@Override
	public void filterTargets(ShipAPI source, List<ShipAPI> targets, String[] params, CombatEngineAPI engine) {

		if (AutomationUtils.contains(params, AutomationUtils.HULL_TYPE_ANY)) {
			return;
		}

		Iterator<ShipAPI> iterator = targets.listIterator();
		while (iterator.hasNext()) {
			ShipAPI target = iterator.next();

			if (AutomationUtils.contains(params, AutomationUtils.HULL_TYPE_PHASE) && target.getHullSpec().getHints().contains(ShipTypeHints.PHASE)) {
				continue;
			}

			if (AutomationUtils.contains(params, AutomationUtils.HULL_TYPE_CARRIER) && target.getHullSpec().getHints().contains(ShipTypeHints.CARRIER)) {
				continue;
			}

			iterator.remove();
		}
	}
}
