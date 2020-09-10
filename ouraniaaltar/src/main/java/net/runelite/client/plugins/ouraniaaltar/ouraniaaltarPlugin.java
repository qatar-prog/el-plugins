package net.runelite.client.plugins.ouraniaaltar;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.TileQuery;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.Runes;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.ge.GrandExchangeClient;
import net.runelite.http.api.osbuddy.OSBGrandExchangeClient;
import net.runelite.rs.api.RSMenuAction;
import okhttp3.OkHttpClient;
import org.pf4j.Extension;
import net.runelite.client.plugins.botutils.BotUtils;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Extension
@PluginDescriptor(
	name = "Ourania Crafter",
	description = "Crafts at ourania altar.",
	type = PluginType.SKILLING
)
@Slf4j
public class ouraniaaltarPlugin extends Plugin
{
	// Injects our config
	@Inject
	private ouraniaaltarConfig config;

	@Inject
	private Client client;

	@Inject
	private BotUtils utils;

	@Inject
	private GrandExchangeClient grandExchangeClient;

	@Inject
	private OSBGrandExchangeClient osbGrandExchangeClient;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ouraniaaltarOverlay overlay;

	//plugin data
	MenuEntry targetMenu;
	int clientTickBreak = 0;
	int withdrawClickCount = 0;
	int tickTimer;
	String status = "UNKNOWN";
	boolean clientTickBanking;

	//overlay data
	Instant botTimer;

	// Provides our config
	@Provides
	ouraniaaltarConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ouraniaaltarConfig.class);
	}

	@Provides
	OSBGrandExchangeClient provideOsbGrandExchangeClient(OkHttpClient okHttpClient)
	{
		return new OSBGrandExchangeClient(okHttpClient);
	}

	@Provides
	GrandExchangeClient provideGrandExchangeClient(OkHttpClient okHttpClient)
	{
		return new GrandExchangeClient(okHttpClient);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		botTimer = Instant.now();
		log.info("Plugin started");

	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		log.info("Plugin stopped");
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		status = checkPlayerStatus();
		log.info(status);

		switch (status) {
			case "ANIMATING":
			case "TICK_TIMER":
				break;
		}
	}

	@Subscribe
	private void onClientTick(ClientTick gameTick)
	{
		if(!clientTickBanking){
			return;
		}
		if(clientTickBreak>0){
			clientTickBreak--;
			return;
		}
		clientTickBreak+=utils.getRandomIntBetweenRange(15,20);



		if(utils.isBankOpen()){
			if(withdrawClickCount==0){
				targetMenu = new MenuEntry ("Deposit inventory","",1,57,-1,786473,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				withdrawClickCount++;
				return;
			} else if(withdrawClickCount>0 && withdrawClickCount<4) {
				if (utils.bankContains("Giant seaweed")) {
					targetMenu = new MenuEntry("Withdraw-1", "Withdraw-1", 1, 57, utils.getBankItemWidget(21504).getIndex(), 786444, false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					withdrawClickCount++;
					return;
				}
			} else if(withdrawClickCount==4) {
				if (utils.bankContains("Bucket of sand")) {
					targetMenu = new MenuEntry("Withdraw-18", "<col=ff9040>Bucket of sand</col>", 5, 57, utils.getBankItemWidget(1783).getIndex(), 786444, false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					withdrawClickCount++;
					return;
				}
			} else if(withdrawClickCount==5){
				utils.pressKey(27);
				withdrawClickCount=-1;
				clientTickBanking=false;
				tickTimer+=tickDelay();
				return;
			}
		}

	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.info(event.toString());
		if(targetMenu!=null){
			event.consume();
			client.invokeMenuAction(targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getOpcode(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
		}

	}

	private long sleepDelay()
	{
		return utils.randomDelay(false, 60, 350, 100, 100);
	}

	private int tickDelay()
	{
		return (int) utils.randomDelay(false,1, 3, 1, 2);
	}

	private String checkPlayerStatus()
	{
		if(tickTimer>0)
		{
			tickTimer--;
			return "TICK_TIMER";
		}
		return "UNKNOWN";
	}
	
	private void openNearestBank()
	{
		log.info("openNearestBank called.");
		GameObject targetObject = utils.findNearestBank();
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),4,targetObject.getLocalLocation().getSceneX(),targetObject.getLocalLocation().getSceneY(),false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			return;
		}

	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+utils.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+utils.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-utils.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-utils.getRandomIntBetweenRange(0,2));
	}
}