package data.missions.scripts.automation;

import com.fs.starfarer.api.combat.AssignmentTargetAPI;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.List;

public interface PickerAPI<E> {

	public AssignmentTargetAPI pick(ShipAPI source, List<E> targets, CombatAssignmentType type, CombatEngineAPI engine);
}
