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
package com.kbuffer.khaoticpixeldungeon.actors.mobs;

import java.util.HashSet;

import com.kbuffer.khaoticpixeldungeon.Assets;
import com.kbuffer.khaoticpixeldungeon.actors.blobs.Blob;
import com.kbuffer.khaoticpixeldungeon.actors.blobs.GooWarn;
import com.kbuffer.khaoticpixeldungeon.actors.blobs.ToxicGas;
import com.kbuffer.khaoticpixeldungeon.effects.CellEmitter;
import com.kbuffer.khaoticpixeldungeon.effects.particles.ElmoParticle;
import com.watabou.noosa.Camera;
import com.kbuffer.khaoticpixeldungeon.Badges;
import com.kbuffer.khaoticpixeldungeon.Dungeon;
import com.kbuffer.khaoticpixeldungeon.actors.Char;
import com.kbuffer.khaoticpixeldungeon.actors.buffs.Buff;
import com.kbuffer.khaoticpixeldungeon.actors.buffs.Ooze;
import com.kbuffer.khaoticpixeldungeon.effects.Speck;
import com.kbuffer.khaoticpixeldungeon.items.artifacts.LloydsBeacon;
import com.kbuffer.khaoticpixeldungeon.items.keys.SkeletonKey;
import com.kbuffer.khaoticpixeldungeon.items.scrolls.ScrollOfPsionicBlast;
import com.kbuffer.khaoticpixeldungeon.items.weapon.enchantments.Death;
import com.kbuffer.khaoticpixeldungeon.levels.Level;
import com.kbuffer.khaoticpixeldungeon.scenes.GameScene;
import com.kbuffer.khaoticpixeldungeon.sprites.CharSprite;
import com.kbuffer.khaoticpixeldungeon.sprites.GooSprite;
import com.kbuffer.khaoticpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Goo extends Mob {
	{
		name = "Goo";
		HP = HT = 80;
		EXP = 10;
		defenseSkill = 12;
		spriteClass = GooSprite.class;

		loot = new LloydsBeacon().identify();
		lootChance = 0.333f;
	}

	private int pumpedUp = 0;

	@Override
	public int damageRoll() {
		if (pumpedUp > 0) {
			pumpedUp = 0;
			for (int i = 0; i < Level.NEIGHBOURS9DIST2.length; i++) {
				int j = pos + Level.NEIGHBOURS9DIST2[i];
				if (Level.insideMap(j) && Level.passable[j])
					CellEmitter.get(j).burst(ElmoParticle.FACTORY, 10);
			}
			Sample.INSTANCE.play( Assets.SND_BURNING );
			return Random.NormalIntRange( 5, 30 );
		} else {
			return Random.NormalIntRange( 2, 12 );
		}
	}

	@Override
	public int attackSkill( Char target ) {
		return (pumpedUp > 0) ? 30 : 15;
	}

	@Override
	public int dr() {
		return 2;
	}

	@Override
	public boolean act() {

		if (Level.water[pos] && HP < HT) {
			sprite.emitter().burst( Speck.factory( Speck.HEALING ), 1 );
			HP++;
		}

		return super.act();
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return (pumpedUp > 0) ? distance( enemy ) <= 2 : super.canAttack(enemy);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (Random.Int( 3 ) == 0) {
			Buff.affect( enemy, Ooze.class );
			enemy.sprite.burst( 0x000000, 5 );
		}

		if (pumpedUp > 0) {
			Camera.main.shake( 3, 0.2f );
		}

		return damage;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (pumpedUp == 1) {
			((GooSprite)sprite).pumpUp();
			for (int i = 0; i < Level.NEIGHBOURS9DIST2.length; i++) {
				int j = pos + Level.NEIGHBOURS9DIST2[i];
				if (Level.insideMap(j) && Level.passable[j])
					GameScene.add(Blob.seed(j, 2, GooWarn.class));
			}
			pumpedUp++;

			spend( attackDelay() );

			return true;
		} else if (pumpedUp >= 2 || Random.Int( 3 ) > 0) {

			boolean visible = Dungeon.visible[pos];

			if (visible) {
				if (pumpedUp >= 2) {
					((GooSprite) sprite).pumpAttack();
				}
				else
					sprite.attack( enemy.pos );
			} else {
				attack( enemy );
			}

			spend( attackDelay() );

			return !visible;

		} else {

			pumpedUp++;

			((GooSprite)sprite).pumpUp();

			for (int i=0; i < Level.NEIGHBOURS9.length; i++) {
				int j = pos + Level.NEIGHBOURS9[i];
				GameScene.add( Blob.seed( j , 2, GooWarn.class ));

			}

			if (Dungeon.visible[pos]) {
				sprite.showStatus( CharSprite.NEGATIVE, "!!!" );
				GLog.n( "Goo is pumping itself up!" );
			}

			spend( attackDelay() );

			return true;
		}
	}

	@Override
	public boolean attack( Char enemy ) {
		boolean result = super.attack( enemy );
		pumpedUp = 0;
		return result;
	}

	@Override
	protected boolean getCloser( int target ) {
		pumpedUp = 0;
		return super.getCloser( target );
	}
	
	@Override
	public void move( int step ) {
		Dungeon.level.seal();
		super.move( step );
	}
	
	@Override
	public void die( Object cause ) {
		
		super.die( cause );
		
		Dungeon.level.unseal();
		
		// KPD - Infinite mode
		// Don't do this stuff later on
		if( Dungeon.depth < 26 ) {
			GameScene.bossSlain();
			Dungeon.level.drop(new SkeletonKey(Dungeon.depth), pos).sprite.drop();
		}
		// end KPD

		Badges.validateBossSlain();
		
		yell( "glurp... glurp..." );
	}
	
	@Override
	public void notice() {
		super.notice();
		yell( "GLURP-GLURP!" );
	}
	
	@Override
	public String description() {
		return
			"Little is known about The Goo. It's quite possible that it is not even a creature, but rather a " +
			"conglomerate of vile substances from the sewers that somehow gained basic intelligence. " +
			"Regardless, dark magic is certainly what has allowed Goo to exist.\n\n" +
			"Its gelatinous nature has let it absorb lots of dark energy, you feel a chill just from being near. " +
			"If goo is able to attack with this energy you won't live for long.";
	}

	private final String PUMPEDUP = "pumpedup";

	@Override
	public void storeInBundle( Bundle bundle ) {

		super.storeInBundle( bundle );

		bundle.put( PUMPEDUP , pumpedUp );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {

		super.restoreFromBundle( bundle );

		pumpedUp = bundle.getInt( PUMPEDUP );
	}
	
	private static final HashSet<Class<?>> RESISTANCES = new HashSet<Class<?>>();
	static {
		RESISTANCES.add( ToxicGas.class );
		RESISTANCES.add( Death.class );
		RESISTANCES.add( ScrollOfPsionicBlast.class );
	}
	
	@Override
	public HashSet<Class<?>> resistances() {
		return RESISTANCES;
	}
}
