package data.missions.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponGroupType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import data.missions.scripts.automation.AutomationProcessor;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicRenderPlugin;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.io.IOException;
import java.util.*;

public class AI_missionUtils {

	private static final Logger log = Global.getLogger(AI_missionUtils.class);

	public static class Match {

		public int sidePlayer = 0;
		public int sideEnemy = 0;
		public int mapNumber = -1;
		public float sizeMult = 1;

		public static Match createPVP(int sidePlayer, int sideEnemy, int mapNumber, float sizeMult) {
			Match match = new Match();
			match.sidePlayer = sidePlayer;
			match.sideEnemy = sideEnemy;
			match.mapNumber = mapNumber;
			match.sizeMult = sizeMult;
			return match;
		}

		public static Match createPVE(int sidePlayer, int mapNumber, float sizeMult) {
			Match match = new Match();
			match.sidePlayer = sidePlayer;
			match.mapNumber = mapNumber;
			match.sizeMult = sizeMult;
			return match;
		}
	}

	public static class Fleet {

		private String name = "Unknown Fleet";
		private String author = "Unknown Author";
		private CampaignFleetAPI fleet;

		private final List<String> fleetSkills = new ArrayList<>();
		private final HashMap<FleetMemberAPI, AutomationProcessor> fleetMembers = new HashMap<>();
		private final Set<String> tags = new HashSet<>();

		public String getName() {
			return name;
		}

		public String getAuthor() {
			return author;
		}

		public AutomationProcessor getAutomationProcessor(FleetMemberAPI member) {
			if (!fleetMembers.containsKey(member)) return null;
			return fleetMembers.get(member);
		}

		public boolean hasTag(String tag) {
			return tags.contains(tag);
		}

		public String getTagStartsWith(String prefix) {
			for (String tag : tags) {
				if (tag.startsWith(prefix)) return tag;
			}
			return null;
		}

		private Fleet() {
			fleet = Global.getFactory().createEmptyFleet(Factions.PLAYER, name, true);
		}

		public static Fleet createSimpleFleet() {
			Fleet fleet = new Fleet();
			fleet.fleet.setCommander(Global.getFactory().createPerson());

			return fleet;
		}

