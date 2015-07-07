/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2015 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.kbuffer.khaoticpixeldungeon.levels.features;

import com.kbuffer.khaoticpixeldungeon.Challenges;
import com.kbuffer.khaoticpixeldungeon.Dungeon;
import com.kbuffer.khaoticpixeldungeon.actors.Char;
import com.kbuffer.khaoticpixeldungeon.actors.buffs.Barkskin;
import com.kbuffer.khaoticpixeldungeon.actors.buffs.Buff;
import com.kbuffer.khaoticpixeldungeon.actors.hero.Hero;
import com.kbuffer.khaoticpixeldungeon.actors.hero.HeroSubClass;
import com.kbuffer.khaoticpixeldungeon.effects.CellEmitter;
import com.kbuffer.khaoticpixeldungeon.effects.particles.LeafParticle;
import com.kbuffer.khaoticpixeldungeon.items.Dewdrop;
import com.kbuffer.khaoticpixeldungeon.items.Generator;
import com.kbuffer.khaoticpixeldungeon.items.Item;
import com.kbuffer.khaoticpixeldungeon.items.artifacts.SandalsOfNature;
import com.kbuffer.khaoticpixeldungeon.levels.Level;
import com.kbuffer.khaoticpixeldungeon.levels.Terrain;
import com.kbuffer.khaoticpixeldungeon.plants.BlandfruitBush;
import com.kbuffer.khaoticpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

public class HighGrass {

	public static void trample( Level level, int pos, Char ch ) {
		
		Level.set( pos, Terrain.GRASS );
		GameScene.updateMap( pos );

		if (!Dungeon.isChallenged( Challenges.NO_HERBALISM )) {
			int naturalismLevel = 0;

			if (ch != null) {
				SandalsOfNature.Naturalism naturalism = ch.buff( SandalsOfNature.Naturalism.class );
				if (naturalism != null) {
					if (!naturalism.isCursed()) {
						naturalismLevel = naturalism.level() + 1;
						naturalism.charge();
					} else {
						naturalismLevel = -1;
					}
				}
			}

			if (naturalismLevel >= 0) {
				// Seed, scales from 1/16 to 1/4
				if (Random.Int(16 - ((int) (naturalismLevel * 3))) == 0) {
					Item seed = Generator.random(Generator.Category.SEED);

					if (seed instanceof BlandfruitBush.Seed) {
						if (Random.Int(15) - Dungeon.limitedDrops.blandfruitSeed.count >= 0) {
							level.drop(seed, pos).sprite.drop();
							Dungeon.limitedDrops.blandfruitSeed.count++;
						}
					} else
						level.drop(seed, pos).sprite.drop();
				}

				// Dew, scales from 1/6 to 1/3
				if (Random.Int(24 - naturalismLevel*3) <= 3) {
					level.drop(new Dewdrop(), pos).sprite.drop();
				}
			}
		}

		int leaves = 4;
		
		// Barkskin
		if (ch instanceof Hero && ((Hero)ch).subClass == HeroSubClass.WARDEN) {
			Buff.affect( ch, Barkskin.class ).level( ch.HT / 3 );
			leaves = 8;
		}
		
		CellEmitter.get( pos ).burst( LeafParticle.LEVEL_SPECIFIC, leaves );
		Dungeon.observe();
	}
}
