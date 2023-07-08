package data.missions.scripts.automation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.*;

public class AutomationUtils {

	public static final String SELF = "self";

	public static final String HULL_SIZE_FRIGATE = "frigate";
	public static final String HULL_SIZE_DESTROYER = "destroyer";
	public static final String HULL_SIZE_CRUISER = "cruiser";
	public static final String HULL_SIZE_CAPITAL = "capital";
	public static final String HULL_SIZE_LARGEST = "largest";
	public static final String HULL_SIZE_SMALLEST = "smallest";

	public static final String HULL_TYPE_ANY = "any";
	public static final String HULL_TYPE_PHASE = "phase";
	public static final String HULL_TYPE_CARRIER = "carrier";

	private static final Map<String, HullSize> HULL_SIZE_MAP = new HashMap<>();
	private static final Map<HullSize, String> HULL_SIZE_REV_MAP = new HashMap<>();
	static {
		HULL_SIZE_MAP.put(HULL_SIZE_FRIGATE, HullSize.FRIGATE);
		HULL_SIZE_MAP.put(HULL_SIZE_DESTROYER, HullSize.DESTROYER);
		HULL_SIZE_MAP.put(HULL_SIZE_CRUISER, HullSize.CRUISER);
		HULL_SIZE_MAP.put(HULL_SIZE_CAPITAL, HullSize.CAPITAL_SHIP);
		HULL_SIZE_REV_MAP.put(HullSize.FRIGATE, HULL_SIZE_FRIGATE);
		HULL_SIZE_REV_MAP.put(HullSize.DESTROYER, HULL_SIZE_DESTROYER);
		HULL_SIZE_REV_MAP.put(HullSize.CRUISER, HULL_SIZE_CRUISER);
		HULL_SIZE_REV_MAP.put(HullSize.CAPITAL_SHIP, HULL_SIZE_CAPITAL);
	}

	public static HullSize stringToHullSize(String string) {
		return HULL_SIZE_MAP.get(string.toLowerCase());
	}

	public static String hullSizeToString(HullSize hullSize) {
		return HULL_SIZE_REV_MAP.get(hullSize);
	}

	public static final String LOAD_PATH = "tournament/automation.csv";

	private static final Map<String, String> STARTER_SCRIPT_PATH = new HashMap<>();
	private static final Map<String, String> FILTER_SCRIPT_PATH = new HashMap<>();
	private static final Map<String, String> PICKER_SCRIPT_PATH = new HashMap<>();
	private static final Map<String, String> SUFFIX_SCRIPT_PATH = new HashMap<>();

	public static void initAutomation() {
		STARTER_SCRIPT_PATH.clear();
		FILTER_SCRIPT_PATH.clear();
		PICKER_SCRIPT_PATH.clear();
		SUFFIX_SCRIPT_PATH.clear();

		int i = 0;
		try {
			JSONArray jsonArray = Global.getSettings().getMergedSpreadsheetDataForMod("id", LOAD_PATH, "aibattles");
			for (i = 0; i < jsonArray.length(); i++) {
				JSONObject row = jsonArray.getJSONObject(i);
				String id = row.getString("id").toLowerCase();
				String type = row.getString("type").toLowerCase();
				String script = row.getString("script");

				switch (type.toLowerCase()) {
					case "starter":
						STARTER_SCRIPT_PATH.put(id, script);
						break;
					case "filter":
						FILTER_SCRIPT_PATH.put(id, script);
						break;
					case "picker":
						PICKER_SCRIPT_PATH.put(id, script);
						break;
					case "suffix":
						SUFFIX_SCRIPT_PATH.put(id, script);
						break;
					default:
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("File error while loading automation " + i + ".");
		}
	}

	public static StarterAPI createStarter(String id) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return (StarterAPI)Global.getSettings().getScriptClassLoader().loadClass(STARTER_SCRIPT_PATH.get(id)).newInstance();
	}

	public static boolean isStarter(String id) {
		return STARTER_SCRIPT_PATH.containsKey(id);
	}

	public static FilterAPI createFilter(String id) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return (FilterAPI)Global.getSettings().getScriptClassLoader().loadClass(FILTER_SCRIPT_PATH.get(id)).newInstance();
	}

	public static boolean isFilter(String id) {
		return FILTER_SCRIPT_PATH.containsKey(id);
	}

