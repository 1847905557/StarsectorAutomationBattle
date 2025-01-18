package data.missions.PvE;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import data.missions.scripts.AI_missionUtils;
import data.missions.scripts.AI_missionUtils.Fleet;
import data.missions.scripts.AI_missionUtils.Match;
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

public class MissionDefinition implements MissionDefinitionPlugin {

	private static final Logger log = Global.getLogger(MissionDefinition.class);

	private static int globalCurrentMatch, playerIndex, enemyFactionNumber, minFP = 0;
	private static boolean firstEnteringMission = true;

	private static final String PATH = "tournament/";
	private static final String ROUND_MATCHES = PATH + "PvE_matches.csv";
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
				int map = row.optInt("map", -1);
				float size = (float)row.optDouble("size", 1f);

				MATCHES.put(i, Match.createPVE(playerIndex, map, size));
			}
		} catch (IOException | JSONException ex) {
			log.error("unable to read round_matches.csv");
		}

		//cycle the matches while holding space
		if (firstEnteringMission) {
			firstEnteringMission = false;
			globalCurrentMatch = 0;
			enemyFactionNumber = 0;
			minFP = 10;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			globalCurrentMatch++;
			if (globalCurrentMatch >= MATCHES.size()) {
				globalCurrentMatch = 0;
			}
		} else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			globalCurrentMatch--;
			if (globalCurrentMatch < 0) {
				globalCurrentMatch = MATCHES.size() - 1;
			}
		} else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			minFP += 10;
			if (minFP > 2000) {
				minFP = 2000;
			}
		} else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			minFP -= 10;
			if (minFP < 10) {
				minFP = 10;
			}
		} else {
			enemyFactionNumber++;
			if (enemyFactionNumber > 1000) {
				enemyFactionNumber -= 1000;
			}
		}

		Match currentMatch = MATCHES.get(globalCurrentMatch);

		playerIndex = currentMatch.sidePlayer;
		Fleet thePlayer = Fleet.createFleet(PATH, playerIndex);
		api.initFleet(FleetSide.PLAYER, "", FleetGoal.ATTACK, true, 1);
		api.setFleetTagline(FleetSide.PLAYER, thePlayer.getName());
		thePlayer.deploy(api, FleetSide.PLAYER);

		Fleet theEnemy = Fleet.createFactionFleet(minFP, enemyFactionNumber);
		api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true, 1);
		api.setFleetTagline(FleetSide.ENEMY, theEnemy.getName());
		theEnemy.deploy(api, FleetSide.ENEMY);

		int playerCost = thePlayer.getDeploymentCost();
		int enemyCost = theEnemy.getDeploymentCost();
		api.addBriefingItem("Player fleet deployment cost : " + playerCost);
		api.addBriefingItem("Enemy faction : " + Global.getSector().getFaction(AI_missionUtils.chooseFactions(enemyFactionNumber)).getDisplayName() + " (" + AI_missionUtils.chooseFactions(enemyFactionNumber) + ")");
		api.addBriefingItem("Enemy excepted deployment cost : " + minFP);
		api.addBriefingItem("Enemy fleet deployment cost : " + enemyCost);

		//set the terrain
		int seed = currentMatch.mapNumber;
		if (seed < 0) seed = globalCurrentMatch * playerCost + enemyCost + playerCost;
		seed %= AI_missionUtils.MAP_COUNT;

		float sizeMult = currentMatch.sizeMult;
		if (sizeMult <= 0f) sizeMult = Math.min((float)Math.log(playerCost * 0.5f + enemyCost * 0.5f) * 0.3f, 1.5f);
		AI_missionUtils.setBattleSpace(api, seed, sizeMult);

		//add the price check and anti-retreat plugin
		api.addPlugin(new Plugin());
	}

	public final static class Plugin extends BaseEveryFrameCombatPlugin {

		private CombatEngineAPI engine = null;
		private boolean initialFleetDeployed = false;
		private boolean initialPlayerShown = false;
		private boolean initialSideShown = false;
		private boolean initialSoundPlay = false;

		private boolean shouldShowEnd = false;

		@Override
		public void init(CombatEngineAPI engine) {

			this.engine = engine;
			engine.getContext().aiRetreatAllowed = false;
			engine.getContext().fightToTheLast = true;

			////////////////////////////////////
			//                                //
			//    CAMERA AND TIME CONTROLS    //
			//                                //
			////////////////////////////////////

			EveryFrameCombatPlugin freeCam = new AI_freeCamPlugin();
			engine.removePlugin(freeCam);
			engine.addPlugin(freeCam);

			//EveryFrameCombatPlugin relocate = new AI_relocatePlugin();
			//engine.removePlugin(relocate);
			//engine.addPlugin(relocate);

			Global.getCombatEngine().setMaxFleetPoints(FleetSide.ENEMY, 9999);
			Global.getCombatEngine().setMaxFleetPoints(FleetSide.PLAYER, 9999);

			enemyFactionNumber--;
		}

		private int bonus0 = 0;
		private int bonus1 = 0;

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

					SpriteAPI playerP = AI_missionUtils.getPlayerSprite(playerIndex);
					AI_missionUtils.screenSpace(playerP, MagicRender.positioning.CENTER, new Vector2f(playerP.getWidth() * 1.2f, 0), new Vector2f(playerP.getWidth() * -0.03f, 0), new Vector2f(playerP.getWidth(), playerP.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, 0f, 3f, 0.2f);

					SpriteAPI playerE = AI_missionUtils.getDefaultSprite();
					AI_missionUtils.screenSpace(playerE, MagicRender.positioning.CENTER, new Vector2f(playerE.getWidth() * -1.2f, 0), new Vector2f(playerE.getWidth() * 0.03f, 0), new Vector2f(playerE.getWidth(), playerE.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, 0f, 3f, 0.2f);
				} else if (engine.getTotalElapsedTime(false) > 0.5f) {
					float time = 1f - engine.getTotalElapsedTime(false);

					SpriteAPI playerPF = AI_missionUtils.getPlayerSprite(playerIndex);
					AI_missionUtils.screenSpace(playerPF, MagicRender.positioning.CENTER, new Vector2f(playerPF.getWidth() * 1.2f + (time * 3000), 0), new Vector2f(-10, 0), new Vector2f(playerPF.getWidth(), playerPF.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, -1f, -1f, -1f);

					SpriteAPI playerEF = AI_missionUtils.getDefaultSprite();
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

					SpriteAPI playerP = AI_missionUtils.getPlayerSprite(playerIndex);
					float width = playerP.getWidth();
					float height = playerP.getHeight();
					Vector2f positionU = new Vector2f(-screenWidth * 0.5f + width * 0.25f, -height * 0.5f);
					AI_missionUtils.screenSpace(playerP, MagicRender.positioning.CENTER, positionU, new Vector2f(0, 0), new Vector2f(width * 0.5f, height * 0.5f), new Vector2f(), 0, 0, Color.GREEN, false, 1f, 999999f, 1f);

					SpriteAPI playerE = AI_missionUtils.getDefaultSprite();
					width = playerE.getWidth();
					height = playerE.getHeight();
					Vector2f positionD = new Vector2f(-screenWidth * 0.5f + width * 0.25f, height * 0.5f);
					AI_missionUtils.screenSpace(playerE, MagicRender.positioning.CENTER, positionD, new Vector2f(0, 0), new Vector2f(width * 0.5f, height * 0.5f), new Vector2f(), 0, 0, Color.PINK, false, 1f, 999999f, 1f);
				}
			}

			if (engine.isPaused()) return;

			////////////////////////////////////
			//                                //
			//         TICKING CLOCK          //
			//                                //
			////////////////////////////////////

			int clock = (int) engine.getTotalElapsedTime(false);
			engine.maintainStatusForPlayerShip("clock", null, "Timer", clock + " seconds", !shouldShowEnd);
		}
	}
}