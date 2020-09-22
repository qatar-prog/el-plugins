package net.runelite.client.plugins.ouraniaaltar;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.queries.GameObjectQuery;
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
import static net.runelite.client.plugins.botutils.Banks.BANK_SET;

import java.awt.*;
import java.awt.event.KeyEvent;
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
	@Inject
	private Client client;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	ItemManager itemManager;

	@Inject
	private ouraniaaltarConfig config;

	@Inject
	private ouraniaaltarOverlay overlay;



	//plugin data
	MenuEntry targetMenu;
	int clientTickBreak = 0;
	int withdrawClickCount = 0;
	int tickTimer;
	String status = "UNKNOWN";
	int runecraftProgress = 0;
	//overlay data
	Instant botTimer;
	List<Integer> REQUIRED_ITEMS = new ArrayList<>();
	List<Integer> DEGRADED_POUCHES = new ArrayList<>();
	List<Integer> RUNE_IDS = new ArrayList<>();
	List<Integer> DROP_RUNE_IDS_CONFIG = new ArrayList<>();
	List<Integer> DROP_RUNE_IDS = new ArrayList<>();
	int ESSENCE_ID;
	int startEss;
	int currentEss;
	int clientTickCounter;
	boolean clientClick;
	int craftingTimer;

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
		setValues();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		log.info("Plugin stopped");
		setValues();
	}

	private void setValues()
	{
		runecraftProgress = 0;
		if(config.giantPouch()){
			REQUIRED_ITEMS = List.of(5509,5510,5512,5514,12791);
		} else {
			REQUIRED_ITEMS = List.of(5509,5510,5512,12791);
		}
		DEGRADED_POUCHES = List.of(5511,5513,5515);
		RUNE_IDS = List.of(557,556,566,560,561,564,559,562,554,555,565);
		startEss=0;
		currentEss=0;
		clientTickCounter=-1;
		clientTickBreak=0;
		clientClick=false;
		craftingTimer=-1;
		if(config.daeyalt()){
			ESSENCE_ID = 24704;
		} else {
			ESSENCE_ID = 7936;
		}
		if(config.dropRunes()){
			DROP_RUNE_IDS_CONFIG.clear();
			for(String id : config.dropRunesString().split(",")){
				try{
					DROP_RUNE_IDS_CONFIG.add(Integer.parseInt(id));
				} catch (Exception e) {
					utils.sendGameMessage("INCORRECT FORMAT OF RUNE IDS IN CONFIG.");
				}
			}
		}
	}

	@Subscribe
	private void onClientTick(ClientTick clientTick)
	{
		if(clientTickBreak>0){
			clientTickBreak--;
			return;
		}
		clientTickBreak=utils.getRandomIntBetweenRange(4,6);
		if(config.dropRunes()){
			if(craftingTimer == 3){
				if(runecraftProgress<14){
					dropRunes();
				}
			}
			if(craftingTimer == 0){
				DROP_RUNE_IDS.clear();
				DROP_RUNE_IDS.addAll(DROP_RUNE_IDS_CONFIG);
			}
		}
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		clientTickCounter=0;
		status = checkPlayerStatus();
		switch (status) {
			case "ANIMATING":
			case "NULL_PLAYER":
			case "TICK_TIMER":
				break;
			case "MOVING":
				tickTimer=0;
				break;
			case "OPENING_BANK":
				if(runecraftProgress==17){
					runecraftProgress++;
				}
				openEniolaBank();
				break;
			case "MISSING_REQUIRED":
				withdrawRequiredItems();
				break;
			case "CLICKING_ALTAR":
				DROP_RUNE_IDS.addAll(DROP_RUNE_IDS_CONFIG);
				clickOuraniaAltar();
				runecraftProgress++;
				tickTimer=tickDelay();
				break;
			case "EMPTYING_POUCHES":
				emptyPouches();
				break;
			case "TELEPORT_OURANIA":
				teleToOurania();
				DROP_RUNE_IDS.clear();
				runecraftProgress++;
				tickTimer=tickDelay();
				break;
			case "CLICKING_LADDER":
				climbDownLadder();
				runecraftProgress++;
				tickTimer=tickDelay();
				break;
			case "DEPOSIT_INVENT":
				utils.depositAll();
				runecraftProgress=0;
				break;
			case "POUCH_DEGRADED":
				fixDegradedPouch();
				tickTimer=tickDelay();
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

	@Subscribe
	private void onItemContainerChanged(ItemContainerChanged event)
	{

	}

	private long sleepDelay()
	{
		return utils.randomDelay(false, 60, 350, 100, 150);
	}

	private int tickDelay()
	{
		return (int) utils.randomDelay(false,1, 3, 1, 2);
	}

	private String checkPlayerStatus()
	{
		Player player = client.getLocalPlayer();
		if(player==null){
			return "NULL_PLAYER";
		}
		if(player.getPoseAnimation()!=813){
			if(!player.getWorldLocation().equals(new WorldPoint(3058, 5579, 0))){
				return "Moving";
			}
		}

		if(player.getAnimation()!=-1){
			if(player.getAnimation()==791){
				if(craftingTimer==-1){
					craftingTimer=0;
				} else {
					craftingTimer++;
				}
			}
			return "ANIMATING";
		}
		if(player.getAnimation()==-1){
			craftingTimer=-1;
		}
		if(checkHitpoints()<40){
			return "PLAYER_HP_LOW";
		}
		if(tickTimer>0)
		{
			tickTimer--;
			return "TICK_TIMER";
		}
		if(!utils.inventoryContains(12791)){
			return "MISSING_RUNE_POUCH";
		}
		if(utils.inventoryContains(DEGRADED_POUCHES)){
			return "POUCH_DEGRADED";
		}
		if(!utils.inventoryContainsAllOf(REQUIRED_ITEMS)){
			if(!utils.isBankOpen()){
				return "OPENING_BANK";
			} else {
				return "MISSING_REQUIRED";
			}
		}
		if(utils.inventoryContainsAllOf(REQUIRED_ITEMS)){
			if(runecraftProgress<8){
				if(utils.inventoryContains(RUNE_IDS)){
					if(!utils.isBankOpen()){
						return "OPENING_BANK";
					} else {
						return "DEPOSIT_INVENT";
					}
				}
				if(!utils.isBankOpen()){
					return "OPENING_BANK";
				} else {
					return fillPouches();
				}
			} else if(runecraftProgress==8){
				return "CLICKING_ALTAR";
			} else if(runecraftProgress<15){
				return "EMPTYING_POUCHES";
			} else if(runecraftProgress==15){
				craftingTimer=-1;
				return "TELEPORT_OURANIA";
			} else if(runecraftProgress==16){
				return "CLICKING_LADDER";
			} else if(runecraftProgress==17){
				return "OPENING_BANK";
			} else if(runecraftProgress==18){
				return "DEPOSIT_INVENT";
			}
		}
		return "UNKNOWN";
	}

	private void openEniolaBank()
	{
		targetMenu = new MenuEntry("Bank", "<col=ffff00>Eniola", 3220, 9, 0, 0, false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+utils.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+utils.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-utils.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-utils.getRandomIntBetweenRange(0,2));
	}

	private String fillPouches()
	{
		if(startEss==0){
			startEss = utils.getBankItemWidget(ESSENCE_ID).getItemQuantity();
		}
		currentEss = utils.getBankItemWidget(ESSENCE_ID).getItemQuantity();
		tickTimer=0;
		if(client.getVar(Varbits.RUN_SLOWED_DEPLETION_ACTIVE)==0 && checkRunEnergy()<config.minEnergy()){
			if(utils.inventoryContains(12631)){
				targetMenu = new MenuEntry("Drink","<col=ff9040>Stamina potion(1)</col>",9,1007,utils.getInventoryWidgetItem(12631).getIndex(),983043,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return "DRINKING_STAM";
			} else {
				targetMenu = new MenuEntry("Withdraw-1","<col=ff9040>Stamina potion(1)</col>",1,57,utils.getBankItemWidget(12631).getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return "WITHDRAW_STAM";
			}
		}
		if(checkHitpoints()<config.minHealth()){
			if(utils.inventoryContains(3144)){
				targetMenu = new MenuEntry("Eat","<col=ff9040>Cooked karambwan</col>",9,1007,utils.getInventoryWidgetItem(3144).getIndex(),983043,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return "EATING_KARAM";
			} else {
				targetMenu = new MenuEntry("Withdraw-1","Withdraw-1",1,57,utils.getBankItemWidget(3144).getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return "WITHDRAW_KARAM";
			}
		}
		if(!config.giantPouch()){
			if(runecraftProgress==0){
				runecraftProgress=2;
			}
		}
		switch(runecraftProgress){
			case 0:
			case 2:
			case 6:
				targetMenu = new MenuEntry("Withdraw-All","<col=ff9040>"+itemManager.getItemDefinition(ESSENCE_ID).getName()+"</col>",7,1007,utils.getBankItemWidget(ESSENCE_ID).getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				runecraftProgress++;
				return "WITHDRAW_ESS";
			case 1:
				targetMenu = new MenuEntry("Fill","<col=ff9040>Giant pouch</col>",9,1007,utils.getInventoryWidgetItem(5514).getIndex(),983043,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				runecraftProgress++;
				return "FILL_GIANT";
			case 3:
				targetMenu = new MenuEntry("Fill","<col=ff9040>Large pouch</col>",9,1007,utils.getInventoryWidgetItem(5512).getIndex(),983043,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				runecraftProgress++;
				return "FILL_LARGE";
			case 4:
				targetMenu = new MenuEntry("Fill","<col=ff9040>Medium pouch</col>",9,1007,utils.getInventoryWidgetItem(5510).getIndex(),983043,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				runecraftProgress++;
				return "FILL_MEDIUM";
			case 5:
				targetMenu = new MenuEntry("Fill","<col=ff9040>Small pouch</col>",9,1007,utils.getInventoryWidgetItem(5509).getIndex(),983043,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				runecraftProgress++;
				return "FILL_SMALL";
			case 7:
				targetMenu = new MenuEntry("Close","",1,57,11,786434,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				runecraftProgress++;
				tickTimer=tickDelay();
				return "CLOSING_BANK";
		}
		return "UNKNOWN";
	}

	private String emptyPouches()
	{
		if(config.giantPouch()){
			switch (runecraftProgress) {
				case 9:
					targetMenu = new MenuEntry("Empty", "<col=ff9040>Small pouch</col>", 5509, 34, utils.getInventoryWidgetItem(5509).getIndex(), 9764864, false);
					utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
					runecraftProgress++;
					return "EMPTY_SMALL";
				case 10:
					targetMenu = new MenuEntry("Empty", "<col=ff9040>Giant pouch</col>", 5514, 34, utils.getInventoryWidgetItem(5514).getIndex(), 9764864, false);
					utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
					runecraftProgress++;
					return "EMPTY_GIANT";
				case 11:
				case 14:
					clickOuraniaAltar();
					runecraftProgress++;
					return "CLICKING_ALTAR";
				case 12:
					targetMenu = new MenuEntry("Empty", "<col=ff9040>Large pouch</col>", 5512, 34, utils.getInventoryWidgetItem(5512).getIndex(), 9764864, false);
					utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
					runecraftProgress++;
					return "EMPTY_LARGE";
				case 13:
					targetMenu = new MenuEntry("Empty", "<col=ff9040>Medium pouch</col>", 5510, 34, utils.getInventoryWidgetItem(5510).getIndex(), 9764864, false);
					utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
					runecraftProgress++;
					return "EMPTY_MEDIUM";
			}
			return "UNKNOWN";
		} else {
			switch (runecraftProgress) {
				case 9:
					targetMenu = new MenuEntry("Empty", "<col=ff9040>Small pouch</col>", 5509, 34, utils.getInventoryWidgetItem(5509).getIndex(), 9764864, false);
					utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
					runecraftProgress++;
					return "EMPTY_SMALL";
				case 10:
					targetMenu = new MenuEntry("Empty", "<col=ff9040>Medium pouch</col>", 5510, 34, utils.getInventoryWidgetItem(5510).getIndex(), 9764864, false);
					utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
					runecraftProgress++;
					return "EMPTY_MEDIUM";
				case 11:
				case 14:
					clickOuraniaAltar();
					runecraftProgress++;
					return "CLICKING_ALTAR";
				case 12:
					targetMenu = new MenuEntry("Empty", "<col=ff9040>Large pouch</col>", 5512, 34, utils.getInventoryWidgetItem(5512).getIndex(), 9764864, false);
					utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
					runecraftProgress=14;
					return "EMPTY_LARGE";
			}
			return "UNKNOWN";
		}
	}

	private void withdrawRequiredItems()
	{
		try{
			if(!utils.inventoryContains(REQUIRED_ITEMS.get(0))){
				targetMenu = new MenuEntry("Withdraw-1","Withdraw-1",1,1007,utils.getBankItemWidget(REQUIRED_ITEMS.get(0)).getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			}
			else if(!utils.inventoryContains(REQUIRED_ITEMS.get(1))){
				targetMenu = new MenuEntry("Withdraw-1","Withdraw-1",1,1007,utils.getBankItemWidget(REQUIRED_ITEMS.get(1)).getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			}
			else if(!utils.inventoryContains(REQUIRED_ITEMS.get(2))){
				targetMenu = new MenuEntry("Withdraw-1","Withdraw-1",1,1007,utils.getBankItemWidget(REQUIRED_ITEMS.get(2)).getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			}
			else if(!utils.inventoryContains(REQUIRED_ITEMS.get(3))){
				targetMenu = new MenuEntry("Withdraw-1","Withdraw-1",1,1007,utils.getBankItemWidget(REQUIRED_ITEMS.get(3)).getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			}
		} catch (Exception ignored){

		}
	}

	private void clickOuraniaAltar()
	{
		GameObject targetObject = utils.findNearestGameObject(29631);
		if(targetObject!=null){
			targetMenu = new MenuEntry("Craft-rune","<col=ffff>Runecrafting altar",targetObject.getId(),3,targetObject.getLocalLocation().getSceneX()-1,targetObject.getLocalLocation().getSceneY()-1,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		}
	}

	private void teleToOurania()
	{
		targetMenu = new MenuEntry("Cast", "<col=00ff00>Ourania Teleport</col>", 1, 57, -1, 14286991, false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void climbDownLadder()
	{
		GameObject targetObject = utils.findNearestGameObject(29635);
		if(targetObject!=null){
			targetMenu = new MenuEntry("Climb","<col=ffff>Ladder",targetObject.getId(),3,targetObject.getLocalLocation().getSceneX(),targetObject.getLocalLocation().getSceneY(),false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		}
	}

	private int checkRunEnergy()
	{
		try{
			return Integer.parseInt(client.getWidget(160,23).getText());
		} catch (Exception ignored) {

		}
		return 0;
	}

	private int checkHitpoints()
	{
		try{
			return client.getBoostedSkillLevel(Skill.HITPOINTS);
		} catch (Exception e) {
			return 0;
		}
	}

	private void fixDegradedPouch()
	{
		if(utils.inventoryFull()){
			utils.depositAll();
			return;
		}
		if(!utils.inventoryContains(556)){
			if(!utils.inventoryContainsStack(556,2)){
				targetMenu = new MenuEntry("Withdraw-All","<col=ff9040>Air rune</col>",7,1007,utils.getBankItemWidget(556).getIndex(),786444,false);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			}
		}
		if(!utils.inventoryContains(564)){
			targetMenu = new MenuEntry("Withdraw-All","<col=ff9040>Cosmic rune</col>",7,1007,utils.getBankItemWidget(564).getIndex(),786444,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			return;
		}
		if(utils.isBankOpen()){
			targetMenu = new MenuEntry("Close","",1,57,11,786434,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			return;
		}
		if(client.getWidget(231,4)!=null && client.getWidget(231,4).getText().contains("busy")){
			targetMenu = new MenuEntry("Continue","",0,30,-1,15138819,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			return;
		}
		if(client.getWidget(217,4)!=null && client.getWidget(217,4).getText().contains("essence")){
			targetMenu = new MenuEntry("Continue","",0,30,-1,14221315,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			return;
		}
		if(client.getWidget(231,2)==null || client.getWidget(231,2).isHidden()){
			targetMenu = new MenuEntry("Dark Mage","<col=00ff00>NPC Contact</col>",2,57,-1,14286952,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
			return;
		}
	}

	private void dropRunes()
	{
		for(int i=0;i<DROP_RUNE_IDS.size();i++){
			if(utils.inventoryContains(DROP_RUNE_IDS.get(i))){
				targetMenu = new MenuEntry("", "", DROP_RUNE_IDS.get(i), MenuOpcode.ITEM_DROP.getId(), utils.getInventoryWidgetItem(DROP_RUNE_IDS.get(i)).getIndex(), 9764864, false);
				DROP_RUNE_IDS.remove(i);
				utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				return;
			}
		}
	}
}