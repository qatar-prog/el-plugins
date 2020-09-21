package net.runelite.client.plugins.bankstander;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.table.TableAlignment;
import net.runelite.client.ui.overlay.components.table.TableComponent;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

import static net.runelite.api.MenuOpcode.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

@Slf4j
@Singleton
class bankstanderOverlay extends OverlayPanel
{
    private final Client client;
    private final bankstanderPlugin plugin;
    private final bankstanderConfig config;

    String timeFormat;
    String localstate = "Starting";
    private String infoStatus = "Starting...";

    @Inject
    private bankstanderOverlay(final Client client, final bankstanderPlugin plugin, final bankstanderConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.DYNAMIC);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "bankstander overlay"));
        setPriority(OverlayPriority.HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if(plugin.state!=null){
            localstate=plugin.state.toString();
        }
        TableComponent tableComponent = new TableComponent();
        tableComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);
        Duration duration = Duration.between(plugin.botTimer, Instant.now());
        timeFormat = (duration.toHours() < 1) ? "mm:ss" : "HH:mm:ss";
        tableComponent.addRow("Time:", formatDuration(duration.toMillis(), timeFormat));
        tableComponent.addRow("Mode:", config.type().toString());
        tableComponent.addRow("Status:", localstate);


        if (!tableComponent.isEmpty())
        {
            panelComponent.setBackgroundColor(ColorUtil.fromHex("#121212")); //Material Dark default
            panelComponent.setPreferredSize(new Dimension(200, 200));
            panelComponent.setBorder(new Rectangle(5, 5, 5, 5));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Bank Stander")
                    .color(ColorUtil.fromHex("#40C4FF"))
                    .build());
            panelComponent.getChildren().add(tableComponent);
        }
        return super.render(graphics);
    }
}
