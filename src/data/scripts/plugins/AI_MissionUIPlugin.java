package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.missions.scripts.AI_missionUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class AI_MissionUIPlugin extends BaseEveryFrameCombatPlugin {
    private String playerId, enemyId;
    private SpriteAPI playerFlagSprite;
    private SpriteAPI enemyFlagSprite;
    private boolean initialPlayerShown;
    private boolean initialSoundPlay;
    private CombatEngineAPI engine;
    private float width;
    private float height;

    private float leftFlagX;
    private float leftFlagY;
    private float leftFlagAlphaMult;

    public AI_MissionUIPlugin(String playerId, String enemyId) {
        this.playerId = playerId;
        this.enemyId = enemyId;
        this.playerFlagSprite = AI_missionUtils.getPlayerSprite(this.playerId);
        this.enemyFlagSprite = AI_missionUtils.getPlayerSprite(this.enemyId);
        width = playerFlagSprite.getWidth();
        height = playerFlagSprite.getHeight();
        leftFlagX = width * 0.25f + 75f;
        leftFlagY = Global.getSettings().getScreenHeight() * 0.5f;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!initialPlayerShown) {
            if (engine.getTotalElapsedTime(false) > 1f) {
                initialPlayerShown = true;
                SpriteAPI VS = Global.getSettings().getSprite("misc", "AI_versus");
                AI_missionUtils.screenSpace(VS, MagicRender.positioning.CENTER, new Vector2f(0, VS.getHeight() * 0.25f), new Vector2f(0, VS.getHeight() * -0.1f), new Vector2f(VS.getWidth(), VS.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, false, 0f, 3.2f, 0.2f);
                SpriteAPI VSF = Global.getSettings().getSprite("misc", "AI_versusF");
                AI_missionUtils.screenSpace(VSF, MagicRender.positioning.CENTER, new Vector2f(0, VSF.getHeight() * 0.25f), new Vector2f(0, VSF.getHeight() * -0.1f), new Vector2f(VSF.getWidth(), VSF.getHeight()), new Vector2f(0, 0), 0, 0, Color.WHITE, true, 0f, 0f, 0.5f);

                AI_missionUtils.screenSpace(playerFlagSprite, MagicRender.positioning.CENTER, new Vector2f(playerFlagSprite.getWidth() * 1.2f, 0), new Vector2f(playerFlagSprite.getWidth() * -0.03f, 0), new Vector2f(playerFlagSprite.getWidth(), playerFlagSprite.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, 0f, 3f, 0.2f);
                AI_missionUtils.screenSpace(enemyFlagSprite, MagicRender.positioning.CENTER, new Vector2f(enemyFlagSprite.getWidth() * -1.2f, 0), new Vector2f(enemyFlagSprite.getWidth() * 0.03f, 0), new Vector2f(enemyFlagSprite.getWidth(), enemyFlagSprite.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, 0f, 3f, 0.2f);
            } else if (engine.getTotalElapsedTime(false) > 0.5f) {
                float time = 1f - engine.getTotalElapsedTime(false);

                AI_missionUtils.screenSpace(playerFlagSprite, MagicRender.positioning.CENTER, new Vector2f(playerFlagSprite.getWidth() * 1.2f + (time * 3000), 0), new Vector2f(-10, 0), new Vector2f(playerFlagSprite.getWidth(), playerFlagSprite.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, -1f, -1f, -1f);
                AI_missionUtils.screenSpace(enemyFlagSprite, MagicRender.positioning.CENTER, new Vector2f(enemyFlagSprite.getWidth() * -1.2f - (time * 3000), 0), new Vector2f(10, 0), new Vector2f(enemyFlagSprite.getWidth(), enemyFlagSprite.getHeight()), new Vector2f(), 0, 0, Color.WHITE, false, -1f, -1f, -1f);
                if (!initialSoundPlay) {
                    initialSoundPlay = true;
                    Global.getSoundPlayer().playUISound("AIB_versusS", 1, 1);
                }
            }
        }
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        //        if (engine.getTotalElapsedTime(false) > 4f) {
        //            Vector2f refPoint = viewport.getCenter();
        //
        //            Vector2f positionU = new Vector2f(-805f + width * 0.25f, -height * 0.5f);
        //            positionU.scale(viewport.getViewMult());
        //            Vector2f positionD = new Vector2f(-805f + width * 0.25f, height * 0.5f);
        //            positionD.scale(viewport.getViewMult());
        //
        //            Vector2f.add(positionU, refPoint, positionU);
        //            Vector2f.add(positionD, refPoint, positionD);
        //            playerFlagSprite.setColor(Color.green);
        //            playerFlagSprite.setSize(width * 0.5f * viewport.getViewMult(), height * 0.5f * viewport.getViewMult());
        //            playerFlagSprite.setAlphaMult(1);
        //            playerFlagSprite.renderAtCenter(positionU.x, positionU.y);
        //            enemyFlagSprite.setColor(Color.red);
        //            enemyFlagSprite.setSize(width * 0.5f * viewport.getViewMult(), height * 0.5f * viewport.getViewMult());
        //            enemyFlagSprite.setAlphaMult(1);
        //            enemyFlagSprite.renderAtCenter(positionD.x, positionD.y);
        //        }
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        if (engine.getTotalElapsedTime(false) > 4.5f) {
            playerFlagSprite.setColor(Color.green);
            playerFlagSprite.setSize(width * 0.5f, height * 0.5f);
            playerFlagSprite.setAlphaMult(1);
            playerFlagSprite.renderAtCenter(leftFlagX, leftFlagY - height * 0.5f);
            enemyFlagSprite.setColor(Color.red);
            enemyFlagSprite.setSize(width * 0.5f, height * 0.5f);
            enemyFlagSprite.setAlphaMult(1);
            enemyFlagSprite.renderAtCenter(leftFlagX, leftFlagY + height * 0.5f);
        }
    }
}
