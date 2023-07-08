package data.missions.scripts.automation.pickers;

import com.fs.starfarer.api.combat.*;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.PickerAPI;

import java.util.List;

public class RandomPicker implements PickerAPI<CombatEntityAPI> {

	@Override
	public AssignmentTargetAPI pick(ShipAPI source, List<CombatEntityAPI> targets, CombatAssignmentType type, CombatEngineAPI engine) {
		CombatEntityAPI picked = targets.get(0);

		CombatFleetManagerAPI sourceManager = engine.getFleetManager(source.getOwner());
		CombatFleetManagerAPI targetManager = engine.getFleetManager(picked.getOwner());

		if (picked instanceof ShipAPI) return targetManager.getDeployedFleetMember((ShipAPI)picked);
		else return (AssignmentTargetAPI)picked;
	}
}
