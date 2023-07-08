package data.missions.scripts.automation.starters;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.StarterAPI;

import java.util.List;

public class FighterStrikeStarter implements StarterAPI<ShipAPI> {

	@Override
	public List<ShipAPI> getInitialTargets(ShipAPI source, CombatEngineAPI engine) {
		return AutomationUtils.getEnemiesOnMap(source);
	}

	@Override
	public CombatAssignmentType getCombatAssignmentType() {
		return CombatAssignmentType.STRIKE;
	}
}