	public static PickerAPI createPicker(String id) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return (PickerAPI)Global.getSettings().getScriptClassLoader().loadClass(PICKER_SCRIPT_PATH.get(id)).newInstance();
	}

	public static boolean isPicker(String id) {
		return PICKER_SCRIPT_PATH.containsKey(id);
	}

	public static SuffixAPI createSuffix(String id) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return (SuffixAPI)Global.getSettings().getScriptClassLoader().loadClass(SUFFIX_SCRIPT_PATH.get(id)).newInstance();
	}

	public static boolean isSuffix(String id) {
		return SUFFIX_SCRIPT_PATH.containsKey(id);
	}

	public static List<ShipAPI> getEnemiesOnMap(ShipAPI source) {
		List<ShipAPI> enemies = AIUtils.getEnemiesOnMap(source);
		Iterator<ShipAPI> iterator = enemies.listIterator();
		while (iterator.hasNext()) {
			ShipAPI enemy = iterator.next();
			if (!isTargetBasicallyValid(enemy)) {
				iterator.remove();
			}
		}

		return enemies;
	}

	public static List<ShipAPI> getAlliesOnMap(ShipAPI source) {
		List<ShipAPI> enemies = AIUtils.getAlliesOnMap(source);
		Iterator<ShipAPI> iterator = enemies.listIterator();
		while (iterator.hasNext()) {
			ShipAPI ally = iterator.next();
			if (!isTargetBasicallyValid(ally)) {
				iterator.remove();
			}
		}

		return enemies;
	}

	public static List<BattleObjectiveAPI> getEnemiesObjectiveOnMap(ShipAPI source) {
		List<BattleObjectiveAPI> objectives = new ArrayList<>();
		for (BattleObjectiveAPI objective : Global.getCombatEngine().getObjectives()) {
			if (objective.getOwner() != source.getOwner()) objectives.add(objective);
		}

		return objectives;
	}

	public static List<BattleObjectiveAPI> getAlliesObjectiveOnMap(ShipAPI source) {
		List<BattleObjectiveAPI> objectives = new ArrayList<>();
		for (BattleObjectiveAPI objective : Global.getCombatEngine().getObjectives()) {
			if (objective.getOwner() == source.getOwner()) objectives.add(objective);
		}

		return objectives;
	}

	public static boolean isTargetBasicallyValid(ShipAPI target) {
		if (!target.isAlive()) return false;
		if (target.isFighter()) return false;
		if (target.isDrone()) return false;
		if (target.isStationModule()) return false;
		return true;
	}

	public static int getAnotherOwner(int owner) {
		if (owner == Misc.OWNER_PLAYER) return 1;
		else return 0;
	}

	public static boolean equals(AssignmentInfo info1, AssignmentInfo info2) {
		if (info1 == null || info2 == null) return false;
		if (info1.getType() == CombatAssignmentType.RETREAT && info2.getType() == CombatAssignmentType.RETREAT) return true;
		return info1.getType() == info2.getType() && info1.getTarget() == info2.getTarget();
	}

	public static boolean equals(String[] list, String object) {
		return list.length == 1 && list[0].contentEquals(object);
	}

	public static boolean contains(String[] list, String object) {
		for (String item : list) {
			if (item.contentEquals(object)) return true;
		}
		return false;
	}

	public static float getFactorFromString(String string) throws RuntimeException {

		char last = string.charAt(string.length() - 1);
		if (last == '%' || last == '﹪') {
			return Float.parseFloat(string.substring(0, string.length() - 1)) * 0.01f;
		} else {
			return Float.parseFloat(string);
		}
	}

	public static float getSelfFromString(String string, float selfAlternative) throws RuntimeException {

		if (string.contentEquals(SELF)) return selfAlternative;
		return Float.parseFloat(string);
	}

	public static boolean isPercent(float number) {
		return number <= 1f;
	}

	public static CombatAssignmentType getAssignmentTypeOnTarget(AssignmentTargetAPI target, CombatTaskManagerAPI taskManager) {
		for (AssignmentInfo info : taskManager.getAllAssignments()) {
			if (info.getTarget() == target) return info.getType();
		}
		return null;
	}

	public static int getAssignmentCountOnTarget(AssignmentTargetAPI target, CombatTaskManagerAPI taskManager) {
		int count = 0;
		for (ShipAPI ship : Global.getCombatEngine().getShips()) {
			if (taskManager.getAssignmentFor(ship) == null) continue;
			if (taskManager.getAssignmentFor(ship).getTarget() == target) count++;
		}
		return count;
	}

	public static final String INFO_MAP_DATA_KEY = "Automation_field_";

	public static AutomationField getAutomationField(CombatEngineAPI engine, int owner, boolean ally) {
		String key = INFO_MAP_DATA_KEY + owner;
		if (ally) key += "_ally";

		if (!engine.getCustomData().containsKey(key)) {
			engine.getCustomData().put(key, new AutomationField(engine, owner, ally));
		}

		return (AutomationField)engine.getCustomData().get(key);
	}

	public static class AutomationField {

		private final CombatFleetManagerAPI manager;
		private final CombatTaskManagerAPI tasks;

		private final Map<ShipAPI, AutomationInfo> allAutomationInfo;
		private final List<AssignmentInfo> infoFromAutomation;

		public AutomationField(CombatEngineAPI engine, int owner, boolean ally) {
			this.manager = engine.getFleetManager(owner);
			this.tasks = manager.getTaskManager(ally);

			this.allAutomationInfo = new HashMap<>();
			this.infoFromAutomation = new ArrayList<>();
		}

		public CombatTaskManagerAPI getTasks() {
			return tasks;
		}

		public CombatFleetManagerAPI getManager() {
			return manager;
		}

		public AssignmentInfo getInfoFor(ShipAPI ship) {
			return tasks.getAssignmentFor(ship);
		}

		public boolean hasPlayerForcedAssignment(ShipAPI ship) {
			AssignmentInfo info = tasks.getAssignmentFor(ship);
			if (info == null) return false;

			if (!allAutomationInfo.containsKey(ship)) return false;

			AutomationInfo automationInfo = allAutomationInfo.get(ship);
			return !AutomationUtils.equals(automationInfo.getInfo(), info);
		}

		public CombatAssignmentType getAssignmentTypeOnTarget(AssignmentTargetAPI target) {
			return AutomationUtils.getAssignmentTypeOnTarget(target, tasks);
		}

		public void assignToAutomationInfo(ShipAPI ship, CombatAssignmentType type, AssignmentTargetAPI target, float overridePriority) {

			if (type == null) {
				tasks.orderSearchAndDestroy(manager.getDeployedFleetMember(ship), false);
				allAutomationInfo.remove(ship);
				return;
			}

			if (type == CombatAssignmentType.RETREAT) {
				DeployedFleetMemberAPI dfm = (DeployedFleetMemberAPI)target;
				tasks.orderRetreat(dfm, false, false);

				AssignmentInfo newInfo = new RetreatAssignmentInfo(dfm);
				infoFromAutomation.add(newInfo);
				allAutomationInfo.put(ship, new AutomationInfo(newInfo, overridePriority));
				return;
			}

			AssignmentInfo newInfo = tasks.createAssignment(type, target, false);
			infoFromAutomation.add(newInfo);
			assignToAutomationInfo(ship, new AutomationInfo(newInfo, overridePriority));
		}

		public void assignToAutomationInfo(ShipAPI ship, AutomationInfo automationInfo) {
			tasks.giveAssignment(manager.getDeployedFleetMember(ship), automationInfo.getInfo(), false);
			allAutomationInfo.put(ship, automationInfo);
		}

		public AutomationInfo getOneAutomationInfoOnTarget(AssignmentTargetAPI target) {
			for (AutomationInfo automationInfo : allAutomationInfo.values()) {
				if (automationInfo.getInfo().getTarget() == target) return automationInfo;
			}
			return null;
		}

		public void clearAllAutomationInfoOnTarget(AssignmentTargetAPI target) {
			Iterator<Map.Entry<ShipAPI, AutomationInfo>> iter = allAutomationInfo.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<ShipAPI, AutomationInfo> entry = iter.next();
				ShipAPI source = entry.getKey();
				AutomationInfo automationInfo = entry.getValue();

				if (automationInfo.getInfo().getTarget() == target) {
					tasks.orderSearchAndDestroy(manager.getDeployedFleetMember(source), false);
					iter.remove();
				}
			}
		}

		private final List<ShipAPI> shipsWithAutomation = new ArrayList<>();
		private final List<AssignmentInfo> notAssignedWithShip = new ArrayList<>();
		private final List<AssignmentInfo> infoFromAutomationToRemove = new ArrayList<>();

		public void registerShipCache(ShipAPI ship) {
			shipsWithAutomation.add(ship);
		}

		public void pruneAssignments() {

			out : for (AssignmentInfo info : tasks.getAllAssignments()) {
				for (ShipAPI ship : shipsWithAutomation) {
					if (AutomationUtils.equals(getInfoFor(ship), info)) continue out;
				}

				notAssignedWithShip.add(info);
			}

			for (AssignmentInfo info : notAssignedWithShip) {
				for (AssignmentInfo automationInfo : infoFromAutomation) {
					if (AutomationUtils.equals(info, automationInfo)) {
						infoFromAutomationToRemove.add(automationInfo);
						tasks.removeAssignment(info);
					}
				}
			}

			infoFromAutomation.removeAll(infoFromAutomationToRemove);
			infoFromAutomationToRemove.clear();

			notAssignedWithShip.clear();
			shipsWithAutomation.clear();
		}
	}

	public static class AutomationInfo {

		private final AssignmentInfo info;
		private final float priority;

		public AutomationInfo(AssignmentInfo info, float priority) {
			this.info = info;
			this.priority = priority;
		}

		public AssignmentInfo getInfo() {
			return info;
		}

		public float getPriority() {
			return priority;
		}
	}

	public static class RetreatAssignmentInfo implements AssignmentInfo {

		private final DeployedFleetMemberAPI ship;

		public RetreatAssignmentInfo(DeployedFleetMemberAPI ship) {
			this.ship = ship;
		}

		@Override
		public CombatAssignmentType getType() {
			return CombatAssignmentType.RETREAT;
		}

		@Override
		public AssignmentTargetAPI getTarget() {
			return ship;
		}
	}
}
