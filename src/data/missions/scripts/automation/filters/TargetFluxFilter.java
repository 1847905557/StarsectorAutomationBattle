package data.missions.scripts.automation.filters;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.FilterAPI;

import java.util.Iterator;
import java.util.List;

public class TargetFluxFilter implements FilterAPI<ShipAPI> {

	@Override
	public void filterTargets(ShipAPI source, List<ShipAPI> targets, String[] params, CombatEngineAPI engine) {

		float minFlux = -1f;
		float maxFlux = -1f;
		if (params.length == 1) maxFlux = AutomationUtils.getFactorFromString(params[0]);
		else {
			minFlux = AutomationUtils.getFactorFromString(params[0]);
			maxFlux = AutomationUtils.getFactorFromString(params[1]);
		}

		if (maxFlux <= 0f) {
			targets.clear();
			return;
		}

		Iterator<ShipAPI> iterator = targets.listIterator();
		while (iterator.hasNext()) {
			ShipAPI target = iterator.next();
			if (target.getFluxTracker().isOverloaded()) continue;

			if (AutomationUtils.isPercent(maxFlux)) {
				if (target.getFluxLevel() > maxFlux) {
					iterator.remove();
					return;
				}
			} else {
				if (target.getCurrFlux() > maxFlux) {
					iterator.remove();
					return;
				}
			}

			if (AutomationUtils.isPercent(minFlux)) {
				if (target.getFluxLevel() < minFlux) {
					iterator.remove();
					return;
				}
			} else {
				if (target.getCurrFlux() < minFlux) {
					iterator.remove();
					return;
				}
			}
		}
	}
}
