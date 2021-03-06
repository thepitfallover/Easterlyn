package com.easterlyn.effects.effect.passive;

import com.easterlyn.Easterlyn;
import com.easterlyn.effects.effect.BehaviorCooldown;
import com.easterlyn.effects.effect.BehaviorPassive;
import com.easterlyn.effects.effect.Effect;
import com.easterlyn.utilities.Potions;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Effect for passively granting the speed PotionEffect.
 * 
 * @author Jikoo
 */
public class EffectSpeed extends Effect implements BehaviorPassive, BehaviorCooldown {

	public EffectSpeed(Easterlyn plugin) {
		super(plugin, 500, 2, 10, "Speed");
	}

	@Override
	public String getCooldownName() {
		return "Effect:Speed";
	}

	@Override
	public long getCooldownDuration() {
		return 5000;
	}

	@Override
	public void applyEffect(LivingEntity entity, int level) {
		if (level < 1) {
			level = 1;
		}
		int duration = entity instanceof Player ? 200 : Integer.MAX_VALUE;
		Potions.applyIfBetter(entity, new PotionEffect(PotionEffectType.SPEED, duration, level - 1));
	}

}
