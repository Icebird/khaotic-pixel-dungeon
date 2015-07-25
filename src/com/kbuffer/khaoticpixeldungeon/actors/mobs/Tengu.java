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

import com.kbuffer.khaoticpixeldungeon.items.artifacts.LloydsBeacon;
import com.kbuffer.khaoticpixeldungeon.levels.traps.PoisonTrap;
import com.kbuffer.khaoticpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.kbuffer.khaoticpixeldungeon.Assets;
import com.kbuffer.khaoticpixeldungeon.Badges;
import com.kbuffer.khaoticpixeldungeon.Badges.Badge;
import com.kbuffer.khaoticpixeldungeon.Dungeon;
import com.kbuffer.khaoticpixeldungeon.actors.Actor;
import com.kbuffer.khaoticpixeldungeon.actors.Char;
import com.kbuffer.khaoticpixeldungeon.actors.blobs.ToxicGas;
import com.kbuffer.khaoticpixeldungeon.actors.buffs.Poison;
import com.kbuffer.khaoticpixeldungeon.effects.CellEmitter;
import com.kbuffer.khaoticpixeldungeon.effects.Speck;
import com.kbuffer.khaoticpixeldungeon.items.TomeOfMastery;
import com.kbuffer.khaoticpixeldungeon.items.keys.SkeletonKey;
import com.kbuffer.khaoticpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.kbuffer.khaoticpixeldungeon.items.scrolls.ScrollOfPsionicBlast;
import com.kbuffer.khaoticpixeldungeon.items.weapon.enchantments.Death;
import com.kbuffer.khaoticpixeldungeon.levels.Level;
import com.kbuffer.khaoticpixeldungeon.levels.Terrain;
import com.kbuffer.khaoticpixeldungeon.mechanics.Ballistica;
import com.kbuffer.khaoticpixeldungeon.scenes.GameScene;
import com.kbuffer.khaoticpixeldungeon.sprites.TenguSprite;
import com.watabou.utils.Random;

public class Tengu extends Mob {

	private static final int JUMP_DELAY = 5;
	
	{
		name = "Tengu";
		spriteClass = TenguSprite.class;
		
		HP = HT = 120;
		EXP = 20;
		defenseSkill = 20;
	}
	
	private int timeToJump = JUMP_DELAY;
	
	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 8, 15 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 20;
	}
	
	@Override
	public int dr() {
		return 5;
	}
	
	@Override
	public void die( Object cause ) {

		// KPD - Infinite mode
		// Don't do this stuff later on
		if( Dungeon.depth < 26 ) {
			Badges.Badge badgeToCheck = null;
			switch (Dungeon.hero.heroClass) {
				case WARRIOR:
					badgeToCheck = Badge.MASTERY_WARRIOR;
					break;
				case MAGE:
					badgeToCheck = Badge.MASTERY_MAGE;
					break;
				case ROGUE:
					badgeToCheck = Badge.MASTERY_ROGUE;
					break;
				case HUNTRESS:
					badgeToCheck = Badge.MASTERY_HUNTRESS;
					break;
			}
			if (!Badges.isUnlocked(badgeToCheck)) {
				Dungeon.level.drop(new TomeOfMastery(), pos).sprite.drop();
			}

			GameScene.bossSlain();
			Dungeon.level.drop(new SkeletonKey(Dungeon.depth), pos).sprite.drop();
		}
		// end kPD

		super.die( cause );
		
		Badges.validateBossSlain();

		LloydsBeacon beacon = Dungeon.hero.belongings.getItem(LloydsBeacon.class);
		if (beacon != null) {
			beacon.upgrade();
			GLog.p("Your beacon grows stronger!");
		}
		
		yell( "Free at last..." );
	}
	
	@Override
	protected boolean getCloser( int target ) {
		if (Level.fieldOfView[target]) {
			jump();
			return true;
		} else {
			return super.getCloser( target );
		}
	}
	
	@Override
	protected boolean canAttack( Char enemy ) {
		return new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
	}
	
	@Override
	protected boolean doAttack( Char enemy ) {
		timeToJump--;
		if (timeToJump <= 0 && Level.adjacent( pos, enemy.pos )) {
			jump();
			return true;
		} else {
			return super.doAttack( enemy );
		}
	}
	
	private void jump() {
		timeToJump = JUMP_DELAY;
		
		// KPD - Infinite mode
		// Tengu isn't going to have his trap grid handy so just skip this
		if( Dungeon.depth < 26 ) {
			for (int i = 0; i < 4; i++) {
				int trapPos;
				do {
					trapPos = Random.Int(Level.LENGTH);
				} while (!Level.fieldOfView[trapPos] || !Level.passable[trapPos]);

				if (Dungeon.level.map[trapPos] == Terrain.INACTIVE_TRAP) {
					Dungeon.level.setTrap(new PoisonTrap().reveal(), trapPos);
					Level.set(trapPos, Terrain.TRAP);
					ScrollOfMagicMapping.discover(trapPos);
				}
			}
		}
		// end KPD
		
		int newPos;
		do {
			newPos = Random.Int( Level.LENGTH );
		} while (
			!Level.fieldOfView[newPos] ||
			!Level.passable[newPos] ||
			Level.adjacent( newPos, enemy.pos ) ||
			Actor.findChar( newPos ) != null);
		
		sprite.move( pos, newPos );
		move( newPos );
		
		if (Dungeon.visible[newPos]) {
			CellEmitter.get( newPos ).burst( Speck.factory( Speck.WOOL ), 6 );
			Sample.INSTANCE.play( Assets.SND_PUFF );
		}
		
		spend( 1 / speed() );
	}
	
	@Override
	public void notice() {
		super.notice();
		yell( "Gotcha, " + Dungeon.hero.givenName() + "!" );
	}
	
	@Override
	public String description() {
		return
			"Tengu are members of the ancient assassins clan, which is also called Tengu. " +
			"These assassins are noted for extensive use of shuriken and traps.";
	}
	
	private static final HashSet<Class<?>> RESISTANCES = new HashSet<Class<?>>();
	static {
		RESISTANCES.add( ToxicGas.class );
		RESISTANCES.add( Poison.class );
		RESISTANCES.add( Death.class );
		RESISTANCES.add( ScrollOfPsionicBlast.class );
	}
	
	@Override
	public HashSet<Class<?>> resistances() {
		return RESISTANCES;
	}
}
