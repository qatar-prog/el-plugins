package net.runelite.client.plugins.firemaker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.queries.GameObjectQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.ge.GrandExchangeClient;
import net.runelite.http.api.osbuddy.OSBGrandExchangeClient;
import net.runelite.rs.api.RSClient;
import okhttp3.OkHttpClient;
import org.pf4j.Extension;

import static net.runelite.client.plugins.botutils.Banks.ALL_BANKS;
import static net.runelite.client.plugins.botutils.Banks.BANK_SET;

import java.awt.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Extension
@PluginDescriptor(
	name = "Fire Maker",
	description = "Makes fires for you",
	type = PluginType.MISCELLANEOUS
)
@Slf4j
public class firemakerPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private firemakerConfig config;

	@Inject
	private firemakerOverlay overlay;

	MenuEntry targetMenu;
	Instant botTimer;
	Player player;
	boolean firstTime;
	String state;

	int timeout = 0;
	long sleepLength;
	boolean walkAction;
	WorldArea varrockFountainArea = new WorldArea(new WorldPoint(3209,3428,0), new WorldPoint(3214,3430,0));
	int coordX;
	int coordY;
	boolean northPath;
	GameObject targetObject;

	// Provides our config
	@Provides
	firemakerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(firemakerConfig.class);
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
		log.info("Plugin started");
		botTimer = Instant.now();
		overlayManager.add(overlay);
		walkAction=false;
		coordX=0;
		coordY=0;
		firstTime=true;
		northPath=true;


		// example how to use config items
	}

	@Override
	protected void shutDown()
	{
		// runs on plugin shutdown
		log.info("Plugin stopped");
		overlayManager.remove(overlay);
	}

	private long sleepDelay()
	{
		return utils.randomDelay(false, 60,350,100,10);
	}

	private int tickDelay()
	{
		return (int) utils.randomDelay(false,config.tickDelayMin(),config.tickDelayMax(),config.tickDelayDev(),config.tickDelayTarg());
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{

		player = client.getLocalPlayer();
		if(player==null){
			state = "null player";
			return;
		}
		if(player.getAnimation()!=-1){
			state = "animating";
			timeout=tickDelay();
			return;
		}
		if(timeout>0){
			timeout--;
			return;
		}
		if(!utils.isBankOpen()){
			if(config.justLaws() && utils.getInventorySpace()==26){
				openNearestBank();
				state = "opening nearest bank";
				northPath=!northPath;
				timeout=4+tickDelay();
				return;
			} else if(!config.justLaws() && utils.getInventorySpace()==25) {
				openNearestBank();
				state = "opening nearest bank";
				northPath = !northPath;
				timeout = 6 + tickDelay();
				return;
			}
		}
		//26185 fire id
		if(!utils.isBankOpen() && utils.inventoryFull() && player.getWorldLocation().equals(new WorldPoint(3185, 3436, 0))){
			teleportToVarrock();
			state = "teleporting to varrock";
			timeout=tickDelay();
			return;
		}
		if(northPath) {
			if (!utils.isBankOpen() && utils.inventoryFull() && !player.getWorldArea().intersectsWith(varrockFountainArea)) {
				WorldPoint startTile = new WorldPoint(3209 + utils.getRandomIntBetweenRange(0,4), 3429, 0);
				LocalPoint startTileLocal = LocalPoint.fromWorld(client, startTile);
				if (startTileLocal != null) {
					walk(startTileLocal, 0, sleepDelay());
				}
				timeout = 2 + tickDelay();
				state = "walking to start tile";
				return;
			}
		} else {
			if (!utils.isBankOpen() && utils.inventoryFull() && !player.getWorldArea().intersectsWith(varrockFountainArea)) {
				WorldPoint startTile = new WorldPoint(3209 + utils.getRandomIntBetweenRange(0,4), 3428, 0);
				LocalPoint startTileLocal = LocalPoint.fromWorld(client, startTile);
				if (startTileLocal != null) {
					walk(startTileLocal, 0, sleepDelay());
				}
				timeout = 2 + tickDelay();
				state = "walking to start tile";
				return;
			}
		}
		if(!utils.isBankOpen()){
			if(firstTime){
				targetMenu=new MenuEntry("Use","<col=ff9040>Tinderbox",590,38,utils.getInventoryWidgetItem(590).getIndex(),9764864,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				firstTime=false;
				return;
			}
			targetMenu = new MenuEntry("Use","<col=ff9040>Tinderbox<col=ffffff> -> <col=ff9040>"+itemManager.getItemDefinition(config.logId()).getName(),config.logId(),31,utils.getInventoryWidgetItem(config.logId()).getIndex(),9764864,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			timeout = tickDelay();
			return;
		}
		if(utils.inventoryFull()){
			closeBank();
			state = "closing bank";
			timeout=tickDelay();
			return;
		}
		if(utils.isBankOpen() && !utils.inventoryFull()){
			withdrawLogs();
			state = "withdrawing logs";
			timeout=tickDelay();
			return;
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked e)
	{
		//log.info(e.toString());
		if (walkAction)
		{
			e.consume();
			log.debug("Walk action");
			walkTile(coordX, coordY);
			walkAction = false;
			return;
		}
		if(targetMenu!=null){
			e.consume();
			client.invokeMenuAction(targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getOpcode(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
		}
	}

	private void openNearestBank()
	{
		targetObject = new GameObjectQuery()
				.idEquals(34810)
				.result(client)
				.nearestTo(client.getLocalPlayer());
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

	private void closeBank()
	{
		targetMenu = new MenuEntry("Close", "", 1, 57, 11, 786434, false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void teleportToVarrock(){
		targetMenu=new MenuEntry("Cast","<col=00ff00>Varrock Teleport</col>",1,57,-1,14286868,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void withdrawLogs(){
		targetMenu = new MenuEntry("Withdraw-All","<col=ff9040>"+itemManager.getItemDefinition(config.logId()).getName()+"</col>",7,1007,utils.getBankItemWidget(config.logId()).getIndex(),786444,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	public void walk(LocalPoint localPoint, int rand, long delay)
	{
		coordX = localPoint.getSceneX() + utils.getRandomIntBetweenRange(-Math.abs(rand), Math.abs(rand));
		coordY = localPoint.getSceneY() + utils.getRandomIntBetweenRange(-Math.abs(rand), Math.abs(rand));
		walkAction = true;
		targetMenu = new MenuEntry("Walk here", "", 0, MenuOpcode.WALK.getId(),
				0, 0, false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void walkTile(int x, int y)
	{
		RSClient rsClient = (RSClient) client;
		rsClient.setSelectedSceneTileX(x);
		rsClient.setSelectedSceneTileY(y);
		rsClient.setViewportWalking(true);
		rsClient.setCheckClick(false);
	}
}