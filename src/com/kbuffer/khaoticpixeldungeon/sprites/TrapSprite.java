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
package com.kbuffer.khaoticpixeldungeon.sprites;

import com.kbuffer.khaoticpixeldungeon.Assets;
import com.kbuffer.khaoticpixeldungeon.Dungeon;
import com.kbuffer.khaoticpixeldungeon.DungeonTilemap;
import com.kbuffer.khaoticpixeldungeon.levels.Level;
import com.kbuffer.khaoticpixeldungeon.levels.traps.Trap;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

public class TrapSprite extends Image {

	private static TextureFilm frames;

	private int pos = -1;

	public TrapSprite() {
		super( Assets.TRAPS );

		if (frames == null) {
			frames = new TextureFilm( texture, 16, 16 );
		}

		origin.set( 8, 12 );
	}

	public TrapSprite( int image ) {
		this();
		reset( image );
	}

	public void reset( Trap trap ) {

		revive();

		// KPD - Infinite mode
		// Make trap sprite index loop back after level 26 to prevent crash bug at level 41
		reset( trap.image + ((((Dungeon.depth-1) % 25) / 5) * 8) );
		// end KPD

		alpha( 1f );

		pos = trap.pos;
		x = (pos % Level.WIDTH) * DungeonTilemap.SIZE;
		y = (pos / Level.WIDTH) * DungeonTilemap.SIZE;

	}

	public void reset( int image ) {
		frame( frames.get( image ) );
	}

}
