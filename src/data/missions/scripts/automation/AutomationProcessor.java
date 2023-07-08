package data.missions.scripts.automation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AssignmentTargetAPI;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import data.missions.scripts.automation.AutomationUtils.AutomationField;
import data.missions.scripts.automation.AutomationUtils.AutomationInfo;

import java.util.ArrayList;
import java.util.List;

public class AutomationProcessor {

	public static final String SPLIT_BY_LINES = "\\r?\\n";
	public static final String SPLIT_BY_SEMICOLON = ";";

	private final List<AutomationThread> threads;

	private AutomationThread lastThread = null;

	private AutomationProcessor(List<AutomationThread> threads) {
		this.threads = threads;
	}

	public static AutomationProcessor loadAutomationScript(String script) {

		List<AutomationThread> threads = new ArrayList<>();

		String[] lines = script.toLowerCase().split(SPLIT_BY_LINES);
		for (String line : lines) {
			String[] singles = line.split(SPLIT_BY_SEMICOLON);

			for (String single : singles) {
				String afterTrim = single.trim();
				if (afterTrim.isEmpty()) continue;

				try {
					AutomationThread thread = AutomationThread.loadSingleAutomationOrder(afterTrim);
					threads.add(thread);
				} catch (Exception e) {
					e.printStackTrace();
					Global.getLogger(AutomationProcessor.class).info("error while loading " + single);
				}
			}
		}

		AutomationProcessor processor = new AutomationProcessor(threads);
		return processor;
	}

	public boolean advance(CombatEngineAPI engine, ShipAPI ship, AutomationField field) {

		if (field.hasPlayerForcedAssignment(ship)) return false;

		AutomationThread toProcess = null;
		AssignmentTargetAPI assignmentTarget = null;

		for (AutomationThread thread : threads) {
			assignmentTarget = thread.advance(engine, ship, this);
			if (assignmentTarget != null) {
				toProcess = thread;
				break;
			}
		}

		if (toProcess == null) return false;
		lastThread = toProcess;

		float overridePriority = toProcess.getCachedPriority();
		CombatAssignmentType typeOnTarget = field.getAssignmentTypeOnTarget(assignmentTarget);
		if (typeOnTarget == null) { // no order, so just do it
			field.assignToAutomationInfo(ship, toProcess.getCombatAssignmentType(), assignmentTarget, overridePriority);
			return true;
		}

		AutomationInfo automationSampleOnTarget = field.getOneAutomationInfoOnTarget(assignmentTarget);
		if (automationSampleOnTarget == null) { // has order but not auto, means it may be assigned by player, check if should it

			if (ship.getOwner() != Misc.OWNER_PLAYER) { // not assigned by player, do it
				field.assignToAutomationInfo(ship, toProcess.getCombatAssignmentType(), assignmentTarget, overridePriority);
				return true;
			}

			return false;
		}

		AssignmentInfo infoOnSelf = field.getInfoFor(ship);
		if (automationSampleOnTarget.getInfo().getType() == toProcess.getCombatAssignmentType() && infoOnSelf != automationSampleOnTarget.getInfo()) {
			if (automationSampleOnTarget.getInfo().getType() != CombatAssignmentType.RETREAT) field.assignToAutomationInfo(ship, automationSampleOnTarget); // same type, join it
			return true;
		}

		if (overridePriority <= automationSampleOnTarget.getPriority()) return false; // not same type, do nothing

		field.clearAllAutomationInfoOnTarget(assignmentTarget); // dispose all others and create at own
		field.assignToAutomationInfo(ship, toProcess.getCombatAssignmentType(), assignmentTarget, overridePriority);
		return true;
	}

	public AutomationThread getLastThread() {
		return lastThread;
	}

	public boolean isEmpty() {
		return threads.isEmpty();
	}
}
