package me.darkolythe.shulkerpacks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.Queue;

public class ShulkerListener implements Listener {

    public ShulkerPacks main;
    public ShulkerListener(ShulkerPacks plugin) {
        this.main = plugin; //set it equal to an instance of main
    }

    /*
    Saves the shulker on inventory drag if its open
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable() {
                @Override
                public void run() {
                    if (!saveShulker(player, event.getView().getTitle())) {
                        event.setCancelled(true);
                    }
                }
            }, 1);
        }
    }

    @EventHandler
    private void onItemThrow(PlayerDropItemEvent event) {
        main.putThrownItem(event.getPlayer(), event.getItemDrop().getItemStack());
    }

    /*
    Opens the shulker if its not in a weird inventory, then saves it
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem() != null && (main.openshulkers.containsKey(player.getUniqueId()) && event.getCurrentItem().equals(main.openshulkers.get(player.getUniqueId())))) {
                event.setCancelled(true);
                return;
            }
            if (event.getClickedInventory() != null && (event.getClickedInventory().getType() == InventoryType.CHEST && !main.canopeninchests)) {
                return;
            }
            InventoryType type = event.getInventory().getType();
            if (type == InventoryType.WORKBENCH || type == InventoryType.ANVIL || type == InventoryType.BEACON || type == InventoryType.MERCHANT || type == InventoryType.ENCHANTING) {
                return;
            }
            if (type == InventoryType.CRAFTING && event.getRawSlot() >= 1 && event.getRawSlot() <= 4) {
                return;
            }

            if (!main.canopeninenderchest && type == InventoryType.ENDER_CHEST) {
                return;
            }

            for (String str: main.blacklist) {
                if (ChatColor.translateAlternateColorCodes('&', str).equals(player.getOpenInventory().getTitle())) {
                    return;
                }
            }
            if (!main.shiftclicktoopen || event.isShiftClick()) {
                if (event.getClickedInventory() != null && event.isRightClick() && openInventoryIfShulker(event.getCurrentItem(), player)) {
                    event.setCancelled(true);
                    return;
                }
            }
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable() {
                @Override
                public void run() {
                    if (!saveShulker(player, event.getView().getTitle())) {
                        event.setCancelled(true);
                    }
                }
            }, 1);
        }
    }

    /*
    Saves the shulker if its open, then removes the current open shulker from the player data
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (saveShulker(player, player.getOpenInventory().getTitle())) {
                player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, 1, 1);
            }
            main.openshulkers.remove(player.getUniqueId());
        }
    }

    /*
    Opens the shulker if the air was clicked with one
     */
    @EventHandler
    public void onClickAir(PlayerInteractEvent event) {
        if (main.canopeninair && (event.getClickedBlock() == null || event.getClickedBlock().getType() == Material.AIR)) {
            if ((!main.shiftclicktoopen || event.getPlayer().isSneaking())) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                    ItemStack item = event.getItem();
                    openInventoryIfShulker(item, event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onShulkerPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType().toString().contains("SHULKER_BOX")) {
            if (!main.canplaceshulker) {
                event.setCancelled(true);
            }
        }
    }

    /*
    Saves the shulker data in the itemmeta
     */
    public boolean saveShulker(Player player, String title) {
        try {
            if (main.openshulkers.containsKey(player.getUniqueId())) {
                if (title.equals(main.defaultname) || (main.openshulkers.get(player.getUniqueId()).hasItemMeta() &&
                        main.openshulkers.get(player.getUniqueId()).getItemMeta().hasDisplayName() &&
                        (main.openshulkers.get(player.getUniqueId()).getItemMeta().getDisplayName().equals(title)))) {
                    ItemStack item = main.openshulkers.get(player.getUniqueId());
                    if (item != null) {
                        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                        shulker.getInventory().setContents(main.openinventories.get(player.getUniqueId()).getContents());
                        meta.setBlockState(shulker);
                        item.setItemMeta(meta);
                        main.openshulkers.put(player.getUniqueId(), item);
                        player.updateInventory();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            main.openshulkers.remove(player.getUniqueId());
            player.closeInventory();
            return false;
        }
        return false;
    }

    /*
    Opens the shulker inventory with the contents of the shulker
     */
    public boolean openInventoryIfShulker(ItemStack item, Player player) {
        if (player.hasPermission("shulkerpacks.use")) {
            if (item != null) {
                if (item.getAmount() == 1) {
                    if (item.getItemMeta() instanceof BlockStateMeta) {
                        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        if (meta.getBlockState() instanceof ShulkerBox) {
                            ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
                            Inventory inv;
                            if (meta.hasDisplayName()) {
                                inv = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, meta.getDisplayName());
                            } else {
                                inv = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, main.defaultname);
                            }
                            inv.setContents(shulker.getInventory().getContents());
                            player.openInventory(inv);
                            player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1, 1);
                            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable() {
                                @Override
                                public void run() {
                                    main.openshulkers.put(player.getUniqueId(), item);
                                    main.openinventories.put(player.getUniqueId(), player.getOpenInventory().getTopInventory());
                                }
                            }, 1);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
