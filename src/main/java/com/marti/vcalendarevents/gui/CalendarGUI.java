package com.marti.vcalendarevents.gui;

import com.marti.vcalendarevents.events.EventInstance;
import com.marti.vcalendarevents.vCalendarEvents;
import com.marti.vcalendarevents.events.Event;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalendarGUI implements InventoryHolder, Listener {

    private final vCalendarEvents plugin;
    private final Player player;
    private final Inventory inventory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private org.bukkit.scheduler.BukkitTask refreshTask;

    public CalendarGUI(vCalendarEvents plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        ConfigurationSection config = plugin.getConfigManager().getGuiConfig().getConfigurationSection("main_menu");
        String title = config.getString("title", "Calendar");
        int rows = config.getInt("rows", 4);

        this.inventory = Bukkit.createInventory(this, rows * 9, miniMessage.deserialize(title));

        initializeItems(config);

        // Register listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startRefreshTask();
    }

    private void initializeItems(ConfigurationSection config) {
        // Filler
        if (config.getBoolean("filler.enabled")) {
            Material mat = Material.getMaterial(config.getString("filler.material", "BLACK_STAINED_GLASS_PANE"));
            ItemStack filler = createItem(mat != null ? mat.name() : "BLACK_STAINED_GLASS_PANE",
                    config.getString("filler.name", " "));
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        // Navigation
        setNavigationItem(config, "items.back");
        setNavigationItem(config, "items.close");
        setNavigationItem(config, "items.month_calendar");

        updateDynamicItems();
    }

    private void updateDynamicItems() {
        ConfigurationSection config = plugin.getConfigManager().getGuiConfig().getConfigurationSection("main_menu");
        if (config == null)
            return;

        List<Integer> slots = config.getIntegerList("event_slots");
        List<EventInstance> nextInstances = plugin.getEventManager().getNextEvents(slots.size());

        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("Slots size: " + slots.size() + ", Instances size: " + nextInstances.size());
        }

        for (int i = 0; i < nextInstances.size() && i < slots.size(); i++) {
            EventInstance instance = nextInstances.get(i);
            Event event = instance.event();
            int slot = slots.get(i);

            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                plugin.getLogger()
                        .info("Updating slot " + slot + " for event " + event.getId() + " at " + instance.time());
            }

            ItemStack item = createItem(event.getIcon(), event.getDisplayName());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                String timePattern = plugin.getConfigManager().getGuiConfig().getString("formatting.daily_event_time",
                        "<gray>ʜᴏʀᴀ: <color:#FFD180>{time}");
                String timeStr = instance.time().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

                lore.add(miniMessage.deserialize("<!italic>" + timePattern.replace("{time}", timeStr)));

                String countdownPattern = plugin.getConfigManager().getGuiConfig()
                        .getString("formatting.daily_event_countdown", "<gray>ᴇᴍᴘɪᴇᴢᴀ ᴇɴ: <color:#FFD180>{duration}");
                long seconds = java.time.Duration.between(LocalDateTime.now(), instance.time()).getSeconds();
                lore.add(miniMessage.deserialize("<!italic>" + countdownPattern.replace("{duration}",
                        com.marti.vcalendarevents.utils.MessageUtils.formatDuration(seconds))));

                lore.add(miniMessage.deserialize(" "));
                for (String line : event.getDescription()) {
                    lore.add(miniMessage.deserialize("<!italic>" + line));
                }
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
        }
    }

    private void startRefreshTask() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateDynamicItems, 20L, 20L);
    }

    private void setNavigationItem(ConfigurationSection config, String path) {
        ConfigurationSection itemSection = config.getConfigurationSection(path);
        if (itemSection == null)
            return;

        int slot = itemSection.getInt("slot");
        String icon = itemSection.getString("material", "STONE");
        String name = itemSection.getString("name", "Item");
        List<String> loreStrings = itemSection.getStringList("lore");

        ItemStack item = createItem(icon, name);
        if (!loreStrings.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreStrings) {
                    lore.add(miniMessage.deserialize("<!italic>" + line));
                }
                meta.lore(lore);
                item.setItemMeta(meta);
            }
        }

        inventory.setItem(slot, item);
    }

    private ItemStack createItem(String icon, String name) {
        if (icon == null || icon.isEmpty())
            return new ItemStack(Material.PAPER);

        ItemStack item;
        Material mat = Material.matchMaterial(icon.toUpperCase());
        if (mat != null) {
            item = new ItemStack(mat);
        } else if (icon.length() > 64) { // Heuristic for Base64 texture
            item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit
                        .createProfile(java.util.UUID.randomUUID());
                profile.getProperties().add(new com.destroystokyo.paper.profile.ProfileProperty("textures", icon));
                skullMeta.setPlayerProfile(profile);
                item.setItemMeta(skullMeta);
            }
        } else {
            item = new ItemStack(Material.PAPER);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.displayName(miniMessage.deserialize("<!italic>" + name));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != this)
            return;
        e.setCancelled(true);

        if (e.getCurrentItem() == null)
            return;

        // Handle clicks based on slots or items
        // Simplified for now
        // Handle clicks based on slots or items
        // Simplified for now
        ConfigurationSection config = plugin.getConfigManager().getGuiConfig().getConfigurationSection("main_menu");
        int closeSlot = config.getInt("items.close.slot");
        int monthSlot = config.getInt("items.month_calendar.slot");

        int clicked = e.getSlot();
        if (clicked == closeSlot) {
            player.closeInventory();
        } else if (clicked == monthSlot) {
            new MonthCalendarGUI(plugin, player, java.time.LocalDate.now()).open();
        }
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (e.getInventory().getHolder() != this)
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (e.getInventory().getHolder() != this)
            return;
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        org.bukkit.event.HandlerList.unregisterAll(this);
    }
}
