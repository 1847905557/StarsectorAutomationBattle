package data.missions.scripts.automation.suffixes;

import com.fs.starfarer.api.combat.AssignmentTargetAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import data.missions.scripts.automation.SuffixAPI;

public class PrioritySuffix implements SuffixAPI {

	@Override
	public float getFloatResultWithTarget(AssignmentTargetAPI target, String[] params, Object custom, CombatEngineAPI engine) {
		return Float.parseFloat(params[0]);
	}

	@Override
	public Object getCustomResultWithTarget(AssignmentTargetAPI target, String[] params, Object custom, CombatEngineAPI engine) {
		return null;
	}
}
