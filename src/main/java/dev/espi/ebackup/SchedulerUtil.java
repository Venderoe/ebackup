package dev.espi.ebackup;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SchedulerUtil {

    public static ScheduledTask runAsync(Runnable runnable) {
        Plugin plugin = eBackup.getPlugin();
        return plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run());
    }

    public static ScheduledTask runAsyncTimer(Runnable runnable, long initialDelayTicks, long periodTicks) {
        Plugin plugin = eBackup.getPlugin();
        long initialDelayMillis = ticksToMillis(initialDelayTicks);
        long periodMillis = ticksToMillis(periodTicks);
        return plugin.getServer().getAsyncScheduler().runAtFixedRate(
                plugin,
                task -> runnable.run(),
                initialDelayMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public static void runGlobal(Runnable runnable) {
        Plugin plugin = eBackup.getPlugin();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> runnable.run());
    }

    public static <T> T callGlobal(Supplier<T> supplier) throws ExecutionException, InterruptedException {
        CompletableFuture<T> future = new CompletableFuture<>();
        runGlobal(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future.get();
    }

    public static void runGlobalAndWait(Runnable runnable) throws ExecutionException, InterruptedException {
        callGlobal(() -> {
            runnable.run();
            return null;
        });
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            Plugin plugin = eBackup.getPlugin();
            Player player = (Player) sender;
            player.getScheduler().run(plugin, task -> player.sendMessage(message), null);
            return;
        }

        runGlobal(() -> sender.sendMessage(message));
    }

    public static void cancelTasks(Plugin plugin) {
        plugin.getServer().getAsyncScheduler().cancelTasks(plugin);
        plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
    }

    private static long ticksToMillis(long ticks) {
        return ticks * 50L;
    }
}
