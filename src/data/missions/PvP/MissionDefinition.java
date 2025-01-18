package data.missions.PvP;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import data.missions.scripts.AI_missionUtils;
import data.missions.scripts.AI_missionUtils.EmptyAdmiral;
import data.missions.scripts.AI_missionUtils.Fleet;
import data.missions.scripts.AI_missionUtils.Match;
import data.missions.scripts.automation.AutomationController;
import data.scripts.plugins.AI_freeCamPlugin;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MissionDefinition implements MissionDefinitionPlugin {

	private static final Logger log = Global.getLogger(MissionDefinition.class);

	private static int globalCurrentMatch, playerIndex, enemyIndex;
	private static boolean firstEnteringMission = true;

	private static final String PATH = "tournament/";
	private static final String ROUND_MATCHES = PATH + "PvP_matches.csv";
	private static final Map<Integer, Match> MATCHES = new HashMap<>();

	@Override
	public void defineMission(MissionDefinitionAPI api) {

		String music = Global.getSoundPlayer().getCurrentMusicId();
		if (!music.contentEquals("miscallenous_main_menu.ogg")) {
			Global.getSoundPlayer().playCustomMusic(1, 1, null);
		}

		api.getContext().aiRetreatAllowed = false;
		api.getContext().fightToTheLast = true;

		//read round_matches.csv
		MATCHES.clear();
		try {
			JSONArray matches = Global.getSettings().getMergedSpreadsheetDataForMod("player", ROUND_MATCHES, "aibattles");
			for (int i = 0; i < matches.length(); i++) {
				JSONObject row = matches.getJSONObject(i);
				playerIndex = row.getInt("player");
				enemyIndex = row.getInt("enemy");
				int map = row.optInt("map", -1);
				float size = (float)row.optDouble("size", 1f);

				MATCHES.put(2 * i, Match.createPVP(playerIndex, enemyIndex, map, size));
				MATCHES.put(2 * i + 1, Match.createPVP(enemyIndex, playerIndex, map, size));
			}
		} catch (IOException | JSONException ex) {
			log.error("unable to read round_matches.csv");
		}

		//cycle the matches while holding space
		if (firstEnteringMission) {
			firstEnteringMission = false;
			globalCurrentMatch = 0;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			globalCurrentMatch++;
			globalCurrentMatch %= MATCHES.size();
		} else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			globalCurrentMatch--;
			globalCurrentMatch += MATCHES.size();
			globalCurrentMatch %= MATCHES.size();
		}

		Match currentMatch = MATCHES.get(globalCurrentMatch);

		playerIndex = Math.round(currentMatch.sidePlayer);
		Fleet playerFleet = Fleet.createFleet(PATH, playerIndex);
		api.initFleet(FleetSide.PLAYER, "", FleetGoal.ATTACK, playerFleet.hasTag("default_ai"), 1);
		api.setFleetTagline(FleetSide.PLAYER, playerFleet.getName());
		playerFleet.deploy(api, FleetSide.PLAYER);

		enemyIndex = Math.round(currentMatch.sideEnemy);
		Fleet enemyFleet = Fleet.createFleet(PATH, enemyIndex);
		api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, enemyFleet.hasTag("default_ai"), 1);
		api.setFleetTagline(FleetSide.ENEMY, enemyFleet.getName());
		enemyFleet.deploy(api, FleetSide.ENEMY);

		int playerCost = playerFleet.getDeploymentCost();
		int playerWingNum = playerFleet.getWingNum();
		int enemyCost = enemyFleet.getDeploymentCost();
		int enemyWingNum = enemyFleet.getWingNum();

		api.addBriefingItem("Green fleet deployment cost : " + playerCost + ", wing num : " + playerWingNum);
		api.addBriefingItem("Red fleet deployment cost : " + enemyCost + ", wing num : " + enemyWingNum);

		//set the terrain
		int seed = currentMatch.mapNumber;
		if (seed < 0) seed = (MissionDefinition.globalCurrentMatch / 2) * playerCost * enemyCost + playerWingNum + enemyWingNum + playerCost * enemyWingNum + playerWingNum * enemyCost;
		seed %= AI_missionUtils.MAP_COUNT;

		float sizeMult = currentMatch.sizeMult;
		AI_missionUtils.setBattleSpace(api, seed, sizeMult);

		//add the price check and anti-retreat plugin
		api.addPlugin(new Plugin(playerFleet, enemyFleet));
	}

	public final static class Plugin extends BaseEveryFrameCombatPlugin {

		private CombatEngineAPI engine = null;
		private final Fleet playerFleet;
		private final Fleet enemyFleet;

		public Plugin(Fleet player, Fleet enemy) {
			this.playerFleet = player;
			this.enemyFleet = enemy;
		}

		private float timer = 0f;
		private boolean initialFleetDeployed = false;
		private boolean initialPlayerShown = false;
		private boolean initialSideShown = false;
		private boolean initialSoundPlay = false;
		private boolean revealMap = false;

		private int winner = -1;
		private boolean shouldShowEnd = false;
		private boolean shouldShameIfEnd = false;

		private float mapX;
		private float mapY;

		@Override
		public void init(CombatEngineAPI engine) {

			////////////////////////////////////
			//                                //
			//    CAMERA AND TIME CONTROLS    //
			//                                //
			////////////////////////////////////
			this.engine = engine;
			engine.getContext().aiRetreatAllowed = false;
			engine.getContext().fightToTheLast = true;

			EveryFrameCombatPlugin freeCam = new AI_freeCamPlugin();
			engine.addPlugin(freeCam);

			engine.setMaxFleetPoints(FleetSide.ENEMY, 9999);
			engine.setMaxFleetPoints(FleetSide.PLAYER, 9999);

			mapX = engine.getMapWidth();
			mapY = engine.getMapHeight();

			engine.getCustomData().put(AutomationController.DATA_KEY_PLAYER, playerFleet);
			engine.getCustomData().put(AutomationController.DATA_KEY_ENEMY, enemyFleet);

			engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).getCommandPointsStat().modifyFlat("default_ai", 9999);
			engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).getCommandPointsStat().modifyFlat("default_ai", 9999);
			engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).getCommandPointsStat().modifyFlat("default_ai", 9999);

			engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setPreventFullRetreat(true);
			engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setPreventFullRetreat(true);
			engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setPreventFullRetreat(true);

			engine.getFleetManager(FleetSide.PLAYER).setCanForceShipsToEngageWhenBattleClearlyLost(true);
			engine.getFleetManager(FleetSide.ENEMY).setCanForceShipsToEngageWhenBattleClearlyLost(true);

			if (!playerFleet.hasTag("default_ai")) {
				engine.getFleetManager(FleetSide.PLAYER).setAdmiralAI(new EmptyAdmiral());
			}

			if (!enemyFleet.hasTag("default_ai")) {
				engine.getFleetManager(FleetSide.ENEMY).setAdmiralAI(new EmptyAdmiral());
			}

			if (playerFleet.hasTag("full_assault")) {
				engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setFullAssault(true);
				engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(true);
			}

			if (enemyFleet.hasTag("full_assault")) {
				engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setFullAssault(true);
			}
		}

		SpriteAPI playerP = AI_missionUtils.getPlayerSprite(playerIndex);
		SpriteAPI playerE = AI_missionUtils.getPlayerSprite(enemyIndex);
		SpriteAPI playerPF = AI_missionUtils.getPlayerSprite(playerIndex);
		SpriteAPI playerEF = AI_missionUtils.getPlayerSprite(enemyIndex);

		////////////////////////////////////
		//                                //
		//         ADVANCE PLUGIN         //
		//                                //
		////////////////////////////////////

		@Override
		public void advance(float amount, List<InputEventAPI> events) {

			if (engine == null) return;

			////////////////////////////////////
			//                                //
			//          FORCED SPAWN          //
			//                                //
			////////////////////////////////////

			if (!initialFleetDeployed) {

				initialFleetDeployed = true;

				log.info("Map size: " + (int) mapX + "x" + (int) mapY);
				AI_missionUtils.forcedSpawn(engine, FleetSide.PLAYER, mapX, mapY, true);
				AI_missionUtils.forcedSpawn(engine, FleetSide.ENEMY, mapX, mapY, true);

				Global.getSoundPlayer().playCustomMusic(1, 1, null);
				String music = playerFleet.getTagStartsWith("music");
				if (music != null) {
					Pattern PATTERN = Pattern.compile("^(\\w+)\\(([^\\)]+)\\)$");
					Matcher matcher = PATTERN.matcher(music);
					if (matcher.matches()) {
						String musicId = matcher.group(2).split(",\\s*")[0];
						Global.getSoundPlayer().playCustomMusic(1, 1, musicId, true);
					}
				}

				return;
			}

			////////////////////////////////////
			//                                //
			//         VERSUS SCREEN          //
			//                                //
			////////////////////////////////////

			if (!initialPlayerShown) {
				if (engine.getTotalElapsedTime(false) > 1f) {
					initialPlayerShown = true;
					SpriteAPI VS = Global.getSettings().getSprite("misc", "AI_versus");
					AI_missionUtils.screenSpace(VS, MagicRender.positioning.CENTER, new Vector2f(0, VS.getHeight() * 0.25f), new Vector2f(0, VS.getHeight() * -0.1f), new Vector2f(VS.getWidth(), VS.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, false, 0f, 3.2f, 0.2f);
					SpriteAPI VSF = Global.getSettings().getSprite("misc", "AI_versusF");
					AI_missionUtils.screenSpace(VSF, MagicRender.positioning.CENTER, new Vector2f(0, VSF.getHeight() * 0.25f), new Vector2f(0, VSF.getHeight() * -0.1f), new Vector2f(VSF.getWidth(), VSF.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, true, 0f, 0f, 0.5f);

					AI_missionUtils.screenSpace(playerP, MagicRender.positioning.CENTER, new Vector2f(playerP.getWidth() * 1.2f, 0), new Vector2f(playerP.getWidth() * -0.03f, 0), new Vector2f(playerP.getWidth(), playerP.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, 0f, 3f, 0.2f);
					AI_missionUtils.screenSpace(playerE, MagicRender.positioning.CENTER, new Vector2f(playerE.getWidth() * -1.2f, 0), new Vector2f(playerE.getWidth() * 0.03f, 0), new Vector2f(playerE.getWidth(), playerE.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, 0f, 3f, 0.2f);
				} else if (engine.getTotalElapsedTime(false) > 0.5f) {
					float time = 1f - engine.getTotalElapsedTime(false);

					AI_missionUtils.screenSpace(playerPF, MagicRender.positioning.CENTER, new Vector2f(playerPF.getWidth() * 1.2f + (time * 3000), 0), new Vector2f(-10, 0), new Vector2f(playerPF.getWidth(), playerPF.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, -1f, -1f, -1f);
					AI_missionUtils.screenSpace(playerEF, MagicRender.positioning.CENTER, new Vector2f(playerEF.getWidth() * -1.2f - (time * 3000), 0), new Vector2f(10, 0), new Vector2f(playerEF.getWidth(), playerEF.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, -1f, -1f, -1f);
					if (!initialSoundPlay) {
						initialSoundPlay = true;
						Global.getSoundPlayer().playUISound("AIB_versusS", 1, 1);
					}
				}
			} else if (engine.getTotalElapsedTime(false) > 4f) {

				if (!initialSideShown) {
					initialSideShown = true;

					float screenWidth = Global.getSettings().getScreenWidth();

					float width = playerP.getWidth();
					float height = playerP.getHeight();

					width = 360f;
					height = 80f;

					Vector2f positionU = new Vector2f(-screenWidth * 0.5f + width * 0.25f, -height * 0.5f);
					AI_missionUtils.screenSpace(playerP, MagicRender.positioning.CENTER, positionU, new Vector2f(0, 0), new Vector2f(width * 0.5f, height * 0.5f), new Vector2f(), 0, 0, Color.GREEN, false, 1f, 999999f, 1f);

					width = playerE.getWidth();
					height = playerE.getHeight();

					width = 360f;
					height = 80f;

					Vector2f positionD = new Vector2f(-screenWidth * 0.5f + width * 0.25f, height * 0.5f);
					AI_missionUtils.screenSpace(playerE, MagicRender.positioning.CENTER, positionD, new Vector2f(0, 0), new Vector2f(width * 0.5f, height * 0.5f), new Vector2f(), 0, 0, Color.PINK, false, 1f, 999999f, 1f);
				}
			}

			if (engine.isPaused()) return;

			////////////////////////////////////
			//                                //
			//       PRE-ORDER PROCESS        //
			//                                //
			////////////////////////////////////
			AutomationController.processAll(engine);


			////////////////////////////////////
			//                                //
			//         ANTI-CR BATTLE         //
			//                                //
			////////////////////////////////////

			if (engine.getTotalElapsedTime(false) > 500f) {
				if (!revealMap) {
					revealMap = true;

					//TIMEOUT screen
					SpriteAPI timeOut = Global.getSettings().getSprite("misc", "AI_timeout");
					AI_missionUtils.screenSpace(timeOut, MagicRender.positioning.CENTER, new Vector2f(0, 200), new Vector2f(), new Vector2f(timeOut.getWidth(), timeOut.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, false, 0.25f, 3f, 1f);

					//all ships reckless
					for (ShipAPI ship : engine.getShips()) {
						if (!ship.isAlive()) continue;
						if (ship.getCaptain() == null) continue;
						ship.getCaptain().setPersonality(Personalities.RECKLESS);
					}

					//both sides full assault
					engine.getFleetManager(FleetSide.PLAYER).getTaskManager(false).setFullAssault(true);
					engine.getFleetManager(FleetSide.PLAYER).getTaskManager(true).setFullAssault(true);
					engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setFullAssault(true);
				}

				//map reveal to all
				engine.getFogOfWar(1).revealAroundPoint(engine, 0, 0, 90000);
				engine.getFogOfWar(0).revealAroundPoint(engine, 0, 0, 90000);
			}

			////////////////////////////////////
			//                                //
			//         TICKING CLOCK          //
			//                                //
			////////////////////////////////////

			int clock = (int)engine.getTotalElapsedTime(false);
			engine.maintainStatusForPlayerShip("clock", null, "Timer", clock + " seconds", !shouldShowEnd);

			////////////////////////////////////
			//                                //
			//         VICTORY SCREEN         //
			//                                //
			////////////////////////////////////


			timer += engine.getElapsedInLastFrame();
			if (timer >= 1f && !shouldShowEnd) {
				timer -= 1f;
				int playerAlive = 0, enemyAlive = 0, flawless = 0;

				//check for members alive
				for (FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDeployedCopy()) {
					if (!m.isFighterWing()) {
						playerAlive++;
					}
				}

				for (FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy()) {
					if (!m.isFighterWing()) {
						enemyAlive++;
					}
				}

				if (playerAlive == 0) {
					//player dead
					shouldShowEnd = true;
					winner = enemyIndex;
					//check for enemy losses
					for (FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDisabledCopy()) {
						if (!m.isFighterWing()) {
							flawless++;
						}
					}
					for (FleetMemberAPI m : engine.getFleetManager(FleetSide.ENEMY).getDestroyedCopy()) {
						if (!m.isFighterWing()) {
							flawless++;
						}
					}
					//no loss? Shaming defeat
					if (flawless < 1) {
						shouldShameIfEnd = true;
					}
				} else if (enemyAlive == 0) {
					shouldShowEnd = true;
					winner = playerIndex;
					for (FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDisabledCopy()) {
						if (!m.isFighterWing()) {
							flawless++;
						}
					}
					for (FleetMemberAPI m : engine.getFleetManager(FleetSide.PLAYER).getDestroyedCopy()) {
						if (!m.isFighterWing()) {
							flawless++;
						}
					}
					if (flawless < 1) {
						shouldShameIfEnd = true;
					}
				}
			}

			if (shouldShowEnd) {
				boolean GE = false;
				/*
				for (ShipAPI ship : engine.getShips()) {
					if (ship.getName() != null && ship.getName().contentEquals("?") && ship.isAlive()) {
						GE = true;
					}
				}
				 */

				if (winner > -1) {
					//display winner
					SpriteAPI playerW = AI_missionUtils.getPlayerSprite(winner);
					AI_missionUtils.screenSpace(playerW, MagicRender.positioning.CENTER, new Vector2f(0, -50), new Vector2f(0, 5), new Vector2f(playerW.getWidth(), playerW.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, false, 0.25f, 10f, 1f);
					SpriteAPI playerW2 = AI_missionUtils.getPlayerSprite(winner);
					AI_missionUtils.screenSpace(playerW2, MagicRender.positioning.CENTER, new Vector2f(0, -50), new Vector2f(0, 5), new Vector2f(playerW2.getWidth(), playerW2.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, true, 0f, 0f, 1f);

					//play sound
					if (GE) {
						Global.getSoundPlayer().playUISound("explosion_ship", 1, 1);
					} else if (shouldShameIfEnd) {
						Global.getSoundPlayer().playUISound("AIB_shameS", 1, 1);
					} else {
						Global.getSoundPlayer().playUISound("AIB_victoryS", 1, 1);
					}

					//reset winner
					winner = -2;
				}

				if (winner < -1 && timer > 0.5f) {
					winner = -1;
					if (GE) {
						//diplay "END"
						SpriteAPI shame = Global.getSettings().getSprite("misc", "AI_end");
						AI_missionUtils.screenSpace(shame, MagicRender.positioning.CENTER, new Vector2f(0, 0), new Vector2f(0, 0), new Vector2f(shame.getWidth(), shame.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, false, 0f, 10f, 1f);

						SpriteAPI shameF = Global.getSettings().getSprite("misc", "AI_endF");
						AI_missionUtils.screenSpace(shameF, MagicRender.positioning.CENTER, new Vector2f(0, 0), new Vector2f(0, 0), new Vector2f(shameF.getWidth(), shameF.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, true, 0f, 0f, 0.5f);
					} else if (shouldShameIfEnd) {
						//diplay "SHAMED"
						SpriteAPI shame = Global.getSettings().getSprite("misc", "AI_shame");
						AI_missionUtils.screenSpace(shame, MagicRender.positioning.CENTER, new Vector2f(0, 0), new Vector2f(0, 0), new Vector2f(shame.getWidth(), shame.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, false, 0f, 10f, 1f);

						SpriteAPI shameF = Global.getSettings().getSprite("misc", "AI_shameF");
						AI_missionUtils.screenSpace(shameF, MagicRender.positioning.CENTER, new Vector2f(0, 0), new Vector2f(0, 0), new Vector2f(shameF.getWidth(), shameF.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, true, 0f, 0f, 0.5f);
					} else {
						//diplay "WINS"
						SpriteAPI win = Global.getSettings().getSprite("misc", "AI_win");
						AI_missionUtils.screenSpace(win, MagicRender.positioning.CENTER, new Vector2f(0, 120), new Vector2f(0, -3), new Vector2f(win.getWidth(), win.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, false, 0f, 10f, 1f);
						SpriteAPI winF = Global.getSettings().getSprite("misc", "AI_winF");
						AI_missionUtils.screenSpace(winF, MagicRender.positioning.CENTER, new Vector2f(0, 140), new Vector2f(0, -3), new Vector2f(winF.getWidth(), winF.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, true, 0f, 0f, 0.5f);
					}
				}
			}
		}
	}
}