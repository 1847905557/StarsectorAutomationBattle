package data.missions.scripts.automation.filters;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.missions.scripts.automation.FilterAPI;
import data.missions.scripts.automation.AutomationUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class HullSizeFilter implements FilterAPI<ShipAPI> {

	public static final boolean[] hullSizeContext = new boolean[6];
	public static final boolean[] hullSizeContext2 = new boolean[6];

	@Override
	public void filterTargets(ShipAPI source, List<ShipAPI> targets, String[] params, CombatEngineAPI engine) {

		Arrays.fill(hullSizeContext, false);
		Arrays.fill(hullSizeContext2, false);

		boolean setSize = false;
		for (String param : params) {

			HullSize hullSize = AutomationUtils.stringToHullSize(param);
			if (hullSize == null) continue;

			hullSizeContext[hullSize.ordinal()] = true;
			setSize = true;
		}

		HullSize largestInTargets = HullSize.FRIGATE;
		HullSize smallestInTargets = HullSize.CAPITAL_SHIP;
		for (ShipAPI target : targets) {
			hullSizeContext2[target.getHullSize().ordinal()] = true;
			if (target.getHullSize().ordinal() > largestInTargets.ordinal()) largestInTargets = target.getHullSize();
			if (target.getHullSize().ordinal() < smallestInTargets.ordinal()) smallestInTargets = target.getHullSize();
		}

		if (AutomationUtils.contains(params, AutomationUtils.HULL_SIZE_LARGEST)) {

			if (setSize) {
				int keep = -1;
				for (int i = hullSizeContext.length - 1; i > 0; i--) {
					if (hullSizeContext[i] && hullSizeContext2[i]) {
						keep = i;
						break;
					}
				}

				Arrays.fill(hullSizeContext, false);
				if (keep > 0) hullSizeContext[keep] = true;

			} else {
				hullSizeContext[largestInTargets.ordinal()] = true;
			}

		} else if (AutomationUtils.contains(params, AutomationUtils.HULL_SIZE_SMALLEST)) {

			if (setSize) {
				int keep = -1;
				for (int i = 0; i < hullSizeContext.length; i++) {
					if (hullSizeContext[i] && hullSizeContext2[i]) {
						keep = i;
						break;
					}
				}

				Arrays.fill(hullSizeContext, false);
				if (keep > 0) hullSizeContext[keep] = true;

			} else {
				hullSizeContext[smallestInTargets.ordinal()] = true;
			}
		}


		Iterator<ShipAPI> iterator = targets.listIterator();
		while (iterator.hasNext()) {
			ShipAPI target = iterator.next();

			if (!hullSizeContext[target.getHullSize().ordinal()]) {
				iterator.remove();
			}
		}
	}
}