		public static Fleet createFleet(String path, int playerNumber) {

			Fleet fleet = createSimpleFleet();

			String fleetDataPath = path + "player" + playerNumber + "_data.csv";
			try {
				JSONArray playerData = Global.getSettings().getMergedSpreadsheetDataForMod("name", fleetDataPath, "aibattles");
				for (int i = 0; i < playerData.length(); i++) {
					JSONObject row = playerData.getJSONObject(i);
					if (row.getString("name").isEmpty()) continue;

					fleet.name = row.optString("name", fleet.name);
					fleet.author = row.optString("author", fleet.author);

					String rawSkills = row.optString("skills", "");
					if (isStringValid(rawSkills)) for (String skill : rawSkills.split(",")) {
						skill = skill.trim();
						fleet.fleetSkills.add(skill);
						fleet.fleet.getCommander().getStats().increaseSkill(skill);
					}

					String rawTags = row.optString("tags", "");
					if (isStringValid(rawTags)) for (String tag : rawTags.split(",")) {
						tag = tag.trim();
						fleet.tags.add(tag);
					}

					break;
				}

			} catch (IOException | JSONException ex) {
				log.error("unable to read player" + playerNumber + "_data.csv");
				log.error(ex);
			}

			String memberDataPath = path + "player" + playerNumber + "_fleet.csv";
			try {
				JSONArray playerFleet = Global.getSettings().getMergedSpreadsheetDataForMod("rowNumber", memberDataPath, "aibattles");
				for (int i = 0; i < playerFleet.length(); i++) {
					JSONObject row = playerFleet.getJSONObject(i);
					if (row.getString("rowNumber").isEmpty()) continue;

					String variant = row.getString("variant");
					ShipVariantAPI loadedVariant = loadVariant(variant, fleet.hasTag("force_reload"));
					loadedVariant.addTag(Tags.TAG_NO_AUTOFIT);

					FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, loadedVariant);
					fleet.fleet.getFleetData().addFleetMember(member);
					//fleet.fleet.getFleetData().ensureHasFlagship();

					String name = row.optString("name", member.getHullSpec().getHullName());
					if (!isStringValid(name)) name = member.getHullSpec().getHullName();
					member.setShipName(name);
					member.getCrewComposition().setCrew(member.getMaxCrew());
					member.getRepairTracker().setCR(0.7f);
					member.getRepairTracker().setCR(Math.max(member.getRepairTracker().getCR(), member.getRepairTracker().getMaxCR()));
					member.getRepairTracker().setMothballed(false);
					member.getRepairTracker().setCrashMothballed(false);

					String personality = row.optString("personality", Personalities.STEADY);
					if (!isStringValid(personality)) personality = Personalities.STEADY;
					PersonAPI person = Global.getFactory().createPerson();
					member.setCaptain(person);
					person.getStats().setLevel(0);
					person.setPersonality(personality);

					String skills = row.optString("skills", "");
					if (isStringValid(skills)) {

						for (String skill : skills.split(",")) {
							skill = skill.trim();

							if (!person.getStats().hasSkill(skill)) {
								person.getStats().setLevel(person.getStats().getLevel() + 1);
							}

							person.getStats().increaseSkill(skill);
						}

						Global.getSettings().loadTexture("graphics/portraits/portrait_generic.png");
						person.setPortraitSprite("graphics/portraits/portrait_generic.png");
					}

					String portrait = row.optString("portrait", "graphics/portraits/portrait_generic.png");
					if (isStringValid(portrait) && person.getStats().getLevel() > 0) {
						Global.getSettings().loadTexture(portrait);
						person.setPortraitSprite(portrait);
					}

					String orders = row.optString("orders", "");

					Set<String> tags = new HashSet<>();
					String rawTags = row.optString("tags", "");
					if (isStringValid(rawTags)) for (String tag : rawTags.split(",")) {
						tag = tag.trim();
						tags.add(tag);
					}

					AutomationProcessor automationProcessor = AutomationProcessor.loadAutomationScript(orders);
					fleet.fleetMembers.put(member, automationProcessor);
				}
			} catch (IOException | JSONException ex) {
				log.error("unable to read player" + playerNumber + "_fleet.csv");
				log.error(ex);
			}

			return fleet;
		}

		public static Fleet createFactionFleet(int minFP, int factionNumber) {
			Fleet fleet = createSimpleFleet();

			String factionId = chooseFactions(factionNumber);
			CampaignFleetAPI theFleet = FleetFactory.createGenericFleet(factionId, fleet.name, 1, minFP);
			theFleet.getFleetData().sort();
			theFleet.forceSync();

			fleet.fleet = theFleet;

			for (FleetMemberAPI member : theFleet.getFleetData().getMembersListCopy()) {
				if (member.isFighterWing()) {
					continue;
				}

				member.getCaptain().setPersonality(getPersonality(Global.getSector().getFaction(factionId).getDoctrine().getAggression()));
				member.getRepairTracker().setCR(Math.max(member.getRepairTracker().getCR(), member.getRepairTracker().getMaxCR()));
				member.getRepairTracker().setMothballed(false);
				member.getRepairTracker().setCrashMothballed(false);
				fleet.fleetMembers.put(member, null);
			}

			return fleet;
		}

