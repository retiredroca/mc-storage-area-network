package com.retiredroca.craftingnetwork.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;

@Mixin(RecipeBookTabButton.class)
public interface RecipeBookTabButtonAccessor {
    @Accessor("animationTime")
    void craftingnetwork$setAnimationTime(float value);
}
