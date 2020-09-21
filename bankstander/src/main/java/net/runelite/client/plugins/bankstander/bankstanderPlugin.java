package net.runelite.client.plugins.bankstander;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
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
import okhttp3.OkHttpClient;
import org.pf4j.Extension;

import static net.runelite.client.plugins.bankstander.bankstanderState.*;
import static net.runelite.client.plugins.botutils.Banks.ALL_BANKS;
import static net.runelite.client.plugins.botutils.Banks.BANK_SET;

import java.awt.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Extension
@PluginDescriptor(
	name = "Bank Stander",
	description = "Performs various bank standing activities",
	type = PluginType.MISCELLANEOUS
)
@Slf4j
public class bankstanderPlugin extends Plugin
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
	private bankstanderConfig config;

	@Inject
	private bankstanderOverlay overlay;

	bankstanderState state;
	MenuEntry targetMenu;
	Instant botTimer;
	Player player;
	boolean firstTime;

	int timeout = 0;
	long sleepLength;
	boolean startBankStander;

	// Provides our config
	@Provides
	bankstanderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(bankstanderConfig.class);
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

		// example how to use config items
	}

	@Override
	protected void shutDown()
	{
		// runs on plugin shutdown
		log.info("Plugin stopped");
		resetVals();
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		startBankStander = false;
		firstTime=true;
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("bankstanderConfig"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startBankStander)
			{
				startBankStander = true;
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				overlayManager.add(overlay);
			}
			else
			{
				resetVals();
			}
		}
	}

	private long sleepDelay()
	{
		sleepLength = utils.randomDelay(false, 60,350,100,10);
		return sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) utils.randomDelay(false,1,3,2,2);
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private bankstanderState getBankState()
	{
		if (!utils.isBankOpen() && !utils.isDepositBoxOpen())
		{
			return FIND_BANK;
		}
		if(config.type() == bankstanderType.USE_ITEM){
			if(utils.inventoryContains(config.firstId())){
				return CLOSE_BANK;
			} else if (!utils.inventoryEmpty()){
				return DEPOSIT_ALL;
			}
			if(!utils.inventoryFull()){
				return WITHDRAW_ITEMS;
			}
		}
		if(config.type() == bankstanderType.USE_ITEM_ON_ITEM){
			if(utils.inventoryContains(config.firstId()) && utils.inventoryContains(config.secondId())){
				return CLOSE_BANK;
			} else if(!utils.inventoryEmpty()){
				return DEPOSIT_ALL;
			}
			if(!utils.inventoryFull())
			{
				return WITHDRAW_ITEMS;
			}
		}
		if(config.type() == bankstanderType.USE_TOOL_ON_ITEM){
			if(utils.inventoryContains(config.toolId()) && utils.inventoryContains(config.firstId())){
				return CLOSE_BANK;
			} else if(!utils.inventoryEmpty()){
				return DEPOSIT_ALL;
			}
			if(!utils.inventoryFull())
			{
				return WITHDRAW_ITEMS;
			}
		}
		return BANK_NOT_FOUND;
	}

	public bankstanderState getState()
	{
		if (timeout > 0)
		{
			return TIMEOUT;
		}
		if (utils.iterating)
		{
			return ITERATING;
		}
		if(config.type() == bankstanderType.USE_ITEM_ON_ITEM){
			if(utils.inventoryContains(config.firstId()) && utils.inventoryContains(config.secondId())){
				return utils.isBankOpen() ? CLOSE_BANK : USING_ITEM_ON_ITEM;
			}
		} else {
			return getBankState();
		}
		if(config.type() == bankstanderType.USE_ITEM){
			if(utils.inventoryContains(config.firstId())) {
				return utils.isBankOpen() ? CLOSE_BANK : USING_ITEM;
			}
		} else {
			return getBankState();
		}
		if(config.type() == bankstanderType.USE_TOOL_ON_ITEM){
			if(utils.inventoryContains(config.firstId()) && utils.inventoryContains(config.toolId())) {
				return utils.isBankOpen() ? CLOSE_BANK : USING_TOOL_ON_ITEM;
			}
		} else {
			return getBankState();
		}
		return ANIMATING;
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		if (!startBankStander)
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("elli-tt - client must be set to resizable");
				startBankStander = false;
				return;
			}
			if(client.getVar(Varbits.WITHDRAW_X_AMOUNT)!=14){
				utils.sendGameMessage("You should set your withdraw-X amount to 14 in the bank.");
				startBankStander = false;
				return;
			}
			if(player.getAnimation()!=-1){
				timeout = tickDelay();
				return;
			}
			state = getState();
			switch (state)
			{
				case TIMEOUT:
					timeout--;
					break;
				case ITERATING:
					timeout = tickDelay();
					break;
				case FIND_BANK:
					openNearestBank();
					timeout = tickDelay();
					break;
				case DEPOSIT_ALL:
					utils.depositAll();
					timeout = tickDelay();
					break;
				case MISSING_ITEMS:
					startBankStander = false;
					utils.sendGameMessage("Missing required items IDs: " + String.valueOf(config.toolId()) + " from inventory. Stopping.");
					resetVals();
					break;
				case USING_ITEM:
					useItem();
					timeout = tickDelay();
					break;
				case USING_ITEM_ON_ITEM:
					useItemOnItem();
					timeout = tickDelay();
					break;
				case USING_TOOL_ON_ITEM:
					useToolOnItem();
					timeout = tickDelay();
				case ANIMATING:
					break;
				case WITHDRAW_ITEMS:
					handleWithdraw();
					timeout = tickDelay();
					break;
				case CLOSE_BANK:
					closeBank();
					break;
			}
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked e)
	{
		log.info(e.toString());
		if(targetMenu!=null){
			e.consume();
			client.invokeMenuAction(targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getOpcode(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startBankStander)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private void openNearestBank()
	{
		GameObject targetObject = new GameObjectQuery()
				.idEquals(BANK_SET)
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

	private void handleAll()
	{
		if (config.type() == bankstanderType.USE_ITEM)
		{
			Collection<Integer> inventoryItems = utils.getAllInventoryItemIDs();
			utils.inventoryItemsInteract(inventoryItems, 33, false,true, 60, 350);
			return;
		}
		if(config.type() == bankstanderType.USE_ITEM_ON_ITEM)
		{
			if(client.getWidget(270,5)!= null && !client.getWidget(270,5).isHidden()){
				targetMenu = new MenuEntry("Make", "", 1, 57, -1,17694734, false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			} else {
				if(firstTime){
					targetMenu = new MenuEntry("Use","Use",227,38,utils.getInventoryWidgetItem(227).getIndex(),9764864,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					firstTime=false;
					return;
				} else {
					targetMenu = new MenuEntry("Use","<col=ff9040>Vial of water<col=ffffff> -> <col=ff9040>"+itemManager.getItemDefinition(config.firstId()).getName(),config.firstId(),31,utils.getInventoryWidgetItem(config.firstId()).getIndex(),9764864,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					return;
				}

			}
		}
	}

	private void handleWithdraw()
	{
		if(utils.getBankItemWidget(config.firstId())==null){
			return;
		}
		Widget firstItemBankWidget = utils.getBankItemWidget(config.firstId());
		if (config.type() == bankstanderType.USE_ITEM)
		{
			targetMenu = new MenuEntry("Withdraw-All", "<col=ff9040>"+itemManager.getItemDefinition(config.firstId()).getName()+"</col>",7,1007,firstItemBankWidget.getIndex(),786444,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		}

		if(utils.getBankItemWidget(config.secondId())==null){
			return;
		}
		Widget secondItemBankWidget = utils.getBankItemWidget(config.secondId());
		if(config.type() == bankstanderType.USE_ITEM_ON_ITEM)
		{
			if(utils.inventoryEmpty()){
				targetMenu = new MenuEntry("Withdraw-14", "<col=ff9040>"+itemManager.getItemDefinition(config.firstId()).getName()+"</col>",5,57,firstItemBankWidget.getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			} else {
				targetMenu = new MenuEntry("Withdraw-14", "<col=ff9040>"+itemManager.getItemDefinition(config.secondId()).getName()+"</col>",5,57,secondItemBankWidget.getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			}

		}
	}

	private void closeBank()
	{
		targetMenu = new MenuEntry("Close", "", 1, 57, 11, 786434, false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void useItem(){

	}

	private void useItemOnItem(){

	}

	private void useToolOnItem(){
		
	}
}