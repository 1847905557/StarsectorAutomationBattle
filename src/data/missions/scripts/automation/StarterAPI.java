package data.missions.scripts.automation;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.List;

public interface StarterAPI<E> {

	public List<E> getInitialTargets(ShipAPI source, CombatEngineAPI engine);

	public CombatAssignmentType getCombatAssignmentType();
}
