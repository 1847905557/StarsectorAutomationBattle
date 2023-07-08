package data.missions.scripts.automation.pickers;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import data.missions.scripts.automation.AutomationUtils;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class SpreadPicker extends NearestPicker {

	@Override
	public AssignmentTargetAPI pick(ShipAPI source, List<CombatEntityAPI> targets, CombatAssignmentType type, CombatEngineAPI engine) {
		Collections.sort(targets, new CollectionUtils.SortEntitiesByDistance(source.getLocation()));
		CombatEntityAPI sample = targets.get(0);

		CombatFleetManagerAPI sourceManager = engine.getFleetManager(source.getOwner());
		CombatFleetManagerAPI targetManager = engine.getFleetManager(sample.getOwner());
		CombatTaskManagerAPI tasks = sourceManager.getTaskManager(source.isAlly());

		AssignmentInfo current = tasks.getAssignmentFor(source);
		for (CombatEntityAPI target : targets) {
			AssignmentTargetAPI assignmentTarget;
			if (target instanceof ShipAPI) assignmentTarget = targetManager.getDeployedFleetMember((ShipAPI)target);
			else assignmentTarget = (AssignmentTargetAPI)target;

 			if (current != null && current.getTarget() == assignmentTarget && current.getType() == type) {
				int count = AutomationUtils.getAssignmentCountOnTarget(assignmentTarget, tasks);
				if (count <= 1) return assignmentTarget;
			}

			CombatAssignmentType onTarget = AutomationUtils.getAssignmentTypeOnTarget(assignmentTarget, tasks);
			if (onTarget == null) return assignmentTarget;
		}

		return super.pick(source, targets, type, engine);
	}
}
