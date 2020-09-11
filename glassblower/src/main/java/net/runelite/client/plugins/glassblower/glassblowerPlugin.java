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
import net.runelite.client.game.ItemManager;
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
	private ItemManager itemManager;


	@Inject
	private glassblowerOverlay overlay;

	Instant botTimer;
	String status;
	String outputStatus;
	int tickTimer;
	MenuEntry targetMenu;

	int objectToBlowId;
	String objectToBlowName = "";
	boolean firstTime;

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
		updateObjectToBlowId();
		firstTime=true;
		outputStatus="";
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
		log.info("------------------------");
		objectToBlowName=itemManager.getItemDefinition(objectToBlowId).getName();
		// runs every gametick
		status = getStatus();
		if(!status.equals("TICK_TIMER")){
			outputStatus=status;
		}
		log.info(status);
		log.info(objectToBlowName);
		log.info(Integer.toString(objectToBlowId));
		switch (status){
			case "BLOWING":
				if(utils.inventoryContains(1775)){
					tickTimer=3;
				} else {
					tickTimer=tickDelay();
				}
				break;
			case "TAKING_A_BREATH":
				tickTimer+=tickDelay();
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
				log.info(String.valueOf(firstTime));
				if(firstTime) {
					targetMenu = new MenuEntry("Use","Use",1785,38,utils.getInventoryWidgetItem(1785).getIndex(),9764864,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					firstTime=false;
					tickTimer+=tickDelay();
					break;
				}
				targetMenu = new MenuEntry("Use", "<col=ff9040>Glassblowing pipe<col=ffffff> -> <col=ff9040>Molten glass", 1775,31, utils.getInventoryWidgetItem(1775).getIndex(),9764864,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				tickTimer+=(2+tickDelay());
				break;
			case "SELECT_MENU":
				interactWithMultiMenu();
				tickTimer+=(2+tickDelay());
				break;
			case "DEPOSIT_BLOWN":
				targetMenu = new MenuEntry("Deposit-All", "<col=ff9040>"+objectToBlowName+"</col>", 8,1007, utils.getInventoryWidgetItem(objectToBlowId).getIndex(),983043,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				tickTimer+=tickDelay();
				break;
			case "DEPOSIT_INVENTORY":
				targetMenu = new MenuEntry("Deposit inventory","",1,57,-1,786473,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				tickTimer+=tickDelay();
				break;
			case "OPENING_BANK":
				openNearestBank();
				tickTimer+=tickDelay();
				break;

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

	private String getStatus()
	{
		if(client.getLocalPlayer().getAnimation()==884){
			return "BLOWING";
		}
		if(tickTimer>0){
			tickTimer--;
			return "TICK_TIMER";
		}
		if(!utils.inventoryContains(1785)){ //no pipe
			return "NO_PIPE";
		}
		if(utils.isBankOpen()){ //bank open
			if(utils.inventoryContains(1775) && utils.getInventoryItemCount(1775,false)==27){ //full glass
				return "CLOSING_BANK";
			} else if(utils.inventoryContains(objectToBlowId)){ //got blown items
				return "DEPOSIT_BLOWN";
			} else if(utils.getInventorySpace()!=27) { //invent not empty except pipe
				return "DEPOSIT_INVENTORY";
			} else {
				return "WITHDRAWING_GLASS"; //empty invent except pipe
			}
		}
		if(utils.inventoryContains(1775)){ //inventory contains glass & pipe
			if(utils.getInventoryItemCount(1775,false)==27){ //invent is full of glass & pipe
				if(client.getWidget(270,1)==null || client.getWidget(270,1).isHidden()){
					log.info("widget is null");
					return "NEED_TO_BLOW";
				} else {
					return "SELECT_MENU";
				}
			} else { //invent has pipe but not full glass
				if(client.getWidget(270,1)==null || client.getWidget(270,1).isHidden()){
					log.info("widget is null");
					return "NEED_TO_BLOW";
				} else {
					return "SELECT_MENU";
				}
			}
		} else { //pipe & no glass
			return "OPENING_BANK";
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

	private void updateObjectToBlowId(){
		if(config.makeBestItem()){
			int playerCraftingLevel = client.getRealSkillLevel(Skill.CRAFTING);
			if(playerCraftingLevel < 4) {
				objectToBlowId = 1919;
			} else if(playerCraftingLevel<12) {
				objectToBlowId = 4527;
			} else if(playerCraftingLevel<33) {
				objectToBlowId = 4522;
			} else if(playerCraftingLevel<42) {
				objectToBlowId = 229;
			} else if(playerCraftingLevel<46) {
				objectToBlowId = 6667;
			} else if(playerCraftingLevel<49) {
				objectToBlowId = 567;
			} else if(playerCraftingLevel<87) {
				objectToBlowId = 4542;
			} else {
				objectToBlowId = 10980;
			}
			return;
		}
		switch (config.type()) {
			case BEER_GLASS:
				objectToBlowId = 1919;
				break;
			case CANDLE_LANTERN:
				objectToBlowId = 4527;
				break;
			case OIL_LAMP:
				objectToBlowId = 4522;
				break;
			case VIAL:
				objectToBlowId = 229;
				break;
			case FISHBOWL:
				objectToBlowId = 6667;
				break;
			case UNPOWERED_STAFF_ORB:
				objectToBlowId = 567;
				break;
			case LANTERN_LENS:
				objectToBlowId = 4542;
				break;
			case LIGHT_ORB:
				objectToBlowId = 10980;
				break;
		}
	}

	private void interactWithMultiMenu(){
		int param1 = 17694734;
		switch(objectToBlowId){
			case 1919:
				param1 = 17694734;
				break;
			case 4527:
				param1 = 17694735;
				break;
			case 4522:
				param1 = 17694736;
				break;
			case 229:
				param1 = 17694737;
				break;
			case 6667:
				param1 = 17694738;
				break;
			case 567:
				param1 = 17694739;
				break;
			case 4542:
				param1 = 17694740;
				break;
			case 10980:
				param1 = 17694741;
				break;

		}
		targetMenu = new MenuEntry("Make", "<col=ff9040>"+objectToBlowName+"</col>", 1,57, -1,param1,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}
}