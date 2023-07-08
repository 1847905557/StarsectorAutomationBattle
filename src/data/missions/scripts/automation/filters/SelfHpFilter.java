package data.missions.scripts.automation.filters;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.FilterAPI;

import java.util.List;

public class SelfHpFilter<E> implements FilterAPI<E> {

	@Override
	public void filterTargets(ShipAPI source, List<E> targets, String[] params, CombatEngineAPI engine) {

		float minHp = -1f;
		float maxHp = -1f;
		if (params.length == 1) maxHp = AutomationUtils.getFactorFromString(params[0]);
		else {
			minHp = AutomationUtils.getFactorFromString(params[0]);
			maxHp = AutomationUtils.getFactorFromString(params[1]);
		}

		if (maxHp <= 0f) {
			targets.clear();
			return;
		}

		if (AutomationUtils.isPercent(maxHp)) {
			if (source.getHullLevel() > maxHp) {
				targets.clear();
				return;
			}
		} else {
			if (source.getHitpoints() > maxHp) {
				targets.clear();
				return;
			}
		}

		if (AutomationUtils.isPercent(minHp)) {
			if (source.getHullLevel() < minHp) {
				targets.clear();
				return;
			}
		} else {
			if (source.getHitpoints() < minHp) {
				targets.clear();
				return;
			}
		}
	}
}
