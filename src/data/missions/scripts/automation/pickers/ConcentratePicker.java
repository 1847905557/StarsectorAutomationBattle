package data.missions.scripts.automation.pickers;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import data.missions.scripts.automation.AutomationUtils;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class ConcentratePicker extends NearestPicker {

	@Override
	public AssignmentTargetAPI pick(ShipAPI source, List<CombatEntityAPI> targets, CombatAssignmentType type, CombatEngineAPI engine) {
		Collections.sort(targets, new CollectionUtils.SortEntitiesByDistance(source.getLocation()));
		CombatEntityAPI sample = targets.get(0);

		CombatFleetManagerAPI sourceManager = engine.getFleetManager(source.getOwner());
		CombatFleetManagerAPI targetManager = engine.getFleetManager(sample.getOwner());
		CombatTaskManagerAPI tasks = sourceManager.getTaskManager(source.isAlly());

		AssignmentInfo current = tasks.getAssignmentFor(source);
		if (current == null) return super.pick(source, targets, type, engine);

		boolean canKeepCurrent = false;
		for (CombatEntityAPI target : targets) {
			AssignmentTargetAPI assignmentTarget;
			if (target instanceof ShipAPI) assignmentTarget = targetManager.getDeployedFleetMember((ShipAPI)target);
			else assignmentTarget = (AssignmentTargetAPI)target;

			if (assignmentTarget == current.getTarget()) {
				canKeepCurrent = true;
				continue;
			}

			CombatAssignmentType onTarget = AutomationUtils.getAssignmentTypeOnTarget(assignmentTarget, tasks);
			if (onTarget != null && onTarget == current.getType()) return assignmentTarget;
		}

		if (canKeepCurrent && current.getTarget() != null && current.getType() == type) return current.getTarget();
		return super.pick(source, targets, type, engine);
	}
}
