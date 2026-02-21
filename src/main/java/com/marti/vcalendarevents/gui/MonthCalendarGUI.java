package com.marti.vcalendarevents.gui;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MonthCalendarGUI implements InventoryHolder, Listener {

    private final vCalendarEvents plugin;
    private final Player player;
    private final Inventory inventory;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LocalDate date;
    private org.bukkit.scheduler.BukkitTask refreshTask;

    public MonthCalendarGUI(vCalendarEvents plugin, Player player, LocalDate date) {
        this.plugin = plugin;
        this.player = player;
        this.date = date;
        this.inventory = Bukkit.createInventory(this,
                plugin.getConfigManager().getGuiConfig().getInt("month_menu.rows", 6) * 9,
                miniMessage.deserialize(
                        plugin.getConfigManager().getGuiConfig().getString("month_menu.title", "Month Calendar")));

        setupInventory();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startRefreshTask();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void setupInventory() {
        ConfigurationSection config = plugin.getConfigManager().getGuiConfig().getConfigurationSection("month_menu");
        if (config == null)
            return;

        if (config.getBoolean("filler.enabled", true)) {
            ItemStack filler = createItem(config.getString("filler.material", "GRAY_STAINED_GLASS_PANE"),
                    config.getString("filler.name", " "));
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        setNavigationItem(config, "items.back");

        // Days of the month
        int daysInMonth = date.lengthOfMonth();
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES"))
                .toUpperCase();

        for (int i = 1; i <= daysInMonth; i++) {
            updateDayItem(i, date.withDayOfMonth(i), monthName);
        }
    }

    private void updateDayItem(int dayNum, LocalDate dayDate, String monthName) {
        ConfigurationSection config = plugin.getConfigManager().getGuiConfig().getConfigurationSection("month_menu");
        List<Integer> eventSlots = config != null ? config.getIntegerList("event_slots") : null;

        int slot;
        if (eventSlots != null && !eventSlots.isEmpty() && dayNum <= eventSlots.size()) {
            slot = eventSlots.get(dayNum - 1);
        } else {
            LocalDate firstDay = date.withDayOfMonth(1);
            int startSlot = firstDay.getDayOfWeek().getValue() - 1;
            slot = startSlot + dayNum - 1;
        }

        // Configurable date pattern
        String datePattern = plugin.getConfigManager().getGuiConfig().getString("formatting.month_day_name",
                "<color:#FFD180>{day} ᴅᴇ {month}");
        ItemStack dayItem = createItem("PAPER",
                datePattern.replace("{day}", String.valueOf(dayNum)).replace("{month}", monthName));

        ItemMeta meta = dayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            // Configurable day name pattern
            String dayNamePattern = plugin.getConfigManager().getGuiConfig()
                    .getString("formatting.month_day_of_week", "<gray>{day_name}");
            String dayName = dayDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES"))
                    .toUpperCase();
            lore.add(miniMessage.deserialize("<!italic>" + dayNamePattern.replace("{day_name}", dayName)));
            lore.add(miniMessage.deserialize(" "));

            List<Event> events = plugin.getEventManager().getEventsForDate(dayDate);
            if (events.isEmpty()) {
                String noEvents = plugin.getConfigManager().getGuiConfig().getString("formatting.no_events",
                        "<gray>ɴᴏ ʜᴀʏ ᴇᴠᴇɴᴛᴏs");
                lore.add(miniMessage.deserialize("<!italic>" + noEvents));
            } else {
                String eventPattern = plugin.getConfigManager().getGuiConfig().getString(
                        "formatting.month_event_item",
                        "<color:#FFD180>● <gray>{time} - <color:#FFD180>{event} <gray>({duration})");
                for (Event event : events) {
                    if (plugin.getConfig().getBoolean("settings.debug", false)) {
                        plugin.getLogger().info("Updating day " + dayNum + " for event " + event.getId() + " with icon "
                                + event.getIcon());
                    }
                    String time = "??:??";
                    java.time.DayOfWeek dayOfWeek = dayDate.getDayOfWeek();
                    for (Event.EventSchedule schedule : event.getSchedules()) {
                        if (schedule.day() == null || schedule.day() == dayOfWeek) {
                            time = schedule.time().format(DateTimeFormatter.ofPattern("HH:mm"));
                            break;
                        }
                    }
                    long seconds = plugin.getEventManager().getSecondsUntilEvent(event.getId());
                    String line = eventPattern.replace("{time}", time)
                            .replace("{event}", event.getDisplayName())
                            .replace("{duration}",
                                    com.marti.vcalendarevents.utils.MessageUtils.formatDuration(seconds));
                    lore.add(miniMessage.deserialize("<!italic>" + line));
                }
            }
            meta.lore(lore);
            dayItem.setItemMeta(meta);
        }

        inventory.setItem(slot, dayItem);
    }

    private void updateDynamicItems() {
        int daysInMonth = date.lengthOfMonth();
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-ES"))
                .toUpperCase();
        for (int i = 1; i <= daysInMonth; i++) {
            updateDayItem(i, date.withDayOfMonth(i), monthName);
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
        List<String> loreLines = itemSection.getStringList("lore");

        ItemStack item = createItem(icon, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(miniMessage.deserialize("<!italic>" + line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    private ItemStack createItem(String icon, String name) {
        if (icon == null || icon.isEmpty())
            return new ItemStack(Material.PAPER);

        ItemStack item;
        Material mat = Material.getMaterial(icon.toUpperCase());
        if (mat != null) {
            item = new ItemStack(mat);
        } else if (icon.length() > 64) {
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
            meta.displayName(miniMessage.deserialize("<!italic>" + name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory))
            return;
        event.setCancelled(true);

        if (event.getRawSlot() == plugin.getConfigManager().getGuiConfig().getInt("month_menu.items.back.slot", 49)) {
            new CalendarGUI(plugin, player).open();
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!event.getInventory().equals(inventory))
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory))
            return;
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
