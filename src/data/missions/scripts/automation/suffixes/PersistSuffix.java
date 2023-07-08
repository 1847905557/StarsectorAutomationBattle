package data.missions.scripts.automation.suffixes;

import com.fs.starfarer.api.combat.AssignmentTargetAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import data.missions.scripts.automation.AutomationUtils;
import data.missions.scripts.automation.SuffixAPI;

public class PersistSuffix implements SuffixAPI {

	private AssignmentTargetAPI lastTarget = null;

	@Override
	public float getFloatResultWithTarget(AssignmentTargetAPI target, String[] params, Object custom, CombatEngineAPI engine) {
		return 0f;
	}

	@Override
	public Object getCustomResultWithTarget(AssignmentTargetAPI target, String[] params, Object custom, CombatEngineAPI engine) {

		boolean lastIsCurrent = (boolean)custom;
		if (lastTarget != null) {
			if (!lastIsCurrent) lastTarget = null;
			else if (lastTarget instanceof DeployedFleetMemberAPI) {
				DeployedFleetMemberAPI shipTarget = (DeployedFleetMemberAPI)lastTarget;
				if (!AutomationUtils.isTargetBasicallyValid(shipTarget.getShip())) lastTarget = null;
			}
		}
		if (lastTarget == null) lastTarget = target;
		return lastTarget;
	}
}
