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
package net.runelite.client.plugins.ouraniaaltar;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ouraniaaltarConfig")

public interface ouraniaaltarConfig extends Config
{
	@ConfigItem(
			keyName = "giantPouch",
			name = "Use Giant Pouch",
			description = "Use giant pouch",
			position = 0
	)
	default boolean giantPouch() { return false; }

	@ConfigItem(
			keyName = "daeyalt",
			name = "Use Daeyalt Essence",
			description = "Use daeyalt essence",
			position = 1
	)
	default boolean daeyalt() { return false; }

	@ConfigItem(
			keyName = "dropRunes",
			name = "Drop Runes",
			description = "Drop runes at altar",
			position = 2
	)
	default boolean dropRunes() { return false; }

	@ConfigItem(
			keyName = "dropRunesString",
			name = "Runes To Drop",
			description = "Runes you would like to drop.",
			position = 4,
			hidden = true,
			unhide = "dropRunes"
	)
	default String dropRunesString() { return "554,555,556,557,558,559"; }

	@ConfigItem(
			keyName = "minEnergy",
			name = "Minimum Energy",
			description = "Minimum energy before stam pot drank",
			position = 13
	)
	default int minEnergy() { return 35; }

	@ConfigItem(
			keyName = "minHealth",
			name = "Minimum Health",
			description = "Minimum health before food eaten",
			position = 14
	)
	default int minHealth() { return 65; }
}