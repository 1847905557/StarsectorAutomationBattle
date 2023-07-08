package data.missions.scripts.automation.filters;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.FilterAPI;

import java.util.Iterator;
import java.util.List;

public class TargetHpFilter implements FilterAPI<ShipAPI> {

	@Override
	public void filterTargets(ShipAPI source, List<ShipAPI> targets, String[] params, CombatEngineAPI engine) {

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

		Iterator<ShipAPI> iterator = targets.listIterator();
		while (iterator.hasNext()) {
			ShipAPI target = iterator.next();

			if (AutomationUtils.isPercent(maxHp)) {
				if (target.getHullLevel() > maxHp) {
					iterator.remove();
					return;
				}
			} else {
				if (target.getHitpoints() > maxHp) {
					iterator.remove();
					return;
				}
			}

			if (AutomationUtils.isPercent(minHp)) {
				if (target.getHullLevel() < minHp) {
					iterator.remove();
					return;
				}
			} else {
				if (target.getHitpoints() < minHp) {
					iterator.remove();
					return;
				}
			}
		}
	}
}
