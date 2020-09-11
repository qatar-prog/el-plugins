package net.runelite.client.plugins.glassblower;

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
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NullObjectID;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.api.GameState;
import net.runelite.api.MenuOpcode;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.NpcDefinitionChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Glass Blower",
	description = "Blows your glass",
	type = PluginType.SKILLING
)
@Slf4j
public class glassblowerPlugin extends Plugin
{
	// Injects our config
	@Inject
	private glassblowerConfig config;

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
	private glassblowerOverlay overlay;

	Instant botTimer;
	String status;
	int tickTimer;
	MenuEntry targetMenu;

	// Provides our config
	@Provides
	glassblowerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(glassblowerConfig.class);
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
		// runs on plugin startup
		botTimer = Instant.now();
		tickTimer=0;
		log.info("Plugin started");
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		// runs on plugin shutdown
		log.info("Plugin stopped");
		overlayManager.remove(overlay);
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		// runs every gametick
		status = getStatus();
		log.info(status);
		switch (status){
			case "BLOWING":
			case "TAKING_A_BREATH":
			case "TICK_TIMER":
				break;
			case "NO_PIPE":
				if(!utils.isBankOpen()){
					openNearestBank();
				} else {
					if(utils.bankContains(1785,1)){
						targetMenu = new MenuEntry("Withdraw-1", "Withdraw-1", 1,57, utils.getBankItemWidget(1785).getIndex(),786444,false);
						utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					} else {
						utils.sendGameMessage("No pipe in bank.");
						shutDown();
					}
				}
				tickTimer+=tickDelay();
				break;
			case "CLOSING_BANK":
				utils.pressKey(KeyEvent.VK_ESCAPE);
				tickTimer+=tickDelay();
				break;
			case "WITHDRAWING_GLASS":
				if(utils.bankContains(1775,27)){
					targetMenu = new MenuEntry("Withdraw-All", "<col=ff9040>Molten glass</col>", 7,1007, utils.getBankItemWidget(1775).getIndex(),786444,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				} else {
					utils.sendGameMessage("Ran out of glass");
					shutDown();
				}
				tickTimer+=tickDelay();
				break;
			case "NEED_TO_BLOW":
				if(client.getWidget(270,0)==null) {
					targetMenu = new MenuEntry("Use", "<col=ff9040>Glassblowing pipe<col=ffffff> -> <col=ff9040>Molten glass", 1775,31, utils.getInventoryWidgetItem(1775).getIndex(),9764864,false);
				} else {
					targetMenu = new MenuEntry("Make", "<col=ff9040>Lantern lens</col>", 1,57, -1,17694740,false);
				}
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				tickTimer+=tickDelay();
				break;
			case "BANKING_FOR_GLASS":
				if(utils.isBankOpen()){
					targetMenu = new MenuEntry("Deposit-All", "<col=ff9040>Lantern lens</col>", 8,1007, utils.getInventoryWidgetItem(4542).getIndex(),983043,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				} else {
					openNearestBank();
				}
				tickTimer+=tickDelay();
				break;
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		//log.info(event.toString());
		if(targetMenu!=null){
			event.consume();
			client.invokeMenuAction(targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getOpcode(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
		}

	}

	private String getStatus()
	{
		if(tickTimer>0){
			tickTimer--;
			return "TICK_TIMER";
		}
		if(!utils.inventoryContains(1785)){ //no pipe
			return "NO_PIPE";
		}
		if(utils.isBankOpen()){ //bank open
			if(utils.inventoryContains(1775) && utils.getInventoryItemCount(1775,false)==27){
				return "CLOSING_BANK";
			} else if(utils.inventoryContains(4542)){
				return "BANKING_FOR_GLASS";
			} else {
				return "WITHDRAWING_GLASS";
			}
		}
		if(utils.inventoryContains(1775)){ //glass & pipe
			if(utils.getInventoryItemCount(1775,false)==27){ //invent full & glass & pipe
				return "NEED_TO_BLOW";
			} else {
				if(client.getLocalPlayer().getAnimation()==884){ //glass & pipe & blowing
					return "BLOWING";
				} else { //glass & pipe & no blowing
					if(status == null || status.equals("NOT_BLOWING") || status.equals("NEED_TO_BLOW") || status.equals("TICK_TIMER")){
						return "NEED_TO_BLOW";
					} else {
						return "TAKING_A_BREATH";
					}
				}
			}
		} else { //pipe & no glass
			return "BANKING_FOR_GLASS";
		}
	}

	private void openNearestBank()
	{
		log.info("openNearestBank called.");
		GameObject targetObject = utils.findNearestBank();
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),4,targetObject.getLocalLocation().getSceneX(),targetObject.getLocalLocation().getSceneY(),false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
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

	private long sleepDelay()
	{
		return utils.randomDelay(false, 60, 350, 100, 100);
	}

	private int tickDelay()
	{
		return (int) utils.randomDelay(false,1, 3, 1, 2);
	}
}