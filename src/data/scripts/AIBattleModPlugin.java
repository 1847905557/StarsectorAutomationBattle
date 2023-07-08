package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import data.missions.scripts.automation.AutomationUtils;

public class AIBattleModPlugin extends BaseModPlugin {

	@Override
	public void onApplicationLoad() throws Exception {
		this.onDevModeF8Reload();
	}

	@Override
	public void onDevModeF8Reload() {
		AutomationUtils.initAutomation();
	}
}
