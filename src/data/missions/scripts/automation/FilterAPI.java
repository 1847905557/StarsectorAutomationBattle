package data.missions.scripts.automation;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.List;

public interface FilterAPI<E> {

	public void filterTargets(ShipAPI source, List<E> targets, String[] params, CombatEngineAPI engine);
}
