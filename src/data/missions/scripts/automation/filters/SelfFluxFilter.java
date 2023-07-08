package data.missions.scripts.automation.filters;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.FilterAPI;

import java.util.List;

public class SelfFluxFilter<E> implements FilterAPI<E> {

	@Override
	public void filterTargets(ShipAPI source, List<E> targets, String[] params, CombatEngineAPI engine) {

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

		if (AutomationUtils.isPercent(maxFlux)) {
			if (source.getFluxLevel() > maxFlux) {
				targets.clear();
				return;
			}
		} else {
			if (source.getCurrFlux() > maxFlux) {
				targets.clear();
				return;
			}
		}

		if (AutomationUtils.isPercent(minFlux)) {
			if (source.getFluxLevel() < minFlux) {
				targets.clear();
				return;
			}
		} else {
			if (source.getCurrFlux() < minFlux) {
				targets.clear();
				return;
			}
		}
	}
}
