package me.oblueberrey.meowMcEvents.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ButtonBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ButtonBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ButtonBuilder name(String name) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(MessageUtils.format(name));
        }
        return this;
    }

    public ButtonBuilder lore(String... lore) {
        if (itemMeta != null) {
            itemMeta.setLore(Arrays.stream(lore)
                    .map(s -> MessageUtils.format(s))
                    .collect(Collectors.toList()));
        }
        return this;
    }

    public ButtonBuilder lore(List<String> lore) {
        if (itemMeta != null) {
            itemMeta.setLore(lore.stream()
                    .map(s -> MessageUtils.format(s))
                    .collect(Collectors.toList()));
        }
        return this;
    }

    public ButtonBuilder addLore(String line) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(MessageUtils.format(line));
            itemMeta.setLore(lore);
        }
        return this;
    }

    public ButtonBuilder flags(ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }

    public ButtonBuilder glow(boolean glow) {
        if (glow && itemMeta != null) {
            // In modern Spigot/Paper, we might use Enchantment.LUCK_OF_THE_SEA with hide flag
            // For simplicity, we'll just use ItemFlags if they exist for hide_enchants
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
