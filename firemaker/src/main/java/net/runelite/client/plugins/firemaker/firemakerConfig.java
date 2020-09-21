/*
 * Copyright (c) 2018, Andrew EP | ElPinche256 <https://github.com/ElPinche256>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.firemaker;

import net.runelite.client.config.Button;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("firemakerConfig")

public interface firemakerConfig extends Config
{
	@ConfigItem(
			keyName = "firemakerInstructions",
			name = "",
			description = "Instructions.",
			position = 0
	)
	default String firemakerInstructions()
	{
		return "Start in varrock west bank with a tinderbox, law runes, fire runes and an air staff." +
				" Enter in your log ID then turn on the plugin.";
	}

	@ConfigItem(
			keyName = "logId",
			name = "Log ID",
			description = "Enter the Id of the log you want to use.",
			position = 1
	)
	default int logId() { return 0; }

	@ConfigItem(
			keyName = "justLaws",
			name = "Just Law Runes",
			description = "Just Law Runes",
			position = 2
	)
	default boolean justLaws() { return false; }

	@ConfigItem(
			keyName = "customTickDelays",
			name = "Custom Tick Delays",
			description = "Set custom tick delays",
			position = 3
	)
	default boolean customTickDelays() { return false; }

	@ConfigItem(
			keyName = "tickDelayMin",
			name = "Tick Delay Minimum",
			description = "Tick delay minimum.",
			position = 4,
			hidden = true,
			unhide = "customTickDelays"
	)
	default int tickDelayMin() { return 2; }

	@ConfigItem(
			keyName = "tickDelayMax",
			name = "Tick Delay Maximum",
			description = "Tick delay maximum.",
			position = 5,
			hidden = true,
			unhide = "customTickDelays"
	)
	default int tickDelayMax() { return 3; }

	@ConfigItem(
			keyName = "tickDelayDev",
			name = "Tick Delay Deviation",
			description = "Tick delay deviation.",
			position = 6,
			hidden = true,
			unhide = "customTickDelays"
	)
	default int tickDelayDev() { return 1; }

	@ConfigItem(
			keyName = "tickDelayTarg",
			name = "Tick Delay Target",
			description = "Tick delay target.",
			position = 7,
			hidden = true,
			unhide = "customTickDelays"
	)
	default int tickDelayTarg() { return 3; }
}