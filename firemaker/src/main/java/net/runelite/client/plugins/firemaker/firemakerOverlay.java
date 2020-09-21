package net.runelite.client.plugins.firemaker;

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
class firemakerOverlay extends OverlayPanel
{
    private final Client client;
    private final firemakerPlugin plugin;
    private final firemakerConfig config;

    String timeFormat;
    String localstate = "Starting";
    private String infoStatus = "Starting...";

    @Inject
    private firemakerOverlay(final Client client, final firemakerPlugin plugin, final firemakerConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.DYNAMIC);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "firemaker overlay"));
        setPriority(OverlayPriority.HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        TableComponent tableComponent = new TableComponent();
        tableComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);
        Duration duration = Duration.between(plugin.botTimer, Instant.now());
        timeFormat = (duration.toHours() < 1) ? "mm:ss" : "HH:mm:ss";
        tableComponent.addRow("Time:", formatDuration(duration.toMillis(), timeFormat));
        tableComponent.addRow("State:", plugin.state);
        if(plugin.northPath){
            tableComponent.addRow("Using: ", "North path");
        } else {
            tableComponent.addRow("Using: ", "South path");
        }


        if (!tableComponent.isEmpty())
        {
            panelComponent.setBackgroundColor(ColorUtil.fromHex("#121212")); //Material Dark default
            panelComponent.setPreferredSize(new Dimension(200, 200));
            panelComponent.setBorder(new Rectangle(5, 5, 5, 5));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Fire Maker")
                    .color(ColorUtil.fromHex("#40C4FF"))
                    .build());
            panelComponent.getChildren().add(tableComponent);
        }
        return super.render(graphics);
    }
}
