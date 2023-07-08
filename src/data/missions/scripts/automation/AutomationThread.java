package data.missions.scripts.automation;

import com.fs.starfarer.api.combat.AssignmentTargetAPI;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.missions.scripts.automation.suffixes.PersistSuffix;
import data.missions.scripts.automation.suffixes.PrioritySuffix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutomationThread {

	public static final String SPLIT_BY_DOT = "\\.";
	public static final Pattern PATTERN = Pattern.compile("^(\\w+)\\(([^\\)]+)\\)$");

	private final StarterAPI starter;
	private final Map<FilterAPI, String[]> filters;
	private final PickerAPI picker;
	private final Map<SuffixAPI, String[]> suffixes;

	private float cachedPriority = -1f;

	private AutomationThread(StarterAPI starter, Map<FilterAPI, String[]> filters, PickerAPI picker, Map<SuffixAPI, String[]> suffixes) {
		this.starter = starter;
		this.filters = filters;
		this.picker = picker;
		this.suffixes = suffixes;
	}

	public static AutomationThread loadSingleAutomationOrder(String script) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		StarterAPI starter = null;
		Map<FilterAPI, String[]> filters = new LinkedHashMap<>();
		PickerAPI picker = null;
		Map<SuffixAPI, String[]> suffixes = new LinkedHashMap<>();

		boolean picked = false;

		String part = "";
		String[] parts = script.split(SPLIT_BY_DOT);

		for (int i = 0; i < parts.length; i++) {
			part = parts[i];

			if (starter == null) {
				starter = AutomationUtils.createStarter(part);
				continue;
			}

			String key;
			String[] params = null;

			Matcher matcher = PATTERN.matcher(part);
			if (matcher.matches()) {
				key = matcher.group(1);
				params = matcher.group(2).split(",\\s*");
			} else {
				key = part;
			}

			if (AutomationUtils.isPicker(key)) {
				picked = true;

				picker = AutomationUtils.createPicker(key);
				continue;
			}

			if (picked) {
				SuffixAPI suffix = AutomationUtils.createSuffix(key);
				suffixes.put(suffix, params);
			} else {
				FilterAPI filter = AutomationUtils.createFilter(key);
				filters.put(filter, params);
			}
		}

		AutomationThread thread = new AutomationThread(starter, filters, picker, suffixes);
		return thread;
	}

	public AssignmentTargetAPI advance(CombatEngineAPI engine, ShipAPI ship, AutomationProcessor processor) {

		List targets = starter.getInitialTargets(ship, engine);
		for (FilterAPI filter : filters.keySet()) {

			if (targets.isEmpty()) break;
 			filter.filterTargets(ship, targets, filters.get(filter), engine);
		}

		if (targets.isEmpty()) return null;
		AssignmentTargetAPI assignmentTarget = picker.pick(ship, targets, starter.getCombatAssignmentType(), engine);
		cachedPriority = 0f;

		for (SuffixAPI suffix : suffixes.keySet()) {
			if (suffix instanceof PrioritySuffix) {
				cachedPriority = suffix.getFloatResultWithTarget(assignmentTarget, suffixes.get(suffix), null, engine);
			} else if (suffix instanceof PersistSuffix) {
				assignmentTarget = (AssignmentTargetAPI)suffix.getCustomResultWithTarget(assignmentTarget, suffixes.get(suffix), (processor.getLastThread() == this), engine);
			}
		}

		return assignmentTarget;
	}

	public float getCachedPriority() {
		return cachedPriority;
	}

	public CombatAssignmentType getCombatAssignmentType() {
		return starter.getCombatAssignmentType();
	}
}