		public void deploy(MissionDefinitionAPI api, FleetSide side) {
			fleet.updateCounts();
			fleet.forceSync();

			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				if (member.getVariant().hasHullMod(HullMods.AUTOMATED)) {
					member.getStats().getMaxCombatReadiness().modifyFlat("auto", -Automated.MAX_CR_PENALTY, "Automated ship penalty");
				}

				member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
				api.addFleetMember(side, member);
			}
		}

		public int getDeploymentCost() {
			fleet.updateCounts();
			fleet.forceSync();

			int maintenance = 0;
			for (FleetMemberAPI s : fleet.getFleetData().getMembersListCopy()) {
				if (s.isFighterWing()) {
					continue;
				}

				int deploy = (int)s.getDeploymentPointsCost();
				log.info(s.getHullSpec().getHullName() + " " + s.getVariant().getHullVariantId() + " Deployment cost = " + deploy);

				maintenance += deploy;
			}

			log.info("TOTAL DEPLOYMENT COST: " + maintenance);
			log.info("____________________________");

			return maintenance;
		}

		public int getWingNum() {
			int maintenance = 0;
			for (FleetMemberAPI s : fleet.getFleetData().getMembersListCopy()) {
				if (s.isFighterWing()) {
					continue;
				}

				//WINGS nums (except built-in ones)
				int wings = 0;
				List<String> builtInWings = s.getHullSpec().getBuiltInWings();
				if (s.getStats().getNumFighterBays().getModifiedValue() > 0) {
					for (int i = 0; i < s.getStats().getNumFighterBays().getModifiedValue(); i++) {
						if (s.getVariant().getWing(i) != null) {
							if (!builtInWings.isEmpty() && builtInWings.contains(s.getVariant().getWingId(i))) {
								//log.info("      "+"Skipping built-in"+s.getVariant().getWing(i).getId());
							} else {
								wings++;
							}
						}
					}
				}

				log.info(s.getHullSpec().getHullName() + " " + s.getVariant().getHullVariantId() + " Wing num = " + wings);
				maintenance += wings;
			}

			log.info("TOTAL WING NUM: " + maintenance);
			log.info("____________________________");

			return maintenance;
		}
	}

	public static SpriteAPI getDefaultSprite() {
		return Global.getSettings().getSprite("misc", "AI_enemy");
	}

	public static SpriteAPI getPlayerSprite(int player) {
		SpriteAPI sprite = getDefaultSprite();
		try {
			String fileName = "graphics/AIB/VSscreen/names/Player" + player + ".png";
			Global.getSettings().loadTexture(fileName);

			sprite = Global.getSettings().getSprite(fileName);
		} catch (Exception ignored) {}

		return sprite;
	}

	public static boolean isStringValid(String string) {
		if (string == null) return false;
		String toCheck = string.trim();
		if (toCheck.isEmpty()) return false;
		if (toCheck.contentEquals("\"")) return false;
		return true;
	}

	////////////////////////////////////
	//                                //
	//        FACTION CREATION        //
	//                                //
	////////////////////////////////////
	private static final List<String> blacklistFactions = new ArrayList<>();
	static {
		blacklistFactions.add("famous_bounty");
		blacklistFactions.add("merc_hostile");
		blacklistFactions.add("knights_of_ludd");
		blacklistFactions.add("poor");
		blacklistFactions.add("sleeper");
		blacklistFactions.add("scavengers");
		blacklistFactions.add("sector");
		blacklistFactions.add("everything");
		blacklistFactions.add("domain");
		blacklistFactions.add("ML_bounty");
	}

	public static String chooseFactions(int n) {
		List<FactionAPI> acceptableFactions = new ArrayList<>();
		for (FactionAPI faction : Global.getSector().getAllFactions()) {
			if (faction.isNeutralFaction()) continue;
			if (faction.isPlayerFaction()) continue;
			if (blacklistFactions.contains(faction.getId())) continue;

			acceptableFactions.add(faction);
		}
		return acceptableFactions.get(n % acceptableFactions.size()).getId();
	}

	private static String getPersonality(int agg) {
		if (Math.random() < 0.2f) agg += 1;
		if (Math.random() < 0.2f) agg -= 1;
		switch (agg) {
			case 6:
				return Personalities.RECKLESS;
			case 5:
				return Personalities.RECKLESS;
			case 4:
				return Math.random() < 0.5 ? Personalities.RECKLESS : Personalities.AGGRESSIVE;
			case 3:
				return Personalities.AGGRESSIVE;
			case 2:
				return Personalities.STEADY;
			case 1:
				return Personalities.CAUTIOUS;
			default:
				return Personalities.TIMID;
		}
	}

	////////////////////////////////////
	//                                //
	//     BATTLESPACE CREATION       //
	//                                //
	////////////////////////////////////

	/**
	 * @param api Mission api
	 * @param map Map index
	 */

	public static void setBattleSpace(MissionDefinitionAPI api, int map) {
		setBattleSpace(api, map, 1);
	}

	public static int MAP_COUNT = 12;

	public static void setBattleSpace(MissionDefinitionAPI api, int map, float sizeMult) {
		float WIDTH = sizeMult, HEIGHT = sizeMult;
		log.info("Map number: " + map);

		//select the relevant battlescape
		switch (map) {
			case 0:
				WIDTH *= 8000;
				HEIGHT *= 8000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(0, 0, "comm_relay");

				api.addNebula(0.25f * WIDTH, 0.25f * HEIGHT, 500);
				api.addNebula(-0.25f * WIDTH, 0.25f * HEIGHT, 500);
				api.addNebula(0.25f * WIDTH, -0.25f * HEIGHT, 500);
				api.addNebula(-0.25f * WIDTH, -0.25f * HEIGHT, 250);

				api.addAsteroidField(0, 0, 0, 500, 10, 15, 15);
				break;
			case 1:
				WIDTH *= 12000;
				HEIGHT *= 8000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(-0.25f * WIDTH, 0.05f * HEIGHT, "sensor_array");
				api.addObjective(0.25f * WIDTH, -0.05f * HEIGHT, "nav_buoy");
				api.addObjective(0.25f * WIDTH, 0.05f * HEIGHT, "sensor_array");
				api.addObjective(-0.25f * WIDTH, -0.05f * HEIGHT, "nav_buoy");
				api.addObjective(-0.25f * WIDTH, 0, "comm_relay");
				api.addObjective(0.25f * WIDTH, 0, "comm_relay");

				api.addAsteroidField(-0.4f * WIDTH, 0, 90, 1000, 20, 25, 40);
				api.addAsteroidField(0.4f * WIDTH, 0, -90, 1000, 20, 25, 40);
				break;
			case 2:
				WIDTH *= 12000;
				HEIGHT *= 9000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(-0.25f * WIDTH, -0f * HEIGHT, "comm_relay");
				api.addObjective(0.25f * WIDTH, 0f * HEIGHT, "comm_relay");
				api.addObjective(-0.25f * WIDTH, 0.05f * HEIGHT, "nav_buoy");
				api.addObjective(-0.25f * WIDTH, -0.05f * HEIGHT, "nav_buoy");
				api.addObjective(-0.3f * WIDTH, 0f * HEIGHT, "nav_buoy");
				api.addObjective(0.25f * WIDTH, 0.05f * HEIGHT, "sensor_array");
				api.addObjective(0.25f * WIDTH, -0.05f * HEIGHT, "sensor_array");
				api.addObjective(0.3f * WIDTH, 0f * HEIGHT, "sensor_array");

				api.addNebula(0.3f * WIDTH, 0.4f * HEIGHT, 600);
				api.addNebula(-0.3f * WIDTH, 0.4f * HEIGHT, 600);
				api.addNebula(0.3f * WIDTH, -0.4f * HEIGHT, 600);
				api.addNebula(-0.3f * WIDTH, -0.4f * HEIGHT, 600);
				api.addNebula(0.3f * WIDTH, 0.2f * HEIGHT, 1000);
				api.addNebula(-0.3f * WIDTH, 0.2f * HEIGHT, 1000);
				api.addNebula(0.3f * WIDTH, -0.2f * HEIGHT, 1000);
				api.addNebula(-0.3f * WIDTH, -0.2f * HEIGHT, 1000);
				api.addNebula(0.3f * WIDTH, 0, 1500);
				api.addNebula(-0.3f * WIDTH, 0, 1500);
				break;
			case 3:
				WIDTH *= 11000;
				HEIGHT *= 11000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(-0.05f * WIDTH, 0.05f * HEIGHT, "sensor_array");
				api.addObjective(0.05f * WIDTH, 0.05f * HEIGHT, "nav_buoy");
				api.addObjective(0.05f * WIDTH, -0.05f * HEIGHT, "sensor_array");
				api.addObjective(-0.05f * WIDTH, -0.05f * HEIGHT, "nav_buoy");
				api.addObjective(0, -0.05f * HEIGHT, "comm_relay");
				api.addObjective(0, 0.05f * HEIGHT, "comm_relay");

				for (int x = 0; x < 10; x++) {
					Vector2f pos = MathUtils.getRandomPointInCircle(new Vector2f(), 3000);
					float rad = ((float) Math.random() + 0.5f) * 400;
					api.addNebula(pos.x, pos.y, rad);
				}
				break;
			case 4:
				WIDTH *= 10000;
				HEIGHT *= 12000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(0, -0.25f * HEIGHT, "comm_relay");
				api.addObjective(0, 0.25f * HEIGHT, "comm_relay");
				api.addObjective(-0.05f * WIDTH, -0.25f * HEIGHT, "nav_buoy");
				api.addObjective(0.05f * WIDTH, 0.25f * HEIGHT, "nav_buoy");
				api.addObjective(-0.05f * WIDTH, 0.25f * HEIGHT, "sensor_array");
				api.addObjective(0.05f * WIDTH, -0.25f * HEIGHT, "sensor_array");

				api.addNebula(0.05f * WIDTH, 0.3f * HEIGHT, 500);
				api.addNebula(-0.05f * WIDTH, 0.3f * HEIGHT, 500);
				api.addNebula(0.05f * WIDTH, -0.3f * HEIGHT, 500);
				api.addNebula(-0.05f * WIDTH, -0.3f * HEIGHT, 500);
				break;
			case 5:
				WIDTH *= 14000;
				HEIGHT *= 6000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(-0.25f * WIDTH, 0f, "sensor_array");
				api.addObjective(0.25f * WIDTH, 0f, "sensor_array");

				api.addNebula(0f, 0f, 500);
				api.addNebula(0.15f * WIDTH, 0f, 300);
				api.addNebula(-0.15f * WIDTH, 0f, 300);
				break;

			//side spawning maps

			case 6:
				WIDTH *= 12000;
				HEIGHT *= 12000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(0, 0, "sensor_array");
				api.addObjective(-0.25f * WIDTH, -0.25f * HEIGHT, "nav_buoy");
				api.addObjective(0.25f * WIDTH, 0.25f * HEIGHT, "nav_buoy");
				api.addObjective(-0.25f * WIDTH, 0.25f * HEIGHT, "sensor_array");
				api.addObjective(0.25f * WIDTH, -0.25f * HEIGHT, "sensor_array");

				api.addNebula(-0.25f * WIDTH, -0.25f * HEIGHT, 500);
				api.addNebula(0.25f * WIDTH, 0.25f * HEIGHT, 500);
				api.addNebula(-0.25f * WIDTH, 0.25f * HEIGHT, 500);
				api.addNebula(0.25f * WIDTH, -0.25f * HEIGHT, 500);
				break;
			case 7:
				WIDTH *= 10000;
				HEIGHT *= 16000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(0, 0, "nav_buoy");
				api.addObjective(-0.2f * WIDTH, -0.35f * HEIGHT, "comm_relay");
				api.addObjective(0.2f * WIDTH, 0.35f * HEIGHT, "comm_relay");

				api.addAsteroidField(0, 0, 30, 1600, 20, 25, 40);
				api.addAsteroidField(0, 0, -30, 1600, 20, 25, 40);

				api.addNebula(0, -0.45f * HEIGHT, 2000);
				api.addNebula(0.25f * WIDTH, -0.55f * HEIGHT, 2500);
				api.addNebula(-0.25f * WIDTH, -0.55f * HEIGHT, 2500);
				api.addNebula(0.5f * WIDTH, -0.4f * HEIGHT, 2000);
				api.addNebula(-0.5f * WIDTH, -0.4f * HEIGHT, 2000);
				api.addNebula(0, 0.45f * HEIGHT, 2000);
				api.addNebula(0.25f * WIDTH, 0.55f * HEIGHT, 2500);
				api.addNebula(-0.25f * WIDTH, 0.55f * HEIGHT, 2500);
				api.addNebula(0.5f * WIDTH, 0.4f * HEIGHT, 2000);
				api.addNebula(-0.5f * WIDTH, 0.4f * HEIGHT, 2000);
				break;
			case 8:
				WIDTH *= 16000;
				HEIGHT *= 12000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(0, 0, "comm_relay");
				api.addObjective(-0.15f * WIDTH, 0f * HEIGHT, "nav_buoy");
				api.addObjective(0.15f * WIDTH, 0f * HEIGHT, "nav_buoy");
				api.addObjective(-0.35f * WIDTH, 0f * HEIGHT, "sensor_array");
				api.addObjective(0.35f * WIDTH, 0f * HEIGHT, "sensor_array");
				break;
			case 9:
				WIDTH *= 16000;
				HEIGHT *= 14000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(-0.25f * WIDTH, 0.15f * HEIGHT, "nav_buoy");
				api.addObjective(0.25f * WIDTH, -0.15f * HEIGHT, "nav_buoy");
				api.addObjective(-0.25f * WIDTH, -0.15f * HEIGHT, "sensor_array");
				api.addObjective(0.25f * WIDTH, 0.15f * HEIGHT, "sensor_array");
				break;
			case 10:
				WIDTH *= 16000;
				HEIGHT *= 16000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(0, 0.35f * HEIGHT, "nav_buoy");
				api.addObjective(0, -0.35f * HEIGHT, "nav_buoy");
				api.addObjective(0 * WIDTH, 0, "sensor_array");
				api.addObjective(0.15f * WIDTH, 0, "sensor_array");
				api.addObjective(-0.15f * WIDTH, 0, "sensor_array");

				api.addAsteroidField(0, 0, 60, 2000, 20, 25, 40);
				api.addAsteroidField(0, 0, -60, 2000, 20, 25, 40);

				api.addNebula(0, 0, 3200);
				api.addNebula(0, -0.25f * HEIGHT, 1200);
				api.addNebula(0, 0.25f * HEIGHT, 1200);
				break;
			case 11:
				WIDTH *= 18000;
				HEIGHT *= 14000;
				api.initMap(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

				api.addObjective(-0.05f * WIDTH, 0.15f * HEIGHT, "nav_buoy");
				api.addObjective(0.05f * WIDTH, 0.15f * HEIGHT, "sensor_array");
				api.addObjective(-0.05f * WIDTH, -0.15f * HEIGHT, "sensor_array");
				api.addObjective(0.05f * WIDTH, -0.15f * HEIGHT, "nav_buoy");
				break;
		}
	}

	////////////////////////////////////
	//                                //
	//     FORCED FLEET SPAWNING      //
	//                                //
	////////////////////////////////////

	/**
	 * @param engine          Combat Engine.
	 * @param side            FleetSide to deploy.
	 * @param mapX            Map width.
	 * @param mapY            Map height.
	 * @param suppressMessage Suppress UI spawning message on the side.
	 */

	public static void forcedSpawn(CombatEngineAPI engine, FleetSide side, float mapX, float mapY, boolean suppressMessage) {

		if (suppressMessage) {
			engine.getFleetManager(side).setSuppressDeploymentMessages(true);
		}

		if (engine.getFleetManager(side).getReservesCopy().size() > 0) {

			//start by the middle
			float angle = -90, spawnX = 0, spawnY = mapY / 2;

			//reverse for player side
			if (side == FleetSide.PLAYER) {
				spawnY *= -1;
				angle *= -1;
			}

			for (FleetMemberAPI member : engine.getFleetManager(side).getReservesCopy()) {
				//ignore fighter wings
				if (member.isFighterWing()) {
					continue;
				}

				//spawn location
				Vector2f loc = new Vector2f(spawnX, spawnY);

				//add ship
				ShipAPI ship = engine.getFleetManager(side).spawnFleetMember(member, loc, angle, 3);
				if (ship.getCaptain().getStats().hasSkill(Skills.COMBAT_ENDURANCE)) ship.setCurrentCR(0.85f);
				log.info("Spawning " + side.name() + "'s " + member.getHullId() + " at " + (int) spawnX + "x" + (int) spawnY);

				//set new location
				if (spawnX > 0) {
					//switch to the left
					spawnX *= -1;
				} else {
					//switch back to the right
					spawnX *= -1;
					//add offset
					spawnX += 800;
				}
				if (spawnX >= mapX / 4) {
					//if the line of ships is too wide, get back to the center and a row behind
					spawnX = 0;

					//reverse for player side
					if (side == FleetSide.PLAYER) {
						spawnY -= 650;
					} else {
						spawnY += 650;
					}
				}
			}
		}

		if (suppressMessage) {
			engine.getFleetManager(side).setSuppressDeploymentMessages(false);
		}
	}

	public static void screenSpace(SpriteAPI sprite, MagicRender.positioning pos, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, Color color, boolean additive, float fadein, float full, float fadeout) {
		ViewportAPI screen = Global.getCombatEngine().getViewport();
		Vector2f ratio = size;
		Vector2f screenSize = new Vector2f(screen.getVisibleWidth(), screen.getVisibleHeight());
		if (pos == MagicRender.positioning.STRETCH_TO_FULLSCREEN) {
			sprite.setSize(screenSize.x, screenSize.y);
		} else if (pos == MagicRender.positioning.FULLSCREEN_MAINTAIN_RATIO) {
			if (size.x / size.y > screenSize.x / screenSize.y) {
				ratio = new Vector2f(size.x / size.y / (screenSize.x / screenSize.y), 1f);
			} else {
				ratio = new Vector2f(1f, size.y / size.x / (screenSize.y / screenSize.x));
				sprite.setSize(Global.getCombatEngine().getViewport().getVisibleWidth() * ratio.x, Global.getCombatEngine().getViewport().getVisibleHeight() * ratio.y);
			}
		} else {
			sprite.setSize(size.x * screen.getViewMult(), size.y * screen.getViewMult());
		}

		sprite.setAngle(angle);
		sprite.setColor(color);
		if (additive) {
			sprite.setAdditiveBlend();
		}

		Vector2f velocity = new Vector2f(vel);
		MagicRenderPlugin.addScreenspace(sprite, pos, loc, velocity, ratio, growth, spin, fadein, fadein + full, fadein + full + fadeout, CombatEngineLayers.JUST_BELOW_WIDGETS);
	}

	public static ShipVariantAPI loadVariant(String variantId, boolean forceUpdate) {
		if (!forceUpdate && Global.getSettings().getVariant(variantId) != null) {
			return Global.getSettings().getVariant(variantId).clone();
		}

		try {
			JSONObject json = Global.getSettings().loadJSON("data/variants/" + variantId + ".variant");
			if (json != null) {
				String hullId = json.optString("hullId", null);

				ShipVariantAPI variant = Global.getSettings().createEmptyVariant(variantId, Global.getSettings().getHullSpec(hullId));
				variant.setVariantDisplayName(json.optString("displayName", "Empty"));
				variant.setNumFluxCapacitors(json.optInt("fluxCapacitors", 0));
				variant.setNumFluxVents(json.optInt("fluxVents", 0));

				JSONArray hullmods = json.optJSONArray("hullMods");
				if (hullmods != null) {
					for (int i = 0; i < hullmods.length(); i++) {
						variant.addMod(hullmods.getString(i));
					}
				}

				hullmods = json.optJSONArray("permaMods");
				if (hullmods != null) {
					for (int i = 0; i < hullmods.length(); i++) {
						variant.addPermaMod(hullmods.getString(i));
					}
				}

				for (WeaponGroupSpec group : variant.getWeaponGroups()) {
					WeaponGroupSpec clone = group.clone();
					for (String slot : clone.getSlots()) {
						group.removeSlot(slot);
					}
				}

				JSONArray weapons = json.optJSONArray("weaponGroups");
				if (weapons != null) {
					int size = variant.getWeaponGroups().size();
					for (int i = 0; i < weapons.length(); i++) {
						JSONObject weaponGroupJSON = weapons.optJSONObject(i);
						WeaponGroupSpec weaponGroup;
						if (i < size) {
							weaponGroup = variant.getWeaponGroups().get(i);
						} else {
							weaponGroup = new WeaponGroupSpec();
						}
						weaponGroup.setType(weaponGroupJSON.optString("mode", "ALTERNATING").contentEquals("ALTERNATING") ? WeaponGroupType.ALTERNATING : WeaponGroupType.LINKED);
						weaponGroup.setAutofireOnByDefault(weaponGroupJSON.optBoolean("autofire", false));

						JSONObject weaponList = weaponGroupJSON.optJSONObject("weapons");
						if (weaponList != null) {
							Iterator<String> iterator = weaponList.keys();
							while (iterator.hasNext()) {
								String slot = iterator.next();
								weaponGroup.addSlot(slot);
								variant.addWeapon(slot, weaponList.optString(slot));
							}
						}

						if (i >= size) {
							variant.addWeaponGroup(weaponGroup);
						}
					}
				}

				JSONArray wings = json.optJSONArray("wings");
				if (wings != null) {
					for (int i = 0; i < wings.length(); i++) {
						variant.setWingId(i, wings.getString(i));
					}
				}

				JSONArray modules = json.optJSONArray("modules");
				if (modules != null) {
					for (int i = 0; i < modules.length(); i++) {
						JSONObject module = modules.optJSONObject(i);
						if (module != null) {
							String slotModule = (String)module.keys().next();
							String moduleVariantId = module.optString(slotModule);
							if ((moduleVariantId != null) && (!moduleVariantId.isEmpty())) {
								ShipVariantAPI modulevariant = Global.getSettings().getVariant(moduleVariantId);
								if (modulevariant == null) {
									modulevariant = loadVariant(moduleVariantId, forceUpdate);
								}
								if (modulevariant != null) {
									variant.setModuleVariant(slotModule, modulevariant);
								}
							}
						}
					}
				}

				variant.setGoalVariant(false);
				return variant;
			}

		} catch (IOException | JSONException exception) {
			exception.printStackTrace();
			throw new RuntimeException("Wrong(JSON ERROR) with id:" + variantId);
		} catch (RuntimeException exception) {
			//exception.printStackTrace();
			Global.getLogger(AI_missionUtils.class).warn("Not found in standard place with id:" + variantId);
		}

		try {
			return Global.getSettings().getVariant(variantId).clone();
		} catch (Exception exception) {
			exception.printStackTrace();
			throw new RuntimeException("Wrong(NOT FOUND) with id:" + variantId);
		}
	}

	public static class EmptyAdmiral implements AdmiralAIPlugin {

		@Override
		public void preCombat() {

		}

		@Override
		public void advance(float amount) {

		}
	}
}