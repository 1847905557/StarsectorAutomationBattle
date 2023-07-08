package data.missions.scripts.automation.starters;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.StarterAPI;

import java.util.ArrayList;
import java.util.List;

public class CleanStarter implements StarterAPI<ShipAPI> {

	@Override
	public List<ShipAPI> getInitialTargets(ShipAPI source, CombatEngineAPI engine) {
		List<ShipAPI> list = new ArrayList<>();
		list.add(source);
		return list;
	}

	@Override
	public CombatAssignmentType getCombatAssignmentType() {
		return null;
	}
}
